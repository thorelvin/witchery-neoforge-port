package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Designer-authored NBT replacement for the hobgoblin hut.
 */
public class HobgoblinHutTemplatePiece extends TemplateStructurePiece {
    static final Identifier TEMPLATE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hobgoblin_hut/hobgobhut");

    private static final int WIDTH = 14;
    private static final int HEIGHT = 14;
    private static final int DEPTH = 14;
    private static final int[][] SUPPORT_COLUMNS = {
            {2, 4}, {2, 8}, {4, 9}, {8, 8}, {11, 4}, {11, 8}
    };

    public HobgoblinHutTemplatePiece(StructureTemplateManager structureTemplateManager,
                                     int x, int y, int z) {
        super(WitcheryStructures.HOBGOBLIN_HUT_TEMPLATE_PIECE.get(), 0, structureTemplateManager,
                TEMPLATE, TEMPLATE.toString(), makeSettings(), new BlockPos(x, y, z));
    }

    public HobgoblinHutTemplatePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.HOBGOBLIN_HUT_TEMPLATE_PIECE.get(), tag,
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
        clearClutter(level, box);

        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);

        fillSupportColumns(level, box);
        spawnResident(level, box);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        WitcheryMod.LOGGER.warn("[HobgoblinHut] Unknown template marker '{}' at {}", name, pos);
    }

    private void clearClutter(WorldGenLevel level, BoundingBox box) {
        NbtStructurePlacement.clearLocalClutter(level, box, WIDTH, HEIGHT, DEPTH, this::templateWorldPos);
    }

    private void fillSupportColumns(WorldGenLevel level, BoundingBox box) {
        for (int[] column : SUPPORT_COLUMNS) {
            fillColumnDownAt(level, box, templateWorldPos(column[0], -1, column[1]),
                    Blocks.DIRT.defaultBlockState());
        }
    }

    private void fillColumnDownAt(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        if (!box.isInside(cursor)) {
            return;
        }
        while (isReplaceableByStructures(level.getBlockState(cursor))
                && cursor.getY() > level.getMinY() + 1) {
            level.setBlock(cursor, state, 2);
            cursor.move(Direction.DOWN);
        }
    }

    private void spawnResident(WorldGenLevel level, BoundingBox box) {
        BlockPos spawnPos = templateWorldPos(7, 3, 7);
        if (!box.isInside(spawnPos)) {
            return;
        }

        HobgoblinEntity hobgoblin = WitcheryEntities.HOBGOBLIN.get()
                .create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (hobgoblin == null) {
            return;
        }
        hobgoblin.setPersistenceRequired();
        hobgoblin.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        hobgoblin.finalizeSpawn(level,
                new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(hobgoblin);
    }

    private BlockPos templateWorldPos(int localX, int localY, int localZ) {
        return StructureTemplate.calculateRelativePosition(
                placeSettings, new BlockPos(localX, localY, localZ)).offset(templatePosition);
    }
}
