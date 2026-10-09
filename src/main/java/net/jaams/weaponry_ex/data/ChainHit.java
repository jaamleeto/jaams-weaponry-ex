package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChainHit extends HitEffect {

    public float amount = 2.0F;

    public float radius = 4.0F;

    public int max_targets = 3;

    public List<String> targets = new ArrayList<>();

    @Override
    public boolean canDescribe() {
        return amount > 0.0F && radius > 0.0F && max_targets > 0 && !targets.isEmpty();
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.chains",
                Component.literal(amount(amount)), EntityFilters.describe(targets))
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (amount <= 0.0F) {
            return;
        }
        for (LivingEntity nearby : NearbyTargets.around(target, attacker, radius, targets, max_targets)) {
            nearby.hurt(HitDamage.sourceFor(nearby, attacker), amount);
        }
    }
}
