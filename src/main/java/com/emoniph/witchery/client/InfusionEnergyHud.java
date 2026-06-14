package com.emoniph.witchery.client;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The infusion energy bar (port of source {@code RenderInfusionEnergyBar}) — a vertical column showing
 * the witch's current/max infusion energy, plus a second column for the active creature-power charges.
 * It is the resource that gates every active infusion power, so it is only shown while the witch holds a
 * focus that spends it (the Witch Hand or the Mystic Branch). Drawn from
 * {@code WitcheryClientEvents.onRenderGui} alongside {@link SymbolHud}.
 */
public final class InfusionEnergyHud {

    private static final int BAR_W = 9;
    private static final int BAR_H = 50;
    private static final int MARGIN = 8;
    private static final int CREATURE_MAX = 20; // source caps creature-power charges at 20

    private InfusionEnergyHud() {
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        if (!holdingFocus(player)) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType type = data.getInfusionType();
        if (type == InfusionType.NONE) {
            return;
        }
        int energy = data.getInfusionEnergy();
        int max = data.getInfusionMaxEnergy();
        if (max <= 0) {
            return;
        }

        int x = MARGIN;
        int y = g.guiHeight() / 2 - BAR_H / 2;
        int color = infusionColor(type);

        // Low-energy pulse: brighten toward white as the pool runs dry.
        float frac = (float) energy / max;
        if (frac < 0.2f) {
            float pulse = 0.5f + 0.5f * Mth.sin((System.currentTimeMillis() % 1000L) / 1000.0f * Mth.TWO_PI);
            color = lerpColor(color, 0xFFFFFFFF, pulse * 0.6f);
        }

        drawBar(g, x, y, energy, max, color);
        String label = energy + "/" + max;
        g.drawString(mc.font, label, x + BAR_W + 3, y, color, true);

        String cpName = data.getCreaturePowerType();
        int cpCharges = data.getCreaturePowerCharges();
        if (cpName != null && !cpName.isEmpty() && cpCharges > 0) {
            int cx = x + BAR_W + 2;
            drawBar(g, cx, y, Math.min(cpCharges, CREATURE_MAX), CREATURE_MAX, 0xFFB07850);
            g.drawString(mc.font, String.valueOf(cpCharges), cx + BAR_W + 3, y + BAR_H - 9, 0xFFB07850, true);
        }
    }

    private static void drawBar(GuiGraphics g, int x, int y, int value, int max, int color) {
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xC0000000); // frame
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF1A1A1A);                 // empty channel
        int fillH = Mth.clamp(Math.round((float) value / max * BAR_H), 0, BAR_H);
        if (fillH > 0) {
            g.fill(x, y + (BAR_H - fillH), x + BAR_W, y + BAR_H, color); // fill from the bottom up
        }
    }

    private static boolean holdingFocus(LocalPlayer player) {
        return isFocus(player.getMainHandItem()) || isFocus(player.getOffhandItem());
    }

    private static boolean isFocus(ItemStack stack) {
        return stack.is(WitcheryItems.WITCH_HAND.get()) || stack.is(WitcheryItems.MYSTIC_BRANCH.get());
    }

    private static int infusionColor(InfusionType type) {
        return switch (type) {
            case LIGHT      -> 0xFFFFF0B0; // pale gold
            case OVERWORLD  -> 0xFF6FD08A; // green
            case OTHERWHERE -> 0xFFB388E8; // amethyst
            case INFERNAL   -> 0xFFE8702B; // ember orange
            case NONE       -> 0xFFFFFFFF;
        };
    }

    private static int lerpColor(int from, int to, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = lerp((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int gC = lerp((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (gC << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return a + Math.round((b - a) * t);
    }
}
