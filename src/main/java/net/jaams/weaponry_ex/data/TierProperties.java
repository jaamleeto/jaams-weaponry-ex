package net.jaams.weaponry_ex.data;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;

import net.minecraft.world.item.Rarity;

public class TierProperties {

    public String color = "#FFFFFF";

    public int durability = 1461;

    public float speed = 8.0F;

    public float attack_damage_bonus = 0.0F;

    public int harvest_level = 3;

    public int enchantability = 14;

    public boolean fire_resistant = false;

    public String repair_item;

    public String rarity = "common";

    private int parsedColor = Integer.MIN_VALUE;

    public int getColor() {
        if (parsedColor == Integer.MIN_VALUE) {
            parsedColor = parseColor();
        }
        return parsedColor;
    }

    private int parseColor() {
        return parseColor(color);
    }

    public static int parseColor(String value) {
        String c = value == null ? "#FFFFFF" : value.trim();
        if (c.isEmpty()) {
            c = "#FFFFFF";
        }
        c = c.startsWith("0x") || c.startsWith("0X") ? c.substring(2) : c;
        c = c.startsWith("#") ? c.substring(1) : c;
        if (c.length() == 6 || c.length() == 8) {
            try {
                int rgb = (int) Long.parseLong(c, 16);
                return 0xFF000000 | (rgb & 0xFFFFFF);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0xFFFFFFFF;
    }

    public boolean hasRepairItem() {
        return repair_item != null && !repair_item.trim().isEmpty();
    }

    public Rarity getRarity() {
        if (rarity == null || rarity.trim().isEmpty()) {
            return Rarity.COMMON;
        }
        String value = rarity.trim().toLowerCase(java.util.Locale.ROOT);
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1).trim();
        }
        try {
            return Rarity.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            JaamsWeaponryExMod.LOGGER.warn("Unknown rarity '{}', expected one of {}. "
                    + "Falling back to 'common'.", rarity,
                    java.util.Arrays.toString(Rarity.values()));
            return Rarity.COMMON;
        }
    }
}
