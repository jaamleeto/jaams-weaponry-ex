package net.jaams.weaponry_ex.data;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;

import java.util.ArrayList;
import java.util.List;

public class TierSection {

    public String base_tier = "diamond";

    public String material_type = "ingot";

    public String tab_insert_after = "jaams_weaponry:double_netherite_ingot";

    public String ranged_tiered = "pistol";

    public boolean base_tags = true;

    public List<String> only_items = new ArrayList<>();

    public List<String> exclude_items = new ArrayList<>();

    private String heavyName;

    private String doubleName;

    public MaterialType getMaterialType() {
        if (material_type == null || material_type.trim().isEmpty()) {
            return MaterialType.INGOT;
        }
        try {
            return MaterialType.valueOf(material_type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            JaamsWeaponryExMod.LOGGER.warn("Unknown material_type '{}', expected one of {}. "
                    + "Falling back to 'ingot'.", material_type,
                    java.util.Arrays.toString(MaterialType.values()));
            return MaterialType.INGOT;
        }
    }

    public boolean hasBaseTier() {
        return base_tier != null && !base_tier.trim().isEmpty();
    }

    public String getRangedTiered() {
        if (ranged_tiered == null || ranged_tiered.trim().isEmpty()) {
            return "pistol";
        }
        String value = ranged_tiered.trim().toLowerCase(java.util.Locale.ROOT);
        if (!WeaponSpec.RANGED_WEAPONS.contains(value)) {
            JaamsWeaponryExMod.LOGGER.warn("Unknown ranged_tiered '{}', expected one of "
                    + "{}. Falling back to 'pistol'.", ranged_tiered, WeaponSpec.RANGED_WEAPONS);
            return "pistol";
        }
        return value;
    }

    public String pinnedHeavyName() {
        return heavyName;
    }

    public String pinnedDoubleName() {
        return doubleName;
    }

    public void pinMaterialNames(String heavy, String doubleItem) {
        this.heavyName = heavy;
        this.doubleName = doubleItem;
    }

    public boolean collisionFallback() {
        return this.heavyName != null;
    }

    public enum MaterialType {

        INGOT,

        COMPRESSED,

        GEM;

        public String baseModelName(Density density) {
            String name = density.prefix();
            return switch (this) {
                case INGOT -> name + "base_ingot";
                case COMPRESSED -> name + "compressed_base_ingot";
                case GEM -> name + "base";
            };
        }

        public String baseTextureName(Density density) {
            return baseModelName(density);
        }

        public String heavyItemName(String word) {
            return itemName(Density.HEAVY, word);
        }

        public String doubleItemName(String word) {
            return itemName(Density.LIGHT, word);
        }

        private String itemName(Density density, String word) {
            return switch (this) {
                case INGOT -> density.prefix() + word + "_ingot";
                case COMPRESSED -> density.prefix() + "compressed_" + word;
                case GEM -> density.prefix() + word;
            };
        }
    }

    public enum Density {

        HEAVY("heavy_"),
        LIGHT("double_");

        private final String prefix;

        Density(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        public String label() {
            return this == HEAVY ? "Heavy" : "Double";
        }
    }
}
