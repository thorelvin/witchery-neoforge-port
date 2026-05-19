package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.*;
import com.emoniph.witchery.blocks.entity.CoffinBlockEntity;
import com.emoniph.witchery.blocks.entity.RowanDoorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class WitcheryBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, WitcheryMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WitchesOvenBlockEntity>> WITCHES_OVEN =
            BLOCK_ENTITIES.register("witches_oven", () -> new BlockEntityType<>(WitchesOvenBlockEntity::new, Set.of(WitcheryBlocks.WITCHES_OVEN.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KettleBlockEntity>> KETTLE =
            BLOCK_ENTITIES.register("kettle", () -> new BlockEntityType<>(KettleBlockEntity::new, Set.of(WitcheryBlocks.KETTLE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistilleryBlockEntity>> DISTILLERY =
            BLOCK_ENTITIES.register("distillery", () -> new BlockEntityType<>(DistilleryBlockEntity::new, Set.of(WitcheryBlocks.DISTILLERY.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CandelabraBlockEntity>> CANDELABRA =
            BLOCK_ENTITIES.register("candelabra", () -> new BlockEntityType<>(CandelabraBlockEntity::new, Set.of(WitcheryBlocks.CANDELABRA.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoppetShelfBlockEntity>> POPPET_SHELF =
            BLOCK_ENTITIES.register("poppet_shelf", () -> new BlockEntityType<>(PoppetShelfBlockEntity::new, Set.of(WitcheryBlocks.POPPET_SHELF.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrazierBlockEntity>> BRAZIER =
            BLOCK_ENTITIES.register("brazier", () -> new BlockEntityType<>(BrazierBlockEntity::new, Set.of(WitcheryBlocks.BRAZIER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChaliceBlockEntity>> CHALICE =
            BLOCK_ENTITIES.register("chalice", () -> new BlockEntityType<>(ChaliceBlockEntity::new, Set.of(WitcheryBlocks.CHALICE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalBallBlockEntity>> CRYSTAL_BALL =
            BLOCK_ENTITIES.register("crystal_ball", () -> new BlockEntityType<>(CrystalBallBlockEntity::new, Set.of(WitcheryBlocks.CRYSTAL_BALL.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DreamCatcherBlockEntity>> DREAM_CATCHER =
            BLOCK_ENTITIES.register("dream_catcher", () -> new BlockEntityType<>(DreamCatcherBlockEntity::new, Set.of(WitcheryBlocks.DREAM_CATCHER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FumeFunnelBlockEntity>> FUME_FUNNEL =
            BLOCK_ENTITIES.register("fume_funnel", () -> new BlockEntityType<>(FumeFunnelBlockEntity::new, Set.of(WitcheryBlocks.FUME_FUNNEL.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpinningWheelBlockEntity>> SPINNING_WHEEL =
            BLOCK_ENTITIES.register("spinning_wheel", () -> new BlockEntityType<>(SpinningWheelBlockEntity::new, Set.of(WitcheryBlocks.SPINNING_WHEEL.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeechChestBlockEntity>> LEECH_CHEST =
            BLOCK_ENTITIES.register("leech_chest", () -> new BlockEntityType<>(LeechChestBlockEntity::new, Set.of(WitcheryBlocks.LEECH_CHEST.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoffinBlockEntity>> COFFIN =
            BLOCK_ENTITIES.register("coffin", () -> new BlockEntityType<>(CoffinBlockEntity::new, Set.of(WitcheryBlocks.COFFIN.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RowanDoorBlockEntity>> ROWAN_DOOR =
            BLOCK_ENTITIES.register("rowan_door", () -> new BlockEntityType<>(RowanDoorBlockEntity::new, Set.of(WitcheryBlocks.ROWAN_DOOR.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FetishBlockEntity>> FETISH =
            BLOCK_ENTITIES.register("fetish", () -> new BlockEntityType<>(FetishBlockEntity::new,
                    Set.of(WitcheryBlocks.FETISH_SCARECROW.get(),
                           WitcheryBlocks.FETISH_TRENT_EFFIGY.get(),
                           WitcheryBlocks.FETISH_WITCHS_LADDER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueGoddessBlockEntity>> STATUE_GODDESS =
            BLOCK_ENTITIES.register("statue_goddess", () -> new BlockEntityType<>(StatueGoddessBlockEntity::new, Set.of(WitcheryBlocks.STATUE_GODDESS.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AltarBlockEntity>> ALTAR =
            BLOCK_ENTITIES.register("altar", () -> new BlockEntityType<>(AltarBlockEntity::new, Set.of(WitcheryBlocks.ALTAR.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlacedItemBlockEntity>> PLACED_ITEM =
            BLOCK_ENTITIES.register("placed_item", () -> new BlockEntityType<>(PlacedItemBlockEntity::new, Set.of(WitcheryBlocks.PLACED_ITEM.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrierBlockEntity>> BARRIER =
            BLOCK_ENTITIES.register("barrier", () -> new BlockEntityType<>(BarrierBlockEntity::new, Set.of(WitcheryBlocks.BARRIER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrassperBlockEntity>> GRASSPER =
            BLOCK_ENTITIES.register("grassper", () -> new BlockEntityType<>(GrassperBlockEntity::new, Set.of(WitcheryBlocks.GRASSPER.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloodRoseBlockEntity>> BLOOD_ROSE =
            BLOCK_ENTITIES.register("blood_rose", () -> new BlockEntityType<>(BloodRoseBlockEntity::new, Set.of(WitcheryBlocks.BLOOD_ROSE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlluringSkullBlockEntity>> ALLURING_SKULL =
            BLOCK_ENTITIES.register("alluring_skull", () -> new BlockEntityType<>(AlluringSkullBlockEntity::new, Set.of(WitcheryBlocks.ALLURING_SKULL.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DemonHeartBlockEntity>> DEMON_HEART =
            BLOCK_ENTITIES.register("demon_heart", () -> new BlockEntityType<>(DemonHeartBlockEntity::new, Set.of(WitcheryBlocks.DEMON_HEART.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity>> REFILLING_CHEST =
            BLOCK_ENTITIES.register("refilling_chest", () -> new BlockEntityType<>(
                    com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity::new,
                    Set.of(WitcheryBlocks.REFILLING_CHEST.get())));
}
