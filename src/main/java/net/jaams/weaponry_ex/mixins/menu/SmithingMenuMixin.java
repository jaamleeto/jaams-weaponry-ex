package net.jaams.weaponry_ex.mixins.menu;

import net.jaams.weaponry_ex.recipe.SmithingTables;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Shadow
    @Final
    @Mutable
    private List<SmithingRecipe> recipes;

    @Unique
    private ResourceLocation jaamsEx$table;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
            + "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void jaamsEx$narrowCandidates(int syncId, Inventory inventory,
            ContainerLevelAccess access, CallbackInfo ci) {
        this.jaamsEx$table = SmithingTables.tableInUse(access);
        this.recipes = SmithingTables.filter(this.recipes, this.jaamsEx$table);
    }

    @Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/"
            + "item/crafting/RecipeManager;getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;"
            + "Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    private List<SmithingRecipe> jaamsEx$narrowMatches(RecipeManager manager,
            RecipeType<SmithingRecipe> type, Container input, Level level) {
        return SmithingTables.filter(manager.getRecipesFor(type, input, level), this.jaamsEx$table);
    }

    @Inject(method = "isValidBlock", at = @At("HEAD"), cancellable = true)
    private void jaamsEx$allowConfiguredTable(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (SmithingTables.isConfiguredTable(state)) {
            cir.setReturnValue(true);
        }
    }
}
