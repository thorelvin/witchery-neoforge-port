package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Mutandis — right-click a natural plant or sapling to randomly transform it into
 * another plant from the same pool.
 *
 * Mutandis Extremis additionally:
 *  - Can transform crops (age is preserved on the new crop).
 *  - Grass Block ↔ Dirt toggle (50 % chance flip on each use).
 *  - Sand with water or ice directly above → clay patch conversion.
 */
public class MutandisItem extends Item {

    private final boolean extremis;

    public MutandisItem(boolean extremis, Properties props) {
        super(props);
        this.extremis = extremis;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);
        BlockPos mutationPos = normalizeMutationPos(clickedState, pos);
        BlockState state = level.getBlockState(mutationPos);
        Block block    = state.getBlock();

        // ── Extremis special: Grass Block ↔ Dirt toggle ───────────────────────
        if (extremis && (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT)) {
            Block swap = (block == Blocks.GRASS_BLOCK) ? Blocks.DIRT : Blocks.GRASS_BLOCK;
            if (level.random.nextBoolean()) {
                level.setBlock(pos, swap.defaultBlockState(), 3);
            }
            spawnEffects(level, pos.above());
            consumeItem(ctx);
            return InteractionResult.SUCCESS;
        }

        // ── Extremis special: Sand + water/ice above → clay patch ──────────────
        if (extremis && block == Blocks.SAND) {
            Block above = level.getBlockState(pos.above()).getBlock();
            if (above == Blocks.WATER || above == Blocks.ICE) {
                if (level.random.nextBoolean()) {
                    tryConvertToClay(level, pos);
                    tryConvertToClay(level, pos.north());
                    tryConvertToClay(level, pos.south());
                    tryConvertToClay(level, pos.east());
                    tryConvertToClay(level, pos.west());
                }
                spawnEffects(level, pos.above());
                consumeItem(ctx);
                return InteractionResult.SUCCESS;
            }
        }

        // ── Normal pool mutation ───────────────────────────────────────────────
        List<BlockState> pool = buildPool(state, level);
        boolean inPool = pool.stream().anyMatch(s -> s.getBlock() == block);
        if (!inPool) return InteractionResult.PASS;

        pool.removeIf(s -> s.getBlock() == block || !canPlaceMutation(level, mutationPos, state, s));
        if (pool.isEmpty()) return InteractionResult.PASS;

        int sourceAge = getAge(state);
        BlockState chosen = pool.get(level.random.nextInt(pool.size()));
        chosen = applyAge(chosen, sourceAge);

        placeMutation(level, mutationPos, state, chosen);
        spawnEffects(level, mutationPos);
        consumeItem(ctx);
        return InteractionResult.SUCCESS;
    }

    // ── Pool construction ─────────────────────────────────────────────────────

    private List<BlockState> buildPool(BlockState state, Level level) {
        List<BlockState> pool = new ArrayList<>();

        // Witchery plants + saplings
        pool.add(WitcheryBlocks.ROWAN_SAPLING.get().defaultBlockState());
        pool.add(WitcheryBlocks.BLOOD_ROSE.get().defaultBlockState());
        pool.add(WitcheryBlocks.EMBER_MOSS.get().defaultBlockState());
        pool.add(WitcheryBlocks.GLINT_WEED.get().defaultBlockState());
        pool.add(WitcheryBlocks.SPANISH_MOSS.get().defaultBlockState());
        pool.add(WitcheryBlocks.BRAMBLE.get().defaultBlockState());

        // Witchery crops (age applied from source at use-time)
        pool.add(WitcheryBlocks.BELLADONNA.get().defaultBlockState());
        pool.add(WitcheryBlocks.MANDRAKE.get().defaultBlockState());
        pool.add(WitcheryBlocks.WORMWOOD_PLANT.get().defaultBlockState());
        pool.add(WitcheryBlocks.SNOWBELL.get().defaultBlockState());
        pool.add(WitcheryBlocks.ARTICHOKE.get().defaultBlockState());

        // Vanilla saplings
        pool.add(Blocks.OAK_SAPLING.defaultBlockState());
        pool.add(Blocks.SPRUCE_SAPLING.defaultBlockState());
        pool.add(Blocks.BIRCH_SAPLING.defaultBlockState());
        pool.add(Blocks.JUNGLE_SAPLING.defaultBlockState());
        pool.add(Blocks.ACACIA_SAPLING.defaultBlockState());
        pool.add(Blocks.DARK_OAK_SAPLING.defaultBlockState());

        // Vanilla plants
        pool.add(Blocks.DANDELION.defaultBlockState());
        pool.add(Blocks.POPPY.defaultBlockState());
        pool.add(Blocks.ROSE_BUSH.defaultBlockState());
        pool.add(Blocks.FERN.defaultBlockState());
        pool.add(Blocks.DEAD_BUSH.defaultBlockState());
        pool.add(Blocks.SHORT_GRASS.defaultBlockState());

        // Spirit World: nether wart can appear from normal Mutandis in that dim
        if (level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            pool.add(Blocks.NETHER_WART.defaultBlockState());
        }

        // Extremis: vanilla crops + nether wart
        if (extremis) {
            pool.add(Blocks.WHEAT.defaultBlockState());
            pool.add(Blocks.CARROTS.defaultBlockState());
            pool.add(Blocks.POTATOES.defaultBlockState());
            pool.add(Blocks.BEETROOTS.defaultBlockState());
            pool.add(Blocks.SUGAR_CANE.defaultBlockState());
            pool.add(Blocks.MELON_STEM.defaultBlockState());
            pool.add(Blocks.PUMPKIN_STEM.defaultBlockState());
            pool.add(Blocks.NETHER_WART.defaultBlockState());
        }

        return pool;
    }

    // ── Age helpers ───────────────────────────────────────────────────────────

    private static int getAge(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty ip) {
                return state.getValue(ip);
            }
        }
        return 0;
    }

    private static BlockState applyAge(BlockState state, int age) {
        if (age <= 0) return state;
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty ip) {
                int max = Collections.max(ip.getPossibleValues());
                return state.setValue(ip, Math.min(age, max));
            }
        }
        return state;
    }

    private static BlockPos normalizeMutationPos(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof DoublePlantBlock
                && state.hasProperty(DoublePlantBlock.HALF)
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static boolean canPlaceMutation(Level level, BlockPos pos, BlockState currentState, BlockState targetState) {
        if (targetState.getBlock() instanceof DoublePlantBlock) {
            if (!level.isInWorldBounds(pos.above())) {
                return false;
            }
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (isUpperHalfOf(aboveState, currentState.getBlock())) {
                return true;
            }
            return aboveState.canBeReplaced();
        }
        return true;
    }

    private static void placeMutation(Level level, BlockPos pos, BlockState currentState, BlockState targetState) {
        if (!(targetState.getBlock() instanceof DoublePlantBlock) && currentState.getBlock() instanceof DoublePlantBlock) {
            clearUpperHalf(level, pos, currentState.getBlock());
        }
        if (targetState.getBlock() instanceof DoublePlantBlock) {
            DoublePlantBlock.placeAt(level, targetState, pos, 3);
            return;
        }
        level.setBlock(pos, targetState, 3);
    }

    private static void clearUpperHalf(Level level, BlockPos pos, Block block) {
        BlockPos abovePos = pos.above();
        if (isUpperHalfOf(level.getBlockState(abovePos), block)) {
            level.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean isUpperHalfOf(BlockState state, Block block) {
        return state.is(block)
                && state.hasProperty(DoublePlantBlock.HALF)
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    // ── Extremis clay conversion ──────────────────────────────────────────────

    private static void tryConvertToClay(Level level, BlockPos pos) {
        BlockState bs = level.getBlockState(pos);
        if (bs.getBlock() != Blocks.SAND) return;
        Block above = level.getBlockState(pos.above()).getBlock();
        if (above == Blocks.WATER || above == Blocks.ICE) {
            level.setBlock(pos, Blocks.CLAY.defaultBlockState(), 3);
        }
    }

    // ── Effects ───────────────────────────────────────────────────────────────

    private static void spawnEffects(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS,
                0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private static void consumeItem(UseOnContext ctx) {
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) {
            ctx.getItemInHand().shrink(1);
        }
    }
}
