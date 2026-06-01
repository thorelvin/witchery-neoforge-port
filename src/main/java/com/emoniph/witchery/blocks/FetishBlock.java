package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.FetishBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FetishBlock extends BaseEntityBlock {

    public static final MapCodec<FetishBlock> CODEC = simpleCodec(FetishBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public FetishBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FetishBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.is(WitcheryBlocks.FETISH_WITCHS_LADDER.get()) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, com.emoniph.witchery.init.WitcheryBlockEntities.FETISH.get(),
                FetishBlockEntity::serverTick);
    }

    // ── Read effectType from item when placed ─────────────────────────────────

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        FetishBlockEntity be = getEntity(level, pos);
        if (be == null) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        be.setEffectType(tag.getIntOr("FetishEffect", 0));
        be.setFacing(state.getValue(FACING));
        be.setChanged();
    }

    // ── Write effectType to dropped item ──────────────────────────────────────

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity be, ItemStack tool) {
        if (!level.isClientSide() && be instanceof FetishBlockEntity fetish && fetish.getEffectType() != 0) {
            ItemStack drop = new ItemStack(this);
            CustomData.update(DataComponents.CUSTOM_DATA, drop, tag -> tag.putInt("FetishEffect", fetish.getEffectType()));
            popResource(level, pos, drop);
            level.removeBlock(pos, false);
            return;
        }
        super.playerDestroy(level, player, pos, state, be, tool);
    }

    // ── Right-click: taglock (known player) or boline (cycle mode) ────────────

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        FetishBlockEntity be = getEntity(level, pos);
        if (be == null) return InteractionResult.PASS;
        ItemStack held = player.getMainHandItem();
        if (TaglockKitItem.isTaglockKit(held) && TaglockKitItem.isFilled(held)) {
            String name = TaglockKitItem.getBoundName(held);
            if (name != null && !name.isEmpty()) {
                be.toggleKnownPlayer(name);
                player.displayClientMessage(net.minecraft.network.chat.Component
                        .translatable("witchery.fetish.players", be.knownPlayersDisplay()), false);
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(WitcheryItems.BOLINE.get())) {
            be.cycleAlarmMode();
            player.displayClientMessage(net.minecraft.network.chat.Component
                    .translatable("witchery.fetish.mode." + be.getAlarmMode()), false);
            be.setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ── Redstone signal ───────────────────────────────────────────────────────

    @Override
    public boolean isSignalSource(BlockState state) { return true; }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction dir) {
        FetishBlockEntity be = getEntity(level, pos);
        return be != null ? be.getPowerLevel() : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction dir) {
        return dir == net.minecraft.core.Direction.UP ? getSignal(state, level, pos, dir) : 0;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @Nullable
    private FetishBlockEntity getEntity(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof FetishBlockEntity f ? f : null;
    }
}
