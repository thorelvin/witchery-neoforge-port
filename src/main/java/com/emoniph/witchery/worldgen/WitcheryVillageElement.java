package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.data.worldgen.Pools;
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
 * Supported "building" values: apothecary, bookshop, witch_hut, village_watch_tower, village_keep.
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
        // The procedural pieces are only built in their NORTH orientation, so we only offer a
        // connection for the NONE rotation; the building then attaches on the north side of a
        // village street whose street jigsaw faces south. (Offering other rotations without
        // rotating the build would misalign the piece and it would be rejected anyway.)
        if (rotation != Rotation.NONE) return List.of();

        int[] s = dims();
        // Connection jigsaw on the building's north face (z=0), centred in X, at the door
        // course (y=1), facing NORTH so it mates with a street jigsaw just outside (z=-1).
        BlockPos jigPos = new BlockPos(doorX(), 1, 0);
        var state = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.NORTH_UP);

        CompoundTag tag = new CompoundTag();
        tag.putString("name", "minecraft:building_entrance");
        tag.putString("target", "minecraft:empty");
        tag.putString("pool", "minecraft:empty");
        tag.putString("final_state", "minecraft:air");
        tag.putString("joint", "aligned");

        StructureTemplate.StructureBlockInfo info =
                new StructureTemplate.StructureBlockInfo(jigPos, state, tag);
        // name MUST equal the village street's target ("minecraft:building_entrance") to connect.
        return List.of(new StructureTemplate.JigsawBlockInfo(
                info, JigsawBlockEntity.JointType.ALIGNED,
                Identifier.withDefaultNamespace("building_entrance"),
                Pools.EMPTY,
                Identifier.withDefaultNamespace("empty"),
                0, 0));
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
            case "bookshop"             -> new int[]{10, 8, 9};
            case "witch_hut"            -> new int[]{4, 6, 5};
            case "village_watch_tower"  -> new int[]{8, 23, 8};
            case "village_keep"         -> new int[]{16, 26, 16};
            default                     -> new int[]{8, 8, 8};
        };
    }

    private int doorX() {
        return switch (building) {
            case "apothecary" -> 1;
            case "bookshop" -> 5;
            case "witch_hut" -> 1;
            case "village_watch_tower" -> 4;
            case "village_keep" -> 8;
            default -> dims()[0] / 2;
        };
    }

    private StructurePiece createPiece(BlockPos origin) {
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        return switch (building) {
            case "apothecary"          -> new ApothecaryPiece(WitcheryStructures.APOTHECARY_PIECE.get(), x, y, z);
            case "bookshop"            -> new BookshopPiece(WitcheryStructures.BOOKSHOP_PIECE.get(), x, y, z);
            case "witch_hut"           -> new WitchHutPiece(WitcheryStructures.WITCH_HUT_PIECE.get(), x, y, z);
            case "village_watch_tower" -> new VillageWatchTowerPiece(WitcheryStructures.VILLAGE_WATCH_TOWER_PIECE.get(), x, y, z);
            case "village_keep"        -> new VillageKeepPiece(WitcheryStructures.VILLAGE_KEEP_PIECE.get(), x, y, z);
            default -> null;
        };
    }
}
