package com.rlcombat.data;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.rlcombat.RLCombatMod;
import com.rlcombat.util.ItemDamageType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DamageTypeItemReloadListener extends SimpleJsonResourceReloadListener {

	private static final String FOLDER = "damage_type_items";

	private static final Gson GSON = new GsonBuilder().create();
	
	public static Map<ResourceLocation, ResourceLocation> server_map = new HashMap<>();
	
	private static Map<ResourceLocation, ResourceLocation> itemDefinitions = new HashMap<>();

	private static Map<ResourceLocation, ResourceLocation> itemTagDefinitions = new HashMap<>();

	private static Map<String, ResourceLocation> itemClassDefinitions = new HashMap<>();

	private final RegistryAccess registryAccess;
	private static final List<String> default_classes = List.of("sword", "axe", "hoe", "pickaxe", "shovel");

    public DamageTypeItemReloadListener(RegistryAccess registryAccess) {
        super(GSON, FOLDER);
        this.registryAccess = registryAccess;
    }

	@Override
	protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<ResourceLocation, JsonElement> out = new HashMap<>();

		FileToIdConverter converter = FileToIdConverter.json(FOLDER);

		converter.listMatchingResourceStacks(manager).forEach((fileLocation, stack) -> {

			ResourceLocation id = converter.fileToId(fileLocation);

			/*
			 * Resources are ordered from lowest priority to highest priority.
			 *
			 * Therefore the LAST resource in the stack is the resource supplied by the
			 * highest-priority datapack.
			 *
			 * This means:
			 *
			 * datapack A -> swords.json datapack B -> swords.json
			 *
			 * B overrides A.
			 */
			Resource top = stack.get(stack.size() - 1);

			try (Reader reader = top.openAsReader()) {

				JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);

				out.put(id, json);

			} catch (Exception exception) {

				// Replace this with your mod's logger.
				System.err.println("Couldn't parse data file " + fileLocation + " from " + top.sourcePackId());

				exception.printStackTrace();
			}
		});

		return out;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
			ProfilerFiller profiler) {
		
		// Clear previous definitions
        itemDefinitions.clear();
        itemTagDefinitions.clear();
        itemClassDefinitions.clear();
		/*
		 * First remove all previously assigned damage types.
		 *
		 * This is important because /reload can remove a datapack or change its
		 * contents.
		 */
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof ItemDamageType damageTypeItem) {
				damageTypeItem.setDamageType(null);
			}
		}

		/*
		 * Store definitions by priority.
		 *
		 * Priority:
		 *
		 * 1. item - highest 2. item_tag 3. itemclass - lowest
		 */

		/*
		 * Read all JSON files.
		 */
		for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {

			ResourceLocation jsonId = entry.getKey();
			JsonElement jsonElement = entry.getValue();

			try {
				JsonObject json = GsonHelper.convertToJsonObject(jsonElement, jsonId.toString());

				if (!json.has("entries")) {
					continue;
				}

				JsonArray entries = GsonHelper.getAsJsonArray(json, "entries");

				for (JsonElement element : entries) {

					JsonObject definition = GsonHelper.convertToJsonObject(element, "damage type item definition");

					if (!definition.has("damage_type")) {
						throw new JsonParseException("Missing 'damage_type'");
					}

					ResourceLocation damageType = ResourceLocation
							.tryParse(GsonHelper.getAsString(definition, "damage_type"));

					/*
					 * ITEM
					 *
					 * Highest priority.
					 */
					if (definition.has("item")) {

						ResourceLocation item = ResourceLocation.tryParse(GsonHelper.getAsString(definition, "item"));

						itemDefinitions.put(item, damageType);

						continue;
					}

					/*
					 * ITEM TAG
					 *
					 * Second priority.
					 */
					if (definition.has("item_tag")) {

						ResourceLocation tag = ResourceLocation
								.tryParse(GsonHelper.getAsString(definition, "item_tag").trim());
						itemTagDefinitions.put(tag, damageType);

						continue;
					}

					/*
					 * ITEM CLASS
					 *
					 * Lowest priority.
					 */
					if (definition.has("itemclass")) {

						String itemClass = GsonHelper.getAsString(definition, "itemclass").toLowerCase(Locale.ROOT);
						
						if (!default_classes.contains(itemClass)) {
							throw new JsonParseException(
									"ItemClass definition must contain a valid vanilla class item: 'sword', 'axe', 'hoe', 'pickaxe', 'shovel'");
						}
						itemClassDefinitions.put(itemClass, damageType);

						continue;
					}

					throw new JsonParseException(
							"Definition must contain one of: " + "'item', 'item_tag', or 'itemclass'");
				}

			} catch (Exception exception) {

				RLCombatMod.LOGGER.error("Couldn't process damage type item data '{}'", jsonId, exception);
			}
		}

		//loadDamageTypes(this.registryAccess);
	}
	
	public static void loadDamageTypes(RegistryAccess registryAccess) {
		server_map.clear();
	    var damageTypeRegistry = registryAccess.registryOrThrow(Registries.DAMAGE_TYPE);

	    for (Item item : BuiltInRegistries.ITEM) {
	        if (!(item instanceof ItemDamageType damageTypeItem)) {
	            continue;
	        }

	        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
	        Holder<DamageType> holder = null;

	        // 1. Highest priority – individual item definition
	        ResourceLocation damageTypeId = DamageTypeItemReloadListener.itemDefinitions.get(itemId);
	        if (damageTypeId != null) {
	            holder = damageTypeRegistry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId)).orElse(null);
	        }

	        // 2. Item tag definition
	        if (holder == null) {
	            for (Map.Entry<ResourceLocation, ResourceLocation> entry : DamageTypeItemReloadListener.itemTagDefinitions.entrySet()) {
	                TagKey<Item> tag = TagKey.create(Registries.ITEM, entry.getKey());
	                if (item.builtInRegistryHolder().is(tag)) {
	                	damageTypeId = entry.getValue();
	                    holder = damageTypeRegistry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId)).orElse(null);
	                    if (holder != null) break;
	                }
	            }
	        }

	        // 3. Lowest priority – item class (sword / axe / hoe / pickaxe / shovel)
	        if (holder == null) {
	            String itemClass = getItemClass(item); // implement this helper
	            if (itemClass != null) {
	                damageTypeId = DamageTypeItemReloadListener.itemClassDefinitions.get(itemClass);
	                if (damageTypeId != null) {
	                    holder = damageTypeRegistry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId)).orElse(null);
	                }
	            }
	        }

	        //hard fail safety, we set to normal attack instead
	        if (holder == null) {
	        	damageTypeId = new ResourceLocation("player_attack");
	        	holder = damageTypeRegistry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId)).orElse(null);
	        }
	        // Apply
	        damageTypeItem.setDamageType(holder);
	        server_map.put(itemId, damageTypeId);
	    }
	}

	// Simple helper – adapt to how you detect the tool type
	private static String getItemClass(Item item) {
	    if (item instanceof SwordItem) return "sword";
	    if (item instanceof AxeItem)   return "axe";
	    if (item instanceof HoeItem)   return "hoe";
	    if (item instanceof PickaxeItem) return "pickaxe";
	    if (item instanceof ShovelItem) return "shovel";
	    return null;
	}
	
}
