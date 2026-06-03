package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class BlightFibreBlock extends VineBlock {
    public static final BooleanProperty NORTH = VineBlock.NORTH;
    public static final BooleanProperty EAST = VineBlock.EAST;
    public static final BooleanProperty SOUTH = VineBlock.SOUTH;
    public static final BooleanProperty WEST = VineBlock.WEST;
    public static final BooleanProperty UP = VineBlock.UP;

    private static final Direction[] ATTACHMENT_DIRECTIONS = {
            Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public BlightFibreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !BlightHelper.isProtected(level, pos) && super.canSurvive(state, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && !BlightHelper.isProtected(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        BlightHelper.decayIfOrphaned(level, pos, state, random);
        if (level.getBlockState(pos).is(this)) {
            BlightHelper.trySpreadNear(level, pos, random, false);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        BlockState updated = super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        if (!updated.is(this) || BlightHelper.isProtected(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return updated;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && level.random.nextInt(120) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide() && tool.is(Items.SHEARS) && !player.getAbilities().instabuild) {
            popResource(level, pos, new ItemStack(WitcheryItems.BLIGHT_ICHOR.get()));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) != 0) {
            return;
        }
        double y = 0.25D + random.nextDouble() * 0.5D;
        level.addParticle(ParticleTypes.WITCH,
                pos.getX() + random.nextDouble(),
                pos.getY() + y,
                pos.getZ() + random.nextDouble(),
                0.0D, 0.01D, 0.0D);
    }

    public static boolean hasAttachableFace(LevelReader level, BlockPos pos) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            if (canAttachTo(level, pos, direction)) {
                return true;
            }
        }
        return false;
    }

    public static BlockState withAttachments(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            state = state.setValue(VineBlock.getPropertyForFace(direction), canAttachTo(level, pos, direction));
        }
        return state;
    }

    public static boolean hasAnyAttachment(BlockState state) {
        for (Direction direction : ATTACHMENT_DIRECTIONS) {
            if (state.getValue(VineBlock.getPropertyForFace(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canAttachTo(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos supportPos = pos.relative(direction);
        BlockState support = level.getBlockState(supportPos);
        return !support.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                && VineBlock.isAcceptableNeighbour(level, supportPos, direction);
    }
}
