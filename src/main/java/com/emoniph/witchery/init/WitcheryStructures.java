package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.worldgen.ApothecaryPiece;
import com.emoniph.witchery.worldgen.ApothecaryStructure;
import com.emoniph.witchery.worldgen.HobgoblinHutPiece;
import com.emoniph.witchery.worldgen.HobgoblinHutStructure;
import com.emoniph.witchery.worldgen.TormentChamberPiece;
import com.emoniph.witchery.worldgen.TormentChamberStructure;
import com.emoniph.witchery.worldgen.WitchHutPiece;
import com.emoniph.witchery.worldgen.WitchHutStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WitcheryMod.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, WitcheryMod.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<WitchHutStructure>> WITCH_HUT_TYPE =
            STRUCTURE_TYPES.register("witch_hut", () -> () -> WitchHutStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> WITCH_HUT_PIECE =
            PIECE_TYPES.register("witch_hut", () -> WitchHutPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<ApothecaryStructure>> APOTHECARY_TYPE =
            STRUCTURE_TYPES.register("apothecary", () -> () -> ApothecaryStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> APOTHECARY_PIECE =
            PIECE_TYPES.register("apothecary", () -> ApothecaryPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<HobgoblinHutStructure>> HOBGOBLIN_HUT_TYPE =
            STRUCTURE_TYPES.register("hobgoblin_hut", () -> () -> HobgoblinHutStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HOBGOBLIN_HUT_PIECE =
            PIECE_TYPES.register("hobgoblin_hut", () -> HobgoblinHutPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<TormentChamberStructure>> TORMENT_CHAMBER_TYPE =
            STRUCTURE_TYPES.register("torment_chamber", () -> () -> TormentChamberStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TORMENT_CHAMBER_PIECE =
            PIECE_TYPES.register("torment_chamber", () -> TormentChamberPiece::new);
}
