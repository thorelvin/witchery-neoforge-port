package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.blocks.entity.BeartrapBlockEntity;
import com.emoniph.witchery.client.renderer.SpiritWorldSpecialEffects;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.item.EarmuffsItem;
import com.emoniph.witchery.network.HowlPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
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
        if (!mc.level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(mc.player);
        if (!data.isInSpiritWorld()) return;

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
