package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.blocks.entity.BeartrapBlockEntity;
import com.emoniph.witchery.client.renderer.SpiritWorldSpecialEffects;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.effect.SwimmingMobEffect;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.item.EarmuffsItem;
import com.emoniph.witchery.network.HowlPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = WitcheryMod.MODID, value = Dist.CLIENT)
public class WitcheryClientEvents {

    private static final Map<Integer, PlayerVisibilityState> WEREWOLF_VISIBILITY_OVERRIDES = new HashMap<>();

    private static boolean lastNightmareLike;

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.emoniph.witchery.client.SymbolDrawState.clientTick();
        com.emoniph.witchery.client.ScryingCameraState.clientTick();
        applySwimmingBoost();
        refreshCottonOnNightmareChange();
    }

    /**
     * Wispy Cotton swaps to its disturbed art via a dynamic baked model; that art is baked into the
     * chunk mesh, so a nightmare beginning/ending won't show until the section rebuilds. Force a
     * re-mesh on the transition (rare) so the change is visible immediately.
     */
    private static void refreshCottonOnNightmareChange() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean nightmareLike = com.emoniph.witchery.client.renderer.SpiritWorldSpecialEffects.isNightmareLike();
        if (nightmareLike != lastNightmareLike) {
            lastNightmareLike = nightmareLike;
            if (mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        }
    }

    /**
     * Close an open guidebook with the <strong>X</strong> key (mirrors Escape). Covers the native
     * Modonomicon books (Witches' Primer and friends) and the vanilla read-only book screen used for
     * the Immortal's Observations. Skipped while a text field is focused (e.g. a Modonomicon search
     * box) so the key still types there.
     */
    @SubscribeEvent
    public static void onBookScreenKeyPressed(net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != org.lwjgl.glfw.GLFW.GLFW_KEY_X) {
            return;
        }
        net.minecraft.client.gui.screens.Screen screen = event.getScreen();
        if (!isCloseableBookScreen(screen)) {
            return;
        }
        if (screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox) {
            return; // reader is typing (search box) — let the key through
        }
        screen.onClose();
        event.setCanceled(true);
    }

    private static boolean isCloseableBookScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen) {
            return true; // vanilla read-only book (Immortal's Observations)
        }
        // Native Modonomicon guidebook screens all live under this gui.book package.
        return screen.getClass().getName().startsWith("com.klikli_dev.modonomicon.client.gui.book.");
    }

    @SubscribeEvent
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        com.emoniph.witchery.client.ScryingCameraState.beforeRenderFrame();
    }

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        com.emoniph.witchery.client.ScryingCameraState.afterRenderFrame();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (com.emoniph.witchery.client.ScryingCameraState.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        com.emoniph.witchery.client.SymbolHud.render(event.getGuiGraphics(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        com.emoniph.witchery.client.InfusionEnergyHud.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        tryHowl(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        tryHowl(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !EarmuffsItem.isWorn(mc.player)) return;
        event.setSound(null);
    }

    @SubscribeEvent
    public static void onExtractBlockOutlineRenderState(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        BlockPos pos = event.getBlockPos();
        BlockState state = event.getBlockState();
        if (state.is(WitcheryBlocks.PLACED_MODONOMICON.get())) {
            event.setCanceled(true);
            return;
        }
        if (!state.is(WitcheryBlocks.BEARTRAP.get()) || !state.getValue(BeartrapBlock.CLOSED)) return;
        if (event.getLevel().getBlockEntity(pos) instanceof BeartrapBlockEntity trap
                && trap.hasOwner()
                && !trap.isOwner(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        if (!event.getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        var renderState = event.getRenderState();
        renderState.customCloudsRenderer = SpiritWorldSpecialEffects.NO_CLOUDS;

        var sky = renderState.skyRenderState;
        sky.skyColor = SpiritWorldSpecialEffects.currentSpiritRgb();
        sky.sunriseAndSunsetColor = 0;
        sky.starBrightness = 0.0F;

        float sourceAngle = SpiritWorldSpecialEffects.currentSunAngleRadians();
        sky.sunAngle = sourceAngle;
        sky.moonAngle = sourceAngle;
        sky.starAngle = sourceAngle;
        sky.shouldRenderDarkDisc = event.getCamera().position().y < 0.0D;
    }

    @SubscribeEvent
    public static void onRenderWerewolfPlayerPre(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (isWerewolfRenderState(state)) {
            WEREWOLF_VISIBILITY_OVERRIDES.put(state.id,
                    new PlayerVisibilityState(state.isInvisible, state.isInvisibleToPlayer));
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
        }
    }

    @SubscribeEvent
    public static void onRenderWerewolfPlayerPost(RenderPlayerEvent.Post<?> event) {
        AvatarRenderState state = event.getRenderState();
        PlayerVisibilityState original = WEREWOLF_VISIBILITY_OVERRIDES.remove(state.id);
        if (original != null) {
            state.isInvisible = original.invisible();
            state.isInvisibleToPlayer = original.invisibleToPlayer();
        }
    }

    private static boolean isWerewolfRenderState(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level != null ? mc.level.getEntity(state.id) : null;
        return entity instanceof Player player
                && WitcheryAttachments.get(player).isWerewolf()
                && WitcheryAttachments.get(player).getTransformationLevel() > 0.01F;
    }

    private static void applySwimmingBoost() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasEffect(WitcheryMobEffects.SWIMMING)) {
            return;
        }

        SwimmingMobEffect.applyPlayerClientTick(mc.player, mc.player.input.hasForwardImpulse(),
                mc.player.hasEffect(WitcheryMobEffects.PARALYSED));
    }

    private static void tryHowl(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return;
        var data = WitcheryAttachments.get(player);
        if (!data.isWerewolf() || data.getTransformationLevel() <= 0.01F) return;
        if (!player.isShiftKeyDown()) return;
        if (player.getXRot() > -75f) return;
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(HowlPayload.INSTANCE);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (WitcheryDimensions.isMirrorWorld(mc.level)) {
            event.setRed(0.0f);
            event.setGreen(0.03f);
            event.setBlue(0.1f);
            return;
        }
        // Gate on the dimension only, matching onExtractLevelRenderState (the sky). The earlier extra
        // isInSpiritWorld() gate could leave the fog overworld-blue while the sky was already spirit-
        // coloured if that flag had not synced yet.
        if (!mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        SpiritWorldSpecialEffects.SpiritColor color = SpiritWorldSpecialEffects.currentSpiritColor();
        event.setRed(color.red());
        event.setGreen(color.green());
        event.setBlue(color.blue());
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;
        if (event.getType() != FogType.NONE) return;

        SpiritWorldSpecialEffects.SpiritFog fog = SpiritWorldSpecialEffects.currentSpiritFog();
        float far = Math.min(event.getFarPlaneDistance(), fog.farPlane());
        float near = Math.min(fog.nearPlane(), far * 0.75F);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        ((net.neoforged.bus.api.ICancellableEvent) event).setCanceled(true);
    }

    /**
     * Auto-confirm the vanilla ExperimentsScreen (Bundle / Trade Rebalance) so world
     * creation and loading are never blocked by it. Equivalent to the user clicking
     * "Done" with no experiments enabled — the current pack selection is applied and
     * the screen is skipped entirely.
     *
     * NeoForge replaces the vanilla screen with ScrollableExperimentsScreen, which
     * extends ExperimentsScreen, so the instanceof check catches both variants.
     * The private onDone() method is called via reflection; it applies the (unmodified)
     * pack selection and triggers the output callback that navigates back to the caller.
     */
    @SubscribeEvent
    public static void onExperimentsScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof ExperimentsScreen screen)) return;
        try {
            java.lang.reflect.Method onDone = ExperimentsScreen.class.getDeclaredMethod("onDone");
            onDone.setAccessible(true);
            onDone.invoke(screen);
        } catch (Exception e) {
            WitcheryMod.LOGGER.debug("Could not auto-confirm ExperimentsScreen: {}", e.getMessage());
            return;
        }
        event.setCanceled(true);
    }

    private record PlayerVisibilityState(boolean invisible, boolean invisibleToPlayer) {}
}
