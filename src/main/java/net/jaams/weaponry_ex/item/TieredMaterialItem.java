package net.jaams.weaponry_ex.item;

import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.tooltip.TierTooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class TieredMaterialItem extends Item {

    private final TierDefinition tier;

    public TieredMaterialItem(Properties properties, TierDefinition owner) {
        super(properties);
        this.tier = owner;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TierTooltip.add(tier, stack, tooltip);
    }
}
