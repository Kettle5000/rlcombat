package com.rlcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.rlcombat.util.ItemDamageType;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.TieredItem;

@Mixin(TieredItem.class)
public class TieredItemMixin implements ItemDamageType {
	
	@Unique private Holder<DamageType> default_type;

	public Holder<DamageType> getDamageType() {
		return this.default_type;
	}

	public void setDamageType(Holder<DamageType> value) {
		this.default_type = value;
	}

}
