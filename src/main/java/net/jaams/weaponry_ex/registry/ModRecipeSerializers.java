package net.jaams.weaponry_ex.registry;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.recipe.TableSmithingTransformRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, JaamsWeaponryExMod.MODID);

    public static final RegistryObject<TableSmithingTransformRecipeSerializer> TABLE_SMITHING_TRANSFORM =
            SERIALIZERS.register("smithing_transform", TableSmithingTransformRecipeSerializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
