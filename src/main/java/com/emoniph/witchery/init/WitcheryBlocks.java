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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class WitcheryBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WitcheryMod.MODID);

    private static <B extends Block> DeferredBlock<B> registerBlock(String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            BlockBehaviour.Properties properties) {
        return BLOCKS.registerBlock(name, factory, () -> properties);
    }

    private static <B extends Block> DeferredBlock<B> registerBlock(String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        return BLOCKS.registerBlock(name, factory, properties);
    }

    private static <B extends Block> DeferredBlock<B> registerBlock(String name,
            Function<BlockBehaviour.Properties, ? extends B> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return BLOCKS.registerBlock(name, factory, properties);
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
        return BLOCKS.registerSimpleBlock(name, () -> properties);
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name,
            Supplier<BlockBehaviour.Properties> properties) {
        return BLOCKS.registerSimpleBlock(name, properties);
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return BLOCKS.registerSimpleBlock(name, properties);
    }

    private static DeferredBlock<Block> registerSimpleBlock(String name) {
        return BLOCKS.registerSimpleBlock(name);
    }

    private static final TreeGrower ROWAN_TREE = new TreeGrower(
        "rowan",
        Optional.empty(),
        Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath("witchery", "rowan_tree"))),
        Optional.empty());
    private static final TreeGrower ALDER_TREE = new TreeGrower(
        "alder",
        Optional.empty(),
        Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath("witchery", "alder_tree"))),
        Optional.empty());
    private static final TreeGrower HAWTHORN_TREE = new TreeGrower(
        "hawthorn",
        Optional.empty(),
        Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath("witchery", "hawthorn_tree"))),
        Optional.empty());

    private static final BlockBehaviour.StatePredicate NEVER = (state, level, pos) -> false;

    private static BlockBehaviour.Properties bloodedWoolProperties() {
        return BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHITE_WOOL)
                .mapColor(MapColor.COLOR_RED);
    }

    // Wood & Rowan family
    public static final DeferredBlock<Block> ROWAN_PLANKS = registerSimpleBlock("rowan_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 3.0f));
    public static final DeferredBlock<Block> BLOODED_WOOL = registerSimpleBlock("bloodedwool",
            WitcheryBlocks::bloodedWoolProperties);
    public static final DeferredBlock<Block> BLOOD_STAINED_WOOL = registerSimpleBlock("blood_stained_wool",
            WitcheryBlocks::bloodedWoolProperties);
    public static final DeferredBlock<Block> PIT_DIRT = registerSimpleBlock("pit_dirt",
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIRT)
                    .mapColor(MapColor.DIRT)
                    .strength(0.5f)
                    .noCollision()
                    .noOcclusion()
                    .isRedstoneConductor(NEVER)
                    .isSuffocating(NEVER)
                    .isViewBlocking(NEVER));
    public static final DeferredBlock<Block> PIT_GRASS = registerSimpleBlock("pit_grass",
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    .mapColor(MapColor.GRASS)
                    .strength(0.6f)
                    .noCollision()
                    .noOcclusion()
                    .isRedstoneConductor(NEVER)
                    .isSuffocating(NEVER)
                    .isViewBlocking(NEVER));
    public static final DeferredBlock<ShadedGlassBlock> SHADED_GLASS = registerBlock("shaded_glass",
            props -> new ShadedGlassBlock(props.mapColor(state -> state.getValue(ShadedGlassBlock.COLOR).getMapColor())
                    .sound(SoundType.GLASS)
                    .strength(0.3F)
                    .noOcclusion()
                    .isRedstoneConductor(NEVER)
                    .isSuffocating(NEVER)
                    .isViewBlocking(NEVER)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<InfinityEggBlock> INFINITY_EGG = registerBlock("infinity_egg",
            InfinityEggBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DRAGON_EGG)
                    .strength(3.0F, 15.0F)
                    .lightLevel(state -> 2));
    public static final DeferredBlock<WitcheryWoodStairBlock> ROWAN_STAIRS = registerBlock("rowan_stairs",
            props -> new WitcheryWoodStairBlock(ROWAN_PLANKS.get().defaultBlockState(),
                    props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(5.0f, 20.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<WitcheryWoodSlabBlock> ROWAN_SLAB = registerBlock("rowan_slab",
            props -> new WitcheryWoodSlabBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 5.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<RowanDoorBlock> ROWAN_DOOR = registerBlock("rowan_door",
            props -> new RowanDoorBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(5.0f).noOcclusion()),
            BlockBehaviour.Properties.of());

    public static final DeferredBlock<StockadeBlock> STOCKADE = registerBlock("stockade",
            props -> new StockadeBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD)
                    .strength(25.0f, 20.0f).noOcclusion()),
            BlockBehaviour.Properties.of());

    // Ice blocks
    public static final DeferredBlock<PerpetualIceBlock> PERPETUAL_ICE = registerBlock("perpetual_ice",
            PerpetualIceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .friction(0.98f)
                    .strength(2.0f, 5.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isRedstoneConductor(NEVER)
                    .isSuffocating(NEVER)
                    .isViewBlocking(NEVER));
    public static final DeferredBlock<PerpetualIceStairBlock> ICE_STAIRS = registerBlock("ice_stairs",
            props -> new PerpetualIceStairBlock(PERPETUAL_ICE.get().defaultBlockState(),
                    props.mapColor(MapColor.ICE).friction(0.98f).strength(2.0f, 5.0f).sound(SoundType.GLASS)
                            .noOcclusion().isRedstoneConductor(NEVER).isSuffocating(NEVER).isViewBlocking(NEVER)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<PerpetualIceSlabBlock> ICE_SLAB = registerBlock("ice_slab",
            props -> new PerpetualIceSlabBlock(props.mapColor(MapColor.ICE).friction(0.98f)
                    .strength(2.0f, 5.0f).sound(SoundType.GLASS)
                    .noOcclusion().isRedstoneConductor(NEVER).isSuffocating(NEVER).isViewBlocking(NEVER)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<DoorBlock> ICE_DOOR = registerBlock("ice_door",
            props -> new DoorBlock(BlockSetType.OAK, props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<FenceBlock> ICE_FENCE = registerBlock("ice_fence",
            props -> new FenceBlock(props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<FenceGateBlock> ICE_GATE = registerBlock("ice_gate",
            props -> new FenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK,
                    props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(0.5f).noOcclusion().friction(0.98f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<StockadeBlock> ICE_STOCKADE = registerBlock("ice_stockade",
            props -> new StockadeBlock(props.mapColor(MapColor.ICE).sound(SoundType.GLASS)
                    .friction(0.98f).strength(25.0f, 20.0f).noOcclusion()
                    .isRedstoneConductor(NEVER).isSuffocating(NEVER).isViewBlocking(NEVER)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<PressurePlateBlock> ICE_PRESSURE_PLATE = registerBlock("ice_pressure_plate",
            props -> new PerpetualIcePressurePlateBlock(BlockSetType.IRON,
                    props.mapColor(MapColor.ICE).sound(SoundType.GLASS).strength(2.0f, 5.0f)
                            .noCollision().friction(0.98f),
                    true),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<PressurePlateBlock> SNOW_PRESSURE_PLATE = registerBlock("snow_pressure_plate",
            props -> new PerpetualIcePressurePlateBlock(BlockSetType.OAK,
                    props.mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(0.2f).noCollision(),
                    false),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<PressurePlateBlock> CURSED_SNOW_PRESSURE_PLATE = registerBlock("cursed_snow_pressure_plate",
            props -> new PerpetualIcePressurePlateBlock(BlockSetType.OAK,
                    props.mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(0.5f).noCollision(),
                    false),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<StairBlock> SNOW_STAIRS = registerBlock("snow_stairs",
            props -> new StairBlock(net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState(),
                    props.mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(0.2f).noOcclusion()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<SlabBlock> SNOW_SLAB = registerBlock("snow_slab",
            props -> new SlabBlock(props.mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(0.2f).noOcclusion()),
            BlockBehaviour.Properties.of());

    // Wood logs/leaves
    public static final DeferredBlock<RotatedPillarBlock> ROWAN_LOG = registerBlock("rowan_log",
            WitcheryLogBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<Block> ROWAN_WOOD = registerSimpleBlock("rowan_wood",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<RowanLeavesBlock> ROWAN_LEAVES = registerBlock("rowan_leaves",
            RowanLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.2f).noOcclusion().randomTicks());
    public static final DeferredBlock<SaplingBlock> ROWAN_SAPLING = registerBlock("rowan_sapling",
            props -> new SaplingBlock(ROWAN_TREE, props.mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision().randomTicks()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<RotatedPillarBlock> ALDER_LOG = registerBlock("alder_log",
            WitcheryLogBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<Block> ALDER_PLANKS = registerSimpleBlock("alder_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 3.0f));
    public static final DeferredBlock<WitcheryWoodStairBlock> ALDER_STAIRS = registerBlock("alder_stairs",
            props -> new WitcheryWoodStairBlock(ALDER_PLANKS.get().defaultBlockState(),
                    props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(5.0f, 20.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<WitcheryWoodSlabBlock> ALDER_SLAB = registerBlock("alder_slab",
            props -> new WitcheryWoodSlabBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 5.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<AlderDoorBlock> ALDER_DOOR = registerBlock("alder_door",
            props -> new AlderDoorBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.0f).noOcclusion()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<RowanLeavesBlock> ALDER_LEAVES = registerBlock("alder_leaves",
            RowanLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.2f).noOcclusion().randomTicks());
    public static final DeferredBlock<SaplingBlock> ALDER_SAPLING = registerBlock("alder_sapling",
            props -> new SaplingBlock(ALDER_TREE, props.mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision().randomTicks()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<RotatedPillarBlock> HAWTHORN_LOG = registerBlock("hawthorn_log",
            WitcheryLogBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f));
    public static final DeferredBlock<Block> HAWTHORN_PLANKS = registerSimpleBlock("hawthorn_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 3.0f));
    public static final DeferredBlock<WitcheryWoodStairBlock> HAWTHORN_STAIRS = registerBlock("hawthorn_stairs",
            props -> new WitcheryWoodStairBlock(HAWTHORN_PLANKS.get().defaultBlockState(),
                    props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(5.0f, 20.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<WitcheryWoodSlabBlock> HAWTHORN_SLAB = registerBlock("hawthorn_slab",
            props -> new WitcheryWoodSlabBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 5.0f)),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<HawthornDoorBlock> HAWTHORN_DOOR = registerBlock("hawthorn_door",
            props -> new HawthornDoorBlock(props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.0f).noOcclusion()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<RowanLeavesBlock> HAWTHORN_LEAVES = registerBlock("hawthorn_leaves",
            RowanLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.2f).noOcclusion().randomTicks());
    public static final DeferredBlock<SaplingBlock> HAWTHORN_SAPLING = registerBlock("hawthorn_sapling",
            props -> new SaplingBlock(HAWTHORN_TREE, props.mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision().randomTicks()),
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
    public static final DeferredBlock<BelladonnaBlock>  BELLADONNA      = registerBlock("belladonna",  BelladonnaBlock::new,  () -> cropProps());
    public static final DeferredBlock<MandrakeBlock>    MANDRAKE        = registerBlock("mandrake",    MandrakeBlock::new,    () -> cropProps());
    public static final DeferredBlock<MindrakeBlock>    MINDRAKE        = registerBlock("mindrake",    MindrakeBlock::new,    () -> cropProps());
    public static final DeferredBlock<ArtichokeBlock>   ARTICHOKE       = registerBlock("artichoke",   ArtichokeBlock::new,   () -> cropProps());
    public static final DeferredBlock<CottonBlock>      COTTON_CROP     = registerBlock("cotton",      CottonBlock::new,      () -> cropProps());
    public static final DeferredBlock<SnowbellBlock>    SNOWBELL        = registerBlock("snowbell",    SnowbellBlock::new,    () -> cropProps());
    public static final DeferredBlock<WormwoodBlock>    WORMWOOD_PLANT  = registerBlock("wormwood",    WormwoodBlock::new,    () -> cropProps());
    public static final DeferredBlock<GarlicCropBlock>    GARLIC_CROP     = registerBlock("garlic_crop",    GarlicCropBlock::new,    () -> cropProps());
    public static final DeferredBlock<WolfsbaneCropBlock> WOLFSBANE_CROP  = registerBlock("wolfsbane_crop", WolfsbaneCropBlock::new, () -> cropProps());
    public static final DeferredBlock<GarlicGarlandBlock> GARLIC_GARLAND = registerBlock("garlic_garland",
            GarlicGarlandBlock::new, () -> BlockBehaviour.Properties.of().sound(net.minecraft.world.level.block.SoundType.GRASS)
                    .noOcclusion().noCollision().strength(0.2f));

    // Spirit World plants
    public static final DeferredBlock<CottonBlock> WISPY_COTTON = registerBlock("wispy_cotton",
            CottonBlock::new, () -> spiritCottonProps(4));
    public static final DeferredBlock<Block> DISTURBED_COTTON = registerSimpleBlock("disturbed_cotton",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).sound(SoundType.GRASS)
                    .noOcclusion().noCollision().instabreak());

    // Plants
    public static final DeferredBlock<BloodRoseBlock> BLOOD_ROSE = registerBlock("blood_rose",
            BloodRoseBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.GRASS)
                    .strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<EmberMossBlock> EMBER_MOSS = registerBlock("ember_moss",
            EmberMossBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).lightLevel(s -> 6).noOcclusion().noCollision().randomTicks().noLootTable());
    public static final DeferredBlock<GlintWeedBlock> GLINT_WEED = registerBlock("glint_weed",
            GlintWeedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).noOcclusion().noCollision().lightLevel(s -> 15).randomTicks());
    public static final DeferredBlock<SpanishMossBlock> SPANISH_MOSS = registerBlock("spanish_moss",
            SpanishMossBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.2f).noOcclusion().noCollision().randomTicks());
    public static final DeferredBlock<BrambleBlock> BRAMBLE = registerBlock("bramble",
            BrambleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(20.0f, 1000.0f).noOcclusion().noCollision());
    public static final DeferredBlock<VoidBrambleBlock> VOID_BRAMBLE = registerBlock("void_bramble",
            VoidBrambleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GRASS).strength(-1.0f, 1000.0f).noOcclusion().noCollision().lightLevel(s -> 2));
    public static final DeferredBlock<BlightHeartBlock> BLIGHT_HEART = registerBlock("blight_heart",
            BlightHeartBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.WART_BLOCK)
                    .strength(6.0f, 12.0f).randomTicks().lightLevel(s -> 2).noOcclusion());
    public static final DeferredBlock<BlightFibreBlock> BLIGHT_FIBRE = registerBlock("blight_fibre",
            BlightFibreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.WART_BLOCK)
                    .strength(1.0f).randomTicks().noOcclusion().noCollision().noLootTable());
    public static final DeferredBlock<BlightedTerrainBlock> BLIGHTED_SOIL = registerBlock("blighted_soil",
            BlightedTerrainBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.ROOTED_DIRT)
                    .strength(1.0f, 2.0f).randomTicks());
    public static final DeferredBlock<BlightedTerrainBlock> BLIGHTED_STONE = registerBlock("blighted_stone",
            BlightedTerrainBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.STONE)
                    .strength(2.0f, 6.0f).randomTicks());
    public static final DeferredBlock<BlightedLogBlock> BLIGHTED_LOG = registerBlock("blighted_log",
            BlightedLogBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.WOOD)
                    .strength(2.0f, 4.0f).randomTicks());
    public static final DeferredBlock<LeapingLilyBlock> LEAPING_LILY = registerBlock("leaping_lily",
            LeapingLilyBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).lightLevel(s -> 6).noOcclusion().noCollision());
    public static final DeferredBlock<GrassperBlock> GRASSPER = registerBlock("grassper",
            GrassperBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.5f).noOcclusion().noCollision());
    public static final DeferredBlock<GlowGlobeBlock> GLOWGLOBE = registerBlock("glowglobe",
            GlowGlobeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS)
                    .strength(0.0f).lightLevel(s -> 14).noOcclusion().noCollision().noLootTable());

    // Machines / Multiblocks
    public static final DeferredBlock<KettleBlock>      KETTLE        = registerBlock("kettle",         KettleBlock::new,       BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0f, 10.0f).noOcclusion());
    public static final DeferredBlock<WitchCauldronBlock> WITCH_CAULDRON = registerBlock("witch_cauldron",
            WitchCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.CAULDRON));
    public static final DeferredBlock<DistilleryBlock>  DISTILLERY    = registerBlock("distillery",     DistilleryBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5f, 17.5f).lightLevel(state -> state.getValue(DistilleryBlock.LIT) ? 6 : 0).noOcclusion());
    public static final DeferredBlock<SpinningWheelBlock> SPINNING_WHEEL = registerBlock("spinning_wheel", SpinningWheelBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.5f, 17.5f).noOcclusion());
    public static final DeferredBlock<WitchesOvenBlock> WITCHES_OVEN  = registerBlock("witches_oven",   WitchesOvenBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.5f, 17.5f).lightLevel(state -> state.getValue(WitchesOvenBlock.LIT) ? 13 : 0).noOcclusion());
    public static final DeferredBlock<AltarBlock>       ALTAR         = registerBlock("altar",          AltarBlock::new,        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.0f, 10.0f));
    // 1.7.10 BlockFumeFunnel: Material.iron, metal sound, setHardness(3.5F).
    public static final DeferredBlock<FumeFunnelBlock>  FUME_FUNNEL   = registerBlock("fume_funnel",    FumeFunnelBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5f, 17.5f).noOcclusion());
    public static final DeferredBlock<FumeFunnelBlock>  FILTERED_FUME_FUNNEL = registerBlock("filtered_fume_funnel",
            props -> new FumeFunnelBlock(props, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5f, 17.5f).noOcclusion());
    public static final DeferredBlock<BrazierBlock>     BRAZIER       = registerBlock("brazier",        BrazierBlock::new,      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5f, 17.5f).noOcclusion());

    // Storage / Containers
    public static final DeferredBlock<PoppetShelfBlock> POPPET_SHELF  = registerBlock("poppet_shelf",   PoppetShelfBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.5f, 12.5f).noOcclusion());
    public static final DeferredBlock<LeechChestBlock>  LEECH_CHEST   = registerBlock("leech_chest",    LeechChestBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(25.0f, 1000.0f).noOcclusion());
    public static final DeferredBlock<RefillingChestBlock> REFILLING_CHEST = registerBlock("refilling_chest", RefillingChestBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F).noLootTable());

    // Decorative / Ritual
    public static final DeferredBlock<CandelabraBlock>  CANDELABRA    = registerBlock("candelabra",     CandelabraBlock::new,   BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.0f, 10.0f).lightLevel(s -> 15).noOcclusion());
    public static final DeferredBlock<ChaliceBlock>     CHALICE       = registerBlock("chalice",        ChaliceBlock::new,      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.0f, 15.0f).noOcclusion());
    public static final DeferredBlock<CrystalBallBlock> CRYSTAL_BALL  = registerBlock("crystal_ball",   CrystalBallBlock::new,  BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).strength(2.0f, 10.0f).noOcclusion());
    public static final DeferredBlock<DreamCatcherBlock> DREAM_CATCHER = registerBlock("dream_catcher", DreamCatcherBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f, 5.0f).noOcclusion());
    public static final DeferredBlock<AlluringSkullBlock> ALLURING_SKULL = registerBlock("alluring_skull",
            AlluringSkullBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(-1.0f, 1000.0f).lightLevel(state -> 8).noOcclusion());
    public static final DeferredBlock<DemonHeartBlock> DEMON_HEART = registerBlock("demon_heart",
            DemonHeartBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.STONE).strength(1.0f, 5.0f).lightLevel(state -> 3).noOcclusion());
    // Mounted trophy heads (1.7.10 BlockWolfHead, skullType 0=wolf / 1=hellhound) — directional,
    // floor/wall placed, BESR-rendered (RenderShape.INVISIBLE).
    public static final DeferredBlock<WolfHeadBlock> WOLF_HEAD = registerBlock("wolf_head",
            WolfHeadBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<HellhoundHeadBlock> HELLHOUND_HEAD = registerBlock("hellhound_head",
            HellhoundHeadBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_SCARECROW      = registerBlock("fetish_scarecrow",     FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.5f, 100000.0f).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_TRENT_EFFIGY   = registerBlock("fetish_trent_effigy",  FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.5f, 100000.0f).noOcclusion());
    public static final DeferredBlock<FetishBlock>      FETISH_WITCHS_LADDER  = registerBlock("fetish_witchs_ladder", FetishBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.5f, 100000.0f).noOcclusion());
    public static final DeferredBlock<StatueGoddessBlock> STATUE_GODDESS      = registerBlock("statue_goddess",       StatueGoddessBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.5f, 1000.0f).noOcclusion());
    public static final DeferredBlock<WickerBundleBlock> WICKER_BUNDLE = registerBlock("wicker_bundle",
            WickerBundleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.5f).noOcclusion());
    public static final DeferredBlock<WickerBundleBlock> BLOODIED_WICKER_BUNDLE = registerBlock("bloodied_wicker_bundle",
            props -> new WickerBundleBlock(props, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.5f).noOcclusion());
    public static final DeferredBlock<PlacedItemBlock>  PLACED_ITEM    = registerBlock("placed_item",   PlacedItemBlock::new,   BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<WolfAltarBlock>   WOLF_ALTAR     = registerBlock("wolf_altar",    WolfAltarBlock::new,    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.5f));
    public static final DeferredBlock<StatueHobgoblinPatronBlock> STATUE_HOBGOBLIN_PATRON = registerBlock("statue_hobgoblin_patron", StatueHobgoblinPatronBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(3.0f).noOcclusion());
    // 1.7.10 BlockSilverVat: Material.iron, setHardness(8.0F) -> resistance 8*5=40 (auto), metal sound.
    public static final DeferredBlock<SilverVatBlock>   SILVER_VAT     = registerBlock("silver_vat",    SilverVatBlock::new,    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(8.0f, 40.0f).noOcclusion());
    public static final DeferredBlock<CoffinBlock>      COFFIN         = registerBlock("coffin",        CoffinBlock::new,       BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.0f).noOcclusion());
    public static final DeferredBlock<com.emoniph.witchery.blocks.BloodCrucibleBlock> BLOOD_CRUCIBLE = registerBlock("blood_crucible",
            com.emoniph.witchery.blocks.BloodCrucibleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2.5f, 1000.0f).noOcclusion());

    // Mirror World
    public static final DeferredBlock<com.emoniph.witchery.blocks.MirrorWallBlock> MIRROR_WALL = registerBlock("mirror_wall",
            com.emoniph.witchery.blocks.MirrorWallBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).sound(SoundType.GLASS)
                    .strength(-1.0F, 3_600_000.0F).noLootTable().lightLevel(s -> 4));
    public static final DeferredBlock<com.emoniph.witchery.blocks.MirrorBlock> MIRROR = registerBlock("mirror",
            com.emoniph.witchery.blocks.MirrorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).sound(SoundType.GLASS)
                    .strength(1.5f, 6.0f).noOcclusion().lightLevel(s -> 8));

    // Portals / Dimensional
    public static final DeferredBlock<SpiritPortalBlock> SPIRIT_PORTAL = registerBlock("spirit_portal", SpiritPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.GLASS).strength(-1.0f).lightLevel(s -> 10).noOcclusion().noCollision().noLootTable());
    public static final DeferredBlock<Block> TORMENT_STONE = registerSimpleBlock("torment_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.STONE).strength(-1.0F, 3600000.0F).noLootTable());
    public static final DeferredBlock<TormentPortalBlock> TORMENT_PORTAL = registerBlock("torment_portal", TormentPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.GLASS).strength(-1.0F, 3600000.0F).lightLevel(s -> 12).noOcclusion().noCollision().noLootTable());
    public static final DeferredBlock<com.emoniph.witchery.blocks.SunCollectorBlock> SUN_COLLECTOR = registerBlock("sun_collector",
            com.emoniph.witchery.blocks.SunCollectorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).sound(SoundType.GLASS).strength(3.5f).lightLevel(s -> 4).noOcclusion());

    // Chalk circle marks
    public static final DeferredBlock<ChalkCircleBlock> CIRCLE = registerBlock("circle",
            ChalkCircleBlock::new,
            BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.GOLD));
    public static final DeferredBlock<ChalkCircleBlock> CIRCLE_GLYPH = registerBlock("circle_glyph",
            ChalkCircleBlock::new,
            BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.STONE));
    public static final DeferredBlock<ChalkCircleBlock> CIRCLE_OTHERWHERE = registerBlock("circle_otherwhere",
            ChalkCircleBlock::new,
            BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<ChalkCircleBlock> CIRCLE_INFERNAL = registerBlock("circle_infernal",
            ChalkCircleBlock::new,
            BlockBehaviour.Properties.of().strength(0.0f).noOcclusion().noCollision().instabreak().mapColor(MapColor.COLOR_RED));
    public static final DeferredBlock<CritterSnareBlock> CRITTER_SNARE = registerBlock("critter_snare",
            CritterSnareBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(0.5f).noOcclusion().noCollision());
    public static final DeferredBlock<BeartrapBlock> BEARTRAP = registerBlock("beartrap",
            BeartrapBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5.0f, 10.0f).noOcclusion().noCollision());
    public static final DeferredBlock<BeartrapBlock> WOLFTRAP = registerBlock("wolftrap",
            props -> new BeartrapBlock(props, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5.0f, 10.0f).noOcclusion().noCollision());
    public static final DeferredBlock<Block> PLANT_MINE       = registerSimpleBlock("plant_mine",       BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<DoorBlock> WITCH_DOOR = registerBlock("witch_door",
            props -> new DoorBlock(BlockSetType.OAK, props.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(3.0f).noOcclusion()),
            BlockBehaviour.Properties.of());
    public static final DeferredBlock<BarrierBlock> BARRIER   = registerBlock("barrier", BarrierBlock::new,
            BlockBehaviour.Properties.of().strength(-1.0f, 3_600_000.0f).noOcclusion().noLootTable());
    public static final DeferredBlock<ForceBlock> FORCE      = registerBlock("force", ForceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE)
                    .strength(-1.0F, 3600000.0F).noOcclusion().noLootTable()
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<LightBlock> LIGHT = registerBlock("light", LightBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .replaceable()
                    .air()
                    .lightLevel(s -> 15)
                    .noCollision()
                    .noOcclusion()
                    .noLootTable()
                    .isRedstoneConductor(NEVER)
                    .isSuffocating(NEVER)
                    .isViewBlocking(NEVER));
    public static final DeferredBlock<FlowingSpiritBlock> FLOWING_SPIRIT = registerBlock("flowing_spirit",
            FlowingSpiritBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.0f).noOcclusion().noCollision());
    public static final DeferredBlock<HollowTearsBlock> HOLLOW_TEARS = registerBlock("hollow_tears",
            HollowTearsBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).sound(SoundType.GRASS)
                    .strength(100.0F).noOcclusion().noCollision().noLootTable());
}
