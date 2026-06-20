package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Hidden controller block at the base of a {@link com.emoniph.witchery.worldgen.VanishingHut}.
 * Invisible and buried under the floor; it is the hut's "brain" — its block entity tracks who has
 * been inside and triggers the unobserved relocation. Not obtainable as an item (placed only by
 * the hut builder), and effectively unbreakable so players can't pry the brain out.
 */
public class VanishingHutAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<VanishingHutAnchorBlock> CODEC = simpleCodec(VanishingHutAnchorBlock::new);

    public VanishingHutAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VanishingHutAnchorBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, WitcheryBlockEntities.VANISHING_HUT_ANCHOR.get(),
                VanishingHutAnchorBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
