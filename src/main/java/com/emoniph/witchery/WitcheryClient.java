package com.emoniph.witchery;

import com.google.common.reflect.TypeToken;
import com.emoniph.witchery.client.model.*;
import com.emoniph.witchery.client.model.LeonardModel;
import com.emoniph.witchery.client.model.VillageGuardModel;
import com.emoniph.witchery.client.model.WolfmanModel;
import com.emoniph.witchery.client.renderer.WereVillagerRenderer;
import com.emoniph.witchery.client.renderer.WolfmanRenderer;
import com.emoniph.witchery.client.renderer.VillageGuardRenderer;
import com.emoniph.witchery.client.model.LilithModel;
import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.client.model.SpiritModel;
import com.emoniph.witchery.client.model.PoppetShelfCompassAngleProperty;
import com.emoniph.witchery.client.renderer.*;
import com.emoniph.witchery.client.renderer.ElleRenderer;
import com.emoniph.witchery.client.model.BabaYagaModel;
import com.emoniph.witchery.client.model.GoblinGulgModel;
import com.emoniph.witchery.client.model.GoblinMogModel;
import com.emoniph.witchery.client.renderer.BabaYagaRenderer;
import com.emoniph.witchery.client.renderer.VampireRenderer;
import com.emoniph.witchery.client.renderer.BoltRenderer;
import com.emoniph.witchery.client.renderer.GoblinGulgRenderer;
import com.emoniph.witchery.client.renderer.GoblinMogRenderer;
import com.emoniph.witchery.client.renderer.LeonardRenderer;
import com.emoniph.witchery.client.renderer.LilithRenderer;
import com.emoniph.witchery.client.renderer.NightmareRenderer;
import com.emoniph.witchery.client.renderer.SpiritRenderer;
import com.emoniph.witchery.client.color.BrewBagTint;
import com.emoniph.witchery.client.screen.DistilleryScreen;
import com.emoniph.witchery.client.screen.ObservationsScreen;
import com.emoniph.witchery.client.screen.SpinningWheelScreen;
import com.emoniph.witchery.client.screen.WitchesOvenScreen;
import com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity;
import com.emoniph.witchery.blocks.entity.WitchCauldronBlockEntity;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.fluid.BrewLiquidFluid;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.network.OpenObservationsBookPayload;
import com.emoniph.witchery.network.ScryingCameraPayload;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// MOD bus events registered via IEventBus in WitcheryMod (dist-guarded).
// Client-only network handlers live here to keep Minecraft.getInstance() out of shared code.
// Game bus events in WitcheryClientEvents.
public class WitcheryClient {
    public static final List<Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>>> LAYER_DEFS = List.of(
        Map.entry(WitchesOvenModel.LAYER_LOCATION,    WitchesOvenModel::createBodyLayer),
        Map.entry(DistilleryModel.LAYER_LOCATION,      DistilleryModel::createBodyLayer),
        Map.entry(PoppetShelfModel.LAYER_LOCATION,     PoppetShelfModel::createBodyLayer),
        Map.entry(BeartrapModel.LAYER_LOCATION,        BeartrapModel::createBodyLayer),
        Map.entry(KettleModel.LAYER_LOCATION,          KettleModel::createBodyLayer),
        Map.entry(CandelabraModel.LAYER_LOCATION,      CandelabraModel::createBodyLayer),
        Map.entry(BrazierModel.LAYER_LOCATION,         BrazierModel::createBodyLayer),
        Map.entry(ChaliceModel.LAYER_LOCATION,         ChaliceModel::createBodyLayer),
        Map.entry(CrystalBallModel.LAYER_LOCATION,     CrystalBallModel::createBodyLayer),
        Map.entry(DreamCatcherModel.LAYER_LOCATION,    DreamCatcherModel::createBodyLayer),
        Map.entry(GarlicGarlandModel.LAYER_LOCATION,  GarlicGarlandModel::createBodyLayer),
        Map.entry(MirrorModel.LAYER_LOCATION,          MirrorModel::createBodyLayer),
        Map.entry(SpectralFamiliarModel.LAYER_LOCATION, SpectralFamiliarModel::createBodyLayer),
        Map.entry(FumeFunnelModel.LAYER_LOCATION,      FumeFunnelModel::createBodyLayer),
        Map.entry(GrassperModel.LAYER_LOCATION,        GrassperModel::createBodyLayer),
        Map.entry(PlacedModonomiconBookModel.LAYER_LOCATION, PlacedModonomiconBookModel::createBodyLayer),
        Map.entry(SpinningWheelModel.LAYER_LOCATION,   SpinningWheelModel::createBodyLayer),
        Map.entry(LeechChestModel.LAYER_LOCATION,      LeechChestModel::createBodyLayer),
        Map.entry(FetishModel.LAYER_LOCATION,          FetishModel::createBodyLayer),
        Map.entry(FetishTrentModel.LAYER_LOCATION,     FetishTrentModel::createBodyLayer),
        Map.entry(CoffinModel.LAYER_LOCATION,          CoffinModel::createBodyLayer),
        Map.entry(DemonHeartModel.LAYER_LOCATION,      DemonHeartModel::createBodyLayer),
        Map.entry(AlluringSkullModel.LAYER_LOCATION,   AlluringSkullModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.WolfHeadModel.LAYER_LOCATION, com.emoniph.witchery.client.model.WolfHeadModel::createBodyLayer),
        Map.entry(StatueGoddessModel.LAYER_LOCATION,   StatueGoddessModel::createBodyLayer),
        Map.entry(MandrakeStatueModel.LAYER_LOCATION, MandrakeStatueModel::createBodyLayer),
        Map.entry(WolfStatueModel.LAYER_LOCATION,     WolfStatueModel::createBodyLayer),
        Map.entry(SilverVatModel.LAYER_LOCATION,      SilverVatModel::createBodyLayer),
        Map.entry(SilverVatModel.LIQUID_LAYER_LOCATION, SilverVatModel::createLiquidLayer),
        Map.entry(com.emoniph.witchery.client.model.WolfAltarModel.LAYER_LOCATION, com.emoniph.witchery.client.model.WolfAltarModel::createBodyLayer),
        Map.entry(OwlModel.LAYER_LOCATION,             OwlModel::createBodyLayer),
        Map.entry(OwlModel.BABY_LAYER_LOCATION,        OwlModel::createBabyLayer),
        Map.entry(RavenModel.LAYER_LOCATION,           RavenModel::createBodyLayer),
        Map.entry(ToadModel.LAYER_LOCATION,            ToadModel::createBodyLayer),
        Map.entry(ToadModel.BABY_LAYER_LOCATION,       ToadModel::createBabyLayer),
        Map.entry(EarmuffsModel.LAYER_LOCATION,        EarmuffsModel::createBodyLayer),
        Map.entry(ParasyticLouseModel.LAYER_LOCATION,  ParasyticLouseModel::createBodyLayer),
        Map.entry(FlyingMonkeyModel.LAYER_LOCATION,    FlyingMonkeyModel::createBodyLayer),
        Map.entry(CatFamiliarModel.LAYER_LOCATION,     CatFamiliarModel::createBodyLayer),
        Map.entry(CatFamiliarModel.BABY_LAYER_LOCATION, CatFamiliarModel::createBabyLayer),
        Map.entry(BroomModel.LAYER_LOCATION,           BroomModel::createBodyLayer),
        Map.entry(DeathHandModel.LAYER_LOCATION,       DeathHandModel::createLayer),
        Map.entry(BrewBottleModel.LAYER_LOCATION,      BrewBottleModel::createLayer),
        Map.entry(CaneSwordModel.LAYER_LOCATION,       CaneSwordModel::createLayer),
        Map.entry(HandBowModel.LAYER_LOCATION,         HandBowModel::createLayer),
        Map.entry(HuntsmanSpearModel.LAYER_LOCATION,   HuntsmanSpearModel::createLayer),
        Map.entry(MysticBranchModel.LAYER_LOCATION,    MysticBranchModel::createLayer),
        Map.entry(LegacyBipedModel.LAYER_LOCATION,    LegacyBipedModel::createBodyLayer),
        Map.entry(WerewolfWolfModel.LAYER_LOCATION,    WerewolfWolfModel::createBodyLayer),
        Map.entry(WitchHunterModel.LAYER_LOCATION,     WitchHunterModel::createBodyLayer),
        Map.entry(HunterArmorModel.LAYER_LOCATION,     HunterArmorModel::createLayer),
        Map.entry(DeathArmorModel.LAYER_LOCATION,      DeathArmorModel::createLayer),
        Map.entry(WitchesHatArmorModel.LAYER_LOCATION, WitchesHatArmorModel::createLayer),
        Map.entry(WitchesRobeArmorModel.LAYER_LOCATION, WitchesRobeArmorModel::createLayer),
        Map.entry(BabasHatArmorModel.LAYER_LOCATION,   BabasHatArmorModel::createLayer),
        Map.entry(NecromancerRobeArmorModel.LAYER_LOCATION, NecromancerRobeArmorModel::createLayer),
        Map.entry(WitchesShoesArmorModel.LAYER_LOCATION, WitchesShoesArmorModel::createLayer),
        Map.entry(WitchesBeltArmorModel.LAYER_LOCATION, WitchesBeltArmorModel::createLayer),
        Map.entry(BarkGrowthModel.TALL_UV_LAYER, BarkGrowthModel::createTallUvLayer),
        Map.entry(BarkGrowthModel.WIDE_UV_LAYER, BarkGrowthModel::createWideUvLayer),
        Map.entry(TwistingBandArmorModel.LAYER_LOCATION, TwistingBandArmorModel::createLayer),
        Map.entry(MogsQuiverArmorModel.LAYER_LOCATION, MogsQuiverArmorModel::createLayer),
        Map.entry(GulgsGirdleArmorModel.LAYER_LOCATION, GulgsGirdleArmorModel::createLayer),
        Map.entry(HobgoblinModel.LAYER_LOCATION,       HobgoblinModel::createBodyLayer),
        Map.entry(GhostModel.LAYER_BANSHEE,            GhostModel::createBodyLayer),
        Map.entry(GhostModel.LAYER_SPECTRE,            GhostModel::createBodyLayer),
        Map.entry(PoltergeistModel.LAYER_LOCATION,     PoltergeistModel::createBodyLayer),
        Map.entry(TreefydModel.LAYER_LOCATION,         TreefydModel::createBodyLayer),
        Map.entry(EntModel.LAYER_LOCATION,             EntModel::createBodyLayer),
        Map.entry(MandrakeEntityModel.LAYER_LOCATION,  MandrakeEntityModel::createBodyLayer),
        Map.entry(DemonEntityModel.LAYER_LOCATION,     DemonEntityModel::createBodyLayer),
        Map.entry(NightmareModel.LAYER_LOCATION,       NightmareModel::createBodyLayer),
        Map.entry(SpiritModel.LAYER_LOCATION,          SpiritModel::createBodyLayer),
        Map.entry(MirrorFaceModel.LAYER_LOCATION,      MirrorFaceModel::createBodyLayer),
        Map.entry(LordOfTormentModel.LAYER_LOCATION,   LordOfTormentModel::createBodyLayer),
        Map.entry(HornedHuntsmanModel.LAYER_LOCATION,  HornedHuntsmanModel::createBodyLayer),
        Map.entry(DeathEntityModel.LAYER_LOCATION,     DeathEntityModel::createBodyLayer),
        Map.entry(FlameImpModel.LAYER_LOCATION,        FlameImpModel::createBodyLayer),
        Map.entry(FlameImpWingModel.LAYER_LOCATION,    FlameImpWingModel::createBodyLayer),
        Map.entry(GrotesqueModel.LAYER_LOCATION,       GrotesqueModel::createBodyLayer),
        Map.entry(EgassyWartLayer.LAYER_LOCATION,      EgassyWartLayer::createLayerDefinition),
        Map.entry(com.emoniph.witchery.client.model.BloodCrucibleModel.LAYER_LOCATION,
                  com.emoniph.witchery.client.model.BloodCrucibleModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.BloodCrucibleModel.LIQUID_LAYER_LOCATION,
                  com.emoniph.witchery.client.model.BloodCrucibleModel::createLiquidLayer),
        Map.entry(LilithModel.LAYER_LOCATION,     LilithModel::createBodyLayer),
        Map.entry(GoblinGulgModel.LAYER_LOCATION,      GoblinGulgModel::createBodyLayer),
        Map.entry(GoblinMogModel.LAYER_LOCATION,       GoblinMogModel::createBodyLayer),
        Map.entry(BabaYagaModel.LAYER_LOCATION,        BabaYagaModel::createBodyLayer),
        Map.entry(WolfmanModel.LAYER_LOCATION,         WolfmanModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.HellhoundModel.LAYER_LOCATION,
                  com.emoniph.witchery.client.model.HellhoundModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.MindrakeModel.LAYER_LOCATION,
                  com.emoniph.witchery.client.model.MindrakeModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.DarkMarkModel.LAYER_LOCATION,
                  com.emoniph.witchery.client.model.DarkMarkModel::createBodyLayer),
        Map.entry(VillageGuardModel.LAYER_LOCATION,    VillageGuardModel::createBodyLayer),
        Map.entry(LeonardModel.LAYER_LOCATION,    LeonardModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.LostSoulModel.LAYER_LOCATION,
                  com.emoniph.witchery.client.model.LostSoulModel::createBodyLayer),
        Map.entry(com.emoniph.witchery.client.model.VampireArmorModel.Variant.MASCULINE.layer,
                  com.emoniph.witchery.client.model.VampireArmorModel::createLayer),
        Map.entry(com.emoniph.witchery.client.model.VampireArmorModel.Variant.FEMININE.layer,
                  com.emoniph.witchery.client.model.VampireArmorModel::createLayer),
        Map.entry(com.emoniph.witchery.client.model.VampireArmorModel.Variant.METALLIC.layer,
                  com.emoniph.witchery.client.model.VampireArmorModel::createLayer),
        Map.entry(com.emoniph.witchery.client.model.VampireArmorModel.Variant.METALLIC_FEMININE.layer,
                  com.emoniph.witchery.client.model.VampireArmorModel::createLayer)
    );

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LAYER_DEFS.forEach(e -> event.registerLayerDefinition(e.getKey(), e.getValue()));
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WitcheryMenuTypes.WITCHES_OVEN.get(),    WitchesOvenScreen::new);
        event.register(WitcheryMenuTypes.SPINNING_WHEEL.get(), SpinningWheelScreen::new);
        event.register(WitcheryMenuTypes.DISTILLERY.get(),     DistilleryScreen::new);
    }

    /**
     * Make Wispy Cotton show its disturbed art during a nightmare by wrapping its baked
     * {@link net.minecraft.client.renderer.block.model.BlockStateModel} so it delegates to the
     * disturbed-cotton model when {@link com.emoniph.witchery.client.renderer.SpiritWorldSpecialEffects#isNightmareLike()}.
     */
    public static void onModifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        var models = event.getBakingResult().blockStateModels();
        net.minecraft.world.level.block.state.BlockState wispy =
                WitcheryBlocks.WISPY_COTTON.get().defaultBlockState();
        net.minecraft.world.level.block.state.BlockState disturbed =
                WitcheryBlocks.DISTURBED_COTTON.get().defaultBlockState();
        net.minecraft.client.renderer.block.model.BlockStateModel wispyModel = models.get(wispy);
        net.minecraft.client.renderer.block.model.BlockStateModel disturbedModel = models.get(disturbed);
        if (wispyModel != null && disturbedModel != null) {
            models.put(wispy, new com.emoniph.witchery.client.renderer.NightmareCottonModel(
                    wispyModel, disturbedModel));
        }
    }

    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return -1;
                    }
                    if (level.getBlockEntity(pos) instanceof WitchCauldronBlockEntity brew && brew.getColor() != 0) {
                        return brew.getColor();
                    }
                    return BiomeColors.getAverageWaterColor(level, pos);
                },
                WitcheryBlocks.WITCH_CAULDRON.get());
        event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex != 0) {
                        return -1;
                    }
                    if (level != null && pos != null
                            && level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew
                            && brew.getColor() != 0) {
                        return brew.getColor();
                    }
                    return com.emoniph.witchery.blocks.BrewFluidBlock.DEFAULT_BREW_COLOR;
                },
                WitcheryBlocks.BREW_LIQUID.get());
        event.register((state, level, pos, tintIndex) ->
                tintIndex == 0 ? WitcheryMobEffects.DISEASED.value().getColor() : -1,
                WitcheryBlocks.DISEASE.get());
        event.register((state, level, pos, tintIndex) ->
                tintIndex == 0 ? (level != null && pos != null ? 0x208030 : 0x71C35C) : -1,
                WitcheryBlocks.LEAPING_LILY.get());
        event.register((state, level, pos, tintIndex) ->
                tintIndex == 0
                        ? (level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor())
                        : -1,
                WitcheryBlocks.BLIGHTED_SOIL.get());
        // The salt line reuses the redstone-dust models; tint their (red) dust white so it reads as salt.
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? 0xFFFFFF : -1,
                WitcheryBlocks.SALT_LINE.get());
        // Source BlockTormentPortal multiplies its animated portal texture by 0xFF0022.
        event.register((state, level, pos, tintIndex) -> tintIndex == 0
                        ? com.emoniph.witchery.world.TormentAtmosphere.PORTAL_RGB : -1,
                WitcheryBlocks.TORMENT_PORTAL.get());
    }

    public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "brew_bag_color"),
                BrewBagTint.MAP_CODEC);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
            private static final Identifier WATER_FLOW = Identifier.withDefaultNamespace("block/water_flow");
            private static final Identifier WATER_OVERLAY = Identifier.withDefaultNamespace("block/water_overlay");

            @Override
            public Identifier getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public Identifier getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public Identifier getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return 0xFF8A4FCF;
            }

            @Override
            public int getTintColor(net.minecraft.world.level.material.FluidState state,
                                    net.minecraft.world.level.BlockAndTintGetter getter,
                                    BlockPos pos) {
                if (getter == null || pos == null) {
                    return getTintColor();
                }
                return 0xFF000000 | BrewLiquidFluid.brewColor(getter, pos);
            }
        }, WitcheryFluids.BREW_LIQUID_TYPE.value());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final Identifier SPIRIT_STILL = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "block/flowspirit_still");
            private static final Identifier SPIRIT_FLOW = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "block/flowspirit_flow");

            @Override
            public Identifier getStillTexture() {
                return SPIRIT_STILL;
            }

            @Override
            public Identifier getFlowingTexture() {
                return SPIRIT_FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xCCFFFFFF;
            }
        }, WitcheryFluids.FLOWING_SPIRIT_TYPE.value());

        // Vampire clothing wears a custom coat/dress/chestplate model (not the flat armor silhouette).
        event.registerItem(new com.emoniph.witchery.client.VampireArmorClientExtensions(
                        com.emoniph.witchery.client.model.VampireArmorModel.Variant.MASCULINE),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_TOP_HAT.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_DRESS_COAT.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_COAT.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_TROUSERS.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_OXFORD_BOOTS.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_SHOES.get());
        event.registerItem(new com.emoniph.witchery.client.VampireArmorClientExtensions(
                        com.emoniph.witchery.client.model.VampireArmorModel.Variant.FEMININE),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_VEIL_HAT.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_DRESS_JACKET.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_CORSET.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_SKIRTED_TROUSERS.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_SKIRT.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_HEELS.get());
        event.registerItem(new com.emoniph.witchery.client.VampireArmorClientExtensions(
                        com.emoniph.witchery.client.model.VampireArmorModel.Variant.METALLIC),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_HELMET.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_CHAIN_COAT.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_CHESTPLATE.get(),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_LEGGINGS.get(), com.emoniph.witchery.init.WitcheryItems.VAMPIRE_BOOTS.get());
        event.registerItem(new com.emoniph.witchery.client.VampireArmorClientExtensions(
                        com.emoniph.witchery.client.model.VampireArmorModel.Variant.METALLIC_FEMININE),
                com.emoniph.witchery.init.WitcheryItems.VAMPIRE_CHAIN_COAT_LADIES.get());

        event.registerItem(new com.emoniph.witchery.client.HunterArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.HUNTER_HAT.get(),
                com.emoniph.witchery.init.WitcheryItems.HUNTER_COAT.get(),
                com.emoniph.witchery.init.WitcheryItems.HUNTER_LEGGINGS.get(), com.emoniph.witchery.init.WitcheryItems.HUNTER_BOOTS.get(),
                com.emoniph.witchery.init.WitcheryItems.SILVERED_HUNTER_HAT.get(),
                com.emoniph.witchery.init.WitcheryItems.SILVERED_HUNTER_COAT.get(),
                com.emoniph.witchery.init.WitcheryItems.SILVERED_HUNTER_LEGGINGS.get(),
                com.emoniph.witchery.init.WitcheryItems.SILVERED_HUNTER_BOOTS.get(),
                com.emoniph.witchery.init.WitcheryItems.DAWN_HUNTER_HAT.get(),
                com.emoniph.witchery.init.WitcheryItems.DAWN_HUNTER_COAT.get(),
                com.emoniph.witchery.init.WitcheryItems.DAWN_HUNTER_LEGGINGS.get(),
                com.emoniph.witchery.init.WitcheryItems.DAWN_HUNTER_BOOTS.get());

        event.registerItem(new com.emoniph.witchery.client.DeathArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.DEATHS_HOOD.get(),
                com.emoniph.witchery.init.WitcheryItems.DEATHS_ROBE.get(),
                com.emoniph.witchery.init.WitcheryItems.DEATHS_SHOES.get());

        event.registerItem(new com.emoniph.witchery.client.WitchesHatArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.WITCHES_HAT.get());

        event.registerItem(new com.emoniph.witchery.client.WitchesRobeArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.WITCHES_ROBES.get());

        event.registerItem(new com.emoniph.witchery.client.BabasHatArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.BABAS_HAT.get());

        event.registerItem(new com.emoniph.witchery.client.NecromancerRobeArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.NECROMANCER_ROBES.get());

        event.registerItem(new com.emoniph.witchery.client.WitchesShoesArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.ICY_SLIPPERS.get(),
                com.emoniph.witchery.init.WitcheryItems.RUBY_SLIPPERS.get(),
                com.emoniph.witchery.init.WitcheryItems.SEEPING_SHOES.get());

        event.registerItem(new com.emoniph.witchery.client.WitchesBeltArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.BITING_BELT.get(),
                com.emoniph.witchery.init.WitcheryItems.BARK_BELT.get());

        event.registerItem(new com.emoniph.witchery.client.TwistingBandArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.TWISTING_BAND.get());

        event.registerItem(new com.emoniph.witchery.client.MogsQuiverArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.MOGS_QUIVER.get());

        event.registerItem(new com.emoniph.witchery.client.GulgsGirdleArmorClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.GULGS_GIRDLE.get(),
                com.emoniph.witchery.init.WitcheryItems.GULGS_GRUDLE.get());

        event.registerItem(new com.emoniph.witchery.client.WoodenScannerClientExtensions(),
                com.emoniph.witchery.init.WitcheryItems.WOODEN_SCANNER.get());
    }

    public static void registerRangeSelectItemModelProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "chalice_filled"),
                ChaliceFilledProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "poppet_shelf_compass_signal"),
                PoppetShelfCompassAngleProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "player_compass_angle"),
                PlayerCompassAngleProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "blocking"),
                HuntsmanSpearBlockingProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "deaths_hand_deployed"),
                DeathsHandDeployedProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "circle_talisman_variant"),
                CircleTalismanVariantProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spectral_stone_type"),
                SpectralStoneTypeProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "plant_mine_disguise"),
                PlantMineDisguiseProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "stockade_wood"),
                StockadeWoodProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "leonards_urn_capacity"),
                LeonardsUrnCapacityProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "wooden_scanner_using"),
                WoodenScannerUsingProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hand_bow_pull"),
                HandBowPullProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hand_bow_pulling"),
                HandBowPullingProperty.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "hand_bow_charged"),
                HandBowChargedProperty.MAP_CODEC);
    }

    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        WitcherySpecialItemRenderers.register(event);
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<net.minecraft.client.renderer.entity.LivingEntityRenderer<
                        net.minecraft.world.entity.LivingEntity,
                        net.minecraft.client.renderer.entity.state.LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    boolean effectActive = entity.hasEffect(WitcheryMobEffects.GROTESQUE);
                    boolean fixedAuraActive = entity instanceof Player player
                            && WitcheryAttachments.get(player).hasGrotesqueAura();
                    state.setRenderData(GrotesqueLayer.OVERLAY_STATUS,
                            com.emoniph.witchery.effect.GrotesqueMobEffect.isOverlayActive(
                                    effectActive, fixedAuraActive)
                                    ? new GrotesqueLayer.OverlayStatus(effectActive, fixedAuraActive)
                                    : null);
                });
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<?> ar = event.getPlayerRenderer(skin);
            if (ar != null) {
                ar.addLayer(new WerewolfLayer(ar, event.getContext()));
                ar.addLayer(new com.emoniph.witchery.client.renderer.VampireBatLayer(ar, event.getContext()));
                ar.addLayer(new ManifestedGhostLayer(ar));
                ar.addLayer(new MountedBroomInventoryLayer(ar, event.getContext()));
                ar.addLayer(new EarmuffsLayer(ar, event.getContext()));
                ar.addLayer(new VampireArmorChainLayer(ar, event.getContext()));
                addBarkBeltLayer(ar, event.getContext());
                addGrotesqueLayer(ar, event.getContext());
            }
        }
        for (net.minecraft.world.entity.EntityType<?> entityType : event.getEntityTypes()) {
            addBarkBeltLayer(event.getRenderer(entityType), event.getContext());
            addGrotesqueLayer(event.getRenderer(entityType), event.getContext());
        }
    }

    private static void addBarkBeltLayer(
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> livingRenderer
                && livingRenderer.getModel() instanceof net.minecraft.client.model.HumanoidModel<?>) {
            addBarkBeltLayerUnchecked(livingRenderer, context);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addBarkBeltLayerUnchecked(
            net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        renderer.addLayer(new BarkBeltLayer(renderer, context));
    }

    private static void addGrotesqueLayer(
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> livingRenderer) {
            addGrotesqueLayerUnchecked(livingRenderer, context);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addGrotesqueLayerUnchecked(
            net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        renderer.addLayer(new GrotesqueLayer(renderer, context));
    }

    public static void registerBookPageRenderers(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry.registerPageRenderer(
                    com.emoniph.witchery.book.page.StationRecipePage.TYPE,
                    page -> new com.emoniph.witchery.client.StationRecipePageRenderer(
                            (com.emoniph.witchery.book.page.StationRecipePage) page));
            com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry.registerPageRenderer(
                    com.emoniph.witchery.book.page.ItemReferencePage.TYPE,
                    page -> new com.emoniph.witchery.client.ItemReferencePageRenderer(
                            (com.emoniph.witchery.book.page.ItemReferencePage) page));
            // Override Modonomicon's built-in entity page renderer: its scissor box omits the book
            // origin, so the box sat off the frame and clipped the mob's lower body. Runs in
            // enqueueWork (after Modonomicon's own client-setup registration) so this put() wins.
            com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry.registerPageRenderer(
                    com.klikli_dev.modonomicon.api.ModonomiconConstants.Data.Page.ENTITY,
                    page -> new com.emoniph.witchery.client.WitcheryEntityPageRenderer(
                            (com.klikli_dev.modonomicon.book.page.BookEntityPage) page));
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WitcheryBlockEntities.WITCHES_OVEN.get(),   WitchesOvenRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.KETTLE.get(),          KettleRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.DISTILLERY.get(),      DistilleryRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.POPPET_SHELF.get(),    PoppetShelfRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.CANDELABRA.get(),      CandelabraRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.BRAZIER.get(),         BrazierRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.CHALICE.get(),         ChaliceRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.CRYSTAL_BALL.get(),    CrystalBallRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.DREAM_CATCHER.get(),   DreamCatcherRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.GARLIC_GARLAND.get(),  GarlicGarlandRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.MIRROR.get(),           MirrorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.FUME_FUNNEL.get(),     FumeFunnelRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.SPINNING_WHEEL.get(),  SpinningWheelRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.LEECH_CHEST.get(),     LeechChestRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.REFILLING_CHEST.get(), ChestRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.FETISH.get(),          FetishRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.COFFIN.get(),          CoffinRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.PLACED_ITEM.get(),     PlacedItemRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.PLACED_MODONOMICON.get(), PlacedModonomiconRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.GRASSPER.get(),        GrassperRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.BEARTRAP.get(),        BeartrapRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.STATUE_GODDESS.get(),  StatueGoddessRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.AREA_MARKER.get(),     DecurseStatueRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.SILVER_VAT.get(),      SilverVatRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.WOLF_ALTAR.get(),      com.emoniph.witchery.client.renderer.WolfAltarRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.DEMON_HEART.get(),     DemonHeartRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.ALLURING_SKULL.get(),  AlluringSkullRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.WOLF_HEAD.get(),       com.emoniph.witchery.client.renderer.WolfHeadRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.SUN_COLLECTOR.get(),   SunCollectorRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.BLOOD_CRUCIBLE.get(),
                com.emoniph.witchery.client.renderer.BloodCrucibleRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.OWL.get(),             OwlFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.RAVEN.get(),           RavenRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.ATTACK_BAT.get(),      net.minecraft.client.renderer.entity.BatRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.FAMILIAR_CAT.get(),    CatFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPECTRAL_FAMILIAR.get(), SpectralFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.TOAD.get(),             ToadFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.PARASYTIC_LOUSE.get(),  ParasyticLouseRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.COVEN_WITCH.get(),     CovenWitchRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPECTRAL_WITCH.get(),  com.emoniph.witchery.client.renderer.SpectralWitchRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.CRAZY_CAT_LADY.get(),  CrazyCatLadyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.THROWN_CAT.get(),       ThrownCatRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.WITCH_HUNTER.get(),    WitchHunterRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.BANSHEE.get(),         BansheeRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPECTRE.get(),         SpectreRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.POLTERGEIST.get(),     PoltergeistRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.TREEFYD.get(),         TreefydRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.ENT.get(),             EntRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.MANDRAKE_MOB.get(),    MandrakeEntityRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.DEMON.get(),           DemonEntityRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.BROOM.get(),           BroomRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.NIGHTMARE.get(),       NightmareRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPIRIT.get(),          SpiritRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.BINKY.get(),           BinkyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.DEATH.get(),           DeathEntityRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.LORD_OF_TORMENT.get(), LordOfTormentRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.HORNED_HUNTSMAN.get(), HornedHuntsmanRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.FLAME_IMP.get(),       FlameImpRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SOULFIRE.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.THROWN_BREW.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.EARTH_ROCK.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SUN_GRENADE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPELL_EFFECT.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SCRYING_EYE.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.EGASSY.get(),            EgassyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.HOBGOBLIN.get(),        HobgoblinRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.FLYING_MONKEY.get(),   FlyingMonkeyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.CORPSE.get(),          CorpseRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.REFLECTION.get(),      ReflectionRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.MIRROR_DECOY.get(),    com.emoniph.witchery.client.renderer.MirrorDecoyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.MIRROR_FACE.get(),     MirrorFaceRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.ELLE.get(),            ElleRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.LILITH.get(),          LilithRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.GOBLIN_GULG.get(), GoblinGulgRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.GOBLIN_MOG.get(),  GoblinMogRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.VAMPIRE.get(),     VampireRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.BABA_YAGA.get(),  BabaYagaRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.BOLT.get(),       BoltRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.WOLFMAN.get(),          WolfmanRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.HELLHOUND.get(),        com.emoniph.witchery.client.renderer.HellhoundRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.MINDRAKE.get(),         com.emoniph.witchery.client.renderer.MindrakeRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.DARK_MARK.get(),        com.emoniph.witchery.client.renderer.DarkMarkRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.ILLUSION_CREEPER.get(), com.emoniph.witchery.client.renderer.IllusionRenderers.Creeper::new);
        event.registerEntityRenderer(WitcheryEntities.ILLUSION_SPIDER.get(),  com.emoniph.witchery.client.renderer.IllusionRenderers.Spider::new);
        event.registerEntityRenderer(WitcheryEntities.ILLUSION_ZOMBIE.get(),  com.emoniph.witchery.client.renderer.IllusionRenderers.Zombie::new);
        event.registerEntityRenderer(WitcheryEntities.WERE_VILLAGER.get(),   WereVillagerRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.VILLAGE_GUARD.get(),   VillageGuardRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.LEONARD.get(),         LeonardRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.LOST_SOUL.get(),       com.emoniph.witchery.client.renderer.LostSoulRenderer::new);
    }

    // ── Network handlers (client-side only) ───────────────────────────────────

    public static void handleSyncPlayerData(SyncPlayerDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(payload.entityId());
            if (!(entity instanceof Player target)) return;
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            data.setVampire(payload.vampire());
            data.setVampireLevel(payload.vampireLevel());
            data.setBloodPower(payload.bloodPower());
            data.setBloodReserve(payload.bloodReserve());
            data.setHumanBlood(payload.humanBlood());
            data.setSelectedVampirePower(WitcheryPlayerData.VampirePower.fromOrdinal(payload.selectedVampirePower()));
            data.setVampireUltimate(WitcheryPlayerData.VampireUltimate.fromOrdinal(payload.vampireUltimate()),
                    payload.vampireUltimateCharges());
            data.setVampireVisionActive(payload.vampireVisionActive());
            data.setVampireBatForm(payload.vampireBatForm());
            data.setWerewolf(payload.werewolf());
            data.setWerewolfLevel(payload.werewolfLevel());
            data.setWolfForm(WitcheryPlayerData.WolfForm.fromOrdinal(payload.wolfForm()));
            data.setBloodLevel(payload.bloodLevel());
            data.setTransformationLevel(payload.transformationLevel());
            target.refreshDimensions();
            if (!payload.familiarUUID().isEmpty()) {
                try { data.setFamiliarUUID(java.util.UUID.fromString(payload.familiarUUID())); }
                catch (IllegalArgumentException ignored) {}
            } else {
                data.clearFamiliar();
            }
            data.setFamiliarType(payload.familiarType());
            data.setFamiliarName(payload.familiarName());
            data.setFamiliarSitting(payload.familiarSitting());
            data.setFamiliarNameVisible(payload.familiarNameVisible());
            data.setInfusionType(InfusionType.fromString(payload.infusionType()));
            data.setInfusionEnergy(payload.infusionEnergy(), payload.infusionMaxEnergy());
            data.setInSpiritWorld(payload.inSpiritWorld());
            data.setOtherwhereHearthTraveler(payload.otherwhereHearthTraveler());
            data.setManifestedGhost(payload.manifestedGhost());
            data.setSpiritWorldIsDream(payload.spiritWorldDream());
            data.setDemonicNightmare(payload.demonicNightmare());
            data.setGrotesqueTicks(payload.grotesqueTicks());
        });
    }

    public static void handleSyncVampireTargetBlood(
            com.emoniph.witchery.network.SyncVampireTargetBloodPayload payload,
            IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(payload.entityId());
            if (entity instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
                villager.getPersistentData().putInt("witchery_villager_blood",
                        Math.clamp(payload.blood(), 0, 500));
            }
        });
    }

    public static void handleOpenObservationsBook(OpenObservationsBookPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new ObservationsScreen(
                payload.researchLevel(), payload.pageIndex(), payload.offhand())));
    }

    public static void handleScryingCamera(ScryingCameraPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.emoniph.witchery.client.ScryingCameraState.handle(payload));
    }
}
