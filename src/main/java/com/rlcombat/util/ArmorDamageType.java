package com.rlcombat.util;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;

public interface ArmorDamageType {
	
	Map<Holder<DamageType>, Float> getResistances();
	float getResistance(Holder<DamageType> value);
	void setResistances(Map<Holder<DamageType>, Float> value);
	
}
