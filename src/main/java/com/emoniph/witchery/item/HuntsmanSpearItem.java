package com.emoniph.witchery.item;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class HuntsmanSpearItem extends Item {
    private static final int USE_DURATION = 72000;
    private static final int SOURCE_DIAMOND_DURABILITY = 1561;
    private static final double SOURCE_ATTACK_DAMAGE = 8.0;
    private static final double SOURCE_KNOCKBACK_RESISTANCE = 1.0;

    public HuntsmanSpearItem(Properties props) {
        super(props.durability(SOURCE_DIAMOND_DURABILITY)
                .rarity(Rarity.EPIC)
                .attributes(sourceAttributes()));
    }

    private static ItemAttributeModifiers sourceAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id("huntsman_spear_attack_damage"),
                                SOURCE_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(id("huntsman_spear_knockback_resistance"),
                                SOURCE_KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.huntsmans_spear.tip")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
