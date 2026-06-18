package com.emoniph.witchery.client;

import com.klikli_dev.modonomicon.book.page.BookEntityPage;
import com.klikli_dev.modonomicon.client.ClientTicks;
import com.klikli_dev.modonomicon.client.gui.book.BookContentRenderer;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookPageRenderer;
import com.klikli_dev.modonomicon.util.EntityUtil;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Witchery replacement for Modonomicon's built-in {@code modonomicon:entity} page renderer
 * ({@code BookEntityPageRenderer}), registered as an override in {@code WitcheryClient}.
 *
 * <p><b>Why:</b> the stock renderer builds the entity's screen scissor box from {@code this.left}/
 * {@code this.top} but omits the screen offset {@code parentScreen.getBookLeft()/getBookTop()} that
 * every other page coordinate (and {@link BookPageRenderer#addButton}) includes. The portrait frame is
 * drawn at page-relative {@code (9, 7)} under a pose translated by the book origin, so the stock box
 * ends up shifted off the frame — vertically the box bottom lands at the frame's middle, which clipped
 * the lower half of taller mobs and made all of them sit high. No {@code scale}/{@code offset} on the
 * page model can fix this because they move the entity <em>within</em> the mis-placed box.
 *
 * <p>This renderer rebuilds the box from the frame's true screen position
 * ({@code getBookLeft()+left+9}, {@code getBookTop()+top+7}, 106x106), so the entity centres inside the
 * visible frame with no clipping. Everything else mirrors the stock renderer (auto-fit scale, vertical
 * centring, page {@code rotate}/{@code defaultRotation}, title + caption).
 */
public class WitcheryEntityPageRenderer extends BookPageRenderer<BookEntityPage> {

    /** Visible portrait frame: page-relative origin and size, matching the content-texture blit below. */
    private static final int FRAME_X = 9;
    private static final int FRAME_Y = 7;
    private static final int FRAME_SIZE = 106;
    private static final int TEXT_Y = 115;

    private Entity entity;
    private boolean errored;
    private float renderScale;
    private float renderOffset;

    public WitcheryEntityPageRenderer(BookEntityPage page) {
        super(page);
    }

    @Override
    public void onBeginDisplayPage(BookEntryScreen parentScreen, int left, int top) {
        super.onBeginDisplayPage(parentScreen, left, top);
        this.loadEntity(Minecraft.getInstance().level);
    }

    private void loadEntity(Level world) {
        if (this.errored || world == null || (this.entity != null && this.entity.isAlive())) {
            return;
        }
        try {
            Function<Level, Entity> loader = EntityUtil.getEntityLoader(this.page.getEntityId());
            this.entity = loader.apply(world);
            float width = this.entity.getBbWidth();
            float height = this.entity.getBbHeight();
            float entitySize = Math.max(1.0F, Math.max(width, height));
            this.renderScale = 100.0F / entitySize * 0.75F * this.getPage().getScale();
            this.renderOffset = Math.max(height, entitySize) * 0.5F + this.getPage().getOffset();
        } catch (Exception e) {
            this.errored = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float ticks) {
        if (!this.page.getEntityName().isEmpty()) {
            this.renderTitle(guiGraphics, this.page.getEntityName(), false, 62, 0);
        }

        this.renderBookTextHolder(guiGraphics, this.getPage().getText(), 0, TEXT_Y, 124, 155 - TEXT_Y);

        BookContentRenderer.drawFromContentTexture(RenderPipelines.GUI_TEXTURED, guiGraphics,
                this.getPage().getBook(), FRAME_X, FRAME_Y, 405, 149, FRAME_SIZE, FRAME_SIZE);

        if (this.errored) {
            guiGraphics.drawString(this.font,
                    Component.translatable("modonomicon.gui.page.entity.loading_error"), 58, 60, 0xFF0000, true);
        }

        if (this.entity != null) {
            float rotation = this.page.doesRotate() ? ClientTicks.total : this.page.getDefaultRotation();
            this.renderEntity(guiGraphics, rotation);
        }
    }

    private void renderEntity(GuiGraphics guiGraphics, float rotation) {
        if (!(this.entity instanceof LivingEntity livingEntity)) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(livingEntity);
        EntityRenderState renderState = renderer.createRenderState(livingEntity, 10.0F);
        renderState.lightCoords = LightTexture.FULL_BRIGHT;
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;

        // Box aligned to the actual on-screen frame. Unlike the stock renderer this includes the book
        // origin (getBookLeft/getBookTop), so the scissor box matches the frame drawn above instead of
        // sitting shifted off it (the cause of the lower-body clipping / high placement).
        int boxX = this.parentScreen.getBookLeft() + this.left + FRAME_X;
        int boxY = this.parentScreen.getBookTop() + this.top + FRAME_Y;
        int boxX2 = boxX + FRAME_SIZE;
        int boxY2 = boxY + FRAME_SIZE;

        float entityHeight = this.entity.getBbHeight();
        Vector3f translation = new Vector3f(0.0F, entityHeight / 2.0F + this.renderOffset, 0.0F);
        Quaternionf rotationQuat = new Quaternionf().rotateZ((float) Math.PI);
        rotationQuat.mul(new Quaternionf().rotateY(rotation * (float) Math.PI / 180.0F));
        guiGraphics.submitEntityRenderState(renderState, this.renderScale, translation, rotationQuat, null,
                boxX, boxY, boxX2, boxY2);
    }
}
