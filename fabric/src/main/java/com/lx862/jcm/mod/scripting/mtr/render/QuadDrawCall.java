package com.lx862.jcm.mod.scripting.mtr.render;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.MoreRenderLayers;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

public class QuadDrawCall extends RenderDrawCall<QuadDrawCall> {
    private static final Identifier WHITE_TEXTURE = new Identifier("mtr", "textures/block/white.png");
    private final QuadDefinition quadDefinition;
    private Identifier textureId;
    private QueuedRenderLayer renderType;
    private int color = RenderHelper.ARGB_WHITE;
    private float u1 = 0;
    private float v1 = 0;
    private float u2 = 1;
    private float v2 = 1;

    public QuadDrawCall() {
        this.textureId = WHITE_TEXTURE;
        this.quadDefinition = new QuadDefinition();
        this.renderType = QueuedRenderLayer.LIGHT;
    }

    public static QuadDrawCall create() {
        return new QuadDrawCall();
    }

    public static QuadDrawCall create(String comment) {
        return create();
    }

    public QuadDrawCall quad(ScriptVector3f pos1, ScriptVector3f pos2, ScriptVector3f pos3, ScriptVector3f pos4) {
        corner1(pos1);
        corner2(pos2);
        corner3(pos3);
        corner4(pos4);
        return this;
    }

    public QuadDrawCall corner1(ScriptVector3f pos) {
        this.quadDefinition.setPair1(pos);
        return this;
    }

    public QuadDrawCall corner2(ScriptVector3f pos) {
        this.quadDefinition.setPair2(pos);
        return this;
    }

    public QuadDrawCall corner3(ScriptVector3f pos) {
        this.quadDefinition.setPair3(pos);
        return this;
    }

    public QuadDrawCall corner4(ScriptVector3f pos) {
        this.quadDefinition.setPair4(pos);
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

    public QuadDrawCall renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    public QuadDrawCall copy() {
        QuadDrawCall copy = new QuadDrawCall()
                .texture(this.textureId)
                .corner1(new ScriptVector3f(this.quadDefinition.x1, this.quadDefinition.y1, this.quadDefinition.z1))
                .corner2(new ScriptVector3f(this.quadDefinition.x2, this.quadDefinition.y2, this.quadDefinition.z2))
                .corner3(new ScriptVector3f(this.quadDefinition.x3, this.quadDefinition.y3, this.quadDefinition.z3))
                .corner4(new ScriptVector3f(this.quadDefinition.x4, this.quadDefinition.y4, this.quadDefinition.z4))
                .color(this.color)
                .uv(this.u1, this.v1, this.u2, this.v2)
                .renderType(this.renderType.toString());
        return copy;
    }

    @Override
    public void run(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        super.run(world, graphicsHolder, storedMatrixTransformations, facing, light);

        MainRenderer.scheduleRender(renderType, (graphicsHolderNew, offset) -> {
            graphicsHolderNew.createVertexConsumer(getRenderType(textureId));
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            graphicsHolderNew.drawTextureInWorld(
                    quadDefinition.x2,
                    quadDefinition.y2,
                    quadDefinition.z2,
                    quadDefinition.x3,
                    quadDefinition.y3,
                    quadDefinition.z3,
                    quadDefinition.x4,
                    quadDefinition.y4,
                    quadDefinition.z4,
                    quadDefinition.x1,
                    quadDefinition.y1,
                    quadDefinition.z1,
                    u1,
                    v1,
                    u2,
                    v2,
                    facing,
                    this.color,
                    light
            );
            graphicsHolderNew.pop();
        });
    }

    private RenderLayer getRenderType(Identifier textureId) {
        if(this.renderType == QueuedRenderLayer.EXTERIOR) {
            return MoreRenderLayers.getExterior(textureId);
        } else if(this.renderType == QueuedRenderLayer.EXTERIOR_TRANSLUCENT) {
            return MoreRenderLayers.getExteriorTranslucent(textureId);
        } else if(this.renderType == QueuedRenderLayer.INTERIOR) {
            return MoreRenderLayers.getInterior(textureId);
        } else if(this.renderType == QueuedRenderLayer.INTERIOR_TRANSLUCENT) {
            return MoreRenderLayers.getInteriorTranslucent(textureId);
        } else if(this.renderType == QueuedRenderLayer.LIGHT) {
            return MoreRenderLayers.getLight(textureId, false);
        } else if(this.renderType == QueuedRenderLayer.LIGHT_TRANSLUCENT) {
            return MoreRenderLayers.getLight(textureId, true);
        } else if(this.renderType == QueuedRenderLayer.LIGHT_2) {
            return MoreRenderLayers.getLight2(textureId);
        }
        throw new IllegalStateException("Unknown render type: " + this.renderType);
    }

    @Override
    public void validate() {
        if(!this.quadDefinition.isValid()) {
            throw new IllegalStateException("Quad definition must have the position of the 4 vertices specified!");
        }
    }

    public static class QuadDefinition {
        private float x1;
        private float y1;
        private float z1;

        private float x2;
        private float y2;
        private float z2;

        private float x3;
        private float y3;
        private float z3;

        private float x4;
        private float y4;
        private float z4;

        private boolean pair1Initialized;
        private boolean pair2Initialized;
        private boolean pair3Initialized;
        private boolean pair4Initialized;

        public QuadDefinition() {
        }

        public void setPair1(ScriptVector3f pos) {
            this.x1 = pos.x();
            this.y1 = pos.y();
            this.z1 = pos.z();
            pair1Initialized = true;
        }

        public void setPair2(ScriptVector3f pos) {
            this.x2 = pos.x();
            this.y2 = pos.y();
            this.z2 = pos.z();
            pair2Initialized = true;
        }

        public void setPair3(ScriptVector3f pos) {
            this.x3 = pos.x();
            this.y3 = pos.y();
            this.z3 = pos.z();
            pair3Initialized = true;
        }

        public void setPair4(ScriptVector3f pos) {
            this.x4 = pos.x();
            this.y4 = pos.y();
            this.z4 = pos.z();
            pair4Initialized = true;
        }

        public boolean isValid() {
            return pair1Initialized && pair2Initialized && pair3Initialized && pair4Initialized;
        }
    }
}
