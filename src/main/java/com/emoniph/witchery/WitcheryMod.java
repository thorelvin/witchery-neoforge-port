package com.emoniph.witchery;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryChunkGenerators;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.init.WitcheryPlacementModifiers;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryCreativeTabs;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryFeatures;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.network.BroomControlPayload;
import com.emoniph.witchery.network.HowlPayload;
import com.emoniph.witchery.network.OpenObservationsBookPayload;
import com.emoniph.witchery.network.PlacedModonomiconPageTurnPayload;
import com.emoniph.witchery.network.ScryingCameraPayload;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.network.VampireUltimatePayload;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(WitcheryMod.MODID)
public class WitcheryMod {
    public static final String MODID = "witchery";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WitcheryMod(IEventBus modEventBus, ModContainer modContainer) {
        WitcheryFluids.FLUID_TYPES.register(modEventBus);
        WitcheryFluids.FLUIDS.register(modEventBus);
        WitcheryBlocks.BLOCKS.register(modEventBus);
        WitcheryItems.ITEMS.register(modEventBus);
        WitcheryCreativeTabs.TABS.register(modEventBus);
        WitcheryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        WitcheryRecipeTypes.RECIPE_TYPES.register(modEventBus);
        WitcheryRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        WitcheryFeatures.FEATURES.register(modEventBus);
        WitcheryAttachments.ATTACHMENT_TYPES.register(modEventBus);
        WitcheryDataComponents.DATA_COMPONENTS.register(modEventBus);
        WitcheryMenuTypes.MENU_TYPES.register(modEventBus);
        WitcheryEntities.ENTITY_TYPES.register(modEventBus);
        WitcheryMobEffects.MOB_EFFECTS.register(modEventBus);
        WitcherySoundEvents.SOUND_EVENTS.register(modEventBus);
        WitcheryStructures.STRUCTURE_TYPES.register(modEventBus);
        WitcheryStructures.PIECE_TYPES.register(modEventBus);
        WitcheryStructures.POOL_ELEMENT_TYPES.register(modEventBus);
        WitcheryChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        WitcheryPlacementModifiers.PLACEMENT_MODIFIERS.register(modEventBus);
        com.emoniph.witchery.command.OtherwhereHearthGameTests.register(modEventBus);
        com.emoniph.witchery.command.TormentGameTests.register(modEventBus);
        com.emoniph.witchery.command.MirrorWorldGameTests.register(modEventBus);
        com.emoniph.witchery.command.LycanthropyGameTests.register(modEventBus);
        com.emoniph.witchery.entity.HobgoblinEcologyGameTests.register(modEventBus);
        com.emoniph.witchery.brewing.IceWorldGameTests.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, WitcheryConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(com.emoniph.witchery.datagen.WitcheryDataGenerators::onGatherData);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(WitcheryClient::registerRangeSelectItemModelProperties);
            modEventBus.addListener(WitcheryClient::registerLayerDefinitions);
            modEventBus.addListener(WitcheryClient::registerScreens);
            modEventBus.addListener(WitcheryClient::registerBookPageRenderers);
            modEventBus.addListener(WitcheryClient::registerBlockColors);
            modEventBus.addListener(WitcheryClient::registerItemTintSources);
            modEventBus.addListener(WitcheryClient::onModifyBakingResult);
            modEventBus.addListener(WitcheryClient::registerClientExtensions);
            modEventBus.addListener(WitcheryClient::registerRenderStateModifiers);
            modEventBus.addListener(WitcheryClient::onAddLayers);
            modEventBus.addListener(WitcheryClient::registerRenderers);
            modEventBus.addListener(WitcheryClient::registerSpecialModelRenderers);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Force the symbol-spell registry to initialise so Mystic Branch glyphs are recognised.
        com.emoniph.witchery.symbol.EffectRegistry.bootstrap();
        // Build the custom-brew ingredient table.
        com.emoniph.witchery.brewing.Brews.bootstrap();
        // Register custom Modonomicon pages that render live item stacks with tooltips.
        event.enqueueWork(() -> {
            com.klikli_dev.modonomicon.data.LoaderRegistry.registerPageLoader(
                    com.emoniph.witchery.book.page.StationRecipePage.TYPE,
                    com.emoniph.witchery.book.page.StationRecipePage::fromJson,
                    com.emoniph.witchery.book.page.StationRecipePage::fromNetwork);
            com.klikli_dev.modonomicon.data.LoaderRegistry.registerPageLoader(
                    com.emoniph.witchery.book.page.ItemReferencePage.TYPE,
                    com.emoniph.witchery.book.page.ItemReferencePage::fromJson,
                    com.emoniph.witchery.book.page.ItemReferencePage::fromNetwork);
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registrar.playToClient(
                    SyncPlayerDataPayload.TYPE,
                    SyncPlayerDataPayload.STREAM_CODEC,
                    WitcheryClient::handleSyncPlayerData);
            registrar.playToClient(
                    OpenObservationsBookPayload.TYPE,
                    OpenObservationsBookPayload.STREAM_CODEC,
                    WitcheryClient::handleOpenObservationsBook);
            registrar.playToClient(
                    ScryingCameraPayload.TYPE,
                    ScryingCameraPayload.STREAM_CODEC,
                    WitcheryClient::handleScryingCamera);
        } else {
            // Keep protocol registration symmetric without resolving client-only handler classes.
            registrar.playToClient(
                    SyncPlayerDataPayload.TYPE,
                    SyncPlayerDataPayload.STREAM_CODEC,
                    (payload, context) -> { });
            registrar.playToClient(
                    OpenObservationsBookPayload.TYPE,
                    OpenObservationsBookPayload.STREAM_CODEC,
                    (payload, context) -> { });
            registrar.playToClient(
                    ScryingCameraPayload.TYPE,
                    ScryingCameraPayload.STREAM_CODEC,
                    (payload, context) -> { });
        }

        registrar.playToServer(
                        HowlPayload.TYPE,
                        HowlPayload.STREAM_CODEC,
                        HowlPayload::handleOnServer)
                .playToServer(
                        VampireUltimatePayload.TYPE,
                        VampireUltimatePayload.STREAM_CODEC,
                        VampireUltimatePayload::handleOnServer)
                .playToServer(
                        BroomControlPayload.TYPE,
                        BroomControlPayload.STREAM_CODEC,
                        BroomControlPayload::handleOnServer)
                .playToServer(
                        PlacedModonomiconPageTurnPayload.TYPE,
                        PlacedModonomiconPageTurnPayload.STREAM_CODEC,
                        PlacedModonomiconPageTurnPayload::handleOnServer)
                .playToServer(
                        com.emoniph.witchery.network.PlacedModonomiconPageJumpPayload.TYPE,
                        com.emoniph.witchery.network.PlacedModonomiconPageJumpPayload.STREAM_CODEC,
                        com.emoniph.witchery.network.PlacedModonomiconPageJumpPayload::handleOnServer)
                .playToServer(
                        com.emoniph.witchery.network.SpellPreparedPayload.TYPE,
                        com.emoniph.witchery.network.SpellPreparedPayload.STREAM_CODEC,
                        com.emoniph.witchery.network.SpellPreparedPayload::handleOnServer);
    }
}
