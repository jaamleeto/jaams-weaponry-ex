package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BonusDamageHit extends HitEffect {

    public static final String FLAT = "flat";

    public static final String PERCENT_TARGET_MISSING = "percent_target_missing";

    public static final String PERCENT_ATTACKER_MISSING = "percent_attacker_missing";

    public static final String PERCENT_MAX = "percent_max";

    public static final List<String> MODES =
            List.of(FLAT, PERCENT_TARGET_MISSING, PERCENT_ATTACKER_MISSING, PERCENT_MAX);

    public float amount = 2.0F;

    public String mode = FLAT;

    public List<String> targets = new ArrayList<>();

    private String mode() {
        String value = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return MODES.contains(value) ? value : FLAT;
    }

    private float damageFor(LivingEntity target, @Nullable LivingEntity attacker) {
        String value = mode();
        if (FLAT.equals(value)) {
            return amount;
        }
        if (PERCENT_MAX.equals(value)) {
            return target.getMaxHealth() * amount / 100.0F;
        }
        if (attacker == null) {
            return 0.0F;
        }
        LivingEntity basis = PERCENT_ATTACKER_MISSING.equals(value) ? attacker : target;
        float missing = Math.max(0.0F, basis.getMaxHealth() - basis.getHealth());
        return missing * amount / 100.0F;
    }

    @Override
    public boolean canDescribe() {
        return amount > 0.0F && !targets.isEmpty();
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        String value = mode();
        String key = switch (value) {
            case PERCENT_MAX -> "jaams_weaponry_ex.on_hit.deals_damage_percent_max";
            case PERCENT_TARGET_MISSING -> "jaams_weaponry_ex.on_hit.deals_damage_percent_target_missing";
            case PERCENT_ATTACKER_MISSING -> "jaams_weaponry_ex.on_hit.deals_damage_percent_attacker_missing";
            default -> "jaams_weaponry_ex.on_hit.deals_damage";
        };
        lines.add(Component.translatable(key, Component.literal(amount(amount)),
                EntityFilters.describe(targets))
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (amount <= 0.0F || !EntityFilters.matches(target, targets)) {
            return;
        }
        float damage = damageFor(target, attacker);
        if (damage <= 0.0F) {
            return;
        }
        target.hurt(HitDamage.sourceFor(target, attacker), damage);
    }
}
