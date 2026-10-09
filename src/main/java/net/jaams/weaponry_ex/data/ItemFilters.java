package net.jaams.weaponry_ex.data;

import java.util.List;
import java.util.Locale;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class ItemFilters {

    public static final String EXCLUDE_ALL = "*";

    private ItemFilters() {
    }

    public static boolean included(List<String> only, List<String> exclude, String weapon,
            ItemStack stack) {
        if (only != null && !only.isEmpty() && !matches(only, weapon, stack)) {
            return false;
        }
        return !matches(exclude, weapon, stack);
    }

    public static boolean isWildcarded(List<String> entries) {
        if (entries == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry != null && EXCLUDE_ALL.equals(entry.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesName(List<String> entries, String weapon) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        String held = weapon == null ? null : weapon.trim().toLowerCase(Locale.ROOT);
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty() || value.charAt(0) == '#') {
                continue;
            }
            if (EXCLUDE_ALL.equals(value)) {
                return true;
            }
            if (held != null && value.equals(held)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(List<String> entries, String weapon, ItemStack stack) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        String held = weapon == null ? null : weapon.trim().toLowerCase(Locale.ROOT);
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                continue;
            }
            if (EXCLUDE_ALL.equals(value)) {
                return true;
            }
            if (value.charAt(0) == '#') {
                ResourceLocation tag = ResourceLocation.tryParse(value.substring(1));
                if (tag != null && stack != null && !stack.isEmpty() && stack.is(ItemTags.create(tag))) {
                    return true;
                }
                continue;
            }
            if (held != null && value.equals(held)) {
                return true;
            }
        }
        return false;
    }
}
