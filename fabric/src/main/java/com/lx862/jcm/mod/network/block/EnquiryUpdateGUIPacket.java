package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.data.Entry;
import com.lx862.jcm.mod.render.gui.screen.EnquiryScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

import java.util.ArrayList;
import java.util.List;

public class EnquiryUpdateGUIPacket extends PacketHandler {

    private String stationName;
    private long finalFare;
    private String formattedDateTime;
    private List<Entry> entries = new ArrayList<>();
    private final int entryCount;
    private int balance;

    public EnquiryUpdateGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.entryCount = packetBufferReceiver.readInt();
        for (int i = 0; i < entryCount; i++) {
            this.stationName = packetBufferReceiver.readString();
            this.finalFare = packetBufferReceiver.readLong();
            this.formattedDateTime = packetBufferReceiver.readString();
            this.balance = packetBufferReceiver.readInt();
            entries.add(new Entry(stationName, finalFare, formattedDateTime, balance));
        }
    }

    public EnquiryUpdateGUIPacket(List<Entry> entries) {
        this.entries = entries;
        this.entryCount = entries.size();
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeInt(entries.size());
        for (Entry entry : entries) {
            packetBufferSender.writeString(entry.station());
            packetBufferSender.writeLong(entry.fare());
            packetBufferSender.writeString(entry.date());
            packetBufferSender.writeInt(entry.balance());
        }
    }

    @Override
    public void runClient() {
        MinecraftClient.getInstance().openScreen(new Screen(new EnquiryScreen(false, entries)));
    }
}
