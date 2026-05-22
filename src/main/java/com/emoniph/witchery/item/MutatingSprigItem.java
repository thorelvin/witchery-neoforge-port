package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.GrassperBlock;
import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Mutating Sprig - a controlled mutation tool for a few specific block changes.
 * This is intentionally bounded for now: the core block transformations are live,
 * while the remaining advanced mutation-structure family beyond the restored
 * chest/chest-plant/familiar bridges remains follow-up work.
 */
public class MutatingSprigItem extends Item {

    public MutatingSprigItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (tryCreateGrasspers(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateLeechChest(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateCritterSnare(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateFlyingMonkey(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateOwls(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateToads(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateParasyticLouse(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateBloodRoses(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateWildBramble(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateVoidBramble(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (tryCreateWormwood(level, pos, state)) {
            spawnEffects(level, pos);
            if (ctx.getPlayer() != null) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
            }
            return InteractionResult.CONSUME;
        }
        if (!tryMutate(level, pos, state)) {
            return InteractionResult.PASS;
        }

        spawnEffects(level, pos);
        if (ctx.getPlayer() != null) {
            EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), slot);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean tryMutate(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.MYCELIUM.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.MYCELIUM)) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            return true;
        }
        if (!hasWaterOrIceAbove(level, pos)) {
            return false;
        }
        if (state.is(Blocks.DIRT)) {
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.CLAY)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static boolean tryCreateGrasspers(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.CHEST)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                || !chest.isEmpty()
                || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(Blocks.SHORT_GRASS)) {
                return false;
            }
        }

        level.removeBlock(pos, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos grassPos = pos.relative(direction);
            level.setBlock(grassPos, WitcheryBlocks.GRASSPER.get().defaultBlockState().setValue(GrassperBlock.FACING, direction), 3);
        }
        return true;
    }

    private static boolean tryCreateLeechChest(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.TRAPPED_CHEST)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                || !chest.isEmpty()) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos vinePos = pos.relative(direction);
            if (!level.getBlockState(vinePos).is(Blocks.VINE)
                    || !level.getFluidState(vinePos.below()).is(FluidTags.WATER)) {
                return false;
            }
        }

        BlockState resultState = WitcheryBlocks.LEECH_CHEST.get().defaultBlockState()
                .setValue(com.emoniph.witchery.blocks.LeechChestBlock.FACING, state.getValue(ChestBlock.FACING));
        level.setBlock(pos, resultState, 3);
        return true;
    }

    private static boolean tryCreateCritterSnare(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.COBWEB) || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
            return false;
        }

        Zombie zombie = findSnaredZombie(level, pos);
        if (zombie == null) {
            return false;
        }

        List<ItemEntity> saplings = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            ItemEntity sapling = findDroppedAlderSapling(level, pos.relative(direction), saplings);
            if (sapling == null) {
                return false;
            }
            saplings.add(sapling);
        }

        for (ItemEntity sapling : saplings) {
            consumeOne(sapling);
        }
        zombie.discard();
        level.setBlock(pos, WitcheryBlocks.CRITTER_SNARE.get().defaultBlockState(), 3);
        return true;
    }

    private static boolean tryCreateFlyingMonkey(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(Blocks.COBWEB)
                || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)
                || countHorizontalMatureCocoa(level, pos) < 4) {
            return false;
        }
        if (countDiagonalGrasspersWith(level, pos, WitcheryItems.MUTANDIS_EXTREMIS.get().getDefaultInstance()) < 2
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.ATTUNED_STONE_CHARGED.get().getDefaultInstance()) < 1
                || countDiagonalGrasspersWith(level, pos, Items.POPPY.getDefaultInstance()) < 1) {
            return false;
        }

        Mob wolf = findNearbyCreature(level, pos, EntityType.WOLF);
        Mob owl = findNearbyCreature(level, pos, WitcheryEntities.OWL.get());
        if (wolf == null || owl == null) {
            return false;
        }

        var monkey = WitcheryEntities.FLYING_MONKEY.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (monkey == null) {
            return false;
        }

        level.removeBlock(pos, false);
        clearDiagonalGrasspers(level, pos);
        wolf.discard();
        owl.discard();
        monkey.snapTo(pos.getX() + 0.5D, pos.getY() + 0.001D, pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        monkey.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        monkey.setPersistenceRequired();
        serverLevel.addFreshEntity(monkey);
        return true;
    }

    private static boolean tryCreateOwls(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(Blocks.COBWEB)
                || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
            return false;
        }
        if (countAdjacentCritterSnares(level, pos, 1) < 2
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.MUTANDIS_EXTREMIS.get().getDefaultInstance()) < 3
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.ATTUNED_STONE_CHARGED.get().getDefaultInstance()) < 1) {
            return false;
        }

        Mob wolf = findNearbyCreature(level, pos, EntityType.WOLF);
        if (wolf == null) {
            return false;
        }

        if (spawnFamiliarsFromAdjacentSnares(serverLevel, pos, 1, WitcheryEntities.OWL.get()) <= 0) {
            return false;
        }

        level.removeBlock(pos, false);
        clearDiagonalGrasspers(level, pos);
        wolf.discard();
        return true;
    }

    private static boolean tryCreateToads(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(Blocks.COBWEB)
                || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
            return false;
        }
        if (countAdjacentCritterSnares(level, pos, 3) < 2
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.MUTANDIS_EXTREMIS.get().getDefaultInstance()) < 3
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.ATTUNED_STONE_CHARGED.get().getDefaultInstance()) < 1) {
            return false;
        }

        Mob ocelot = findNearbyCreature(level, pos, EntityType.OCELOT);
        if (ocelot == null) {
            return false;
        }

        if (spawnFamiliarsFromAdjacentSnares(serverLevel, pos, 3, WitcheryEntities.TOAD.get()) <= 0) {
            return false;
        }

        level.removeBlock(pos, false);
        clearDiagonalGrasspers(level, pos);
        ocelot.discard();
        return true;
    }

    private static boolean tryCreateParasyticLouse(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)
                || !state.is(WitcheryBlocks.CRITTER_SNARE.get())
                || !state.hasProperty(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT)
                || state.getValue(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT) != 2) {
            return false;
        }
        if (countHorizontalBlocks(level, pos, Blocks.VINE) < 2
                || countHorizontalWaterBelow(level, pos) < 1
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.MUTANDIS.get().getDefaultInstance()) < 2
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.ATTUNED_STONE_CHARGED.get().getDefaultInstance()) < 1
                || countDiagonalGrasspersWith(level, pos, WitcheryItems.TONGUE_OF_DOG.get().getDefaultInstance()) < 1) {
            return false;
        }

        var louse = WitcheryEntities.PARASYTIC_LOUSE.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (louse == null) {
            return false;
        }

        level.removeBlock(pos, false);
        clearDiagonalGrasspers(level, pos);
        louse.snapTo(pos.getX() + 0.5D, pos.getY() + 0.001D, pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        louse.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        louse.setPersistenceRequired();
        serverLevel.addFreshEntity(louse);
        return true;
    }

    private static boolean tryCreateBloodRoses(Level level, BlockPos pos, BlockState state) {
        if (!state.is(WitcheryBlocks.LEECH_CHEST.get())
                || !level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(Blocks.POPPY)) {
                return false;
            }
        }

        level.removeBlock(pos, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(pos.relative(direction), WitcheryBlocks.BLOOD_ROSE.get().defaultBlockState(), 3);
        }
        return true;
    }

    private static boolean tryCreateWildBramble(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.CACTUS) || !hasSpanishMossWaterRing(level, pos)) {
            return false;
        }
        if (countDiagonalGrasspersWith(level, pos, Items.BONE_MEAL.getDefaultInstance()) < 2
                || countDiagonalGrasspersWith(level, pos, Items.BLAZE_POWDER.getDefaultInstance()) < 2) {
            return false;
        }

        clearHorizontalMoss(level, pos);
        clearDiagonalGrasspers(level, pos);
        transformVerticalColumn(level, pos, Blocks.CACTUS, WitcheryBlocks.BRAMBLE.get(), 5);
        return true;
    }

    private static boolean tryCreateVoidBramble(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.SUGAR_CANE) || !hasSpanishMossWaterRing(level, pos)) {
            return false;
        }
        if (countDiagonalGrasspersWith(level, pos, Items.ENDER_PEARL.getDefaultInstance()) < 4) {
            return false;
        }

        clearHorizontalMoss(level, pos);
        clearDiagonalGrasspers(level, pos);
        transformVerticalColumn(level, pos, Blocks.SUGAR_CANE, WitcheryBlocks.VOID_BRAMBLE.get(), 16);
        return true;
    }

    private static boolean tryCreateWormwood(Level level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.WHEAT)
                || !state.hasProperty(BlockStateProperties.AGE_7)
                || state.getValue(BlockStateProperties.AGE_7) < 7) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(WitcheryBlocks.WISPY_COTTON.get())) {
                return false;
            }
        }
        for (int xOffset : new int[] {-1, 1}) {
            for (int zOffset : new int[] {-1, 1}) {
                if (!level.getFluidState(pos.offset(xOffset, -1, zOffset)).isSourceOfType(Fluids.WATER)) {
                    return false;
                }
            }
        }

        clearHorizontalMoss(level, pos);
        level.setBlock(pos, WitcheryBlocks.WORMWOOD_PLANT.get().defaultBlockState(), 3);
        return true;
    }

    private static Zombie findSnaredZombie(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Zombie.class, new AABB(pos), zombie -> zombie.isAlive() && !zombie.isRemoved())
                .stream()
                .min(Comparator.comparingDouble(zombie -> zombie.distanceToSqr(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5)))
                .orElse(null);
    }

    private static Mob findNearbyCreature(Level level, BlockPos pos, EntityType<?> entityType) {
        return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(1.0, 1.0, 1.0),
                        mob -> mob.isAlive() && !mob.isRemoved() && mob.getType() == entityType)
                .stream()
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5)))
                .orElse(null);
    }

    private static ItemEntity findDroppedAlderSapling(Level level, BlockPos pos, List<ItemEntity> excluded) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.35, 0.25, 0.35), itemEntity ->
                        itemEntity.isAlive()
                                && !itemEntity.isRemoved()
                                && !excluded.contains(itemEntity)
                                && itemEntity.getItem().is(WitcheryItems.ALDER_SAPLING.get()))
                .stream()
                .min(Comparator.comparingDouble(itemEntity -> itemEntity.distanceToSqr(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5)))
                .orElse(null);
    }

    private static void consumeOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
            return;
        }
        entity.setItem(stack);
    }

    private static boolean hasSpanishMossWaterRing(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos mossPos = pos.relative(direction);
            if (!level.getBlockState(mossPos).is(WitcheryBlocks.SPANISH_MOSS.get())
                    || !level.getFluidState(mossPos.below()).isSourceOfType(Fluids.WATER)) {
                return false;
            }
        }
        return true;
    }

    private static int countDiagonalGrasspersWith(Level level, BlockPos pos, ItemStack expected) {
        int count = 0;
        for (int xOffset : new int[] {-1, 1}) {
            for (int zOffset : new int[] {-1, 1}) {
                BlockPos grassperPos = pos.offset(xOffset, 0, zOffset);
                if (!(level.getBlockEntity(grassperPos) instanceof GrassperBlockEntity grassper)
                        || !level.getBlockState(grassperPos).is(WitcheryBlocks.GRASSPER.get())) {
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(grassper.getItem(), expected)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countAdjacentCritterSnares(Level level, BlockPos pos, int caughtType) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState adjacentState = level.getBlockState(pos.relative(direction));
            if (adjacentState.is(WitcheryBlocks.CRITTER_SNARE.get())
                    && adjacentState.hasProperty(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT)
                    && adjacentState.getValue(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT) == caughtType) {
                count++;
            }
        }
        return count;
    }

    private static int countHorizontalBlocks(Level level, BlockPos pos, Block block) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(block)) {
                count++;
            }
        }
        return count;
    }

    private static int countHorizontalMatureCocoa(Level level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(Blocks.COCOA)
                    && state.hasProperty(CocoaBlock.AGE)
                    && state.getValue(CocoaBlock.AGE) >= 2) {
                count++;
            }
        }
        return count;
    }

    private static int countHorizontalWaterBelow(Level level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction).below()).isSourceOfType(Fluids.WATER)) {
                count++;
            }
        }
        return count;
    }

    private static int spawnFamiliarsFromAdjacentSnares(ServerLevel level, BlockPos pos, int caughtType,
                                                        EntityType<? extends Mob> familiarType) {
        int spawned = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos snarePos = pos.relative(direction);
            BlockState snareState = level.getBlockState(snarePos);
            if (!snareState.is(WitcheryBlocks.CRITTER_SNARE.get())
                    || !snareState.hasProperty(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT)
                    || snareState.getValue(com.emoniph.witchery.blocks.CritterSnareBlock.CAUGHT) != caughtType) {
                continue;
            }

            Mob familiar = familiarType.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (familiar == null) {
                continue;
            }

            level.removeBlock(snarePos, false);
            familiar.snapTo(snarePos.getX() + 0.5, snarePos.getY() + 0.05, snarePos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            familiar.finalizeSpawn(level, level.getCurrentDifficultyAt(snarePos),
                    net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
            familiar.setPersistenceRequired();
            if (familiar instanceof com.emoniph.witchery.entity.familiar.FamiliarEntity familiarEntity) {
                familiarEntity.setOrderedToSit(false);
            }
            level.addFreshEntity(familiar);
            spawned++;
        }
        return spawned;
    }

    private static void clearHorizontalMoss(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.removeBlock(pos.relative(direction), false);
        }
    }

    private static void clearDiagonalGrasspers(Level level, BlockPos pos) {
        for (int xOffset : new int[] {-1, 1}) {
            for (int zOffset : new int[] {-1, 1}) {
                BlockPos grassperPos = pos.offset(xOffset, 0, zOffset);
                if (level.getBlockEntity(grassperPos) instanceof GrassperBlockEntity grassper) {
                    grassper.takeItem();
                }
            }
        }
    }

    private static void transformVerticalColumn(Level level, BlockPos pos, Block original, Block replacement, int maxHeight) {
        for (int offset = maxHeight - 1; offset >= 0; offset--) {
            BlockPos cursor = pos.above(offset);
            if (level.getBlockState(cursor).is(original)) {
                level.setBlock(cursor, replacement.defaultBlockState(), 3);
            }
        }
    }

    private static boolean hasWaterOrIceAbove(Level level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        return aboveState.is(Blocks.WATER) || aboveState.is(Blocks.ICE);
    }

    private static void spawnEffects(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS,
                0.6F, 0.5F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    8,
                    0.3,
                    0.3,
                    0.3,
                    0.1);
        }
    }
}
