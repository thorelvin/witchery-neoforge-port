package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Base for solidifying brews - converts connected Hollow Tears into the target block. */
public abstract class BrewOfSolidifyingItem extends BrewItem {
    private static final int MAX_SPREAD_DISTANCE = 64;
    private static final int MAX_SPREAD_BLOCKS = 4096;

    private final Block replacementBlock;

    protected BrewOfSolidifyingItem(Properties p, Block replacementBlock) {
        super(p);
        this.replacementBlock = replacementBlock;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos start = findHollowTears(level, context.getClickedPos(), context.getClickedFace());
        if (start == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int changed = solidify(serverLevel, start, player);
        if (changed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        // Direct block use is the practical port of the source thrown-brew behavior.
    }

    private int solidify(ServerLevel level, BlockPos start, Player player) {
        Deque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(start.immutable());

        int changed = 0;
        while (!pending.isEmpty() && changed < MAX_SPREAD_BLOCKS) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos) || !inSourceRange(start, pos) || !isHollowTears(level, pos)) {
                continue;
            }

            if (!replaceHollowTears(level, pos, player)) {
                continue;
            }
            changed++;

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction).immutable();
                if (!visited.contains(next) && inSourceRange(start, next)) {
                    pending.addLast(next);
                }
            }
        }

        if (changed > 0) {
            level.playSound(null, start, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return changed;
    }

    private boolean replaceHollowTears(ServerLevel level, BlockPos pos, Player player) {
        if (replacementBlock == null) {
            if (!WitcheryTerrainProtection.canBreak(player, level, pos)) {
                return false;
            }
            solidifyingParticles(level, pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (canBreakWithoutDrops(level, below, belowState)) {
                if (!WitcheryTerrainProtection.canBreak(player, level, below)) {
                    return true;
                }
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return true;
        }

        if (!WitcheryTerrainProtection.canTransform(player, level, pos)) {
            return false;
        }
        solidifyingParticles(level, pos);
        level.setBlock(pos, replacementBlock.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private static void solidifyingParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                16, 2.0D, 2.0D, 2.0D, 0.0D);
    }

    private static BlockPos findHollowTears(Level level, BlockPos clickedPos, Direction face) {
        if (isHollowTears(level, clickedPos)) {
            return clickedPos;
        }

        BlockPos adjacent = clickedPos.relative(face);
        return isHollowTears(level, adjacent) ? adjacent : null;
    }

    private static boolean isHollowTears(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(WitcheryBlocks.HOLLOW_TEARS.get());
    }

    private static boolean inSourceRange(BlockPos origin, BlockPos pos) {
        return Math.abs(origin.getX() - pos.getX()) < MAX_SPREAD_DISTANCE
                && Math.abs(origin.getY() - pos.getY()) < MAX_SPREAD_DISTANCE
                && Math.abs(origin.getZ() - pos.getZ()) < MAX_SPREAD_DISTANCE;
    }

    private static boolean canBreakWithoutDrops(Level level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !state.liquid()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F;
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

    public static final class Stone extends BrewOfSolidifyingItem {
        public Stone(Properties p) {
            super(p, Blocks.STONE);
        }
    }

    public static final class Dirt extends BrewOfSolidifyingItem {
        public Dirt(Properties p) {
            super(p, Blocks.DIRT);
        }
    }

    public static final class Sand extends BrewOfSolidifyingItem {
        public Sand(Properties p) {
            super(p, Blocks.SAND);
        }
    }

    public static final class Sandstone extends BrewOfSolidifyingItem {
        public Sandstone(Properties p) {
            super(p, Blocks.SANDSTONE);
        }
    }

    public static final class Erosion extends BrewOfSolidifyingItem {
        public Erosion(Properties p) {
            super(p, null);
        }
    }
}
