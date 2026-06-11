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

    /**
     * Station GUI panels are 60–70px tall — taller than a book page (PAGE_HEIGHT 155) can spare once the
     * title (y=24) and a caption are placed, so at native size they leave too little room and the caption
     * overflows the bottom. Cap the drawn panel height and scale the whole composite (background + items)
     * to fit, so every recipe page keeps room for its text.
     */
    private static final int MAX_PANEL_HEIGHT = 52;
    private static final int PANEL_TOP = 24;
    private static final int TEXT_GAP = 6;

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
        float scale = Math.min(1.0F, MAX_PANEL_HEIGHT / (float) bgHeight);
        int drawnW = Math.round(bgWidth * scale);
        int drawnH = Math.round(bgHeight * scale);
        int bgX = (pageWidth - drawnW) / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(this.left + bgX, this.top + PANEL_TOP);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.page.getBackground(),
                0, 0, 0.0F, 0.0F, bgWidth, bgHeight, bgWidth, bgHeight);
        // Map the mouse into the panel's pre-scale space so item hover/tooltips still line up.
        int localMouseX = Math.round((mouseX - (this.left + bgX)) / scale);
        int localMouseY = Math.round((mouseY - (this.top + PANEL_TOP)) / scale);
        for (StationRecipePage.Slot slot : this.page.getSlots()) {
            this.parentScreen.renderItemStack(graphics, slot.x(), slot.y(), localMouseX, localMouseY, slot.stack());
        }
        graphics.pose().popMatrix();

        // Auto-fit the caption into the space left below the panel: the height-bounded overload shrinks
        // the text just enough to fit (down a size or two on long captions), so nothing overflows the page.
        int textY = PANEL_TOP + drawnH + TEXT_GAP;
        int textHeight = BookEntryScreen.PAGE_HEIGHT - textY - 2;
        this.renderBookTextHolder(graphics, this.page.getText(), 0, textY, pageWidth, textHeight);
    }
}
