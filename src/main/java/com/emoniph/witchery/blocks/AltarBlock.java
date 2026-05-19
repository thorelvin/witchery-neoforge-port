package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static com.emoniph.witchery.init.WitcheryBlockEntities.ALTAR;

public class AltarBlock extends BaseEntityBlock {
    public static final MapCodec<AltarBlock> CODEC = simpleCodec(AltarBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);
    public static final EnumProperty<JoinedPart> PART = EnumProperty.create("part", JoinedPart.class);

    public enum JoinedPart implements StringRepresentable {
        NOT_CONNECTED("not_connected"),
        BACK_LEFT("back_left"),
        BACK_MIDDLE("back_middle"),
        BACK_RIGHT("back_right"),
        FRONT_LEFT("front_left"),
        FRONT_MIDDLE("front_middle"),
        FRONT_RIGHT("front_right");

        private final String name;

        JoinedPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public AltarBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(PART, JoinedPart.NOT_CONNECTED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS, PART);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AltarBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** Right-click to inspect current power level. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            player.displayClientMessage(Component.literal(
                    "Altar power: " + altar.getPower() + " / " + altar.getEffectiveMaxPower()
                    + "  (recharge: " + altar.getEffectiveRecharge() + "/20t"
                    + ", range: " + altar.getRange()
                    + ", valid: " + altar.hasValidStructure() + ")"), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            refreshNearbyAltars(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            refreshNearbyAltars(level, pos);
        }
    }

    private static void refreshNearbyAltars(Level level, BlockPos pos) {
        refreshIfAltar(level, pos);
        refreshIfAltar(level, pos.north());
        refreshIfAltar(level, pos.south());
        refreshIfAltar(level, pos.east());
        refreshIfAltar(level, pos.west());
    }

    private static void refreshIfAltar(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            altar.invalidate(level);
        }
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, ALTAR.get(), AltarBlockEntity::serverTick);
    }
}
