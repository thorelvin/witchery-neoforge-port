package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.events.VillageWallEvents;
import com.emoniph.witchery.init.WitcheryStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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
    private static final int VILLAGE_CLAIM_RADIUS_BLOCKS = 384;
    private static final int VILLAGE_CLAIM_RADIUS_SQR = VILLAGE_CLAIM_RADIUS_BLOCKS * VILLAGE_CLAIM_RADIUS_BLOCKS;
    private static final Map<ServerLevel, List<VillageClaim>> VILLAGE_CLAIMS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final String building;

    private record VillageKey(int chunkX, int chunkZ, int minX, int minZ, int maxX, int maxZ) {
        static VillageKey of(StructureStart start) {
            BoundingBox bb = start.getBoundingBox();
            ChunkPos chunk = start.getChunkPos();
            return new VillageKey(chunk.x, chunk.z, bb.minX(), bb.minZ(), bb.maxX(), bb.maxZ());
        }
    }

    private static final class VillageClaim {
        VillageKey key;
        final int centerX;
        final int centerZ;
        final Map<String, BlockPos> buildings = new HashMap<>();
        final Set<BlockPos> skippedOrigins = new HashSet<>();

        VillageClaim(VillageKey key, BlockPos origin) {
            this.key = key;
            this.centerX = origin.getX();
            this.centerZ = origin.getZ();
        }

        boolean matches(VillageKey candidateKey, BlockPos origin) {
            if (key != null && key.equals(candidateKey)) return true;
            if (key == null && candidateKey != null && isNear(origin)) {
                key = candidateKey;
                return true;
            }
            return candidateKey == null && isNear(origin);
        }

        boolean isNear(BlockPos origin) {
            long dx = origin.getX() - (long) centerX;
            long dz = origin.getZ() - (long) centerZ;
            return dx * dx + dz * dz <= VILLAGE_CLAIM_RADIUS_SQR;
        }
    }

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
        // The procedural pieces are only built in their StructurePiece NORTH orientation.
        // That orientation flips local z=0 to the generated south face, so the jigsaw must
        // be advertised on the local south edge to line up with the visible front door.
        if (rotation != Rotation.NONE) return List.of();

        int[] s = dims();
        // Vanilla village houses put their building_entrance jigsaw at y=0 while streets
        // expose theirs at y=1. Matching that baseline raises the building floor one block
        // above the path, instead of sinking it level with the road.
        BlockPos jigPos = new BlockPos(doorX(), 0, s[2] - 1);
        var state = Blocks.JIGSAW.defaultBlockState()
                .setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP);

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
        if (!claimVillageBuilding(level, structureMgr, offset)) return true;
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

    private boolean claimVillageBuilding(WorldGenLevel level, StructureManager structureMgr, BlockPos origin) {
        BlockPos immutableOrigin = origin.immutable();
        ServerLevel serverLevel = level.getLevel();
        VillageKey villageKey = findVillageKey(serverLevel, structureMgr, origin);
        synchronized (VILLAGE_CLAIMS) {
            List<VillageClaim> claims = VILLAGE_CLAIMS.computeIfAbsent(serverLevel, key -> new ArrayList<>());
            VillageClaim claim = null;
            for (VillageClaim candidate : claims) {
                if (candidate.matches(villageKey, origin)) {
                    claim = candidate;
                    break;
                }
            }
            if (claim == null) {
                claim = new VillageClaim(villageKey, origin);
                claims.add(claim);
            }

            BlockPos existing = claim.buildings.get(building);
            if (existing == null) {
                claim.buildings.put(building, immutableOrigin);
                return true;
            }
            if (existing.equals(immutableOrigin)) {
                return true;
            }
            if (claim.skippedOrigins.add(immutableOrigin)) {
                WitcheryMod.LOGGER.info("[VillageBuildings] Skipping duplicate {} near {} (already placed at {}).",
                        building, immutableOrigin, existing);
            }
            return false;
        }
    }

    private VillageKey findVillageKey(ServerLevel level, StructureManager structureMgr, BlockPos origin) {
        StructureStart best = null;
        long bestDist = Long.MAX_VALUE;
        for (StructureStart start : structureMgr.startsForStructure(
                new ChunkPos(origin), structure -> VillageWallEvents.isVillageStructure(structure, level))) {
            if (start == null || !start.isValid()) continue;
            BoundingBox bb = start.getBoundingBox();
            if (origin.getX() >= bb.minX() && origin.getX() <= bb.maxX()
                    && origin.getZ() >= bb.minZ() && origin.getZ() <= bb.maxZ()) {
                return VillageKey.of(start);
            }
            long dist = distToCenterSqr(bb, origin);
            if (dist < bestDist) {
                best = start;
                bestDist = dist;
            }
        }
        return best != null && bestDist <= VILLAGE_CLAIM_RADIUS_SQR ? VillageKey.of(best) : null;
    }

    private static long distToCenterSqr(BoundingBox bb, BlockPos pos) {
        long dx = pos.getX() - (long) ((bb.minX() + bb.maxX()) / 2);
        long dz = pos.getZ() - (long) ((bb.minZ() + bb.maxZ()) / 2);
        return dx * dx + dz * dz;
    }
}
