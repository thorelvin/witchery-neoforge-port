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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Source-shaped 9x9x6 village apothecary.
 * Front door at local X=1, Z=0. Spawns a librarian-skinned Apothecary villager
 * with the source trade list.
 */
public class ApothecaryPiece extends StructurePiece {

    private static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("witchery", "chests/apothecary"));

    public ApothecaryPiece(StructurePieceType type, int x, int y, int z) {
        super(type, 0, new BoundingBox(x, y, z, x + 8, y + 8, z + 5));
        setOrientation(Direction.NORTH);
    }

    public ApothecaryPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WitcheryStructures.APOTHECARY_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

    @Override
    public void postProcess(WorldGenLevel level, StructureManager mgr, ChunkGenerator gen,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        VillageBuildingMaterials materials = VillageBuildingMaterials.forVillage(level, boundingBox, box);
        BlockState stone = materials.foundation();
        BlockState planks = materials.wall();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glassFront = VillageBuildingMaterials.paneEastWest();
        BlockState glassSide = VillageBuildingMaterials.paneNorthSouth();

        generateBox(level, box, 1, 1, 1, 7, 5, 4, air, air, false);
        generateBox(level, box, 0, 0, 0, 8, 0, 5, stone, stone, false);
        generateBox(level, box, 0, 5, 0, 8, 5, 5, stone, stone, false);
        generateBox(level, box, 0, 6, 1, 8, 6, 4, stone, stone, false);
        generateBox(level, box, 0, 7, 2, 8, 7, 3, stone, stone, false);

        for (int k = -1; k <= 2; k++) {
            for (int x = 0; x <= 8; x++) {
                placeBlock(level, materials.roofStairs(Direction.NORTH), x, 6 + k, k, box);
                placeBlock(level, materials.roofStairs(Direction.SOUTH), x, 6 + k, 5 - k, box);
            }
        }

        generateBox(level, box, 0, 1, 0, 0, 1, 5, stone, stone, false);
        generateBox(level, box, 1, 1, 5, 8, 1, 5, stone, stone, false);
        generateBox(level, box, 8, 1, 0, 8, 1, 4, stone, stone, false);
        generateBox(level, box, 2, 1, 0, 7, 1, 0, stone, stone, false);
        generateBox(level, box, 0, 2, 0, 0, 4, 0, stone, stone, false);
        generateBox(level, box, 0, 2, 5, 0, 4, 5, stone, stone, false);
        generateBox(level, box, 8, 2, 5, 8, 4, 5, stone, stone, false);
        generateBox(level, box, 8, 2, 0, 8, 4, 0, stone, stone, false);
        generateBox(level, box, 0, 2, 1, 0, 4, 4, planks, planks, false);
        generateBox(level, box, 1, 2, 5, 7, 4, 5, planks, planks, false);
        generateBox(level, box, 8, 2, 1, 8, 4, 4, planks, planks, false);
        generateBox(level, box, 1, 2, 0, 7, 4, 0, planks, planks, false);

        placeBlock(level, glassFront, 4, 2, 0, box);
        placeBlock(level, glassFront, 5, 2, 0, box);
        placeBlock(level, glassFront, 6, 2, 0, box);
        placeBlock(level, glassFront, 4, 3, 0, box);
        placeBlock(level, glassFront, 5, 3, 0, box);
        placeBlock(level, glassFront, 6, 3, 0, box);
        placeBlock(level, glassSide, 0, 2, 2, box);
        placeBlock(level, glassSide, 0, 2, 3, box);
        placeBlock(level, glassSide, 0, 3, 2, box);
        placeBlock(level, glassSide, 0, 3, 3, box);
        placeBlock(level, glassSide, 8, 2, 2, box);
        placeBlock(level, glassSide, 8, 2, 3, box);
        placeBlock(level, glassSide, 8, 3, 2, box);
        placeBlock(level, glassSide, 8, 3, 3, box);
        placeBlock(level, glassFront, 2, 2, 5, box);
        placeBlock(level, glassFront, 3, 2, 5, box);
        placeBlock(level, glassFront, 5, 2, 5, box);
        placeBlock(level, glassFront, 6, 2, 5, box);
        placeBlock(level, glassFront, 2, 3, 5, box);
        placeBlock(level, glassFront, 3, 3, 5, box);
        placeBlock(level, glassFront, 5, 3, 5, box);
        placeBlock(level, glassFront, 6, 3, 5, box);

        generateBox(level, box, 1, 4, 1, 7, 4, 1, planks, planks, false);
        generateBox(level, box, 1, 4, 4, 7, 4, 4, planks, planks, false);

        placeBlock(level, air, 1, 1, 0, box);
        placeBlock(level, air, 1, 2, 0, box);
        placeBlock(level, materials.door(Direction.SOUTH, DoubleBlockHalf.LOWER), 1, 1, 0, box);
        placeBlock(level, materials.door(Direction.SOUTH, DoubleBlockHalf.UPPER), 1, 2, 0, box);
        placeFrontStep(level, box, materials);

        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 7, 1, 1, box);
        placeBlock(level, upperStair(materials, Direction.SOUTH), 5, 1, 1, box);
        placeBlock(level, upperStair(materials, Direction.NORTH), 5, 1, 3, box);
        placeBlock(level, Blocks.FLOWER_POT.defaultBlockState(), 5, 1, 2, box);
        placeBlock(level, Blocks.TORCH.defaultBlockState(), 5, 2, 3, box);
        placeBlock(level, Blocks.SKELETON_SKULL.defaultBlockState(), 5, 2, 1, box);
        createChest(level, box, random, 7, 0, 1, LOOT_TABLE);

        placeApothecarySign(level, box);

        for (int z = 0; z < 6; z++) {
            for (int x = 0; x < 9; x++) {
                fillColumnDown(level, stone, x, -1, z, box);
            }
        }

        BlockPos spawnPos = new BlockPos(
                boundingBox.minX() + 2,
                boundingBox.minY() + 1,
                boundingBox.minZ() + 2);
        if (!box.isInside(spawnPos)) return;

        Villager villager = EntityType.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (villager != null) {
            villager.setPersistenceRequired();
            villager.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            villager.finalizeSpawn(level, new DifficultyInstance(level.getDifficulty(), 0L, 0L, 0.0F),
                    EntitySpawnReason.STRUCTURE, null);
            ApothecaryVillager.configure(villager, level);
            level.addFreshEntityWithPassengers(villager);
        }
    }

    private static BlockState upperStair(VillageBuildingMaterials materials, Direction facing) {
        return materials.roofStairs(facing).setValue(StairBlock.HALF, Half.TOP);
    }

    private void placeFrontStep(WorldGenLevel level, BoundingBox box, VillageBuildingMaterials materials) {
        BlockPos stepPos = getWorldPos(1, 0, -1);
        if (!box.isInside(stepPos)) return;
        if (level.getBlockState(stepPos).isAir() && !level.getBlockState(stepPos.below()).isAir()) {
            level.setBlock(stepPos, materials.stepStairs(Direction.NORTH), 2);
        }
    }

    private void placeApothecarySign(WorldGenLevel level, BoundingBox box) {
        BlockPos signWorld = getWorldPos(1, 3, -1);
        if (!box.isInside(signWorld)) return;

        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.SOUTH);
        level.setBlock(signWorld, signState, 3);
        if (level.getBlockEntity(signWorld) instanceof SignBlockEntity sign) {
            if (sign.getLevel() == null) sign.setLevel(level.getLevel());
            SignText text = new SignText().setMessage(1, Component.literal("~ Apothecary ~"));
            sign.setText(text, true);
        }
    }
}
