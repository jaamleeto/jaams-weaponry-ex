package net.jaams.weaponry_ex.pack;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierSection.Density;
import net.jaams.weaponry_ex.data.TierSection.MaterialType;
import net.jaams.weaponry_ex.data.WeaponSpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TextureSource {

    private static final Logger LOGGER = LogManager.getLogger(JaamsWeaponryExMod.MOD_NAME);
    private static final String ITEM_FOLDER = "assets/jaams_weaponry_ex/textures/item/";
    private static final String BASE_ITEM = ITEM_FOLDER + "base_";

    private TextureSource() {
    }

    public static Map<String, byte[]> perTier(TierDefinition tier) {
        String tierId = tier.id;
        MaterialType material = tier.getMaterialType();
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (WeaponSpec weapon : tier.weapons()) {
            for (String sprite : ModelSource.sprites(weapon.name())) {
                copy(ITEM_FOLDER + sprite + ".png",
                        tierId + "_" + sprite.substring("base_".length()) + ".png", out);
            }
        }
        copy(ITEM_FOLDER + material.baseTextureName(Density.HEAVY) + ".png",
                tier.heavyMaterialName() + ".png", out);
        copy(ITEM_FOLDER + material.baseTextureName(Density.LIGHT) + ".png",
                tier.doubleMaterialName() + ".png", out);
        return out;
    }

    private static void copy(String basePath, String tierFileName, Map<String, byte[]> out) {
        try (InputStream in = JaamsWeaponryExMod.class.getClassLoader().getResourceAsStream(basePath)) {
            if (in != null) {
                out.put("textures/item/" + tierFileName, in.readAllBytes());
            } else {
                LOGGER.debug("Base texture {} not found, tier file {} skipped", basePath, tierFileName);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to copy base texture {} to {}", basePath, tierFileName, e);
        }
    }
}
