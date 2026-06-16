package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.DistilleryMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class DistilleryScreen extends AbstractContainerScreen<DistilleryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/gui/distillery.png");

    // Altar power gauge (relative to the GUI top-left). Sits just right of the output slots.
    private static final int GAUGE_X = 152;
    private static final int GAUGE_Y = 16;
    private static final int GAUGE_W = 8;
    private static final int GAUGE_H = 50;
    // Fill tint thresholds, in permille (0..1000) of the altar's effective max power.
    private static final int FILL_LOW = 200;
    private static final int FILL_OK = 500;

    private static final int COLOR_BORDER = 0xFF2B2B2B;
    private static final int COLOR_TRACK = 0xFF555555;
    private static final int COLOR_GREEN = 0xFF4CAF50;
    private static final int COLOR_AMBER = 0xFFE0A030;
    private static final int COLOR_RED = 0xFFD23A2E;

    public DistilleryScreen(DistilleryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = (this.imageWidth - this.font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        if (!menu.hasPower()) {
            // No power: draw the red "X" only. Its source tile is opaque panel grey, so it
            // fully hides the arrow art baked into the background. The progress arrow is
            // intentionally not drawn here so the X and arrow never show at the same time.
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 77, y + 34, 197.0f, 0.0f, 28, 17, 256, 256);
        } else {
            int arrowWidth = menu.getCookProgress();
            if (arrowWidth > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 77, y + 37, 176.0f, 29.0f, arrowWidth, 9, 256, 256);

                int bubbleHeight = menu.getBubbleFrameHeight();
                if (bubbleHeight > 0) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 40, y + 18 + 29 - bubbleHeight, 185.0f, 29.0f - bubbleHeight, 12, bubbleHeight, 256, 256);
                }
            }
        }

        renderPowerGauge(graphics, x, y);
    }

    /** Vertical altar-power bar, tinted green/amber/red as the altar drains toward empty. */
    private void renderPowerGauge(GuiGraphics graphics, int x, int y) {
        int left = x + GAUGE_X;
        int top = y + GAUGE_Y;
        int right = left + GAUGE_W;
        int bottom = top + GAUGE_H;

        graphics.fill(left, top, right, bottom, COLOR_BORDER);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, COLOR_TRACK);

        int innerHeight = GAUGE_H - 2;
        int permille = menu.getAltarPowerFill();
        int fillHeight = permille * innerHeight / 1000;
        if (fillHeight > 0) {
            int color = permille >= FILL_OK ? COLOR_GREEN : permille >= FILL_LOW ? COLOR_AMBER : COLOR_RED;
            graphics.fill(left + 1, bottom - 1 - fillHeight, right - 1, bottom - 1, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        renderPowerTooltip(graphics, mouseX, mouseY);
    }

    private void renderPowerTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = this.leftPos + GAUGE_X;
        int top = this.topPos + GAUGE_Y;
        if (mouseX < left || mouseX >= left + GAUGE_W || mouseY < top || mouseY >= top + GAUGE_H) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.witchery.distillery.power_title")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (!menu.hasAltar()) {
            lines.add(Component.translatable("gui.witchery.distillery.no_altar").withStyle(ChatFormatting.RED));
        } else {
            int permille = menu.getAltarPowerFill();
            Component hint = permille >= FILL_OK
                    ? Component.translatable("gui.witchery.distillery.power_full").withStyle(ChatFormatting.GREEN)
                    : permille >= FILL_LOW
                    ? Component.translatable("gui.witchery.distillery.power_ebbing").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.witchery.distillery.power_waning").withStyle(ChatFormatting.RED);
            lines.add(hint);
            // Dim, secondary numeric readout for the curious.
            lines.add(Component.literal(menu.getAltarPower() + " of " + menu.getAltarMaxPower())
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }
}
