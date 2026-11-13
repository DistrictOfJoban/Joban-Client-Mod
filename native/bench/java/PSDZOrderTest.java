import com.lx862.jcm.mod.scripting.pids.PIDSDrawCall;
import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.jcm.mod.scripting.psd.PSDScriptContext;
import com.lx862.mtrscripting.core.util.Matrices;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public final class PSDZOrderTest {
    private static final double EPS = 1e-8;

    private static final class Probe extends PIDSDrawCall<Probe> {
        StoredMatrixTransformations queued;
        Probe() { super(10, 10); }
        public void validate() {}
        protected void drawTransformed(StoredMatrixTransformations matrix, Direction facing, int light) { queued=matrix; }
        int persistentSize() { return storedMatrixTransformations == null ? 0 : storedMatrixTransformations.size(); }
        double[] replay() {
            run(null, new StoredMatrixTransformations(), Direction.NORTH, 0);
            GraphicsHolder graphics = new GraphicsHolder();
            queued.transform(graphics, new Vector3d(0, 0, 0));
            return graphics.point(0, 0, 0);
        }
    }

    private static void close(double expected, double actual, String label) {
        if (Math.abs(expected-actual)>EPS) throw new AssertionError(label+": expected "+expected+", got "+actual);
    }

    private static void compare(double[] expected, double[] actual, String label) {
        for(int i=0;i<3;i++) close(expected[i],actual[i],label+" axis "+i);
    }

    private static void userTransforms(GraphicsHolder graphics) {
        graphics.translate(1, 2, 3);
        graphics.rotateYDegrees(30);
        graphics.scale(1.5F, 2, 0.75F);
    }

    public static void main(String[] args) {
        PSDScriptContext context = new PSDScriptContext("regression");
        Probe background = new Probe(), text = new Probe(), foreground = new Probe();
        context.draw(background);
        context.draw(text);
        context.draw(foreground);
        close(0, background.replay()[2], "background depth");
        close(-0.0002, text.replay()[2], "text in front of backing");
        close(-0.0004, foreground.replay()[2], "later item nearest platform");

        // A persistent JS wrapper does not call resetDefaults between frames.
        for (int frame=0;frame<1000;frame++) {
            context.resetForNextRun();
            context.draw(background);
            context.draw(text);
            close(-0.0002, text.replay()[2], "persistent wrapper frame "+frame);
            if(text.persistentSize()!=0) throw new AssertionError("auto-z polluted persistent matrices");
        }

        StoredMatrixTransformations input = new StoredMatrixTransformations();
        input.add(PSDZOrderTest::userTransforms);
        Probe transformed = new Probe().matrices(new Matrices(input)).pos(12, 24).zOrder(3);
        PIDSScriptContext pids = new PIDSScriptContext("ordinary PIDS");
        for (int frame=0;frame<100;frame++) {
            pids.resetForNextRun();
            pids.draw(transformed);
            GraphicsHolder oldSingleDraw = new GraphicsHolder();
            oldSingleDraw.rotateXDegrees(180);
            userTransforms(oldSingleDraw);
            oldSingleDraw.translate(0, 0, 3 * 0.0002);
            oldSingleDraw.rotateXDegrees(-180);
            oldSingleDraw.scale(1/96F, 1/96F, 1/96F);
            oldSingleDraw.translate(12, 24, 0);
            compare(oldSingleDraw.point(0, 0, 0), transformed.replay(), "user matrices preserve previous single-draw semantics");
            if(transformed.persistentSize()!=1) throw new AssertionError("persistent matrices grew");
        }

        // Custom negative steps retain their intentional reverse-order meaning.
        text.incrementZOrder(2, -0.001);
        close(0.002, text.replay()[2], "custom step sign");
        text.resetDefaults(10, 10);
        close(0, text.replay()[2], "pool reset clears auto-z");
        text.zOrder(7).incrementZOrder(1, 0.0002);
        close(-0.0014, text.replay()[2], "explicit zOrder wins");
        System.out.println("PSD/PIDS z-order: front-to-back layering, 1000 persistent frames, user matrices, custom steps and pool reset passed");
    }
}
