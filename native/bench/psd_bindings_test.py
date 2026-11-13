"""Exercise production PSD instance invalidation without Minecraft dependencies.

Stubs model world identity and block replacement; binding cache and both instance
implementations are the real sources. Tests cover same-id source changes, surface
changes, unbinding, and reconnecting to identical coordinates in a different world.
"""
import argparse
import os
from pathlib import Path
import subprocess

STUBS = {
    'org/mtr/mapping/holder/BlockPos.java': """package org.mtr.mapping.holder;
public record BlockPos(int x, int y, int z) {
 public int getX(){return x;}public int getY(){return y;}public int getZ(){return z;}
}""",
    'org/mtr/mapping/holder/BlockState.java': """package org.mtr.mapping.holder;
public class BlockState {
    public static class Block { public Object data; Block(Object data) { this.data = data; } }
    private final Block block;
    public BlockState(Object block) { this.block = new Block(block); }
    public Block getBlock() { return block; }
}""",
    'org/mtr/mapping/holder/World.java': """package org.mtr.mapping.holder;
public class World {
    public Object block;
    public BlockState getBlockState(BlockPos pos) { return new BlockState(block); }
    public static World cast(ClientWorld world) { return world; }
}""",
    'org/mtr/mapping/holder/ClientWorld.java': """package org.mtr.mapping.holder;
public class ClientWorld extends World { public final Object data = new Object(); }""",
    'org/mtr/mapping/holder/MinecraftClient.java': """package org.mtr.mapping.holder;
public class MinecraftClient {
    private static final MinecraftClient INSTANCE = new MinecraftClient();
    public ClientWorld world;
    public static MinecraftClient getInstance() { return INSTANCE; }
    public ClientWorld getWorldMapped() { return world; }
}""",
    'org/mtr/mod/block/BlockPSDAPGGlassBase.java': 'package org.mtr.mod.block; public class BlockPSDAPGGlassBase {}',
    'org/mtr/mod/block/BlockPSDTop.java': 'package org.mtr.mod.block; public class BlockPSDTop {}',
    'org/mtr/mod/Init.java': """package org.mtr.mod;
public class Init { public static String getWorldId(org.mtr.mapping.holder.World world) { return "test/world"; } }""",
    'com/lx862/jcm/mod/util/JCMLogger.java': """package com.lx862.jcm.mod.util;
public class JCMLogger { public static void warn(String format, Object arg) {} }""",
    'com/lx862/jcm/mod/registry/Networking.java': """package com.lx862.jcm.mod.registry;
public class Networking { public static void sendPacketToServer(Object packet) {} }""",
    'com/lx862/mtrscripting/mod/network/packet/psd/RequestPSDScriptsC2SPacket.java': """package com.lx862.mtrscripting.mod.network.packet.psd;
public record RequestPSDScriptsC2SPacket(String dimension) {}""",
    'com/lx862/jcm/mod/scripting/psd/PSDWrapper.java': """package com.lx862.jcm.mod.scripting.psd;
public class PSDWrapper { public enum PSDType { GLASS, TOP } }""",
    'com/lx862/jcm/mod/scripting/pids/PIDSWrapper.java': 'package com.lx862.jcm.mod.scripting.pids; public class PIDSWrapper {}',
    'com/lx862/jcm/mod/scripting/psd/PSDScriptContext.java': """package com.lx862.jcm.mod.scripting.psd;
public class PSDScriptContext {
    private final String name;
    public PSDScriptContext(String name) { this.name = name; }
    public String getName() { return name; }
}""",
    'com/lx862/mtrscripting/core/primitive/ParsedScript.java': 'package com.lx862.mtrscripting.core.primitive; public class ParsedScript {}',
    'com/lx862/mtrscripting/core/primitive/ScriptInstance.java': """package com.lx862.mtrscripting.core.primitive;
import com.lx862.jcm.mod.scripting.psd.PSDScriptContext;
public abstract class ScriptInstance<T> {
    private final PSDScriptContext context;
    public ScriptInstance(PSDScriptContext context, ParsedScript script) { this.context = context; }
    public PSDScriptContext getContextObject() { return context; }
    public abstract boolean shouldInvalidate();
}""",
    'com/lx862/mtrscripting/core/util/render/ScriptRenderManager.java': """package com.lx862.mtrscripting.core.util.render;
public class ScriptRenderManager { public void takeFrom(ScriptRenderManager other) {} }""",
    'com/lx862/mtrscripting/core/util/sound/ScriptSoundManager.java': """package com.lx862.mtrscripting.core.util.sound;
public class ScriptSoundManager { public void takeFrom(ScriptSoundManager other) {} }""",
    'com/lx862/mtrscripting/core/util/ScriptVector3f.java': """package com.lx862.mtrscripting.core.util;
public class ScriptVector3f {
    public ScriptVector3f(org.mtr.mapping.holder.BlockPos pos) {}
    public double distance(ScriptVector3f other) { return 0; }
}""",
    'com/lx862/mtrscripting/core/integration/MinecraftClientWrapper.java': """package com.lx862.mtrscripting.core.integration;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
public class MinecraftClientWrapper {
    public static MinecraftClientWrapper localPlayer() { return new MinecraftClientWrapper(); }
    public ScriptVector3f pos() { return new ScriptVector3f(null); }
}""",
    'com/lx862/mtrscripting/mod/gui/hud/SortableScriptInstance.java': """package com.lx862.mtrscripting.mod.gui.hud;
public interface SortableScriptInstance { int getSortScore(); }""",
    'org/mtr/mapping/registry/PacketHandler.java': """package org.mtr.mapping.registry;
import org.mtr.mapping.tool.PacketBufferSender;
public abstract class PacketHandler {public abstract void write(PacketBufferSender b);public void runClient(){}}""",
    'org/mtr/mapping/tool/PacketBufferSender.java': """package org.mtr.mapping.tool;
import java.io.*;
// Match MTR Mapping's logical wire format: fixed-width int character counts
// followed by UTF-16 chars, not Minecraft's VarInt/UTF-8 string encoding.
public final class PacketBufferSender {
 private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
 private final DataOutputStream data=new DataOutputStream(bytes);
 public void writeInt(int value){try{data.writeInt(value);}catch(IOException e){throw new UncheckedIOException(e);}}
 public void writeBoolean(boolean value){try{data.writeBoolean(value);}catch(IOException e){throw new UncheckedIOException(e);}}
 public void writeString(String value){writeInt(value.length());try{for(int i=0;i<value.length();i++)data.writeChar(value.charAt(i));}catch(IOException e){throw new UncheckedIOException(e);}}
 public byte[] bytes(){return bytes.toByteArray();}
}""",
    'org/mtr/mapping/tool/PacketBufferReceiver.java': """package org.mtr.mapping.tool;
import java.io.*;
public final class PacketBufferReceiver {
 private final DataInputStream data;
 public PacketBufferReceiver(byte[] bytes){data=new DataInputStream(new ByteArrayInputStream(bytes));}
 public int readInt(){try{return data.readInt();}catch(IOException e){throw new UncheckedIOException(e);}}
 public boolean readBoolean(){try{return data.readBoolean();}catch(IOException e){throw new UncheckedIOException(e);}}
 public String readString(){int count=readInt();char[] chars=new char[count];try{for(int i=0;i<count;i++)chars[i]=data.readChar();}catch(IOException e){throw new UncheckedIOException(e);}return new String(chars);}
}""",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'psd-bindings-test'
    work.mkdir(parents=True, exist_ok=True)
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    base = repo / 'fabric/src/main/java'
    for name in ['PSDBinding', 'PSDClientBindings', 'PSDScriptInstance', 'PSDPidsScriptInstance']:
        sources.append(base / ('com/lx862/jcm/mod/scripting/psd/' + name + '.java'))
    sources += [base / 'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
                base / 'com/lx862/mtrscripting/mod/network/packet/psd/PSDScriptsS2CPacket.java',
                Path(__file__).with_name('java') / 'PSDBindingsTest.java',
                Path(__file__).with_name('java') / 'PSDScriptsPacketTest.java']
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')
    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name
    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.PSDBindingsTest'], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.PSDScriptsPacketTest'], check=True)


if __name__ == '__main__':
    main()
