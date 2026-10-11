package com.rlcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.rlcombat.util.ItemDamageType;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(DamageSources.class)
public class DamageSourcesMixin {

	@ModifyReturnValue(method = "playerAttack", at = @At("RETURN"))
	private DamageSource rlcombat$modifyPlayerAttack(DamageSource original, Player player) {
		ItemStack stack = player.getMainHandItem();

		if (stack.getItem() instanceof ItemDamageType damageTypeItem) {
			Holder<DamageType> holder = damageTypeItem.getDamageType();

			if (holder != null) {
				return new DamageSource(holder, player);
			}
		}

		return original;
	}
	
	@ModifyReturnValue(method = "mobAttack", at = @At("RETURN"))
	private DamageSource rlcombat$modifyMobAttack(DamageSource original, LivingEntity entity) {
		ItemStack stack = entity.getMainHandItem();

		if (stack.getItem() instanceof ItemDamageType damageTypeItem) {
			Holder<DamageType> holder = damageTypeItem.getDamageType();

			if (holder != null) {
				return new DamageSource(holder, entity);
			}
		}

		return original;
	}

}
