package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.item.EarmuffsItem;
import com.emoniph.witchery.network.HowlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = WitcheryMod.MODID, value = Dist.CLIENT)
public class WitcheryClientEvents {

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
        if ("entity.ghast.scream".equals(event.getName()) || "entity.banshee.scream".equals(event.getName())) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onRenderWerewolfPlayerPre(RenderPlayerEvent.Pre<?> event) {
        if (isWerewolfRenderState(event.getRenderState())) {
            event.getRenderer().getModel().setAllVisible(false);
        }
    }

    @SubscribeEvent
    public static void onRenderWerewolfPlayerPost(RenderPlayerEvent.Post<?> event) {
        AvatarRenderState state = event.getRenderState();
        if (isWerewolfRenderState(state)) {
            restorePlayerModelVisibility(event.getRenderer().getModel(), state);
        }
    }

    private static boolean isWerewolfRenderState(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level != null ? mc.level.getEntity(state.id) : null;
        return entity instanceof Player player && WitcheryAttachments.get(player).isWerewolf();
    }

    private static void restorePlayerModelVisibility(PlayerModel model, AvatarRenderState state) {
        if (state.isSpectator) {
            model.setAllVisible(false);
            model.head.visible = true;
            model.hat.visible = true;
            return;
        }

        model.setAllVisible(true);
        model.hat.visible = state.showHat;
        model.jacket.visible = state.showJacket;
        model.leftPants.visible = state.showLeftPants;
        model.rightPants.visible = state.showRightPants;
        model.leftSleeve.visible = state.showLeftSleeve;
        model.rightSleeve.visible = state.showRightSleeve;
    }

    private static void tryHowl(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!WitcheryAttachments.get(player).isWerewolf()) return;
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

        float r, g, b;
        if (data.isDemonicNightmare()) {
            r = 1.0f; g = 0.0f; b = 0.0f;
        } else if (data.isNightmare()) {
            r = 0.0f; g = 1.0f; b = 0.0f;
        } else {
            r = 0.8f; g = 0.2f; b = 0.6f;
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
