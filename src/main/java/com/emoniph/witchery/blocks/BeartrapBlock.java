package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BeartrapBlockEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BeartrapBlock extends BaseEntityBlock {
    public static final MapCodec<BeartrapBlock> CODEC = simpleCodec(BeartrapBlock::new);
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create(
            "axis", Direction.Axis.class, axis -> axis != Direction.Axis.Y);

    private static final int ARM_DELAY_TICKS = 20;
    private static final VoxelShape SHAPE = box(3.2, 0.1, 3.2, 12.8, 1.6, 12.8);

    private final boolean silvered;

    public BeartrapBlock(BlockBehaviour.Properties properties) {
        this(properties, false);
    }

    public BeartrapBlock(BlockBehaviour.Properties properties, boolean silvered) {
        super(properties);
        this.silvered = silvered;
        registerDefaultState(stateDefinition.any()
                .setValue(CLOSED, false)
                .setValue(ARMED, false)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeartrapBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, WitcheryBlockEntities.BEARTRAP.get(), BeartrapBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED, ARMED, AXIS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(AXIS, context.getHorizontalDirection().getAxis())
                .setValue(ARMED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()
                && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof BeartrapBlockEntity trap) {
            trap.setOwner(player);
            level.sendBlockUpdated(pos, state, state, 3);
        }
        if (!level.isClientSide() && !state.getValue(CLOSED)) {
            level.scheduleTick(pos, this, ARM_DELAY_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CLOSED) && !state.getValue(ARMED)) {
            level.setBlock(pos, state.setValue(ARMED, true), 3);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide()
                || state.getValue(CLOSED)
                || !state.getValue(ARMED)
                || !(entity instanceof LivingEntity living)
                || !canTrap(living)) {
            return;
        }

        if (!isCreativePlayer(living)) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 2, true, true));
        }
        living.hurt(level.damageSources().fellOutOfWorld(), 4.0F);
        level.playSound(null, pos, WitcherySoundEvents.BLOCK_BEAR_TRAP_MANTRAP.get(),
                SoundSource.BLOCKS, 0.5F, soundPitch(level.getRandom()));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(DustParticleOptions.REDSTONE,
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    20, 0.25D, 0.5D, 0.25D, 0.0D);
        }
        level.setBlock(pos, state.setValue(CLOSED, true).setValue(ARMED, false), 3);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            boolean closing = !state.getValue(CLOSED);
            BlockState updated = state.setValue(CLOSED, closing).setValue(ARMED, false);
            level.setBlock(pos, updated, 3);
            level.playSound(null, pos, WitcherySoundEvents.BLOCK_BEAR_TRAP_CLICK.get(),
                    SoundSource.BLOCKS, 0.5F, soundPitch(level.getRandom()));
            if (!closing) {
                level.scheduleTick(pos, this, ARM_DELAY_TICKS);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static float soundPitch(RandomSource random) {
        return 0.4F / (random.nextFloat() * 0.4F + 0.8F);
    }

    private static boolean isCreativePlayer(LivingEntity living) {
        return living instanceof Player player && player.isCreative();
    }

    private boolean canTrap(LivingEntity living) {
        return !silvered
                || living instanceof Player player && WitcheryAttachments.get(player).isWerewolf();
    }

    public boolean isSilvered() {
        return silvered;
    }
}
