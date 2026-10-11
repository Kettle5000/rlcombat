package com.rlcombat.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rlcombat.Listener;
import com.rlcombat.Packet;
import com.rlcombat.RLCombatMod;

//* Current Mixin is allowing players to ignore iframes, we MUST REMOVE THIS.

@Mixin(targets = "net/minecraft/server/network/ServerGamePacketListenerImpl$1")
public class ListenerImplMixin implements Listener {

    @Unique
    public ServerPlayer player;
    @Unique
    public ServerboundInteractPacket serverboundInteractPacket;

    @Dynamic
    @Inject(method = "onAttack()V", at = @At(target = "Lnet/minecraft/server/level/ServerPlayer;attack(Lnet/minecraft/world/entity/Entity;)V", value = "INVOKE", shift = At.Shift.BEFORE))
    public void onAttack(CallbackInfo ci) {
        if (((Packet) this.serverboundInteractPacket).get()) {
            RLCombatMod.Data data = RLCombatMod.get(this.player);
            data.doOverride = true;
            this.player = null;
            this.serverboundInteractPacket = null;
        }
    }

    @Override
    public void setPlayer(ServerPlayer serverPlayer) {
        this.player = serverPlayer;
    }

    @Override
    public void setPacket(ServerboundInteractPacket serverboundInteractPacket) {
        this.serverboundInteractPacket = serverboundInteractPacket;
    }
}