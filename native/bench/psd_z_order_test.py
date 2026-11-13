"""Regression for production PIDS/PSD auto-z ordering and persistent wrappers.

The real context and draw-call classes run against a matrix-evaluating
GraphicsHolder. This catches inverted PSD depth and offsets accumulating
across repeated JS frames, including wrappers with user matrices.
"""
import argparse
import os
from pathlib import Path
import subprocess

from scripted_render_test import STUBS as RENDER_STUBS


STUBS = dict(RENDER_STUBS)
STUBS.update({
    'org/mtr/mapping/mapper/GraphicsHolder.java': '''package org.mtr.mapping.mapper;
import java.util.ArrayDeque;
public class GraphicsHolder {
    private double[] matrix = identity();
    private final ArrayDeque<double[]> stack = new ArrayDeque<>();
    private static double[] identity() {
        double[] m = new double[16]; m[0]=m[5]=m[10]=m[15]=1; return m;
    }
    private void multiply(double[] right) {
        double[] out = new double[16];
        for (int r=0;r<4;r++) for (int c=0;c<4;c++) for (int k=0;k<4;k++)
            out[r*4+c] += matrix[r*4+k] * right[k*4+c];
        matrix=out;
    }
    public void push() { stack.push(matrix.clone()); }
    public void pop() { matrix=stack.pop(); }
    public void translate(double x,double y,double z) {
        double[] t=identity();t[3]=x;t[7]=y;t[11]=z;multiply(t);
    }
    public void scale(float x,float y,float z) {
        double[] t=identity();t[0]=x;t[5]=y;t[10]=z;multiply(t);
    }
    public void rotateXDegrees(float d) {
        double c=Math.cos(Math.toRadians(d)),s=Math.sin(Math.toRadians(d));
        double[] t=identity();t[5]=c;t[6]=-s;t[9]=s;t[10]=c;multiply(t);
    }
    public void rotateYDegrees(float d) {
        double c=Math.cos(Math.toRadians(d)),s=Math.sin(Math.toRadians(d));
        double[] t=identity();t[0]=c;t[2]=s;t[8]=-s;t[10]=c;multiply(t);
    }
    public void rotateZDegrees(float d) {
        double c=Math.cos(Math.toRadians(d)),s=Math.sin(Math.toRadians(d));
        double[] t=identity();t[0]=c;t[1]=-s;t[4]=s;t[5]=c;multiply(t);
    }
    public double[] point(double x,double y,double z) {
        return new double[]{matrix[0]*x+matrix[1]*y+matrix[2]*z+matrix[3],
            matrix[4]*x+matrix[5]*y+matrix[6]*z+matrix[7],
            matrix[8]*x+matrix[9]*y+matrix[10]*z+matrix[11]};
    }
}''',
    'com/lx862/mtrscripting/core/util/Matrices.java': '''package com.lx862.mtrscripting.core.util;
import org.mtr.mod.render.StoredMatrixTransformations;
public class Matrices {
    private final StoredMatrixTransformations value;
    public Matrices(StoredMatrixTransformations value) { this.value=value; }
    public StoredMatrixTransformations compileStoredMatrixTransformations() { return value; }
}''',
    'com/lx862/mtrscripting/core/util/render/ScriptRenderManager.java': '''package com.lx862.mtrscripting.core.util.render;
public class ScriptRenderManager {
    public void draw(RenderDrawCall<?> call) { call.validate(); }
    public void reset() {}
}''',
    'com/lx862/mtrscripting/core/util/sound/ScriptSoundManager.java': '''package com.lx862.mtrscripting.core.util.sound;
public class ScriptSoundManager { public void reset() {} }''',
    'com/lx862/mtrscripting/mod/impl/mtr/MTRScriptContext.java': '''package com.lx862.mtrscripting.mod.impl.mtr;
public class MTRScriptContext {
    public MTRScriptContext(String name) {}
    public void resetForNextRun() {}
}''',
    'com/lx862/jcm/mod/data/pids/preset/PIDSPresetBase.java': '''package com.lx862.jcm.mod.data.pids.preset;
public class PIDSPresetBase { public static final float BASE_SCALE=1/96F; }''',
    'com/lx862/jcm/mod/data/pids/preset/components/base/PIDSComponent.java': '''package com.lx862.jcm.mod.data.pids.preset.components.base;
public class PIDSComponent { public static PIDSComponent parse(Object object) { return null; } }''',
    'com/google/gson/JsonParser.java': '''package com.google.gson;
public class JsonParser { public JsonParser parse(String value) { return this; } public Object getAsJsonObject() { return null; } }''',
})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'psd-z-order-test'
    work.mkdir(parents=True, exist_ok=True)

    def tool(name):
        java_home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)

    base = repo / 'fabric/src/main/java'
    sources += [base / path for path in [
        'com/lx862/jcm/nativeapi/NativeTransformScope.java',
        'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
        'com/lx862/mtrscripting/core/annotation/ValueNullable.java',
        'com/lx862/mtrscripting/core/api/ScriptResultCall.java',
        'com/lx862/mtrscripting/core/util/render/RenderDrawCall.java',
        'com/lx862/jcm/mod/scripting/pids/PIDSDrawCall.java',
        'com/lx862/jcm/mod/scripting/pids/PIDSScriptContext.java',
        'com/lx862/jcm/mod/scripting/psd/PSDScriptContext.java',
    ]]
    sources.append(repo / 'native/bench/java/PSDZOrderTest.java')
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    subprocess.run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), *map(str, sources)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'PSDZOrderTest'], check=True)


if __name__ == '__main__':
    main()
