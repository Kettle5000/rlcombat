package com.rlcombat.util;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class SyncDamageTypeHelper {

	/*This is meant to only fire on client side, be careful
	 *
	 */
	public static void sync(Map<ResourceLocation, ResourceLocation> map) {
		// Get the client's registry access
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        var damageTypeRegistry = mc.level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);

        // Clear previous assignments
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ItemDamageType damageTypeItem) {
                damageTypeItem.setDamageType(null);
            }
        }

        // Apply the received mapping
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : map.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            ResourceLocation damageTypeId = entry.getValue();

            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) continue;

            if (item instanceof ItemDamageType damageTypeItem) {
                damageTypeRegistry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId))
                        .ifPresent(damageTypeItem::setDamageType);
            }
        }
	}
	
}
