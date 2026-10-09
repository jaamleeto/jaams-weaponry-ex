package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class StatusEffectHit extends HitEffect {

    private static final int VANILLA_POTION_MIN_TICKS = 20;

    public String effect = "minecraft:poison";

    public int ticks = 100;

    public int amplifier = 0;

    public boolean particles = true;

    public boolean icon = true;

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
        return instance() != null;
    }

    @Override
    public void describe(List<Component> lines) {
        MobEffectInstance instance = instance();
        if (instance == null) {
            return;
        }
        MutableComponent name = Component.translatable(instance.getEffect().getDescriptionId());
        if (instance.getAmplifier() > 0) {
            name = Component.translatable("potion.withAmplifier", name,
                    Component.translatable("potion.potency." + instance.getAmplifier()));
        }
        if (!instance.endsWithin(VANILLA_POTION_MIN_TICKS)) {
            name = Component.translatable("potion.withDuration", name,
                    MobEffectUtil.formatDuration(instance, 1.0F));
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.inflicts", name)
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void apply(LivingEntity target, LivingEntity attacker, RandomSource random) {
        MobEffectInstance instance = instance();
        if (instance == null) {
            return;
        }
        target.addEffect(instance);
    }
}
