package com.emoniph.witchery.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Disturbed Cotton is an ingredient in old Witchery, not a player-placeable block.
 * The block still exists for Spirit World worldgen and internal conversions, but the
 * carried item should not place that block back into the world.
 */
public class DisturbedCottonItem extends BlockItem {
    public DisturbedCottonItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.FAIL;
    }
}
