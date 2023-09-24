package com.github.may2beez.farmhelperv2.event;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ReceivePacketEvent extends Event {
    public Packet<?> packet;
    public ReceivePacketEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
