package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

/**
 * Custom StructurePoolElement that places Witchery village buildings via
 * procedural Java code (no NBT template). Registered as "witchery:village_building"
 * and referenced from vanilla village/{type}/houses pools.
 *
 * Supported "building" values: apothecary, witch_hut, village_watch_tower, village_keep.
 */
public class WitcheryVillageElement extends StructurePoolElement {

    public static final MapCodec<WitcheryVillageElement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("building").forGetter(e -> e.building),
                    projectionCodec()
            ).apply(instance, WitcheryVillageElement::new));

    private final String building;

    public WitcheryVillageElement(String building, StructureTemplatePool.Projection projection) {
        super(projection);
        this.building = building;
    }

    // ── Abstract method implementations ──────────────────────────────────────

    @Override
    public Vec3i getSize(StructureTemplateManager mgr, Rotation rotation) {
        int[] s = dims();
        return new Vec3i(s[0], s[1], s[2]);
    }

    @Override
    public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
            StructureTemplateManager mgr, BlockPos pos, Rotation rotation, RandomSource random) {
        return List.of(); // standalone — no jigsaw road connections
    }

    @Override
    public BoundingBox getBoundingBox(StructureTemplateManager mgr, BlockPos pos, Rotation rotation) {
        int[] s = dims();
        return new BoundingBox(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + s[0] - 1, pos.getY() + s[1] - 1, pos.getZ() + s[2] - 1);
    }

    @Override
    public boolean place(StructureTemplateManager mgr, WorldGenLevel level, StructureManager structureMgr,
                         ChunkGenerator generator, BlockPos offset, BlockPos pos, Rotation rotation,
                         BoundingBox box, RandomSource random,
                         LiquidSettings liquidSettings, boolean keepJigsaws) {
        StructurePiece piece = createPiece(offset);
        if (piece == null) return false;
        ChunkPos chunkPos = new ChunkPos(new BlockPos(box.minX(), 0, box.minZ()));
        piece.postProcess(level, structureMgr, generator, random, box, chunkPos, pos);
        return true;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return WitcheryStructures.VILLAGE_BUILDING_ELEMENT_TYPE.get();
    }

    @Override
    public String toString() {
        return "WitcheryVillageElement[" + building + "]";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int[] dims() {
        return switch (building) {
            case "apothecary"           -> new int[]{9, 8, 6};
            case "witch_hut"            -> new int[]{7, 7, 7};
            case "village_watch_tower"  -> new int[]{8, 23, 8};
            case "village_keep"         -> new int[]{16, 26, 16};
            default                     -> new int[]{8, 8, 8};
        };
    }

    private StructurePiece createPiece(BlockPos origin) {
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        return switch (building) {
            case "apothecary"          -> new ApothecaryPiece(WitcheryStructures.APOTHECARY_PIECE.get(), x, y, z);
            case "witch_hut"           -> new WitchHutPiece(WitcheryStructures.WITCH_HUT_PIECE.get(), x, y, z);
            case "village_watch_tower" -> new VillageWatchTowerPiece(WitcheryStructures.VILLAGE_WATCH_TOWER_PIECE.get(), x, y, z);
            case "village_keep"        -> new VillageKeepPiece(WitcheryStructures.VILLAGE_KEEP_PIECE.get(), x, y, z);
            default -> null;
        };
    }
}
