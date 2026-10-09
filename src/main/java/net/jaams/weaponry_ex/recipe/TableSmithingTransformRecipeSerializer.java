package net.jaams.weaponry_ex.recipe;

import com.google.gson.JsonObject;

import net.jaams.weaponry_ex.data.TierRecipe;
import net.jaams.weaponry_ex.data.TierDefinition;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class TableSmithingTransformRecipeSerializer
        implements RecipeSerializer<TableSmithingTransformRecipe> {

    @Override
    public TableSmithingTransformRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient template = Ingredient.fromJson(GsonHelper.getNonNull(json, "template"));
        Ingredient base = Ingredient.fromJson(GsonHelper.getNonNull(json, "base"));
        Ingredient addition = Ingredient.fromJson(GsonHelper.getNonNull(json, "addition"));
        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new TableSmithingTransformRecipe(id, template, base, addition, result,
                table(json));
    }

    @Override
    public TableSmithingTransformRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient template = Ingredient.fromNetwork(buf);
        Ingredient base = Ingredient.fromNetwork(buf);
        Ingredient addition = Ingredient.fromNetwork(buf);
        ItemStack result = buf.readItem();
        ResourceLocation table = buf.readResourceLocation();
        return new TableSmithingTransformRecipe(id, template, base, addition, result, table);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, TableSmithingTransformRecipe recipe) {
        for (Ingredient ingredient : recipe.ingredients()) {
            ingredient.toNetwork(buf);
        }
        buf.writeItem(recipe.getResultItem(null));
        buf.writeResourceLocation(recipe.smithingTable());
    }

    private static ResourceLocation table(JsonObject json) {
        ResourceLocation parsed = ResourceLocation.tryParse(
                GsonHelper.getAsString(json, "smithing_table", TierRecipe.VANILLA_SMITHING_TABLE));
        return parsed == null ? ResourceLocation.tryParse(TierRecipe.VANILLA_SMITHING_TABLE)
                : parsed;
    }
}
