package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BrewFluidHoldBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/** Invisible source {@code BlockSlurp}: temporarily holds water or lava, then restores it. */
public final class BrewFluidHoldBlock extends BaseEntityBlock {
    public enum HeldFluid implements StringRepresentable {
        WATER("water"), LAVA("lava");

        private final String serializedName;

        HeldFluid(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final MapCodec<BrewFluidHoldBlock> CODEC = simpleCodec(BrewFluidHoldBlock::new);
    public static final EnumProperty<HeldFluid> FLUID = EnumProperty.create("fluid", HeldFluid.class);
    public static final BooleanProperty SOURCE = BooleanProperty.create("source");

    public BrewFluidHoldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FLUID, HeldFluid.WATER).setValue(SOURCE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLUID, SOURCE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewFluidHoldBlockEntity(pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrewFluidHoldBlockEntity hold
                && hold.originalState() != null) {
            level.setBlock(pos, hold.originalState(), Block.UPDATE_ALL);
            return;
        }
        var fluid = state.getValue(FLUID) == HeldFluid.WATER
                ? (state.getValue(SOURCE) ? Fluids.WATER : Fluids.FLOWING_WATER)
                : (state.getValue(SOURCE) ? Fluids.LAVA : Fluids.FLOWING_LAVA);
        level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
    }
}
