package net.jaams.weaponry_ex.data;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

public final class HitDamage {

    private HitDamage() {
    }

    public static DamageSource sourceFor(LivingEntity target, @Nullable LivingEntity attacker) {
        DamageSources sources = target.level().damageSources();
        if (attacker instanceof Player player) {
            return sources.playerAttack(player);
        }
        if (attacker instanceof Mob mob) {
            return sources.mobAttack(mob);
        }
        return attacker == null ? sources.generic() : sources.mobAttack(attacker);
    }
}
