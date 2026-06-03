package com.emoniph.witchery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Draws the Mystic Branch glyph on screen while it is being traced — the committed strokes as a
 * connected path from screen centre, plus a live "pen" segment for the stroke in progress, plus the
 * spell name once a glyph is recognised. This live feedback is what makes the air-glyph gesture
 * reliable to draw (Witchery Resurrected does the same with a shader). See {@link SymbolDrawState}.
 */
public final class SymbolHud {

    /** On-screen pixels per stroke (matches {@link SymbolDrawState#THRESHOLD} degrees). */
    private static final int SEG = 20;
    private static final float SCALE = SEG / SymbolDrawState.THRESHOLD;
    private static final int THICK = 3;

    private static final int COMMITTED = 0xFFB388E8; // amethyst
    private static final int PENDING   = 0x99B388E8;
    private static final int TIP       = 0xFFFFFFFF;
    private static final int PREPARED  = 0xFF6FD08A; // green once a spell is recognised

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
        int pathColor = prepared ? PREPARED : COMMITTED;

        int cx = g.guiWidth() / 2;
        int cy = g.guiHeight() / 2 - 30; // a little above the crosshair

        // Committed strokes — connected axis-aligned segments.
        int px = cx;
        int py = cy;
        dot(g, px, py, pathColor); // start node
        for (byte s : strokes) {
            int nx = px + dx(s) * SEG;
            int ny = py + dy(s) * SEG;
            thickLine(g, px, py, nx, ny, pathColor);
            px = nx;
            py = ny;
        }

        // Pending stroke — the live pen from the last node toward where the look now points,
        // relative to the moving anchor. When it reaches one segment's length, a stroke commits.
        if (!prepared) {
            float yaw = player.getViewYRot(partialTick);
            float pitch = player.getViewXRot(partialTick);
            float dxDeg = Mth.wrapDegrees(yaw - SymbolDrawState.anchorYaw());
            float dyDeg = pitch - SymbolDrawState.anchorPitch();
            int tipX = px + Math.round(Mth.clamp(dxDeg * SCALE, -SEG * 1.5f, SEG * 1.5f));
            int tipY = py + Math.round(Mth.clamp(dyDeg * SCALE, -SEG * 1.5f, SEG * 1.5f));
            steppedLine(g, px, py, tipX, tipY, PENDING);
            dot(g, tipX, tipY, TIP);
        }

        // Spell name once recognised.
        if (prepared) {
            String name = Component.translatable(SymbolDrawState.preparedNameKey()).getString();
            int w = mc.font.width(name);
            g.drawString(mc.font, name, cx - w / 2, cy + 26, PREPARED, true);
        }
    }

    private static int dx(byte s) {
        return s == 2 ? 1 : s == 3 ? -1 : 0; // right / left
    }

    private static int dy(byte s) {
        return s == 1 ? 1 : s == 0 ? -1 : 0; // down / up
    }

    private static void thickLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int h = THICK / 2;
        if (y0 == y1) {
            g.fill(Math.min(x0, x1) - h, y0 - h, Math.max(x0, x1) + h, y0 + THICK - h, color);
        } else {
            g.fill(x0 - h, Math.min(y0, y1) - h, x0 + THICK - h, Math.max(y0, y1) + h, color);
        }
    }

    private static void steppedLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 3;
        for (int i = 1; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            g.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private static void dot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 2, y + 2, color);
    }
}
