package com.emoniph.witchery.brewing;

import com.emoniph.witchery.blocks.StockadeBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Source-shaped block conversion shared by direct Brew of Frost use and composed Ice World brews. */
public final class IceWorldConversion {
    private static final double SOURCE_RADIUS_SCALE = 1.5D;
    private static final int REPLACEMENT_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final int DOOR_REPLACEMENT_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS;

    private IceWorldConversion() {
    }

    /** Converts one clicked block for the direct Brew of Frost compatibility route. */
    public static int convertSingle(ServerLevel level, BlockPos pos, @Nullable Entity actor) {
        int changed = convertDoor(level, pos, actor);
        if (changed == 0) {
            changed = convertRegularBlock(level, pos, actor);
        }
        if (changed > 0) {
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
        return changed;
    }

    /**
     * Applies the original two filled-sphere passes: wooden doors first, then terrain and fixtures.
     * {@code sourceRadius} is the dispersal radius (3 + Extent for an instant splash).
     */
    public static int convertArea(ServerLevel level, BlockPos center, int sourceRadius,
                                  @Nullable Entity actor) {
        int areaRadius = sourceAreaRadius(sourceRadius);
        List<BlockPos> positions = sourceSpherePositions(center, areaRadius);
        int changed = 0;

        for (BlockPos pos : positions) {
            if (isWoodenDoor(level.getBlockState(pos))) {
                changed += convertDoor(level, pos, actor);
            }
        }
        for (BlockPos pos : positions) {
            changed += convertRegularBlock(level, pos, actor);
        }

        level.playSound(null, center, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS, 0.5F, 2.0F);
        return changed;
    }

    /** Original {@code ceil(max(radius * 1.5, 1))} Ice World extent. */
    public static int sourceAreaRadius(double sourceRadius) {
        return (int) Math.ceil(Math.max(sourceRadius * SOURCE_RADIUS_SCALE, 1.0D));
    }

    static boolean sourceSphereContains(int dx, int dy, int dz, int radius) {
        if (radius <= 1) {
            return dx == 0 && dy == 0 && dz == 0;
        }
        int distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared < radius * radius - 1;
    }

    static boolean sourceCanBreak(float destroySpeed) {
        return destroySpeed >= 0.0F;
    }

    static BlockState replacementFor(BlockState current) {
        if (current.is(WitcheryBlocks.STOCKADE.get())) {
            return copyStockadeProperties(current, WitcheryBlocks.ICE_STOCKADE.get().defaultBlockState());
        }
        if (isIceWorldOutput(current)) {
            return null;
        }
        if (current.getBlock() instanceof FenceGateBlock) {
            return copyGateProperties(current, WitcheryBlocks.ICE_GATE.get().defaultBlockState());
        }
        if (current.getBlock() instanceof FenceBlock) {
            return copyFenceProperties(current, WitcheryBlocks.ICE_FENCE.get().defaultBlockState());
        }
        if (current.getBlock() instanceof StairBlock) {
            return copyStairProperties(current, WitcheryBlocks.ICE_STAIRS.get().defaultBlockState());
        }
        if (current.getBlock() instanceof SlabBlock) {
            return copySlabProperties(current, WitcheryBlocks.ICE_SLAB.get().defaultBlockState());
        }
        if (current.getBlock() instanceof PressurePlateBlock) {
            return copyProperty(current, WitcheryBlocks.ICE_PRESSURE_PLATE.get().defaultBlockState(),
                    BlockStateProperties.POWERED);
        }
        if (current.is(Blocks.DIRT)
                || current.is(Blocks.GRASS_BLOCK)
                || current.is(Blocks.MYCELIUM)
                || current.is(Blocks.SAND)) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        if (current.is(Blocks.COBBLESTONE)
                || current.is(Blocks.STONE_BRICKS)
                || current.is(BlockTags.LOGS)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (current.is(Blocks.STONE)
                || current.is(Blocks.BRICKS)
                || current.is(Blocks.SANDSTONE)
                || current.is(BlockTags.PLANKS)
                || current.is(BlockTags.LEAVES)) {
            return WitcheryBlocks.PERPETUAL_ICE.get().defaultBlockState();
        }
        return null;
    }

    private static List<BlockPos> sourceSpherePositions(BlockPos center, int radius) {
        if (radius <= 1) {
            return List.of(center.immutable());
        }
        List<BlockPos> positions = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (sourceSphereContains(dx, dy, dz, radius)) {
                        positions.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }
        return positions;
    }

    private static int convertRegularBlock(ServerLevel level, BlockPos pos, @Nullable Entity actor) {
        if (!canTransform(level, pos, actor)) {
            return 0;
        }
        BlockState current = level.getBlockState(pos);
        if (isWoodenDoor(current)) {
            return 0;
        }
        BlockState replacement = replacementFor(current);
        if (replacement == null || replacement.getBlock() == current.getBlock()) {
            return 0;
        }
        if (!level.setBlock(pos, replacement, REPLACEMENT_FLAGS)) {
            return 0;
        }
        sendConversionParticles(level, pos);
        return 1;
    }

    private static int convertDoor(ServerLevel level, BlockPos pos, @Nullable Entity actor) {
        if (!isReadable(level, pos)) {
            return 0;
        }
        BlockState clicked = level.getBlockState(pos);
        if (!isWoodenDoor(clicked)) {
            return 0;
        }

        BlockPos lowerPos = clicked.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos upperPos = lowerPos.above();
        if (!canTransform(level, lowerPos, actor) || !canTransform(level, upperPos, actor)) {
            return 0;
        }
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(upperPos);
        if (!isWoodenDoor(lower) || !isWoodenDoor(upper)
                || lower.getBlock() != upper.getBlock()
                || lower.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
                || upper.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) {
            return 0;
        }

        BlockState iceLower = copyDoorProperties(lower, WitcheryBlocks.ICE_DOOR.get().defaultBlockState())
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState iceUpper = copyDoorProperties(upper, WitcheryBlocks.ICE_DOOR.get().defaultBlockState())
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        if (!level.setBlock(lowerPos, iceLower, DOOR_REPLACEMENT_FLAGS)) {
            return 0;
        }
        if (!level.setBlock(upperPos, iceUpper, DOOR_REPLACEMENT_FLAGS)) {
            level.setBlock(lowerPos, lower, DOOR_REPLACEMENT_FLAGS);
            return 0;
        }
        level.updateNeighborsAt(lowerPos, iceLower.getBlock());
        level.updateNeighborsAt(upperPos, iceUpper.getBlock());
        sendConversionParticles(level, upperPos);
        return 2;
    }

    private static boolean canTransform(ServerLevel level, BlockPos pos, @Nullable Entity actor) {
        if (!isReadable(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.hasBlockEntity()
                && level.getBlockEntity(pos) == null
                && sourceCanBreak(state.getDestroySpeed(level, pos))
                && WitcheryTerrainProtection.canTransform(actor, level, pos);
    }

    private static boolean isReadable(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.hasChunkAt(pos);
    }

    private static boolean isWoodenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS);
    }

    private static boolean isIceWorldOutput(BlockState state) {
        return state.is(WitcheryBlocks.ICE_DOOR.get())
                || state.is(WitcheryBlocks.ICE_FENCE.get())
                || state.is(WitcheryBlocks.ICE_GATE.get())
                || state.is(WitcheryBlocks.ICE_PRESSURE_PLATE.get())
                || state.is(WitcheryBlocks.ICE_SLAB.get())
                || state.is(WitcheryBlocks.ICE_STAIRS.get())
                || state.is(WitcheryBlocks.ICE_STOCKADE.get())
                || state.is(WitcheryBlocks.PERPETUAL_ICE.get())
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.PACKED_ICE);
    }

    private static BlockState copyDoorProperties(BlockState from, BlockState to) {
        BlockState copied = copyProperty(from, to, DoorBlock.FACING);
        copied = copyProperty(from, copied, DoorBlock.HINGE);
        copied = copyProperty(from, copied, DoorBlock.OPEN);
        return copyProperty(from, copied, DoorBlock.POWERED);
    }

    private static BlockState copyGateProperties(BlockState from, BlockState to) {
        BlockState copied = copyProperty(from, to, FenceGateBlock.FACING);
        copied = copyProperty(from, copied, FenceGateBlock.OPEN);
        copied = copyProperty(from, copied, FenceGateBlock.POWERED);
        return copyProperty(from, copied, FenceGateBlock.IN_WALL);
    }

    private static BlockState copyFenceProperties(BlockState from, BlockState to) {
        BlockState copied = copyProperty(from, to, BlockStateProperties.NORTH);
        copied = copyProperty(from, copied, BlockStateProperties.EAST);
        copied = copyProperty(from, copied, BlockStateProperties.SOUTH);
        copied = copyProperty(from, copied, BlockStateProperties.WEST);
        return copyProperty(from, copied, BlockStateProperties.WATERLOGGED);
    }

    private static BlockState copyStairProperties(BlockState from, BlockState to) {
        BlockState copied = copyProperty(from, to, StairBlock.FACING);
        copied = copyProperty(from, copied, StairBlock.HALF);
        copied = copyProperty(from, copied, StairBlock.SHAPE);
        return copyProperty(from, copied, StairBlock.WATERLOGGED);
    }

    private static BlockState copySlabProperties(BlockState from, BlockState to) {
        BlockState copied = copyProperty(from, to, SlabBlock.TYPE);
        if (copied.hasProperty(SlabBlock.TYPE) && copied.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            return copied.setValue(SlabBlock.WATERLOGGED, false);
        }
        return copyProperty(from, copied, SlabBlock.WATERLOGGED);
    }

    private static BlockState copyStockadeProperties(BlockState from, BlockState to) {
        return to.setValue(StockadeBlock.NORTH, from.getValue(StockadeBlock.NORTH))
                .setValue(StockadeBlock.EAST, from.getValue(StockadeBlock.EAST))
                .setValue(StockadeBlock.SOUTH, from.getValue(StockadeBlock.SOUTH))
                .setValue(StockadeBlock.WEST, from.getValue(StockadeBlock.WEST))
                .setValue(StockadeBlock.UP, from.getValue(StockadeBlock.UP))
                .setValue(StockadeBlock.WATERLOGGED, from.getValue(StockadeBlock.WATERLOGGED));
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
                                                                      Property<T> property) {
        if (!from.hasProperty(property) || !to.hasProperty(property)) {
            return to;
        }
        return to.setValue(property, from.getValue(property));
    }

    private static void sendConversionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
    }
}
