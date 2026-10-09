package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

import net.jaams.weaponry_ex.registry.TieredItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class TierHitEffects {

    private TierHitEffects() {
    }

    public static List<HitEffectRef> eligible(TierDefinition tier, ItemStack stack) {
        List<HitEffectRef> refs = new ArrayList<>();
        if (tier == null || tier.on_hit == null) {
            return refs;
        }
        String weapon = weaponOf(stack);
        for (HitEffectRef ref : tier.on_hit.entries()) {
            HitEffect effect = ref.effect();
            if (effect == null || !effect.enabled) {
                continue;
            }
            if (!effect.applies(weapon, stack)) {
                continue;
            }
            refs.add(ref);
        }
        return refs;
    }

    public static void apply(TierDefinition tier, LivingEntity target, LivingEntity attacker, ItemStack stack) {
        if (tier == null || target == null || target.isRemoved()) {
            return;
        }
        RandomSource random = attacker != null ? attacker.getRandom() : target.getRandom();
        float strength = strengthOf(attacker);
        for (HitEffectRef ref : eligible(tier, stack)) {
            HitEffect effect = ref.effect();
            if (!effect.allowsStrength(strength)) {
                continue;
            }
            if (effect.rolls(random)) {
                effect.apply(target, attacker, random);
            }
        }
    }

    public static float strengthOf(@Nullable LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return player.getAttackStrengthScale(0.5F);
        }
        return HitEffect.MAX_STRENGTH;
    }

    public static String weaponOf(ItemStack stack) {
        TieredItems.ItemOwner owner = TieredItems.owner(stack);
        WeaponSpec spec = owner == null ? null : owner.spec();
        return spec == null ? null : spec.name();
    }
}
