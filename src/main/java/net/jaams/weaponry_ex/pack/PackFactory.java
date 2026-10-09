package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.ItemFilters;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierSection.Density;
import net.jaams.weaponry_ex.data.TierSection.MaterialType;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.diag.TierDiagnostics;
import net.jaams.weaponry_ex.registry.TieredItems;
import net.jaams.weaponry_ex.registry.TieredItems.TierEntry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PackFactory {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String DESCRIPTION_KEY = "pack.jaams_weaponry_ex.description";

    private static final String PRESET_FOLDER = "data/jaams_weaponry/weapon_attributes/jaams_presets/";

    private static final String CAPABILITY_FOLDER = "data/jaams_weaponry/capabilities/weapons/";

    private static final List<String> CAPABILITY_TIERS = List.of(
            "shinerite", "wooden", "stone", "iron", "golden", "diamond", "netherite",
            "copper", "electrum", "enderium", "rosegold");

    private static final String BASE_DATA = "jaams_weaponry";

    private PackFactory() {
    }

    private static void putWeaponData(GeneratedPack resources, String folder, String weapon, byte[] data) {
        String path = folder + "/" + weapon + ".json";
        resources.put(ResourceLocation.tryBuild(BASE_DATA, path), data);
        resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE, path), data);
    }

    public static Pack clientPack() {
        GeneratedPack resources = new GeneratedPack("jaams_weaponry_ex_models", PackType.CLIENT_RESOURCES,
                "pack.jaams_weaponry_ex.description", JaamsWeaponryExMod.ITEM_NAMESPACE);

        Map<String, JsonObject> langs = new LinkedHashMap<>();
        for (TierEntry tier : TieredItems.entries()) {
            TierDefinition def = tier.definition();
            String base = def.id;

            for (Map.Entry<String, byte[]> texture : TextureSource.perTier(def).entrySet()) {
                resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                        texture.getKey()), texture.getValue());
            }

            int modelCount = 0;

            for (WeaponSpec weapon : def.weapons()) {
                modelCount += generateWeaponModels(resources, langs, tier, base, weapon);
            }

            MaterialType material = def.getMaterialType();
            String heavy = def.heavyMaterialName();
            String dbl = def.doubleMaterialName();
            int materialModels = 0;
            if (def.hasMaterials()) {
                materialModels = 2;
                resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                        "models/item/" + heavy + ".json"),
                        ModelSource.forTier(material.baseModelName(Density.HEAVY), def));
                resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                        "models/item/" + dbl + ".json"),
                        ModelSource.forTier(material.baseModelName(Density.LIGHT), def));
                for (String code : languagesOf(tier)) {
                    JsonObject lang = langs.computeIfAbsent(code, k -> new JsonObject());
                    lang.addProperty("item." + JaamsWeaponryExMod.ITEM_NAMESPACE + "." + heavy,
                            materialLabel(code, def, material, Density.HEAVY));
                    lang.addProperty("item." + JaamsWeaponryExMod.ITEM_NAMESPACE + "." + dbl,
                            materialLabel(code, def, material, Density.LIGHT));
                }
            }
            JaamsWeaponryExMod.LOGGER.debug("Generated {} models + {} textures for {} '{}'",
                    modelCount * 3 + materialModels,
                    TextureSource.perTier(def).size(), def.standalone ? "item" : "tier", base);
        }
        for (Map.Entry<String, JsonObject> entry : langs.entrySet()) {
            resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                    "lang/" + entry.getKey() + ".json"), GSON.toJson(entry.getValue()));
        }

        JaamsWeaponryExMod.LOGGER.debug("Client pack built for {} tier(s) with {} resources",
                TieredItems.entries().size(), resources.size());
        TierDiagnostics.finish(false);
        return wrap("jaams_weaponry_ex_models", resources, DESCRIPTION_KEY, PackType.CLIENT_RESOURCES);
    }

    private static int generateWeaponModels(GeneratedPack resources, Map<String, JsonObject> langs,
            TierEntry tier, String base, WeaponSpec weapon) {
        TierDefinition def = tier.definition();
        String modelName = "base_" + weapon.name();
        String itemName = base + "_" + weapon.registryName();
        resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                "models/item/" + itemName + ".json"), ModelSource.forTier(weapon.name(), modelName, def));

        String specName = base + "_" + weapon.name();
        List<String> subModelNames = specName.equals(itemName)
                ? List.of(itemName)
                : List.of(specName, itemName);
        for (String model : ModelSource.closure(weapon.name())) {
            if (model.equals(weapon.name())) {
                continue;
            }
            for (String name : subModelNames) {

                String pose = "base_" + model;
                resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                        "models/item/" + name + model.substring(weapon.name().length()) + ".json"),
                        ModelSource.forTier(weapon.name(), pose, def));
            }
        }
        resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                "item_skins/" + itemName + ".json"),
                ItemSkinSource.forTier(weapon.registryName(), tier.definition().getColor()));
        for (String code : languagesOf(tier)) {
            JsonObject lang = langs.computeIfAbsent(code, k -> new JsonObject());
            lang.addProperty("item." + JaamsWeaponryExMod.ITEM_NAMESPACE + "." + itemName,
                    weaponLabel(def, code, weapon, label(tier, code)));
        }
        return 1;
    }

    public static Pack serverPack() {
        GeneratedPack resources = new GeneratedPack("jaams_weaponry_ex_data", PackType.SERVER_DATA,
                "pack.jaams_weaponry_ex.description", BASE_DATA, JaamsWeaponryExMod.ITEM_NAMESPACE);

        Map<String, JsonArray> ownTags = new LinkedHashMap<>();
        Map<String, JsonArray> baseTags = new LinkedHashMap<>();
        Map<String, Map<String, LinkedHashSet<String>>> customTags = new LinkedHashMap<>();

        List<String> tierIds = new ArrayList<>();
        for (TierEntry tier : TieredItems.entries()) {
            TierDefinition def = tier.definition();
            String base = def.id;
            tierIds.add(base);
            boolean intoBase = def.tier.base_tags;

            for (WeaponSpec weapon : def.weapons()) {
                processWeapon(resources, ownTags, intoBase ? baseTags : null, customTags, def, base,
                        weapon);
            }
            processMaterialTags(customTags, def);
            processTierTag(customTags, def);
            RecipeFactory.add(resources, tier);
        }

        JsonArray tabEntries = buildTabEntries(TieredItems.entries(), JaamsWeaponryExMod.ITEM_NAMESPACE);

        for (Map.Entry<String, JsonArray> entry : ownTags.entrySet()) {
            resources.put(ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE,
                    tagPath(entry.getKey())), tagFile(entry.getValue()));
        }

        if (baseTags.isEmpty()) {
            JaamsWeaponryExMod.LOGGER.debug("No tier asked for the base mod's item tags, so "
                    + "{} tag file(s) are left alone. The weapons are only in this mod's own tags",
                    ownTags.size());
        } else {
            for (Map.Entry<String, JsonArray> entry : baseTags.entrySet()) {
                resources.put(ResourceLocation.tryBuild(BASE_DATA,
                        tagPath(entry.getKey())), tagFile(entry.getValue()));
            }
        }

        for (Map.Entry<String, Map<String, LinkedHashSet<String>>> namespace : customTags.entrySet()) {
            resources.allowNamespace(namespace.getKey());
            for (Map.Entry<String, LinkedHashSet<String>> entry : namespace.getValue().entrySet()) {
                JsonArray values = new JsonArray();
                for (String item : entry.getValue()) {
                    values.add(tagValue(item));
                }
                resources.put(ResourceLocation.tryBuild(namespace.getKey(), entry.getKey()),
                        tagFile(values));
            }
        }

        if (!tabEntries.isEmpty()) {
            JsonObject tab = new JsonObject();
            tab.addProperty("tab_id", "weaponry");
            tab.addProperty("priority", 0);
            tab.add("entries", tabEntries);
            resources.put(ResourceLocation.tryBuild("jaams_weaponry",
                    "jaams/tab_modifier/jaams_weaponry_ex_tiers.json"), GSON.toJson(tab));
        }

        JaamsWeaponryExMod.LOGGER.debug("Server data pack built for {} tier(s) with {} resources",
                TieredItems.entries().size(), resources.size());

        TierDiagnostics.finish(false);
        return wrap("jaams_weaponry_ex_data", resources, DESCRIPTION_KEY, PackType.SERVER_DATA);
    }

    private static void processWeapon(GeneratedPack resources, Map<String, JsonArray> ownTags,
            Map<String, JsonArray> baseTags, Map<String, Map<String, LinkedHashSet<String>>> customTags,
            TierDefinition def, String base, WeaponSpec weapon) {
        String itemName = base + "_" + weapon.registryName();
        String item = JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + itemName;
        List<String> tags = new ArrayList<>();
        tags.add(weapon.tag());
        tags.addAll(WeaponSpec.EXTRA_TAGS.getOrDefault(weapon.name(), List.of()));
        for (String tag : tags) {
            ownTags.computeIfAbsent(tag, k -> new JsonArray()).add(tagValue(item));
            if (baseTags != null) {
                baseTags.computeIfAbsent(tag, k -> new JsonArray()).add(tagValue(item));
            }
        }
        for (String tag : def.tags.tagsFor(weapon)) {
            addCustomTag(customTags, tag, item);
        }
        if (hasWeaponPreset(weapon)) {
            putWeaponData(resources, "weapon_attributes", itemName, weaponAttributes(weapon));
        }
        byte[] capability = weaponCapability(weapon);
        if (capability != null) {
            putWeaponData(resources, "capabilities/weapons", itemName, capability);
        }
    }

    private static void processMaterialTags(Map<String, Map<String, LinkedHashSet<String>>> customTags,
            TierDefinition def) {
        if (!def.hasMaterials() || def.tags.materials.isEmpty()) {
            return;
        }
        for (String material : List.of(def.heavyMaterialName(), def.doubleMaterialName())) {
            String item = JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + material;
            for (String tag : def.tags.materials) {
                addCustomTag(customTags, tag, item);
            }
        }
    }

    private static void processTierTag(Map<String, Map<String, LinkedHashSet<String>>> customTags,
            TierDefinition def) {
        if (def.standalone || !def.tags.tier_tag.enabled) {
            return;
        }
        String tag = tierTagId(def, def.tags.tier_tag.name);
        for (WeaponSpec weapon : def.weapons()) {
            if (ItemFilters.matchesName(def.tags.tier_tag.exclude_items, weapon.name())) {
                continue;
            }
            addCustomTag(customTags, tag,
                    JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + def.id + "_" + weapon.registryName());
        }
        for (String extra : def.tags.tier_tag.include_items) {
            String value = resolveItemRef(extra);
            if (value != null) {
                addCustomTag(customTags, tag, value);
            }
        }
    }

    private static String tierTagId(TierDefinition def, String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            return JaamsWeaponryExMod.ITEM_NAMESPACE + ":tier/" + def.id;
        }
        return value.indexOf(':') < 0
                ? JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + value
                : value;
    }

    private static String resolveItemRef(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        boolean reference = value.charAt(0) == '#';
        String id = reference ? value.substring(1) : value;
        if (id.indexOf(':') < 0) {
            id = JaamsWeaponryExMod.ITEM_NAMESPACE + ":" + id;
        }
        return reference ? "#" + id : id;
    }

    private static void addCustomTag(Map<String, Map<String, LinkedHashSet<String>>> customTags,
            String rawTag, String item) {
        String tag = rawTag == null ? "" : rawTag.trim();
        if (tag.isEmpty()) {
            return;
        }
        int colon = tag.indexOf(':');
        ResourceLocation id = colon < 0
                ? ResourceLocation.tryParse(BASE_DATA + ":" + tag)
                : ResourceLocation.tryParse(tag);
        if (id == null) {
            return;
        }
        String file = "tags/items/" + id.getPath() + ".json";
        customTags.computeIfAbsent(id.getNamespace(), k -> new LinkedHashMap<>())
                .computeIfAbsent(file, k -> new LinkedHashSet<>())
                .add(item);
    }

    private static byte[] weaponCapability(WeaponSpec weapon) {
        for (String tier : CAPABILITY_TIERS) {
            byte[] capability = classpathBytes(CAPABILITY_FOLDER + tier + "_" + weapon.registryName() + ".json");
            if (capability != null) {
                return capability;
            }
        }
        return null;
    }

    private static String tagPath(String tag) {
        return "tags/items/" + (tag.indexOf('/') >= 0 ? tag : "item/" + tag) + ".json";
    }

    public static boolean hasWeaponPreset(WeaponSpec weapon) {
        return ModelSource.hasClasspath(PRESET_FOLDER + weapon.preset() + ".json");
    }

    public static boolean hasWeaponCapability(WeaponSpec weapon) {
        return weaponCapability(weapon) != null;
    }

    private static JsonObject tagValue(String item) {
        JsonObject value = new JsonObject();
        value.addProperty("id", item);
        value.addProperty("required", false);
        return value;
    }

    private static byte[] tagFile(JsonArray values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        return GSON.toJson(tag).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] weaponAttributes(WeaponSpec weapon) {
        JsonObject parentOnly = new JsonObject();
        parentOnly.addProperty("parent", "jaams_weaponry:jaams_presets/" + weapon.preset());
        return GSON.toJson(parentOnly).getBytes(StandardCharsets.UTF_8);
    }

    static JsonArray buildTabEntries(List<TierEntry> tiers, String namespace) {
        TabChain chain = new TabChain("jaams_weaponry:double_netherite_ingot");
        for (TierEntry tier : tiers) {
            String anchor = tier.definition().tier.tab_insert_after != null
                    && !tier.definition().tier.tab_insert_after.isBlank()
                    ? tier.definition().tier.tab_insert_after.trim()
                    : "jaams_weaponry:double_netherite_ingot";
            chain.setAnchor(anchor);
            Set<String> owned = new LinkedHashSet<>();
            for (WeaponSpec spec : tier.definition().weapons()) {
                owned.add(spec.name());
            }

            for (String weapon : WeaponSpec.TAB_ORDER) {
                WeaponSpec spec = WeaponSpec.byName(weapon);
                if (spec == null || !owned.contains(spec.name())) {
                    continue;
                }
                chain.add(namespace + ":" + tier.definition().id + "_" + spec.registryName());
            }

            if (tier.definition().hasMaterials()) {
                chain.add(namespace + ":" + tier.definition().heavyMaterialName());
                chain.add(namespace + ":" + tier.definition().doubleMaterialName());
            }
        }
        return chain.entries();
    }

    private static final class TabChain {

        private final JsonArray entries = new JsonArray();
        private String anchor;

        TabChain(String anchor) {
            this.anchor = anchor;
        }

        void setAnchor(String anchor) {
            this.anchor = anchor;
        }

        void add(String item) {
            JsonObject entry = new JsonObject();
            entry.addProperty("item", item);
            if (anchor != null && !anchor.isEmpty()) {
                entry.addProperty("after", anchor);
            }
            entries.add(entry);
            anchor = item;
        }

        JsonArray entries() {
            return entries;
        }
    }

    private static byte[] classpathBytes(String path) {
        try (java.io.InputStream in = JaamsWeaponryExMod.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            JaamsWeaponryExMod.LOGGER.warn("Failed to read class path resource {}", path, e);
        }
        return null;
    }

    private static Pack wrap(String id, GeneratedPack resources, String descriptionKey, PackType type) {
        return Pack.readMetaAndCreate(id, Component.translatable(descriptionKey), true,
                name -> resources, type, Pack.Position.TOP, PackSource.BUILT_IN);
    }

    private static String humanize(String name) {
        for (WeaponSpec spec : WeaponSpec.WEAPONS) {
            if (spec.name().equals(name)) {
                return spec.label();
            }
        }
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static Set<String> languagesOf(TierEntry tier) {
        LinkedHashSet<String> codes = new LinkedHashSet<>(BaseLang.shipped());
        codes.addAll(tier.definition().translations.keySet());
        return codes;
    }

    private static String label(TierEntry tier, String code) {
        return label(tier.definition(), code);
    }

    private static String label(TierDefinition def, String code) {
        if ("en_us".equals(code)) {
            return def.display_name;
        }
        String translated = def.translations.get(code);
        return translated == null ? def.display_name : translated;
    }

    private static String weaponLabel(TierDefinition def, String code, WeaponSpec weapon, String tierLabel) {
        String overridden = def == null ? null : def.overriddenName(weapon, code);
        if (overridden != null) {
            return overridden;
        }
        String fromBase = BaseLang.weapon(code, weapon.registryName(), tierLabel);
        return fromBase != null ? fromBase : tierLabel + " " + humanize(weapon.registryName());
    }

    private static String materialLabel(String code, TierDefinition def, MaterialType material,
            Density density) {
        boolean heavy = density == Density.HEAVY;
        String fromBase = switch (material) {
            case INGOT -> heavy ? BaseLang.heavyIngot(code, label(def, code))
                    : BaseLang.doubleIngot(code, label(def, code));
            case COMPRESSED -> heavy ? BaseLang.heavyCompressed(code, def.materialWord())
                    : BaseLang.doubleCompressed(code, def.materialWord());
            case GEM -> heavy ? BaseLang.heavyGem(code, def.materialWord())
                    : BaseLang.doubleGem(code, def.materialWord());
        };
        if (fromBase != null) {
            return fromBase;
        }
        return switch (material) {
            case INGOT -> density.label() + " " + label(def, code) + " Ingot";
            case COMPRESSED -> density.label() + " Compressed " + def.materialWord();
            case GEM -> density.label() + " " + def.materialWord();
        };
    }
}
