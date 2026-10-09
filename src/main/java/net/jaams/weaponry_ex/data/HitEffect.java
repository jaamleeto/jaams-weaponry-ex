package net.jaams.weaponry_ex.data;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class HitEffect {

    public static final float MAX_STRENGTH = 1.0F;

    private static final float MAX_STRENGTH_EPSILON = 1.0E-4F;

    public boolean enabled = false;

    public float chance = 1.0F;

    public boolean show_tooltip = false;

    public List<String> exclude_items = new ArrayList<>();

    public List<String> only_items = new ArrayList<>();

    public boolean at_max_strength = true;

    public boolean below_max_strength = true;

    public boolean hasDuration() {
        return false;
    }

    public int duration() {
        return 0;
    }

    public boolean canDescribe() {
        return false;
    }

    public void describe(List<Component> lines) {
    }

    public boolean applies(String weapon, ItemStack stack) {
        return ItemFilters.included(only_items, exclude_items, weapon, stack);
    }

    public boolean allowsStrength(float strength) {
        boolean maxed = !Float.isNaN(strength) && strength >= MAX_STRENGTH - MAX_STRENGTH_EPSILON;
        return maxed ? at_max_strength : below_max_strength;
    }

    public float clampChance() {
        if (Float.isNaN(chance)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, chance));
    }

    public String percentText() {
        return amount(clampChance() * 100.0F) + "%";
    }

    protected static String amount(float value) {
        if (Float.isNaN(value)) {
            return "0";
        }
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    public boolean rolls(RandomSource random) {
        float value = clampChance();
        if (value <= 0.0F) {
            return false;
        }
        return value >= 1.0F || random.nextFloat() < value;
    }

    public abstract void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random);
}
