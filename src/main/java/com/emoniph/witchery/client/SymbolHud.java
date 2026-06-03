package com.emoniph.witchery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Draws the Mystic Branch glyph being traced as a left-to-right row of direction arrows — one per
 * committed stroke, in order, plus a growing "pending" arrow for the stroke in progress and the
 * spell name once recognised.
 *
 * <p>A row of discrete arrows (rather than one connected path) is deliberate: glyphs like
 * Flipendo (R R L) or Expelliarmus (U U D) retrace themselves, and a connected path overdraws the
 * retraced segments so it looks like a stroke "didn't count". Per-stroke arrows can never overlap,
 * so the stroke count is always plain — matching how the original 1.7.10 client drew per-stroke
 * icons. Recognition is unchanged (see {@link SymbolDrawState}).</p>
 */
public final class SymbolHud {

    private static final int ARROW = 5;     // half-size of an arrow (px)
    private static final int CELL = 15;     // horizontal spacing between arrows
    private static final int ROW_ABOVE = 38; // pixels above screen centre

    private static final int COMMITTED = 0xFFB388E8; // amethyst
    private static final int PREPARED  = 0xFF6FD08A; // green once a spell is recognised
    private static final int PENDING_RGB = 0xB388E8;

    private SymbolHud() {
    }

    public static void render(GuiGraphics g, float partialTick) {
        if (!SymbolDrawState.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        byte[] strokes = SymbolDrawState.strokesView();
        boolean prepared = SymbolDrawState.preparedNameKey() != null;
        int color = prepared ? PREPARED : COMMITTED;

        // Pending stroke: where the look is moving relative to the moving anchor (0..1 of a stroke).
        int pendingDir = -1;
        float pendingProgress = 0.0f;
        if (!prepared) {
            float dxDeg = Mth.wrapDegrees(player.getViewYRot(partialTick) - SymbolDrawState.anchorYaw());
            float dyDeg = player.getViewXRot(partialTick) - SymbolDrawState.anchorPitch();
            float mag;
            if (Math.abs(dyDeg) >= Math.abs(dxDeg)) {
                pendingDir = dyDeg < 0 ? 0 : 1; // up / down (look up = smaller pitch)
                mag = Math.abs(dyDeg);
            } else {
                pendingDir = dxDeg > 0 ? 2 : 3; // right / left
                mag = Math.abs(dxDeg);
            }
            pendingProgress = Mth.clamp(mag / SymbolDrawState.THRESHOLD, 0.0f, 1.0f);
            if (pendingProgress < 0.18f) {
                pendingDir = -1; // too small to show — avoids flicker at rest
            }
        }

        int cells = strokes.length + (pendingDir >= 0 ? 1 : 0);
        int cx = g.guiWidth() / 2;
        int rowY = g.guiHeight() / 2 - ROW_ABOVE;
        int startX = cx - (cells * CELL) / 2 + CELL / 2;

        if (cells == 0) {
            // Channelling but nothing drawn yet — a faint reticle so the player knows it is live.
            dot(g, cx, rowY, 0x66B388E8);
        }

        for (int i = 0; i < strokes.length; i++) {
            arrow(g, startX + i * CELL, rowY, strokes[i], ARROW, color);
        }
        if (pendingDir >= 0) {
            int alpha = 30 + (int) (pendingProgress * 200);
            int faded = (alpha << 24) | PENDING_RGB;
            int s = 2 + Math.round(pendingProgress * (ARROW - 2));
            arrow(g, startX + strokes.length * CELL, rowY, (byte) pendingDir, s, faded);
        }

        if (prepared) {
            String name = Component.translatable(SymbolDrawState.preparedNameKey()).getString();
            int w = mc.font.width(name);
            g.drawString(mc.font, name, cx - w / 2, rowY + ARROW + 6, PREPARED, true);
        }
    }

    /** Solid triangle pointing in {@code dir} (0=up,1=down,2=right,3=left), centred at (cx,cy). */
    private static void arrow(GuiGraphics g, int cx, int cy, byte dir, int s, int color) {
        if (s < 1) {
            return;
        }
        int span = 2 * s;
        if (dir == 0 || dir == 1) {
            for (int r = 0; r <= span; r++) {
                float t = (float) r / span;            // 0 at top .. 1 at bottom
                float frac = dir == 0 ? t : (1.0f - t); // up: narrow at top; down: narrow at bottom
                int hw = Math.round(frac * s);
                int y = cy - s + r;
                g.fill(cx - hw, y, cx + hw + 1, y + 1, color);
            }
        } else {
            for (int c = 0; c <= span; c++) {
                float t = (float) c / span;             // 0 at left .. 1 at right
                float frac = dir == 2 ? (1.0f - t) : t; // tip points in the arrow direction
                int hh = Math.round(frac * s);
                int x = cx - s + c;
                g.fill(x, cy - hh, x + 1, cy + hh + 1, color);
            }
        }
    }

    private static void dot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 2, y + 2, color);
    }
}
