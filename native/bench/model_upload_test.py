"""Run production ModelJS/RawMeshBuilderJS against MTR's real GPU upload classes.

A hidden GLFW context uses real LWJGL. Only Minecraft services, material lookup
and mixin accessor implementations are substituted; RawModel, RawMesh, Face,
VertexArray, VertexBuffer, IndexBuffer and GlStateTracker come from --mtr-jar.
"""
import argparse
import os
from pathlib import Path
import platform
import subprocess
import urllib.request

STUBS = {
    'net/minecraft/class_5944.java': 'package net.minecraft; public class class_5944 {}',
    'com/mojang/blaze3d/systems/RenderSystem.java': '''package com.mojang.blaze3d.systems;
public class RenderSystem {
 public static net.minecraft.class_5944 getShader() { return null; }
 public static void setShader(java.util.function.Supplier<net.minecraft.class_5944> s) {}
 public static void enableCull() { org.lwjgl.opengl.GL33.glEnable(org.lwjgl.opengl.GL33.GL_CULL_FACE); }
 public static void depthMask(boolean b) { org.lwjgl.opengl.GL33.glDepthMask(b); }
 public static boolean isOnRenderThread() { return true; }
 public static void recordRenderCall(net.minecraft.class_4573 r) { r.execute(); }
}''',
    'net/minecraft/class_4573.java': 'package net.minecraft; public interface class_4573 { void execute(); }',
    'org/mtr/mapping/tool/DummyClass.java': '''package org.mtr.mapping.tool;
public class DummyClass { public static void logException(Exception e) {} }''',
    'org/mtr/mapping/holder/Identifier.java': '''package org.mtr.mapping.holder;
public record Identifier(String value) { public String getPath() { return value; } }''',
    'org/mtr/mapping/render/batch/MaterialProperties.java': '''package org.mtr.mapping.render.batch;
public class MaterialProperties {
 public final org.mtr.mapping.mapper.OptimizedModel.ShaderType shaderType;
 public final org.mtr.mapping.render.vertex.VertexAttributeState vertexAttributeState;
 private org.mtr.mapping.holder.Identifier texture;
 public MaterialProperties(org.mtr.mapping.mapper.OptimizedModel.ShaderType s,org.mtr.mapping.holder.Identifier t,Integer c) {
  shaderType=s; texture=t; vertexAttributeState=new org.mtr.mapping.render.vertex.VertexAttributeState(c,null);
 }
 public org.mtr.mapping.holder.Identifier getTexture() { return texture; }
 public void setTexture(org.mtr.mapping.holder.Identifier t) { texture=t; }
}''',
    'org/mtr/mapping/mapper/OptimizedModel.java': '''package org.mtr.mapping.mapper;
public class OptimizedModel implements com.lx862.jcm.mixin.mtrscripting.OptimizedModelAccessor {
 public enum ShaderType { CUTOUT, CUTOUT_BRIGHT, CUTOUT_GLOWING, TRANSLUCENT, TRANSLUCENT_BRIGHT, TRANSLUCENT_GLOWING }
 public final java.util.List<org.mtr.mapping.render.object.VertexArray> parts;
 public OptimizedModel(java.util.List<org.mtr.mapping.render.object.VertexArray> p) { parts=p; }
 public java.util.List<org.mtr.mapping.render.object.VertexArray> getUploadedParts() { return parts; }
}''',
    'com/lx862/jcm/mixin/mtrscripting/OptimizedModelAccessor.java': '''package com.lx862.jcm.mixin.mtrscripting;
public interface OptimizedModelAccessor {
 static org.mtr.mapping.render.vertex.VertexAttributeMapping getDefaultMapping() {
  var builder=new org.mtr.mapping.render.vertex.VertexAttributeMapping.Builder();
  for(var type:org.mtr.mapping.render.vertex.VertexAttributeType.values())
    builder.set(type,org.mtr.mapping.render.vertex.VertexAttributeSource.GLOBAL);
  for(var type:java.util.List.of(org.mtr.mapping.render.vertex.VertexAttributeType.POSITION,
    org.mtr.mapping.render.vertex.VertexAttributeType.UV_TEXTURE,org.mtr.mapping.render.vertex.VertexAttributeType.NORMAL))
    builder.set(type,org.mtr.mapping.render.vertex.VertexAttributeSource.VERTEX_BUFFER);
  return builder.build();
 }
 java.util.List<org.mtr.mapping.render.object.VertexArray> getUploadedParts();
}''',
    'com/lx862/jcm/mixin/mtrscripting/GlStateTrackerAccessor.java': '''package com.lx862.jcm.mixin.mtrscripting;
public interface GlStateTrackerAccessor {
 static boolean isStateProtected() {
  try { var f=org.mtr.mapping.render.tool.GlStateTracker.class.getDeclaredField("isStateProtected"); f.setAccessible(true); return f.getBoolean(null); }
  catch(ReflectiveOperationException e) { throw new AssertionError(e); }
 }
}''',
    'com/lx862/jcm/mixin/mtrscripting/OptimizedModelWrapperAccessor.java': '''package com.lx862.jcm.mixin.mtrscripting;
public interface OptimizedModelWrapperAccessor {
 static org.mtr.mod.resource.OptimizedModelWrapper createNew(org.mtr.mapping.mapper.OptimizedModel model) { return new org.mtr.mod.resource.OptimizedModelWrapper(model); }
 org.mtr.mapping.mapper.OptimizedModel getOptimizedModel();
}''',
    'org/mtr/mod/resource/OptimizedModelWrapper.java': '''package org.mtr.mod.resource;
public class OptimizedModelWrapper implements com.lx862.jcm.mixin.mtrscripting.OptimizedModelWrapperAccessor {
 private final org.mtr.mapping.mapper.OptimizedModel model;
 public OptimizedModelWrapper(org.mtr.mapping.mapper.OptimizedModel m) { model=m; }
 public org.mtr.mapping.mapper.OptimizedModel getOptimizedModel() { return model; }
}''',
    'org/mtr/mapping/mapper/OptimizedRenderer.java': 'package org.mtr.mapping.mapper; public class OptimizedRenderer { public static boolean hasOptimizedRendering() { return true; } }',
    'org/mtr/mod/client/CustomResourceLoader.java': '''package org.mtr.mod.client;
public class CustomResourceLoader {
 public static final CustomResourceLoader OPTIMIZED_RENDERER_WRAPPER=new CustomResourceLoader();
 public void queue(org.mtr.mod.resource.OptimizedModelWrapper m,org.mtr.mod.render.StoredMatrixTransformations.Graphics g,int light) {}
}''',
    'org/mtr/mod/render/StoredMatrixTransformations.java': '''package org.mtr.mod.render;
public class StoredMatrixTransformations {
 public static class Graphics { public void pop() {} }
 public void transform(Graphics g,Object o) {}
}''',
    'org/mtr/mod/render/MainRenderer.java': '''package org.mtr.mod.render;
public class MainRenderer { public static void scheduleRender(QueuedRenderLayer l,java.util.function.BiConsumer<StoredMatrixTransformations.Graphics,Object> c) {} }''',
    'org/mtr/mod/render/QueuedRenderLayer.java': 'package org.mtr.mod.render; public enum QueuedRenderLayer { TEXT }',
    'com/lx862/mtrscripting/core/annotation/ApiInternal.java': 'package com.lx862.mtrscripting.core.annotation; public @interface ApiInternal {}',
    'com/lx862/mtrscripting/core/util/ScriptVector3f.java': '''package com.lx862.mtrscripting.core.util;
public record ScriptVector3f(float x,float y,float z) {}''',
    'com/lx862/jcm/mixin/mtrscripting/VertexAttributeStateAccessor.java': '''package com.lx862.jcm.mixin.mtrscripting;
public interface VertexAttributeStateAccessor { void setColor(int c); }''',
    'com/lx862/mtrscripting/core/util/model/RawModelJS.java': '''package com.lx862.mtrscripting.core.util.model;
public class RawModelJS {
 public final org.mtr.mapping.render.model.RawModel rawModel;
 public RawModelJS(org.mtr.mapping.render.model.RawModel m) { rawModel=m; }
 public static org.mtr.mod.resource.RenderStage stringToRenderStage(String s) { return org.mtr.mod.resource.RenderStage.valueOf(s.toUpperCase(java.util.Locale.ROOT)); }
}''',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mtr-jar', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--model-source', type=Path)
    parser.add_argument('--expect-unprotected-failure', action='store_true')
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    native = 'windows' if os.name == 'nt' else ('macos' if platform.system() == 'Darwin' else 'linux')
    if platform.machine().lower() in ('arm64', 'aarch64'):
        native += '-arm64'
    dependencies = []
    for module in ('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl'):
        for suffix in ('', '-natives-' + native):
            name = module + '-3.3.1' + suffix + '.jar'
            path = work / name
            if not path.exists():
                urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/lwjgl/' + module + '/3.3.1/' + name, path)
            dependencies.append(path)
    joml = work / 'joml-1.10.5.jar'
    if not joml.exists():
        urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/' + joml.name, joml)
    dependencies.extend((joml, args.mtr_jar.resolve()))
    sources = []
    for name, content in STUBS.items():
        path = work / 'src' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        sources.append(path)
    base = repo / 'fabric/src/main/java/com/lx862/mtrscripting/core/util/model'
    sources.extend((args.model_source or base / 'ModelJS.java', base / 'RawMeshBuilderJS.java',
                    Path(__file__).with_name('jvm') / 'ModelUploadTest.java'))
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    def java_tool(name):
        return str(Path(args.java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if args.java_home else name
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + str(p).replace('\\', '/') + '"' for p in sources), encoding='utf-8')
    cp = os.pathsep.join(map(str, dependencies))
    subprocess.run([java_tool('javac'), '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), '@' + str(argfile)], check=True)
    mode = 'old' if args.expect_unprotected_failure else 'fixed'
    subprocess.run([java_tool('java'), '-cp', str(classes) + os.pathsep + cp,
                    'com.lx862.mtrscripting.core.util.model.ModelUploadTest', mode], check=True)


if __name__ == '__main__':
    main()
