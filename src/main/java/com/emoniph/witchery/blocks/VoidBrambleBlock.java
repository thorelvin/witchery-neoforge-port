package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.VoidBrambleBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Void bramble teleports living entities to a distant safe point in the same
 * dimension, echoing the original block's punishment effect.
 */
public class VoidBrambleBlock extends BaseEntityBlock {
    public static final MapCodec<VoidBrambleBlock> CODEC = simpleCodec(VoidBrambleBlock::new);

    private static final VoxelShape SHAPE = box(0.8, 0.0, 0.8, 15.2, 16.0, 15.2);
    private static final int TELEPORT_DISTANCE = 500;

    public VoidBrambleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidBrambleBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) {
            return;
        }
        double x = pos.getX() + random.nextFloat();
        double y = pos.getY() + 0.65F + random.nextFloat() * 0.3F;
        double z = pos.getZ() + random.nextFloat();
        level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, -1.2, 0.0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(placer instanceof Player player) || !(level.getBlockEntity(pos) instanceof VoidBrambleBlockEntity bramble)) {
            return;
        }
        bramble.setOwner(player.getScoreboardName());
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof LivingEntity)) {
            return;
        }

        teleportRandomly(level, pos, entity, TELEPORT_DISTANCE);
    }

    public static boolean teleportRandomly(Level level, BlockPos origin, Entity entity, int distance) {
        int targetX = origin.getX() + level.random.nextInt(distance * 2) - distance;
        int targetZ = origin.getZ() + level.random.nextInt(distance * 2) - distance;
        int y = origin.getY();
        int minY = level.getMinY();
        int maxY = Math.min(Math.min(origin.getY() + 64, 250), level.getMaxY() - 4);

        while (y >= minY && !isSolidAt(level, targetX, y, targetZ)) {
            y--;
        }

        while (y < maxY && !isValidLandingBase(level, targetX, y, targetZ)) {
            y++;
        }

        if (y > minY && y < maxY) {
            teleportToLocation(level, entity, targetX + 0.5, y + 1.0, targetZ + 0.5);
            return true;
        }
        return false;
    }

    private static void teleportToLocation(Level level, Entity entity, double x, double y, double z) {
        sendTeleportFeedback(level, entity);
        entity.teleportTo(x, y, z);
        sendTeleportFeedback(level, entity);
    }

    private static void sendTeleportFeedback(Level level, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        serverLevel.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z,
                20, 0.5D, 1.0D, 0.5D, 0.02D);
        float pitch = 0.4F / (level.random.nextFloat() * 0.4F + 0.8F);
        SoundSource source = entity instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        serverLevel.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, source, 0.5F, pitch);
    }

    private static boolean isValidLandingBase(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isSolid()
                && !level.getBlockState(pos).is(Blocks.BEDROCK)
                && level.isEmptyBlock(pos.above())
                && level.isEmptyBlock(pos.above(2))
                && level.isEmptyBlock(pos.above(3));
    }

    private static boolean isSolidAt(Level level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).isSolid();
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof VoidBrambleBlockEntity bramble)) {
            return;
        }
        if (!player.isCreative() && !bramble.isOwner(player)) {
            return;
        }

        for (BlockPos cursor = pos; level.getBlockState(cursor).is(this); cursor = cursor.above()) {
            level.destroyBlock(cursor, false, player);
            popResource(level, cursor, asItem().getDefaultInstance());
        }
    }
}
