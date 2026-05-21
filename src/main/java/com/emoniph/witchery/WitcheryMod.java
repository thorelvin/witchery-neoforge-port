package com.emoniph.witchery;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryChunkGenerators;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryStructures;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.init.WitcheryPlacementModifiers;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryCreativeTabs;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeSerializers;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.emoniph.witchery.network.HowlPayload;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
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
        WitcheryBlocks.BLOCKS.register(modEventBus);
        WitcheryItems.ITEMS.register(modEventBus);
        WitcheryCreativeTabs.TABS.register(modEventBus);
        WitcheryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        WitcheryRecipeTypes.RECIPE_TYPES.register(modEventBus);
        WitcheryRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        WitcheryAttachments.ATTACHMENT_TYPES.register(modEventBus);
        WitcheryMenuTypes.MENU_TYPES.register(modEventBus);
        WitcheryEntities.ENTITY_TYPES.register(modEventBus);
        WitcheryStructures.STRUCTURE_TYPES.register(modEventBus);
        WitcheryStructures.PIECE_TYPES.register(modEventBus);
        WitcheryChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        WitcheryPlacementModifiers.PLACEMENT_MODIFIERS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, WitcheryConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(WitcheryClient::registerLayerDefinitions);
            modEventBus.addListener(WitcheryClient::registerScreens);
            modEventBus.addListener(WitcheryClient::onAddLayers);
            modEventBus.addListener(WitcheryClient::registerRenderers);
            modEventBus.addListener(WitcheryClient::registerSpecialModelRenderers);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {}

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        SyncPlayerDataPayload.TYPE,
                        SyncPlayerDataPayload.STREAM_CODEC,
                        WitcheryClient::handleSyncPlayerData)
                .playToServer(
                        HowlPayload.TYPE,
                        HowlPayload.STREAM_CODEC,
                        HowlPayload::handleOnServer);
    }
}
