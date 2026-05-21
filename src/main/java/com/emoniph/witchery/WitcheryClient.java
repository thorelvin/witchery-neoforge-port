package com.emoniph.witchery;

import com.emoniph.witchery.client.model.*;
import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.client.model.SpiritModel;
import com.emoniph.witchery.client.renderer.*;
import com.emoniph.witchery.client.renderer.NightmareRenderer;
import com.emoniph.witchery.client.renderer.SpiritRenderer;
import com.emoniph.witchery.client.screen.DistilleryScreen;
import com.emoniph.witchery.client.screen.SpinningWheelScreen;
import com.emoniph.witchery.client.screen.WitchesOvenScreen;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// MOD bus events registered via IEventBus in WitcheryMod (dist-guarded).
// Client-only network handlers live here to keep Minecraft.getInstance() out of shared code.
// Game bus events in WitcheryClientEvents.
public class WitcheryClient {
    public static final List<Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>>> LAYER_DEFS = List.of(
        Map.entry(WitchesOvenModel.LAYER_LOCATION,    WitchesOvenModel::createBodyLayer),
        Map.entry(KettleModel.LAYER_LOCATION,          KettleModel::createBodyLayer),
        Map.entry(DistilleryModel.LAYER_LOCATION,      DistilleryModel::createBodyLayer),
        Map.entry(PoppetShelfModel.LAYER_LOCATION,     PoppetShelfModel::createBodyLayer),
        Map.entry(CandelabraModel.LAYER_LOCATION,      CandelabraModel::createBodyLayer),
        Map.entry(BrazierModel.LAYER_LOCATION,         BrazierModel::createBodyLayer),
        Map.entry(ChaliceModel.LAYER_LOCATION,         ChaliceModel::createBodyLayer),
        Map.entry(CrystalBallModel.LAYER_LOCATION,     CrystalBallModel::createBodyLayer),
        Map.entry(DreamCatcherModel.LAYER_LOCATION,    DreamCatcherModel::createBodyLayer),
        Map.entry(FumeFunnelModel.LAYER_LOCATION,      FumeFunnelModel::createBodyLayer),
        Map.entry(GrassperModel.LAYER_LOCATION,        GrassperModel::createBodyLayer),
        Map.entry(SpinningWheelModel.LAYER_LOCATION,   SpinningWheelModel::createBodyLayer),
        Map.entry(LeechChestModel.LAYER_LOCATION,      LeechChestModel::createBodyLayer),
        Map.entry(FetishModel.LAYER_LOCATION,          FetishModel::createBodyLayer),
        Map.entry(FetishTrentModel.LAYER_LOCATION,     FetishTrentModel::createBodyLayer),
        Map.entry(CoffinModel.HEAD_LAYER_LOCATION,     CoffinModel::createHeadLayer),
        Map.entry(CoffinModel.BOTTOM_LAYER_LOCATION,   CoffinModel::createBottomLayer),
        Map.entry(DemonHeartModel.LAYER_LOCATION,      DemonHeartModel::createBodyLayer),
        Map.entry(AlluringSkullModel.LAYER_LOCATION,   AlluringSkullModel::createBodyLayer),
        Map.entry(StatueGoddessModel.LAYER_LOCATION,   StatueGoddessModel::createBodyLayer),
        Map.entry(OwlModel.LAYER_LOCATION,             OwlModel::createBodyLayer),
        Map.entry(RavenModel.LAYER_LOCATION,           RavenModel::createBodyLayer),
        Map.entry(ToadModel.LAYER_LOCATION,            ToadModel::createBodyLayer),
        Map.entry(CatFamiliarModel.LAYER_LOCATION,     CatFamiliarModel::createBodyLayer),
        Map.entry(BroomModel.LAYER_LOCATION,           BroomModel::createBodyLayer),
        Map.entry(WerewolfModel.LAYER_LOCATION,        WerewolfModel::createBodyLayer),
        Map.entry(WitchHunterModel.LAYER_LOCATION,     WitchHunterModel::createBodyLayer),
        Map.entry(HobgoblinModel.LAYER_LOCATION,       HobgoblinModel::createBodyLayer),
        Map.entry(GhostModel.LAYER_BANSHEE,            GhostModel::createBodyLayer),
        Map.entry(GhostModel.LAYER_SPECTRE,            GhostModel::createBodyLayer),
        Map.entry(GhostModel.LAYER_POLTERGEIST,        GhostModel::createBodyLayer),
        Map.entry(TreefydModel.LAYER_LOCATION,         TreefydModel::createBodyLayer),
        Map.entry(EntModel.LAYER_LOCATION,             EntModel::createBodyLayer),
        Map.entry(MandrakeEntityModel.LAYER_LOCATION,  MandrakeEntityModel::createBodyLayer),
        Map.entry(DemonEntityModel.LAYER_LOCATION,     DemonEntityModel::createBodyLayer),
        Map.entry(NightmareModel.LAYER_LOCATION,       NightmareModel::createBodyLayer),
        Map.entry(SpiritModel.LAYER_LOCATION,          SpiritModel::createBodyLayer),
        Map.entry(LordOfTormentModel.LAYER_LOCATION,   LordOfTormentModel::createBodyLayer),
        Map.entry(HornedHuntsmanModel.LAYER_LOCATION,  HornedHuntsmanModel::createBodyLayer),
        Map.entry(DeathEntityModel.LAYER_LOCATION,     DeathEntityModel::createBodyLayer),
        Map.entry(FlameImpModel.LAYER_LOCATION,        FlameImpModel::createBodyLayer)
    );

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        LAYER_DEFS.forEach(e -> event.registerLayerDefinition(e.getKey(), e.getValue()));
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WitcheryMenuTypes.WITCHES_OVEN.get(),    WitchesOvenScreen::new);
        event.register(WitcheryMenuTypes.SPINNING_WHEEL.get(), SpinningWheelScreen::new);
        event.register(WitcheryMenuTypes.DISTILLERY.get(),     DistilleryScreen::new);
    }

    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        WitcherySpecialItemRenderers.register(event);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<?> ar = event.getPlayerRenderer(skin);
            if (ar != null) {
                ar.addLayer(new WerewolfLayer(ar, event.getContext()));
            }
        }
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
        event.registerBlockEntityRenderer(WitcheryBlockEntities.FUME_FUNNEL.get(),     FumeFunnelRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.SPINNING_WHEEL.get(),  SpinningWheelRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.LEECH_CHEST.get(),     LeechChestRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.REFILLING_CHEST.get(), ChestRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.FETISH.get(),          FetishRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.COFFIN.get(),          CoffinRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.PLACED_ITEM.get(),     PlacedItemRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.GRASSPER.get(),        GrassperRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.STATUE_GODDESS.get(),  StatueGoddessRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.DEMON_HEART.get(),     DemonHeartRenderer::new);
        event.registerBlockEntityRenderer(WitcheryBlockEntities.ALLURING_SKULL.get(),  AlluringSkullRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.OWL.get(),             OwlFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.RAVEN.get(),           RavenRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.FAMILIAR_CAT.get(),    CatFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.SPECTRAL_FAMILIAR.get(), SpectralFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.TOAD.get(),             ToadFamiliarRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.COVEN_WITCH.get(),     CovenWitchRenderer::new);
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
        event.registerEntityRenderer(WitcheryEntities.HOBGOBLIN.get(),       HobgoblinRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.FLYING_MONKEY.get(),   FlyingMonkeyRenderer::new);
        event.registerEntityRenderer(WitcheryEntities.CORPSE.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
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
            data.setWerewolf(payload.werewolf());
            data.setBloodLevel(payload.bloodLevel());
            data.setTransformationLevel(payload.transformationLevel());
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
        });
    }
}
