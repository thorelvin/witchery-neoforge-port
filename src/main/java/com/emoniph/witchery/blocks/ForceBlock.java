package com.emoniph.witchery.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Torment maze force walls are solid barriers, but old Witchery set their light
 * opacity to zero. Keeping that behavior prevents the dimension from becoming
 * pitch black under stacked maze ceilings.
 */
public class ForceBlock extends Block {
    public static final MapCodec<ForceBlock> CODEC = simpleCodec(ForceBlock::new);

    public ForceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return 0;
    }
}
