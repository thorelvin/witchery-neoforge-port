package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ember Moss is a low, spreading fire-moss that only drops itself when sheared.
 */
public class EmberMossBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(1.6, 0.0, 1.6, 14.4, 6.4, 14.4);

    public EmberMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0 || tooDense(level, pos)) {
            return;
        }

        BlockPos candidate = pos;
        for (int i = 0; i < 4; i++) {
            BlockPos next = candidate.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            if (canSpreadTo(level, next)) {
                candidate = next;
            }
        }

        if (canSpreadTo(level, candidate)) {
            level.setBlock(candidate, defaultBlockState(), 3);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || living.fireImmune() || living.isOnFire()) {
            return;
        }
        if (living instanceof Player player && player.getAbilities().instabuild) {
            return;
        }

        living.igniteForSeconds(3.0F);
        level.playSound(null, pos, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 0.5F, 1.0F);
        ((ServerLevel) level).sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5,
                8, 0.35, 0.15, 0.35, 0.02);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) != 0) {
            return;
        }
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.8;
        double y = pos.getY() + 0.15 + random.nextDouble() * 0.3;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.8;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide() && tool.is(Items.SHEARS) && !player.getAbilities().instabuild) {
            popResource(level, pos, asItem().getDefaultInstance());
        }
    }

    private boolean tooDense(ServerLevel level, BlockPos pos) {
        int remaining = 5;
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(scan).is(this) && --remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean canSpreadTo(LevelReader level, BlockPos pos) {
        return level.isEmptyBlock(pos)
                && canSpreadOver(level.getBlockState(pos.below()))
                && canSurvive(defaultBlockState(), level, pos);
    }

    private boolean canSpreadOver(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SAND)
                || state.is(Blocks.FARMLAND);
    }
}
