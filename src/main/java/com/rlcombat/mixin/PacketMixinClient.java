package com.rlcombat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rlcombat.Packet;
import com.rlcombat.RLCombatMod;

@Mixin(ServerboundInteractPacket.class)
public class PacketMixinClient {

    @Inject(method = "<init>(IZLnet/minecraft/network/protocol/game/ServerboundInteractPacket$Action;)V", at = @At(value = "RETURN"))
    public void init(int i, boolean bl, ServerboundInteractPacket.Action action, CallbackInfo ci) {
    	Minecraft instance = Minecraft.getInstance();
        if(instance.player != null) {
            ((Packet)this).set(RLCombatMod.get(instance.player).doOverride);
        }
    }
}
