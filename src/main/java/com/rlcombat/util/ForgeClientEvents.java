package com.rlcombat.util;

import com.rlcombat.RLCombatMod;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.List;
import java.util.Map;

import com.rlcombat.Config;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = RLCombatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeClientEvents {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
    public static void onShieldsTooltip(ItemTooltipEvent event) {
		List<Component> list = event.getToolTip();
		Item item = event.getItemStack().getItem();
		if (item instanceof ShieldItem) {
			ItemStack itemStack = event.getItemStack();
			float shieldDurability = (float)((itemStack.getMaxDamage() > 0) ? itemStack.getMaxDamage() : Config.shield_fallback);
			float shieldProtection = Math.max(Config.shield_mindamage_threshold, Math.min(Config.shield_maxdamage_threshold, shieldDurability * Config.shield_durability_scale));
			double shieldProtectionRounded = ((double)((int)(shieldProtection * 100))) / 100D;
			list.add(Component.translatable("tooltip.rlcombat.shield_power", shieldProtectionRounded));
		}
		if (item instanceof ArmorItem armor2) {
			ArmorDamageType armor = (ArmorDamageType) armor2;
			Component finder;
			int position = 0;
			if (armor2.getEquipmentSlot() != null) {
				finder = Component.translatable("item.modifiers." + armor2.getEquipmentSlot().getName()).withStyle(ChatFormatting.GRAY);
				position = list.indexOf(finder);
				if (position == -1) {
					position = list.size() - 1;
				}
			} else {
				position = list.size() - 1;
			}
			for (Map.Entry<Holder<DamageType>, Float> entry : armor.getResistances().entrySet()) {
		        Holder<DamageType> holder = entry.getKey();
		        float resistance = entry.getValue();

		        // Skip if the holder has no key (shouldn't happen, but safe)
		        if (holder.unwrapKey().isEmpty()) continue;
		        // Format: "75% Resistance to Slashing"
		        String percent = String.format("%.0f%%", resistance * 100f);

		        Component line = Component.literal(" ").append(Component.literal(percent + " "))
		                .append(Component.translatable("tooltip.rlcombat.resistance", holder.get().msgId()))
		                .withStyle(ChatFormatting.BLUE);

		        list.add(position++, line);
		    }
			list.add(position++, Component.literal(" ").append(Component.literal("100% "))
	                .append(Component.translatable("tooltip.rlcombat.resistance", "other"))
	                .withStyle(ChatFormatting.GRAY));
		}
		if (item instanceof ItemDamageType itemdamage) {
			Component finder2= Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY);
			int position = list.indexOf(finder2);
			if (position == -1) {
				position = list.size() - 1;
			}
			Holder<DamageType> damagetype = itemdamage.getDamageType();
			Component info = Component.translatable("tooltip.rlcombat.damage_type").append(Component.translatable("rlcombat.damage_type." + (damagetype != null ? damagetype.get().msgId() : "unknown"))).withStyle(ChatFormatting.DARK_GREEN);
			list.add(position, info);
		}
	}
}
