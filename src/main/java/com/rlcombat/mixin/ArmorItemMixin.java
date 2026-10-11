package com.rlcombat.mixin;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.common.aliasing.qual.Unique;
import org.spongepowered.asm.mixin.Mixin;

import com.rlcombat.util.ArmorDamageType;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ArmorItem;

@Mixin(ArmorItem.class)
public class ArmorItemMixin implements ArmorDamageType{
	
	@Unique private Map<Holder<DamageType>, Float> rl_resistances = new HashMap<Holder<DamageType>, Float>();

	public Map<Holder<DamageType>, Float> getResistances() {
		return this.rl_resistances;
	}

	public float getResistance(Holder<DamageType> value) {
		return this.rl_resistances.getOrDefault(value, 1.0f);
	}

	public void setResistances(Map<Holder<DamageType>, Float> value) {
		this.rl_resistances = value;
	}

}
