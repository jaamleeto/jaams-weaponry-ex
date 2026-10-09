package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

public class TooltipSection {

    public List<String> lines = new ArrayList<>();

    public List<String> exclude_items = new ArrayList<>();

    public List<String> only_items = new ArrayList<>();

    public boolean appliesTo(String weapon, ItemStack stack) {
        return ItemFilters.included(only_items, exclude_items, weapon, stack);
    }
}
