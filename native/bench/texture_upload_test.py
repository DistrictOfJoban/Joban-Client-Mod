"""Exercise production GraphicsTexture with real LWJGL/OpenGL.

Only Minecraft holders/services are substituted. They own a real off-heap
NativeImage and queue GL uploads; production image copies, clipping arguments
and GPU texture swizzle run unchanged. No user artwork or resource pack needed.
"""
import argparse
import os
from pathlib import Path
import platform
import subprocess
import urllib.request

STUBS = {
    'com/lx862/jcm/mapping/LoaderImplClient.java': '''package com.lx862.jcm.mapping;
public class LoaderImplClient { public static long getNativeImagePointer(org.mtr.mapping.holder.NativeImage i){return i.pointer;} }''',
    'com/lx862/mtrscripting/core/annotation/ApiInternal.java': 'package com.lx862.mtrscripting.core.annotation; public @interface ApiInternal {}',
    'com/lx862/mtrscripting/mod/MTRScriptingMod.java': '''package com.lx862.mtrscripting.mod;
public class MTRScriptingMod { public static org.mtr.mapping.holder.Identifier id(String s){return new org.mtr.mapping.holder.Identifier(s);} }''',
    'com/mojang/blaze3d/platform/GlStateManager.java': '''package com.mojang.blaze3d.platform;
public class GlStateManager { public static void _bindTexture(int id){org.lwjgl.opengl.GL33.glBindTexture(3553,id);} }''',
    'com/mojang/blaze3d/systems/RenderSystem.java': '''package com.mojang.blaze3d.systems;
public class RenderSystem {
 private static final java.util.ArrayList<Runnable> calls=new java.util.ArrayList<>();
 public static void recordRenderCall(Runnable r){calls.add(r);}
 public static void drain(){for(Runnable r:calls)r.run();calls.clear();}
}''',
    'org/mtr/mapping/holder/Identifier.java': 'package org.mtr.mapping.holder; public record Identifier(String value) {}',
    'org/mtr/mapping/holder/AbstractTexture.java': '''package org.mtr.mapping.holder;
public class AbstractTexture { public final NativeImageBackedTexture texture; public AbstractTexture(NativeImageBackedTexture t){texture=t;} }''',
    'org/mtr/mapping/holder/NativeImage.java': '''package org.mtr.mapping.holder;
import org.lwjgl.opengl.GL33; import org.lwjgl.system.MemoryUtil;
public class NativeImage {
 public final int width,height; public final java.nio.ByteBuffer pixels; public final long pointer;
 public NativeImage(int w,int h,boolean clear){width=w;height=h;pixels=MemoryUtil.memCalloc(w*h*4);pointer=MemoryUtil.memAddress(pixels);}
 public void upload(int level,int dx,int dy,int sx,int sy,int w,int h,boolean a,boolean b,boolean c,boolean d){
  GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT,4);GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH,width);
  GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_PIXELS,sx);GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_ROWS,sy);
  GL33.glTexSubImage2D(GL33.GL_TEXTURE_2D,level,dx,dy,w,h,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,pixels);
  GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH,0);GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_PIXELS,0);GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_ROWS,0);
 }
}''',
    'org/mtr/mapping/holder/NativeImageBackedTexture.java': '''package org.mtr.mapping.holder;
import org.lwjgl.opengl.GL33;
public class NativeImageBackedTexture {
 public final NativeImageBackedTexture data=this; public final int id; private final NativeImage image;
 public NativeImageBackedTexture(NativeImage i){image=i;id=GL33.glGenTextures();bindTexture();
  GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA8,i.width,i.height,0,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,i.pixels);
 }
 public NativeImage getImage(){return image;}
 public void bindTexture(){GL33.glBindTexture(GL33.GL_TEXTURE_2D,id);}
 public void upload(){bindTexture();image.upload(0,0,0,0,0,image.width,image.height,false,false,false,false);}
 public void close(){GL33.glDeleteTextures(id);org.lwjgl.system.MemoryUtil.memFree(image.pixels);}
}''',
    'org/mtr/mapping/holder/MinecraftClient.java': '''package org.mtr.mapping.holder;
public class MinecraftClient {
 private static final MinecraftClient INSTANCE=new MinecraftClient(); private final TextureManager manager=new TextureManager();
 public static MinecraftClient getInstance(){return INSTANCE;}public void execute(Runnable r){r.run();}public TextureManager getTextureManager(){return manager;}
 public static class TextureManager {
  private final java.util.Map<Identifier,AbstractTexture> entries=new java.util.HashMap<>();
  public void registerTexture(Identifier id,AbstractTexture t){entries.put(id,t);}
  public void destroyTexture(Identifier id){var t=entries.remove(id);if(t!=null)t.texture.close();}
  public NativeImageBackedTexture get(Identifier id){return entries.get(id).texture;}
 }
}''',
}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--work', required=True, type=Path)
    p.add_argument('--java-home', type=Path)
    args = p.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)
    system = 'windows' if os.name == 'nt' else 'macos' if platform.system() == 'Darwin' else 'linux'
    if platform.machine().lower() in ('arm64', 'aarch64'):
        system += '-arm64'
    jars = []
    for module in ('lwjgl', 'lwjgl-glfw', 'lwjgl-opengl'):
        for suffix in ('', '-natives-' + system):
            name = module + '-3.3.1' + suffix + '.jar'
            target = work / name
            if not target.exists():
                cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.lwjgl' / module / '3.3.1'
                candidates = list(cache.glob('*/' + name))
                if candidates:
                    target.write_bytes(candidates[0].read_bytes())
                else:
                    urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/lwjgl/' + module + '/3.3.1/' + name, target)
            jars.append(target)
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    base = repo / 'fabric/src/main/java'
    sources += [base / 'com/lx862/mtrscripting/core/util/GraphicsTexture.java',
                base / 'com/lx862/jcm/nativeapi/NativePixelBuffer.java',
                Path(__file__).with_name('java') / 'NativeTextureUploadTest.java']
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    def tool(name):
        return str(args.java_home / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if args.java_home else name
    cp = os.pathsep.join(map(str, jars))
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + p.as_posix() + '"' for p in sources), encoding='utf-8')
    subprocess.run([tool('javac'), '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes) + os.pathsep + cp, 'com.lx862.jcm.nativeapi.NativeTextureUploadTest'], check=True)


if __name__ == '__main__':
    main()
