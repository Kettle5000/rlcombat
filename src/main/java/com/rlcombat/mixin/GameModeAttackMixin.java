package com.rlcombat.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rlcombat.HandPlatform;
import com.rlcombat.RLCombatMod;


@Mixin(MultiPlayerGameMode.class)
public class GameModeAttackMixin {
	
	@Unique
	private static int previous_index = 0;

    @Inject(method = "attack", at = @At(target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V", value = "INVOKE"), cancellable = true)
    public void attack(Player player, Entity entity, CallbackInfo ci) {
        RLCombatMod.Data data = RLCombatMod.get(player);
        if(data.doOverride) {
            HandPlatform.attack(player, entity);
            ci.cancel();
        }
    }
}