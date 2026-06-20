package com.emoniph.witchery.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Disturbed Cotton is an ingredient in old Witchery, not a player-placeable block. It is obtained
 * only by harvesting Wispy Cotton during a nightmare (see {@link com.emoniph.witchery.blocks.CottonBlock}).
 * Source has no disturbed-cotton worldgen — the block is kept solely as this item's registry form and
 * for internal conversions, and the carried item must not place it back into the world.
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
