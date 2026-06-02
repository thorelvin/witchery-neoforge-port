package com.emoniph.witchery.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Hellhound Head trophy block — the hellhound variant of {@link WolfHeadBlock}.
 *
 * Source parity: {@code BlockWolfHead} skullType 1 (1.7.10-0.24.1). A thin subclass keeps the
 * shared placement/shape/BESR while supplying its own {@link MapCodec} (so {@code simpleCodec}
 * still resolves a {@code (Properties)} constructor) and flagging the hellhound texture.
 */
public class HellhoundHeadBlock extends WolfHeadBlock {

    public static final MapCodec<HellhoundHeadBlock> CODEC = simpleCodec(HellhoundHeadBlock::new);

    public HellhoundHeadBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isHellhound() {
        return true;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
