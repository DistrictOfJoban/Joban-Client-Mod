"""GitHub-only production PSD server persistence and batching regression."""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import time
import urllib.request

from psd_bindings_test import STUBS as BINDING_STUBS


STUBS = {name: source for name, source in BINDING_STUBS.items() if name in {
    'com/lx862/jcm/mod/scripting/psd/PSDWrapper.java',
    'org/mtr/mapping/holder/BlockState.java',
    'org/mtr/mod/block/BlockPSDAPGGlassBase.java',
    'org/mtr/mod/block/BlockPSDTop.java',
}}
STUBS.update({
    'org/mtr/mapping/holder/BlockPos.java': '''package org.mtr.mapping.holder;
public record BlockPos(int x,int y,int z) {public int getX(){return x;}public int getY(){return y;}public int getZ(){return z;}}''',
    'org/mtr/mapping/holder/World.java': '''package org.mtr.mapping.holder;
import java.util.*;
public class World {
 public final Map<BlockPos,BlockState> blocks=new HashMap<>();public final List<PlayerEntity> players=new ArrayList<>();
 public boolean loaded=true;public int blockReads;
 public BlockState getBlockState(BlockPos p){blockReads++;return blocks.getOrDefault(p,new BlockState(new Object()));}
 public boolean isChunkLoaded(int x,int z){return loaded;}
}''',
    'org/mtr/mapping/holder/ServerWorld.java': '''package org.mtr.mapping.holder;
public final class ServerWorld extends World {public static ServerWorld cast(World w){return (ServerWorld)w;}}''',
    'org/mtr/mapping/holder/PlayerEntity.java': '''package org.mtr.mapping.holder;
public class PlayerEntity {
 public final Object data;public final World world;
 public PlayerEntity(Object data,World world){this.data=data;this.world=world;}
 public static PlayerEntity cast(ServerPlayerEntity p){return p;}
}''',
    'org/mtr/mapping/holder/ServerPlayerEntity.java': '''package org.mtr.mapping.holder;
public final class ServerPlayerEntity extends PlayerEntity {
 public ServerPlayerEntity(Object data,World world){super(data,world);}
 public World getEntityWorld(){return world;}public boolean isHolding(Object item){return true;}
 public BlockPos getBlockPos(){return new BlockPos(0,0,0);}public Name getName(){return new Name();}
 public static final class Name {public String getString(){return "test";}}
}''',
    'org/mtr/mapping/holder/WorldSavePath.java': '''package org.mtr.mapping.holder;
public final class WorldSavePath {public static WorldSavePath getRootMapped(){return new WorldSavePath();}}''',
    'org/mtr/mapping/holder/MinecraftServer.java': '''package org.mtr.mapping.holder;
import java.nio.file.Path;
public final class MinecraftServer {private final Path root;public MinecraftServer(Path root){this.root=root;}public Path getSavePath(WorldSavePath path){return root;}}''',
    'org/mtr/mapping/mapper/MinecraftServerHelper.java': '''package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.*;import java.util.function.Consumer;
public final class MinecraftServerHelper {public static void iteratePlayers(ServerWorld world,Consumer<PlayerEntity> f){world.players.forEach(f);}}''',
    'org/mtr/mod/Init.java': '''package org.mtr.mod;
public final class Init {public static String getWorldId(org.mtr.mapping.holder.World world){return "test/world";}}''',
    'com/lx862/jcm/mod/registry/Items.java': '''package com.lx862.jcm.mod.registry;
public final class Items {public static final Item PSD_SCRIPT_EDITOR=new Item();public static final class Item {public Object get(){return this;}}}''',
    'com/lx862/jcm/mod/registry/Networking.java': '''package com.lx862.jcm.mod.registry;
import java.util.*;import org.mtr.mapping.holder.PlayerEntity;import com.lx862.mtrscripting.mod.network.packet.psd.PSDScriptsS2CPacket;
public final class Networking {public static final List<PSDScriptsS2CPacket> sent=new ArrayList<>();public static void sendPacketToClient(PlayerEntity p,PSDScriptsS2CPacket packet){sent.add(packet);}}''',
    'com/lx862/jcm/mod/util/JCMLogger.java': '''package com.lx862.jcm.mod.util;
public final class JCMLogger {public static void warn(String s,Object... args){}public static void info(String s,Object... args){}}''',
    'com/lx862/mtrscripting/mod/network/packet/psd/PSDScriptsS2CPacket.java': '''package com.lx862.mtrscripting.mod.network.packet.psd;
import java.util.List;import com.lx862.jcm.mod.scripting.psd.PSDBinding;
public record PSDScriptsS2CPacket(String dimension,List<PSDBinding> bindings) {}''',
})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'psd-server-bindings-test'
    work.mkdir(parents=True, exist_ok=True)
    gson = work / 'gson.jar'
    if not gson.exists():
        for attempt in range(3):
            try:
                urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar', gson)
                break
            except Exception:
                if attempt == 2:
                    raise
                time.sleep(2)
    if hashlib.sha256(gson.read_bytes()).hexdigest() != '57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b':
        raise ValueError('Gson dependency checksum mismatch')
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    base = repo / 'fabric/src/main/java'
    sources += [base / 'com/lx862/jcm/mod/scripting/psd/PSDBinding.java',
                base / 'com/lx862/mtrscripting/mod/impl/mtr/server/PSDServerBindings.java',
                Path(__file__).with_name('java') / 'PSDServerBindingsTest.java']
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-cp', str(gson), '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes) + os.pathsep + str(gson), 'com.lx862.jcm.test.PSDServerBindingsTest'], check=True)


if __name__ == '__main__':
    main()
