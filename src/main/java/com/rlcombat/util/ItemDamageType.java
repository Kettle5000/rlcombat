package com.rlcombat.util;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;

public interface ItemDamageType {

	Holder<DamageType> getDamageType();
	void setDamageType(Holder<DamageType> value);
	
}
