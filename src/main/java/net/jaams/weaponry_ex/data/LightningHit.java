package net.jaams.weaponry_ex.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class LightningHit extends HitEffect {

    @Override
    public boolean canDescribe() {
        return true;
    }

    @Override
    public void describe(List<Component> lines) {
        lines.add(Component.translatable("jaams_weaponry_ex.on_hit.summons",
                Component.translatable("jaams_weaponry_ex.on_hit.lightning"))
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void apply(LivingEntity target, LivingEntity attacker, RandomSource random) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.setCause(attacker instanceof ServerPlayer player ? player : null);
        bolt.setPos(target.getX(), target.getY(), target.getZ());
        level.addFreshEntity(bolt);
    }
}
