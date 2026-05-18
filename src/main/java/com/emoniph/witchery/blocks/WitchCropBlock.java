package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Witch Crop (Rowan crop) — 8 stages (0-7, same as vanilla wheat). Drops Rowan Berries when mature. */
public class WitchCropBlock extends CropBlock {
    public WitchCropBlock(BlockBehaviour.Properties props) { super(props); }
    // Uses default CropBlock.AGE (AGE_7, stages 0-7) — no override needed.
    @Override
    protected ItemLike getBaseSeedId() { return WitcheryItems.WITCH_SEEDS.get(); }
}
