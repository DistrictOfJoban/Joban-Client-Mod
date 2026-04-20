package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mixin.modded.mtr.ObjModelAccessor;
import com.lx862.jcm.mixin.modded.mtr.RawMeshAccessor;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.model.RawMesh;
import org.mtr.mapping.render.model.RawModel;
import org.mtr.mod.resource.RenderStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RawModelJS {
    public final RawModel rawModel;
    public String renderType;

    public RawModelJS() {
        this.rawModel = new RawModel();
        this.renderType = "EXTERIOR";
    }

    public RawModelJS(OptimizedModel.ObjModel... rawModel) {
        this();
        for(OptimizedModel.ObjModel model : rawModel) {
            ObjModelAccessor accessor = ((ObjModelAccessor) (Object) model);
            for(RawMesh mesh : accessor.getRawMeshes()) {
                this.rawModel.append(mesh);
            }
        }
    }

    public RawModelJS(RawModel model) {
        this.rawModel = model;
    }

    public void append(RawModelJS other) {
        this.rawModel.append(other.rawModel);
    }

    public void append(RawMesh mesh) {
        this.rawModel.append(mesh);
    }

    public void applyTranslation(float x, float y, float z) {
        this.rawModel.applyTranslation(x, -y, -z);
    }

    public void applyRotation(ScriptVector3f vector3f, float angle) {
        this.rawModel.applyRotation(vector3f.rawVector3f(), angle);
    }

    public void applyScale(float x, float y, float z) {
        this.rawModel.applyScale(x, y, z);
    }

    public void applyMirror(boolean x, boolean y, boolean z) {
        this.rawModel.applyMirror(x, y, z, x, y, z);
    }

    public void applyMirror(boolean x, boolean y, boolean z, boolean nx, boolean ny, boolean nz) {
        this.rawModel.applyMirror(x, y, z, nx, ny, nz);
    }

    public void applyUVMirror(boolean u, boolean v) {
        this.rawModel.applyUVMirror(u, v);
    }

    public RawModelJS copy() {
        RawModel newModel = new RawModel();

        rawModel.iterateRawMeshList(f -> {
            RawMesh newMesh = new RawMesh(f.materialProperties.shaderType, f);
            newModel.append(newMesh);
        });

        return new RawModelJS(newModel);
    }

    public RawModelJS copyForMaterialChanges() {
        RawModel newModel = new RawModel();
        List<RawMesh> meshes = new ArrayList<>();
        rawModel.iterateRawMeshList(f -> {
            MaterialProperties newProp = new MaterialProperties(f.materialProperties.shaderType, f.materialProperties.getTexture(), f.materialProperties.vertexAttributeState.color);
            RawMesh newMesh = new RawMesh(newProp);
            /* Change the list reference */
            ((RawMeshAccessor)(Object)newMesh).setVertices(f.vertices);
            ((RawMeshAccessor)(Object)newMesh).setFaces(f.faces);

            newModel.append(newMesh);
        });

        return new RawModelJS(newModel);
    }

    public void replaceTexture(String oldTexture, Identifier newTexture) {
        rawModel.iterateRawMeshList(f -> {
            Identifier id = f.materialProperties.getTexture();
            String oldPath = id.getPath();
            if (oldPath.substring(oldPath.lastIndexOf("/") + 1).equals(oldTexture)) {
                f.materialProperties.setTexture(newTexture);
            }
        });
    }

    public void triangulate() {
        this.rawModel.triangulate();
    }

    public void distinct() {
        this.rawModel.distinct();
    }

    public void generateNormals() {
        this.rawModel.generateNormals();
    }

    public void replaceAllTexture(Identifier id) {
        rawModel.iterateRawMeshList(f -> {
            f.materialProperties.setTexture(id);
        });
    }

    public void setAllRenderType(String renderType) {
        rawModel.iterateRawMeshList(rawMesh -> {
            MaterialProperties originalProp = rawMesh.materialProperties;
            ((RawMeshAccessor)(Object)rawMesh).setMaterialProperties(new MaterialProperties(stringToRenderStage(renderType).shaderType, originalProp.getTexture(), originalProp.vertexAttributeState.color));
        });

        this.renderType = renderType;
    }

    @ApiInternal
    public static RenderStage stringToRenderStage(String s) {
        s = s
                .replace("exteriortranslucent", "interior_translucent")
                .replace("interiortranslucent", "interior_translucent")
                .replace("lighttranslucent", "always_on_light")
                .toUpperCase(Locale.ROOT);
        return RenderStage.valueOf(s);
    }
}