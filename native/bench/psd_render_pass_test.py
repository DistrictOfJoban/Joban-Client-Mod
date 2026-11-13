"""Exercise the production PSD renderAll gate with adversarial stale pass state.

Extracts renderAll verbatim from PSDScriptRenderer, retaining its actual control
flow and calls. Only world/model setup and the downstream glass/top renderer are
replaced with small stubs, avoiding hundreds of irrelevant geometry dependencies.
The real ShaderDetector is compiled against an Iris API stub whose live state
alternates independently of OptimizedRenderer's deliberately stale cache.

This checks pass routing, not pixels or GPU visibility. A mutation of the actual
shadowPass assignment back to the old cached query must fail the same assertions.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess


STUBS = {
    'net/irisshaders/iris/api/v0/IrisApi.java': '''package net.irisshaders.iris.api.v0;
public final class IrisApi {
 public static final IrisApi INSTANCE=new IrisApi();
 public static boolean shadow,pack; public static int reads;
 public static IrisApi getInstance(){return INSTANCE;}
 public boolean isRenderingShadowPass(){reads++;return shadow;}
 public boolean isShaderPackInUse(){return pack;}
}''',
    'org/mtr/mapping/render/shader/ModShaderHandler.java': '''package org.mtr.mapping.render.shader;
public final class ModShaderHandler {
 public static boolean renderingShadows(){throw new AssertionError("live Iris API must be used");}
}''',
    'org/mtr/mapping/holder/Vector3d.java': '''package org.mtr.mapping.holder;
public final class Vector3d {
 public double getXMapped(){return 0;}public double getYMapped(){return 0;}public double getZMapped(){return 0;}
}''',
    'com/lx862/mtrscripting/mod/MTRScriptingModClient.java': '''package com.lx862.mtrscripting.mod;
public final class MTRScriptingModClient {
 public static final Logger LOGGER=new Logger();
 public static final class Logger {public void info(String s){}}
}''',
}


HARNESS = '''package com.lx862.jcm.mod.scripting.psd;
import java.util.*;
import java.util.function.Consumer;
import com.lx862.jcm.mod.util.ShaderDetector;
import net.irisshaders.iris.api.v0.IrisApi;

public final class PSDScriptRenderer {
 private static final double MAX_DISTANCE_SQ=192*192;
 private static int glassCalls,topCalls;
 private static boolean glassShadow,glassShader,topShadow;

__PRODUCTION_RENDER_ALL__

 private static void renderGlass(PSDBinding binding,BlockPos pos,BlockState state,World world,
   boolean shadow,boolean shaders,Set<BlockPos> sections,double cameraX,double cameraZ) {
  glassCalls++;glassShadow=shadow;glassShader=shaders;
 }
 private static void renderTop(PSDBinding binding,BlockPos pos,BlockState state,World world,
   boolean shadow,Set<BlockPos> sections) {topCalls++;topShadow=shadow;}
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static void checkPass(boolean live,boolean cached,boolean pack) {
  IrisApi.shadow=live;IrisApi.pack=pack;OptimizedRenderer.cached=cached;
  glassCalls=topCalls=0;renderAll();
  check(glassCalls==1 && topCalls==1,"nearby glass/top must each render once; far binding must be culled");
  check(glassShadow==live && topShadow==live,"PSD routed using stale shadow cache: live="+live+", cached="+cached);
  check(glassShader==(!live && pack),"shader backing must run only in the current camera pass");
 }
 public static void main(String[] args) {
  PSDClientBindings.bindings.add(new PSDBinding(new BlockPos(0),PSDWrapper.PSDType.GLASS));
  PSDClientBindings.bindings.add(new PSDBinding(new BlockPos(1),PSDWrapper.PSDType.TOP));
  PSDClientBindings.bindings.add(new PSDBinding(new BlockPos(1000),PSDWrapper.PSDType.GLASS));
  // The PSD HEAD hook runs before the other HEAD hook updates the cache.
  // Shadow and camera passes therefore see the preceding pass's opposite flag.
  for(int i=0;i<100;i++){checkPass(true,false,true);checkPass(false,true,true);}
  checkPass(false,true,false);checkPass(false,false,false);checkPass(false,false,true);
  int reads=IrisApi.reads;
  MinecraftClient.INSTANCE.world=null;renderAll();check(IrisApi.reads==reads,"disconnected world queried render state");
  MinecraftClient.INSTANCE.world=new World();MinecraftClient.INSTANCE.player=null;
  renderAll();check(IrisApi.reads==reads,"missing player queried render state");
  MinecraftClient.INSTANCE.player=new ClientPlayerEntity();PSDClientBindings.bindings.clear();
  renderAll();check(IrisApi.reads==reads,"empty binding set queried render state");
  System.out.println("PSD production renderAll: alternating live/cached passes, shader toggles, glass/top routing and early exits passed");
 }
}
final class OptimizedRenderer {static boolean cached;static boolean renderingShadows(){return cached;}}
final class ClientPlayerEntity {}
final class BlockPos {
 final int x;BlockPos(int x){this.x=x;}int getX(){return x;}int getY(){return 0;}int getZ(){return 0;}
}
final class BlockState {
 final Object data;BlockState(Object data){this.data=data;}Block getBlock(){return new Block(data);}
 static final class Block {final Object data;Block(Object data){this.data=data;}}
}
final class World {
 static World cast(World w){return w;}
 BlockState getBlockState(BlockPos p){return new BlockState(p.x==1?new BlockPSDTop():new BlockPSDAPGGlassBase());}
}
final class BlockPSDTop {} final class BlockPSDAPGGlassBase {}
final class PSDWrapper {enum PSDType {GLASS,TOP}}
final class PSDBinding {
 final BlockPos pos;final PSDWrapper.PSDType type;
 PSDBinding(BlockPos p,PSDWrapper.PSDType t){pos=p;type=t;}
}
final class PSDClientBindings {
 static final List<PSDBinding> bindings=new ArrayList<>();
 static boolean isEmpty(){return bindings.isEmpty();}
 static void forEach(Consumer<PSDBinding> f){bindings.forEach(f);}
}
final class MinecraftClient {
 static final MinecraftClient INSTANCE=new MinecraftClient();
 World world=new World();ClientPlayerEntity player=new ClientPlayerEntity();
 static MinecraftClient getInstance(){return INSTANCE;}
 World getWorldMapped(){return world;}ClientPlayerEntity getPlayerMapped(){return player;}
 GameRenderer getGameRendererMapped(){return new GameRenderer();}
 static final class GameRenderer {Camera getCamera(){return new Camera();}}
 static final class Camera {org.mtr.mapping.holder.Vector3d getPos(){return new org.mtr.mapping.holder.Vector3d();}}
}
'''


def render_all(source):
    start = source.index('    public static void renderAll() {')
    # Restrict the slice to the next member boundary before counting braces;
    # this keeps the actual method body, comments and statements unchanged.
    depth = 0
    for index in range(source.index('{', start), len(source)):
        if source[index] == '{':
            depth += 1
        elif source[index] == '}':
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise ValueError('Unterminated production renderAll')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    java_root = repo / 'fabric/src/main/java'
    work = args.build.resolve() / 'psd-render-pass-test'
    work.mkdir(parents=True, exist_ok=True)
    body = render_all((java_root / 'com/lx862/jcm/mod/scripting/psd/PSDScriptRenderer.java').read_text(encoding='utf8'))

    def tool(name):
        home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if home else name

    def run_case(label, method):
        folder = work / label
        classes = folder / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        contents = dict(STUBS)
        contents['com/lx862/jcm/mod/scripting/psd/PSDScriptRenderer.java'] = HARNESS.replace('__PRODUCTION_RENDER_ALL__', method)
        sources = []
        for name, content in contents.items():
            target = folder / 'src' / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding='utf8')
            sources.append(target)
        sources.append(java_root / 'com/lx862/jcm/mod/util/ShaderDetector.java')
        argfile = folder / 'sources.txt'
        argfile.write_text('\n'.join('"' + p.as_posix() + '"' for p in sources), encoding='utf8')
        subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '--release', '17', '-d', str(classes), '@' + str(argfile)], check=True)
        return subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer'], capture_output=True, text=True)

    fixed = run_case('production', body)
    if fixed.returncode:
        raise AssertionError(fixed.stdout + fixed.stderr)
    print(fixed.stdout.strip())
    old, count = re.subn(r'(final boolean shadowPass\s*=\s*)ShaderDetector\.isRenderingShadowPass\(\)', r'\1OptimizedRenderer.renderingShadows()', body)
    if count != 1:
        raise AssertionError('Expected exactly one production live-pass assignment for mutation test')
    mutant = run_case('old-cached-gate', old)
    if mutant.returncode == 0 or 'PSD routed using stale shadow cache' not in mutant.stderr:
        raise AssertionError('Old cached gate must fail the production routing assertions: ' + mutant.stdout + mutant.stderr)
    print('Mutation check: old cached-pass assignment fails the same routing test as expected')


if __name__ == '__main__':
    main()
