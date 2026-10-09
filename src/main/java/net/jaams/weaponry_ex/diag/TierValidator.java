package net.jaams.weaponry_ex.diag;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.BonusDamageHit;
import net.jaams.weaponry_ex.data.ChainHit;
import net.jaams.weaponry_ex.data.Conditions;
import net.jaams.weaponry_ex.data.EntityFilters;
import net.jaams.weaponry_ex.data.ExplosionHit;
import net.jaams.weaponry_ex.data.FireEffect;
import net.jaams.weaponry_ex.data.HitEffect;
import net.jaams.weaponry_ex.data.HitEffectRef;
import net.jaams.weaponry_ex.data.ItemFilters;
import net.jaams.weaponry_ex.data.KnockbackHit;
import net.jaams.weaponry_ex.data.LaunchHit;
import net.jaams.weaponry_ex.data.LifestealHit;
import net.jaams.weaponry_ex.data.LightningHit;
import net.jaams.weaponry_ex.data.ModCondition;
import net.jaams.weaponry_ex.data.OnHit;
import net.jaams.weaponry_ex.data.ParticleHit;
import net.jaams.weaponry_ex.data.RecipeSpec;
import net.jaams.weaponry_ex.data.SpreadHit;
import net.jaams.weaponry_ex.data.StatusEffectHit;
import net.jaams.weaponry_ex.data.SummonHit;
import net.jaams.weaponry_ex.data.TagSection;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierProperties;
import net.jaams.weaponry_ex.data.TierRecipe;
import net.jaams.weaponry_ex.data.TierSection;
import net.jaams.weaponry_ex.data.TierTagSection;
import net.jaams.weaponry_ex.data.TierRecipe.RecipeType;
import net.jaams.weaponry_ex.data.TierSection.MaterialType;
import net.jaams.weaponry_ex.data.TooltipSection;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.pack.BaseLang;
import net.jaams.weaponry_ex.pack.PackFactory;
import net.jaams.weaponry_ex.tooltip.TierTooltip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class TierValidator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+");

    private static final String NAMESPACE = JaamsWeaponryExMod.ITEM_NAMESPACE;

    private static final int MAX_TOOLTIP_LINE = 256;

    private static final int MAX_SANE_TICKS = 7200;

    private static final float MAX_SANE_POWER = 12.0F;

    private static final float MAX_SANE_HEALING = 20.0F;

    private static final float MAX_SANE_DAMAGE = 20.0F;

    private static final float MAX_SANE_PERCENT = 100.0F;

    private static final float MAX_SANE_KNOCKBACK = 4.0F;

    private static final float MAX_SANE_LAUNCH = 6.0F;

    private static final float MAX_SANE_RADIUS = 16.0F;

    private static final int MAX_SANE_SUMMONS = 16;

    private static final int MAX_SANE_TARGETS = 64;

    private static final int MAX_SANE_PARTICLES = 512;

    private static final List<String> SMITHING_ONLY = List.of("base_tier", "smithing_template", "smithing_table");

    private static final Map<String, KeyType> FIELDS = knownFields(TierDefinition.class);

    private record KeyType(Class<?> raw, Type generic) {
    }

    private TierValidator() {
    }

    private static Map<String, KeyType> knownFields(Class<?> type) {
        Map<String, KeyType> fields = new TreeMap<>();
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isTransient(modifiers)) {
                    continue;
                }
                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)
                        && !fields.containsKey(field.getName())) {
                    fields.put(field.getName(), new KeyType(field.getType(), field.getGenericType()));
                }
            }
        }

        return Collections.unmodifiableMap(fields);
    }

    public static TierReport parse(String json, String source) {
        return parse(json, source, false);
    }

    public static TierReport parse(String json, String source, boolean standalone) {
        TierReport report = new TierReport(source, json);
        if (json == null || json.isBlank()) {
            report.error("is empty - the file is skipped");
            return report;
        }

        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element == null || !element.isJsonObject()) {
                report.error("expected a JSON object at the top level, found "
                        + describe(element) + " - the file is skipped");
                return report;
            }
            root = element.getAsJsonObject();
        } catch (JsonParseException e) {
            report.error("is not valid JSON: " + reason(e) + " - the file is skipped");
            return report;
        }

        if (shippedExample(root)) {
            report.quiet();
        }

        normalizeTooltip(root, report);
        checkKeys(root, report);
        checkLegacyLayout(root, report);
        if (checkSections(root, report)) {
            return report;
        }

        String id = readId(root, report);
        if (id == null) {
            return report;
        }

        TierDefinition def;
        try {
            def = GSON.fromJson(root, TierDefinition.class);
        } catch (JsonParseException e) {
            report.error("cannot be read: " + reason(e) + " - the file is skipped");
            return report;
        }
        if (def == null) {
            report.error("is empty - the file is skipped");
            return report;
        }

           def.id = id;
           if (def.display_name == null || def.display_name.isBlank()) {
               def.display_name = capitalize(id);
           }
           def.standalone = standalone;
           report.define(def);
        checkDefinition(def, root, report);

        if (!def.enabled) {
               report.skip("'enabled' is false, so the "
                       + (def.standalone ? "item" : "tier") + " is left out of the game for now");
        }
        return report;
    }

    private static boolean shippedExample(JsonObject root) {
        JsonElement example = root.get("example");
        if (example == null || !example.isJsonPrimitive()
                || !example.getAsJsonPrimitive().isBoolean()
                || !example.getAsBoolean()) {
            return false;
        }
        JsonElement enabled = root.get("enabled");
        return enabled == null || !enabled.isJsonPrimitive()
                || !enabled.getAsJsonPrimitive().isBoolean() || !enabled.getAsBoolean();
    }

    private static String readId(JsonObject root, TierReport report) {
        JsonElement element = root.get("id");
        String id = element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString().trim()
                : "";
        if (id.isEmpty()) {
            report.error("'id' is missing or is not a string; every item is named after it, so "
                    + "there is nothing to build - the file is skipped");
            return null;
        }
        if (!VALID_ID.matcher(id).matches()) {
            report.error("'id' '" + id + "' is not usable: it must match " + VALID_ID.pattern()
                    + " - the file is skipped");
            return null;
        }
        return id;
    }

    private static void checkKeys(JsonObject root, TierReport report) {
        Set<String> unknown = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            KeyType field = FIELDS.get(key);
            if (field == null) {
                unknown.add(key);
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonNull()) {
                report.warn("'" + key + "' is null, so its default is used");
            } else if (!fits(field, value)) {
                report.warn("'" + key + "' should be " + describe(field.raw()) + ", found " + describe(value)
                        + "; the value is read as best it can be");
            }
        }
        if (!unknown.isEmpty()) {
            report.warn("unknown " + (unknown.size() == 1 ? "key " : "keys ")
                    + quoted(unknown) + " - ignored. Known keys: " + String.join(", ", FIELDS.keySet()));
        }
    }

    private static boolean checkSections(JsonObject root, TierReport report) {
        boolean fatal = false;
        for (String name : List.of("properties", "recipe", "tier")) {
            JsonElement element = root.get(name);
            if (element == null) {
                continue;
            }
            if (!element.isJsonObject()) {
                report.warn("'" + name + "' should be a JSON object, found " + describe(element)
                        + ", so the whole section keeps its defaults");
                continue;
            }
            checkSubKeys(element.getAsJsonObject(), sectionType(name), name, report);
        }
        JsonElement tooltip = root.get("tooltip");
        if (tooltip != null && tooltip.isJsonObject()) {
            checkSubKeys(tooltip.getAsJsonObject(), TooltipSection.class, "tooltip", report);
        }
        JsonElement tags = root.get("tags");
        if (tags != null && tags.isJsonObject()) {
            checkSubKeys(tags.getAsJsonObject(), TagSection.class, "tags", report);
            JsonElement tierTag = tags.getAsJsonObject().get("tier_tag");
            if (tierTag != null && tierTag.isJsonObject()) {
                checkSubKeys(tierTag.getAsJsonObject(), TierTagSection.class, "tags.tier_tag", report);
            }
        }
        JsonElement conditions = root.get("conditions");
        if (conditions != null && conditions.isJsonObject()) {
            checkConditions(conditions.getAsJsonObject(), report);
        }
        if (checkOnHitKeys(root, report)) {
            fatal = true;
        }
        return fatal;
    }

    private static void checkConditions(JsonObject node, TierReport report) {
        checkSubKeys(node, Conditions.class, "conditions", report);
        JsonElement mods = node.get("mods");
        if (mods != null && mods.isJsonArray()) {
            for (int i = 0; i < mods.getAsJsonArray().size(); i++) {
                JsonElement entry = mods.getAsJsonArray().get(i);
                if (entry != null && entry.isJsonObject()) {
                    checkSubKeys(entry.getAsJsonObject(), ModCondition.class, "conditions.mods[" + i + "]", report);
                }
            }
        }
    }

    private static void normalizeTooltip(JsonObject root, TierReport report) {
        JsonElement element = root.get("tooltip");
        if (element == null || !element.isJsonArray()) {
            return;
        }
        JsonObject wrapped = new JsonObject();
        wrapped.add("lines", element);
        root.add("tooltip", wrapped);
        report.warn("'tooltip' is a plain list of lines, so 'exclude_items' cannot be used on it. "
                + "Write it as 'tooltip': { \"lines\": [ ... ], \"exclude_items\": [ ... ] } - the "
                + "lines are read as they are");
    }

    private static Class<?> sectionType(String name) {
        return switch (name) {
            case "properties" -> TierProperties.class;
            case "recipe" -> TierRecipe.class;
            default -> TierSection.class;
        };
    }

    private static final Map<String, String> MOVED_KEYS = movedKeys();

    private static Map<String, String> movedKeys() {
        Map<String, String> moved = new LinkedHashMap<>();
        for (String key : knownFields(TierProperties.class).keySet()) {
            moved.put(key, "properties");
        }
        for (String key : knownFields(TierRecipe.class).keySet()) {
            moved.put(key, "recipe");
        }
        for (String key : knownFields(TierSection.class).keySet()) {
            moved.put(key, "tier");
        }
        return Collections.unmodifiableMap(moved);
    }

    private static void checkLegacyLayout(JsonObject root, TierReport report) {
        Set<String> flat = new LinkedHashSet<>();
        Map<String, String> homes = new LinkedHashMap<>();
        for (String key : MOVED_KEYS.keySet()) {
            if (root.has(key)) {
                flat.add(key);
                homes.put(key, MOVED_KEYS.get(key));
            }
        }
        if (flat.isEmpty()) {
            return;
        }
        StringBuilder where = new StringBuilder();
        for (String key : flat) {
            if (where.length() > 0) {
                where.append(", ");
            }
            where.append('\'').append(key).append("' -> '").append(homes.get(key)).append('.').append(key).append('\'');
        }
        report.error("the keys " + where + " used to sit at the top level and are no longer read. "
                + "Wrap them in a 'properties', 'recipe' or 'tier' object - the file is skipped");
    }

    private static boolean checkOnHitKeys(JsonObject root, TierReport report) {
        JsonElement element = root.get("on_hit");
        if (element == null || !element.isJsonObject()) {
            return false;
        }
        JsonObject onHit = element.getAsJsonObject();
        if (checkLegacyOnHit(onHit, report)) {
            return true;
        }
        checkSubKeys(onHit, OnHit.class, "on_hit", report);
        JsonElement fire = onHit.get("fire");
        if (fire != null && fire.isJsonObject()) {
            checkSubKeys(fire.getAsJsonObject(), FireEffect.class, "on_hit.fire", report);
        }
        checkEffectKeys(onHit, "lightning", LightningHit.class, report);
        checkEffectKeys(onHit, "explosion", ExplosionHit.class, report);
        checkEffectKeys(onHit, "lifesteal", LifestealHit.class, report);
        checkEffectKeys(onHit, "bonus_damage", BonusDamageHit.class, report);
        checkEffectKeys(onHit, "knockback", KnockbackHit.class, report);
        checkEffectKeys(onHit, "launch", LaunchHit.class, report);
        checkEffectKeys(onHit, "summon", SummonHit.class, report);
        checkEffectKeys(onHit, "chain", ChainHit.class, report);
        checkEffectKeys(onHit, "spread", SpreadHit.class, report);
        checkEffectKeys(onHit, "particle", ParticleHit.class, report);
        JsonElement status = onHit.get("status");
        if (status == null || !status.isJsonArray()) {
            return false;
        }
        for (int i = 0; i < status.getAsJsonArray().size(); i++) {
            JsonElement entry = status.getAsJsonArray().get(i);
            if (entry != null && entry.isJsonObject()) {
                checkSubKeys(entry.getAsJsonObject(), StatusEffectHit.class, "on_hit.status[" + i + "]", report);
            }
        }
        return false;
    }

    private static void checkEffectKeys(JsonObject onHit, String key, Class<?> type, TierReport report) {
        JsonElement value = onHit.get(key);
        if (value != null && value.isJsonObject()) {
            checkSubKeys(value.getAsJsonObject(), type, "on_hit." + key, report);
        }
    }

    private static final List<String> LEGACY_ON_HIT =
            List.of("fire", "fire_chance", "fire_ticks", "fire_max_ticks", "max_ticks");

    private static boolean checkLegacyOnHit(JsonObject onHit, TierReport report) {
        Set<String> flat = new LinkedHashSet<>();
        for (String key : LEGACY_ON_HIT) {
            JsonElement value = onHit.get(key);
            if (value == null) {
                continue;
            }
            if (!key.equals("fire") || !value.isJsonObject()) {
                flat.add(key);
            }
        }
        if (flat.isEmpty()) {
            return false;
        }
        report.error("'on_hit." + String.join("', 'on_hit.", flat) + "' belong to the old flat layout and "
                + "cannot be read. Each hit effect now has its own subcategory with a single 'ticks' "
                + "value, so use "
                + "'on_hit.fire': { \"enabled\": true, \"chance\": 1.0, \"ticks\": 100 } "
                + "and 'on_hit.status': [ { \"enabled\": true, \"chance\": 1.0, \"effect\": "
                + "\"minecraft:poison\", \"ticks\": 80, \"amplifier\": 0 } ] - the file is skipped");
        return true;
    }

    private static void checkSubKeys(JsonObject node, Class<?> type, String path, TierReport report) {
        Map<String, KeyType> known = knownFields(type);
        Set<String> unknown = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            String key = entry.getKey();
            KeyType field = known.get(key);
            if (field == null) {
                unknown.add(key);
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonNull()) {
                report.warn("'" + path + "." + key + "' is null, so its default is used");
            } else if (!fits(field, value)) {
                report.warn("'" + path + "." + key + "' should be " + describe(field.raw()) + ", found "
                        + describe(value) + "; the value is read as best it can be");
            }
        }
        if (!unknown.isEmpty()) {
            report.warn("unknown " + (unknown.size() == 1 ? "key " : "keys ")
                    + path + "." + String.join(", ", unknown) + " - ignored. Known keys: "
                    + String.join(", ", known.keySet()));
        }
    }

    private static void checkDefinition(TierDefinition def, JsonObject root, TierReport report) {
        RecipeType type = recipeType(def, report);
        String name = type.name().toLowerCase(Locale.ROOT);

        if (type == RecipeType.NORMAL || type == RecipeType.SMITHING) {
            if (def.hasBaseMaterial()) {
                checkIngredientSyntax(def.recipe.base_material, "base_material", report);
            } else {
                report.error("'recipe_type' is '" + name + "' but 'base_material' is missing, so no "
                        + "recipe can be built; set it or use 'recipe_type': 'none'");
            }
        }
        if (def.hasIngotMaterial()) {
            checkIngredientSyntax(def.recipe.ingot_material, "ingot_material", report);
        }
        if (def.hasRepairItem()) {
            checkIngredientSyntax(def.properties.repair_item, "repair_item", report);
        }

        if (type == RecipeType.SMITHING) {
            if (!def.hasBaseTier()) {
                report.error("'recipe_type' is 'smithing' but 'base_tier' is empty, so no weapon of "
                        + "the base mod would be upgraded");
            } else if (def.tier.base_tier.trim().equalsIgnoreCase(def.id)) {
                report.error("'base_tier' is this tier's own id ('" + def.id + "'), so every upgrade "
                        + "would try to transform the item it produces");
            }
            if (!def.hasSmithingTemplate()) {
                report.error("'recipe_type' is 'smithing' but 'smithing_template' is empty, so no "
                        + "upgrade can be forged");
            }
            if (def.hasCustomSmithingTable()) {
                checkIngredientSyntax(def.smithingTable(), "smithing_table", report);
            }
        } else {
            for (String key : SMITHING_ONLY) {
                if (root.has(key) && present(root.get(key))) {
                    report.warn("'" + key + "' is set but only used when 'recipe_type' is 'smithing', "
                            + "which this tier is not");
                }
            }
        }

        if (def.properties.color != null && !colorLike(def.properties.color)) {
            report.warn("'color' '" + def.properties.color + "' is not a #RRGGBB colour, so the items are "
                    + "tinted white");
        }
        if (def.properties.durability <= 0) {
            report.error("'durability' is " + def.properties.durability + ", so every weapon of the tier "
                    + "breaks as soon as it is used");
        }
        if (!Float.isFinite(def.properties.speed) || def.properties.speed <= 0) {
            report.error("'speed' is " + def.properties.speed + ", but it is the weapons' attack speed and has "
                    + "to be a positive number");
        }
        if (!Float.isFinite(def.properties.attack_damage_bonus)) {
            report.error("'attack_damage_bonus' is " + def.properties.attack_damage_bonus + ", which is not a number");
        }
        if (def.properties.enchantability < 0) {
            report.warn("'enchantability' is negative (" + def.properties.enchantability + ")");
        }
        if (def.properties.harvest_level < 0) {
            report.warn("'harvest_level' is negative (" + def.properties.harvest_level + ")");
        }
        checkTooltip(def, report);
        for (String code : def.translations.keySet()) {
            if (code.isBlank()) {
                report.warn("'translations' has a key that is empty, so no language file is written for it");
            } else if (!code.equals(code.toLowerCase(Locale.ROOT))) {
                report.warn("'translations' key '" + code + "' is not lowercase; Minecraft reads '"
                        + code.toLowerCase(Locale.ROOT) + "' instead, so this entry is never used");
            }
        }
        untranslatedBaseModLanguages(def, report);
        materialType(def, report);
        tierWeapons(def, report);
        weaponChoice(def, report);
        rarity(def, report);
        nameOverrides(def, report);
        missingBaseWeaponData(def, report);
        onHit(def, report);
        checkTags(def, report);
    }

    private static void nameOverrides(TierDefinition def, TierReport report) {
        if (def.name_overrides == null || def.name_overrides.isEmpty()) {
            return;
        }
        Set<String> known = new LinkedHashSet<>();
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            known.add(weapon.name());
            known.add(weapon.registryName());
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, String>> entry : def.name_overrides.entrySet()) {
            String weapon = entry.getKey() == null ? null : entry.getKey().trim();
            if (weapon == null || weapon.isEmpty()) {
                report.error("'name_overrides' has a key that is empty, so it names no weapon");
                continue;
            }
            if (!known.contains(weapon)) {
                unknown.add(weapon);
            }
            Map<String, String> names = entry.getValue();
            if (names == null || names.isEmpty()) {
                report.error("'name_overrides." + weapon + "' has no languages, so the name is never "
                        + "used; write at least 'en_us'");
                continue;
            }
            for (Map.Entry<String, String> name : names.entrySet()) {
                String code = name.getKey() == null ? "" : name.getKey().trim();
                if (code.isEmpty()) {
                    report.error("'name_overrides." + weapon + "' has a language that is empty");
                } else if (!code.equals(code.toLowerCase(Locale.ROOT))) {
                    report.warn("'name_overrides." + weapon + "' key '" + code + "' is not lowercase; "
                            + "Minecraft reads '" + code.toLowerCase(Locale.ROOT) + "' instead, so "
                            + "this name is never used");
                }
                if (name.getValue() == null || name.getValue().isBlank()) {
                    report.warn("'name_overrides." + weapon + "." + code + "' is empty, so the name "
                            + "is worked out from the base mod's names instead");
                }
            }
        }
        if (!unknown.isEmpty()) {
            report.error("'name_overrides' names " + quoted(unknown) + ", which are not weapons of "
                    + "the base mod, so those names are never used. Use one of: "
                    + String.join(", ", known));
        }
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            boolean byItem = def.name_overrides.containsKey(weapon.registryName());
            boolean byKind = def.name_overrides.containsKey(weapon.name());
            if (byItem && byKind && !weapon.name().equals(weapon.registryName())) {
                report.warn("'name_overrides' has both '" + weapon.registryName() + "' and '"
                        + weapon.name() + "', which are the same weapon; only '"
                        + weapon.registryName() + "' is read");
            }
        }
    }

    private static void checkTags(TierDefinition def, TierReport report) {
        if (def.tags == null || def.tags.isEmpty()) {
            return;
        }
        checkTagIds("tags.tier", def.tags.tier, report);
        checkTagIds("tags.materials", def.tags.materials, report);
        Set<String> generated = new LinkedHashSet<>();
        for (WeaponSpec weapon : def.weapons()) {
            generated.add(weapon.name());
            generated.add(weapon.registryName());
        }
        for (Map.Entry<String, List<String>> entry : def.tags.items.entrySet()) {
            String weapon = entry.getKey() == null ? "" : entry.getKey().trim();
            if (weapon.isEmpty()) {
                report.error("'tags.items' has a key that is empty, so it names no weapon");
                continue;
            }
            if (!generated.contains(weapon)) {
                report.error("'tags.items' names '" + weapon + "', which this "
                        + (def.standalone ? "item" : "tier") + " does not generate, so that tag is "
                        + "never applied. Use one of: " + String.join(", ", generated));
            }
            checkTagIds("tags.items." + weapon, entry.getValue(), report);
        }
        if (!def.tags.materials.isEmpty() && !def.hasMaterials()) {
            report.warn("'tags.materials' only applies to tiers that build the two material items, "
                    + "and this file is a single weapon, so those tags are never applied");
        }
        checkTierTag(def, report);
    }

    private static void checkTierTag(TierDefinition def, TierReport report) {
        TierTagSection group = def.tags.tier_tag;
        if (group == null) {
            return;
        }
        if (def.standalone) {
            boolean configured = !group.isDefault() || !group.enabled;
            if (configured) {
                report.warn("'tags.tier_tag' only works for tiers, and this file is a single weapon, "
                        + "so no tier tag is created");
            }
            return;
        }
        if (group.name != null && !group.name.isBlank()) {
            String id = group.name.trim();
            String qualified = id.indexOf(':') < 0
                    ? JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + id
                    : id;
            if (ResourceLocation.tryParse(qualified) == null) {
                report.error("'tags.tier_tag.name' is '" + group.name + "', which is not a valid tag "
                        + "id; write it as 'namespace:path' like '"
                        + JaamsWeaponryExMod.ITEM_NAMESPACE + ":tier/" + def.id + "'");
            }
        }
        checkItemFilter("tags.tier_tag", "exclude_items", group.exclude_items, report, false);
        for (String entry : group.include_items) {
            String value = entry == null ? "" : entry.trim();
            if (value.isEmpty()) {
                report.error("'tags.tier_tag.include_items' has an empty value; write it as "
                        + "'namespace:path' or '#namespace:path'");
                continue;
            }
            String id = value.charAt(0) == '#' ? value.substring(1) : value;
            if (id.indexOf(':') < 0) {
                id = JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + id;
            }
            if (ResourceLocation.tryParse(id) == null) {
                report.error("'tags.tier_tag.include_items' has '" + value + "', which is not a valid "
                        + "item or tag id; write it as 'namespace:path' or '#namespace:path'");
            }
        }
    }

    private static void checkTagIds(String path, List<String> entries, TierReport report) {
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                report.error("'" + path + "' has an empty value; write it as 'namespace:path' like "
                        + "'" + JaamsWeaponryExMod.BASE_NAMESPACE + ":trait/decapitation'");
                continue;
            }
            String value = entry.trim();
            String id = value.indexOf(':') < 0 ? JaamsWeaponryExMod.BASE_NAMESPACE + ":" + value : value;
            if (ResourceLocation.tryParse(id) == null) {
                report.error("'" + path + "' has '" + value + "', which is not a valid tag id; write "
                        + "it as 'namespace:path' like '" + JaamsWeaponryExMod.BASE_NAMESPACE
                        + ":trait/decapitation'");
            }
        }
    }

    private static void missingBaseWeaponData(TierDefinition def, TierReport report) {
        List<String> noPreset = new ArrayList<>();
        List<String> noCapability = new ArrayList<>();
        for (WeaponSpec weapon : def.weapons()) {
            if (!PackFactory.hasWeaponPreset(weapon)) {
                noPreset.add(weapon.name());
            }
            if (!PackFactory.hasWeaponCapability(weapon)) {
                noCapability.add(weapon.name());
            }
        }
        if (!noPreset.isEmpty()) {
            report.warn("the base mod ships no weapon_attributes preset for " + String.join(", ", noPreset)
                    + ", so no attribute data is written for the " + noPreset.size()
                    + " item(s) this tier makes from them, and they use the base mod's defaults");
        }
        if (!noCapability.isEmpty()) {
            report.warn("the base mod ships no capability data for " + String.join(", ", noCapability)
                    + ", so the " + noCapability.size() + " item(s) this tier makes from them have "
                    + "no capability of their own, like bare hands");
        }
    }

    private static void checkTooltip(TierDefinition def, TierReport report) {
        if (def.tooltip == null) {
            return;
        }
        checkItemFilter("tooltip", "only_items", def.tooltip.only_items, report, true);
        checkExclusions("tooltip", def.tooltip.exclude_items, report);
        if (ItemFilters.isWildcarded(def.tooltip.only_items) && ItemFilters.isWildcarded(def.tooltip.exclude_items)) {
            report.warn("'tooltip.only_items' is '" + ItemFilters.EXCLUDE_ALL
                    + "' and so is 'tooltip.exclude_items', so no weapon of this tier ever "
                    + "shows these tooltip lines");
        }
        for (int i = 0; i < def.tooltip.lines.size(); i++) {
            String line = def.tooltip.lines.get(i);
            if (line == null || line.isBlank()) {
                report.info("'tooltip' line " + (i + 1) + " is empty, so no line is drawn for it");
            } else if (line.length() > MAX_TOOLTIP_LINE) {
                report.warn("'tooltip' line " + (i + 1) + " is " + line.length() + " characters "
                        + "long; lines longer than " + MAX_TOOLTIP_LINE + " are cut off in game");
            } else if (looksLikeKey(line) && !TierTooltip.isTranslationKey(line)) {
                report.warn("'tooltip' line " + (i + 1) + " looks like a translation key but '"
                        + line.trim() + "' is not one; keys are written as 'namespace:path' in "
                        + "lowercase, and anything else is drawn as plain text");
            }
        }
    }

    private static boolean looksLikeKey(String line) {
        String value = line.trim();
        return value.indexOf(':') > 0 && value.indexOf(' ') < 0;
    }

    private static void onHit(TierDefinition def, TierReport report) {
        if (def.on_hit == null) {
            return;
        }
        for (HitEffectRef ref : def.on_hit.entries()) {
            HitEffect effect = ref.effect();
            if (effect == null) {
                continue;
            }
            String path = ref.path();
            if (effect.chance < 0.0F || effect.chance > 1.0F) {
                report.warn("'" + path + ".chance' is " + effect.chance + ", but it is read as the "
                        + "chance of the effect being applied, so it has to be between 0 and 1; 1 is used");
                effect.chance = 1.0F;
            }
            if (effect.hasDuration()) {
                if (effect.enabled && effect.duration() <= 0) {
                    report.warn("'" + path + "' is enabled but '" + path + ".ticks' is "
                            + effect.duration() + ", so it never applies");
                } else if (effect.duration() > MAX_SANE_TICKS) {
                    report.warn("'" + path + ".ticks' is " + effect.duration() + ", which is over "
                            + (MAX_SANE_TICKS / 20) + "s), so the effect "
                            + "stays on the enemy for a very long time");
                }
            }
            if (effect instanceof StatusEffectHit status) {
                if (status.resolve() == null) {
                    report.warn("'" + path + ".effect' is '" + status.effect + "', which is not a known "
                            + "mob effect, so the effect never applies");
                }
            }
            if (effect instanceof ExplosionHit blast) {
                checkAmount(path, "power", blast.power, MAX_SANE_POWER, report, true);
                if (blast.enabled && blast.breaks_blocks) {
                    report.warn("'" + path + ".breaks_blocks' is true, so every hit with these weapons "
                            + "destroys the terrain around the enemy, not just the enemy");
                }
            }
            if (effect instanceof LifestealHit drain) {
                checkAmount(path, "amount", drain.amount, MAX_SANE_HEALING, report, true);
            }
            if (effect instanceof BonusDamageHit bonus) {
                String mode = bonus.mode == null ? "" : bonus.mode.trim().toLowerCase(Locale.ROOT);
                if (!BonusDamageHit.MODES.contains(mode)) {
                    report.error("'" + path + ".mode' is '" + bonus.mode + "', but only "
                            + String.join(", ", BonusDamageHit.MODES) + " are known; 'flat' is used");
                    bonus.mode = BonusDamageHit.FLAT;
                    checkAmount(path, "amount", bonus.amount, MAX_SANE_DAMAGE, report, true);
                } else if (BonusDamageHit.FLAT.equals(mode)) {
                    checkAmount(path, "amount", bonus.amount, MAX_SANE_DAMAGE, report, true);
                } else {
                    checkAmount(path, "amount", bonus.amount, MAX_SANE_PERCENT, report, true);
                }
                checkEntityFilter(path, bonus.targets, bonus.enabled, report);
            }
            if (effect instanceof KnockbackHit knock) {
                checkAmount(path, "power", knock.power, MAX_SANE_KNOCKBACK, report, false);
            }
            if (effect instanceof LaunchHit launch) {
                checkAmount(path, "power", launch.power, MAX_SANE_LAUNCH, report, false);
            }
            if (effect instanceof SummonHit summon) {
                if (summon.count > MAX_SANE_SUMMONS) {
                    report.warn("'" + path + ".count' is " + summon.count + ", so a single hit spawns "
                            + summon.count + " entities; at most " + MAX_SANE_SUMMONS + " are spawned");
                    summon.count = MAX_SANE_SUMMONS;
                } else if (summon.enabled && summon.count < 0) {
                    report.warn("'" + path + ".count' is " + summon.count + ", so it never spawns anything");
                    summon.count = 0;
                }
                checkAmount(path, "spread", summon.spread, MAX_SANE_RADIUS, report, false);
                if (summon.enabled && summon.resolve() == null) {
                    report.error("'" + path + ".entity' is '" + summon.entity + "', which is not an entity "
                            + "type, so nothing is ever spawned; write it as 'minecraft:zombie'");
                }
            }
            if (effect instanceof ChainHit chain) {
                checkAmount(path, "amount", chain.amount, MAX_SANE_DAMAGE, report, true);
                checkAmount(path, "radius", chain.radius, MAX_SANE_RADIUS, report, false);
                checkCount(path, "max_targets", chain.max_targets, MAX_SANE_TARGETS, report);
                checkEntityFilter(path, chain.targets, chain.enabled, report);
            }
            if (effect instanceof SpreadHit spread) {
                checkAmount(path, "radius", spread.radius, MAX_SANE_RADIUS, report, false);
                checkCount(path, "max_targets", spread.max_targets, MAX_SANE_TARGETS, report);
                checkEntityFilter(path, spread.targets, spread.enabled, report);
                if (spread.enabled && spread.instance() == null) {
                    report.error("'" + path + ".effect' is '" + spread.effect + "' with "
                            + spread.ticks + " ticks, which never applies; write it as "
                            + "'minecraft:poison' with 'ticks' over 0");
                }
            }
            if (effect instanceof ParticleHit dust) {
                checkAmount(path, "radius", dust.radius, MAX_SANE_RADIUS, report, false);
                checkCount(path, "count", dust.count, MAX_SANE_PARTICLES, report);
                if (dust.speed < 0.0D) {
                    report.warn("'" + path + ".speed' is " + dust.speed + ", which pushes the particles "
                            + "towards the enemy; it is read as a speed, so 0 is used");
                    dust.speed = 0.0D;
                }
                if (dust.enabled && dust.resolve() == null) {
                    report.error("'" + path + ".particle' is '" + dust.particle + "', which is not a "
                            + "particle type, so no particles appear; write it as 'minecraft:crit'");
                }
            }
            if (effect.show_tooltip) {
                if (!effect.enabled) {
                    report.info("'" + path + ".show_tooltip' is true, but '" + path + "' is not "
                            + "enabled, so no line is drawn for it on the weapons");
                } else if (!effect.canDescribe()) {
                    report.warn("'" + path + ".show_tooltip' is true, but the effect cannot be "
                            + "named, so no line is drawn for it on the weapons");
                }
            }
            checkItemFilter(path, "only_items", effect.only_items, report, true);
            checkExclusions(path, effect.exclude_items, report);
            if (ItemFilters.isWildcarded(effect.only_items) && ItemFilters.isWildcarded(effect.exclude_items)) {
                report.warn("'" + path + ".only_items' is '" + ItemFilters.EXCLUDE_ALL
                        + "' and so is '" + path + ".exclude_items', so '" + path + "' can never apply");
            }
            if (!effect.at_max_strength && !effect.below_max_strength) {
                report.error("'" + path + ".at_max_strength' and '" + path + ".below_max_strength' are both "
                        + "false, so '" + path + "' can never apply; leave at least one of them true");
            } else if (effect.at_max_strength != effect.below_max_strength) {
                report.info("'" + path + "' only applies " + (effect.at_max_strength
                        ? "when the attack strength scale is at its maximum"
                        : "when the attack strength scale is below its maximum"));
            }
            if (!effect.enabled && effect.at_max_strength != effect.below_max_strength) {
                report.info("'" + path + "' is not enabled, so its 'at_max_strength' and "
                        + "'below_max_strength' options are not used");
            }
            if (!effect.enabled && effect.chance != 1.0F) {
                report.info("'" + path + "' is not enabled, so its 'chance' and 'ticks' are not used");
            }
        }
    }

    private static void checkExclusions(String path, List<String> entries, TierReport report) {
        checkItemFilter(path, "exclude_items", entries, report, true);
    }

    private static void checkItemFilter(String path, String key, List<String> entries, TierReport report,
            boolean tagsAllowed) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Set<String> unknown = new LinkedHashSet<>();
        Set<String> known = new LinkedHashSet<>();
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            known.add(weapon.name());
        }
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (ItemFilters.EXCLUDE_ALL.equals(value)) {
                continue;
            }
            if (value.charAt(0) == '#') {
                String id = value.substring(1);
                if (ResourceLocation.tryParse(id) == null) {
                    report.error("'" + path + "." + key + "' has '" + value + "', which is not a "
                            + "valid tag id; write it as '#namespace:path'");
                } else if (!tagsAllowed) {
                    report.error("'" + path + "." + key + "' has '" + value + "', but tags are not "
                            + "read here: the items of a tier are decided before any item tag exists, "
                            + "so only weapon names and '*' work. List the weapons instead");
                }
                continue;
            }
            if (!known.contains(value)) {
                unknown.add(value);
            }
        }
        if (!unknown.isEmpty()) {
            report.error("'" + path + "." + key + "' names " + quoted(unknown)
                    + ", which are not weapons of the base mod, so they match nothing. Use '*' for "
                    + "every weapon" + (tagsAllowed ? ", '#namespace:path' for a tag" : "")
                    + ", or one of: " + String.join(", ", known));
        }
    }

    private static void checkCount(String path, String key, int value, int max, TierReport report) {
        if (value < 0) {
            report.warn("'" + path + "." + key + "' is " + value + ", so the effect never touches "
                    + "anything; 0 is used");
        } else if (value > max) {
            report.warn("'" + path + "." + key + "' is " + value + ", so one hit can affect that many "
                    + "entities; at most " + max + " are affected");
        }
    }

    private static void checkAmount(String path, String key, float value, float max, TierReport report,
            boolean fatal) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            report.error("'" + path + "." + key + "' is " + value + ", which is not a number, so the "
                    + "effect never applies");
            return;
        }
        if (value <= 0.0F) {
            if (fatal) {
                report.warn("'" + path + "." + key + "' is " + value + ", so the effect never applies");
            }
        } else if (value > max) {
            report.warn("'" + path + "." + key + "' is " + value + ", which is over " + max
                    + ", so the effect is far stronger than any weapon of the base mod");
        }
    }

    private static void checkEntityFilter(String path, List<String> entries, boolean enabled,
            TierReport report) {
        if (entries == null || entries.isEmpty()) {
            if (enabled) {
                report.warn("'" + path + ".targets' is empty, so the effect matches no enemy and never "
                        + "applies; write '*' for every enemy, 'minecraft:zombie' for one kind, or "
                        + "'#minecraft:skeletons' for a tag");
            }
            return;
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (EntityFilters.ANY.equals(value)) {
                continue;
            }
            if (value.charAt(0) == '#') {
                ResourceLocation tag = ResourceLocation.tryParse(value.substring(1));
                if (tag == null) {
                    report.error("'" + path + ".targets' has '" + value + "', which is not a valid tag "
                            + "id; write it as '#namespace:path'");
                }
                continue;
            }
            ResourceLocation id = EntityFilters.idOf(value);
            if (id == null) {
                report.error("'" + path + ".targets' has '" + value + "', which is not a valid entity "
                        + "type id; write it as 'minecraft:zombie' or as '#namespace:path' for a tag");
            } else if (ForgeRegistries.ENTITY_TYPES.getValue(id) == null) {
                unknown.add(value);
            }
        }
        if (!unknown.isEmpty()) {
            report.error("'" + path + ".targets' names " + quoted(unknown) + ", which are not entity "
                    + "types, so the effect never applies to them. Use '*' for every enemy, or "
                    + "'#namespace:path' for a tag");
        }
    }

    private static void rarity(TierDefinition def, TierReport report) {
        String value = def.properties.rarity;
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String name = value.trim().toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1).trim();
        }
        for (Rarity known : Rarity.values()) {
            if (known.name().equalsIgnoreCase(name)) {
                return;
            }
        }
        report.warn("'rarity' is '" + value + "', but only " + java.util.Arrays.toString(Rarity.values())
                + " are known; the items are named in the common colour");
    }

    public static void checkNameCollisions(TierDefinition def, Collection<TierDefinition> accepted,
            TierReport report) {
        if (def == null || !def.enabled || def.standalone || accepted.isEmpty()) {
            return;
        }
        for (TierDefinition other : accepted) {
            if (other == def || !other.enabled) {
                continue;
            }

            for (String mine : List.of(def.naturalHeavyName(), def.naturalDoubleName())) {
                for (String theirs : List.of(other.naturalHeavyName(), other.naturalDoubleName())) {
                    if (mine.equals(theirs)) {
                        report.warn("'" + NAMESPACE + ":" + mine + "' is what this tier and tier '"
                                + other.id + "' are both named after, so only the first of them can "
                                + "be called that; the other gets its own id put into its name");
                        return;
                    }
                }
            }
        }
    }

    private static void tierWeapons(TierDefinition def, TierReport report) {
        checkItemFilter("tier", "only_items", def.tier.only_items, report, false);
        checkItemFilter("tier", "exclude_items", def.tier.exclude_items, report, false);
        if (def.standalone) {
            return;
        }
        if (def.tier.only_items.isEmpty() && def.tier.exclude_items.isEmpty()) {
            return;
        }
        List<String> itemOnly = new ArrayList<>();
        for (String entry : def.tier.only_items) {
            if (entry == null) {
                continue;
            }
            WeaponSpec spec = WeaponSpec.byName(entry.trim());
            if (spec != null && spec.itemOnly()) {
                itemOnly.add(spec.name());
            }
        }
        if (!itemOnly.isEmpty()) {
            report.error("'tier.only_items' names " + quoted(itemOnly) + ", which are only built from "
                    + "the 'item' folder as a single weapon, so no tier can make " + (itemOnly.size() == 1
                            ? "it" : "them") + ". Move " + (itemOnly.size() == 1 ? "it" : "them")
                    + " to the 'item' folder and use 'weapon' there instead");
        }
        List<String> dropped = new ArrayList<>();
        if (!ItemFilters.isWildcarded(def.tier.only_items)) {
            for (String entry : def.tier.exclude_items) {
                if (entry != null && ItemFilters.matchesName(def.tier.only_items, entry.trim())) {
                    dropped.add(entry.trim());
                }
            }
        }
        if (!dropped.isEmpty()) {
            report.warn("'tier.exclude_items' names " + quoted(dropped) + ", which 'tier.only_items' "
                    + "also lists, so those weapons are left out either way");
        }

        List<WeaponSpec> weapons = def.weapons();
        if (weapons.isEmpty()) {
            report.error("'tier.only_items' and 'tier.exclude_items' together leave this tier with no "
                    + "weapons at all, so only its two material items are registered");
            return;
        }
        if (!def.tier.only_items.isEmpty()) {
            List<String> names = new ArrayList<>();
            boolean ranged = false;
            for (WeaponSpec weapon : weapons) {
                names.add(weapon.name());
                ranged = ranged || WeaponSpec.RANGED_WEAPONS.contains(weapon.name());
            }
            report.info("'tier.only_items' makes this tier " + String.join(", ", names) + (ranged
                    ? ", and it already picks the ranged weapon, so 'tier.ranged_tiered' is not read"
                    : ". It lists no ranged weapon, so 'tier.ranged_tiered' adds none either"));
        } else {
            List<String> names = new ArrayList<>();
            for (WeaponSpec weapon : weapons) {
                names.add(weapon.name());
            }
            report.info("'tier.exclude_items' leaves this tier with " + String.join(", ", names));
        }
    }

    private static void weaponChoice(TierDefinition def, TierReport report) {
        String value = def.weapon == null ? "" : def.weapon.trim();
        if (value.isEmpty()) {
            return;
        }
        if (def.standalone) {
            return;
        }
        if (WeaponSpec.byName(value) == null) {
            report.error("'weapon' is '" + def.weapon + "', which is not one of the weapons of the base "
                    + "mod, so nothing can be built from it. Use one of: " + knownWeapons());
            return;
        }
        report.error("'weapon' only works inside the 'item' folder, which creates a single weapon. "
                + "This file is in the 'tier' folder, so move it there to use 'weapon', or use "
                + "'tier.only_items' instead");
    }

    private static String knownWeapons() {
        List<String> names = new ArrayList<>();
        for (WeaponSpec weapon : WeaponSpec.WEAPONS) {
            names.add(weapon.name());
        }
        return String.join(", ", names);
    }

    private static void materialType(TierDefinition def, TierReport report) {
        if (!def.tier.base_tags) {
            report.info("'base_tags' is false, so this tier's weapons are not added to the base "
                    + "mod's item tags; they are only in '" + NAMESPACE + ":item/*', so recipes, "
                    + "loot and quests that look for the base tags will not find them");
        }
        String value = def.tier.material_type;
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            MaterialType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            report.error("'material_type' is '" + value + "', but only "
                    + java.util.Arrays.toString(MaterialType.values()) + " are known; the tier falls "
                    + "back to 'ingot'");
        }
    }

    private static void untranslatedBaseModLanguages(TierDefinition def, TierReport report) {
        Set<String> missing = new LinkedHashSet<>(BaseLang.shipped());
        missing.remove("en_us");
        missing.removeAll(def.translations.keySet());
        if (missing.isEmpty()) {
            return;
        }
        report.warn("'translations' has no entry for " + String.join(", ", missing)
                + ", which the base mod is written in, so this tier's weapons are named "
                + "after '" + def.display_name + "' there");
    }

    private static RecipeType recipeType(TierDefinition def, TierReport report) {
        if (def.recipe.recipe_type == null || def.recipe.recipe_type.isBlank()) {
            def.recipe.recipe_type = "none";
            return RecipeType.NONE;
        }
        try {
            return RecipeType.valueOf(def.recipe.recipe_type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            report.error("'recipe_type' '" + def.recipe.recipe_type + "' is not one of normal, smithing or "
                    + "none, so no recipes are generated for this tier");
            def.recipe.recipe_type = "none";
            return RecipeType.NONE;
        }
    }

    public static void audit(TierDefinition def, TierReport report, boolean checkTags) {
        if (def == null || report.audited()) {
            return;
        }
        report.markAudited();
        try {
            if (!registryUsable()) {
                report.info("the item registry is not usable yet, so the item and tag checks "
                        + "were skipped");
                return;
            }
            if (def.hasRepairItem()) {
                reportIngredient(def.properties.repair_item, "repair_item", report, checkTags, true);
            }
            if (def.hasBaseMaterial()) {
                reportIngredient(def.recipe.base_material, "base_material", report, checkTags, true);
            }
            if (def.hasIngotMaterial()) {
                reportIngredient(def.recipe.ingot_material, "ingot_material", report, checkTags, true);
            }
            if (def.getRecipeType() == RecipeType.SMITHING) {
                auditSmithing(def, report, checkTags);
            }
        } catch (Exception e) {
            report.warn("the registry checks could not be completed: " + e);
        }
    }

    private static void auditSmithing(TierDefinition def, TierReport report, boolean checkTags) {
        if (def.hasSmithingTemplate()) {
            reportIngredient(def.recipe.smithing_template, "smithing_template", report, checkTags, true);
        }
        if (def.hasCustomSmithingTable()) {

            reportIngredient(def.recipe.smithing_table, "smithing_table", report, checkTags, false);
        }
        if (!def.hasBaseTier()) {
            return;
        }
            String prefix = def.tier.base_tier.trim() + "_";
            List<String> missing = new ArrayList<>();
            for (RecipeSpec spec : RecipeSpec.RECIPES) {
                WeaponSpec weapon = WeaponSpec.byName(spec.weapon());
                String baseWeapon = weapon != null ? weapon.registryName() : spec.weapon();
                if (!baseShips(prefix + baseWeapon)) {
                    missing.add(baseWeapon);
                }
            }
            if (missing.size() == RecipeSpec.RECIPES.size()) {
                report.error("'base_tier' '" + def.tier.base_tier.trim() + "' is not a tier the base "
                        + "mod ships: it has no '" + JaamsWeaponryExMod.BASE_NAMESPACE + ":" + prefix + "*' items, so none "
                        + "of its upgrades can be performed." + shippedTiersHint());
            } else if (!missing.isEmpty()) {
                report.info((RecipeSpec.RECIPES.size() - missing.size()) + " of " + RecipeSpec.RECIPES.size()
                        + " upgrades will be generated; the base mod ships no '" + def.tier.base_tier.trim()
                        + "' version of " + join(missing));
        }
    }

    private static void reportIngredient(String value, String field, TierReport report,
            boolean checkTags, boolean fatal) {
        String trimmed = value.trim();
        boolean tag = trimmed.startsWith("#");
        String id = tag ? trimmed.substring(1) : trimmed;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {

            if (!fatal) {
                report.warn("'" + field + "' '" + value + "' is not a valid item id, so the recipes "
                        + "are offered at the vanilla smithing table instead");
            }
            return;
        }
        if (tag) {
            if (checkTags && !tagExists(key)) {
                report.error("'" + field + "' names the tag '#" + key + "', which no item is in yet");
            }
        } else if (!ForgeRegistries.ITEMS.containsKey(key)) {
            report.error("'" + field + "' names '" + key + "', which no registered item is"
                    + (fatal ? "; the tier falls back to its own ingots" : ", so the recipes are "
                    + "offered at the vanilla smithing table instead"));
        }
    }

    private static void checkIngredientSyntax(String value, String field, TierReport report) {
        String trimmed = value == null ? "" : value.trim();
        boolean tag = trimmed.startsWith("#");
        String id = tag ? trimmed.substring(1) : trimmed;
        if (id.isEmpty() || ResourceLocation.tryParse(id) == null) {
            report.error("'" + field + "' '" + value + "' is not a valid " + (tag ? "tag" : "item")
                    + " id - it is ignored");
        }
    }

    private static boolean baseShips(String path) {
        return BaseLang.ships(path, true);
    }

    private static String shippedTiersHint() {
        List<String> tiers = BaseLang.registeredTiers();
        if (tiers.isEmpty()) {
            return "";
        }
        return " The tiers it does ship: " + String.join(", ", tiers) + ".";
    }

    private static boolean exists(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && ForgeRegistries.ITEMS.containsKey(key);
    }

    private static boolean registryUsable() {
        var items = ForgeRegistries.ITEMS;
        if (items.isEmpty()) {
            return false;
        }
        ResourceLocation vanilla = ResourceLocation.tryParse("minecraft:stone");
        return vanilla != null && items.containsKey(vanilla);
    }

    private static boolean tagExists(ResourceLocation id) {
        return ForgeRegistries.ITEMS.tags().isKnownTagName(ItemTags.create(id));
    }

    private static boolean fits(KeyType field, JsonElement value) {
        if (value instanceof JsonPrimitive primitive) {
            Class<?> raw = field.raw();
            if (raw == String.class) {
                return primitive.isString();
            }
            if (raw == boolean.class || raw == Boolean.class) {
                return primitive.isBoolean();
            }
            if (raw == int.class || raw == float.class
                    || raw == Integer.class || raw == Float.class) {
                return primitive.isNumber();
            }
            return true;
        }
        if (value.isJsonObject()) {
            return Map.class.isAssignableFrom(field.raw()) || isPlainObject(field.raw());
        }
        if (value.isJsonArray()) {
            Class<?> element = elementType(field);
            if (element == null) {
                return false;
            }
            for (JsonElement child : value.getAsJsonArray()) {
                if (!fits(new KeyType(element, element), child)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static boolean isPlainObject(Class<?> field) {
        if (field.isPrimitive() || field.isEnum() || field.isArray() || field == Object.class) {
            return false;
        }
        return field != String.class && !Number.class.isAssignableFrom(field)
                && !Boolean.class.isAssignableFrom(field) && !Character.class.isAssignableFrom(field);
    }

    private static Class<?> elementType(KeyType field) {
        if (!Collection.class.isAssignableFrom(field.raw())) {
            return null;
        }
        if (field.generic() instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 1 && arguments[0] instanceof Class<?> raw) {
                return raw;
            }
        }
        return Object.class;
    }

    private static String describe(Class<?> field) {
        if (field == String.class) {
            return "a string";
        }
        if (field == boolean.class || field == Boolean.class) {
            return "true or false";
        }
        if (field == int.class || field == float.class
                || field == Integer.class || field == Float.class) {
            return "a number";
        }
        if (Collection.class.isAssignableFrom(field)) {
            return "a list of strings";
        }
        return "an object";
    }

    private static String describe(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "null";
        }
        if (value.isJsonArray()) {
            return "an array";
        }
        if (value.isJsonObject()) {
            return "an object";
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return "true or false";
        }
        return primitive.isString() ? "the string \"" + primitive.getAsString() + '"' : "a number";
    }

    private static boolean colorLike(String value) {
        String hex = value.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        } else if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6 && hex.length() != 8) {
            return false;
        }
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean present(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        return !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || !value.getAsString().isBlank();
    }

    private static String reason(JsonParseException e) {
        Throwable cause = e.getCause();
        String message = cause == null ? e.getMessage() : cause.getMessage();
        return message == null ? e.toString() : message;
    }

    private static String quoted(Collection<String> keys) {
        return keys.stream().map(key -> "'" + key + "'").collect(java.util.stream.Collectors.joining(", "));
    }

    private static String join(List<String> names) {
        return String.join(", ", names);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
