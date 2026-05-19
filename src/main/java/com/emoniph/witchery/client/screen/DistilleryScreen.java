package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.DistilleryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class DistilleryScreen extends AbstractContainerScreen<DistilleryMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/gui/distillery.png");

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

        graphics.blit(TEXTURE, x, y, this.imageWidth, this.imageHeight, 0.0f, 0.0f, 256.0f, 256.0f);

        if (!menu.hasPower()) {
            graphics.blit(TEXTURE, x + 77, y + 34, 28, 17, 197.0f, 0.0f, 256.0f, 256.0f);
        }

        int arrowWidth = menu.getCookProgress();
        if (arrowWidth > 0) {
            graphics.blit(TEXTURE, x + 77, y + 37, arrowWidth, 9, 176.0f, 29.0f, 256.0f, 256.0f);

            int bubbleHeight = menu.getBubbleFrameHeight();
            if (bubbleHeight > 0) {
                graphics.blit(TEXTURE, x + 40, y + 18 + 29 - bubbleHeight, 12, bubbleHeight, 185.0f, (float) (29 - bubbleHeight), 256.0f, 256.0f);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
