package com.emoniph.witchery.worldgen;

import java.util.Map;

/** Immutable provenance and cleaned-template signatures for the three designer-authored wells. */
public final class WitchWellSourceContract {

    private static final Map<WitchWellPiece.Variant, Contract> CONTRACTS = Map.of(
            WitchWellPiece.Variant.REGULAR, new Contract(
                    15, 15, 14, 3_150, 138, 3, 1, 9_360,
                    "E35EBA38E8E9E56E80F31E985351A67BEF3D0BB2877C7337AA0A12059144FA89",
                    Map.ofEntries(
                            Map.entry("minecraft:chiseled_stone_bricks", 39),
                            Map.entry("minecraft:cobblestone_wall", 16),
                            Map.entry("minecraft:cobweb", 3),
                            Map.entry("minecraft:dispenser", 1),
                            Map.entry("minecraft:lantern", 4),
                            Map.entry("minecraft:mossy_cobblestone_wall", 9),
                            Map.entry("minecraft:mossy_stone_brick_slab", 10),
                            Map.entry("minecraft:mossy_stone_brick_stairs", 5),
                            Map.entry("minecraft:mossy_stone_brick_wall", 13),
                            Map.entry("minecraft:spawner", 2),
                            Map.entry("minecraft:stone_brick_slab", 9),
                            Map.entry("minecraft:stone_brick_stairs", 5),
                            Map.entry("minecraft:stone_brick_wall", 19),
                            Map.entry("minecraft:stone_bricks", 2),
                            Map.entry("minecraft:water", 1))),
            WitchWellPiece.Variant.SMALL, new Contract(
                    10, 8, 10, 800, 42, 0, 1, 2_967,
                    "B3202B2A6B087AED224EAB317A2A45DCBF5F7FDAA46D76F39198F329788B4584",
                    Map.ofEntries(
                            Map.entry("minecraft:cauldron", 1),
                            Map.entry("minecraft:iron_chain", 3),
                            Map.entry("minecraft:lantern", 1),
                            Map.entry("minecraft:mangrove_fence", 5),
                            Map.entry("minecraft:mangrove_slab", 11),
                            Map.entry("minecraft:mangrove_stairs", 4),
                            Map.entry("minecraft:mangrove_trapdoor", 6),
                            Map.entry("minecraft:mossy_stone_brick_slab", 2),
                            Map.entry("minecraft:stone_brick_stairs", 4),
                            Map.entry("minecraft:stone_brick_wall", 2),
                            Map.entry("minecraft:stone_bricks", 3))),
            WitchWellPiece.Variant.CAVE, new Contract(
                    30, 20, 30, 18_000, 496, 19, 7, 54_549,
                    "B802780182A0F5541FF61748959A190EAD20C641535E00DC0C7A15A06ED89C28",
                    Map.ofEntries(
                            Map.entry("minecraft:black_stained_glass_pane", 3),
                            Map.entry("minecraft:chest", 1),
                            Map.entry("minecraft:chiseled_deepslate", 67),
                            Map.entry("minecraft:chiseled_stone_bricks", 73),
                            Map.entry("minecraft:cobblestone_wall", 16),
                            Map.entry("minecraft:cobweb", 3),
                            Map.entry("minecraft:cracked_deepslate_bricks", 27),
                            Map.entry("minecraft:cracked_deepslate_tiles", 28),
                            Map.entry("minecraft:cracked_stone_bricks", 45),
                            Map.entry("minecraft:deepslate_tile_slab", 18),
                            Map.entry("minecraft:deepslate_tile_stairs", 6),
                            Map.entry("minecraft:deepslate_tile_wall", 10),
                            Map.entry("minecraft:gilded_blackstone", 23),
                            Map.entry("minecraft:ladder", 3),
                            Map.entry("minecraft:lantern", 4),
                            Map.entry("minecraft:lever", 2),
                            Map.entry("minecraft:mossy_cobblestone_wall", 9),
                            Map.entry("minecraft:mossy_stone_brick_slab", 10),
                            Map.entry("minecraft:mossy_stone_brick_stairs", 5),
                            Map.entry("minecraft:mossy_stone_brick_wall", 13),
                            Map.entry("minecraft:player_head", 1),
                            Map.entry("minecraft:redstone_lamp", 2),
                            Map.entry("minecraft:skeleton_skull", 2),
                            Map.entry("minecraft:soul_campfire", 4),
                            Map.entry("minecraft:spawner", 2),
                            Map.entry("minecraft:spruce_trapdoor", 1),
                            Map.entry("minecraft:stone_brick_slab", 10),
                            Map.entry("minecraft:stone_brick_stairs", 8),
                            Map.entry("minecraft:stone_brick_wall", 25),
                            Map.entry("minecraft:stone_bricks", 37),
                            Map.entry("minecraft:water", 3),
                            Map.entry("witchery:brazier", 2),
                            Map.entry("witchery:chalice", 1),
                            Map.entry("witchery:circle_infernal", 4),
                            Map.entry("witchery:circle_otherwhere", 4),
                            Map.entry("witchery:coffin", 2),
                            Map.entry("witchery:fetish_witchs_ladder", 3),
                            Map.entry("witchery:flowing_spirit", 3),
                            Map.entry("witchery:hollow_tears", 3),
                            Map.entry("witchery:mirror_wall", 3),
                            Map.entry("witchery:spanish_moss", 9),
                            Map.entry("witchery:statue_goddess", 1))));

    private WitchWellSourceContract() {}

    public static Contract forVariant(WitchWellPiece.Variant variant) {
        return CONTRACTS.get(variant);
    }

    public record Contract(int sizeX, int sizeY, int sizeZ, int sourceBlockCount,
                           int cleanedBlockCount, int blockEntityCount, int fluidCells,
                           int sourceBytes, String sourceSha256, Map<String, Integer> blockCounts) {
        public Contract {
            blockCounts = Map.copyOf(blockCounts);
            int total = blockCounts.values().stream().mapToInt(Integer::intValue).sum();
            if (total != cleanedBlockCount) {
                throw new IllegalArgumentException("Witch Well signature totals " + total
                        + " blocks, expected " + cleanedBlockCount);
            }
        }
    }
}
