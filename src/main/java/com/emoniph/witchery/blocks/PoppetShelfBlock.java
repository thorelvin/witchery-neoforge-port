package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.data.AnchoredCurseSavedData;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.RedThreadBinding;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PoppetShelfBlock extends BaseEntityBlock {
    public static final MapCodec<PoppetShelfBlock> CODEC = simpleCodec(PoppetShelfBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 8.16, 16.0);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public PoppetShelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoppetShelfBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(WitcheryItems.RED_THREAD.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof PoppetShelfBlockEntity shelf)) {
            return InteractionResult.CONSUME;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "witchery.red_thread.anchor.unbreakable"), false);
            return InteractionResult.CONSUME;
        }
        if (!WitcheryProtectionService.canModifyBlock(serverPlayer, serverLevel, pos,
                WitcheryProtectionAction.ITEM_TRANSFER)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "witchery.red_thread.anchor.protected"), false);
            return InteractionResult.CONSUME;
        }

        java.util.List<Integer> eligible = new java.util.ArrayList<>();
        for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
            if (RedThreadBinding.canPrepare(shelf.getItem(slot))) {
                eligible.add(slot);
            }
        }
        if (eligible.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "witchery.red_thread.anchor.none"), false);
            return InteractionResult.CONSUME;
        }
        if (eligible.size() > 1) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "witchery.red_thread.anchor.ambiguous"), false);
            return InteractionResult.CONSUME;
        }

        ItemStack poppet = shelf.getItem(eligible.getFirst());
        String targetName = PoppetItem.getBoundName(poppet);
        if (!RedThreadBinding.prepare(poppet, serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        shelf.setChanged();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xB51F35, 0.8F),
                pos.getX() + 0.5D, pos.getY() + 0.72D, pos.getZ() + 0.5D,
                18, 0.3D, 0.15D, 0.3D, 0.01D);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHAIN_PLACE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.65F, 1.35F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "witchery.red_thread.anchor.prepared", targetName), false);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PoppetShelfBlockEntity shelf) {
            player.openMenu(shelf);
        }
        return (level.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS_SERVER : net.minecraft.world.InteractionResult.SUCCESS);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PoppetShelfBlockEntity shelf) {
            Containers.dropContents(level, pos, shelf);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        AnchoredCurseSavedData.get(level.getServer()).terminateAt(
                level, pos, AnchoredCurseSavedData.TerminationCause.BROKEN);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
}
