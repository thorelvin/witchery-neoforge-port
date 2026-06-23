package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.SunCollectorBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SunCollectorBlock extends BaseEntityBlock {

    public static final MapCodec<SunCollectorBlock> CODEC = simpleCodec(SunCollectorBlock::new);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private static final VoxelShape SHAPE = Block.box(3.2, 0.0, 3.2, 12.8, 12.8, 12.8);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public SunCollectorBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SunCollectorBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, WitcheryBlockEntities.SUN_COLLECTOR.get(),
                                     SunCollectorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                       BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SunCollectorBlockEntity be)) return InteractionResult.PASS;
        if (!be.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!stack.is(WitcheryItems.QUARTZ_SPHERE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;

        be.insertSphere(stack, level);
        playCollectorFeedback(level, pos, true);
        player.displayClientMessage(Component.translatable("witchery.sun_collector.inserted"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SunCollectorBlockEntity be)) return InteractionResult.PASS;
        if (be.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.sun_collector.empty"), true);
            return InteractionResult.SUCCESS;
        }

        ItemStack result = be.retrieveStored(level);
        if (!player.addItem(result)) {
            Block.popResource(level, pos, result);
        }
        playCollectorFeedback(level, pos, false);
        player.displayClientMessage(Component.translatable("witchery.sun_collector.retrieved"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SunCollectorBlockEntity be
                && !be.isEmpty()) {
            Block.popResource(level, pos, be.getStored().copy());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static void playCollectorFeedback(Level level, BlockPos pos, boolean inserting) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, pos,
                inserting ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BEACON_POWER_SELECT,
                SoundSource.BLOCKS, inserting ? 0.65F : 0.45F, inserting ? 1.25F : 0.8F);
        serverLevel.sendParticles(inserting ? ParticleTypes.END_ROD : ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.62D, pos.getZ() + 0.5D,
                inserting ? 14 : 8, 0.2D, 0.2D, 0.2D, inserting ? 0.03D : 0.01D);
    }
}
