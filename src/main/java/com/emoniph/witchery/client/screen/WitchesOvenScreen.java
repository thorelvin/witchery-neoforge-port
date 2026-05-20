package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.WitchesOvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class WitchesOvenScreen extends AbstractContainerScreen<WitchesOvenMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/gui/witches_oven.png");

    public WitchesOvenScreen(WitchesOvenMenu menu, Inventory inv, Component title) {
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

        if (menu.isLit()) {
            int fireHeight = menu.getBurnProgress();
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 56, y + 36 + 12 - fireHeight, 176.0f, 12.0f - fireHeight, 14, fireHeight + 2, 256, 256);
        }

        int arrowWidth = menu.getCookProgress();
        if (arrowWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 79, y + 21, 176.0f, 14.0f, arrowWidth + 1, 16, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
