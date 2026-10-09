package net.jaams.weaponry_ex.recipe;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.TierRecipe;
import net.jaams.weaponry_ex.data.TierDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SmithingTables {

    private static final ResourceLocation VANILLA =
            ResourceLocation.tryParse(TierRecipe.VANILLA_SMITHING_TABLE);

    private static volatile Set<ResourceLocation> configured = Set.of();

    private SmithingTables() {
    }

    public static void collect(List<TierDefinition> tiers) {
        LinkedHashSet<ResourceLocation> found = new LinkedHashSet<>();
        for (TierDefinition tier : tiers) {
            if (tier.getRecipeType() != TierRecipe.RecipeType.SMITHING
                    || !tier.hasCustomSmithingTable()) {
                continue;
            }
            ResourceLocation table = ResourceLocation.tryParse(tier.smithingTable());
            if (table == null) {
                JaamsWeaponryExMod.LOGGER.warn("Tier '{}': 'smithing_table' '{}' is not a valid item "
                        + "id, its recipes will only be offered at the vanilla smithing table.",
                        tier.id, tier.smithingTable());
                continue;
            }
            found.add(table);
        }
        configured = Collections.unmodifiableSet(found);
    }

    public static Set<ResourceLocation> configured() {
        return configured;
    }

    public static List<SmithingRecipe> filter(List<SmithingRecipe> recipes, ResourceLocation table) {
        if (configured.isEmpty() || table == null) {
            return recipes;
        }
        List<SmithingRecipe> kept = new ArrayList<>(recipes.size());
        for (SmithingRecipe recipe : recipes) {
            if (tableOf(recipe).equals(table)) {
                kept.add(recipe);
            }
        }
        return kept;
    }

    public static ResourceLocation tableOf(SmithingRecipe recipe) {
        return recipe instanceof TableBoundSmithingRecipe bound ? bound.smithingTable() : VANILLA;
    }

    public static ResourceLocation tableInUse(ContainerLevelAccess access) {
        if (access == null) {
            return VANILLA;
        }
        ItemStack table = access.evaluate(
                (level, pos) -> new ItemStack(level.getBlockState(pos).getBlock()), ItemStack.EMPTY);
        return table.isEmpty() ? VANILLA : idOf(table.getItem());
    }

    public static boolean isConfiguredTable(BlockState state) {
        if (configured.isEmpty()) {
            return false;
        }
        return configured.contains(idOf(state.getBlock().asItem()));
    }

    private static ResourceLocation idOf(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? VANILLA : id;
    }
}
