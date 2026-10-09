package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

public final class EntityFilters {

    public static final String ANY = "*";

    private EntityFilters() {
    }

    public static boolean matches(LivingEntity target, List<String> entries) {
        if (entries == null || entries.isEmpty() || target == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (ANY.equals(value)) {
                return true;
            }
            if (value.charAt(0) == '#') {
                ResourceLocation tag = ResourceLocation.tryParse(value.substring(1));
                if (tag != null && target.getType().is(TagKey.create(Registries.ENTITY_TYPE, tag))) {
                    return true;
                }
                continue;
            }
            if (matchesId(target, value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesAll(List<String> entries) {
        if (entries == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry != null && ANY.equals(entry.trim())) {
                return true;
            }
        }
        return false;
    }

    public static ResourceLocation idOf(String entry) {        if (entry == null) {
            return null;
        }
        String value = entry.trim();
        if (value.isEmpty() || ANY.equals(value) || value.charAt(0) == '#') {
            return null;
        }
        return ResourceLocation.tryParse(value);
    }

    public static List<String> ids(List<String> entries) {
        List<String> found = new ArrayList<>();
        if (entries == null) {
            return found;
        }
        for (String entry : entries) {
            ResourceLocation id = idOf(entry);
            if (id != null && !found.contains(id.toString())) {
                found.add(id.toString());
            }
        }
        return found;
    }

    public static Component describe(List<String> entries) {
        if (matchesAll(entries)) {
            return Component.translatable("jaams_weaponry_ex.on_hit.every_enemy");
        }
        List<String> ids = ids(entries);
        if (ids.size() == 1) {
            ResourceLocation id = ResourceLocation.tryParse(ids.get(0));
            EntityType<?> type = id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) {
                return Component.translatable(type.getDescriptionId());
            }
        }
        return Component.translatable("jaams_weaponry_ex.on_hit.kinds_of_enemy",
                Component.literal(String.valueOf(ids.size())));
    }

    private static boolean matchesId(LivingEntity target, String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return false;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        return type != null && type == target.getType();
    }
}
