package net.jaams.weaponry_ex.item;

import net.jaams.weaponry.item.shinerite.ShineriteSimpleItem;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.TierHitEffects;
import net.jaams.weaponry_ex.tooltip.TierTooltip;

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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class TieredBladedGunItem extends ShineriteSimpleItem {

    private final int defaultColor;
    private final int enchantValue;
    private final TierDefinition tier;

    public TieredBladedGunItem(Item.Properties properties, int defaultColor, int enchantValue,
            TierDefinition owner) {
        super(properties);
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(equipmentSlot));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "Item modifier", 4.0D, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Item modifier", -2.4D, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(equipmentSlot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, holder -> holder.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        TierHitEffects.apply(tier, target, attacker, stack);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TierTooltip.add(tier, stack, tooltip);
    }
}
