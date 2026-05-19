package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Koboldite Pentacle — placed on an altar top to multiply its recharge rate x2.
 * Places a PlacedItemBlock holding this item above the altar block.
 */
public class PentacleItem extends Item {

    public PentacleItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return AltarPlacedItemSupport.placeOnAltar(ctx);
    }
}
