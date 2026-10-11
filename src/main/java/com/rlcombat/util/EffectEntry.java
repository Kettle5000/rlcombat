package com.rlcombat.util;

import net.minecraft.world.effect.MobEffect;

public record EffectEntry(
	    MobEffect effect,
	    int duration,
	    int amplifier
	) {}
