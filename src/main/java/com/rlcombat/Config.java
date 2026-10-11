package com.rlcombat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.rlcombat.util.EffectEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;


// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = RLCombatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue COOLDOWN = BUILDER
            .comment("Percentage of cooldown applied to the opposite hand after an attack is made")
            .defineInRange("AttackCooldown", 0.5d, 0.0d, 1.0d);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNEDITEMS_LIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNEDENCHANTMENTS_LIST;
    private static final ForgeConfigSpec.BooleanValue RANDOM_CRITS = BUILDER
            .comment("Should crits be random")
            .define("Random Crits", true);
    private static final ForgeConfigSpec.DoubleValue CRIT_CHANCE = BUILDER
            .comment("Grants a chance for attacks to be critical hits")
            .defineInRange("Critical Chance", 0.2d, 0.0d, 1.0d);
    private static final ForgeConfigSpec.DoubleValue CRIT_DISTANCE = BUILDER
            .comment("Distance required for a jumping crit to fully land, if set to 0 vanilla calculations will be used.")
            .defineInRange("Crit Distance Required", 2.0d, 0.0d, 10d);
    private static final ForgeConfigSpec.IntValue SHIELD_MAX_CD = BUILDER
            .comment("Maximum amount of cooldown ticks a shield break will cause.")
            .defineInRange("Cooldown Maximum Ticks", 100, 0, 600);
    private static final ForgeConfigSpec.IntValue SHIELD_MIN_CD = BUILDER
            .comment("Minimum amount of cooldown ticks a shield break will cause.")
            .defineInRange("Cooldown Minimum Ticks", 5, 0, 600);
    private static final ForgeConfigSpec.DoubleValue SHIELD_MULTIPLIER = BUILDER
            .comment("Multiplier for cooldown ticks per point of damage.")
            .defineInRange("Cooldown Ticks Scaling", 5.0d, 0.0d, 1000.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_DURABILITY_SCALE = BUILDER
            .comment("Scaling for how much damage a shield will withstand before chance of cooldown based on max durability. (Durability/This = Damage)")
            .defineInRange("Damage Durability Scaling", 100d, 0.0d, 100000.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_MAXDAMAGE_THRESHOLD = BUILDER
            .comment("Maximum amount of damage any shield will withstand before chance of cooldown.")
            .defineInRange("Damage Maximum Threshold", 40d, 0.0d, 100000.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_MINDAMAGE_THRESHOLD = BUILDER
            .comment("Minimum amount of damage any shield will withstand before chance of cooldown.")
            .defineInRange("Damage Minimum Threshold", 1.0d, 0.0d, 100000.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_KNOCKBACK_BREAK = BUILDER
            .comment("Knockback power on break.")
            .defineInRange("Knockback break", 0.25d, 0.0d, 100.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_KNOCKBACK_NORMAL = BUILDER
            .comment("Knockback power on normal block.")
            .defineInRange("Knockback normal", 0.5d, 0.0d, 100.0d);
    private static final ForgeConfigSpec.DoubleValue SHIELD_KNOCKBACK_PARRY = BUILDER
            .comment("Knockback power on parry.")
            .defineInRange("Knockback parry", 1.0d, 0.0d, 100.0d);
    private static final ForgeConfigSpec.IntValue SHIELD_PARRY_RANGE = BUILDER
            .comment("Range for how long the shield can be held up and still parry the attack. (Counted after the initial shield delay, if any)")
            .defineInRange("Parry Range", 10, 0, 100);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHIELD_ATTACKER_BREAK = BUILDER
            .comment("Potion Effects to apply to an attacker on a break. (Potion;Duration;Amplifier)")
            .defineListAllowEmpty("Potion Effect Attacker Break", new ArrayList<>(List.of()), Config::validateMobEffects);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHIELD_ATTACKER_PARRY = BUILDER
            .comment("Potion Effects to apply to an attacker on a parry. (Potion;Duration;Amplifier)")
            .defineListAllowEmpty("Potion Effect Attacker Parry", new ArrayList<>(List.of("minecraft:slowness;40;2", "minecraft:weakness;40;1")), Config::validateMobEffects);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHIELD_DEFENDER_BREAK = BUILDER
            .comment("Potion Effects to apply to an defender on a break. (Potion;Duration;Amplifier)")
            .defineListAllowEmpty("Potion Effect Defender Break", new ArrayList<>(List.of("minecraft:slowness;40;2", "minecraft:weakness;40;1")), Config::validateMobEffects);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHIELD_DEFENDER_PARRY = BUILDER
            .comment("Potion Effects to apply to an defender on a parry. (Potion;Duration;Amplifier)")
            .defineListAllowEmpty("Potion Effect Defender Parry", new ArrayList<>(List.of()), Config::validateMobEffects);
    private static final ForgeConfigSpec.DoubleValue SHIELD_BYPASS_CHANCE = BUILDER
            .comment("Base chance for a weapon that can break shields to ignore shield protection and cause a cooldown. (Ex. Axe and Halberd)")
            .defineInRange("Shield Bypass Chance", 0.25d, 0.0d, 1.0d);
    private static final ForgeConfigSpec.IntValue SHIELD_BYPASS_COOLDOWN = BUILDER
            .comment("How long should the cooldown be if a weapon that can break shields bypasses the shield. (Ex. Axe and Halberd)")
            .defineInRange("Shield Bypass Cooldown", 100, 0, Short.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue SHIELD_FALLBACK = BUILDER
            .comment("How much durability a shield should have for the purpose of scaling if it's max durability is 0.")
            .defineInRange("Shield Durability Fallback", 3000, 0, Short.MAX_VALUE);
    private static final ForgeConfigSpec.IntValue SHIELD_RAISE_DELAY = BUILDER
            .comment("How many ticks should it take when raising a shield for it to become active.")
            .defineInRange("Shield Raise Tick Delay", 2, 0, Short.MAX_VALUE);
    
    private static final ForgeConfigSpec.BooleanValue ATTACK_HELD = BUILDER
            .comment("Should holding the attack button automatically attack the first target in sight.")
            .define("Attack Holding", false);
    
    static {
        // Default list of enchantments
        List<String> defaultEnchantments = new ArrayList<>(List.of());
        List<String> defaultItems = new ArrayList<>(List.of());
        // Check if the other mod is loaded
        if (ModList.get().isLoaded("jlme")) {
            // Add additional enchantments if the mod is present
            defaultEnchantments.addAll(List.of("jlme:critical_strike"));
        }
        if (ModList.get().isLoaded("fn")) {
            // Add additional items if the mod is present
            defaultItems.addAll(List.of(
            		"fn:wood_nunchakus",
            		"fn:stone_nunchakus",
            		"fn:copper_nunchakus",
            		"fn:gold_nunchakus",
            		"fn:iron_nunchakus",
            		"fn:silver_nunchakus",
            		"fn:desertchitin_nunchakus",
            		"fn:junglechitin_nunchakus",
            		"fn:dragonbone_nunchakus",
            		"fn:firedragonbone_nunchakus",
            		"fn:icedragonbone_nunchakus",
            		"fn:lightningdragonbone_nunchakus"
            		));
        }
        BANNEDITEMS_LIST = BUILDER
                .comment("List of Items that are banned to work with offhand combat, meant to avoid buggy interactions with other mods")
                .defineListAllowEmpty("Banned Items", defaultItems, Config::validateItems);
        BANNEDENCHANTMENTS_LIST = BUILDER
                .comment("List of enchantments that will cause offhand to not work")
                .defineListAllowEmpty("Banned Enchantments", defaultEnchantments, Config::validateEnchantments);
        
        BUILDER.build();
    }


    static final ForgeConfigSpec SPEC = BUILDER.build();

 
    public static float cooldown;
    public static Set<Item> items_blacklist;
    public static Set<Enchantment> enchantments_blacklist;
    public static boolean random_crits;
    public static float crit_distance;
    public static float crit_chance;
    public static int shield_max_cd;
    public static int shield_min_cd;
    public static float shield_multiplier;
    public static float shield_durability_scale;
    public static float shield_maxdamage_threshold;
    public static float shield_mindamage_threshold;
    public static float shield_knockback_break;
    public static float shield_knockback_normal;
    public static float shield_knockback_parry;
    public static int shield_parry_ticks;
    public static List<EffectEntry> shield_attacker_break;
    public static List<EffectEntry> shield_attacker_parry;
    public static List<EffectEntry> shield_defender_break;
    public static List<EffectEntry> shield_defender_parry;
    public static float shield_bypass_chance;
    public static int shield_bypass_cd;
    public static int shield_fallback;
    public static int shield_raise_delay;
    public static boolean isFNloaded;
    public static boolean isSWloaded;
    public static boolean isAttackHeld;
    public static boolean isItemPhysicsLoaded;
    public static boolean isTalentsLoaded;
    
    private static boolean validateItems(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }
    
    private static boolean validateEnchantments(final Object obj)
    {
        return obj instanceof final String enchName && ForgeRegistries.ENCHANTMENTS.containsKey(new ResourceLocation(enchName));
    }
    
    private static boolean validateMobEffects(final Object obj)
    {
        return obj instanceof final String effectName && ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(effectName.split(";")[0]));
    }
    
    private static List<EffectEntry> processEffects(List<? extends String> raw) {
    	List<EffectEntry> result = new ArrayList<EffectEntry>(List.of());
    	for (String entry : raw) {
            String[] parts = entry.split(";");
            if (parts.length != 3) continue;

            ResourceLocation effectId = new ResourceLocation(parts[0]);
            int amplifier = Integer.parseInt(parts[2]);
            int duration = Integer.parseInt(parts[1]);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            if (effect != null) {
                result.add(new EffectEntry(effect, duration, amplifier));
            }
        }
    	return result;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        cooldown = (float)(COOLDOWN.get() * 1f);
        items_blacklist = BANNEDITEMS_LIST.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());
        enchantments_blacklist = BANNEDENCHANTMENTS_LIST.get().stream()
                .map(itemName -> ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());
        random_crits = RANDOM_CRITS.get();
        crit_distance = (float) (CRIT_DISTANCE.get() * 1d);
        crit_chance = (float) (CRIT_CHANCE.get() * 1d);
        shield_max_cd = SHIELD_MAX_CD.get();
        shield_min_cd = SHIELD_MIN_CD.get();
        shield_multiplier = (float) (SHIELD_MULTIPLIER.get() * 1d);
        shield_durability_scale = (float) (1d/SHIELD_DURABILITY_SCALE.get());
        shield_maxdamage_threshold = (float) (SHIELD_MAXDAMAGE_THRESHOLD.get() * 1d);
        shield_mindamage_threshold = (float) (SHIELD_MINDAMAGE_THRESHOLD.get() * 1d);
        shield_knockback_break = (float) (SHIELD_KNOCKBACK_BREAK.get() * 1d);
        shield_knockback_normal = (float) (SHIELD_KNOCKBACK_NORMAL.get() * 1d);
        shield_knockback_parry = (float) (SHIELD_KNOCKBACK_PARRY.get() * 1d);
        shield_parry_ticks = SHIELD_PARRY_RANGE.get();
        shield_attacker_break = processEffects(SHIELD_ATTACKER_BREAK.get());
        shield_attacker_parry = processEffects(SHIELD_ATTACKER_PARRY.get());
        shield_defender_break = processEffects(SHIELD_DEFENDER_BREAK.get());
        shield_defender_parry = processEffects(SHIELD_DEFENDER_PARRY.get());
        shield_bypass_chance = (float) (SHIELD_BYPASS_CHANCE.get() * 1d);
        shield_bypass_cd = SHIELD_BYPASS_COOLDOWN.get();
        shield_fallback = SHIELD_FALLBACK.get();
        shield_raise_delay = SHIELD_RAISE_DELAY.get();
        isFNloaded = ModList.get().isLoaded("fn");
        isSWloaded = ModList.get().isLoaded("spartanweaponry");
        isItemPhysicsLoaded = ModList.get().isLoaded("itemphysic");
        isAttackHeld = ATTACK_HELD.get();
        isTalentsLoaded = ModList.get().isLoaded("talents");
    }
}