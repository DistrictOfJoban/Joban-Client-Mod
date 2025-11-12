package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

public class QuadDrawCall extends RenderDrawCall<QuadDrawCall> {
    private final QuadDefinition quadDefinition;
    private Identifier textureId;
    private int color = RenderHelper.ARGB_WHITE;
    private int light = -1;
    private boolean translucent;
    private float u1 = 0;
    private float v1 = 0;
    private float u2 = 1;
    private float v2 = 1;

    public QuadDrawCall() {
        this.textureId = new Identifier("mtr", "textures/block/white.png");
        this.quadDefinition = new QuadDefinition();
    }

    public static QuadDrawCall create() {
        return new QuadDrawCall();
    }

    public static QuadDrawCall create(String comment) {
        return create();
    }

    public QuadDrawCall quad(Vector3dWrapper pos1, Vector3dWrapper pos2, Vector3dWrapper pos3, Vector3dWrapper pos4) {
        this.quadDefinition.x1 = (float)pos1.x();
        this.quadDefinition.y1 = (float)pos1.y();
        this.quadDefinition.z1 = (float)pos1.z();

        this.quadDefinition.x2 = (float)pos2.x();
        this.quadDefinition.y2 = (float)pos2.y();
        this.quadDefinition.z2 = (float)pos2.z();

        this.quadDefinition.x3 = (float)pos3.x();
        this.quadDefinition.y3 = (float)pos3.y();
        this.quadDefinition.z3 = (float)pos3.z();

        this.quadDefinition.x4 = (float)pos4.x();
        this.quadDefinition.y4 = (float)pos4.y();
        this.quadDefinition.z4 = (float)pos4.z();
        return this;
    }

    public QuadDrawCall maxLight() {
        return light(-2);
    }

    public QuadDrawCall light(int light) {
        this.light = light;
        return this;
    }

    public QuadDrawCall corner1(Vector3dWrapper pos) {
        this.quadDefinition.x1 = (float)pos.x();
        this.quadDefinition.y1 = (float)pos.y();
        this.quadDefinition.z1 = (float)pos.z();
        return this;
    }

    public QuadDrawCall corner2(Vector3dWrapper pos) {
        this.quadDefinition.x2 = (float)pos.x();
        this.quadDefinition.y2 = (float)pos.y();
        this.quadDefinition.z2 = (float)pos.z();
        return this;
    }

    public QuadDrawCall corner3(Vector3dWrapper pos) {
        this.quadDefinition.x3 = (float)pos.x();
        this.quadDefinition.y3 = (float)pos.y();
        this.quadDefinition.z3 = (float)pos.z();
        return this;
    }

    public QuadDrawCall corner4(Vector3dWrapper pos) {
        this.quadDefinition.x4 = (float)pos.x();
        this.quadDefinition.y4 = (float)pos.y();
        this.quadDefinition.z4 = (float)pos.z();
        return this;
    }

    public QuadDrawCall uv(float u1, float v1, float u2, float v2) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public QuadDrawCall uv(float u2, float v2) {
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public QuadDrawCall texture(String id) {
        return texture(new Identifier(id));
    }

    public QuadDrawCall texture(Identifier id) {
        this.textureId = id;
        return this;
    }

    public QuadDrawCall color(int color) {
        this.color = color;
        return this;
    }

    public QuadDrawCall translucent() {
        this.translucent = true;
        return this;
    }

    @Override
    public void run(World world, Vector3dWrapper basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolderNew, offset) -> {
            graphicsHolderNew.createVertexConsumer(RenderLayer.getBeaconBeam(textureId, translucent));
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            graphicsHolderNew.drawTextureInWorld(
                    quadDefinition.x1,
                    quadDefinition.y1,
                    quadDefinition.z1,
                    quadDefinition.x2,
                    quadDefinition.y2,
                    quadDefinition.z2,
                    quadDefinition.x3,
                    quadDefinition.y3,
                    quadDefinition.z3,
                    quadDefinition.x4,
                    quadDefinition.y4,
                    quadDefinition.z4,
                    u1,
                    v1,
                    u2,
                    v2,
                    facing,
                    this.color,
                    this.light == -2 ? RenderHelper.MAX_RENDER_LIGHT : this.light == -1 ? light : this.light
            );
            graphicsHolderNew.pop();
        });
    }

    public static class QuadDefinition {
        public float x1;
        public float y1;
        public float z1;

        public float x2;
        public float y2;
        public float z2;

        public float x3;
        public float y3;
        public float z3;

        public float x4;
        public float y4;
        public float z4;

        public QuadDefinition() {
            this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public QuadDefinition(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;

            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;

            this.x3 = x3;
            this.y3 = y3;
            this.z3 = z3;

            this.x4 = x4;
            this.y4 = y4;
            this.z4 = z4;
        }

        public QuadDefinition(Vector3f corner1, Vector3f corner2, Vector3f corner3, Vector3f corner4) {
            this(
                corner1.getX(),
                corner1.getY(),
                corner1.getZ(),
                corner2.getX(),
                corner2.getY(),
                corner2.getZ(),
                corner3.getX(),
                corner3.getY(),
                corner3.getZ(),
                corner4.getX(),
                corner4.getY(),
                corner4.getZ()
            );
        }
    }
}
