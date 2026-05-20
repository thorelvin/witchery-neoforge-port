package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.world.TormentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Torment Portal — a 1×2 red portal block placed at the exit of each maze level.
 *
 * Collision behaviour:
 *   • Outside Torment → send player to a random Torment level.
 *   • Inside  Torment → 95% chance return to Overworld; 5% chance send to another level.
 * A 100-tick cooldown prevents portal loops.
 */
public class TormentPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS =
            EnumProperty.create("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public TormentPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state) {
        return 0;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        var data = WitcheryAttachments.get(player);
        if (data.getTormentPortalCooldown() > 0) return;
        data.setTormentPortalCooldown(100);

        if (level.dimension().equals(WitcheryDimensions.TORMENT)) {
            // Inside Torment: 95 % exit, 5 % more Torment
            if (level.random.nextFloat() < 0.95f) {
                TormentHelper.returnFromTorment(player);
            } else {
                TormentHelper.sendToTorment(player, level.random.nextInt(TormentHelper.NUM_LEVELS));
            }
        } else {
            TormentHelper.sendToTorment(player, level.random.nextInt(TormentHelper.NUM_LEVELS));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F,
                    random.nextFloat() * 0.4F + 0.8F, false);
        }

        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }
}
