package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.item.ImmortalsObservationsBookItem;
import com.emoniph.witchery.network.SyncObservationsBookPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-shaped reader for Observations of an Immortal's compact markup dialect. */
public final class ObservationsScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            WitcheryMod.MODID, "textures/gui/book_single.png");
    private static final Pattern MARKUP = Pattern.compile("\\[([^]]+)]");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int BOOK_TOP = 2;
    private static final int TEXT_LEFT = 36;
    private static final int TEXT_TOP = 32;
    private static final int TEXT_WIDTH = 116;
    private static final int TEXT_COLOR = 0xFF000000;

    private final int visiblePages;
    private final boolean offhand;
    private int currentPage;
    private NavigateButton topButton;
    private NavigateButton previousButton;
    private NavigateButton nextButton;
    private boolean synced;

    public ObservationsScreen(int researchLevel, int pageIndex, boolean offhand) {
        super(GameNarrator.NO_TITLE);
        this.visiblePages = ImmortalsObservationsBookItem.getVisiblePageCount(researchLevel);
        this.currentPage = Math.clamp(pageIndex, 0, this.visiblePages - 1);
        this.offhand = offhand;
    }

    @Override
    protected void init() {
        int left = (this.width - BOOK_WIDTH) / 2;
        this.topButton = this.addRenderableWidget(new NavigateButton(
                left + 81, 16, 2, Component.translatable("gui.witchery.observations.top"),
                button -> setPage(0)));
        this.previousButton = this.addRenderableWidget(new NavigateButton(
                left + 36, 16, 1, Component.translatable("gui.witchery.observations.previous"),
                button -> setPage(this.currentPage - 1)));
        this.nextButton = this.addRenderableWidget(new NavigateButton(
                left + 125, 16, 0, Component.translatable("gui.witchery.observations.next"),
                button -> setPage(this.currentPage + 1)));
        updateButtonVisibility();
    }

    private void setPage(int pageIndex) {
        int clamped = Math.clamp(pageIndex, 0, this.visiblePages - 1);
        if (clamped != this.currentPage) {
            this.currentPage = clamped;
            updateButtonVisibility();
        }
    }

    private void updateButtonVisibility() {
        boolean hasHistory = this.currentPage > 0;
        this.topButton.visible = hasHistory;
        this.previousButton.visible = hasHistory;
        this.nextButton.visible = this.currentPage + 1 < this.visiblePages;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - BOOK_WIDTH) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, BOOK_TOP,
                0.0F, 0.0F, BOOK_WIDTH, BOOK_HEIGHT, 256, 256);
        renderMarkup(graphics, left, Component.translatable(
                ImmortalsObservationsBookItem.getPageTranslationKey(this.currentPage)).getString());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The 1.7.10 screen left the world visible around the original book texture.
    }

    private void renderMarkup(GuiGraphics graphics, int bookLeft, String markup) {
        LayoutState state = new LayoutState(TEXT_TOP, this.font.lineHeight);
        Matcher matcher = MARKUP.matcher(markup);
        int cursor = 0;
        while (matcher.find()) {
            renderText(graphics, bookLeft, state, markup.substring(cursor, matcher.start()));
            renderTag(graphics, bookLeft, state, matcher.group(1));
            cursor = matcher.end();
        }
        renderText(graphics, bookLeft, state, markup.substring(cursor));
    }

    private void renderTag(GuiGraphics graphics, int bookLeft, LayoutState state, String tag) {
        if (tag.equals("br")) {
            state.newLine(this.font.lineHeight);
        } else if (tag.startsWith("h1 ")) {
            renderText(graphics, bookLeft, state, tag.substring(3),
                    ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC);
            state.lineHeight = (int)Math.ceil(state.lineHeight * 1.5F);
            state.newLine(this.font.lineHeight);
        } else if (tag.startsWith("darkred ")) {
            renderText(graphics, bookLeft, state, tag.substring(8), ChatFormatting.DARK_RED);
        } else if (tag.startsWith("img=")) {
            renderImage(graphics, bookLeft, state, tag.substring(4));
        }
        // The next= tag is represented by the authoritative visible-page count and navigation button.
    }

    private void renderText(GuiGraphics graphics, int bookLeft, LayoutState state, String text,
                            ChatFormatting... formatting) {
        if (text.isEmpty()) {
            return;
        }
        for (String word : text.split("(?<=\\s)")) {
            Component component = Component.literal(word).withStyle(formatting);
            int wordWidth = this.font.width(component);
            if (state.x + wordWidth > TEXT_WIDTH) {
                state.newLine(this.font.lineHeight);
            }
            if (state.x != 0 || !word.trim().isEmpty()) {
                graphics.drawString(this.font, component,
                        bookLeft + TEXT_LEFT + state.x, state.y, TEXT_COLOR, false);
                state.x += wordWidth;
            }
        }
    }

    private void renderImage(GuiGraphics graphics, int bookLeft, LayoutState state, String attributes) {
        String[] values = attributes.split("\\|");
        Identifier texture = values.length > 0 ? Identifier.tryParse(values[0]) : null;
        if (texture == null) {
            return;
        }
        String horizontal = values.length > 1 ? values[1] : "left";
        String vertical = values.length > 2 ? values[2] : "top";
        int imageWidth = parseInt(values, 3, 32);
        int imageHeight = parseInt(values, 4, imageWidth);
        if (horizontal.equals("right")) {
            state.x = TEXT_WIDTH - imageWidth;
        } else if (horizontal.equals("center")) {
            state.x = TEXT_WIDTH / 2 - imageWidth / 2;
        }
        if (state.x + imageWidth > TEXT_WIDTH) {
            state.newLine(this.font.lineHeight);
        }
        int imageY = state.y;
        if (state.lineHeight > imageHeight) {
            if (vertical.equals("bottom")) {
                imageY += state.lineHeight - imageHeight;
            } else if (vertical.equals("middle")) {
                imageY += state.lineHeight / 2 - imageHeight / 2;
            }
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                bookLeft + TEXT_LEFT + state.x, imageY, 0.0F, 0.0F,
                imageWidth, imageHeight, 256, 256, 256, 256);
        state.x += imageWidth;
        state.lineHeight = Math.max(state.lineHeight, imageHeight);
    }

    private static int parseInt(String[] values, int index, int fallback) {
        if (index >= values.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(values[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void removed() {
        if (!this.synced && this.minecraft != null && this.minecraft.getConnection() != null) {
            this.synced = true;
            ClientPacketDistributor.sendToServer(new SyncObservationsBookPayload(this.currentPage, this.offhand));
        }
        super.removed();
    }

    private static final class LayoutState {
        private int x;
        private int y;
        private int lineHeight;

        private LayoutState(int y, int lineHeight) {
            this.y = y;
            this.lineHeight = lineHeight;
        }

        private void newLine(int defaultLineHeight) {
            this.x = 0;
            this.y += this.lineHeight + 1;
            this.lineHeight = defaultLineHeight;
        }
    }

    private static final class NavigateButton extends Button {
        private final int type;

        private NavigateButton(int x, int y, int type, Component tooltip, OnPress onPress) {
            super(x, y, 23, 13, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.type = type;
            this.setTooltip(Tooltip.create(tooltip));
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float textureX = this.isHoveredOrFocused() ? 23.0F : 0.0F;
            float textureY = 192.0F + 13.0F * this.type;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.getX(), this.getY(),
                    textureX, textureY, this.width, this.height, 256, 256);
        }
    }
}
