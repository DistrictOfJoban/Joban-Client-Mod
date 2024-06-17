package com.lx862.jcm.mod.network;

import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.AbstractCollection;
import java.util.function.Consumer;

public abstract class JCMPacketHandlerHelper {
    public static void readArray(PacketBufferReceiver packetBufferReceiver, Consumer<Integer> callback) {
        int size = packetBufferReceiver.readInt();
        for(int i = 0; i < size; i++) {
            callback.accept(i);
        }
    }

    public static <T> void writeArray(PacketBufferSender packetBufferSender, T[] arr, Consumer<Integer> callback) {
        packetBufferSender.writeInt(arr.length);
        for(int i = 0; i < arr.length; i++) {
            callback.accept(i);
        }
    }

    public static void writeArray(PacketBufferSender packetBufferSender, boolean[] arr, Consumer<Integer> callback) {
        packetBufferSender.writeInt(arr.length);
        for(int i = 0; i < arr.length; i++) {
            callback.accept(i);
        }
    }

    public static <T> void writeArray(PacketBufferSender packetBufferSender, AbstractCollection<T> arr, Consumer<T> callback) {
        packetBufferSender.writeInt(arr.size());
        for(T item : arr) {
            callback.accept(item);
        }
    }
}
