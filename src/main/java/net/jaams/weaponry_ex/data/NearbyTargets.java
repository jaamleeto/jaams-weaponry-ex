package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

public final class NearbyTargets {

    private NearbyTargets() {
    }

    public static List<LivingEntity> around(LivingEntity origin, @Nullable LivingEntity attacker,
            float radius, List<String> filter, int max) {
        List<LivingEntity> found = new ArrayList<>();
        if (origin == null || radius <= 0.0F || max <= 0) {
            return found;
        }
        AABB box = origin.getBoundingBox().inflate(radius);
        List<Entity> near = origin.level().getEntities(origin, box,
                e -> e.isAlive() && e != attacker && e instanceof LivingEntity living
                        && EntityFilters.matches(living, filter));
        for (Entity e : near) {
            if (found.size() >= max) {
                break;
            }
            found.add((LivingEntity) e);
        }
        return found;
    }
}
