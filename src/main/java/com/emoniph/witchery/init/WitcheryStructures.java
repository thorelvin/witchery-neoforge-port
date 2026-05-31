package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.worldgen.ApothecaryPiece;
import com.emoniph.witchery.worldgen.BookshopPiece;
import com.emoniph.witchery.worldgen.CovenCirclePiece;
import com.emoniph.witchery.worldgen.CovenCircleStructure;
import com.emoniph.witchery.worldgen.HobgoblinHutPiece;
import com.emoniph.witchery.worldgen.HobgoblinHutStructure;
import com.emoniph.witchery.worldgen.StrawManPiece;
import com.emoniph.witchery.worldgen.StrawManStructure;
import com.emoniph.witchery.worldgen.TormentChamberPiece;
import com.emoniph.witchery.worldgen.TormentChamberStructure;
import com.emoniph.witchery.worldgen.VillageKeepPiece;
import com.emoniph.witchery.worldgen.VillageWatchTowerPiece;
import com.emoniph.witchery.worldgen.WitchHutPiece;
import com.emoniph.witchery.worldgen.WitcheryVillageElement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, WitcheryMod.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, WitcheryMod.MODID);

    public static final DeferredRegister<StructurePoolElementType<?>> POOL_ELEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, WitcheryMod.MODID);

    // ── Standalone structures (not village-injected) ──────────────────────────

    public static final DeferredHolder<StructureType<?>, StructureType<CovenCircleStructure>> COVEN_CIRCLE_TYPE =
            STRUCTURE_TYPES.register("coven_circle", () -> () -> CovenCircleStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> COVEN_CIRCLE_PIECE =
            PIECE_TYPES.register("coven_circle", () -> CovenCirclePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<StrawManStructure>> STRAW_MAN_TYPE =
            STRUCTURE_TYPES.register("straw_man", () -> () -> StrawManStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> STRAW_MAN_PIECE =
            PIECE_TYPES.register("straw_man", () -> StrawManPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<HobgoblinHutStructure>> HOBGOBLIN_HUT_TYPE =
            STRUCTURE_TYPES.register("hobgoblin_hut", () -> () -> HobgoblinHutStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HOBGOBLIN_HUT_PIECE =
            PIECE_TYPES.register("hobgoblin_hut", () -> HobgoblinHutPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<TormentChamberStructure>> TORMENT_CHAMBER_TYPE =
            STRUCTURE_TYPES.register("torment_chamber", () -> () -> TormentChamberStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TORMENT_CHAMBER_PIECE =
            PIECE_TYPES.register("torment_chamber", () -> TormentChamberPiece::new);

    // ── Village pool pieces (injected via WitcheryVillageElement) ─────────────
    // Piece types remain registered so saved worlds can deserialize them.

    public static final DeferredHolder<StructurePieceType, StructurePieceType> WITCH_HUT_PIECE =
            PIECE_TYPES.register("witch_hut", () -> WitchHutPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> APOTHECARY_PIECE =
            PIECE_TYPES.register("apothecary", () -> ApothecaryPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> BOOKSHOP_PIECE =
            PIECE_TYPES.register("bookshop", () -> BookshopPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> VILLAGE_WATCH_TOWER_PIECE =
            PIECE_TYPES.register("village_watch_tower", () -> VillageWatchTowerPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> VILLAGE_KEEP_PIECE =
            PIECE_TYPES.register("village_keep", () -> VillageKeepPiece::new);

    // ── Pool element type ─────────────────────────────────────────────────────

    public static final DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<WitcheryVillageElement>>
            VILLAGE_BUILDING_ELEMENT_TYPE =
            POOL_ELEMENT_TYPES.register("village_building",
                    () -> () -> WitcheryVillageElement.CODEC);
}
