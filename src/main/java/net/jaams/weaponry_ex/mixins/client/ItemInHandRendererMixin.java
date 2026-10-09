package net.jaams.weaponry_ex.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.jaams.weaponry.compat.EpicFightCompat;
import net.jaams.weaponry.util.ModUtils;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.item.TieredAxeItem;
import net.jaams.weaponry_ex.item.TieredPickaxeItem;
import net.jaams.weaponry_ex.item.TieredSimpleItem;
import net.jaams.weaponry_ex.item.TieredSwordItem;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    private static final String DEBUG_PROPERTY = "jaams_weaponry_ex.epicfight_debug";

    private static final Set<String> BASE_HANDLED = Set.of(
            "katar", "hook_sword", "scythe", "greatsword",
            "longsword", "zweihander", "buster_sword", "greathammer");

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private static final Map<String, TagKey<Item>> WEAPON_TAGS = WeaponSpec.WEAPONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    WeaponSpec::name,
                    spec -> TagKey.create(Registries.ITEM,
                            ResourceLocation.tryBuild(JaamsWeaponryExMod.ITEM_NAMESPACE, "item/" + spec.tag()))));

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void jaamsEx$onRenderItem(LivingEntity entity, ItemStack itemStack,
            ItemDisplayContext displayContext, boolean isLeft, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (entity == null || itemStack == null || itemStack.isEmpty() || poseStack == null) {
            return;
        }
        if (displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        Item item = itemStack.getItem();
        if (!isGenerated(item)) {
            return;
        }
        String weapon = weaponOf(itemStack);
        if (weapon == null || BASE_HANDLED.contains(weapon)) {
            return;
        }
        if (!inEpicFightBattle(entity)) {
            return;
        }

        applyPose(weapon, poseStack);
        if (LOGGED.add(weapon + '/' + ForgeRegistries.ITEMS.getKey(item))) {
            JaamsWeaponryExMod.LOGGER.debug("Applied addon Epic Fight pose '{}' to {}",
                    weapon, itemStack);
        }
    }

    private static boolean isGenerated(Item item) {
        return item instanceof TieredSwordItem || item instanceof TieredAxeItem
                || item instanceof TieredPickaxeItem || item instanceof TieredSimpleItem;
    }

    private static String weaponOf(ItemStack stack) {
        for (Map.Entry<String, TagKey<Item>> entry : WEAPON_TAGS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean inEpicFightBattle(LivingEntity entity) {
        if (Boolean.getBoolean(DEBUG_PROPERTY)) {
            return true;
        }
        if (ModList.get() == null || !ModList.get().isLoaded("epicfight")) {
            return false;
        }
        return ModUtils.isEntityInBattleMode(entity)
                || ModUtils.hasEpicFightAttribute(entity)
                || EpicFightCompat.hasEpicFightModel(entity);
    }

    private static void applyPose(String weapon, PoseStack poseStack) {
        switch (weapon) {
            case "battle_axe", "hammer" -> poseStack.mulPose(Axis.XP.rotationDegrees(17.0F));
            case "kama", "claw", "knuckle", "ring", "sickle" -> poseStack.translate(0.0D, 0.05D, 0.0D);
            case "pistol" -> poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            default -> {
                poseStack.translate(0.0D, 0.02D, -0.01D);
                poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
            }
        }
    }
}
