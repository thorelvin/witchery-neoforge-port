package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryStructures;
import com.emoniph.witchery.world.VanishingHutData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.UUID;

/**
 * Worldgen placement for a {@link VanishingHut}. Clears clutter from the footprint, builds the hut
 * + buried anchor + resident witch through the shared {@code VanishingHut} logic, and records it in
 * {@link VanishingHutData} so {@code /witchery goto vanishing_hut} can find it. After generation the
 * hut behaves exactly like a command-placed one (relocates when unobserved, held by a salt line).
 */
public class VanishingHutPiece extends StructurePiece {

    public VanishingHutPiece(int centerX, int anchorY, int centerZ) {
        super(WitcheryStructures.VANISHING_HUT_PIECE.get(), 0, new BoundingBox(
                centerX + VanishingHut.MIN_DX, anchorY, centerZ + VanishingHut.MIN_DZ,
                centerX + VanishingHut.MAX_DX, anchorY + VanishingHut.ROOF_DY,
                centerZ + VanishingHut.MAX_DZ));
        setOrientation(Direction.NORTH);
    }

    public VanishingHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.VANISHING_HUT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        int centerX = boundingBox.minX() - VanishingHut.MIN_DX;
        int centerZ = boundingBox.minZ() - VanishingHut.MIN_DZ;
        BlockPos anchor = new BlockPos(centerX, boundingBox.minY(), centerZ);

        clearClutter(level, centerX, centerZ, boundingBox.minY());

        ServerLevel serverLevel = level.getLevel();
        UUID hutId = UUID.randomUUID();
        VanishingHut.buildForWorldgen(level, anchor, hutId);
        CovenWitchEntity witch = VanishingHut.spawnResident(level, anchor);
        if (level.getBlockEntity(anchor) instanceof VanishingHutAnchorBlockEntity be) {
            be.setResident(witch == null ? null : witch.getUUID());
        }
        VanishingHutData.get(serverLevel).upsert(
                hutId, serverLevel, anchor, witch == null ? null : witch.getUUID());
    }

    /** Clear plants/leaves/loose logs from the hut volume so trees don't poke through the walls. */
    private static void clearClutter(WorldGenLevel level, int centerX, int centerZ, int anchorY) {
        int floorY = anchorY + 1;
        int roofY = anchorY + VanishingHut.ROOF_DY;
        NbtStructurePlacement.clearClutterBox(level,
                centerX + VanishingHut.MIN_DX, floorY, centerZ + VanishingHut.MIN_DZ,
                centerX + VanishingHut.MAX_DX, roofY, centerZ + VanishingHut.MAX_DZ);
    }
}
