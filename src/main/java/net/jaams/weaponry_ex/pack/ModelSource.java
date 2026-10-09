package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierSection.Density;
import net.jaams.weaponry_ex.data.TierSection.MaterialType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ModelSource {

    private static final Logger LOGGER = LogManager.getLogger(JaamsWeaponryExMod.MOD_NAME);
    private static final String BASE_MODEL = "assets/jaams_weaponry_ex/models/item/";
    private static final String BASE_NAMESPACE = JaamsWeaponryExMod.MODID;

    private static final String ITEM_PREFIX = JaamsWeaponryExMod.ITEM_NAMESPACE + ":item/";

    private static final String BASE_PREFIX = ITEM_PREFIX + "base_";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ModelSource() {
    }

    public static boolean has(String baseModelName) {
        return JaamsWeaponryExMod.class.getClassLoader()
                .getResource(BASE_MODEL + baseModelName + ".json") != null;
    }

    public static boolean hasClasspath(String path) {
        return JaamsWeaponryExMod.class.getClassLoader().getResource(path) != null;
    }

    public static Set<String> closure(String weaponName) {
        Set<String> found = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(weaponName);
        while (!pending.isEmpty()) {
            String name = pending.pop();
            if (!found.add(name)) {
                continue;
            }

            pending.addAll(references(readModel(name)));
        }
        return found;
    }

    public static Set<String> sprites(String weaponName) {
        Set<String> found = new LinkedHashSet<>();
        found.add("base_" + weaponName);
        for (String model : closure(weaponName)) {
            collectSprites(readModel(model), found);
        }
        return found;
    }

    private static Set<String> references(JsonObject model) {
        Set<String> found = new LinkedHashSet<>();
        collectReferences(model, found);
        return found;
    }

    private static void collectSprites(JsonElement element, Set<String> out) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if ("textures".equals(entry.getKey()) && value.isJsonObject()) {
                    for (JsonElement sprite : value.getAsJsonObject().asMap().values()) {
                        addSprite(sprite, out);
                    }
                } else {
                    collectSprites(value, out);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectSprites(child, out);
            }
        }
    }

    private static void addSprite(JsonElement value, Set<String> out) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String sprite = value.getAsString();
            if (sprite.startsWith(ITEM_PREFIX)) {
                out.add(sprite.substring(ITEM_PREFIX.length()));
            }
        }
    }

    private static void collectReferences(JsonElement element, Set<String> out) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if ("textures".equals(entry.getKey())) {
                    continue;
                }
                collectReferences(entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectReferences(child, out);
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith(BASE_PREFIX)) {
                out.add(value.substring(BASE_PREFIX.length()));
            }
        }
    }

    private static JsonObject readModel(String baseModelName) {
        try (InputStream in = JaamsWeaponryExMod.class.getClassLoader()
                .getResourceAsStream(BASE_MODEL + "base_" + baseModelName + ".json")) {
            if (in != null) {
                JsonObject model = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8),
                        JsonObject.class);
                if (model != null) {
                    return model;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read base model {}", baseModelName, e);
        }
        return new JsonObject();
    }

    public static byte[] forTier(String weaponName, String baseModelName, TierDefinition tier) {        String tierId = tier.id;
        MaterialType material = tier.getMaterialType();
        String path = BASE_MODEL + baseModelName + ".json";
        try (InputStream in = JaamsWeaponryExMod.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject model = GSON.fromJson(json, JsonObject.class);
                if (model != null) {
                    stripForgeData(model);
                    json = GSON.toJson(model);
                }

                json = json.replace("\"" + BASE_NAMESPACE + ":item/" + material.baseTextureName(Density.HEAVY),
                        "\"" + JaamsWeaponryExMod.ITEM_NAMESPACE + ":item/" + tier.heavyMaterialName());
                json = json.replace("\"" + BASE_NAMESPACE + ":item/" + material.baseTextureName(Density.LIGHT),
                        "\"" + JaamsWeaponryExMod.ITEM_NAMESPACE + ":item/" + tier.doubleMaterialName());
                json = json.replace("\"" + BASE_NAMESPACE + ":item/base_",
                        "\"" + JaamsWeaponryExMod.ITEM_NAMESPACE + ":item/" + tierId + "_");
                return json.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read base model {}", path, e);
        }
        return fallback(weaponName, baseModelName, tierId);
    }

    public static byte[] forTier(String baseModelName, TierDefinition tier) {
        return forTier(baseModelName.substring("base_".length()), baseModelName, tier);
    }

    private static void stripForgeData(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            object.remove("forge_data");
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                stripForgeData(entry.getValue());
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                stripForgeData(child);
            }
        }
    }

    private static byte[] fallback(String weaponName, String baseModelName, String tierId) {
        JsonObject model = new JsonObject();
        if (!baseModelName.equals("base_" + weaponName)) {
            model.addProperty("parent", "item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", JaamsWeaponryExMod.ITEM_NAMESPACE + ":item/"
                    + tierId + "_" + baseModelName.substring("base_".length()));
            model.add("textures", textures);
        } else {
            model.addProperty("parent", JaamsWeaponryExMod.MODID + ":item/" + baseModelName);
        }
        return GSON.toJson(model).getBytes(StandardCharsets.UTF_8);
    }
}
