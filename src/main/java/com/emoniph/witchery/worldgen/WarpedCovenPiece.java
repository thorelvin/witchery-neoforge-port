package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * NBT-backed Nether coven found in warped forests.
 */
public class WarpedCovenPiece extends TemplateStructurePiece {
    static final Identifier TEMPLATE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "warped_coven/warpedcoven");

    private static final int WIDTH = 17;
    private static final int HEIGHT = 17;
    private static final int DEPTH = 17;
    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "chests/warped_coven"));

    public WarpedCovenPiece(StructureTemplateManager structureTemplateManager, int x, int y, int z) {
        super(WitcheryStructures.WARPED_COVEN_PIECE.get(), 0, structureTemplateManager,
                TEMPLATE, TEMPLATE.toString(), makeSettings(), new BlockPos(x, y, z));
    }

    public WarpedCovenPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.WARPED_COVEN_PIECE.get(), tag,
                ctx.structureTemplateManager(), template -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {
        clearVolumeAboveBase(level, box);

        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);

        placeLootChest(level, box, random);
        spawnCovenWitch(level, box);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        WitcheryMod.LOGGER.warn("[WarpedCoven] Unknown template marker '{}' at {}", name, pos);
    }

    private void clearVolumeAboveBase(WorldGenLevel level, BoundingBox box) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 1; y < HEIGHT; y++) {
                    BlockPos blockPos = templateWorldPos(x, y, z);
                    if (box.isInside(blockPos)) {
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void placeLootChest(WorldGenLevel level, BoundingBox box, RandomSource random) {
        createChest(level, box, random, templateWorldPos(9, 1, 8), LOOT_TABLE, null);
    }

    private void spawnCovenWitch(WorldGenLevel level, BoundingBox box) {
        BlockPos spawnPos = templateWorldPos(8, 1, 8);
        if (!box.isInside(spawnPos)) {
            return;
        }

        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get()
                .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (witch == null) {
            return;
        }
        witch.setPersistenceRequired();
        witch.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        witch.finalizeSpawn(level,
                new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(witch);
    }

    private BlockPos templateWorldPos(int localX, int localY, int localZ) {
        return StructureTemplate.calculateRelativePosition(
                placeSettings, new BlockPos(localX, localY, localZ)).offset(templatePosition);
    }
}
