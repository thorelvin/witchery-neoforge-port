package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Brew of Sprouting - source-style branch growth in practical item form. */
public class BrewOfSproutingItem extends BrewItem {
    private static final int NORMAL_EXTENT = 15;
    private static final int ENHANCED_EXTENT = 20;

    private static final BranchBlocks OAK_BRANCH = new BranchBlocks(Blocks.OAK_LOG, Blocks.OAK_LEAVES);
    private static final BranchBlocks SPRUCE_BRANCH = new BranchBlocks(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES);
    private static final BranchBlocks BIRCH_BRANCH = new BranchBlocks(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);
    private static final BranchBlocks JUNGLE_BRANCH = new BranchBlocks(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES);
    private static final BranchBlocks ROWAN_BRANCH = new BranchBlocks(WitcheryBlocks.ROWAN_LOG.get(), WitcheryBlocks.ROWAN_LEAVES.get());
    private static final BranchBlocks ALDER_BRANCH = new BranchBlocks(WitcheryBlocks.ALDER_LOG.get(), WitcheryBlocks.ALDER_LEAVES.get());
    private static final BranchBlocks HAWTHORN_BRANCH = new BranchBlocks(WitcheryBlocks.HAWTHORN_LOG.get(), WitcheryBlocks.HAWTHORN_LEAVES.get());
    private static final BranchBlocks[] VANILLA_BRANCHES = { OAK_BRANCH, SPRUCE_BRANCH, BIRCH_BRANCH, JUNGLE_BRANCH };
    private static final BranchBlocks[] WITCHERY_BRANCHES = { ROWAN_BRANCH, ALDER_BRANCH, HAWTHORN_BRANCH };

    public BrewOfSproutingItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int placed = growBranch(serverLevel, context.getClickedPos(), context.getClickedFace(),
                hasBrewMastery(player) ? ENHANCED_EXTENT : NORMAL_EXTENT);
        if (placed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Direction facing = player.getDirection();
        BlockPos surface = player.blockPosition();
        for (int i = 0; i < 4 && !serverLevel.getBlockState(surface).isSolid(); i++) {
            surface = surface.relative(facing.getOpposite());
        }

        growBranch(serverLevel, surface, facing, hasBrewMastery(player) ? ENHANCED_EXTENT : NORMAL_EXTENT);
    }

    private static boolean hasBrewMastery(Player player) {
        return player != null && FamiliarHelper.hasActiveToadFamiliar(player);
    }

    public static int growBranch(ServerLevel level, BlockPos clickedPos, Direction face, int extent) {
        return growBranch(level, clickedPos, face, extent, true);
    }

    public static int growBranch(ServerLevel level, BlockPos clickedPos, Direction face, int extent, boolean feedback) {
        BlockState clickedState = level.getBlockState(clickedPos);
        BranchBlocks blocks = chooseBranchBlocks(clickedState, level.random);
        BlockState log = orientLog(blocks.log().defaultBlockState(), face);
        BlockState leaves = blocks.leaves().defaultBlockState();
        boolean initialSolid = clickedState.isSolid();

        int placed = 0;
        int firstOffset = face == Direction.UP && !initialSolid ? 0 : 1;
        for (int i = firstOffset; i < extent; i++) {
            BlockPos pos = clickedPos.relative(face, i);
            if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) {
                break;
            }
            if (!setBranchBlock(level, pos, log, feedback)) {
                break;
            }

            placed++;
            placeRandomLeaf(level, pos, face, leaves, feedback);
        }

        if (placed > 0 && feedback) {
            level.playSound(null, clickedPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        if (placed > 0) {
            if (face == Direction.UP) {
                moveNearbyEntitiesToTip(level, clickedPos, placed + 1);
            }
        }
        return placed;
    }

    private static BlockState orientLog(BlockState state, Direction face) {
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.setValue(RotatedPillarBlock.AXIS, face.getAxis());
        }
        return state;
    }

    private static void placeRandomLeaf(ServerLevel level, BlockPos pos, Direction face, BlockState leaves, boolean feedback) {
        RandomSource random = level.random;
        int lx = face.getAxis() != Direction.Axis.X && random.nextInt(4) == 0 ? random.nextInt(3) - 1 : 0;
        int ly = face.getAxis() != Direction.Axis.Y && lx == 0 && random.nextInt(4) == 0 ? random.nextInt(3) - 1 : 0;
        int lz = face.getAxis() != Direction.Axis.Z && lx == 0 && ly == 0 && random.nextInt(4) == 0 ? random.nextInt(3) - 1 : 0;
        if (lx != 0 || ly != 0 || lz != 0) {
            setBranchBlock(level, pos.offset(lx, ly, lz), leaves, feedback);
        }
    }

    private static boolean setBranchBlock(ServerLevel level, BlockPos pos, BlockState state, boolean feedback) {
        BlockState current = level.getBlockState(pos);
        if (current.isSolid() || current.hasBlockEntity()) {
            return false;
        }

        level.setBlock(pos, state, 3);
        if (feedback) {
            level.sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    4, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        return true;
    }

    private static void moveNearbyEntitiesToTip(ServerLevel level, BlockPos clickedPos, int offset) {
        BlockPos landing = clickedPos.above(offset);
        if (landing.getY() >= level.getMaxY() - 2
                || level.getBlockState(landing.above()).isSolid()
                || level.getBlockState(landing.above(2)).isSolid()) {
            return;
        }

        AABB area = new AABB(clickedPos).inflate(0.5D, 2.0D, 0.5D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.teleportTo(landing.getX() + 0.5D, landing.getY() + 1.0D, landing.getZ() + 0.5D);
        }
    }

    private static BranchBlocks chooseBranchBlocks(BlockState clickedState, RandomSource random) {
        BranchBlocks vanilla = vanillaBranchBlocks(clickedState);
        if (vanilla != null) {
            return vanilla;
        }

        BranchBlocks witchery = witcheryBranchBlocks(clickedState);
        if (witchery != null) {
            return witchery;
        }

        if (random.nextBoolean()) {
            return VANILLA_BRANCHES[random.nextInt(VANILLA_BRANCHES.length)];
        }
        return WITCHERY_BRANCHES[random.nextInt(WITCHERY_BRANCHES.length)];
    }

    private static BranchBlocks vanillaBranchBlocks(BlockState state) {
        if (state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_WOOD) || state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.OAK_SAPLING)) {
            return OAK_BRANCH;
        }
        if (state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.SPRUCE_WOOD) || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.SPRUCE_SAPLING)) {
            return SPRUCE_BRANCH;
        }
        if (state.is(Blocks.BIRCH_LOG) || state.is(Blocks.BIRCH_WOOD) || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.BIRCH_SAPLING)) {
            return BIRCH_BRANCH;
        }
        if (state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.JUNGLE_WOOD) || state.is(Blocks.JUNGLE_PLANKS)
                || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.JUNGLE_SAPLING)) {
            return JUNGLE_BRANCH;
        }
        return null;
    }

    private static BranchBlocks witcheryBranchBlocks(BlockState state) {
        if (state.is(WitcheryBlocks.ROWAN_LOG.get()) || state.is(WitcheryBlocks.ROWAN_WOOD.get())
                || state.is(WitcheryBlocks.ROWAN_PLANKS.get()) || state.is(WitcheryBlocks.ROWAN_LEAVES.get())
                || state.is(WitcheryBlocks.ROWAN_SAPLING.get())) {
            return ROWAN_BRANCH;
        }
        if (state.is(WitcheryBlocks.ALDER_LOG.get()) || state.is(WitcheryBlocks.ALDER_PLANKS.get())
                || state.is(WitcheryBlocks.ALDER_LEAVES.get()) || state.is(WitcheryBlocks.ALDER_SAPLING.get())) {
            return ALDER_BRANCH;
        }
        if (state.is(WitcheryBlocks.HAWTHORN_LOG.get()) || state.is(WitcheryBlocks.HAWTHORN_PLANKS.get())
                || state.is(WitcheryBlocks.HAWTHORN_LEAVES.get()) || state.is(WitcheryBlocks.HAWTHORN_SAPLING.get())) {
            return HAWTHORN_BRANCH;
        }
        return null;
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

    private record BranchBlocks(Block log, Block leaves) {}
}
