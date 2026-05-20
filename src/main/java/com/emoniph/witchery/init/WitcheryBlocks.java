package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.*;
import com.emoniph.witchery.blocks.RowanLeavesBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class WitcheryBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WitcheryMod.MODID);

    private static final TreeGrower ROWAN_TREE = new TreeGrower(
        "rowan",
        Optional.empty(),
        Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath("witchery", "rowan_tree"))),
        Optional.empty());

    // Wood & Rowan family
    public static final DeferredBlock<Block> ROWAN_PLANKS = BLOCKS.registerSimpleBlock("rowan_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 3.0f));
    public static final DeferredBlock<RowanDoorBlock> ROWAN_DOOR = BLOCKS.registerBlock("rowan_door",
            props -> new RowanDoorBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.0f).noOcclusion()),
            BlockBehaviour.Properties.of());

    // Ice blocks
    public static final DeferredBlock<DoorBlock> ICE_DOOR = BLOCKS.registerBlock("ice_door",
            props -> new DoorBlock(BlockSetType.OAK, props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<FenceBlock> ICE_FENCE = BLOCKS.registerBlock("ice_fence",
            props -> new FenceBlock(props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<FenceGateBlock> ICE_GATE = BLOCKS.registerBlock("ice_gate",
            props -> new FenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK,
                    props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<PressurePlateBlock> ICE_PRESSURE_PLATE = BLOCKS.registerBlock("ice_pressure_plate",
            props -> new PressurePlateBlock(BlockSetType.IRON, props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noCollision().friction(0.98f)),
            BlockBehaviour.Properties.of());

    // Wood logs/leaves
    public static final DeferredBlock<RotatedPillarBlock> ROWAN_LOG = BLOCKS.registerBlock("rowan_log",
            RotatedPillarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<Block> ROWAN_WOOD = BLOCKS.registerSimpleBlock("rowan_wood",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<RowanLeavesBlock> ROWAN_LEAVES = BLOCKS.registerBlock("rowan_leaves",
            RowanLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.2f).noOcclusion().randomTicks());
    public static final DeferredBlock<SaplingBlock> ROWAN_SAPLING = BLOCKS.registerBlock("rowan_sapling",
            props -> new SaplingBlock(ROWAN_TREE, props.mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision().randomTicks()),
            BlockBehaviour.Properties.of());

    // Crops
    private static BlockBehaviour.Properties cropProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                .noOcclusion().noCollision().randomTicks().instabreak();
    }
    private static BlockBehaviour.Properties spiritCottonProps(int lightLevel) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).sound(SoundType.GRASS)
                .noOcclusion().noCollision().randomTicks().instabreak().lightLevel(s -> lightLevel);
    }
    public static final DeferredBlock<BelladonnaBlock>  BELLADONNA      = BLOCKS.registerBlock("belladonna",  BelladonnaBlock::new,  () -> cropProps());
    public static final DeferredBlock<MandrakeBlock>    MANDRAKE        = BLOCKS.registerBlock("mandrake",    MandrakeBlock::new,    () -> cropProps());
    public static final DeferredBlock<ArtichokeBlock>   ARTICHOKE       = BLOCKS.registerBlock("artichoke",   ArtichokeBlock::new,   () -> cropProps());
    public static final DeferredBlock<CottonBlock>      COTTON_CROP     = BLOCKS.registerBlock("cotton",      CottonBlock::new,      () -> cropProps());
    public static final DeferredBlock<WitchCropBlock>   WITCH_CROP_BLOCK= BLOCKS.registerBlock("witch_crop",  WitchCropBlock::new,   () -> cropProps());
    public static final DeferredBlock<SnowbellBlock>    SNOWBELL        = BLOCKS.registerBlock("snowbell",    SnowbellBlock::new,    () -> cropProps());
    public static final DeferredBlock<WormwoodBlock>    WORMWOOD_PLANT  = BLOCKS.registerBlock("wormwood",    WormwoodBlock::new,    () -> cropProps());

    // Spirit World plants
    public static final DeferredBlock<CottonBlock> WISPY_COTTON = BLOCKS.registerBlock("wispy_cotton",
            CottonBlock::new, () -> spiritCottonProps(4));
    public static final DeferredBlock<Block> DISTURBED_COTTON = BLOCKS.registerSimpleBlock("disturbed_cotton",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).sound(SoundType.GRASS)
                    .noOcclusion().noCollision().instabreak());

    // Plants
    public static final DeferredBlock<BloodRoseBlock> BLOOD_ROSE = BLOCKS.registerBlock("blood_rose",
            BloodRoseBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.GRASS)
                    .strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<Block> EMBER_MOSS     = BLOCKS.registerSimpleBlock("ember_moss",    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<Block> GLINT_WEED     = BLOCKS.registerSimpleBlock("glint_weed",    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision().lightLevel(s -> 7));
    public static final DeferredBlock<SpanishMossBlock> SPANISH_MOSS = BLOCKS.registerBlock("spanish_moss",
            SpanishMossBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.2f).noOcclusion().randomTicks());
    public static final DeferredBlock<BrambleBlock> BRAMBLE = BLOCKS.registerBlock("bramble",
            BrambleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.2f).noOcclusion().noCollision());
    public static final DeferredBlock<VoidBrambleBlock> VOID_BRAMBLE = BLOCKS.registerBlock("void_bramble",
            VoidBrambleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GRASS).strength(0.2f).noOcclusion().noCollision().lightLevel(s -> 2));
    public static final DeferredBlock<LeapingLilyBlock> LEAPING_LILY = BLOCKS.registerBlock("leaping_lily",
            LeapingLilyBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).lightLevel(s -> 6).noOcclusion().noCollision());
    public static final DeferredBlock<GrassperBlock> GRASSPER = BLOCKS.registerBlock("grassper",
            GrassperBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.5f).noOcclusion().noCollision());
    public static final DeferredBlock<GlowGlobeBlock> GLOWGLOBE = BLOCKS.registerBlock("glowglobe",
            GlowGlobeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).lightLevel(s -> 14).noOcclusion().noCollision());

    // Machines / Multiblocks
    public static final DeferredBlock<KettleBlock>      KETTLE        = BLOCKS.registerBlock("kettle",         KettleBlock::new,       BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.0f).noOcclusion());
    public static final DeferredBlock<DistilleryBlock>  DISTILLERY    = BLOCKS.registerBlock("distillery",     DistilleryBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5f).noOcclusion());
    public static final DeferredBlock<SpinningWheelBlock> SPINNING_WHEEL = BLOCKS.registerBlock("spinning_wheel", SpinningWheelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f).noOcclusion());
    public static final DeferredBlock<WitchesOvenBlock> WITCHES_OVEN  = BLOCKS.registerBlock("witches_oven",   WitchesOvenBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.5f).noOcclusion());
    public static final DeferredBlock<AltarBlock>       ALTAR         = BLOCKS.registerBlock("altar",          AltarBlock::new,        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(5.0f));
    public static final DeferredBlock<FumeFunnelBlock>  FUME_FUNNEL   = BLOCKS.registerBlock("fume_funnel",    FumeFunnelBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.5f).noOcclusion());
    public static final DeferredBlock<BrazierBlock>     BRAZIER       = BLOCKS.registerBlock("brazier",        BrazierBlock::new,      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0f).noOcclusion());

    // Storage / Containers
    public static final DeferredBlock<PoppetShelfBlock> POPPET_SHELF  = BLOCKS.registerBlock("poppet_shelf",   PoppetShelfBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f).noOcclusion());
    public static final DeferredBlock<LeechChestBlock>  LEECH_CHEST   = BLOCKS.registerBlock("leech_chest",    LeechChestBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.5f).noOcclusion());
    public static final DeferredBlock<RefillingChestBlock> REFILLING_CHEST = BLOCKS.registerBlock("refilling_chest", RefillingChestBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F).noLootTable());

    // Decorative / Ritual
    public static final DeferredBlock<CandelabraBlock>  CANDELABRA    = BLOCKS.registerBlock("candelabra",     CandelabraBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0f).lightLevel(s -> 15).noOcclusion());
    public static final DeferredBlock<ChaliceBlock>     CHALICE       = BLOCKS.registerBlock("chalice",        ChaliceBlock::new,      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.0f).noOcclusion());
    public static final DeferredBlock<CrystalBallBlock> CRYSTAL_BALL  = BLOCKS.registerBlock("crystal_ball",   CrystalBallBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).strength(2.0f).noOcclusion());
    public static final DeferredBlock<DreamCatcherBlock> DREAM_CATCHER = BLOCKS.registerBlock("dream_catcher", DreamCatcherBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<AlluringSkullBlock> ALLURING_SKULL = BLOCKS.registerBlock("alluring_skull",
            AlluringSkullBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(1.0f).lightLevel(state -> 8).noOcclusion());
    public static final DeferredBlock<DemonHeartBlock> DEMON_HEART = BLOCKS.registerBlock("demon_heart",
            DemonHeartBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.STONE).strength(1.0f).lightLevel(state -> 3).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_SCARECROW      = BLOCKS.registerBlock("fetish_scarecrow",     FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_TRENT_EFFIGY   = BLOCKS.registerBlock("fetish_trent_effigy",  FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_WITCHS_LADDER  = BLOCKS.registerBlock("fetish_witchs_ladder", FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<StatueGoddessBlock> STATUE_GODDESS      = BLOCKS.registerBlock("statue_goddess",       StatueGoddessBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.0f).noOcclusion());
    public static final DeferredBlock<Block>            WICKER_BUNDLE  = BLOCKS.registerSimpleBlock("wicker_bundle",   BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.5f).noOcclusion());
    public static final DeferredBlock<PlacedItemBlock>  PLACED_ITEM    = BLOCKS.registerBlock("placed_item",   PlacedItemBlock::new,   BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<WolfAltarBlock>   WOLF_ALTAR     = BLOCKS.registerBlock("wolf_altar",    WolfAltarBlock::new,    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.5f));
    public static final DeferredBlock<StatueHobgoblinPatronBlock> STATUE_HOBGOBLIN_PATRON = BLOCKS.registerBlock("statue_hobgoblin_patron", StatueHobgoblinPatronBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.0f).noOcclusion());
    public static final DeferredBlock<SilverVatBlock>   SILVER_VAT     = BLOCKS.registerBlock("silver_vat",    SilverVatBlock::new,    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.0f));
    public static final DeferredBlock<CoffinBlock>      COFFIN         = BLOCKS.registerBlock("coffin",        CoffinBlock::new,       BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.5f).noOcclusion());

    // Portals / Dimensional
    public static final DeferredBlock<SpiritPortalBlock> SPIRIT_PORTAL = BLOCKS.registerBlock("spirit_portal", SpiritPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).strength(-1.0f).lightLevel(s -> 10).noOcclusion().noCollision().noLootTable());
    public static final DeferredBlock<Block> TORMENT_STONE = BLOCKS.registerSimpleBlock("torment_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.STONE).strength(-1.0F, 3600000.0F).noLootTable());
    public static final DeferredBlock<TormentPortalBlock> TORMENT_PORTAL = BLOCKS.registerBlock("torment_portal", TormentPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.GLASS).strength(-1.0F, 3600000.0F).lightLevel(s -> 12).noOcclusion().noCollision().noLootTable());

    // Chalk circle marks
    public static final DeferredBlock<Block> CIRCLE           = BLOCKS.registerSimpleBlock("circle",           BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.GOLD));
    public static final DeferredBlock<Block> CIRCLE_GLYPH     = BLOCKS.registerSimpleBlock("circle_glyph",     BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.STONE));
    public static final DeferredBlock<Block> CIRCLE_OTHERWHERE = BLOCKS.registerSimpleBlock("circle_otherwhere", BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> CIRCLE_INFERNAL  = BLOCKS.registerSimpleBlock("circle_infernal",  BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.COLOR_RED));
    public static final DeferredBlock<Block> CRITTER_SNARE    = BLOCKS.registerSimpleBlock("critter_snare",    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(0.5f).noOcclusion());
    public static final DeferredBlock<Block> PLANT_MINE       = BLOCKS.registerSimpleBlock("plant_mine",       BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<DoorBlock> WITCH_DOOR = BLOCKS.registerBlock("witch_door",
            props -> new DoorBlock(BlockSetType.OAK, props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.0f).noOcclusion()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<BarrierBlock> BARRIER   = BLOCKS.registerBlock("barrier", BarrierBlock::new,
            BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000.0f).noOcclusion().noLootTable());
    public static final DeferredBlock<Block> FORCE            = BLOCKS.registerSimpleBlock("force",            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(-1.0F, 3600000.0F).noLootTable());
    public static final DeferredBlock<FlowingSpiritBlock> FLOWING_SPIRIT = BLOCKS.registerBlock("flowing_spirit",
            FlowingSpiritBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.0f).noOcclusion().noCollision());
}
