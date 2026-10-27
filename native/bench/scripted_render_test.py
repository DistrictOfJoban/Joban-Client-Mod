"""Flush-time semantics regression for the scripted render replay path.

Compiles the PRODUCTION ScriptRenderManager / RenderDrawCall /
TransformCapturePool / NativeTransformScope sources against stubs that
replicate MTR 4.0.5's StoredMatrixTransformations semantics (verified
against the decompiled shipped jar: transform() pushes, applies
translate(initialTranslate - offset) when the holder was built with the
4-arg world-anchor constructor, then every consumer in order, and does NOT
pop — the caller balances the push; the consumer list is iterated live at
flush time). The test drives the real capture/replay lifecycle and asserts
that each draw call's per-call transforms (PIDS BASE_SCALE + position,
.matrices() rotate sandwich) survive until the deferred callback reads
them, that draw calls stay isolated from each other, that the shared base
is never mutated, that the base's world anchor survives into the pooled
snapshot and is re-anchored on every recycled handout (dropping it is what
made scripted PIDS text follow the camera), and that the pool reaches
steady state. It also embeds the yanyang.4..yanyang.8 mark/truncate replay
and asserts it demonstrably LOSES those transforms — that loss is the
jsblock:pids_1a oversized-text regression this test exists to guard.

Pure JVM: no native bridge, no Minecraft dependencies.
"""
import argparse
import os
from pathlib import Path
import subprocess

STUBS = {
    # Recording graphics holder: every matrix operation becomes a typed Op.
    'org/mtr/mapping/mapper/GraphicsHolder.java': '''package org.mtr.mapping.mapper;
import java.util.ArrayList;
import java.util.List;
public class GraphicsHolder {
    public static final class Op {
        public final String type; public final double x, y, z;
        public Op(String type, double x, double y, double z) { this.type = type; this.x = x; this.y = y; this.z = z; }
    }
    public final List<Op> ops = new ArrayList<>();
    public void push() { ops.add(new Op("push", 0, 0, 0)); }
    public void pop() { ops.add(new Op("pop", 0, 0, 0)); }
    public void translate(double x, double y, double z) { ops.add(new Op("translate", x, y, z)); }
    public void scale(float x, float y, float z) { ops.add(new Op("scale", x, y, z)); }
    public void rotateXDegrees(float d) { ops.add(new Op("rotateX", d, 0, 0)); }
    public void rotateYDegrees(float d) { ops.add(new Op("rotateY", d, 0, 0)); }
    public void rotateZDegrees(float d) { ops.add(new Op("rotateZ", d, 0, 0)); }
}''',
    # Byte-for-byte semantics of MTR 4.0.5's StoredMatrixTransformations,
    # decompiled from the shipped jar (transform = push + conditional
    # translate(initialTranslate - offset) when useDefaultOffset is set +
    # forEach consumers, NO pop; add(SMT) append-merges the other list's
    # consumers but does NOT carry the anchor — only copy() does). The
    # jcm$* methods stand in for what StoredMatrixTransformationsMixin adds
    # to the real class at runtime; jcm$copyWorldAnchorFrom mirrors the
    # mixin's @Mutable field writes (the stub keeps the anchor fields
    # non-final because plain Java has no PUTFIELD bypass).
    'org/mtr/mod/render/StoredMatrixTransformations.java': '''package org.mtr.mod.render;
import com.lx862.jcm.nativeapi.NativeTransformScope;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public class StoredMatrixTransformations implements NativeTransformScope {
    final List<Consumer<GraphicsHolder>> transformations = new ArrayList<>();
    boolean useDefaultOffset;
    double initialTranslateX, initialTranslateY, initialTranslateZ;
    public StoredMatrixTransformations() { useDefaultOffset = false; }
    public StoredMatrixTransformations(double x, double y, double z) {
        useDefaultOffset = true;
        initialTranslateX = x; initialTranslateY = y; initialTranslateZ = z;
    }
    private StoredMatrixTransformations(boolean anchored, double x, double y, double z) {
        useDefaultOffset = anchored;
        initialTranslateX = x; initialTranslateY = y; initialTranslateZ = z;
    }
    public void add(Consumer<GraphicsHolder> consumer) { transformations.add(consumer); }
    public void add(StoredMatrixTransformations other) { transformations.addAll(other.transformations); }
    public void transform(GraphicsHolder graphicsHolder, Vector3d offset) {
        graphicsHolder.push();
        if (useDefaultOffset) {
            graphicsHolder.translate(initialTranslateX - offset.getXMapped(),
                    initialTranslateY - offset.getYMapped(),
                    initialTranslateZ - offset.getZMapped());
        }
        transformations.forEach(consumer -> consumer.accept(graphicsHolder));
    }
    public StoredMatrixTransformations copy() {
        StoredMatrixTransformations s = new StoredMatrixTransformations(useDefaultOffset, initialTranslateX, initialTranslateY, initialTranslateZ);
        s.transformations.addAll(transformations);
        return s;
    }
    public int size() { return transformations.size(); }
    public int jcm$mark() { return transformations.size(); }
    public void jcm$truncate(int mark) {
        if (mark >= 0 && transformations.size() > mark) transformations.subList(mark, transformations.size()).clear();
    }
    public void jcm$clearTransformations() { transformations.clear(); }
    public void jcm$copyWorldAnchorFrom(StoredMatrixTransformations base) {
        this.useDefaultOffset = base.useDefaultOffset;
        this.initialTranslateX = base.initialTranslateX;
        this.initialTranslateY = base.initialTranslateY;
        this.initialTranslateZ = base.initialTranslateZ;
    }
}''',
    'org/mtr/mapping/holder/Vector3d.java': '''package org.mtr.mapping.holder;
public class Vector3d {
    public final double x, y, z;
    public Vector3d(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public double getXMapped() { return x; }
    public double getYMapped() { return y; }
    public double getZMapped() { return z; }
}''',
    'org/mtr/mapping/holder/Direction.java': 'package org.mtr.mapping.holder; public enum Direction { NORTH, SOUTH, EAST, WEST }',
    'org/mtr/mapping/holder/World.java': 'package org.mtr.mapping.holder; public class World {}',
    # Matrices stub: compileStoredMatrixTransformations yields one marker consumer.
    'com/lx862/mtrscripting/core/util/Matrices.java': '''package com.lx862.mtrscripting.core.util;
import org.mtr.mod.render.StoredMatrixTransformations;
public class Matrices {
    public StoredMatrixTransformations compileStoredMatrixTransformations() {
        StoredMatrixTransformations smt = new StoredMatrixTransformations();
        smt.add(gh -> gh.translate(0.5, 0.5, 0.5));
        return smt;
    }
}''',
    # ScriptRenderManager references these in its drawModel overloads.
    'com/lx862/mtrscripting/core/util/model/ModelJS.java': '''package com.lx862.mtrscripting.core.util.model;
import org.mtr.mod.render.StoredMatrixTransformations;
public class ModelJS {
    public void draw(StoredMatrixTransformations storedMatrixTransformations, int light) {}
}''',
    'com/lx862/mtrscripting/core/util/model/DynamicModelHolderJS.java': '''package com.lx862.mtrscripting.core.util.model;
public class DynamicModelHolderJS {
    public ModelJS getUploadedModel() { return null; }
}''',
}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--build', required=True, type=Path)
    p.add_argument('--java-home', type=Path)
    args = p.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'scripted-render-test'
    work.mkdir(parents=True, exist_ok=True)

    def tool(name):
        home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if home else name

    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)

    base = repo / 'fabric/src/main/java'
    sources += [
        base / 'com/lx862/mtrscripting/core/util/render/ScriptRenderManager.java',
        base / 'com/lx862/mtrscripting/core/util/render/RenderDrawCall.java',
        base / 'com/lx862/mtrscripting/core/util/render/TransformCapturePool.java',
        base / 'com/lx862/mtrscripting/core/util/render/ModelDrawCall.java',
        base / 'com/lx862/mtrscripting/core/util/render/DynamicModelDrawCall.java',
        base / 'com/lx862/jcm/nativeapi/NativeTransformScope.java',
        base / 'com/lx862/mtrscripting/core/api/ScriptResultCall.java',
        base / 'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
        base / 'com/lx862/mtrscripting/core/annotation/ValueNullable.java',
        Path(__file__).with_name('java') / 'ScriptedRenderSemanticsTest.java',
    ]

    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + p.as_posix() + '"' for p in sources), encoding='utf-8')
    subprocess.run([tool('javac'), '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.ScriptedRenderSemanticsTest'], check=True)


if __name__ == '__main__':
    main()
