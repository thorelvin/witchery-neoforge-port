package com.emoniph.witchery.client;

import com.emoniph.witchery.book.page.StationRecipePage;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Client renderer for {@link StationRecipePage}: draws the station GUI background and renders each
 * real {@link net.minecraft.world.item.ItemStack} at its slot via
 * {@code parentScreen.renderItemStack(...)}, which also draws the stack count and a hover tooltip.
 */
public class StationRecipePageRenderer extends BookPageRenderer<StationRecipePage> {

    public StationRecipePageRenderer(StationRecipePage page) {
        super(page);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int pageWidth = BookEntryScreen.PAGE_WIDTH;
        if (this.page.hasTitle()) {
            this.renderTitle(graphics, this.page.getTitle(), true, pageWidth / 2, 0);
        }

        int bgWidth = this.page.getBgWidth();
        int bgHeight = this.page.getBgHeight();
        int bgX = (pageWidth - bgWidth) / 2;
        int bgY = 24;

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.page.getBackground(),
                this.left + bgX, this.top + bgY, 0.0F, 0.0F, bgWidth, bgHeight, bgWidth, bgHeight);

        for (StationRecipePage.Slot slot : this.page.getSlots()) {
            this.parentScreen.renderItemStack(graphics,
                    this.left + bgX + slot.x(), this.top + bgY + slot.y(), mouseX, mouseY, slot.stack());
        }

        this.renderBookTextHolder(graphics, this.page.getText(), 0, bgY + bgHeight + 6, pageWidth);
    }
}
