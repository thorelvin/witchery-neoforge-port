package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.ShadedGlassBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

public class ShadedGlassBlockItem extends BlockItem {
    public ShadedGlassBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor color = getColor(stack);
        return Component.translatable("block.witchery.shaded_glass." + color.getName());
    }

    public static DyeColor getColor(ItemStack stack) {
        BlockItemStateProperties state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        DyeColor color = state.get(ShadedGlassBlock.COLOR);
        return color == null ? DyeColor.WHITE : color;
    }
}
