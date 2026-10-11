package com.rlcombat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@ModifyVariable(method = "pick", at = @At(value = "STORE", ordinal = 2), ordinal = 2)
	private double modifyBlockDistance(double original, float partialTicks,@Share("arg") LocalDoubleRef argRef) {
		Minecraft instance = Minecraft.getInstance();
		HitResult hit = instance.hitResult;
		if (hit instanceof BlockHitResult bhr) {
			BlockState state = instance.level.getBlockState(bhr.getBlockPos());
			
			if ((state.canBeReplaced() || state.getBlock() instanceof FlowerBlock) && !state.isAir() && state.getFluidState().isEmpty()) {
				argRef.set(original);
			} else {
				argRef.set(0d);
			}
		}
		return original;
	}
	
	@ModifyArg(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;"
			+ "getEntityHitResult(" + "Lnet/minecraft/world/entity/Entity;" + "Lnet/minecraft/world/phys/Vec3;"
			+ "Lnet/minecraft/world/phys/Vec3;" + "Lnet/minecraft/world/phys/AABB;" + "Ljava/util/function/Predicate;"
			+ "D)" + "Lnet/minecraft/world/phys/EntityHitResult;"), index = 5)
	private double modifyEntityRayDistance(double original, @Share("arg") LocalDoubleRef ref) {
		if (ref.get() > 0d) {
			return ref.get();
		}
		return original;
	}
	
	@Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 1))
	private double redirectDistanceToSqr(Vec3 from, Vec3 to, @Local(ordinal = 2) double d1, @Local(ordinal = 1)double entityReach,@Local(ordinal = 1) Entity entity1, @Share("arg") LocalDoubleRef shared) {
		if (shared.get() > 0d && entity1 != null) {
			Minecraft instance = Minecraft.getInstance();
			instance.hitResult = null;
			return Math.min(entityReach * entityReach, d1);
		}
		double vanilla = from.distanceToSqr(to);
		return vanilla;
	}
	

//problem is d1 changes so it never hits an entity.
}
