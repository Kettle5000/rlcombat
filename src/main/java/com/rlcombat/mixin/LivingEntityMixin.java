package com.rlcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.rlcombat.Config;
import org.spongepowered.asm.mixin.injection.Constant;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
	private int modifyShieldDelay(int original) {
		return Config.shield_raise_delay;
	}
	
}