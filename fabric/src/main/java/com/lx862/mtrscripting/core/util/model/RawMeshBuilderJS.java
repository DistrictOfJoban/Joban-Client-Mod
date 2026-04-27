package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mixin.modded.mtr.VertexAttributeStateAccessor;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Vector3f;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.model.Face;
import org.mtr.mapping.render.model.RawMesh;
import org.mtr.mapping.render.model.RawModel;
import org.mtr.mapping.render.vertex.Vertex;

import java.util.stream.IntStream;

public class RawMeshBuilderJS {

    private final RawMesh mesh;

    private final int faceSize;
    private Vertex buildingVertex = new Vertex();

    public RawMeshBuilderJS(int faceSize, String renderType, Identifier texture) {
        this.faceSize = faceSize;
        this.mesh = new RawMesh(new MaterialProperties(RawModelJS.stringToRenderStage(renderType).shaderType, texture, 0xFFFFFFFF));
    }

    public RawMesh getMesh() {
        return mesh;
    }

    public RawModel asRawModel() {
        RawModel rawModel = new RawModel();
        rawModel.append(getMesh());
        return rawModel;
    }

    public RawMeshBuilderJS reset() {
        mesh.vertices.clear();
        mesh.faces.clear();
        setNewDefaultVertex();
        return this;
    }

    public RawMeshBuilderJS vertex(ScriptVector3f position) {
        float x = position.x();
        float y = position.x();
        float z = position.x();
        buildingVertex.position = new Vector3f(x, -y, -z);
        return this;
    }

    public RawMeshBuilderJS vertex(double d, double e, double f) {
        buildingVertex.position = new Vector3f((float) d, (float) -e, (float) -f);
        return this;
    }

    public RawMeshBuilderJS normal(float f, float g, float h) {
        buildingVertex.normal = new Vector3f(f, -g, -h);
        return this;
    }

    public RawMeshBuilderJS color(int r, int g, int b, int a) {
        ((VertexAttributeStateAccessor)(Object)mesh.materialProperties.vertexAttributeState).setColor((r << 24) + (g << 16) + (b << 8) + a);
        return this;
    }

    public RawMeshBuilderJS uv(float f, float g) {
        buildingVertex.u = f;
        buildingVertex.v = g;
        return this;
    }

    public RawMeshBuilderJS endVertex() {
        mesh.vertices.add(buildingVertex);
        setNewDefaultVertex();
        if (mesh.vertices.size() % faceSize == 0) {
            mesh.faces.addAll(Face.triangulate(IntStream.range(mesh.vertices.size() - faceSize, mesh.vertices.size()).toArray()));
        }
        return this;
    }

    @ApiInternal
    private void setNewDefaultVertex() {
        buildingVertex = new Vertex();
        buildingVertex.position = new Vector3f(0, 0, 0);
        buildingVertex.normal = new Vector3f(0, 0, 0);
        buildingVertex.u = 0;
        buildingVertex.v = 0;
    }
}
