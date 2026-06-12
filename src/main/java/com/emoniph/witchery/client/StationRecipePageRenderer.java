package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.book.page.StationRecipePage;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Client renderer for {@link StationRecipePage}: draws the station GUI background and renders each
 * real {@link net.minecraft.world.item.ItemStack} at its slot via
 * {@code parentScreen.renderItemStack(...)}, which also draws the stack count and a hover tooltip.
 */
public class StationRecipePageRenderer extends BookPageRenderer<StationRecipePage> {

    /**
     * Panels are drawn at native 1:1 scale. They must be authored to fit a book page: ≤ {@code PAGE_WIDTH}
     * wide and short enough to leave room for the caption below (all current panels are ≤112×70).
     * <p>
     * We deliberately do <em>not</em> shrink panels to a max height: {@code GuiGraphics.renderItem} only
     * honours the <em>translation</em> of the 2D pose, not its scale (the item model always draws at 16px),
     * so a downscaled background leaves full-size 16px items overflowing the shrunken slots — the alignment
     * bug this replaces. At scale 1.0 the 16px items land exactly in the 16px slots. The only fallback is a
     * width clamp, so an over-wide panel shrinks rather than running off the page (its items would then
     * overflow, but no real recipe panel exceeds the page width).
     */
    private static final int PANEL_TOP = 24;
    private static final int PANEL_TOP_NO_TITLE = 7;
    private static final int TEXT_GAP = 6;
    private static final int INK_SOFT = 0x9953341A;
    private static final int BUBBLE = 0xCC6F5438;
    private static final int BUBBLE_LIGHT = 0xAAA9875A;
    private static final Identifier FURNACE_FLAME = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "book/furnace_flame");

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
        float scale = Math.min(1.0F, pageWidth / (float) bgWidth);
        int drawnW = Math.round(bgWidth * scale);
        int drawnH = Math.round(bgHeight * scale);
        int bgX = (pageWidth - drawnW) / 2;
        int panelTop = this.page.hasTitle() ? PANEL_TOP : PANEL_TOP_NO_TITLE;

        if (this.page.shouldShowBackground()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(bgX, panelTop);
            graphics.pose().scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.page.getBackground(),
                    0, 0, 0.0F, 0.0F, bgWidth, bgHeight, bgWidth, bgHeight);
            graphics.pose().popMatrix();
        }

        if (this.page.hasOvenCues()) {
            this.renderOvenCues(graphics, bgX, panelTop, scale);
        }
        if (this.page.hasDistilleryCues()) {
            this.renderDistilleryCues(graphics, bgX, panelTop, scale);
        }
        for (StationRecipePage.Slot slot : this.page.getSlots()) {
            int itemX = bgX + Math.round(slot.x() * scale);
            int itemY = panelTop + Math.round(slot.y() * scale);
            this.parentScreen.renderItemStack(graphics, itemX, itemY, mouseX, mouseY, slot.stack());
        }

        // Auto-fit the caption into the space left below the panel: the height-bounded overload shrinks
        // the text just enough to fit (down a size or two on long captions), so nothing overflows the page.
        int textY = panelTop + drawnH + TEXT_GAP;
        int textHeight = BookEntryScreen.PAGE_HEIGHT - textY - 2;
        this.renderBookTextHolder(graphics, this.page.getText(), 0, textY, pageWidth, textHeight);
    }

    private void renderOvenCues(GuiGraphics graphics, int bgX, int panelTop, float scale) {
        int arrowY = panelTop + Math.round(21 * scale);
        int arrowLeft = bgX + Math.round(31 * scale);
        int arrowBase = bgX + Math.round(49 * scale);
        int arrowTip = bgX + Math.round(63 * scale);

        this.renderPointedArrow(graphics, arrowLeft, arrowY, arrowBase, arrowTip);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FURNACE_FLAME,
                bgX + Math.round(9 * scale), panelTop + Math.round(31 * scale),
                Math.round(14 * scale), Math.round(14 * scale));
    }

    private void renderDistilleryCues(GuiGraphics graphics, int bgX, int panelTop, float scale) {
        this.renderPointedArrow(graphics,
                bgX + Math.round(39 * scale),
                panelTop + Math.round(27 * scale),
                bgX + Math.round(52 * scale),
                bgX + Math.round(63 * scale));

        this.renderBubble(graphics, bgX + Math.round(13 * scale), panelTop + Math.round(5 * scale),
                Math.max(3, Math.round(3 * scale)));
        this.renderBubble(graphics, bgX + Math.round(9 * scale), panelTop + Math.round(13 * scale),
                Math.max(3, Math.round(4 * scale)));
        this.renderBubble(graphics, bgX + Math.round(15 * scale), panelTop + Math.round(22 * scale),
                Math.max(3, Math.round(3 * scale)));
        this.renderBubble(graphics, bgX + Math.round(10 * scale), panelTop + Math.round(32 * scale),
                Math.max(3, Math.round(5 * scale)));
        this.renderBubble(graphics, bgX + Math.round(16 * scale), panelTop + Math.round(45 * scale),
                Math.max(3, Math.round(3 * scale)));
    }

    private void renderPointedArrow(GuiGraphics graphics, int arrowLeft, int arrowY, int arrowBase, int arrowTip) {
        graphics.fill(arrowLeft, arrowY - 1, arrowBase + 1, arrowY + 2, INK_SOFT);
        for (int row = -5; row <= 5; row++) {
            int rowEnd = arrowTip - Math.abs(row) * 2;
            graphics.fill(arrowBase, arrowY + row, rowEnd, arrowY + row + 1, INK_SOFT);
        }
    }

    private void renderBubble(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x + 1, y, x + size - 1, y + 1, BUBBLE);
        graphics.fill(x + 1, y + size - 1, x + size - 1, y + size, BUBBLE);
        graphics.fill(x, y + 1, x + 1, y + size - 1, BUBBLE);
        graphics.fill(x + size - 1, y + 1, x + size, y + size - 1, BUBBLE);
        if (size > 3) {
            graphics.fill(x + size - 2, y + 1, x + size - 1, y + 2, BUBBLE_LIGHT);
        }
    }
}
