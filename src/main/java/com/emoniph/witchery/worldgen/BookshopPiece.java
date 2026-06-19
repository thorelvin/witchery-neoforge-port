package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Source-shaped 10x8x9 Witchery village bookshop.
 * The old component carried a book loot chest at local 2,1,4 and four book
 * item frames along the rear wall.
 */
public class BookshopPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/bookshop"));
    private static final Map<ServerLevel, Set<BlockPos>> CLAIMED_FRAME_POSITIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

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
        VillageBuildingMaterials materials = VillageBuildingMaterials.forVillage(level, mgr, boundingBox, box);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = materials.foundation();
        BlockState floor = materials.floor();
        BlockState wall = materials.wall();
        BlockState trim = materials.trim();
        BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();

        debugLog("Bookshop shell start at {} for generation box {}.", boundingBox, box);

        // Source ComponentVillageBookShop footprint.
        generateBox(level, box, 1, 0, 1, 8, 0, 6, stone, stone, false);
        generateBox(level, box, 2, 0, 2, 7, 0, 5, floor, floor, false);
        generateBox(level, box, 1, 1, 0, 8, 7, 6, air, air, false);

        // Compact rear shop body with the old open front.
        generateBox(level, box, 1, 1, 3, 8, 5, 6, stone, stone, false);
        generateBox(level, box, 1, 6, 4, 8, 6, 5, stone, stone, false);
        generateBox(level, box, 1, 4, 1, 8, 4, 2, stone, stone, false);
        generateBox(level, box, 2, 1, 4, 7, 4, 5, air, air, false);
        generateBox(level, box, 2, 1, 3, 7, 3, 3, wall, wall, false);
        generateBox(level, box, 3, 2, 3, 6, 3, 3, air, air, false);
        placeBlock(level, air, 6, 1, 3, box);

        generateBox(level, box, 1, 2, 4, 1, 4, 5, wall, wall, false);
        generateBox(level, box, 8, 2, 4, 8, 4, 5, wall, wall, false);
        generateBox(level, box, 2, 2, 6, 7, 4, 6, wall, wall, false);

        generateBox(level, box, 1, 1, 1, 1, 3, 1, trim, trim, false);
        generateBox(level, box, 8, 1, 1, 8, 3, 1, trim, trim, false);
        placeBlock(level, materials.stepStairs(Direction.NORTH), 3, 0, 0, box);
        placeBlock(level, materials.stepStairs(Direction.NORTH), 4, 0, 0, box);
        placeBlock(level, materials.stepStairs(Direction.NORTH), 5, 0, 0, box);
        placeBlock(level, materials.stepStairs(Direction.NORTH), 6, 0, 0, box);

        // Keep the bookshop recognisable in the modern port while preserving the source shell.
        placeBlock(level, shelf, 2, 1, 5, box);
        placeBlock(level, shelf, 3, 1, 5, box);
        placeBlock(level, shelf, 6, 1, 5, box);
        placeBlock(level, shelf, 7, 1, 5, box);
        placeBlock(level, Blocks.LECTERN.defaultBlockState(), 5, 1, 3, box);
        placeBlock(level, Blocks.TORCH.defaultBlockState(), 3, 4, 4, box);
        placeBlock(level, Blocks.TORCH.defaultBlockState(), 6, 4, 4, box);

        // Explicit pane connections avoid isolated "pole" panes when generated from code.
        placeBlock(level, VillageBuildingMaterials.paneNorthSouth(), 1, 3, 5, box);
        placeBlock(level, VillageBuildingMaterials.paneNorthSouth(), 8, 3, 5, box);

        for (int x = 1; x <= 8; x++) {
            placeBlock(level, materials.roofStairs(Direction.NORTH), x, 5, 2, box);
            placeBlock(level, materials.roofStairs(Direction.NORTH), x, 6, 3, box);
            placeBlock(level, materials.roofStairs(Direction.NORTH), x, 7, 4, box);
            placeBlock(level, materials.roofStairs(Direction.SOUTH), x, 5, 7, box);
            placeBlock(level, materials.roofStairs(Direction.SOUTH), x, 6, 6, box);
            placeBlock(level, materials.roofStairs(Direction.SOUTH), x, 7, 5, box);
        }

        debugLog("Bookshop shell finished at {} for generation box {}.", boundingBox, box);
        createChest(level, box, random, 2, 1, 4, LOOT_TABLE);
        debugLog("Bookshop chest phase finished at {} for generation box {}.", boundingBox, box);
        int frames = placeBookFrames(level, box, random, wall);
        debugLog("Bookshop frame phase finished at {} for generation box {} ({} frame(s)).",
                boundingBox, box, frames);
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 6; z++) {
                fillColumnDown(level, stone, x, -1, z, box);
            }
        }
        debugLog("Bookshop foundation phase finished at {} for generation box {}.", boundingBox, box);
        spawnShopkeeper(level, box);
        debugLog("Bookshop resident phase finished at {} for generation box {}.", boundingBox, box);
    }

    private int placeBookFrames(WorldGenLevel level, BoundingBox box, RandomSource random, BlockState wall) {
        int placed = 0;
        for (int x = 3; x <= 6; x++) {
            placeBlock(level, wall, x, 3, 6, box);
            BlockPos framePos = getWorldPos(x, 3, 5).immutable();
            BlockPos supportPos = getWorldPos(x, 3, 6).immutable();
            if (!box.isInside(framePos) || !box.isInside(supportPos)) continue;
            BlockState support = level.getBlockState(supportPos);
            BlockState front = level.getBlockState(framePos);
            if (!support.isSolid() || (!front.isAir() && !front.canBeReplaced())) continue;
            if (!claimFramePosition(level.getLevel(), framePos)) continue;

            ItemFrame frame = new ItemFrame(level.getLevel(), framePos, Direction.SOUTH);
            frame.setItem(randomBookFrameStack(random), false);
            level.addFreshEntityWithPassengers(frame);
            placed++;
        }
        return placed;
    }

    private static ItemStack randomBookFrameStack(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0, 1 -> new ItemStack(Items.BOOK);
            case 2, 3, 4 -> new ItemStack(WitcheryItems.TORN_PAGE.get());
            default -> new ItemStack(WitcheryItems.VAMPIRE_BOOK.get());
        };
    }

    private void spawnShopkeeper(WorldGenLevel level, BoundingBox box) {
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 5,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 4);
        if (!box.isInside(spawnPos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (villager == null) return;
        villager.setPersistenceRequired();
        villager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        villager.finalizeSpawn(level, new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(villager);
    }

    private static boolean claimFramePosition(ServerLevel level, BlockPos pos) {
        synchronized (CLAIMED_FRAME_POSITIONS) {
            Set<BlockPos> positions = CLAIMED_FRAME_POSITIONS.computeIfAbsent(level, key -> new HashSet<>());
            return positions.add(pos.immutable());
        }
    }

    private static void debugLog(String message, Object... args) {
        if (WitcheryConfig.DEBUG.get()) {
            WitcheryMod.LOGGER.debug("[VillageBuildings:Debug] " + message, args);
        }
    }
}
