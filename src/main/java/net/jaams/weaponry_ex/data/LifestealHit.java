package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class LifestealHit extends HitEffect {

    public float amount = 2.0F;

    public float hearts() {
        return Math.max(0.0F, amount) / 2.0F;
    }

    @Override
    public boolean canDescribe() {
        return amount > 0.0F;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.heals_for",
                Component.translatable("jaams_weaponry_ex.on_hit.hearts",
                        Component.literal(amount(hearts()))))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void apply(LivingEntity target, LivingEntity attacker, RandomSource random) {
        if (amount <= 0.0F || attacker == null || !attacker.isAlive()) {
            return;
        }
        attacker.heal(amount);
    }
}
