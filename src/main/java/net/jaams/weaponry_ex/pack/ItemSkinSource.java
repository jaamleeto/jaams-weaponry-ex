package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ItemSkinSource {

    private static final String REFERENCE_TIER = "shinerite";

    private static final String BASE_SKIN = "assets/jaams_weaponry/item_skins/";
    private static final String EPIC_FIGHT = "epicfight";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ItemSkinSource() {
    }

    public static byte[] forTier(String weaponName, int argb) {
        return forTier(weaponName, argb, argb);
    }

    public static byte[] forTier(String weaponName, int argb, int trailArgb) {
        JsonObject skin = read(REFERENCE_TIER + "_" + weaponName);
        if (skin == null) {
            skin = new JsonObject();
        }
        JsonObject trail = skin.getAsJsonObject("trail");
        if (trail == null) {
            trail = new JsonObject();
            skin.add("trail", trail);
        }
        trail.add("color", color(trailArgb));
        if (!trail.has("begin_pos")) {
            trail.add("begin_pos", floats(0.0, 0.0, -0.2));
        }
        if (!trail.has("end_pos")) {
            trail.add("end_pos", floats(0.0, 0.1, -0.7));
        }
        if (!trail.has("lifetime")) {
            trail.addProperty("lifetime", 4);
        }
        if (!trail.has("interpolations")) {
            trail.addProperty("interpolations", 4);
        }
        if (!trail.has("texture_path")) {
            trail.addProperty("texture_path", EPIC_FIGHT + ":textures/particle/swing_trail.png");
        }
        if (!trail.has("particle_type")) {
            trail.addProperty("particle_type", EPIC_FIGHT + ":swing_trail");
        }
        return GSON.toJson(skin).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject read(String skinName) {
        String path = BASE_SKIN + skinName + ".json";
        try (InputStream in = JaamsWeaponryExMod.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                return GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            }
        } catch (Exception e) {
            JaamsWeaponryExMod.LOGGER.warn("Failed to read base item skin {}", path, e);
        }
        return null;
    }

    private static JsonArray color(int argb) {
        JsonArray color = new JsonArray();
        color.add((argb >> 16) & 0xFF);
        color.add((argb >> 8) & 0xFF);
        color.add(argb & 0xFF);
        return color;
    }

    private static JsonArray floats(double x, double y, double z) {
        JsonArray pos = new JsonArray();
        pos.add(x);
        pos.add(y);
        pos.add(z);
        return pos;
    }
}
