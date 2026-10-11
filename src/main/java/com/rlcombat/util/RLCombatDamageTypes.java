package com.rlcombat.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class RLCombatDamageTypes {
	
	public static final ResourceKey<DamageType> SLASHING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("rlcombat", "slashing"));
	public static final ResourceKey<DamageType> BLUNT = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("rlcombat", "blunt"));
	public static final ResourceKey<DamageType> PIERCING = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("rlcombat", "piercing"));

    public static DamageSource getDamageSource(Level level, ResourceKey<DamageType> type, Entity entity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), entity, entity);
    }
    
}
