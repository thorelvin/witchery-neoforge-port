package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Base for solidifying brews — converts certain blocks in a radius to the target block.
 * Subclasses define which blocks to replace and what to replace them with.
 */
public abstract class BrewOfSolidifyingItem extends BrewItem {
    private final Block targetBlock;
    private final int radius;
    private final Predicate<BlockState> convertable;

    protected BrewOfSolidifyingItem(Properties p, Block target, int radius,
                                     Predicate<BlockState> convertable) {
        super(p);
        this.targetBlock = target;
        this.radius = radius;
        this.convertable = convertable;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        BlockState newState = targetBlock.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx*dx + dy*dy + dz*dz > radius*radius) continue;
                    BlockPos bp = center.offset(dx, dy, dz);
                    if (convertable.test(level.getBlockState(bp))) {
                        level.setBlockAndUpdate(bp, newState);
                    }
                }
            }
        }
    }

    // ── Concrete variants ─────────────────────────────────────────────────────

    public static final class Stone extends BrewOfSolidifyingItem {
        public Stone(Properties p) {
            super(p, Blocks.STONE, 5, s ->
                    s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.GRAVEL) || s.is(Blocks.SAND));
        }
    }

    public static final class Dirt extends BrewOfSolidifyingItem {
        public Dirt(Properties p) {
            super(p, Blocks.DIRT, 5, s ->
                    s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.SAND) || s.is(Blocks.GRAVEL));
        }
    }

    public static final class Sand extends BrewOfSolidifyingItem {
        public Sand(Properties p) {
            super(p, Blocks.SAND, 5, s ->
                    s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.STONE) || s.is(Blocks.GRAVEL));
        }
    }

    public static final class Sandstone extends BrewOfSolidifyingItem {
        public Sandstone(Properties p) {
            super(p, Blocks.SANDSTONE, 5, s ->
                    s.is(Blocks.SAND) || s.is(Blocks.DIRT) || s.is(Blocks.GRAVEL) || s.is(Blocks.STONE));
        }
    }
}
