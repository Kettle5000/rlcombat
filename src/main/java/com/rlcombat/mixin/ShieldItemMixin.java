package com.rlcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.rlcombat.Config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

@Mixin(ShieldItem.class)
public class ShieldItemMixin {

	@Unique private static String shield_damage = "shield_damage";
	
	public void m_6883_(ItemStack stack, Level level, Entity entity, int slotindex, boolean flag) {
		CompoundTag tag = stack.getOrCreateTag();
		if (tag.contains(shield_damage, 99)) { // we check if it has float
			float dmg = tag.getFloat(shield_damage);
			if (dmg > 0f) {
				float shieldDurability = (float) ((stack.getMaxDamage() > 0) ? stack.getMaxDamage()
						: Config.shield_fallback);
				float shieldProtection = Math.max(Config.shield_mindamage_threshold,
						Math.min(Config.shield_maxdamage_threshold, shieldDurability * Config.shield_durability_scale));
				tag.putFloat(shield_damage, Math.max(dmg - (shieldProtection * 0.025f), 0));
			}
		}
	}
	
}
