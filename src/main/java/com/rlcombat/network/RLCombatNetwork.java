package com.rlcombat.network;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class RLCombatNetwork {
	private static final ResourceLocation NAME = new ResourceLocation("rlcombat", "network");

    public static SimpleChannel channel;

    public static void init() {
        channel = NetworkRegistry.ChannelBuilder.named(NAME)
                .clientAcceptedVersions(s -> Objects.equals(s, "3"))
                .serverAcceptedVersions(s -> Objects.equals(s, "3"))
                .networkProtocolVersion(() -> "3")
                .simpleChannel();
        
        channel.messageBuilder(AttackHoldPacket.class, 1)
        .decoder(AttackHoldPacket::decode)
        .encoder(AttackHoldPacket::encode)
        .consumerMainThread(AttackHoldPacket::handle)
        .add();
        
        channel.messageBuilder(SyncDamageTypeMapPacket.class, 2)
        .decoder(SyncDamageTypeMapPacket::decode)
        .encoder(SyncDamageTypeMapPacket::encode)
        .consumerMainThread(SyncDamageTypeMapPacket::handle)
        .add();
        
        channel.messageBuilder(SyncArmorResistanceMapPacket.class, 3)
        .decoder(SyncArmorResistanceMapPacket::decode)
        .encoder(SyncArmorResistanceMapPacket::encode)
        .consumerMainThread(SyncArmorResistanceMapPacket::handle)
        .add();
        
    }
}
