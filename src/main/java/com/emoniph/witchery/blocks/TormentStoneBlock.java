package com.emoniph.witchery.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Visible Torment maze stone from old BlockForce(false): solid, unbreakable,
 * but with source light opacity 0 so maze rooms do not become fully dark.
 */
public class TormentStoneBlock extends Block {
    public static final MapCodec<TormentStoneBlock> CODEC = simpleCodec(TormentStoneBlock::new);

    public TormentStoneBlock(BlockBehaviour.Properties properties) {
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
