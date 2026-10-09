package net.jaams.weaponry_ex.data;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;

public class TierRecipe {

    public static final String VANILLA_SMITHING_TABLE = "minecraft:smithing_table";

    public String recipe_type = "none";

    public String base_material;

    public String smithing_template = "minecraft:netherite_upgrade_smithing_template";

    public String smithing_table = VANILLA_SMITHING_TABLE;

    public String ingot_material;

    public boolean hasBaseMaterial() {
        return base_material != null && !base_material.trim().isEmpty();
    }

    public boolean hasIngotMaterial() {
        return ingot_material != null && !ingot_material.trim().isEmpty();
    }

    public boolean hasSmithingTemplate() {
        return smithing_template != null && !smithing_template.trim().isEmpty();
    }

    public String smithingTable() {
        if (smithing_table == null || smithing_table.trim().isEmpty()) {
            return VANILLA_SMITHING_TABLE;
        }
        return smithing_table.trim();
    }

    public boolean hasCustomSmithingTable() {
        return !VANILLA_SMITHING_TABLE.equals(smithingTable());
    }

    public RecipeType getRecipeType() {
        if (recipe_type == null || recipe_type.trim().isEmpty()) {
            return RecipeType.NONE;
        }
        try {
            return RecipeType.valueOf(recipe_type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            JaamsWeaponryExMod.LOGGER.warn("Unknown recipe_type '{}', expected one of "
                    + "{}. Falling back to 'none'.", recipe_type,
                    java.util.Arrays.toString(RecipeType.values()));
            return RecipeType.NONE;
        }
    }

    public enum RecipeType {

        NORMAL,

        SMITHING,

        NONE
    }
}
