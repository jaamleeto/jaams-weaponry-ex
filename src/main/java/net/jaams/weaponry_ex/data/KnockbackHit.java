package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KnockbackHit extends HitEffect {

    public float power = 0.6F;

    @Override
    public boolean canDescribe() {
        return power > 0.0F;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.knocks_back",
                Component.literal(amount(power)))
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (power <= 0.0F) {
            return;
        }
        double dx;
        double dz;
        if (attacker == null) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            Vec3 away = target.position().subtract(attacker.position());
            double length = Math.sqrt(away.x * away.x + away.z * away.z);
            if (length < 1.0E-4D) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                dx = Math.cos(angle);
                dz = Math.sin(angle);
            } else {
                dx = away.x / length;
                dz = away.z / length;
            }
        }
        target.knockback(power, dx, dz);
    }
}
