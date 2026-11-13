"""Compile real PSD wall/section scanners against small block-world stubs.

Run in GitHub Actions. Exercises all horizontal facings and adjacent independent
door pairs, including world changes which do not pass through a disconnected tick.
"""
import argparse
import os
from pathlib import Path
import subprocess

from psd_bindings_test import STUBS as BINDING_STUBS


STUBS = {name: source for name, source in BINDING_STUBS.items() if name in {
    'com/lx862/jcm/mod/scripting/psd/PSDWrapper.java',
}}
STUBS.update({
    'javax/annotation/Nullable.java': 'package javax.annotation; public @interface Nullable {}',
    'org/mtr/mapping/holder/Axis.java': 'package org.mtr.mapping.holder; public enum Axis {X, Z}',
    'org/mtr/mapping/holder/Direction.java': '''package org.mtr.mapping.holder;
public enum Direction {
 NORTH(0,-1), EAST(1,0), SOUTH(0,1), WEST(-1,0);
 public final int x,z; Direction(int x,int z){this.x=x;this.z=z;}
 public Axis getAxis(){return x==0?Axis.Z:Axis.X;}
 public Direction rotateYClockwise(){return values()[(ordinal()+1)%4];}
 public Direction rotateYCounterclockwise(){return values()[(ordinal()+3)%4];}
 public Direction getOpposite(){return values()[(ordinal()+2)%4];}
}''',
    'org/mtr/mapping/holder/BlockPos.java': '''package org.mtr.mapping.holder;
public record BlockPos(int x,int y,int z) {
 public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
 public BlockPos up(){return new BlockPos(x,y+1,z);}
 public BlockPos down(){return down(1);} public BlockPos down(int n){return new BlockPos(x,y-n,z);}
 public BlockPos offset(Direction d){return offset(d,1);}
 public BlockPos offset(Direction d,int n){return new BlockPos(x+d.x*n,y,z+d.z*n);}
}''',
    'org/mtr/mapping/holder/BlockState.java': '''package org.mtr.mapping.holder;
import java.util.*; import org.mtr.mod.block.IBlock.Property;
public final class BlockState {
 public final Map<Property<?>,Object> values=new HashMap<>(); private final Block block;
 public BlockState(Object block){this.block=new Block(block);}
 public Block getBlock(){return block;}
 public <T> BlockState with(Property<T> property,T value){values.put(property,value);return this;}
 public static final class Block {public final Object data;Block(Object data){this.data=data;}}
}''',
    'org/mtr/mapping/holder/BlockEntity.java': '''package org.mtr.mapping.holder;
public final class BlockEntity {public final Object data;public BlockEntity(Object data){this.data=data;}}''',
    'org/mtr/mapping/holder/World.java': '''package org.mtr.mapping.holder;
import java.util.*;
public final class World {
 public final Object data=new Object();
 public int reads;
 public final Map<BlockPos,BlockState> blocks=new HashMap<>();
 public final Map<BlockPos,BlockEntity> entities=new HashMap<>();
 public BlockState getBlockState(BlockPos p){reads++;return blocks.getOrDefault(p,new BlockState(new Object()));}
 public BlockEntity getBlockEntity(BlockPos p){return entities.get(p);}
}''',
    'org/mtr/mod/block/IBlock.java': '''package org.mtr.mod.block;
import org.mtr.mapping.holder.BlockState;
public interface IBlock {
 final class Property<T> {} enum DoubleBlockHalf {LOWER,UPPER} enum EnumSide {LEFT,RIGHT,SINGLE}
 Property<DoubleBlockHalf> HALF=new Property<>();
 @SuppressWarnings("unchecked") static <T> T getStatePropertySafe(BlockState s,Property<T> p){return (T)s.values.get(p);}
}''',
    'org/mtr/mod/block/BlockPSDAPGBase.java': '''package org.mtr.mod.block;
import org.mtr.mapping.holder.Direction;
public class BlockPSDAPGBase {public static final IBlock.Property<Direction> FACING=new IBlock.Property<>();}''',
    'org/mtr/mod/block/BlockPSDAPGGlassBase.java': 'package org.mtr.mod.block; public class BlockPSDAPGGlassBase extends BlockPSDAPGBase {}',
    'org/mtr/mod/block/BlockPSDAPGGlassEndBase.java': 'package org.mtr.mod.block; public class BlockPSDAPGGlassEndBase extends BlockPSDAPGBase {}',
    'org/mtr/mod/block/BlockPSDAPGDoorBase.java': '''package org.mtr.mod.block;
public class BlockPSDAPGDoorBase extends BlockPSDAPGBase {
 public static final IBlock.Property<IBlock.EnumSide> SIDE=new IBlock.Property<>();
 public static final IBlock.Property<Boolean> UNLOCKED=new IBlock.Property<>();
 public static final class BlockEntityBase {
  private final double value;public BlockEntityBase(double value){this.value=value;}public double getDoorValue(){return value;}
 }
}''',
    'org/mtr/mod/block/BlockPSDTop.java': '''package org.mtr.mod.block;
import org.mtr.mapping.holder.Direction;
public final class BlockPSDTop {public static final IBlock.Property<Direction> FACING=BlockPSDAPGBase.FACING;}''',
    'org/mtr/core/data/Position.java': '''package org.mtr.core.data;
public class Position {public double getX(){return 0;}public double getZ(){return 0;}}''',
    'org/mtr/core/data/Platform.java': '''package org.mtr.core.data;
public class Platform {public Position getMidPosition(){return new Position();}}''',
    'org/mtr/core/data/TransportMode.java': 'package org.mtr.core.data; public enum TransportMode {TRAIN}',
    'org/mtr/core/data/Route.java': '''package org.mtr.core.data;
public class Route {public TransportMode getTransportMode(){return TransportMode.TRAIN;}}''',
    'org/mtr/core/operation/CarDetails.java': '''package org.mtr.core.operation;
public class CarDetails {public String getVehicleId(){return "";}}''',
    'org/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList.java': '''package org.mtr.libraries.it.unimi.dsi.fastutil.objects;
public class ObjectArrayList<T> extends java.util.ArrayList<T> {}''',
    'org/mtr/mod/client/CustomResourceLoader.java': '''package org.mtr.mod.client;
import java.util.function.Consumer;import org.mtr.core.data.TransportMode;
public final class CustomResourceLoader {
 public static void getVehicleById(TransportMode mode,String id,Consumer<Details> c){c.accept(new Details());}
 public static final class Details {public Vehicle left(){return null;}}
 public static final class Vehicle {public double getLength(){return 20;}}
}''',
    'com/lx862/jcm/mod/util/JCMLogger.java': '''package com.lx862.jcm.mod.util;
public final class JCMLogger {public static void warn(String message,Object... args){}}''',
    'com/lx862/jcm/mod/scripting/psd/PSDClientBindings.java': '''package com.lx862.jcm.mod.scripting.psd;
public final class PSDClientBindings {
 public static final java.util.Map<org.mtr.mapping.holder.BlockPos,PSDBinding> bindings=new java.util.HashMap<>();
 public static PSDBinding get(org.mtr.mapping.holder.BlockPos p){return bindings.get(p);}
}''',
})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'psd-wall-test'
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    for name in ('PSDBinding', 'PSDRuns', 'PSDWallScanner'):
        sources.append(repo / 'fabric/src/main/java/com/lx862/jcm/mod/scripting/psd' / (name + '.java'))
    sources.append(Path(__file__).with_name('java') / 'PSDWallTest.java')
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.PSDWallTest'], check=True)


if __name__ == '__main__':
    main()
