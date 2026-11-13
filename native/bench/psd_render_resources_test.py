"""Exercise production PSD backing geometry, stack cleanup and resource reload.

The renderer's drawBacking/reset and the Fabric 1.20+ window-cache methods are
extracted verbatim. GPU submission and texture analysis are replaced by recording
stubs: this verifies queue geometry and cache invalidation, not rendered pixels.
Only GitHub Actions invokes this harness; it has no Minecraft runtime dependency.
"""

import argparse
import os
from pathlib import Path
import re
import subprocess


STUBS = {
    'org/mtr/mapping/holder/BlockState.java': '''package org.mtr.mapping.holder;
public final class BlockState {public final Object data;public BlockState(Object data){this.data=data;}}''',
    'org/mtr/mapping/holder/Direction.java': '''package org.mtr.mapping.holder;
public final class Direction {public final Object data;public Direction(Object data){this.data=data;}}''',
    'com/lx862/mtrscripting/mod/MTRScriptingModClient.java': '''package com.lx862.mtrscripting.mod;
public final class MTRScriptingModClient {
 public static final Logger LOGGER=new Logger();
 public static final class Logger {public void info(String s){}public void warn(String s,Throwable t){}}
}''',
}


HARNESS = '''package com.lx862.jcm.mod.scripting.psd;
import java.util.*;

public final class PSDScriptRenderer {
 private static final Object BACKING_TEXTURE=new Object();
 private static final int MAX_RENDER_LIGHT=0xF000F0;
 private static boolean shaderBackingPlatformLogged,shaderBackingTrackLogged,BACKING_B_WARNED,BACKING_B_OK;
__CONSTANTS__
__DRAW_BACKING__
__RENDERER_RESET__

 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private static void near(float actual,float expected,String message){check(Math.abs(actual-expected)<0.00001F,message+": "+actual+" != "+expected);}
 private static float winding(Face face){
  float sum=0;
  for(int i=0;i<4;i++){int next=(i+1)%4;sum+=face.xyz[i*3]*face.xyz[next*3+1]-face.xyz[next*3]*face.xyz[i*3+1];}
  return sum;
 }
 public static void main(String[] args){
  StoredMatrixTransformations transform=new StoredMatrixTransformations();
  GraphicsHolder holder=new GraphicsHolder();holder.depth=3;
  drawBacking(transform,0.2F,0.75F,0.8F,0.1F,2,Direction.NORTH);
  check(MainRenderer.queue.size()==1,"valid backing must schedule exactly one callback");
  check(holder.faces.isEmpty(),"backing must remain queued until render flush");
  MainRenderer.flush(holder);
  check(holder.depth==3,"matrix stack leaked after backing draw");
  check(holder.faces.size()==2,"backing needs one platform face and one track face");
  for(Face face:holder.faces){
   near(face.xyz[0],0.2F,"backing left boundary must follow pane extent");
   near(face.xyz[3],0.75F,"backing right boundary must follow pane extent");
   near(face.xyz[6],0.75F,"backing third-vertex boundary");
   near(face.xyz[9],0.2F,"backing fourth-vertex boundary");
   check(face.light==MAX_RENDER_LIGHT,"backing light changed");
  }
  Face platform=holder.faces.get(0),track=holder.faces.get(1);
  check(platform.color==0xFF000000 && track.color==0xFFFFFFFF,"backing face colors must be black/white");
  check(platform.facing==Direction.SOUTH && track.facing==Direction.NORTH,"backing normals must oppose");
  check(winding(platform)*winding(track)<0,"backing faces must have opposite winding");
  near(platform.xyz[1],1.9F,"platform lower y");near(platform.xyz[7],1.2F,"platform upper y");
  near(track.xyz[1],1.2F,"track upper y");near(track.xyz[7],1.9F,"track lower y");
  check(track.xyz[2]>platform.xyz[2],"track face must sit behind platform face");
  check(BACKING_B_OK,"successful backing should mark diagnostic success");

  drawBacking(transform,0.75F,0.2F,0.8F,0.1F,2,Direction.NORTH);
  drawBacking(transform,0.2F,0.75F,0.1F,0.8F,2,Direction.NORTH);
  check(MainRenderer.queue.isEmpty(),"inverted extents must not schedule a backing");

  GraphicsHolder failing=new GraphicsHolder();failing.depth=7;failing.throwAt=2;
  drawBacking(transform,0.2F,0.75F,0.8F,0.1F,2,Direction.NORTH);
  MainRenderer.flush(failing);
  check(failing.calls==2,"failure fixture did not reach the second face");
  check(failing.depth==7,"matrix stack leaked after second backing face threw");
  check(BACKING_B_WARNED,"backing failure must be diagnosed");

  BlockState state=new BlockState();
  org.mtr.mapping.holder.BlockState mappedState=new org.mtr.mapping.holder.BlockState(state);
  org.mtr.mapping.holder.Direction mappedDirection=new org.mtr.mapping.holder.Direction(Direction.NORTH);
  PSDWindowBounds.Bounds before=PSDWindowBounds.window(mappedState,mappedDirection);
  check(PSDWindowBounds.computations==1,"first geometry lookup must compute once");
  PSDWindowBounds.generation=9;
  check(PSDWindowBounds.window(mappedState,mappedDirection)==before,"same block state must hit the cache");
  check(PSDWindowBounds.computations==1,"cache hit recomputed geometry");
  shaderBackingPlatformLogged=shaderBackingTrackLogged=true;
  reset();
  check(!BACKING_B_OK && !BACKING_B_WARNED && !shaderBackingPlatformLogged && !shaderBackingTrackLogged,"reload must clear renderer diagnostic flags");
  PSDWindowBounds.Bounds after=PSDWindowBounds.window(mappedState,mappedDirection);
  check(after!=before && after.generation==9 && PSDWindowBounds.computations==2,"resource reload reused stale pane geometry");
  check(PSDWindowBounds.window(new org.mtr.mapping.holder.BlockState(state),mappedDirection)==after,"mapping wrappers must share native-state cache keys");
  check(PSDWindowBounds.computations==2,"cache key must be native block state");
  System.out.println("PSD render resources: horizontal extents, colors/winding, exceptional pop and reload invalidation passed");
 }
}
final class Face {
 final float[] xyz;final Direction facing;final int color,light;
 Face(float[] xyz,Direction facing,int color,int light){this.xyz=xyz;this.facing=facing;this.color=color;this.light=light;}
}
final class GraphicsHolder {
 int depth,calls,throwAt;final List<Face> faces=new ArrayList<>();
 void drawTextureInWorld(float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,float x4,float y4,float z4,
   float u1,float v1,float u2,float v2,Direction facing,int color,int light){
  calls++;if(calls==throwAt)throw new IllegalStateException("deliberate second-face failure");
  faces.add(new Face(new float[]{x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4},facing,color,light));
 }
 void pop(){depth--;}
}
final class StoredMatrixTransformations {void transform(GraphicsHolder holder,Object offset){holder.depth++;}}
enum Direction {NORTH,SOUTH;Direction getOpposite(){return this==NORTH?SOUTH:NORTH;}}
enum QueuedRenderLayer {EXTERIOR}
final class MainRenderer {
 interface Draw {void accept(GraphicsHolder holder,Object offset);}
 static final List<Draw> queue=new ArrayList<>();
 static void scheduleRender(Object texture,boolean translucent,QueuedRenderLayer layer,Draw draw){
  if(translucent || layer!=QueuedRenderLayer.EXTERIOR)throw new AssertionError("wrong backing render queue");
  queue.add(draw);
 }
 static void flush(GraphicsHolder holder){List<Draw> pending=new ArrayList<>(queue);queue.clear();for(Draw draw:pending)draw.accept(holder,null);}
}
final class BlockState {}
'''


CACHE_HARNESS = '''package com.lx862.jcm.mod.scripting.psd;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
final class PSDWindowBounds {
 static int computations,generation;
__CACHE_FIELDS__
__WINDOW__
__CACHE_RESET__
 private static Bounds compute(BlockState state,Direction facing){computations++;return new Bounds(generation);}
 static final class Bounds {final int generation;Bounds(int generation){this.generation=generation;}}
}
'''


def method(source, signature):
    start = source.index(signature)
    depth = 0
    for index in range(source.index('{', start), len(source)):
        if source[index] == '{':
            depth += 1
        elif source[index] == '}':
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise ValueError('Unterminated production method: ' + signature)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    java_root = repo / 'fabric/src/main/java/com/lx862/jcm/mod/scripting/psd'
    renderer = (java_root / 'PSDScriptRenderer.java').read_text(encoding='utf8')
    bounds = (java_root / 'PSDWindowBounds.java').read_text(encoding='utf8')
    backing = method(renderer, '    private static void drawBacking(')
    renderer_reset = method(renderer, '    public static void reset()')
    window = method(bounds, '    static Bounds window(')
    cache_reset = method(bounds, '    static void reset()')
    # The selected production branch is Fabric 1.20+, where this clear is compiled in.
    cache_reset = '\n'.join(line for line in cache_reset.splitlines() if not line.lstrip().startswith('#'))
    constants = []
    for name in ('BACKING_COLOR', 'TRACK_SIDE_COLOR', 'TRACK_BACK_Z', 'CARRIER_B_Z'):
        match = re.search(r'    private static final (?:int|float) ' + name + r'\s*=.*?;', renderer)
        if match is None:
            raise AssertionError('Missing production backing constant: ' + name)
        constants.append(match.group())
    cache_fields = []
    for name in ('CACHE_LIMIT', 'CACHE'):
        match = re.search(r'    private static final [^\n]+\b' + name + r'\s*=.*?;', bounds)
        if match is None:
            raise AssertionError('Missing production cache field: ' + name)
        cache_fields.append(match.group())

    def tool(name):
        home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if home else name

    def run_case(label, backing_method, reset_method):
        folder = args.build.resolve() / 'psd-render-resources-test' / label
        classes = folder / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        contents = dict(STUBS)
        contents['com/lx862/jcm/mod/scripting/psd/PSDScriptRenderer.java'] = (HARNESS
            .replace('__CONSTANTS__', '\n'.join(constants))
            .replace('__DRAW_BACKING__', backing_method)
            .replace('__RENDERER_RESET__', renderer_reset))
        contents['com/lx862/jcm/mod/scripting/psd/PSDWindowBounds.java'] = (CACHE_HARNESS
            .replace('__CACHE_FIELDS__', '\n'.join(cache_fields))
            .replace('__WINDOW__', window)
            .replace('__CACHE_RESET__', reset_method))
        sources = []
        for name, content in contents.items():
            target = folder / 'src' / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding='utf8')
            sources.append(target)
        argfile = folder / 'sources.txt'
        argfile.write_text('\n'.join('"' + source.as_posix() + '"' for source in sources), encoding='utf8')
        subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '--release', '17',
                        '-d', str(classes), '@' + str(argfile)], check=True)
        return subprocess.run([tool('java'), '-cp', str(classes),
                               'com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer'], capture_output=True, text=True)

    result = run_case('production', backing, cache_reset)
    if result.returncode:
        raise AssertionError(result.stdout + result.stderr)
    print(result.stdout.strip())
    pop_after_draw, count = re.subn(r'}\s*finally\s*{\s*graphicsHolderNew\.pop\(\);\s*}',
                                    '} finally {}\n                graphicsHolderNew.pop();', backing)
    if count != 1:
        raise AssertionError('Expected exactly one guaranteed backing stack pop')
    mutants = [
        ('full-width-backing', backing.replace('drawTextureInWorld(left,', 'drawTextureInWorld(0,')
         .replace(', right,', ', 1,').replace(', left,', ', 0,'), cache_reset, 'backing left boundary'),
        ('pop-outside-finally', pop_after_draw, cache_reset, 'matrix stack leaked after second backing face threw'),
        ('stale-resource-cache', backing, cache_reset.replace('CACHE.clear();', ''), 'resource reload reused stale pane geometry'),
    ]
    for label, draw, reset, failure in mutants:
        if draw == backing and reset == cache_reset:
            raise AssertionError('Mutation did not change production source: ' + label)
        result = run_case(label, draw, reset)
        if result.returncode == 0 or failure not in result.stderr:
            raise AssertionError(label + ' did not fail its regression: ' + result.stdout + result.stderr)
        print('Mutation check: ' + label + ' fails the same production regression as expected')


if __name__ == '__main__':
    main()
