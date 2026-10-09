package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.RecipeSpec;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierRecipe.RecipeType;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.registry.TieredItems.TierEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RecipeFactory {

    private static final String NAMESPACE = "jaams_weaponry";

    private static final String OWN = JaamsWeaponryExMod.ITEM_NAMESPACE;

    private static final String FORGING_TOOL = "#jaams_weaponry:misc/forging_tool";
    private static final String SMITHING_TRANSFORM = "minecraft:smithing_transform";
    private static final String TABLE_SMITHING_TRANSFORM =
            JaamsWeaponryExMod.MODID + ":smithing_transform";
    private static final com.google.gson.Gson GSON =
            new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final AtomicBoolean CATALOG_CHECKED = new AtomicBoolean();

    private RecipeFactory() {
    }

    public static void add(GeneratedPack resources, TierEntry tier) {
        TierDefinition def = tier.definition();
        RecipeType type = def.getRecipeType();
        if (type == RecipeType.NONE) {
            JaamsWeaponryExMod.LOGGER.debug("Tier '{}': recipe_type is 'none', no recipes generated. "
                    + "Set \"recipe_type\" to \"normal\" or \"smithing\" plus a \"base_material\" to "
                    + "generate them.", def.id);
            return;
        }
        if (!def.hasBaseMaterial()) {
            JaamsWeaponryExMod.LOGGER.warn("Tier '{}': recipe_type is '{}' but 'base_material' is missing, "
                    + "no recipes generated.", def.id, type.name().toLowerCase(java.util.Locale.ROOT));
            return;
        }

        String id = def.id;
        JsonObject material = ingredientValue(def.recipe.base_material.trim());
        String doubleIngot = OWN + ":" + def.doubleMaterialName();
        String heavyIngot = OWN + ":" + def.heavyMaterialName();
        if (!def.hasMaterials()) {
            doubleIngot = null;
            heavyIngot = null;
        }

        checkCatalog();
        int count = def.hasMaterials() ? ingots(resources, def, ingotMaterial(def), doubleIngot, heavyIngot) : 0;
        count += switch (type) {
            case NORMAL -> normal(resources, def, material, doubleIngot, heavyIngot);
            case SMITHING -> smithing(resources, def, material, doubleIngot, heavyIngot);
            case NONE -> 0;
        };
        JaamsWeaponryExMod.LOGGER.debug("Tier '{}': generated {} recipe(s) for recipe_type '{}'",
                def.id, count, type.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static void checkCatalog() {
        if (!CATALOG_CHECKED.compareAndSet(false, true)) {
            return;
        }
        Set<String> weapons = new LinkedHashSet<>();
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            weapons.add(weapon.name());
        }
        Set<String> specs = new LinkedHashSet<>();
        for (RecipeSpec spec : RecipeSpec.RECIPES) {
            specs.add(spec.weapon());
        }

        List<String> unreceipable = weapons.stream().filter(w -> !specs.contains(w)).toList();
        List<String> unregistered = specs.stream().filter(w -> !weapons.contains(w)).toList();
        if (!unreceipable.isEmpty()) {
            JaamsWeaponryExMod.LOGGER.error("These weapons are registered but have no recipe, so a "
                    + "tier carrying them could not obtain them: {}. Add a RecipeSpec for each.",
                    String.join(", ", unreceipable));
        }
        if (!unregistered.isEmpty()) {
            JaamsWeaponryExMod.LOGGER.warn("These recipes have no matching weapon, so they are never "
                    + "written: {}", String.join(", ", unregistered));
        }
    }

    private static List<RecipeSpec> weaponsOf(TierDefinition def) {
        Set<String> owned = new LinkedHashSet<>();
        for (WeaponSpec spec : def.weapons()) {
            owned.add(spec.name());
        }
        List<RecipeSpec> specs = new ArrayList<>();
        for (RecipeSpec spec : RecipeSpec.RECIPES) {
            if (owned.contains(spec.weapon())) {
                specs.add(spec);
            }
        }
        return specs;
    }

    private static int normal(GeneratedPack resources, TierDefinition def, JsonObject material,
            String doubleIngot, String heavyIngot) {
        int count = 0;
        for (RecipeSpec spec : weaponsOf(def)) {
            count += craft(resources, def, spec, material, doubleIngot, heavyIngot);
        }
        return count;
    }

    private static int craft(GeneratedPack resources, TierDefinition def, RecipeSpec spec,
            JsonObject material, String doubleIngot, String heavyIngot) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("group", spec.weapon());
        recipe.addProperty("category", spec.category());
        JsonArray pattern = new JsonArray();
        spec.pattern().forEach(pattern::add);
        recipe.add("pattern", pattern);
        JsonObject key = new JsonObject();
        for (Map.Entry<Character, String> entry : spec.key().entrySet()) {
            key.add(String.valueOf(entry.getKey()),
                    resolve(entry.getValue(), material, doubleIngot, heavyIngot));
        }
        recipe.add("key", key);
        recipe.add("result", result(OWN + ":" + def.id + "_" + registryName(spec)));
        put(resources, "ex_" + def.id + "_" + spec.weapon() + "_recipe", recipe);
        return 1;
    }

    private static int ingots(GeneratedPack resources, TierDefinition def, JsonObject raw,
            String doubleIngot, String heavyIngot) {
        if (raw == null) {
            return 0;
        }
        put(resources, "ex_" + def.id + "_" + def.doubleMaterialName() + "_recipe", shapeless("double_ingot",
                "misc", doubleIngot, ingredientTag(FORGING_TOOL), raw, raw));
        put(resources, "ex_" + def.id + "_" + def.heavyMaterialName() + "_recipe", shapeless("heavy_ingot",
                "misc", heavyIngot, ingredientTag(FORGING_TOOL), item(doubleIngot), item(doubleIngot)));
        return 2;
    }

    private static int smithing(GeneratedPack resources, TierDefinition def, JsonObject material,
            String doubleIngot, String heavyIngot) {
        if (!def.hasBaseTier() || !def.hasSmithingTemplate()) {
            JaamsWeaponryExMod.LOGGER.warn("Tier '{}': recipe_type is 'smithing' but 'base_tier' or "
                    + "'smithing_template' is missing, so the weapons are crafted at a table "
                    + "instead of upgraded.", def.id);
            return normal(resources, def, material, doubleIngot, heavyIngot);
        }
        String baseTier = def.tier.base_tier.trim();
        String template = def.recipe.smithing_template.trim();
        String table = def.smithingTable();
        boolean vanillaTable = !def.hasCustomSmithingTable();
        if (!vanillaTable && ResourceLocation.tryParse(table) == null) {
            JaamsWeaponryExMod.LOGGER.warn("Tier '{}': 'smithing_table' '{}' is not a valid item id, "
                    + "falling back to the vanilla smithing table.", def.id, table);
            vanillaTable = true;
        }

        int count = 0;
        List<String> crafted = new ArrayList<>();
        for (RecipeSpec spec : weaponsOf(def)) {
            count += generateSmithingRecipe(resources, def, spec, baseTier, template, table, vanillaTable,
                    material, doubleIngot, heavyIngot, crafted);
        }
        if (!crafted.isEmpty()) {
            JaamsWeaponryExMod.LOGGER.debug("Tier '{}': the base mod ships no '{}' version of {}, so "
                    + "those weapons are crafted at a table instead of upgraded.", def.id,
                    baseTier, String.join(", ", crafted));
        }
        return count;
    }

    private static int generateSmithingRecipe(GeneratedPack resources, TierDefinition def, RecipeSpec spec,
            String baseTier, String template, String table, boolean vanillaTable,
            JsonObject material, String doubleIngot, String heavyIngot, List<String> crafted) {
        String basePath = baseTier + "_" + registryName(spec);
        if (!baseItemExists(basePath)) {

            crafted.add(spec.weapon());
            return craft(resources, def, spec, material, doubleIngot, heavyIngot);
        }
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", vanillaTable ? SMITHING_TRANSFORM : TABLE_SMITHING_TRANSFORM);
        recipe.add("template", item(template));
        recipe.add("base", item(NAMESPACE + ":" + basePath));
        recipe.add("addition", resolve(spec.smithingAddition(), material, doubleIngot, heavyIngot));
        recipe.add("result", result(OWN + ":" + def.id + "_" + registryName(spec)));
        if (!vanillaTable) {
            recipe.addProperty("smithing_table", table);
        }
        put(resources, "ex_" + def.id + "_" + spec.weapon() + "_recipe", recipe);
        return 1;
    }

    private static String registryName(RecipeSpec spec) {
        WeaponSpec weapon = WeaponSpec.byName(spec.weapon());
        return weapon != null ? weapon.registryName() : spec.weapon();
    }

    private static boolean baseItemExists(String path) {
        return BaseLang.ships(path, true);
    }

    private static JsonObject ingotMaterial(TierDefinition def) {
        if (def.hasIngotMaterial()) {
            return ingredientValue(def.recipe.ingot_material.trim());
        }
        return def.hasBaseMaterial() ? ingredientValue(def.recipe.base_material.trim()) : null;
    }

    private static ResourceLocation recipe(String path) {
        return ResourceLocation.tryBuild(NAMESPACE, "recipes/" + path + ".json");
    }

    private static void put(GeneratedPack resources, String path, JsonObject recipe) {
        resources.put(recipe(path), GSON.toJson(recipe));
    }

    private static JsonObject ingredientValue(String value) {
        return value.startsWith("#") ? ingredientTag(value) : item(value);
    }

    private static JsonObject item(String id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id);
        return json;
    }

    private static JsonObject ingredientTag(String tag) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tag.startsWith("#") ? tag.substring(1) : tag);
        return json;
    }

    private static JsonObject result(String id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id);
        json.addProperty("count", 1);
        return json;
    }

    private static JsonObject shapeless(String group, String category, String output,
            JsonObject... ingredients) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("group", group);
        recipe.addProperty("category", category);
        JsonArray array = new JsonArray();
        for (JsonObject ingredient : ingredients) {
            array.add(ingredient);
        }
        recipe.add("ingredients", array);
        recipe.add("result", result(output));
        return recipe;
    }

    private static JsonObject resolve(String token, JsonObject raw, String doubleIngot,
            String heavyIngot) {
        return switch (token) {
            case RecipeSpec.RAW -> raw;
            case RecipeSpec.DOUBLE -> doubleIngot == null ? raw : item(doubleIngot);
            case RecipeSpec.HEAVY -> heavyIngot == null ? raw : item(heavyIngot);
            default -> ingredientValue(token);
        };
    }
}
