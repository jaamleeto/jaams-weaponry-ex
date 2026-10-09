package net.jaams.weaponry_ex.recipe;

import net.jaams.weaponry_ex.registry.ModRecipeSerializers;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

import java.util.stream.Stream;

public class TableSmithingTransformRecipe implements SmithingRecipe, TableBoundSmithingRecipe {

    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;
    private final ResourceLocation smithingTable;

    public TableSmithingTransformRecipe(ResourceLocation id, Ingredient template, Ingredient base,
            Ingredient addition, ItemStack result, ResourceLocation smithingTable) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result;
        this.smithingTable = smithingTable;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return template.test(container.getItem(0))
                && base.test(container.getItem(1))
                && addition.test(container.getItem(2));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack assembled = result.copy();
        CompoundTag tag = container.getItem(1).getTag();
        if (tag != null) {
            assembled.setTag(tag.copy());
        }
        return assembled;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(template, base, addition).anyMatch(net.minecraftforge.common.ForgeHooks::hasNoElements);
    }

    @Override
    public ResourceLocation smithingTable() {
        return smithingTable;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public Ingredient[] ingredients() {
        return new Ingredient[] { template, base, addition };
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.TABLE_SMITHING_TRANSFORM.get();
    }
}
