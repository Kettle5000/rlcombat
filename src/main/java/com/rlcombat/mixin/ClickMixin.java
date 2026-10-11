package com.rlcombat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.rlcombat.HandPlatform;
import com.rlcombat.RLCombatMod;
import java.util.Objects;

@Debug(export=true)
@Mixin(Minecraft.class)
public abstract class ClickMixin {

	@Shadow
	public LocalPlayer player;
	@Shadow
	public HitResult hitResult;
	@Shadow
	public MultiPlayerGameMode gameMode;
	@Shadow
	private int rightClickDelay;
	@Shadow
	@Final
	public Options options;
	@Shadow
	public ClientLevel level;
	@Shadow
	@Final
	public GameRenderer gameRenderer;

	@Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionResult;consumesAction()Z", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
	public void injectentityoffhand(CallbackInfo ci, @Local(ordinal = 0) InteractionResult interactionResult, @Local Entity entity, @Local InteractionHand interactionhand) {
		if (!interactionResult.consumesAction() && interactionhand == InteractionHand.OFF_HAND) {
			RLCombatMod.Data data = RLCombatMod.get(this.player);
			if (!this.player.isCrouching() && HandPlatform.canUseOffhand(player)
					&& HandPlatform.canSwingHand(this.player, InteractionHand.OFF_HAND)
					&& data.missTime <= 0 && entity.isAttackable()) {
				data.doOverride = true;
				this.gameMode.attack(this.player, entity);
				this.player.swing(InteractionHand.OFF_HAND);
				// Cancel default key behavior
				this.options.keyUse.release();
				ci.cancel();
			}
		}
	}
	
	@Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionResult;consumesAction()Z", ordinal = 3, shift = At.Shift.BEFORE), cancellable = true)
	public void injectmissoffhand(CallbackInfo ci, @Local(ordinal = 0) InteractionResult interactionResult, @Local InteractionHand interactionhand, @Local ItemStack itemstack) {
		if (!interactionResult.consumesAction() && interactionhand == InteractionHand.OFF_HAND) {
			RLCombatMod.Data data = RLCombatMod.get(this.player);
			UseAnim useAnimation = itemstack.getUseAnimation();
			if (useAnimation != UseAnim.NONE) {
				return;
			}

			if (Objects.requireNonNull(this.gameMode).hasMissTime()) {
				data.missTime = 10;
			}
			this.player.swing(InteractionHand.OFF_HAND);
			this.options.keyUse.release();
			ci.cancel();
		}
	}

}