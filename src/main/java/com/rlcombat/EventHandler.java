package com.rlcombat;

import com.kettle.pml.api.events.FirstAidHurtEvent;
import com.kettle.pml.core.DamageControl;
import com.kettle.pml.util.AdvancedDamageSource;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rlcombat.RLCombatMod.Data;
import com.rlcombat.data.ArmorDamageResistanceReloadListener;
import com.rlcombat.data.DamageTypeItemReloadListener;
import com.rlcombat.network.AttackHoldPacket;
import com.rlcombat.network.RLCombatNetwork;
import com.rlcombat.network.SyncArmorResistanceMapPacket;
import com.rlcombat.network.SyncDamageTypeMapPacket;
import com.rlcombat.util.ArmorDamageType;
import com.rlcombat.util.EffectEntry;
import com.seniors.talents.handler.HandlerCommonConfig;
import com.seniors.talents.registry.RegistryPassives;
import com.seniors.talents.registry.passive.Passive;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class EventHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) { 
    	if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
    		Data data = RLCombatMod.get(serverPlayer);
            data.missTime--;
            data.attackStrengthTicker++;
            data.ticksSinceLastActiveStack++;
    	}
    }
    
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void ShieldBreakDamage(ShieldBlockEvent event) {
		if (!event.isCanceled() && event.getEntity() instanceof Player player) {
			if (!event.getDamageSource().is(DamageTypeTags.BYPASSES_SHIELD)) {
				ItemStack playerItem = player.getUseItem();
				int parryTicks = player.getTicksUsingItem();
				float damage = event.getBlockedDamage();
				float shieldDurability = (float) ((playerItem.getMaxDamage() > 0) ? playerItem.getMaxDamage()
						: Config.shield_fallback);
				float shieldProtection = Math.max(Config.shield_mindamage_threshold,
						Math.min(Config.shield_maxdamage_threshold, shieldDurability * Config.shield_durability_scale));
				CompoundTag tag = playerItem.getOrCreateTag();
				float accumulated = tag.getFloat("shield_damage") + damage;
				tag.putFloat("shield_damage", Math.min(accumulated, shieldProtection));
				boolean parry = false;
				boolean broken = false;
				
				// Parry
				if (parryTicks < (Config.shield_parry_ticks + Config.shield_raise_delay)) {
					player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
							SoundSource.PLAYERS, 1.0F, 0.3F);
					parry = true;
				}
				// Shield broken by damage
				else if (accumulated >= shieldProtection) {
					shieldCooldown((int) Math.max(Config.shield_min_cd, Math.min(Config.shield_max_cd,
							((accumulated - shieldProtection) * Config.shield_multiplier))), player);
					//player.disableShield(true); // We use this inbuilt method
					broken = true;
				}
				// Normal block
				else {
					player.level().broadcastEntityEvent(player, (byte) 29);
				}

				if (parry) {
					if (Config.shield_defender_parry != null) {
						for (EffectEntry entry : Config.shield_defender_parry) {
							player.addEffect(
									new MobEffectInstance(entry.effect(), entry.duration(), entry.amplifier()));
						}
					}
					if (Config.shield_attacker_parry != null && event.getDamageSource().getDirectEntity() instanceof LivingEntity entity) { // only melee
						for (EffectEntry entry : Config.shield_attacker_parry) {
							entity.addEffect(
									new MobEffectInstance(entry.effect(), entry.duration(), entry.amplifier()));
						}
					}
				} else if (broken) {
					if (Config.shield_defender_break != null) {
						for (EffectEntry entry : Config.shield_defender_break) {
							player.addEffect(
									new MobEffectInstance(entry.effect(), entry.duration(), entry.amplifier()));
						}
					}
					if (Config.shield_attacker_break != null && event.getDamageSource().getDirectEntity() instanceof LivingEntity entity) { // only melee
						for (EffectEntry entry : Config.shield_attacker_break) {
							entity.addEffect(
									new MobEffectInstance(entry.effect(), entry.duration(), entry.amplifier()));
						}
					}
				}

			}
		}
	}
    
    
    @SubscribeEvent
    public static void onInfinityArrows(ArrowNockEvent event) {
    	if (event.getBow().getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0 && !event.hasAmmo()) {
    		event.getEntity().startUsingItem(event.getHand());
    		event.setAction(InteractionResultHolder.consume(event.getBow()));
    	}
    }
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
                new AttackHoldPacket(Config.isAttackHeld));
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncDamageTypeMapPacket(
                            DamageTypeItemReloadListener.server_map
                    ));
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncArmorResistanceMapPacket(
                            ArmorDamageResistanceReloadListener.server_map
                    ));
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
                new AttackHoldPacket(Config.isAttackHeld));
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncDamageTypeMapPacket(
                            DamageTypeItemReloadListener.server_map
                    ));
            RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncArmorResistanceMapPacket(
                            ArmorDamageResistanceReloadListener.server_map
                    ));
        }
    }
    
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
        	RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new AttackHoldPacket(Config.isAttackHeld));
        	RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncDamageTypeMapPacket(
                            DamageTypeItemReloadListener.server_map
                    ));
        	RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(() -> player),
            		new SyncArmorResistanceMapPacket(
                            ArmorDamageResistanceReloadListener.server_map
                    ));
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST) //this only works for mobs, players only work if first aid mod is installed
    public static void onRLDamageTaken(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return; // safety
        if (entity instanceof Player) return;
        DamageSource source = event.getSource();
        Holder<DamageType> damageType = source.typeHolder();

        float multiplier = 1.0f;

        // Get random armor slot when attacked
        EquipmentSlot slot = EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, entity.getRandom().nextIntBetweenInclusive(0, 3));
        ItemStack item = entity.getItemBySlot(slot);
        if (!item.isEmpty() && item.getItem() instanceof ArmorDamageType armor) { //this should already check that the itemstack is NOT empty
        	multiplier = armor.getResistance(damageType);
        }
        if (multiplier != 1.0f) {
        	float amount = event.getAmount();
        	if (source instanceof AdvancedDamageSource ads) {
        		DamageControl control = ads.getDamageControl();
        		control.setArmorReduction(control.getArmorReduction() + (amount * (1f - multiplier)));
        	}
            event.setAmount(amount * multiplier);
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST) //highest because its mean to run before armor even
    public static void onFirstAidCompat(FirstAidHurtEvent event) {
    	if (!event.isCanceled() && event.getDamagedSlot() != null) {
    		ItemStack piece = event.getEntity().getItemBySlot(event.getDamagedSlot());
    		if (!piece.isEmpty() && piece.getItem() instanceof ArmorDamageType armor) {
    			DamageSource source = event.getSource();
                Holder<DamageType> damageType = source.typeHolder();
                float resistance = armor.getResistance(damageType); //this should return either 1 or the correct value but never a null
                if (resistance == 1.0f) return;
                event.setAmount(event.getAmount() * resistance); //we multiply directly since it should be value beetwen 1.0 to 0 float
    		}
    	}
    }
    
    
	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		SyncDamageTypeMapPacket packet = new SyncDamageTypeMapPacket(DamageTypeItemReloadListener.server_map);
		SyncArmorResistanceMapPacket packet2 = new SyncArmorResistanceMapPacket(
				ArmorDamageResistanceReloadListener.server_map);

		if (event.getPlayer() != null) {
			// Single player joining
			RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
			RLCombatNetwork.channel.send(PacketDistributor.PLAYER.with(event::getPlayer), packet2);
		} else {
			// /reload → everyone
			RLCombatNetwork.channel.send(PacketDistributor.ALL.noArg(), packet);
			RLCombatNetwork.channel.send(PacketDistributor.ALL.noArg(), packet2);
		}
	}
 
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			if (player == null)
				return;
			//attack held mechanic
			if (player.getAttackStrengthScale(0.0f) >= 0.9f && Config.isAttackHeld) {
				ItemStack stack = player.getMainHandItem();
				if (!stack.isEmpty()) {
					ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
					if (!id.getPath().contains("nunchaku") && mc.options.keyAttack.isDown()) {
						RLCombatMod.Data data = RLCombatMod.get(player);
						if (data.attackStrengthTicker > 10) {
							MultiPlayerGameMode mode = mc.gameMode;
							if (mode != null) {
								HitResult hit = mc.hitResult;
								if (hit instanceof EntityHitResult entityResult) {
									var entity = entityResult.getEntity();
									if (entity.isAttackable() && entity.isAlive()) {
										mode.attack(player, entity);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void renderShieldBreakHUD(RenderGuiOverlayEvent.Post event) {
	    Minecraft mc = Minecraft.getInstance();

	    if (mc.player == null) {
	        return;
	    }

	    Player player = mc.player;

	    /*
	     * Find whether either hand has a shield on cooldown.
	     * the shield cooldown if it is a custom cooldown.
	     */
	    int mainHandCooldown = getShieldCooldown(player, InteractionHand.MAIN_HAND);
	    int offHandCooldown = getShieldCooldown(player, InteractionHand.OFF_HAND);

	    boolean shieldOnCooldown = mainHandCooldown > 0 || offHandCooldown > 0;

	    /*
	     * Only render the normal shield-capacity HUD while actively
	     * using a shield.
	     *
	     * The red broken shield is allowed to remain visible during
	     * cooldown even after the player stops blocking.
	     */
	    if (!player.isUsingItem() && !shieldOnCooldown) {
	        return;
	    }

	    /*
	     * Find the shield to use for calculating capacity.
	     *
	     * While blocking, use the shield currently being used.
	     * Otherwise use whichever shield is on cooldown.
	     */
	    ItemStack shield;

	    if (player.isUsingItem()
	            && player.getUseItem().getItem() instanceof ShieldItem) {

	        shield = player.getUseItem();

	    } else if (mainHandCooldown >= offHandCooldown
	            && player.getMainHandItem().getItem() instanceof ShieldItem) {

	        shield = player.getMainHandItem();

	    } else if (player.getOffhandItem().getItem() instanceof ShieldItem) {

	        shield = player.getOffhandItem();

	    } else {
	        return;
	    }

	    /*
	     * Read accumulated shield damage.
	     */
	    CompoundTag tag = shield.getTag();

	    float shieldDamage = tag != null
	            ? tag.getFloat("shield_damage")
	            : 0.0F;

	    /*
	     * Calculate maximum shield protection.
	     */
	    float shieldDurability = shield.getMaxDamage() > 0
	            ? shield.getMaxDamage()
	            : Config.shield_fallback;

	    float shieldProtection = Math.max(
	            Config.shield_mindamage_threshold,
	            Math.min(
	                    Config.shield_maxdamage_threshold,
	                    shieldDurability * Config.shield_durability_scale
	            )
	    );

	    if (shieldProtection <= 0.0F) {
	        return;
	    }

	    /*
	     * Remaining protection.
	     */
	    float remaining = Mth.clamp(
	            shieldProtection - shieldDamage,
	            0.0F,
	            shieldProtection
	    );

	    float fillPercent = remaining / shieldProtection;

	    /*
	     * GUI size and position.
	     */
	    int width = 16;
	    int height = 16;

	    int screenWidth = mc.getWindow().getGuiScaledWidth();
	    int screenHeight = mc.getWindow().getGuiScaledHeight();

	    int x = (int) (screenWidth * 0.45F);
	    int y = (int) (screenHeight * 0.45F) - (height / 2);

	    ResourceLocation background = new ResourceLocation(
	            RLCombatMod.MOD_ID,
	            "textures/gui/shield-block-background.png"
	    );

	    ResourceLocation fill = new ResourceLocation(
	            RLCombatMod.MOD_ID,
	            "textures/gui/shield-block-fill.png"
	    );

	    ResourceLocation broken = new ResourceLocation(
	            RLCombatMod.MOD_ID,
	            "textures/gui/shield-block-break.png"
	    );

	    RenderSystem.enableBlend();
	    RenderSystem.defaultBlendFunc();

	    /*
	     * Always draw the empty/background shield first.
	     */
	    event.getGuiGraphics().blit(
	            background,
	            x,
	            y,
	            0,
	            0,
	            width,
	            height,
	            width,
	            height
	    );

	    /*
	     * If a shield is on cooldown, show the FULL red shield.
	     *
	     * This intentionally ignores fillPercent.
	     */
	    if (shieldOnCooldown) {

	        event.getGuiGraphics().blit(
	                broken,
	                x,
	                y,
	                0,
	                0,
	                width,
	                height,
	                width,
	                height
	        );

	    } else if (fillPercent > 0.0F) {

	        /*
	         * Normal shield:
	         * fill from bottom according to remaining capacity.
	         */
	        int fillHeight = Mth.ceil(height * fillPercent);
	        int fillY = y + height - fillHeight;

	        event.getGuiGraphics().blit(
	                fill,
	                x,
	                fillY,
	                0,
	                height - fillHeight,
	                width,
	                fillHeight,
	                width,
	                height
	        );
	    }

	    RenderSystem.disableBlend();
	}

    private static void shieldCooldown(int value, Player player) {
    	if (Config.isTalentsLoaded) {
    		float resistChance = HandlerCommonConfig.HANDLER.instance().shieldproficiency;
    		if (RegistryPassives.SHIELD_PROFICIENCY != null) {
    			Passive shield = RegistryPassives.SHIELD_PROFICIENCY.get();
    			if (shield.getLevel(player) > 0) {
    				if (player.getRandom().nextFloat() < resistChance * (shield.getLevel(player) * shield.getLevelRatio())) {
    					return;
    				}
    			}
    		}
    	}
    	player.getCooldowns().addCooldown(player.getUseItem().getItem(), value);
        player.stopUsingItem();
        player.level().broadcastEntityEvent(player, (byte)30);
    }
    
    private static int getShieldCooldown(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof ShieldItem)) {
            return 0;
        }

        return player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F) > 0.0F
                ? 1
                : 0;
    }
    
}
