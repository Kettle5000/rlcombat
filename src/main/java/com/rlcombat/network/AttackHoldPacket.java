package com.rlcombat.network;

import java.util.function.Supplier;

import com.rlcombat.Config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class AttackHoldPacket {
    private final boolean value;

    public AttackHoldPacket(boolean value) {
        this.value = value;
    }

    public static void encode(AttackHoldPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.value);
    }

    public static AttackHoldPacket decode(FriendlyByteBuf buf) {
        return new AttackHoldPacket(buf.readBoolean());
    }

    public static void handle(AttackHoldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Config.isAttackHeld = msg.value;
        });
        ctx.get().setPacketHandled(true);
    }
}