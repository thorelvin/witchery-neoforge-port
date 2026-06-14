package com.emoniph.witchery.client;

import com.emoniph.witchery.book.page.ItemReferencePage;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import net.minecraft.client.gui.GuiGraphics;

/** Client renderer for compact item-icon reference pages. */
public class ItemReferencePageRenderer extends BookPageRenderer<ItemReferencePage> {

    private static final int PAGE_WIDTH = BookEntryScreen.PAGE_WIDTH;
    private static final int PAGE_HEIGHT = BookEntryScreen.PAGE_HEIGHT;
    private static final int ICON_X = 4;
    private static final int TEXT_X = 24;
    private static final int ROW_HEIGHT = 19;
    private static final int INK_SOFT = 0x6653341A;

    public ItemReferencePageRenderer(ItemReferencePage page) {
        super(page);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int y = 2;
        if (this.page.hasTitle()) {
            this.renderTitle(graphics, this.page.getTitle(), true, PAGE_WIDTH / 2, 0);
            y = 23;
        }

        if (!this.page.getText().isEmpty()) {
            int textHeight = 27;
            this.renderBookTextHolder(graphics, this.page.getText(), 0, y, PAGE_WIDTH, textHeight);
            y += textHeight + 3;
        }

        for (ItemReferencePage.Row row : this.page.getRows()) {
            if (y + ROW_HEIGHT > PAGE_HEIGHT - 2) {
                return;
            }
            graphics.fill(TEXT_X, y + ROW_HEIGHT - 1, PAGE_WIDTH - 2, y + ROW_HEIGHT, INK_SOFT);
            this.parentScreen.renderItemStack(graphics, ICON_X, y + 1, mouseX, mouseY, row.stack());
            this.renderBookTextHolder(graphics, row.text(), TEXT_X, y, PAGE_WIDTH - TEXT_X, ROW_HEIGHT - 1);
            y += ROW_HEIGHT;
        }
    }
}
