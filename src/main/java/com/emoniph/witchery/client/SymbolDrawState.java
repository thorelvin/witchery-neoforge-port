package com.emoniph.witchery.client;

import com.emoniph.witchery.item.MysticBranchItem;
import com.emoniph.witchery.network.SpellPreparedPayload;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

/**
 * Client-side air-glyph recognition for the Mystic Branch. Run once per client tick while the
 * local player is channelling the branch: it watches the look deltas, appends strokes, and tells
 * the server which symbol has been drawn so it can be cast on release.
 *
 * Source parity: {@code ItemMysticBranch.onUsingTick} (1.7.10-0.24.1).
 */
public final class SymbolDrawState {

    private static final float THRESHOLD = 7.0F;
    /** Per-tick movement below this counts as "the flick has stopped", re-arming the next stroke. */
    private static final float SETTLE = 2.5F;
    private static final int MAX_STROKES = 15;

    private static boolean active;
    private static float anchorYaw;
    private static float anchorPitch;
    private static float prevYaw;
    private static float prevPitch;
    /** True after a stroke until the look settles, so one flick = one stroke (and glyphs chain). */
    private static boolean awaitingSettle;
    private static byte[] strokes = new byte[0];
    private static int lastSentId = -1;
    private static int lastSentLevel = -1;

    private SymbolDrawState() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            reset();
            return;
        }

        boolean usingBranch = player.isUsingItem()
                && player.getUseItem().getItem() instanceof MysticBranchItem;
        if (!usingBranch) {
            reset();
            return;
        }

        if (!active) {
            begin(player);
            return;
        }

        float pitch = player.getXRot();
        float yaw = player.getYRot();

        // How much the look moved this tick — used to tell when a flick has ended.
        float tickPitch = Math.abs(pitch - prevPitch);
        float tickYaw = Math.abs(Mth.wrapDegrees(yaw - prevYaw));
        boolean moving = tickPitch > SETTLE || tickYaw > SETTLE;
        prevPitch = pitch;
        prevYaw = yaw;

        // Stop appending once a full symbol matches or the stroke budget is spent.
        if (strokes.length >= MAX_STROKES || EffectRegistry.instance().contains(strokes)) {
            return;
        }

        // After a stroke, wait for the flick to stop before arming the next one. This makes one
        // flick = exactly one stroke (a fast/large flick no longer counts as several), so the
        // player can chain a multi-stroke glyph instead of an early short spell firing.
        if (awaitingSettle) {
            if (!moving) {
                awaitingSettle = false;
                anchorPitch = pitch;
                anchorYaw = yaw;
            }
            return;
        }

        float pitchDiff = anchorPitch - pitch;
        float yawDiff = Mth.wrapDegrees(anchorYaw - yaw);

        int stroke = -1;
        if (pitchDiff >= THRESHOLD) {
            stroke = 0; // up
        } else if (pitchDiff <= -THRESHOLD) {
            stroke = 1; // down
        } else if (yawDiff <= -THRESHOLD) {
            stroke = 2;
        } else if (yawDiff >= THRESHOLD) {
            stroke = 3;
        }

        if (stroke < 0) {
            return;
        }

        strokes = Arrays.copyOf(strokes, strokes.length + 1);
        strokes[strokes.length - 1] = (byte) stroke;
        anchorYaw = yaw;
        anchorPitch = pitch;
        awaitingSettle = true;

        SymbolEffect effect = EffectRegistry.instance().getEffect(strokes);
        if (effect != null) {
            int level = EffectRegistry.instance().getLevel(strokes);
            if (effect.getEffectId() != lastSentId || level != lastSentLevel) {
                lastSentId = effect.getEffectId();
                lastSentLevel = level;
                ClientPacketDistributor.sendToServer(new SpellPreparedPayload(effect.getEffectId(), level));
                player.displayClientMessage(Component.translatable("witchery.symbol.prepared",
                        Component.translatable(effect.getNameKey())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 1.5F);
            }
        }
    }

    private static void begin(LocalPlayer player) {
        active = true;
        strokes = new byte[0];
        anchorYaw = player.getYRot();
        anchorPitch = player.getXRot();
        prevYaw = player.getYRot();
        prevPitch = player.getXRot();
        awaitingSettle = false;
        lastSentId = -1;
        lastSentLevel = -1;
    }

    private static void reset() {
        active = false;
        strokes = new byte[0];
        awaitingSettle = false;
        lastSentId = -1;
        lastSentLevel = -1;
    }
}
