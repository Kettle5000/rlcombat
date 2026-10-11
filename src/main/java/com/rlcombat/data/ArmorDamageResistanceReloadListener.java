package com.rlcombat.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.rlcombat.RLCombatMod;
import com.rlcombat.util.ArmorDamageType;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import java.io.Reader;

public class ArmorDamageResistanceReloadListener extends SimpleJsonResourceReloadListener {

    private static final String FOLDER = "armor_resistance";

    private static final Gson GSON = new GsonBuilder().create();
    
    public static Map<ResourceLocation, Map<ResourceLocation, Float>> server_map = new HashMap<>(); // item res gives you a list map of damage type - float 

    private final RegistryAccess registryAccess;

    public ArmorDamageResistanceReloadListener(RegistryAccess registryAccess) {
        super(GSON, FOLDER);
        this.registryAccess = registryAccess;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(
            ResourceManager manager,
            ProfilerFiller profiler) {

        Map<ResourceLocation, JsonElement> out = new HashMap<>();

        FileToIdConverter converter = FileToIdConverter.json(FOLDER);

        converter.listMatchingResourceStacks(manager).forEach((fileLocation, stack) -> {

            ResourceLocation id = converter.fileToId(fileLocation);

            /*
             * Resources are ordered from lowest priority to highest priority.
             *
             * Therefore the LAST resource is supplied by the
             * highest-priority datapack.
             */
            Resource top = stack.get(stack.size() - 1);

            try (Reader reader = top.openAsReader()) {

                JsonElement json =
                        GsonHelper.fromJson(GSON, reader, JsonElement.class);

                out.put(id, json);

            } catch (Exception exception) {

                RLCombatMod.LOGGER.error(
                        "Couldn't parse armor damage resistance data '{}' from {}",
                        fileLocation,
                        top.sourcePackId(),
                        exception
                );
            }
        });

        return out;
    }
    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsons,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {

        // Clear previous data (important for /reload)
        server_map.clear();

        // Clear all previously assigned resistances on items
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ArmorDamageType armor) {
                armor.setResistances(new HashMap<>());
            }
        }

        // Process every JSON definition
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation jsonId = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            try {
                JsonObject json = GsonHelper.convertToJsonObject(jsonElement, jsonId.toString());

                if (!json.has("item")) {
                    throw new JsonParseException("Missing 'item'");
                }
                if (!json.has("damage_type")) {
                    throw new JsonParseException("Missing 'damage_type'");
                }
                if (!json.has("resistance")) {
                    throw new JsonParseException("Missing 'resistance'");
                }
                // --- Support both single string and array of items ---
                List<ResourceLocation> itemIds = new ArrayList<>();
                JsonElement itemElement = json.get("item");

                if (itemElement.isJsonArray()) {
                    JsonArray array = itemElement.getAsJsonArray();
                    for (JsonElement el : array) {
                        String raw = GsonHelper.convertToString(el, "item");
                        ResourceLocation id = ResourceLocation.tryParse(raw);
                        if (id == null) {
                            throw new JsonParseException("Invalid item ResourceLocation: " + raw);
                        }
                        itemIds.add(id);
                    }
                } else if (itemElement.isJsonPrimitive() && itemElement.getAsJsonPrimitive().isString()) {
                    ResourceLocation id = ResourceLocation.tryParse(itemElement.getAsString());
                    if (id == null) {
                        throw new JsonParseException("Invalid item ResourceLocation");
                    }
                    itemIds.add(id);
                } else {
                    throw new JsonParseException("'item' must be a string or an array of strings");
                }

                if (itemIds.isEmpty()) {
                    throw new JsonParseException("'item' list is empty");
                }
                // ----------------------------------------------------

                ResourceLocation damageTypeId = ResourceLocation.tryParse(
                        GsonHelper.getAsString(json, "damage_type")
                );
                if (damageTypeId == null) {
                    throw new JsonParseException("Invalid damage_type ResourceLocation");
                }

                float resistance = GsonHelper.getAsFloat(json, "resistance");

                ResourceKey<DamageType> damageTypeKey =
                        ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);

                Holder<DamageType> damageTypeHolder =
                        registryAccess
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolder(damageTypeKey)
                                .orElse(null);

                if (damageTypeHolder == null) {
                    RLCombatMod.LOGGER.warn(
                            "Armor damage resistance '{}' references unknown damage type '{}'",
                            jsonId,
                            damageTypeId
                    );
                    continue;
                }

                // Apply the same resistance to every listed item
                for (ResourceLocation itemId : itemIds) {
                    Item item1 = BuiltInRegistries.ITEM.get(itemId);

                    if (!(item1 instanceof ArmorDamageType armor)) {
                        RLCombatMod.LOGGER.warn(
                                "Armor damage resistance '{}' targets item '{}', " +
                                "but that item does not implement ArmorDamageType",
                                jsonId,
                                itemId
                        );
                        continue;
                    }

                    // Server-side runtime data
                    armor.getResistances().put(damageTypeHolder, resistance);
                    // Data that will be synced to the client
                    server_map
                            .computeIfAbsent(itemId, k -> new HashMap<>())
                            .put(damageTypeId, resistance);
                }

            } catch (Exception exception) {
                RLCombatMod.LOGGER.error(
                        "Couldn't process armor damage resistance data '{}'",
                        jsonId,
                        exception
                );
            }
        }
    }
}
