package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SummonHit extends HitEffect {

    private static final int MAX_SPAWNED = 16;

    public String entity = "minecraft:zombie";

    public int count = 1;

    public float spread = 1.5F;

    public EntityType<?> resolve() {
        ResourceLocation id = entity == null ? null : ResourceLocation.tryParse(entity.trim());
        return id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
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
        EntityType<?> type = resolve();
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.summons_entity",
                Component.translatable(type.getDescriptionId()),
                Component.literal(String.valueOf(count)))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public void apply(LivingEntity target, @Nullable LivingEntity attacker, RandomSource random) {
        if (count <= 0 || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<?> type = resolve();
        if (type == null) {
            return;
        }
        int total = Math.min(count, MAX_SPAWNED);
        double range = Math.max(0.0F, spread);
        for (int i = 0; i < total; i++) {
            Entity spawned = type.create(level);
            if (spawned == null) {
                continue;
            }
            double x = target.getX() + (random.nextDouble() * 2.0D - 1.0D) * range;
            double z = target.getZ() + (random.nextDouble() * 2.0D - 1.0D) * range;
            spawned.moveTo(x, target.getY(), z, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(spawned);
        }
    }
}
