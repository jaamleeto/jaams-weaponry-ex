package net.jaams.weaponry_ex.data;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.registry.TieredItems;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class TierHitEvents {

    private static final Set<String> ALLOWED_NAMESPACES = Set.of(
            JaamsWeaponryExMod.BASE_NAMESPACE, JaamsWeaponryExMod.ITEM_NAMESPACE);

    private static boolean applying;

    private TierHitEvents() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(TierHitEvents.class);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (applying || event.getEntity().level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Mob mob) || source.getDirectEntity() != attacker) {
            return;
        }
        ItemStack hurtItem = mob.getMainHandItem();
        if (!isOwnItem(hurtItem) || TieredItems.owner(hurtItem) == null) {
            return;
        }
        LivingEntity hitLiving = event.getEntity();
        applying = true;
        try {
            hurtItem.getItem().hurtEnemy(hurtItem, hitLiving, mob);
        } finally {
            applying = false;
        }
    }

    private static boolean isOwnItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && ALLOWED_NAMESPACES.contains(key.getNamespace());
    }
}