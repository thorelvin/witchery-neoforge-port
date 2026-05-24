package com.emoniph.witchery.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Indestructible mirror-glass block that makes up Mirror World room walls. */
public class MirrorWallBlock extends Block {

    public static final MapCodec<MirrorWallBlock> CODEC = simpleCodec(MirrorWallBlock::new);

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    public MirrorWallBlock(BlockBehaviour.Properties props) {
        super(props);
    }
}
