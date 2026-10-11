package com.lx862.jcm.nativeapi;

import com.google.gson.*;
import com.lx862.mtrscripting.core.util.GraphicsTexture;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.core.util.model.*;
import org.mtr.mapping.holder.Identifier;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.*;

/** The documented DisplayHelper, accessible to C++ without loading JavaScript. */
public final class NativeDisplayHelper implements AutoCloseable {
    private final JsonObject config;
    private final ModelJS base;
    private final Map<String, AffineTransform> slots = new HashMap<>();
    private boolean ownsTexture;
    private boolean ownsBase;
    private boolean closed;
    public GraphicsTexture texture;
    public ModelJS model;

    public NativeDisplayHelper(JsonObject config) {
        if (config.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unknown DisplayHelper version");
        this.config = new JsonParser().parse(config.toString()).getAsJsonObject();
        JsonArray size = config.getAsJsonArray("texSize");
        String renderType = config.has("renderType") ? config.get("renderType").getAsString() : "interior";
        RawMeshBuilderJS mesh = new RawMeshBuilderJS(4, renderType, new Identifier("minecraft:textures/misc/white.png"));
        for (JsonElement entry : config.getAsJsonArray("slots")) {
            JsonObject slot = entry.getAsJsonObject();
            JsonArray area = slot.getAsJsonArray("texArea");
            float u1 = area.get(0).getAsFloat() / size.get(0).getAsFloat();
            float v1 = area.get(1).getAsFloat() / size.get(1).getAsFloat();
            float u2 = (area.get(0).getAsFloat() + area.get(2).getAsFloat()) / size.get(0).getAsFloat();
            float v2 = (area.get(1).getAsFloat() + area.get(3).getAsFloat()) / size.get(1).getAsFloat();
            float[][] uv = {{u1,v1},{u1,v2},{u2,v2},{u2,v1}};
            JsonArray offsets = slot.has("offsets") ? slot.getAsJsonArray("offsets") : new JsonParser().parse("[[0,0,0]]").getAsJsonArray();
            for (JsonElement offset : offsets) for (JsonElement polygon : slot.getAsJsonArray("pos")) {
                for (int i = 0; i < 4; i++) {
                    JsonArray vertex = polygon.getAsJsonArray().get(i).getAsJsonArray();
                    JsonArray delta = offset.getAsJsonArray();
                    mesh.vertex(vertex.get(0).getAsDouble() + delta.get(0).getAsDouble(), vertex.get(1).getAsDouble() + delta.get(1).getAsDouble(), vertex.get(2).getAsDouble() + delta.get(2).getAsDouble())
                            .normal(0,1,0).uv(uv[i][0],uv[i][1]).endVertex();
                }
            }
        }
        RawModelJS raw = mesh.asRawModel();
        raw.triangulate();
        base = ModelJS.uploadRawModel(raw);
        ownsBase = true;
    }

    private NativeDisplayHelper(NativeDisplayHelper template, GraphicsTexture sharedTexture) {
        config = template.config;
        base = template.base;
        JsonArray size = config.getAsJsonArray("texSize");
        ownsTexture = sharedTexture == null;
        texture = sharedTexture == null ? new GraphicsTexture(size.get(0).getAsInt(), size.get(1).getAsInt()) : sharedTexture;
        model = base.copyForMaterialChanges();
        model.replaceAllTexture(texture.identifier);
        for (JsonElement entry : config.getAsJsonArray("slots")) {
            JsonObject slot = entry.getAsJsonObject();
            JsonArray area = slot.getAsJsonArray("texArea");
            AffineTransform transform = AffineTransform.getTranslateInstance(area.get(0).getAsDouble(), area.get(1).getAsDouble());
            if (slot.has("paintingSize")) {
                JsonArray paint = slot.getAsJsonArray("paintingSize");
                transform.scale(area.get(2).getAsDouble() / paint.get(0).getAsDouble(), area.get(3).getAsDouble() / paint.get(1).getAsDouble());
            }
            slots.put(slot.get("name").getAsString(), transform);
        }
    }

    public NativeDisplayHelper create() { return create(null); }
    public NativeDisplayHelper create(GraphicsTexture sharedTexture) { return new NativeDisplayHelper(this, sharedTexture); }
    public Graphics2D graphics() { texture.graphics.setTransform(new AffineTransform()); return texture.graphics; }
    public Graphics2D graphicsFor(String slotName) {
        AffineTransform transform = slots.get(slotName);
        if (transform == null) throw new IllegalArgumentException("Unknown DisplayHelper slot " + slotName);
        texture.graphics.setTransform(transform);
        return texture.graphics;
    }
    public void upload() { if (ownsTexture) texture.upload(); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        if (ownsTexture && texture != null) texture.close();
        if (model != null) model.close();
        if (ownsBase) base.close();
    }
}
