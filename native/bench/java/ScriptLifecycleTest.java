import com.lx862.mtrscripting.core.ScriptInstanceManager;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.rhinojs.RhinoParsedScript;
import com.lx862.mtrscripting.core.util.TimingJS;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Function;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Lifecycle integration against the complete production classes.
 * Rhino's interpreter is a fixture; scheduling, state flags, cleanup and
 * manager invalidation are production code. No sleeps or copied lifecycle.
 */
public class ScriptLifecycleTest {
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private static void check(boolean value, String message) {
        CHECKS.incrementAndGet();
        if (!value) throw new AssertionError(message);
    }

    private static void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError(message);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(message, exception);
        }
    }

    private static void finish(Future<?> task) throws Exception {
        check(task != null, "expected a scheduled production task");
        task.get(5, TimeUnit.SECONDS);
    }

    private static final class TestContext extends AbstractScriptContext {
        final AtomicInteger resets = new AtomicInteger();
        TestContext() { super("lifecycle-fixture"); }
        @Override public void resetForNextRun() {
            super.resetForNextRun();
            resets.incrementAndGet();
        }
    }

    private static final class TestInstance extends ScriptInstance<Object> {
        volatile boolean invalid;
        TestInstance(TestContext context, ParsedScript script) { super(context, script); }
        @Override public boolean shouldInvalidate() { return invalid; }
    }

    private static final class TestScript extends RhinoParsedScript {
        TestScript(ScriptManager manager, double rateLimit) {
            super(manager, "fixture", "fixture", Configuration.create(List.of(), "javascript", rateLimit));
            parse(false);
        }
        void expireCooldown() { lastFailedTime = System.currentTimeMillis() - 5000; }
        void startCooldown() { lastFailedTime = System.currentTimeMillis(); }
        @SuppressWarnings("unchecked")
        void callback(String phase, Function function) throws Exception {
            Field field = RhinoParsedScript.class.getDeclaredField(phase + "Functions");
            field.setAccessible(true);
            ((List<Function>) field.get(this)).add(function);
        }
    }

    private static final class Environment implements AutoCloseable {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ScriptManager manager = new ScriptManager(executor);
        final ScriptInstanceManager instances = new ScriptInstanceManager();
        final TestScript script;
        final TestContext context = new TestContext();
        final TestInstance instance;
        Environment(double rateLimit) {
            script = new TestScript(manager, rateLimit);
            instance = new TestInstance(context, script);
            instance.setWrapperObject(new Object());
        }
        void drain() throws Exception {
            finish(executor.submit(() -> check(Context.getCurrentContext() == null,
                    "worker Rhino context is balanced after lifecycle tasks")));
        }
        @Override public void close() throws Exception {
            executor.shutdownNow();
            check(executor.awaitTermination(5, TimeUnit.SECONDS), "test worker stopped");
        }
    }

    private static void failedCreateCanRetry() throws Exception {
        try (Environment env = new Environment(0)) {
            AtomicInteger creates = new AtomicInteger(), renders = new AtomicInteger();
            AtomicInteger published = new AtomicInteger(), closed = new AtomicInteger();
            env.script.callback("create", (cx, scope, self, arguments) -> {
                int attempt = creates.incrementAndGet();
                env.context.addDisposable(() -> closed.incrementAndGet());
                if (attempt == 1) throw new IllegalStateException(); // deliberately no message
                return null;
            });
            env.script.callback("render", (cx, scope, self, arguments) -> { renders.incrementAndGet(); return null; });
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(!env.instance.isCreateFunctionInvoked(), "failed create stays incomplete");
            check(published.get() == 0, "failed create does not publish a frame");
            check(closed.get() == 1, "failed create closes its registered resource");
            check(env.script.getCapturedScriptException() != null
                    && env.script.getCapturedScriptException().getCause() instanceof IllegalStateException,
                    "exception with null message is captured without masking cleanup");
            check(env.script.duringFailCooldown(), "failed create enters cooldown");
            int submitted = env.manager.submissions.get();
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            check(env.manager.submissions.get() == submitted && creates.get() == 1,
                    "cooldown suppresses a retry");
            env.script.expireCooldown();
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(creates.get() == 2 && env.instance.isCreateFunctionInvoked(),
                    "create is retried successfully after cooldown");
            check(published.get() == 1 && renders.get() == 0,
                    "successful retry publishes only its create frame");
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(renders.get() == 1 && published.get() == 2, "render follows successful create");
            env.script.invokeDisposeFunctions(env.instance, null);
            env.drain();
            check(closed.get() == 2, "retry resource closes once on disposal");
            check(env.manager.getLogger().fatals.get() == 0, "no lifecycle finalization error");
        }
    }

    private static void nullableCallbacksAndDisposeFailure() throws Exception {
        try (Environment env = new Environment(0)) {
            AtomicInteger closed = new AtomicInteger(), completed = new AtomicInteger();
            env.script.callback("create", (cx, scope, self, arguments) -> {
                env.context.addDisposable(() -> closed.incrementAndGet());
                return null;
            });
            finish(env.script.invokeCreateFunctions(env.instance, null));
            check(env.instance.isCreateFunctionInvoked(), "nullable create completion is supported");
            env.script.callback("dispose", (cx, scope, self, arguments) -> { throw new IllegalStateException(); });
            env.script.invokeDisposeFunctions(env.instance, completed::incrementAndGet);
            env.drain();
            check(closed.get() == 1, "throwing dispose closes each resource once");
            check(completed.get() == 1, "throwing dispose still completes cleanup");
            env.script.invokeDisposeFunctions(env.instance, completed::incrementAndGet);
            env.drain();
            check(closed.get() == 1 && completed.get() == 1, "repeated disposal is idempotent");
        }
    }

    private static void failedRenderPreservesCreatedResources() throws Exception {
        try (Environment env = new Environment(0)) {
            AtomicInteger creates = new AtomicInteger(), renders = new AtomicInteger();
            AtomicInteger published = new AtomicInteger(), closed = new AtomicInteger();
            AtomicReference<Object> createdState = new AtomicReference<>();
            env.script.callback("create", (cx, scope, self, arguments) -> {
                creates.incrementAndGet();
                createdState.set(arguments[1]);
                env.context.addDisposable(() -> closed.incrementAndGet());
                return null;
            });
            env.script.callback("render", (cx, scope, self, arguments) -> {
                check(arguments[1] == createdState.get(), "render retains the created state object");
                check(closed.get() == 0, "render can use its create-time resource");
                if (renders.incrementAndGet() == 1) throw new IllegalStateException("temporary render failure");
                return null;
            });
            env.script.callback("dispose", (cx, scope, self, arguments) -> {
                check(arguments[1] == createdState.get() && closed.get() == 0,
                        "dispose receives the same state with its resource still open");
                return null;
            });
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(creates.get() == 1 && published.get() == 1, "initial create publishes normally");
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(env.instance.isCreateFunctionInvoked() && closed.get() == 0,
                    "render failure preserves the created instance and its resources");
            check(published.get() == 1, "failed render does not publish a partial frame");
            check(env.context.resets.get() == 1, "failed render discards its pending frame via resetForNextRun");
            check(env.script.duringFailCooldown(), "failed render enters cooldown");
            int submitted = env.manager.submissions.get();
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            check(env.manager.submissions.get() == submitted && renders.get() == 1,
                    "render cooldown suppresses premature retries");
            env.script.expireCooldown();
            env.script.invokeRenderFunctions(env.instance, published::incrementAndGet);
            finish(env.instance.scriptTask);
            check(creates.get() == 1 && renders.get() == 2 && published.get() == 2,
                    "render recovers after cooldown without re-running create");
            check(closed.get() == 0 && env.context.resets.get() == 1,
                    "successful render keeps resources live without failure cleanup");
            env.script.invokeDisposeFunctions(env.instance, null);
            env.drain();
            check(closed.get() == 1, "final disposal closes the retained resource exactly once");
            check(env.manager.getLogger().fatals.get() == 0, "render recovery has no finalization error");
        }
    }

    private static void disposalWaitsForCreate() throws Exception {
        try (Environment env = new Environment(100)) {
            List<String> events = new CopyOnWriteArrayList<>();
            AtomicInteger closed = new AtomicInteger(), renders = new AtomicInteger();
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            env.script.callback("create", (cx, scope, self, arguments) -> {
                events.add("create-start");
                env.context.addDisposable(() -> { closed.incrementAndGet(); events.add("close"); });
                entered.countDown();
                await(release, "release in-flight create");
                check(closed.get() == 0, "create still owns its live resource");
                events.add("create-end");
                return null;
            });
            env.script.callback("render", (cx, scope, self, arguments) -> { renders.incrementAndGet(); return null; });
            env.script.callback("dispose", (cx, scope, self, arguments) -> {
                check(closed.get() == 0, "dispose callback sees live resources");
                events.add("dispose");
                return null;
            });
            try {
                env.script.invokeRenderFunctions(env.instance, null);
                await(entered, "create entered worker");
                check(!env.instance.isCreateFunctionInvoked(), "create is still incomplete");
                env.script.startCooldown();
                env.script.invokeDisposeFunctions(env.instance, () -> events.add("complete"));
                check(env.instance.isDisposing(), "disposal immediately blocks new work");
                check(closed.get() == 0, "requesting disposal does not close resources on caller thread");
                int submitted = env.manager.submissions.get();
                env.script.invokeRenderFunctions(env.instance, null);
                check(!env.script.willInvokeRender(env.instance)
                        && env.manager.submissions.get() == submitted,
                        "render is rejected after disposal is requested");
                check(env.script.invokeCreateFunctions(env.instance, null) == null,
                        "direct create is rejected after disposal is requested");
            } finally {
                release.countDown();
            }
            finish(env.instance.scriptTask);
            env.drain();
            check(events.equals(List.of("create-start", "create-end", "dispose", "close", "complete")),
                    "dispose bypasses cooldown/rate-limit and follows in-flight create");
            check(closed.get() == 1 && renders.get() == 0, "in-flight instance resource closes once");
        }
    }

    private static void managerDisposesInFlightCreate(boolean reset) throws Exception {
        try (Environment env = new Environment(0)) {
            AtomicInteger closed = new AtomicInteger(), disposed = new AtomicInteger();
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            env.instances.getInstance(new UniqueKey(reset ? "reset" : "tick"), () -> env.instance);
            env.script.callback("create", (cx, scope, self, arguments) -> {
                env.context.addDisposable(() -> closed.incrementAndGet());
                entered.countDown();
                await(release, "release create during manager cleanup");
                check(closed.get() == 0, "manager must not close resource while create is running");
                return null;
            });
            env.script.callback("dispose", (cx, scope, self, arguments) -> { disposed.incrementAndGet(); return null; });
            try {
                env.script.invokeRenderFunctions(env.instance, null);
                await(entered, "manager fixture create entered");
                check(!env.instance.isCreateFunctionInvoked(), "manager sees unfinished create");
                if (reset) {
                    env.instances.reset();
                } else {
                    env.instance.invalid = true;
                    check(env.instances.tick() == 1, "tick removes invalid in-flight instance");
                }
                check(env.instances.getInstances().isEmpty(), "manager drops removed instance");
                check(closed.get() == 0 && disposed.get() == 0,
                        "manager queues cleanup instead of prematurely closing resources");
            } finally {
                release.countDown();
            }
            finish(env.instance.scriptTask);
            env.drain();
            check(disposed.get() == 1 && closed.get() == 1,
                    (reset ? "reset" : "tick") + " cleans up after unfinished create");
            env.instances.reset();
            check(env.instances.tick() == 0, "removed instance cannot be disposed again by manager");
        }
    }

    private static void neverCreatedInstanceStillCleansContext() throws Exception {
        try (Environment env = new Environment(0)) {
            AtomicInteger closed = new AtomicInteger(), disposed = new AtomicInteger();
            env.context.addDisposable(() -> closed.incrementAndGet());
            env.script.callback("dispose", (cx, scope, self, arguments) -> { disposed.incrementAndGet(); return null; });
            env.instances.getInstance(new UniqueKey("never-created"), () -> env.instance);
            env.instance.invalid = true;
            check(env.instances.tick() == 1, "never-created invalid instance removed");
            env.drain();
            check(closed.get() == 1 && disposed.get() == 0,
                    "never-created instance frees context without calling JS dispose");
        }
    }

    public static void main(String[] arguments) throws Exception {
        TimingJS.update(1_000_000); // deterministic game time; no sleeping for rate limits
        failedCreateCanRetry();
        failedRenderPreservesCreatedResources();
        nullableCallbacksAndDisposeFailure();
        disposalWaitsForCreate();
        managerDisposesInFlightCreate(false);
        managerDisposesInFlightCreate(true);
        neverCreatedInstanceStillCleansContext();
        check(Context.getCurrentContext() == null, "main-thread parsing context balanced");
        System.out.println("SCRIPT LIFECYCLE OK: " + CHECKS.get() + " assertions");
    }
}
