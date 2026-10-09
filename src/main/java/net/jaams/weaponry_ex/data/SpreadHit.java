package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpreadHit extends HitEffect {

    public String effect = "minecraft:poison";

    public int ticks = 100;

    public int amplifier = 0;

    public boolean particles = true;

    public boolean icon = true;

    public float radius = 4.0F;

    public int max_targets = 8;

    public List<String> targets = new ArrayList<>();

    @Override
    public boolean hasDuration() {
        return true;
    }

    @Override
    public int duration() {
        return ticks;
    }

    public MobEffect resolve() {
        ResourceLocation id = effect == null ? null : ResourceLocation.tryParse(effect.trim());
        return id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
    }

    public MobEffectInstance instance() {
        MobEffect mob = resolve();
        if (mob == null || ticks <= 0) {
            return null;
        }
        int level = Math.max(0, Math.min(255, amplifier));
        return new MobEffectInstance(mob, ticks, level, false, particles, icon);
    }

    @Override
    public boolean canDescribe() {
        return ticks > 0 && radius > 0.0F && max_targets > 0 && !targets.isEmpty()
                && instance() != null;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.spreads",
                Component.translatable(instance().getEffect().getDescriptionId()),
                EntityFilters.describe(targets))
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        MobEffectInstance instance = instance();
        if (instance == null) {
            return;
        }
        for (LivingEntity nearby : NearbyTargets.around(target, attacker, radius, targets, max_targets)) {
            nearby.addEffect(instance);
        }
    }
}
