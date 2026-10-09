package net.jaams.weaponry_ex.client;

import net.jaams.weaponry.dyeable.IDyeableItem;
import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.registry.TieredItems;
import net.jaams.weaponry_ex.registry.TieredItems.TierEntry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = JaamsWeaponryExMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class TieredItemColors {

    private TieredItemColors() {
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (TierEntry tier : TieredItems.entries()) {
            int color = tier.definition().getColor();
            ItemLike[] items = tier.allItems().stream()
                    .map(RegistryObject::get)
                    .toArray(ItemLike[]::new);
            event.getItemColors().register(
                    (stack, tintIndex) -> tintIndex > 0 ? 0xFFFFFFFF : tint(stack, color), items);
        }
    }

    private static int tint(ItemStack stack, int defaultColor) {
        return stack.getItem() instanceof IDyeableItem dyeable ? dyeable.getColor(stack) : defaultColor;
    }
}
