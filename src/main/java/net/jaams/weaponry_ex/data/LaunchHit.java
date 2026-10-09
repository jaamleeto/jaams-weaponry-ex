package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LaunchHit extends HitEffect {

    public float power = 1.0F;

    @Override
    public boolean canDescribe() {
        return power > 0.0F;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.launches",
                Component.literal(amount(power)))
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (power <= 0.0F) {
            return;
        }
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, motion.y + power, motion.z);
        target.hurtMarked = true;
    }
}
