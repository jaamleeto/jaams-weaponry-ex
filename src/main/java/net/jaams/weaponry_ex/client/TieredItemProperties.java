package net.jaams.weaponry_ex.client;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.registry.TieredItems;
import net.jaams.weaponry_ex.registry.TieredItems.TierEntry;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = JaamsWeaponryExMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TieredItemProperties {

    private static final String SLINGSHOT = "slingshot";

    private static final float SLINGSHOT_FULL_DRAW = 20.0f;

    private TieredItemProperties() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (TierEntry tier : TieredItems.entries()) {
                for (int i = 0; i < tier.specs().size(); i++) {
                    if (SLINGSHOT.equals(tier.specs().get(i).name())) {
                        makeSlingshot(tier.weapons().get(i).get());
                    }
                }
            }
        });
    }

    private static void makeSlingshot(Item item) {
        ItemProperties.register(item, ResourceLocation.parse("pull"),
                (stack, level, living, seed) -> {
                    if (living == null || living.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration() - living.getUseItemRemainingTicks())
                            / SLINGSHOT_FULL_DRAW;
                });
        ItemProperties.register(item, ResourceLocation.parse("pulling"),
                (stack, level, living, seed) ->
                        living != null && living.isUsingItem() && living.getUseItem() == stack
                                ? 1.0F : 0.0F);
    }
}
