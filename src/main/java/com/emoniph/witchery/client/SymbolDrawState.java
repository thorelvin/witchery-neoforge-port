package com.emoniph.witchery.client;

import com.emoniph.witchery.item.MysticBranchItem;
import com.emoniph.witchery.network.SpellPreparedPayload;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

/**
 * Client-side air-glyph recognition for the Mystic Branch. Runs once per client tick while the local
 * player channels the branch: it watches the look deltas, appends strokes, and tells the server which
 * symbol has been drawn so it can be cast on release.
 *
 * <p>Detection is anchor-relative (as in {@code ItemMysticBranch.onUsingTick}, 1.7.10-0.24.1, and
 * Witchery Resurrected): each tick the look's offset from a moving anchor is measured; once the
 * dominant axis passes {@link #THRESHOLD} a stroke is recorded and the anchor is re-taken at the
 * current look. Drawing a glyph is therefore a single continuous sweep — a longer sweep in one
 * direction simply yields more strokes. {@link SymbolHud} draws the glyph live on screen so the
 * count is always visible (this is what makes the gesture reliable).</p>
 */
public final class SymbolDrawState {

    /** Degrees of look movement that commit one stroke. Also the on-screen length of a stroke. */
    public static final float THRESHOLD = 8.0F;
    private static final int MAX_STROKES = 15;

    private static boolean active;
    private static float anchorYaw;
    private static float anchorPitch;
    private static byte[] strokes = new byte[0];
    private static int lastSentId = -1;
    private static int lastSentLevel = -1;
    private static String preparedNameKey;

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

        // Once a full symbol matches, lock it in (the anchor stops moving; the HUD keeps the glyph).
        if (strokes.length >= MAX_STROKES || EffectRegistry.instance().contains(strokes)) {
            return;
        }

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float pitchDiff = anchorPitch - pitch;                 // + = looked up
        float yawDiff = Mth.wrapDegrees(anchorYaw - yaw);      // - = turned right, + = turned left

        // Pick the axis that moved furthest, so a clean cardinal flick is never mis-read as the
        // other axis (the old pitch-first ordering turned diagonals into spurious up/down strokes).
        int stroke = -1;
        if (Math.abs(pitchDiff) >= Math.abs(yawDiff)) {
            if (pitchDiff >= THRESHOLD) {
                stroke = 0; // up
            } else if (pitchDiff <= -THRESHOLD) {
                stroke = 1; // down
            }
        } else {
            if (yawDiff <= -THRESHOLD) {
                stroke = 2; // right
            } else if (yawDiff >= THRESHOLD) {
                stroke = 3; // left
            }
        }

        if (stroke < 0) {
            return;
        }

        strokes = Arrays.copyOf(strokes, strokes.length + 1);
        strokes[strokes.length - 1] = (byte) stroke;
        anchorYaw = yaw;
        anchorPitch = pitch;
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.7F);

        SymbolEffect effect = EffectRegistry.instance().getEffect(strokes);
        if (effect != null) {
            int level = EffectRegistry.instance().getLevel(strokes);
            preparedNameKey = effect.getNameKey();
            if (effect.getEffectId() != lastSentId || level != lastSentLevel) {
                lastSentId = effect.getEffectId();
                lastSentLevel = level;
                ClientPacketDistributor.sendToServer(new SpellPreparedPayload(effect.getEffectId(), level));
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.5F);
            }
        } else {
            preparedNameKey = null;
        }
    }

    // ── State exposed to the HUD ──────────────────────────────────────────────

    public static boolean isActive() {
        return active;
    }

    public static byte[] strokesView() {
        return strokes;
    }

    public static float anchorYaw() {
        return anchorYaw;
    }

    public static float anchorPitch() {
        return anchorPitch;
    }

    public static String preparedNameKey() {
        return preparedNameKey;
    }

    private static void begin(LocalPlayer player) {
        active = true;
        strokes = new byte[0];
        anchorYaw = player.getYRot();
        anchorPitch = player.getXRot();
        lastSentId = -1;
        lastSentLevel = -1;
        preparedNameKey = null;
    }

    private static void reset() {
        active = false;
        strokes = new byte[0];
        lastSentId = -1;
        lastSentLevel = -1;
        preparedNameKey = null;
    }
}
