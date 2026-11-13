"""Exercise production TextWrapper's deferred background fill with engine stubs.

The TextRenderer stub implements the empty-run background bounds verified from
Minecraft 1.20.1's Drawer.finish: [-1, 1] x [-1, 9], z=0.01. Tests cover exact
coverage, font independence, pooled reuse and matrix cleanup on renderer failure.
No resource-pack assets are used.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess

STUBS = {
    'org/joml/Matrix4f.java': '''package org.joml;
public class Matrix4f {
 public double sx=1, sy=1, sz=1, tx, ty, tz;
 public Matrix4f copy(){Matrix4f m=new Matrix4f();m.sx=sx;m.sy=sy;m.sz=sz;m.tx=tx;m.ty=ty;m.tz=tz;return m;}
 public void scale(double x,double y,double z){sx*=x;sy*=y;sz*=z;}
 public void translate(double x,double y,double z){tx+=sx*x;ty+=sy*y;tz+=sz*z;}
}''',
    'net/minecraft/client/util/math/MatrixStack.java': '''package net.minecraft.client.util.math;
import org.joml.Matrix4f; import java.util.ArrayDeque;
public class MatrixStack {
 public Matrix4f matrix=new Matrix4f(); public final ArrayDeque<Matrix4f> stack=new ArrayDeque<>();
 public void push(){stack.push(matrix.copy());} public void pop(){matrix=stack.pop();}
 public MatrixStack peek(){return this;} public Matrix4f getPositionMatrix(){return matrix;}
}''',
    'net/minecraft/client/font/TextRenderer.java': '''package net.minecraft.client.font;
import org.joml.Matrix4f; import net.minecraft.client.render.VertexConsumerProvider;
public class TextRenderer {
 public enum TextLayerType {NORMAL}
 public static boolean fail; public static int calls; public static String text;
 public static double left,right,top,bottom,z; public static int background;
 public int draw(String s,float x,float y,int color,boolean shadow,Matrix4f m,VertexConsumerProvider v,TextLayerType layer,int bg,int light){
  calls++; text=s; background=bg; if(fail)throw new IllegalStateException("simulated draw failure");
  if(!s.isEmpty())throw new AssertionError("background must not rasterize glyphs");
  left=m.tx+m.sx*(x-1);right=m.tx+m.sx*(x+1);top=m.ty+m.sy*(y-1);bottom=m.ty+m.sy*(y+9);z=m.tz+m.sz*.01;
  return 0;
 }
}''',
    'net/minecraft/client/MinecraftClient.java': '''package net.minecraft.client;
public class MinecraftClient {
 private static final MinecraftClient INSTANCE=new MinecraftClient();
 public final net.minecraft.client.font.TextRenderer textRenderer=new net.minecraft.client.font.TextRenderer();
 public static MinecraftClient getInstance(){return INSTANCE;}
}''',
    'net/minecraft/client/render/VertexConsumerProvider.java': '''package net.minecraft.client.render;
public class VertexConsumerProvider {
 public static Immediate immediate(Object buffer){return new Immediate();}
 public static class Immediate extends VertexConsumerProvider {public void draw(){}}
}''',
    'net/minecraft/client/render/Tessellator.java': '''package net.minecraft.client.render;
public class Tessellator {public static Tessellator getInstance(){return new Tessellator();}public Object getBuffer(){return null;}}''',
    'com/lx862/jcm/nativeapi/NativeMatrixApplier.java': '''package com.lx862.jcm.nativeapi;
public interface NativeMatrixApplier {net.minecraft.client.util.math.MatrixStack jcm$getMatrixStack();}''',
    'org/mtr/mapping/mapper/GraphicsHolder.java': '''package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.MutableText;
public class GraphicsHolder implements com.lx862.jcm.nativeapi.NativeMatrixApplier {
 public final net.minecraft.client.util.math.MatrixStack matrices=new net.minecraft.client.util.math.MatrixStack();
 public static boolean forbidMetrics; public static int textCalls;
 public net.minecraft.client.util.math.MatrixStack jcm$getMatrixStack(){return matrices;}
 public void push(){matrices.push();}public void pop(){matrices.pop();}
 public void scale(float x,float y,float z){matrices.matrix.scale(x,y,z);}
 public void translate(double x,double y,double z){matrices.matrix.translate(x,y,z);}
 public static int getTextWidth(String s){if(forbidMetrics)throw new AssertionError("font metrics queried");return (int)Math.ceil(s.length()*1.875);}
 public static int getTextWidth(MutableText s){return getTextWidth(s.getString());}
 public void drawText(MutableText t,int x,int y,int c,boolean s,int l){textCalls++;}
}''',
    'org/mtr/mapping/holder/MutableText.java': '''package org.mtr.mapping.holder;
public class MutableText {private final String s;public MutableText(String s){this.s=s;}public String getString(){return s;}}''',
    'org/mtr/mapping/holder/Style.java': '''package org.mtr.mapping.holder;
public class Style {public static Style getEmptyMapped(){return new Style();}public Style withBold(boolean b){return this;}public Style withItalic(boolean b){return this;}}''',
    'org/mtr/mapping/holder/Identifier.java': '''package org.mtr.mapping.holder;
public class Identifier {public Identifier(String a,String b){}}''',
    'org/mtr/mapping/holder/Direction.java': 'package org.mtr.mapping.holder;public enum Direction {NORTH,SOUTH}',
    'org/mtr/mapping/holder/Vector3d.java': 'package org.mtr.mapping.holder;public class Vector3d {}',
    'org/mtr/mapping/mapper/TextHelper.java': '''package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.*;
public class TextHelper {public static MutableText literal(String s){return new MutableText(s);}public static MutableText setStyle(MutableText t,Style s){return t;}}''',
    'org/mtr/mod/Init.java': 'package org.mtr.mod;public class Init {public static final String MOD_ID="mtr";}',
    'org/mtr/mod/InitClient.java': 'package org.mtr.mod;public class InitClient {public static long getGameTick(){return 0;}}',
    'org/mtr/mod/render/QueuedRenderLayer.java': 'package org.mtr.mod.render;public enum QueuedRenderLayer {TEXT}',
    'org/mtr/mod/render/MainRenderer.java': '''package org.mtr.mod.render;
import java.util.function.BiConsumer;import org.mtr.mapping.mapper.GraphicsHolder;import org.mtr.mapping.holder.Vector3d;
public class MainRenderer {public static BiConsumer<GraphicsHolder,Vector3d> pending;
 public static void scheduleRender(QueuedRenderLayer l,BiConsumer<GraphicsHolder,Vector3d> draw){pending=draw;}}
''',
    'org/mtr/mod/render/StoredMatrixTransformations.java': '''package org.mtr.mod.render;
import org.mtr.mapping.mapper.GraphicsHolder;import org.mtr.mapping.holder.Vector3d;
public class StoredMatrixTransformations {public void transform(GraphicsHolder g,Vector3d v){g.push();}}
''',
    'com/lx862/jcm/mod/util/CachedIdentifiers.java': '''package com.lx862.jcm.mod.util;
import org.mtr.mapping.holder.Identifier;public class CachedIdentifiers {public static Identifier of(String s){return new Identifier("",s);}}''',
    'com/lx862/jcm/mod/util/TextUtil.java': '''package com.lx862.jcm.mod.util;
import org.mtr.mapping.holder.*;public class TextUtil {public static Style withFontStyle(Identifier id){return new Style();}}''',
    'com/lx862/mtrscripting/mod/util/JSUtil.java': '''package com.lx862.mtrscripting.mod.util;
import org.mtr.mapping.holder.Identifier;public class JSUtil {public static Identifier parseId(String s){return new Identifier("",s);}}''',
    'com/lx862/jcm/mod/render/RenderHelper.java': 'package com.lx862.jcm.mod.render;public interface RenderHelper {int MAX_RENDER_LIGHT=0xF000F0;}',
    'com/lx862/jcm/mod/scripting/pids/PIDSDrawCall.java': '''package com.lx862.jcm.mod.scripting.pids;
import org.mtr.mod.render.StoredMatrixTransformations;import org.mtr.mapping.holder.Direction;
public abstract class PIDSDrawCall<T extends PIDSDrawCall<?>> {
 public double w,h;public PIDSDrawCall(double w,double h){resetDefaults(w,h);}
 public T size(double w,double h){this.w=w;this.h=h;return (T)this;}
 public void resetDefaults(double w,double h){this.w=w;this.h=h;}public void release(){}
 public abstract void validate();protected abstract void drawTransformed(StoredMatrixTransformations m,Direction d,int light);
}''',
}


def preprocess(source):
    """Select production's Fabric 1.20.1 branch, without changing Java bodies."""
    output, frames, active = [], [], True
    values = {'LOADER': 'fabric', 'MC_VERSION': '12001'}
    for line in source.splitlines():
        stripped = line.strip()
        if stripped.startswith('#if '):
            match = re.fullmatch(r'#if (\w+) (==|>=|<=) "([^"]+)"', stripped)
            if not match:
                raise ValueError(stripped)
            key, op, value = match.groups()
            actual = values[key]
            selected = {'==': actual == value, '>=': actual >= value, '<=': actual <= value}[op]
            frames.append((active, selected))
            active = active and selected
        elif stripped == '#else':
            parent, selected = frames[-1]
            active = parent and not selected
        elif stripped == '#endif':
            active, _ = frames.pop()
        elif active:
            output.append(line)
    assert not frames
    return '\n'.join(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'text-background-test'
    work.mkdir(parents=True, exist_ok=True)
    java_root = repo / 'fabric/src/main/java'
    sources = []
    contents = dict(STUBS)
    relative = 'com/lx862/jcm/mod/scripting/pids/TextWrapper.java'
    contents[relative] = preprocess((java_root / relative).read_text(encoding='utf8'))
    for name, content in contents.items():
        path = work / 'src' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf8')
        sources.append(path)
    sources += [java_root / 'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
                Path(__file__).with_name('java') / 'TextBackgroundTest.java']
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf8')
    home = args.java_home or os.environ.get('JAVA_HOME')
    def tool(name):
        return str(Path(home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if home else name
    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.mod.scripting.pids.TextBackgroundTest'], check=True)


if __name__ == '__main__':
    main()
