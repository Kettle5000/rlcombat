package com.rlcombat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rlcombat.data.ArmorDamageResistanceReloadListener;
import com.rlcombat.data.DamageTypeItemReloadListener;
import com.rlcombat.network.RLCombatNetwork;
import com.rlcombat.network.SyncDamageTypeMapPacket;

import net.minecraftforge.fml.config.ModConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(RLCombatMod.MOD_ID)
public class RLCombatMod {   // changed name since it conflicted with net.minecraftforge.fml.common.Mod

    public static final String MOD_ID = "rlcombat";   // maybe we should change the name of this mod?
    public static final Logger LOGGER = LogManager.getLogger();

    public static Map<UUID, Data> swing = new HashMap<>();
    public static Map<UUID, Data> swingLocal = new HashMap<>();
    public static final ResourceLocation animation = new ResourceLocation(MOD_ID, "animation");

    public RLCombatMod() {
    	MinecraftForge.EVENT_BUS.register(this);
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		MinecraftForge.EVENT_BUS.addListener(this::addReloadListener);
		MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdated);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "RLCombat.toml");
		RLCombatNetwork.init();
    }
    
    private void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DamageTypeItemReloadListener(event.getRegistryAccess()));
        event.addListener(new ArmorDamageResistanceReloadListener(event.getRegistryAccess()));
    }
    
    public void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
			DamageTypeItemReloadListener.loadDamageTypes(event.getRegistryAccess());
		}
    }
    
    public static Data get(Player entity) {
        if (entity.isLocalPlayer()) {
            if (!swingLocal.containsKey(entity.getUUID())) {
                swingLocal.put(entity.getUUID(), new Data());
            }
            return swingLocal.get(entity.getUUID());
        } else {
            if (!swing.containsKey(entity.getUUID())) {
                swing.put(entity.getUUID(), new Data());
            }
            return swing.get(entity.getUUID());
        }
    }

    public static class Data {
        //
        public boolean doOverride;
        //
        public int missTime;
        //
        public int swingTime;
        public boolean swinging;
        public float attackAnim;
        public float attackAnim_;
        public int attackStrengthTicker;
        public InteractionHand swingingArm;
        //
        public int ticksSinceLastActiveStack;
        public InteractionHand handOfLastActiveStack;
    }
}
