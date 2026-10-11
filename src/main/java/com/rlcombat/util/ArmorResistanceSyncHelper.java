package com.rlcombat.util;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ArmorResistanceSyncHelper {
	public static void sync(Map<ResourceLocation, Map<ResourceLocation, Float>> map) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        RegistryAccess registryAccess = mc.level.registryAccess();

        // Clear all existing resistances on the client
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ArmorDamageType armor) {
                armor.setResistances(new HashMap<>());
            }
        }

        // Apply the received mapping
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Float>> entry : map.entrySet()) {
            ResourceLocation itemId = entry.getKey();
            Map<ResourceLocation, Float> resistances = entry.getValue();

            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR || !(item instanceof ArmorDamageType armor)) {
                continue;
            }

            for (Map.Entry<ResourceLocation, Float> resEntry : resistances.entrySet()) {
                ResourceLocation damageTypeId = resEntry.getKey();
                float resistance = resEntry.getValue();

                ResourceKey<DamageType> damageTypeKey =
                        ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);

                Holder<DamageType> damageTypeHolder =
                        registryAccess
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolder(damageTypeKey)
                                .orElse(null);

                if (damageTypeHolder == null) {
                    // Damage type not present on the client – skip
                    continue;
                }

                armor.getResistances().put(damageTypeHolder, resistance);
            }
        }
    }
}
