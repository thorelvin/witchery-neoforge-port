package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.EntEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.blocks.StockadeBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Internal `brew_of_ice` item, displayed as Brew of Frost like original Witchery. */
public class BrewOfIceItem extends BrewItem {
    private static final int FREEZE_RANGE = 3;
    private static final int SHIELD_HEIGHT = 3;
    private static final int CAGE_HEIGHT = 4;

    private static final BlockPos[] CAGE_RING_OFFSETS = {
            new BlockPos(-2, 0, -1),
            new BlockPos(-2, 0, 0),
            new BlockPos(-1, 0, 1),
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0),
            new BlockPos(1, 0, -1),
            new BlockPos(0, 0, -2),
            new BlockPos(-1, 0, -2),
            new BlockPos(-2, 0, -2),
            new BlockPos(-2, 0, 1),
            new BlockPos(1, 0, 1),
            new BlockPos(1, 0, -2)
    };

    private static final BlockPos[] CAGE_TOP_OFFSETS = {
            BlockPos.ZERO,
            new BlockPos(-1, 0, -1),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, -1)
    };

    public BrewOfIceItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos clicked = context.getClickedPos();
        if (tryApplyIceWorldConversion(serverLevel, clicked) > 0) {
            consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
            return InteractionResult.SUCCESS;
        }

        int placed;
        if (hasAdjacentWater(serverLevel, clicked)) {
            placed = freezeSurroundingWater(serverLevel, clicked, FREEZE_RANGE);
        } else if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        } else {
            BlockPos shieldPos = context.getClickedFace() == Direction.UP
                    ? clicked
                    : clicked.relative(context.getClickedFace());
            placed = createIceShield(serverLevel, context.getPlayer(), shieldPos, SHIELD_HEIGHT);
        }

        if (placed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        int placed = createIceBlock(serverLevel, target);
        if (placed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = player.blockPosition();
        freezeNearbyWater(serverLevel, center, FREEZE_RANGE);
        createIceShell(serverLevel, center, FREEZE_RANGE);
    }

    private static boolean hasAdjacentWater(ServerLevel level, BlockPos center) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(center.relative(direction)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static int tryApplyIceWorldConversion(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (isIceWorldDoor(current)) {
            return freezeDoor(level, pos, current);
        }
        if (current.hasBlockEntity()) {
            return 0;
        }

        BlockState replacement = iceWorldReplacement(current);
        if (replacement == null || replacement.getBlock() == current.getBlock()) {
            return 0;
        }

        level.setBlock(pos, replacement, 3);
        iceWorldFeedback(level, pos);
        return 1;
    }

    private static int freezeDoor(ServerLevel level, BlockPos pos, BlockState clickedState) {
        BlockPos lowerPos = clickedState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos upperPos = lowerPos.above();
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(upperPos);
        if (!isIceWorldDoor(lower) || !isIceWorldDoor(upper)) {
            return 0;
        }
        if (level.getBlockEntity(lowerPos) != null || level.getBlockEntity(upperPos) != null) {
            return 0;
        }

        BlockState iceLower = copyDoorProperties(lower, WitcheryBlocks.ICE_DOOR.get().defaultBlockState())
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState iceUpper = iceLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlock(lowerPos, iceLower, 3);
        level.setBlock(upperPos, iceUpper, 3);
        iceWorldFeedback(level, upperPos);
        return 2;
    }

    private static boolean isIceWorldDoor(BlockState state) {
        return state.is(Blocks.OAK_DOOR)
                || state.is(Blocks.SPRUCE_DOOR)
                || state.is(Blocks.BIRCH_DOOR)
                || state.is(Blocks.JUNGLE_DOOR)
                || state.is(Blocks.ACACIA_DOOR)
                || state.is(Blocks.DARK_OAK_DOOR)
                || state.is(Blocks.MANGROVE_DOOR)
                || state.is(Blocks.CHERRY_DOOR)
                || state.is(Blocks.BAMBOO_DOOR)
                || state.is(Blocks.CRIMSON_DOOR)
                || state.is(Blocks.WARPED_DOOR);
    }

    private static BlockState iceWorldReplacement(BlockState current) {
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
            return copyProperty(current, WitcheryBlocks.ICE_PRESSURE_PLATE.get().defaultBlockState(), BlockStateProperties.POWERED);
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
            copied = copied.setValue(SlabBlock.WATERLOGGED, false);
        } else {
            copied = copyProperty(from, copied, SlabBlock.WATERLOGGED);
        }
        return copied;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        if (!from.hasProperty(property) || !to.hasProperty(property)) {
            return to;
        }
        return to.setValue(property, from.getValue(property));
    }

    private static void iceWorldFeedback(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
        level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
    }

    private static BlockState copyStockadeProperties(BlockState from, BlockState to) {
        return to.setValue(StockadeBlock.NORTH, from.getValue(StockadeBlock.NORTH))
                .setValue(StockadeBlock.EAST, from.getValue(StockadeBlock.EAST))
                .setValue(StockadeBlock.SOUTH, from.getValue(StockadeBlock.SOUTH))
                .setValue(StockadeBlock.WEST, from.getValue(StockadeBlock.WEST))
                .setValue(StockadeBlock.WATERLOGGED, from.getValue(StockadeBlock.WATERLOGGED));
    }

    private static int freezeSurroundingWater(ServerLevel level, BlockPos origin, int range) {
        int frozen = 0;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!withinFreezeRange(origin, current, range)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.add(next)) {
                    continue;
                }
                if (level.getBlockState(next).is(Blocks.WATER)) {
                    level.setBlock(next, Blocks.ICE.defaultBlockState(), 3);
                    frozen++;
                    level.sendParticles(ParticleTypes.POOF,
                            next.getX() + 0.5D, next.getY() + 0.5D, next.getZ() + 0.5D,
                            3, 0.2D, 0.2D, 0.2D, 0.0D);
                    queue.add(next);
                }
            }
        }

        if (frozen > 0) {
            level.playSound(null, origin, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
        return frozen;
    }

    private static boolean withinFreezeRange(BlockPos origin, BlockPos pos, int range) {
        return Math.abs(pos.getX() - origin.getX()) <= range
                && Math.abs(pos.getY() - origin.getY()) <= range
                && Math.abs(pos.getZ() - origin.getZ()) <= range;
    }

    private static int createIceShield(ServerLevel level, Player player, BlockPos pos, int height) {
        BlockPos base = level.getBlockState(pos).isSolid() ? pos : pos.below();
        Direction side = player != null ? player.getDirection().getClockWise() : Direction.EAST;
        int placed = 0;
        placed += createIceColumn(level, base.above(), height);
        placed += createIceColumn(level, base.above().relative(side), height);
        placed += createIceColumn(level, base.above().relative(side.getOpposite()), height);
        if (placed > 0) {
            level.playSound(null, base, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F);
        }
        return placed;
    }

    private static int createIceColumn(ServerLevel level, BlockPos start, int height) {
        int placed = 0;
        for (int i = 0; i < height; i++) {
            if (setBlockIfNotSolid(level, start.above(i), Blocks.ICE.defaultBlockState())) {
                placed++;
            }
        }
        return placed;
    }

    private static int createIceBlock(ServerLevel level, LivingEntity target) {
        int x = (int)Math.round(target.getX());
        int y = Mth.floor(target.getY()) - 1;
        int z = (int)Math.round(target.getZ());
        BlockPos base = new BlockPos(x, y, z);

        if (isIceResistant(target)) {
            return setBlockIfNotSolid(level, base.above(), Blocks.FIRE.defaultBlockState()) ? 1 : 0;
        }

        int placed = 0;
        for (int i = 0; i < CAGE_HEIGHT; i++) {
            for (BlockPos offset : CAGE_RING_OFFSETS) {
                if (setBlockIfNotSolid(level, base.offset(offset).above(i), Blocks.ICE.defaultBlockState())) {
                    placed++;
                }
            }
        }
        if (setBlockIfNotSolid(level, base, Blocks.ICE.defaultBlockState())) {
            placed++;
        }
        for (BlockPos offset : CAGE_TOP_OFFSETS) {
            if (setBlockIfNotSolid(level, base.offset(offset).above(CAGE_HEIGHT - 1), Blocks.ICE.defaultBlockState())) {
                placed++;
            }
        }

        if (placed > 0) {
            level.playSound(null, base, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.9F, 0.6F);
        }
        return placed;
    }

    private static boolean isIceResistant(LivingEntity target) {
        return target instanceof DemonEntity
                || target instanceof Blaze
                || target instanceof EnderDragon
                || target instanceof HornedHuntsmanEntity
                || target instanceof EntEntity
                || target instanceof WitherBoss
                || target instanceof IronGolem;
    }

    private static boolean setBlockIfNotSolid(ServerLevel level, BlockPos pos, BlockState newState) {
        BlockState current = level.getBlockState(pos);
        if (current.isSolid() || current.hasBlockEntity()) {
            return false;
        }
        if (newState.is(Blocks.FIRE) && !newState.canSurvive(level, pos)) {
            return false;
        }

        level.setBlock(pos, newState, 3);
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                4, 0.2D, 0.2D, 0.2D, 0.01D);
        return true;
    }

    private static void freezeNearbyWater(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(Blocks.WATER)) {
                        level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void createIceShell(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > radius * radius || distSq < (radius - 1) * (radius - 1)) {
                        continue;
                    }
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy >= 0 && dy <= 1) {
                        continue;
                    }
                    setBlockIfNotSolid(level, center.offset(dx, dy, dz), Blocks.ICE.defaultBlockState());
                }
            }
        }
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
