package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ParticleHit extends HitEffect {

    private static final int MAX_COUNT = 512;

    public String particle = "minecraft:crit";

    public int count = 12;

    public float radius = 0.5F;

    public double speed = 0.05D;

    public ParticleType<?> resolve() {
        ResourceLocation id = particle == null ? null : ResourceLocation.tryParse(particle.trim());
        return id == null ? null : ForgeRegistries.PARTICLE_TYPES.getValue(id);
    }

    @Override
    public boolean canDescribe() {
        return count > 0 && resolve() != null;
    }

    @Override
    public void describe(List<Component> lines) {
        if (!canDescribe()) {
            return;
        }
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.spawns_particles",
                Component.literal(String.valueOf(count)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (count <= 0 || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        ParticleType<?> type = resolve();
        if (type == null) {
            return;
        }
        ParticleOptions options = (ParticleOptions) type;
        double spread = Math.max(0.0F, radius);
        level.sendParticles(options, target.getX(), target.getEyeY(), target.getZ(),
                Math.min(count, MAX_COUNT), spread, spread * 0.6D, spread, speed);
    }
}
