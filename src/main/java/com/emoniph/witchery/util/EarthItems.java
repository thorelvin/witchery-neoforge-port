package com.emoniph.witchery.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Port of source {@code com.emoniph.witchery.util.EarthItems} — the metal-gear and ore mapping the
 * Overworld infusion's earth-metal powers act on. The metal-gear set mirrors source (iron / golden /
 * chainmail tools and armour + iron/gold ingots and gold nugget). <b>Modern divergence:</b> 1.7.10 used
 * the OreDictionary for {@code oreToIngot}; with no OreDictionary in 1.21 the port uses an explicit
 * Block→ingot map (vanilla iron/gold/copper ores).
 */
public final class EarthItems {

    private EarthItems() {}

    private static final Set<Item> METAL = Set.of(
            Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE,
            Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_NUGGET);

    private static final Map<Block, Item> ORE_TO_INGOT = Map.ofEntries(
            Map.entry(Blocks.IRON_ORE, Items.IRON_INGOT),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, Items.IRON_INGOT),
            Map.entry(Blocks.GOLD_ORE, Items.GOLD_INGOT),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Items.GOLD_INGOT),
            Map.entry(Blocks.NETHER_GOLD_ORE, Items.GOLD_INGOT),
            Map.entry(Blocks.COPPER_ORE, Items.COPPER_INGOT),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Items.COPPER_INGOT));

    private static final Set<Block> THROWABLE_ROCK = Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DIRT, Blocks.COARSE_DIRT,
            Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL,
            Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.NETHERRACK, Blocks.STONE_SLAB, Blocks.SMOOTH_STONE, Blocks.ANDESITE, Blocks.DIORITE,
            Blocks.GRANITE);

    /** Source {@code isMatch}: an iron/golden/chainmail tool, armour piece, or metal ingot/nugget. */
    public static boolean isMetal(ItemStack stack) {
        return !stack.isEmpty() && METAL.contains(stack.getItem());
    }

    /** Source {@code oreToIngot}: the smelted ingot a vanilla ore yields, or null if not an ore. */
    @Nullable
    public static Item oreToIngot(BlockState state) {
        return ORE_TO_INGOT.get(state.getBlock());
    }

    /** Source {@code isThrowableRock} block list (the earthen blocks the Overworld witch can hurl). */
    public static boolean isThrowableRock(BlockState state) {
        return THROWABLE_ROCK.contains(state.getBlock());
    }
}
