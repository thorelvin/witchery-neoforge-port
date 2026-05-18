package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.network.chat.Component;

/**
 * 9×8×6 apothecary shop placed on the surface.
 * Front door at Z=0, X=1. Contains a Coven Witch shopkeeper and a loot chest.
 */
public class ApothecaryPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/apothecary"));

    public ApothecaryPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 8, y + 7, z + 5));
        setOrientation(net.minecraft.core.Direction.NORTH);
    }

    public ApothecaryPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.APOTHECARY_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState air    = Blocks.AIR.defaultBlockState();
        BlockState glass  = Blocks.GLASS_PANE.defaultBlockState();
        BlockState log    = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState();
        BlockState slab   = Blocks.OAK_SLAB.defaultBlockState();

        // Y=0 — cobblestone foundation
        generateBox(level, box, 0, 0, 0, 8, 0, 5, cobble, cobble, false);

        // Y=1-5 — oak plank walls (full perimeter, hollow interior)
        for (int y = 1; y <= 5; y++) {
            generateBox(level, box, 0, y, 0, 8, y, 5, planks, planks, false);
            generateBox(level, box, 1, y, 1, 7, y, 4, air, air, false);
        }

        // Witch log corner pillars (Y=1-5)
        for (int y = 1; y <= 5; y++) {
            placeBlock(level, log, 0, y, 0, box); placeBlock(level, log, 8, y, 0, box);
            placeBlock(level, log, 0, y, 5, box); placeBlock(level, log, 8, y, 5, box);
        }

        // Y=1-2 — front door opening at X=1, Z=0
        placeBlock(level, air, 1, 1, 0, box);
        placeBlock(level, air, 1, 2, 0, box);

        // Front face windows (Y=2-3, spaced across front)
        placeBlock(level, glass, 3, 2, 0, box); placeBlock(level, glass, 3, 3, 0, box);
        placeBlock(level, glass, 5, 2, 0, box); placeBlock(level, glass, 5, 3, 0, box);
        placeBlock(level, glass, 7, 2, 0, box); placeBlock(level, glass, 7, 3, 0, box);

        // Back face windows
        placeBlock(level, glass, 2, 2, 5, box); placeBlock(level, glass, 2, 3, 5, box);
        placeBlock(level, glass, 6, 2, 5, box); placeBlock(level, glass, 6, 3, 5, box);

        // Side windows (left X=0, right X=8)
        placeBlock(level, glass, 0, 2, 2, box); placeBlock(level, glass, 0, 3, 2, box);
        placeBlock(level, glass, 0, 2, 3, box); placeBlock(level, glass, 0, 3, 3, box);
        placeBlock(level, glass, 8, 2, 2, box); placeBlock(level, glass, 8, 3, 2, box);
        placeBlock(level, glass, 8, 2, 3, box); placeBlock(level, glass, 8, 3, 3, box);

        // Y=6 — oak plank ceiling
        generateBox(level, box, 0, 6, 0, 8, 6, 5, planks, planks, false);

        // Y=7 — slabbed gable roof (low apex at center, slabs on the ridge)
        generateBox(level, box, 0, 7, 0, 8, 7, 5, slab, slab, false);

        // ── Interior furnishings (Y=1) ────────────────────────────────────────
        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 2, 1, 1, box);
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(),      6, 1, 1, box);
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(),      7, 1, 1, box);
        placeBlock(level, Blocks.FLOWER_POT.defaultBlockState(),     1, 1, 2, box);
        placeBlock(level, Blocks.FLOWER_POT.defaultBlockState(),     1, 1, 3, box);
        placeBlock(level, WitcheryBlocks.ALTAR.get().defaultBlockState(),      4, 1, 1, box);
        placeBlock(level, WitcheryBlocks.KETTLE.get().defaultBlockState(),     3, 1, 4, box);
        placeBlock(level, WitcheryBlocks.DISTILLERY.get().defaultBlockState(), 5, 1, 4, box);
        placeBlock(level, WitcheryBlocks.CANDELABRA.get().defaultBlockState(), 7, 1, 4, box);

        // Wall torches (Y=3)
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 1, 3, 1, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 7, 3, 1, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 1, 3, 4, box);
        placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 7, 3, 4, box);

        // ── Loot chest ────────────────────────────────────────────────────────
        createChest(level, box, random, 7, 1, 3, LOOT_TABLE);

        // ── Apothecary sign on the front wall above the door ─────────────────
        BlockPos signWorld = new BlockPos(
                boundingBox.minX() + 1,
                boundingBox.minY() + 4,
                boundingBox.minZ());
        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.SOUTH);
        level.setBlock(signWorld, signState, 3);
        if (level.getBlockEntity(signWorld) instanceof SignBlockEntity sign) {
            if (sign.getLevel() == null) sign.setLevel(level.getLevel());
            SignText text = new SignText();
            text = text.setMessage(0, Component.literal("~ Apothecary ~"));
            sign.setText(text, true);
        }

        // ── Coven Witch (apothecary shopkeeper) ───────────────────────────────
        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 4,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 2);
        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (witch != null) {
            witch.setPersistenceRequired();
            witch.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            witch.finalizeSpawn(level, level.getLevel().getCurrentDifficultyAt(spawnPos), EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(witch);
        }
    }
}
