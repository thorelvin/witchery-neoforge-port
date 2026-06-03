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
    /** Ticks to ignore after a stroke, so one quick flick can't register as several strokes. */
    private static final int STROKE_COOLDOWN = 4;
    private static final int MAX_STROKES = 15;

    private static boolean active;
    private static float anchorYaw;
    private static float anchorPitch;
    /** Counts down after each stroke; the look anchor is re-taken when it reaches zero. */
    private static int cooldown;
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

        // Stop appending once a full symbol matches or the stroke budget is spent.
        if (strokes.length >= MAX_STROKES || EffectRegistry.instance().contains(strokes)) {
            return;
        }

        // Brief cooldown after each stroke so a single quick flick can't count several times.
        // When it ends, re-take the anchor at the current look so detection restarts fresh — this
        // is what lets a multi-stroke glyph chain (flick, flick, flick) instead of one fast flick
        // firing a short spell early.
        if (cooldown > 0) {
            cooldown--;
            if (cooldown == 0) {
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
        cooldown = STROKE_COOLDOWN;

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
        cooldown = 0;
        lastSentId = -1;
        lastSentLevel = -1;
    }

    private static void reset() {
        active = false;
        strokes = new byte[0];
        cooldown = 0;
        lastSentId = -1;
        lastSentLevel = -1;
    }
}
