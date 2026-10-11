package com.rlcombat.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.rlcombat.util.ArmorResistanceSyncHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public class SyncArmorResistanceMapPacket {
	/**
     * itemId → (damageTypeId → resistance)
     */
    private final Map<ResourceLocation, Map<ResourceLocation, Float>> map;

    public SyncArmorResistanceMapPacket(Map<ResourceLocation, Map<ResourceLocation, Float>> map) {
        this.map = map;
    }

    // Encoder
    public static void encode(SyncArmorResistanceMapPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.map.size());

        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Float>> entry : packet.map.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            Map<ResourceLocation, Float> resistances = entry.getValue();

            buf.writeResourceLocation(itemId);
            buf.writeVarInt(resistances.size());

            for (Map.Entry<ResourceLocation, Float> resEntry : resistances.entrySet()) {
                buf.writeResourceLocation(resEntry.getKey()); // damage type id
                buf.writeFloat(resEntry.getValue());          // resistance value
            }
        }
    }

    // Decoder
    public static SyncArmorResistanceMapPacket decode(FriendlyByteBuf buf) {
        int outerSize = buf.readVarInt();
        Map<ResourceLocation, Map<ResourceLocation, Float>> map = new HashMap<>(outerSize);

        for (int i = 0; i < outerSize; i++) {
            ResourceLocation itemId = buf.readResourceLocation();
            int innerSize = buf.readVarInt();

            Map<ResourceLocation, Float> resistances = new HashMap<>(innerSize);
            for (int j = 0; j < innerSize; j++) {
                ResourceLocation damageTypeId = buf.readResourceLocation();
                float resistance = buf.readFloat();
                resistances.put(damageTypeId, resistance);
            }

            map.put(itemId, resistances);
        }

        return new SyncArmorResistanceMapPacket(map);
    }

    // Handler
    public static void handle(SyncArmorResistanceMapPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Replace this with whatever helper you prefer
            ArmorResistanceSyncHelper.sync(packet.map);
            // or directly:
            // ArmorDamageResistanceReloadListener.client_map = packet.map;
        });
        ctx.get().setPacketHandled(true);
    }
}
