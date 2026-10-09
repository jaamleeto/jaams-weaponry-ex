package net.jaams.weaponry_ex.item;

import net.jaams.weaponry.dyeable.IDyeableItem;
import net.jaams.weaponry.util.ModUtils;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierHitEffects;
import net.jaams.weaponry_ex.tooltip.TierTooltip;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class TieredGauntletItem extends Item implements IDyeableItem {

    private static final String HIT_SOUND = "jaams_weaponry:gauntlet_hit";

    private final int defaultColor;
    private final int enchantValue;
    private final float attackDamage;
    private final TierDefinition tier;

    public TieredGauntletItem(Item.Properties properties, float attackDamage, int defaultColor,
            int enchantValue, TierDefinition owner) {
        super(properties);
        this.attackDamage = attackDamage;
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
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier",
                (double) attackDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Item modifier",
                -2.4D, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TierTooltip.add(tier, stack, tooltip);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (target != null && attacker != null) {
            ModUtils.playSound(target, HIT_SOUND);
        }
        TierHitEffects.apply(tier, target, attacker, stack);
        return result;
    }
}