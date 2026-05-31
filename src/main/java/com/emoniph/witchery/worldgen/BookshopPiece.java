package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Source-inspired 10x8x9 Witchery village bookshop.
 * The old component carried a book loot chest at local 2,1,4 and a shopkeeper.
 */
public class BookshopPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/bookshop"));

    public BookshopPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 9, y + 7, z + 8));
        setOrientation(Direction.NORTH);
    }

    public BookshopPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.BOOKSHOP_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState bookshelf = Blocks.BOOKSHELF.defaultBlockState();
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        BlockState topSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

        // Foundation and floor.
        generateBox(level, box, 0, 0, 0, 9, 0, 8, cobble, cobble, false);
        generateBox(level, box, 1, 0, 1, 8, 0, 7, stoneBricks, stoneBricks, false);
        for (int bx = 0; bx <= 9; bx++) {
            for (int bz = 0; bz <= 8; bz++) {
                fillColumnDown(level, cobble, bx, -1, bz, box);
            }
        }

        // Shop shell.
        for (int y = 1; y <= 4; y++) {
            generateBox(level, box, 0, y, 0, 9, y, 8, planks, planks, false);
            generateBox(level, box, 1, y, 1, 8, y, 7, air, air, false);
        }
        for (int y = 1; y <= 5; y++) {
            placeBlock(level, log, 0, y, 0, box);
            placeBlock(level, log, 9, y, 0, box);
            placeBlock(level, log, 0, y, 8, box);
            placeBlock(level, log, 9, y, 8, box);
        }

        // Door and windows. The jigsaw connection is centered at x=5,z=0.
        placeBlock(level, air, 5, 1, 0, box);
        placeBlock(level, air, 5, 2, 0, box);
        placeBlock(level, glass, 2, 2, 0, box);
        placeBlock(level, glass, 3, 2, 0, box);
        placeBlock(level, glass, 7, 2, 0, box);
        placeBlock(level, glass, 8, 2, 0, box);
        placeBlock(level, glass, 0, 2, 3, box);
        placeBlock(level, glass, 0, 2, 5, box);
        placeBlock(level, glass, 9, 2, 3, box);
        placeBlock(level, glass, 9, 2, 5, box);

        // Bookshelves and reading counters.
        generateBox(level, box, 1, 1, 7, 8, 3, 7, bookshelf, bookshelf, false);
        for (int z = 2; z <= 6; z++) {
            placeBlock(level, bookshelf, 1, 1, z, box);
            placeBlock(level, bookshelf, 8, 1, z, box);
        }
        for (int x = 3; x <= 6; x++) {
            placeBlock(level, Blocks.CHISELED_BOOKSHELF.defaultBlockState(), x, 3, 6, box);
        }
        placeBlock(level, Blocks.LECTERN.defaultBlockState(), 5, 1, 3, box);
        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 3, 1, 2, box);
        placeBlock(level, Blocks.OAK_PRESSURE_PLATE.defaultBlockState(), 4, 1, 3, box);
        placeBlock(level, Blocks.OAK_PRESSURE_PLATE.defaultBlockState(), 6, 1, 3, box);

        // Roof: shallow oak gable plus a central ridge.
        generateBox(level, box, 0, 5, 0, 9, 5, 8, planks, planks, false);
        for (int z = 0; z <= 8; z++) {
            placeBlock(level, stairs(Direction.WEST), 0, 6, z, box);
            placeBlock(level, stairs(Direction.EAST), 9, 6, z, box);
            placeBlock(level, stairs(Direction.WEST), 1, 7, z, box);
            placeBlock(level, stairs(Direction.EAST), 8, 7, z, box);
            placeBlock(level, topSlab, 4, 7, z, box);
            placeBlock(level, topSlab, 5, 7, z, box);
        }

        // A small loft ladder gives the high interior a purpose without blocking the shop.
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = 1; y <= 4; y++) placeBlock(level, ladder, 8, y, 6, box);

        createChest(level, box, random, 2, 1, 4, LOOT_TABLE);
        placeSign(level);
        spawnShopkeeper(level);
    }

    private static BlockState stairs(Direction direction) {
        return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, direction);
    }

    private void placeSign(WorldGenLevel level) {
        BlockPos signWorld = new BlockPos(
                boundingBox.minX() + 5,
                boundingBox.minY() + 4,
                boundingBox.minZ());
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.SOUTH);
        level.setBlock(signWorld, signState, 3);
        if (level.getBlockEntity(signWorld) instanceof SignBlockEntity sign) {
            if (sign.getLevel() == null) sign.setLevel(level.getLevel());
            SignText text = new SignText();
            text = text.setMessage(0, Component.literal("~ Bookshop ~"));
            sign.setText(text, true);
        }
    }

    private void spawnShopkeeper(WorldGenLevel level) {
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 5,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 4);
        Villager villager = EntityType.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (villager == null) return;
        villager.setPersistenceRequired();
        villager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        villager.finalizeSpawn(level, new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(villager);
    }
}
