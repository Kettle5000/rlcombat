package com.rlcombat.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.rlcombat.util.SyncDamageTypeHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public class SyncDamageTypeMapPacket {

    private final Map<ResourceLocation, ResourceLocation> map;

    public SyncDamageTypeMapPacket(Map<ResourceLocation, ResourceLocation> map) {
        this.map = map;
    }

    // Encoder
    public static void encode(SyncDamageTypeMapPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.map.size());
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : packet.map.entrySet()) {
        	if (entry.getValue() == null) {
        		System.out.println("Item: " + entry.getKey().toString() + " has a null value for some reason");
        	}
            buf.writeResourceLocation(entry.getKey());   // item id
            buf.writeResourceLocation(entry.getValue()); // damage type id
        }
    }

    // Decoder
    public static SyncDamageTypeMapPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, ResourceLocation> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation itemId = buf.readResourceLocation();
            ResourceLocation damageId = buf.readResourceLocation();
            map.put(itemId, damageId);
        }
        return new SyncDamageTypeMapPacket(map);
    }

    // Handler
    public static void handle(SyncDamageTypeMapPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
        	SyncDamageTypeHelper.sync(packet.map);
        });
        ctx.get().setPacketHandled(true);
    }
}