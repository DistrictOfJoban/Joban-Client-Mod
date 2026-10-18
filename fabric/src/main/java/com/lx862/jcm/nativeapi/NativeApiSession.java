package com.lx862.jcm.nativeapi;

import com.google.gson.*;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject;
import org.mtr.mapping.holder.Identifier;

import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Direct access to the same Java objects used by JS, without evaluating scripts. */
public final class NativeApiSession {
    private static final Gson JSON = new GsonBuilder().serializeNulls().create();
    private static final Map<String, NativeApiSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ThreadLocal<NativeApiSession> ACTIVE = new ThreadLocal<>();
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "JCM native background worker");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    /** Values of TYPES, snapshotted once: resolveClass scanned the whole map
     *  (containsValue) on every class resolution. */
    private static final Set<String> TYPE_NAMES = new HashSet<>();
    static {
        String util = "com.lx862.mtrscripting.core.util.";
        register("Resources", util + "ScriptResourceUtil");
        for (String name : new String[]{"GraphicsTexture", "Matrices"}) register(name, util + name);
        register("Vector3f", util + "ScriptVector3f");
        for (String name : new String[]{"StateTracker", "CycleTracker", "RateLimit", "Networking", "Files", "DataReader"}) register(name, util + name + "JS");
        for (String name : new String[]{"ModelManager", "RawModel", "RawMeshBuilder", "DynamicModelHolder", "Model"}) register(name, util + "model." + name + "JS");
        for (String name : new String[]{"ModelDrawCall", "QuadDrawCall"}) register(name, util + "render." + name);
        register("TickableSoundInstance", util + "sound.TickableSoundInstanceJS");
        for (String name : new String[]{"MinecraftClient", "VanillaText", "VoxelShape", "PlayerEntity", "ItemStack"}) {
            register(name, "com.lx862.mtrscripting.core.integration." + name + "Wrapper");
        }
        for (String name : new String[]{"Text", "Texture", "Rectangle"}) register(name, "com.lx862.jcm.mod.scripting.pids." + name + "Wrapper");
        register("MTRClientData", "org.mtr.mod.client.MinecraftClientData");
        register("MTR", "com.lx862.mtrscripting.mod.impl.mtr.util.MTRWrapper");
        register("TextUtil", "com.lx862.mtrscripting.mod.impl.mtr.util.TextUtilJS");
        for (String name : new String[]{"TransportMode", "Route", "RouteType", "Station", "StationExit", "Siding", "Platform", "SimplifiedRoute", "SimplifiedRoutePlatform", "VehicleExtraData", "PathData", "Rail", "Position"}) register(name, "org.mtr.core.data." + name);
        register("Vector", "org.mtr.core.tool.Vector");
        register("Direction", "org.mtr.mapping.holder.Direction");
        register("DisplayHelper", "com.lx862.jcm.nativeapi.NativeDisplayHelper");
        register("RenderManager", util + "render.ScriptRenderManager");
        register("SoundManager", util + "sound.ScriptSoundManager");
        register("NetworkResponse", util + "NetworkingJS$NetworkResponse");
        register("Vehicle", "org.mtr.core.data.Vehicle");
        register("VehicleRidingEntity", "org.mtr.core.data.VehicleRidingEntity");
        register("Route.CircularState", "org.mtr.core.data.Route$CircularState");
        register("CircularState", "org.mtr.core.data.Route$CircularState");
        TYPE_NAMES.addAll(TYPES.values());
    }

    private static void register(String alias, String name) { TYPES.put(alias, name); }

    private static final ClassValue<Map<String, List<Method>>> METHODS = new ClassValue<>() {
        @Override protected Map<String, List<Method>> computeValue(Class<?> type) {
            Map<String, List<Method>> result = new HashMap<>();
            for (Method method : type.getMethods()) {
                Method visible = publicDeclaration(type, method);
                if (visible != null) result.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(visible);
            }
            return result;
        }
    };

    private static Method publicDeclaration(Class<?> type, Method method) {
        if (Modifier.isPublic(method.getDeclaringClass().getModifiers()) && !method.getDeclaringClass().getName().startsWith("sun.")) return method;
        for (Class<?> iface : type.getInterfaces()) {
            try { return iface.getMethod(method.getName(), method.getParameterTypes()); }
            catch (NoSuchMethodException ignored) {}
        }
        Class<?> parent = type.getSuperclass();
        if (parent == null) return null;
        try { return publicDeclaration(parent, parent.getMethod(method.getName(),method.getParameterTypes())); }
        catch (NoSuchMethodException e) { return null; }
    }

    private final String key;
    private final Map<Long, Object> objects = new ConcurrentHashMap<>();
    private final IdentityHashMap<Object, Long> identities = new IdentityHashMap<>();
    private final Map<Long, Integer> references = new HashMap<>();
    private final Set<Object> owned = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Future<?>> tasks = new CopyOnWriteArrayList<>();
    private final ReentrantReadWriteLock lifetime = new ReentrantReadWriteLock();
    private final Object console;
    private final NativeTiming timing = new NativeTiming();
    private volatile Object context;
    private volatile Object wrapper;
    /* volatile: bind() writes these on the render thread, dispatch() can run
       on the "JCM native background worker" thread (Background.submit) — a
       plain field allowed the worker to read a stale scriptInput/resourceBase. */
    private volatile JsonElement scriptInput = JsonNull.INSTANCE;
    private volatile String resourceBase = "minecraft:";
    private volatile boolean closed;
    private volatile boolean callbacksStopped;

    private NativeApiSession(String key) {
        this.key = key;
        try { console = Class.forName("com.lx862.mtrscripting.core.util.ConsoleJS").getConstructor().newInstance(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    public static NativeApiSession bind(String key, Object context, Object wrapper, JsonElement input, String resourceBase) {
        NativeApiSession session = SESSIONS.computeIfAbsent(key, NativeApiSession::new);
        session.context = context;
        session.wrapper = wrapper;
        session.scriptInput = input == null ? JsonNull.INSTANCE : input;
        session.resourceBase = resourceBase;
        session.timing.step();
        return session;
    }

    public static byte[] invoke(byte[] key, byte[] request) {
        NativeApiSession session = SESSIONS.get(new String(key, StandardCharsets.UTF_8));
        if (session == null) return "{\"error\":\"native API session is disposed\"}".getBytes(StandardCharsets.UTF_8);
        return session.invoke(new String(request, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
    }

    public String invoke(String request) {
        lifetime.readLock().lock();
        NativeApiSession previous = ACTIVE.get();
        ACTIVE.set(this);
        try {
            if (closed) throw new IllegalStateException("native API session is disposed");
            JsonObject reply = new JsonObject();
            reply.add("value", dispatch(JsonParser.parseString(request).getAsJsonObject()));
            return JSON.toJson(reply);
        } catch (Throwable e) {
            /* Throwable, not Exception: a StackOverflowError/OOM escaping as a
               pending JNI exception breaks the JSON reply contract with the
               C++ caller; degrade to an error reply like every other failure. */
            Throwable cause = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
            JsonObject error = new JsonObject();
            error.addProperty("error", cause.toString());
            return JSON.toJson(error);
        } finally {
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
            lifetime.readLock().unlock();
        }
    }

    private JsonElement dispatch(JsonObject request) throws Exception {
        String op = request.get("op").getAsString();
        if (op.equals("root")) {
            switch (request.get("name").getAsString()) {
                case "context": return encode(context);
                case "wrapper": return encode(wrapper);
                case "SCRIPT_INPUT": return scriptInput;
                case "console": return encode(console);
                case "Timing": return encode(timing);
                case "BackgroundWorker": return encode(new Background(this));
                default: throw new IllegalArgumentException("Unknown native API root");
            }
        }
        if (op.equals("release")) {
            release(request.get("handle").getAsLong());
            return JsonNull.INSTANCE;
        }
        Object target;
        if (request.has("class")) target = resolveClass(request.get("class").getAsString());
        else target = decode(request.get("target"));
        if (target == null) throw new IllegalArgumentException("Null native API target");
        if (op.equals("new")) {
            Class<?> type = (Class<?>)target;
            JsonArray args = request.getAsJsonArray("args");
            Executable selected = select(Arrays.asList(type.getConstructors()), args, false);
            Object value = ((Constructor<?>)selected).newInstance(arguments(selected, args));
            own(value);
            return encode(value);
        }
        Class<?> type = target instanceof Class<?> ? (Class<?>)target : target.getClass();
        String member = request.get("member").getAsString();
        if (type.isArray()) {
            if (member.equals("size") || member.equals("length")) return encode(Array.getLength(target));
            int index = op.equals("call") ? request.getAsJsonArray("args").get(0).getAsInt() : Integer.parseInt(member);
            if(op.equals("set")) { Array.set(target,index,convert(request.get("value"),type.getComponentType())); return JsonNull.INSTANCE; }
            return encode(Array.get(target,index));
        }
        if (op.equals("get") || op.equals("set")) {
            if (target instanceof Map<?, ?>) {
                if (op.equals("get")) return encode(((Map<?, ?>)target).get(member));
                ((Map<Object, Object>)target).put(member, decode(request.get("value")));
                return JsonNull.INSTANCE;
            }
            Field field = type.getField(member);
            if (op.equals("get")) return encode(field.get(target instanceof Class<?> ? null : target));
            field.set(target instanceof Class<?> ? null : target, convert(request.get("value"), field.getType()));
            return JsonNull.INSTANCE;
        }
        if (!op.equals("call")) throw new IllegalArgumentException("Unknown native API operation: " + op);
        // Native libraries have a declared resource base, unlike the JS parse stack.
        if (target instanceof Class<?> && ((Class<?>)target).getName().equals(TYPES.get("Resources")) && (member.equals("idr") || member.equals("idRelative"))) {
            Identifier base = new Identifier(resourceBase);
            String path = request.getAsJsonArray("args").get(0).getAsString();
            return encode(new Identifier(base.getNamespace() + ":" + java.nio.file.Path.of(base.getPath()).resolveSibling(path).normalize().toString().replace('\\', '/')));
        }
        List<Method> methods = METHODS.get(type).getOrDefault(member, Collections.emptyList());
        JsonArray args = request.getAsJsonArray("args");
        Executable selected = select(methods, args, target instanceof Class<?>);
        Object result = ((Method)selected).invoke(target instanceof Class<?> ? null : target, arguments(selected, args));
        if (member.equals("close") || member.equals("dispose")) {
            synchronized (owned) { owned.remove(target); }
        }
        if (member.equals("create") || member.equals("copyForMaterialChanges")) own(result);
        return encode(result);
    }

    private void own(Object value) {
        if (value == null) return;
        try { value.getClass().getMethod("close"); synchronized (owned) { owned.add(value); } }
        catch (NoSuchMethodException ignored) {}
    }

    private Class<?> resolveClass(String alias) throws ClassNotFoundException {
        String name = TYPES.getOrDefault(alias, alias);
        if (!TYPE_NAMES.contains(name) && !name.startsWith("java.awt.") && !name.startsWith("java.lang.") && !name.startsWith("java.util.") && !name.startsWith("org.mtr.core.")) {
            throw new IllegalArgumentException("Class is not a scripting API: " + name);
        }
        return Class.forName(name);
    }

    private Executable select(List<? extends Executable> choices, JsonArray args, boolean requireStatic) {
        Executable winner = null;
        int best = Integer.MAX_VALUE;
        for (Executable choice : choices) {
            if (choice instanceof Method && requireStatic && !Modifier.isStatic(choice.getModifiers())) continue;
            Class<?>[] types = choice.getParameterTypes();
            if ((!choice.isVarArgs() && types.length != args.size()) || (choice.isVarArgs() && args.size() < types.length - 1)) continue;
            int score = choice.isVarArgs() ? 8 : 0;
            try {
                for (int i = 0; i < args.size(); i++) {
                    Class<?> type = choice.isVarArgs() && !packedVarargs(choice, args) && i >= types.length - 1 ? types[types.length - 1].getComponentType() : types[i];
                    Object decoded = decode(args.get(i));
                    if (decoded == null) { if (type.isPrimitive()) throw new IllegalArgumentException(); score += 2; }
                    else if (decoded instanceof Number && Number.class.isAssignableFrom(box(type))) {
                        convert(args.get(i), type);
                        score += numericCost((Number)decoded, type);
                    }
                    else if (box(type).isInstance(decoded)) score += type == Object.class ? 12 : 0;
                    else { convert(args.get(i), type); score += type == Object.class ? 12 : 3; }
                }
                if (score < best) { winner = choice; best = score; }
            } catch (RuntimeException ignored) {}
        }
        if (winner == null) throw new IllegalArgumentException("No matching public overload for " + choices + " with " + args);
        return winner;
    }

    private static int numericCost(Number value, Class<?> type) {
        boolean integral = value instanceof Long || value instanceof Integer;
        if (integral) {
            boolean fitsInt = value.longValue() >= Integer.MIN_VALUE && value.longValue() <= Integer.MAX_VALUE;
            if (type == int.class || type == Integer.class) return fitsInt ? 0 : 10;
            if (type == long.class || type == Long.class) return fitsInt ? 1 : 0;
            if (type == short.class || type == Short.class || type == byte.class || type == Byte.class) return 2;
            return type == double.class || type == Double.class ? 3 : 4;
        }
        if (type == double.class || type == Double.class) return 0;
        if (type == float.class || type == Float.class) return 1;
        return 8;
    }

    private Object[] arguments(Executable method, JsonArray args) {
        Class<?>[] types = method.getParameterTypes();
        Object[] result = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (method.isVarArgs() && !packedVarargs(method, args) && i == types.length - 1) {
                Object array = Array.newInstance(types[i].getComponentType(), args.size() - i);
                for (int j = i; j < args.size(); j++) Array.set(array, j - i, convert(args.get(j), types[i].getComponentType()));
                result[i] = array;
            } else result[i] = convert(args.get(i), types[i]);
        }
        return result;
    }

    private boolean packedVarargs(Executable method, JsonArray args) {
        if (!method.isVarArgs() || args.size() != method.getParameterCount()) return false;
        JsonElement last = args.get(args.size() - 1);
        if (last.isJsonArray()) return true;
        Object decoded = decode(last);
        return decoded != null && method.getParameterTypes()[args.size() - 1].isInstance(decoded);
    }

    private Object convert(JsonElement json, Class<?> type) {
        Object value = decode(json);
        if (value == null) { if (type.isPrimitive()) throw new IllegalArgumentException("Null primitive"); return null; }
        if (box(type).isInstance(value)) return value;
        if (type == String.class) return String.valueOf(value);
        if (type == JsonObject.class || type == JsonArray.class || type == JsonElement.class) return json;
        if (type.isEnum()) {
            String name = json.isJsonObject() ? json.getAsJsonObject().get("name").getAsString() : json.getAsString();
            return Enum.valueOf((Class)type, name);
        }
        if (type == NativeObject.class && json.isJsonObject()) return nativeObject(json.getAsJsonObject());
        if (json.isJsonObject() && json.getAsJsonObject().has("$callback") && type.isInterface()) return callback(json.getAsJsonObject().getAsJsonObject("$callback"), type);
        if (type.isArray() && json.isJsonArray()) {
            JsonArray items = json.getAsJsonArray();
            Object array = Array.newInstance(type.getComponentType(), items.size());
            for (int i = 0; i < items.size(); i++) Array.set(array, i, convert(items.get(i), type.getComponentType()));
            return array;
        }
        if (value instanceof Number) {
            Number number = (Number)value;
            if (type == int.class || type == Integer.class) return number.intValue();
            if (type == long.class || type == Long.class) return number.longValue();
            if (type == float.class || type == Float.class) return number.floatValue();
            if (type == double.class || type == Double.class) return number.doubleValue();
            if (type == byte.class || type == Byte.class) return number.byteValue();
            if (type == short.class || type == Short.class) return number.shortValue();
        }
        throw new IllegalArgumentException("Cannot convert " + json + " to " + type.getName());
    }

    private NativeObject nativeObject(JsonObject json) {
        NativeObject object = new NativeObject();
        json.entrySet().forEach(e -> object.put(e.getKey(), object, e.getValue().isJsonObject() && !e.getValue().getAsJsonObject().has("$handle") ? nativeObject(e.getValue().getAsJsonObject()) : decode(e.getValue())));
        return object;
    }

    private Object callback(JsonObject callback, Class<?> type) {
        long function = Long.parseUnsignedLong(callback.get("function").getAsString());
        long user = Long.parseUnsignedLong(callback.get("user").getAsString());
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("toString")) return "Native callback";
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
            }
            lifetime.readLock().lock();
            NativeApiSession previous = ACTIVE.get();
            ACTIVE.set(this);
            try {
                if (closed || callbacksStopped) return method.getReturnType().isPrimitive() ? primitiveDefault(method.getReturnType()) : null;
                JsonArray parameters = new JsonArray();
                if (args != null) for (Object arg : args) parameters.add(encode(arg));
                byte[] result = NativeScriptManager.nativeApiCallback(function, user, JSON.toJson(parameters).getBytes(StandardCharsets.UTF_8));
                JsonObject reply = JsonParser.parseString(new String(result, StandardCharsets.UTF_8)).getAsJsonObject();
                if (reply.has("error")) throw new IllegalStateException(reply.get("error").getAsString());
                return method.getReturnType() == void.class ? null : convert(reply.get("value"), method.getReturnType());
            } finally {
                if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
                lifetime.readLock().unlock();
            }
        });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == void.class) return null;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == short.class) return (short)0;
        if (type == byte.class) return (byte)0;
        return 0;
    }

    private static Class<?> box(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private Object decode(JsonElement json) {
        if (json == null || json.isJsonNull()) return null;
        if (json.isJsonPrimitive()) {
            JsonPrimitive value = json.getAsJsonPrimitive();
            if (value.isBoolean()) return value.getAsBoolean();
            if (value.isString()) return value.getAsString();
            String number = value.getAsString();
            return number.contains(".") || number.contains("e") || number.contains("E") ? (Number)Double.valueOf(number) : (Number)Long.valueOf(number);
        }
        if (json.isJsonObject() && json.getAsJsonObject().has("$handle")) {
            Object value = objects.get(json.getAsJsonObject().get("$handle").getAsLong());
            if (value == null) throw new IllegalArgumentException("Expired native object handle");
            return value;
        }
        if (json.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            json.getAsJsonArray().forEach(e -> list.add(decode(e)));
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        json.getAsJsonObject().entrySet().forEach(e -> map.put(e.getKey(), decode(e.getValue())));
        return map;
    }

    private JsonElement encode(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof JsonElement) return (JsonElement)value;
        if (value instanceof Number) return new JsonPrimitive((Number)value);
        if (value instanceof Boolean) return new JsonPrimitive((Boolean)value);
        if (value instanceof String || value instanceof Character) return new JsonPrimitive(value.toString());
        JsonObject result = new JsonObject();
        synchronized (identities) {
            long id = identities.computeIfAbsent(value, k -> NEXT_HANDLE.getAndIncrement());
            objects.put(id, value);
            references.merge(id, 1, Integer::sum);
            result.addProperty("$handle", Long.toString(id));
        }
        if (value instanceof Enum<?>) {
            result.addProperty("$enum", ((Enum<?>)value).getDeclaringClass().getName());
            result.addProperty("name", ((Enum<?>)value).name());
        }
        result.addProperty("$type", value.getClass().getName());
        return result;
    }

    private void release(long id) {
        synchronized (identities) {
            Integer count = references.get(id);
            if (count == null) return;
            if (count > 1) references.put(id, count - 1);
            else { references.remove(id); Object value = objects.remove(id); if (value != null) identities.remove(value); }
        }
    }

    /** Stop callbacks and wait for active native calls before destroying C++ state. */
    public static void quiesce(String key) {
        NativeApiSession session = SESSIONS.get(key);
        if (session == null) return;
        session.callbacksStopped = true;
        session.tasks.forEach(task -> task.cancel(true));
        session.lifetime.writeLock().lock();
        session.lifetime.writeLock().unlock();
    }

    public static void quiesceAll() { new ArrayList<>(SESSIONS.keySet()).forEach(NativeApiSession::quiesce); }

    public static void dispose(String key) {
        quiesce(key);
        NativeApiSession session = SESSIONS.get(key);
        if (session == null) return;
        session.lifetime.writeLock().lock();
        try {
            session.closed = true;
            synchronized (session.owned) {
                for (Object object : session.owned) {
                    try { object.getClass().getMethod("close").invoke(object); }
                    catch (ReflectiveOperationException ignored) {}
                }
                session.owned.clear();
            }
            session.objects.clear(); session.identities.clear(); session.references.clear();
            SESSIONS.remove(key, session);
        } finally { session.lifetime.writeLock().unlock(); }
    }

    public static void reset() { new ArrayList<>(SESSIONS.keySet()).forEach(NativeApiSession::dispose); }

    public static final class Background {
        private final NativeApiSession session;
        private Background(NativeApiSession session) { this.session = session; }
        public void submit(Runnable task) {
            if (session.callbacksStopped || session.closed) throw new IllegalStateException("native API session is stopping");
            session.tasks.removeIf(Future::isDone);
            session.tasks.add(BACKGROUND.submit(task));
        }
    }

    public static final class NativeTiming {
        private double previous;
        private double elapsed;
        private double delta;
        private void step() { elapsed = com.lx862.mtrscripting.core.util.TimingJS.globalElapsed(); delta = elapsed - previous; previous = elapsed; }
        public double elapsed() { return elapsed; }
        public double delta() { return delta; }
        public long currentTimeMillis() { return System.currentTimeMillis(); }
        public long nanoTime() { return System.nanoTime(); }
    }
}
