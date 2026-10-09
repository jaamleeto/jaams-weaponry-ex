package net.jaams.weaponry_ex.tooltip;

import net.jaams.weaponry_ex.data.HitEffect;
import net.jaams.weaponry_ex.data.HitEffectRef;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierHitEffects;
import net.jaams.weaponry_ex.registry.TieredItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TierTooltip {

    private static final char SECTION = '§';

    private TierTooltip() {
    }

    public static void add(ItemStack stack, List<Component> tooltip) {
        TieredItems.ItemOwner owner = TieredItems.owner(stack);
        if (owner == null) {
            return;
        }
        add(owner.tier(), stack, tooltip);
    }

    public static void add(TierDefinition tier, ItemStack stack, List<Component> tooltip) {
        if (tier == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (tier.tooltip != null && tier.tooltip.appliesTo(TierHitEffects.weaponOf(stack), stack)) {
            for (String line : tier.tooltip.lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                lines.add(render(line, tier));
            }
        }
        if (!lines.isEmpty() && hasDescribableEffects(tier, stack)) {
            lines.add(Component.empty());
        }
        for (HitEffectRef ref : TierHitEffects.eligible(tier, stack)) {
            HitEffect effect = ref.effect();
            if (!effect.show_tooltip || !effect.canDescribe()) {
                continue;
            }
            int before = lines.size();
            effect.describe(lines);
            for (int i = before; i < lines.size(); i++) {
                lines.set(i, withChance(lines.get(i), effect));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        tooltip.add(Component.empty());
        tooltip.addAll(lines);
    }

    public static boolean isTranslationKey(String line) {
        if (line == null) {
            return false;
        }
        String value = line.trim();
        if (value.isEmpty() || value.indexOf('&') >= 0 || value.indexOf(SECTION) >= 0) {
            return false;
        }
        int colon = value.indexOf(':');
        if (colon < 1) {
            return false;
        }
        if (!value.substring(0, colon).matches("[a-z0-9_.-]+")) {
            return false;
        }
        String path = value.substring(colon + 1);
        return !path.isEmpty() && path.matches("[a-z0-9_./-]+");
    }

    private static Component withChance(Component line, HitEffect effect) {
        if (effect.clampChance() >= 1.0F) {
            return line;
        }
        String key;
        if (effect.at_max_strength && !effect.below_max_strength) {
            key = "jaams_weaponry_ex.on_hit.chance_max_strength";
        } else if (!effect.at_max_strength && effect.below_max_strength) {
            key = "jaams_weaponry_ex.on_hit.chance_below_max_strength";
        } else {
            key = "jaams_weaponry_ex.on_hit.chance";
        }
        return line.copy().append(Component.translatable(key, effect.percentText()));
    }

    private static boolean hasDescribableEffects(TierDefinition tier, ItemStack stack) {
        for (HitEffectRef ref : TierHitEffects.eligible(tier, stack)) {
            HitEffect effect = ref.effect();
            if (effect.show_tooltip && effect.canDescribe()) {
                return true;
            }
        }
        return false;
    }

    public static List<Component> arguments(TierDefinition tier) {
        List<Component> arguments = new ArrayList<>();
        arguments.add(Component.literal(tier.display_name == null ? tier.id : tier.display_name));
        arguments.add(Component.literal(tier.id));
        arguments.add(Component.literal(tier.materialWord()));
        return arguments;
    }

    public static String fill(String line, TierDefinition tier) {
        return line.replace("%tier%", tier.display_name == null ? tier.id : tier.display_name)
                .replace("%id%", tier.id)
                .replace("%material%", tier.materialWord());
    }

    public static MutableComponent render(String line, TierDefinition tier) {
        if (isTranslationKey(line)) {
            String key = line.trim();
            String fromTier = translated(tier, key);
            if (fromTier != null) {
                return styled(fill(fromTier, tier), tier.getColor());
            }
            List<Component> arguments = arguments(tier);
            return Component.translatable(key, arguments.get(0), arguments.get(1), arguments.get(2))
                    .setStyle(Style.EMPTY.withColor(tier.getColor() & 0xFFFFFF));
        }
        return styled(fill(line, tier), tier.getColor());
    }

    public static String translated(TierDefinition tier, String key) {
        if (tier.tooltip_translations == null) {
            return null;
        }
        Map<String, String> byLang = tier.tooltip_translations.get(key);
        if (byLang == null || byLang.isEmpty()) {
            return null;
        }
        String active = activeLanguage();
        String value = active == null ? null : byLang.get(active);
        if (value == null) {
            value = byLang.get("en_us");
        }
        return value;
    }

    private static String activeLanguage() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }
        return Minecraft.getInstance().options.languageCode;
    }

    public static MutableComponent styled(String line, int argb) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY.withColor(argb & 0xFFFFFF);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (i + 1 < line.length() && (c == '&' || c == SECTION)) {
                ChatFormatting formatting = ChatFormatting.getByCode(line.charAt(i + 1));
                if (formatting != null) {
                    flush(result, text, style);
                    if (formatting == ChatFormatting.RESET) {
                        style = Style.EMPTY.withColor(argb & 0xFFFFFF);
                    } else if (formatting.isColor()) {
                        style = style.withColor(formatting);
                    } else if (formatting.isFormat()) {
                        style = style.applyLegacyFormat(formatting);
                    }
                    i++;
                    continue;
                }
            }
            text.append(c);
        }
        flush(result, text, style);
        return result;
    }

    private static void flush(MutableComponent result, StringBuilder text, Style style) {
        if (text.isEmpty()) {
            return;
        }
        result.append(Component.literal(text.toString()).setStyle(style));
        text.setLength(0);
    }
}
