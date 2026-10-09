package net.jaams.weaponry_ex.item;

import net.jaams.weaponry.item.shinerite.ShineritePickaxeItem;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierHitEffects;
import net.jaams.weaponry_ex.tooltip.TierTooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class TieredPickaxeItem extends ShineritePickaxeItem {

    private final int defaultColor;
    private final int enchantValue;
    private final TierDefinition tier;

    public TieredPickaxeItem(Tier tier, int attackDamageModifier, float attackSpeedModifier,
            Item.Properties properties, int defaultColor, int enchantValue, TierDefinition owner) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.defaultColor = defaultColor;
        this.enchantValue = enchantValue;
        this.tier = owner;
    }

    @Override
    public int getDefaultColor() {
        return defaultColor;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantValue;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TierTooltip.add(tier, stack, tooltip);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        TierHitEffects.apply(tier, target, attacker, stack);
        return result;
    }
}
