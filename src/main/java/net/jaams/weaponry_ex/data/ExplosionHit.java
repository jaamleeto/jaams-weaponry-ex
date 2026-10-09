package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ExplosionHit extends HitEffect {

    public float power = 3.0F;

    public boolean breaks_blocks = false;

    @Override
    public boolean canDescribe() {
        return power > 0.0F;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.summons",
                Component.translatable("jaams_weaponry_ex.on_hit.explosion"))
                .withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public void apply(LivingEntity target, LivingEntity attacker, RandomSource random) {
        if (power <= 0.0F) {
            return;
        }
        Level.ExplosionInteraction interaction = breaks_blocks
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.MOB;
        target.level().explode(attacker, target.getX(), target.getY(), target.getZ(), power,
                breaks_blocks, interaction);
    }
}
