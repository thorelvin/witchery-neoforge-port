package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.VoidBrambleBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }

        for (int i = 0; i < 16; i++) {
            int targetX = pos.getX() + level.random.nextInt(TELEPORT_DISTANCE * 2) - TELEPORT_DISTANCE;
            int targetZ = pos.getZ() + level.random.nextInt(TELEPORT_DISTANCE * 2) - TELEPORT_DISTANCE;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(targetX, pos.getY(), targetZ));
            double x = targetX + 0.5;
            double y = top.getY() + 1.0;
            double z = targetZ + 0.5;
            if (living.randomTeleport(x, y, z, true)) {
                return;
            }
        }
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
