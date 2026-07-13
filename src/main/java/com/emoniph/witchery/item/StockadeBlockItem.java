package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.StockadeBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class StockadeBlockItem extends BlockItem {
    public StockadeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        StockadeBlock.WoodVariant variant = StockadeBlock.getWoodVariant(stack);
        return Component.translatable("block.witchery.stockade." + variant.getSerializedName());
    }
}
