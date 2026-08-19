package com.emoniph.witchery.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.blocks.entity.BeartrapBlockEntity;
import com.emoniph.witchery.client.renderer.SpiritWorldSpecialEffects;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BroomEntity;
import com.emoniph.witchery.effect.SwimmingMobEffect;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.item.EarmuffsItem;
import com.emoniph.witchery.network.BroomControlPayload;
import com.emoniph.witchery.network.HowlPayload;
import com.emoniph.witchery.network.VampireUltimatePayload;
import com.emoniph.witchery.world.TormentAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = WitcheryMod.MODID, value = Dist.CLIENT)
public class WitcheryClientEvents {

    private static final Map<Integer, PlayerVisibilityState> WEREWOLF_VISIBILITY_OVERRIDES = new HashMap<>();
    private static final Map<Integer, PlayerVisibilityState> MANIFESTED_VISIBILITY_OVERRIDES = new HashMap<>();

    private static boolean lastNightmareLike;
    private static boolean lastBroomControlRiding;
    private static boolean lastBroomControlUp;
    private static boolean lastBroomControlDown;

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.emoniph.witchery.client.SymbolDrawState.clientTick();
        com.emoniph.witchery.client.ScryingCameraState.clientTick();
        com.emoniph.witchery.client.WoodenScannerPoseHarness.clientTick();
        applySwimmingBoost();
        syncBroomControls();
        refreshCottonOnNightmareChange();
    }

    private static void syncBroomControls() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof BroomEntity broom)) {
            if (lastBroomControlRiding) {
                sendBroomControls(false, false);
            }
            lastBroomControlRiding = false;
            lastBroomControlUp = false;
            lastBroomControlDown = false;
            return;
        }

        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keySprint.isDown();
        broom.setRiderVerticalInput(up, down);
        if (!lastBroomControlRiding || up != lastBroomControlUp || down != lastBroomControlDown) {
            sendBroomControls(up, down);
            lastBroomControlRiding = true;
            lastBroomControlUp = up;
            lastBroomControlDown = down;
        }
    }

    private static void sendBroomControls(boolean up, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new BroomControlPayload(up, down));
        }
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
        com.emoniph.witchery.client.WoodenScannerPoseHarness.afterRenderFrame();
    }

    @SubscribeEvent
    public static void onScreenRenderPost(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        com.emoniph.witchery.client.WoodenScannerPoseHarness.afterScreenRender(event.getScreen());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (com.emoniph.witchery.client.ScryingCameraState.isActive()) {
            event.setCanceled(true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof AbstractClientPlayer player) || !isUsingWoodenScanner(player)) {
            return;
        }
        InteractionHand renderHand = com.emoniph.witchery.client.WoodenScannerPoseHarness.isRenderingScanner(player)
                ? InteractionHand.MAIN_HAND
                : player.getUsedItemHand();
        if (event.getHand() == renderHand) {
            renderWoodenScannerFirstPerson(event, player);
        }
        event.setCanceled(true);
    }

    private static boolean isUsingWoodenScanner(Player player) {
        return (player.isUsingItem()
                && player.getUseItem().is(com.emoniph.witchery.init.WitcheryItems.WOODEN_SCANNER.get()))
                || com.emoniph.witchery.client.WoodenScannerPoseHarness.isRenderingScanner(player);
    }

    private static void renderWoodenScannerFirstPerson(RenderHandEvent event, AbstractClientPlayer player) {
        boolean harnessScanner = com.emoniph.witchery.client.WoodenScannerPoseHarness.isRenderingScanner(player);
        ItemStack stack = harnessScanner ? player.getMainHandItem() : player.getUseItem();
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        int packedLight = event.getPackedLight();

        float useTicks = harnessScanner
                ? com.emoniph.witchery.client.WoodenScannerPoseHarness.useTicksWithPartial(player, event.getPartialTick())
                : player.getTicksUsingItem() + event.getPartialTick();
        float focus = Mth.clamp(useTicks / 20.0F, 0.0F, 1.0F);
        focus = (focus * focus + focus * 2.0F) / 3.0F;
        float tension = focus > 0.1F ? Mth.sin((useTicks - 0.1F) * 1.3F) * (focus - 0.1F) : 0.0F;
        float portalCenterLift = focus * 0.22F;
        float portalCenterNudge = focus * -0.03F;
        float armSwing = scannerArmSwing(focus, event.getSwingProgress());

        poseStack.pushPose();
        poseStack.translate(0.22F + portalCenterNudge - (1.0F - focus) * 0.06F,
                0.02F + portalCenterLift - (1.0F - focus) * 0.08F
                        - event.getEquipProgress() * 0.05F + tension * 0.004F,
                -1.08F + focus * 0.03F);
        poseStack.mulPose(Axis.XP.rotationDegrees((1.0F - focus) * -4.0F));
        poseStack.scale(0.94F, 0.94F, 0.94F + focus * 0.06F);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                .renderItem(player, stack, ItemDisplayContext.FIXED, poseStack, collector, packedLight);
        poseStack.popPose();

        renderWoodenScannerHands(poseStack, collector, packedLight, player, event.getEquipProgress(),
                focus, tension, portalCenterLift, portalCenterNudge, armSwing);
    }

    private static void renderWoodenScannerHands(PoseStack poseStack, SubmitNodeCollector collector,
                                                 int packedLight, AbstractClientPlayer player, float equipProgress,
                                                 float focus, float tension, float portalCenterLift,
                                                 float portalCenterNudge, float armSwing) {
        Minecraft mc = Minecraft.getInstance();
        if (player.isInvisible()) {
            return;
        }
        AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        float handLift = portalCenterLift * 0.30F;

        poseStack.pushPose();
        poseStack.translate(0.22F + portalCenterNudge - (1.0F - focus) * 0.05F,
                0.38F + handLift - equipProgress * 0.12F + tension * 0.004F,
                -0.76F + focus * 0.02F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderWoodenScannerHand(renderer, poseStack, collector, packedLight, player, HumanoidArm.RIGHT, armSwing);
        renderWoodenScannerHand(renderer, poseStack, collector, packedLight, player, HumanoidArm.LEFT, armSwing);
        poseStack.popPose();
    }

    private static float scannerArmSwing(float focus, float vanillaSwingProgress) {
        float useGrip = focus * focus * (3.0F - 2.0F * focus);
        float vanillaSwing = Mth.sin(Mth.sqrt(vanillaSwingProgress) * (float) Math.PI) * 0.12F;
        return Mth.clamp(useGrip + vanillaSwing * (1.0F - useGrip), 0.0F, 1.0F);
    }

    private static void renderWoodenScannerHand(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack poseStack,
                                                SubmitNodeCollector collector, int packedLight,
                                                AbstractClientPlayer player, HumanoidArm arm, float armSwing) {
        poseStack.pushPose();
        boolean rightArm = arm == HumanoidArm.RIGHT;
        float side = rightArm ? 1.0F : -1.0F;
        if (!rightArm) {
            poseStack.translate(-0.14F, -0.14F, 0.0F);
        }
        float gripSpread = rightArm
                ? Mth.lerp(armSwing, 0.50F, 0.66F)
                : Mth.lerp(armSwing, 0.30F, 0.22F);
        float gripY = rightArm
                ? Mth.lerp(armSwing, -1.10F, -1.23F)
                : Mth.lerp(armSwing, -1.12F, -1.28F);
        float gripZ = rightArm
                ? Mth.lerp(armSwing, 0.42F, 0.40F)
                : Mth.lerp(armSwing, 0.46F, 0.43F);
        float pitch = Mth.lerp(armSwing, 45.0F, 47.0F);
        float roll = side * Mth.lerp(armSwing, -39.0F, -35.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(side * gripSpread, gripY, gripZ);
        Identifier skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, collector, packedLight, skin,
                    player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
        } else {
            renderer.renderLeftHand(poseStack, collector, packedLight, skin,
                    player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
        }
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        com.emoniph.witchery.client.SymbolHud.render(event.getGuiGraphics(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        com.emoniph.witchery.client.InfusionEnergyHud.render(event.getGuiGraphics());
        com.emoniph.witchery.client.BarkBeltHud.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (tryVampireUltimate(event.getEntity(), event.getHand())) return;
        tryHowl(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryVampireUltimate(event.getEntity(), event.getHand())) return;
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
        if (event.getLevel().dimension().equals(WitcheryDimensions.TORMENT)) {
            var renderState = event.getRenderState();
            renderState.customCloudsRenderer = SpiritWorldSpecialEffects.NO_CLOUDS;
            var sky = renderState.skyRenderState;
            sky.skyColor = TormentAtmosphere.SKY_RGB;
            sky.sunriseAndSunsetColor = 0;
            sky.starBrightness = 0.0F;
            sky.sunAngle = TormentAtmosphere.FIXED_SUN_ANGLE_RADIANS;
            sky.moonAngle = TormentAtmosphere.FIXED_SUN_ANGLE_RADIANS;
            sky.starAngle = TormentAtmosphere.FIXED_SUN_ANGLE_RADIANS;
            sky.shouldRenderDarkDisc = false;
            return;
        }
        if (!event.getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        var renderState = event.getRenderState();
        renderState.customCloudsRenderer = SpiritWorldSpecialEffects.NO_CLOUDS;

        var sky = renderState.skyRenderState;
        sky.sunriseAndSunsetColor = 0;
        sky.starBrightness = 0.0F;

        float sourceAngle = SpiritWorldSpecialEffects.currentSunAngleRadians();
        sky.sunAngle = sourceAngle;
        sky.moonAngle = sourceAngle;
        sky.starAngle = sourceAngle;
        sky.shouldRenderDarkDisc = event.getCamera().position().y < 0.0D;
    }

    /**
     * Vampire clothing ships slim-arm models/textures, but vanilla armor is slim-agnostic so the armor
     * model hook can't see the player's skin. Stash the slim flag for the duration of this player's render
     * (cleared in Post) so {@link com.emoniph.witchery.client.VampireArmorClientExtensions} can pick the
     * slim model + texture. Non-player wearers never fire this event and default to wide, which is correct.
     */
    @SubscribeEvent
    public static void onRenderPlayerSlimPre(RenderPlayerEvent.Pre<?> event) {
        boolean slim = event.getRenderState().skin.model()
                == net.minecraft.world.entity.player.PlayerModelType.SLIM;
        com.emoniph.witchery.client.VampireArmorClientExtensions.SLIM_RENDER.set(slim);
    }

    @SubscribeEvent
    public static void onRenderPlayerSlimPost(RenderPlayerEvent.Post<?> event) {
        com.emoniph.witchery.client.VampireArmorClientExtensions.SLIM_RENDER.set(Boolean.FALSE);
    }

    @SubscribeEvent
    public static void onRenderManifestedPlayerPre(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level != null ? mc.level.getEntity(state.id) : null;
        if (entity instanceof Player player && WitcheryAttachments.get(player).isManifestedGhost()) {
            MANIFESTED_VISIBILITY_OVERRIDES.put(state.id,
                    new PlayerVisibilityState(state.isInvisible, state.isInvisibleToPlayer));
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
        }
    }

    @SubscribeEvent
    public static void onRenderManifestedPlayerPost(RenderPlayerEvent.Post<?> event) {
        AvatarRenderState state = event.getRenderState();
        PlayerVisibilityState original = MANIFESTED_VISIBILITY_OVERRIDES.remove(state.id);
        if (original != null) {
            state.isInvisible = original.invisible();
            state.isInvisibleToPlayer = original.invisibleToPlayer();
        }
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
        ClientPacketDistributor.sendToServer(HowlPayload.INSTANCE);
    }

    private static boolean tryVampireUltimate(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return false;
        if (!player.isShiftKeyDown()) return false;
        if (player.getXRot() > -75f) return false;
        var data = WitcheryAttachments.get(player);
        if (!data.isVampire()
                || data.getVampireUltimate() != WitcheryPlayerData.VampireUltimate.SWARM
                || data.getVampireUltimateCharges() <= 0) {
            return false;
        }
        ClientPacketDistributor.sendToServer(VampireUltimatePayload.INSTANCE);
        return true;
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
        if (mc.level.dimension().equals(WitcheryDimensions.TORMENT)) {
            event.setRed(TormentAtmosphere.red(TormentAtmosphere.FOG_RGB));
            event.setGreen(TormentAtmosphere.green(TormentAtmosphere.FOG_RGB));
            event.setBlue(TormentAtmosphere.blue(TormentAtmosphere.FOG_RGB));
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
