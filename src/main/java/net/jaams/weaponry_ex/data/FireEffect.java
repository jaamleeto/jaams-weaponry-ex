package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class FireEffect extends HitEffect {

    public int ticks = 100;

    @Override
    public boolean hasDuration() {
        return true;
    }

    @Override
    public int duration() {
        return ticks;
    }

    public int seconds() {
        return Math.max(1, Math.round(ticks / 20.0F));
    }

    @Override
    public boolean canDescribe() {
        return ticks > 0;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.inflicts_for",
                Component.translatable("jaams_weaponry_ex.on_hit.fire"),
                Component.translatable("jaams_weaponry_ex.on_hit.seconds",
                        Component.literal(String.valueOf(seconds()))))
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public void apply(LivingEntity target, LivingEntity attacker, RandomSource random) {
        if (ticks <= 0) {
            return;
        }
        target.setSecondsOnFire(seconds());
    }
}
