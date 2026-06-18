package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.book.PlacedModonomiconBookResolver;
import com.emoniph.witchery.book.page.ItemReferencePage;
import com.emoniph.witchery.book.page.StationRecipePage;
import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.RenderedBookTextHolder;
import com.klikli_dev.modonomicon.book.entries.BookContentEntry;
import com.klikli_dev.modonomicon.book.page.BookPage;
import com.klikli_dev.modonomicon.book.page.BookCraftingRecipePage;
import com.klikli_dev.modonomicon.book.page.BookEntityPage;
import com.klikli_dev.modonomicon.book.page.BookRecipePage;
import com.klikli_dev.modonomicon.book.page.BookSpotlightPage;
import com.klikli_dev.modonomicon.book.page.BookTextPage;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.gui.book.markdown.MarkdownComponentRenderUtils;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import com.klikli_dev.modonomicon.util.EntityUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight Hexerei-style page layer for the placed Modonomicon block.
 *
 * <p>Modonomicon's normal page renderers target {@code GuiGraphics}; block entities submit world
 * geometry instead. This class resolves the same page spread as the GUI and draws a compact,
 * visual-only version onto the model's page quads while the placed block is being read in-world.
 */
public final class PlacedModonomiconPages {
    private static final int PAGE_WIDTH = BookEntryScreen.PAGE_WIDTH;
    private static final int PAGE_HEIGHT = BookEntryScreen.PAGE_HEIGHT;
    private static final int PAGE_INK = 0xFF1F160F;
    private static final int PAGE_INK_SOFT = 0xCC53341A;
    private static final int BUBBLE = 0xCC6F5438;
    private static final int BUBBLE_LIGHT = 0xAAA9875A;
    private static final int PAGE_TEXT_MARGIN_X = 8;
    private static final int PAGE_LIGHT = LightTexture.FULL_BRIGHT;
    private static final int PAGE_FRAME_ORDER = 1;
    private static final int PAGE_ICON_ORDER = 4;
    private static final int PAGE_NATIVE_TEXT_ORDER = 5;
    private static final RenderType PAGE_INK_RENDER_TYPE = RenderTypes.textBackground();
    private static final float PAGE_MODEL_WIDTH = 5.0F;
    private static final float PAGE_PIXEL_SCALE = PAGE_MODEL_WIDTH / 16.0F / PAGE_WIDTH;
    private static final float PAGE_CONTENT_LIFT_PIXELS = 0.5F;
    private static final float PAGE_TEXT_Z_PIXELS = 1.0F;
    private static final float PAGE_ICON_Z_PIXELS = 1.2F;
    private static final float BODY_TEXT_SCALE = 0.56F;
    private static final float STATION_TEXT_SCALE = 0.52F;
    private static final float INDEX_TEXT_SCALE = STATION_TEXT_SCALE;
    private static final float ITEM_REFERENCE_TEXT_SCALE = 0.44F;
    private static final float TITLE_TEXT_SCALE = 0.72F;
    private static final float FOOTER_TEXT_SCALE = 0.48F;
    private static final float ITEM_COUNT_TEXT_SCALE = 0.48F;
    private static final float NATIVE_TEXT_SCALE_FACTOR = 1.0F;
    private static final float FLAT_ITEM_DEPTH_SCALE = 0.01F;
    private static final Map<Identifier, Boolean> ITEM_DEPTH_FLATTEN_CACHE = new ConcurrentHashMap<>();
    /** Lazily-built display entities for entity ("Creatures") pages, keyed by entity id. */
    private static final Map<String, LivingEntity> FRAMED_ENTITY_CACHE = new ConcurrentHashMap<>();
    /** Fraction of the portrait frame the entity's largest bounding-box dimension fills (QA-tunable). */
    private static final float ENTITY_FIT_FACTOR = 0.8F;
    /** Y rotation applied so the framed entity faces the reader (QA-tunable; 0/180 flip front/back). */
    private static final float ENTITY_FACING_Y_DEGREES = 0.0F;
    private static final int BODY_LINE_PADDING = 1;
    private static final int PAGE_TEXT_BOTTOM = PAGE_HEIGHT - 22;
    private static final int STATION_PANEL_TOP = 18;
    private static final int STATION_PANEL_TOP_NO_TITLE = 7;
    private static final int STATION_TEXT_GAP = 3;
    private static final int STATION_ITEM_SIZE = 16;
    private static final int ITEM_REFERENCE_ROW_HEIGHT = 18;
    private static final int ITEM_REFERENCE_ICON_SIZE = 13;
    private static final float LEFT_PAGE_CONTENT_INSET_PIXELS = 6.0F;
    private static final float PAGE_TOP_X = 0.26F / 16.0F;
    private static final float PAGE_TOP_Y = -3.72F / 16.0F;
    private static final Identifier GUI_ATLAS =
            Identifier.withDefaultNamespace("textures/atlas/gui.png");
    private static final Identifier FURNACE_FLAME =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "book/furnace_flame");
    // The page cubes are one pixel thick. The readable paper faces are the inner faces near z=0.
    // The left page is rotated into mirrored page-space so native text and custom geometry face out.
    private static final float LEFT_PAGE_SURFACE_Z = -0.025F / 16.0F;
    private static final float RIGHT_PAGE_SURFACE_Z = 0.025F / 16.0F;

    private PlacedModonomiconPages() {}

    public static void submit(ItemStack bookStack, int openPagesIndex, PoseStack ps, SubmitNodeCollector collector,
                              int lightCoords, float openAmount, CameraRenderState camera) {
        if (bookStack.isEmpty()) {
            return;
        }

        Spread spread = resolveSpread(bookStack, openPagesIndex);
        if (spread == null) {
            return;
        }

        submitPage(spread.leftPage(), PageSide.LEFT, ps, collector, lightCoords, openAmount,
                spread.openPagesIndex() > 0, spread.openPagesIndex() + 1, spread.pageCount(), camera);
        submitPage(spread.rightPage(), PageSide.RIGHT, ps, collector, lightCoords, openAmount,
                spread.openPagesIndex() + 2 < spread.pageCount(), spread.openPagesIndex() + 2, spread.pageCount(), camera);
    }

    @Nullable
    private static Spread resolveSpread(ItemStack bookStack, int openPagesIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        ensureBooksBuilt(mc);
        List<PlacedModonomiconBookResolver.PlacedPage> pages =
                PlacedModonomiconBookResolver.resolvePages(bookStack, mc.level, mc.player);
        if (pages.isEmpty()) {
            return null;
        }

        openPagesIndex = clampToDoublePage(openPagesIndex, pages.size());
        int pageCount = pages.size();
        PlacedModonomiconBookResolver.PlacedPage leftPage = pages.get(openPagesIndex);
        PlacedModonomiconBookResolver.PlacedPage rightPage =
                openPagesIndex + 1 < pages.size() ? pages.get(openPagesIndex + 1) : null;
        return new Spread(leftPage, rightPage, openPagesIndex, pageCount);
    }

    private static void ensureBooksBuilt(Minecraft mc) {
        BookDataManager manager = BookDataManager.get();
        if (!manager.areBooksBuilt() && mc.level != null) {
            manager.registries(mc.level.registryAccess());
            manager.tryBuildBooks(mc.level);
            manager.prerenderMarkdown(mc.level.registryAccess());
        }
    }

    private static int clampToDoublePage(int openPagesIndex, int pageCount) {
        int evenIndex = Math.max(0, openPagesIndex) & -2;
        if (evenIndex >= pageCount) {
            evenIndex = (pageCount - 1) & -2;
        }
        return Math.max(0, evenIndex);
    }

    private static void submitPage(@Nullable PlacedModonomiconBookResolver.PlacedPage placedPage, PageSide side, PoseStack ps,
                                   SubmitNodeCollector collector, int lightCoords, float openAmount,
                                   boolean showNavigationArrow, int pageNumber, int pageCount, CameraRenderState camera) {
        if (placedPage == null) {
            return;
        }

        ps.pushPose();
        try {
            applyPagePose(ps, side, openAmount);
            applyPagePixelPose(ps);

            BookPage page = placedPage.page();
            if (placedPage.indexPage() != null) {
                submitIndexPage(placedPage.indexPage(), ps, collector, lightCoords);
            } else if (page instanceof BookTextPage textPage) {
                submitTextPage(textPage, ps, collector, lightCoords);
            } else if (page instanceof BookSpotlightPage spotlightPage) {
                submitSpotlightPage(spotlightPage, ps, collector, lightCoords);
            } else if (page instanceof BookEntityPage entityPage) {
                submitEntityPage(entityPage, ps, collector, lightCoords, camera);
            } else if (page instanceof BookCraftingRecipePage recipePage) {
                submitCraftingRecipePage(recipePage, ps, collector, lightCoords);
            } else if (page instanceof StationRecipePage stationPage) {
                submitStationRecipePage(stationPage, ps, collector, lightCoords);
            } else if (page instanceof ItemReferencePage itemReferencePage) {
                submitItemReferencePage(itemReferencePage, ps, collector, lightCoords);
            } else if (page != null && placedPage.entry() != null) {
                submitFallbackPage(page, placedPage.entry(), ps, collector, lightCoords);
            }
            if (showNavigationArrow) {
                submitNavigationArrow(side == PageSide.RIGHT, ps, collector, lightCoords);
            }
            if (!placedPage.isBlank()) {
                submitIndexButton(side, ps, collector, lightCoords);
                submitPageNumber(side, pageNumber, pageCount, ps, collector, lightCoords);
            }
        } finally {
            ps.popPose();
        }
    }

    private static void applyPagePixelPose(PoseStack ps) {
        // Model-page local +Y is visually upward, while Modonomicon page coordinates use GUI +Y downward.
        ps.translate(0.0F, PAGE_HEIGHT * PAGE_PIXEL_SCALE, 0.0F);
        ps.scale(PAGE_PIXEL_SCALE, -PAGE_PIXEL_SCALE, PAGE_PIXEL_SCALE);
    }

    private static void applyPagePose(PoseStack ps, PageSide side, float openAmount) {
        float spread = 1.2F * Mth.clamp(openAmount, 0.0F, 1.0F);
        float hingeOffset = Mth.sin(spread);
        float surfaceZ = side == PageSide.RIGHT ? RIGHT_PAGE_SURFACE_Z : LEFT_PAGE_SURFACE_Z;
        ps.translate(hingeOffset / 16.0F, 0.0F, 0.0F);
        ps.mulPose(Axis.YP.rotation(side == PageSide.RIGHT ? spread : -spread));
        ps.translate(PAGE_TOP_X, PAGE_TOP_Y, surfaceZ);
        if (side == PageSide.LEFT) {
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.translate(-PAGE_MODEL_WIDTH / 16.0F + LEFT_PAGE_CONTENT_INSET_PIXELS * PAGE_PIXEL_SCALE, 0.0F, 0.0F);
        }
        ps.translate(0.0F, 0.0F, PAGE_CONTENT_LIFT_PIXELS * PAGE_PIXEL_SCALE);
    }

    private static void submitIndexPage(PlacedModonomiconBookResolver.PlacedIndexPage page, PoseStack ps,
                                        SubmitNodeCollector collector, int lightCoords) {
        submitCentered(Component.literal("Entries"), 0, PAGE_WIDTH, TITLE_TEXT_SCALE, ps, collector, lightCoords, PAGE_INK);
        submitRect(ps, collector, lightCoords, 18, 12, PAGE_WIDTH - 18, 13, PAGE_INK_SOFT);

        int y = PlacedModonomiconBookResolver.INDEX_START_Y_PIXELS;
        int entryIndent = 6;
        int lineHeight = PlacedModonomiconBookResolver.INDEX_LINE_HEIGHT_PIXELS;
        for (PlacedModonomiconBookResolver.IndexLine line : page.lines()) {
            String text = localized(line.text());
            if (line.category()) {
                y += y == PlacedModonomiconBookResolver.INDEX_START_Y_PIXELS
                        ? 0
                        : PlacedModonomiconBookResolver.INDEX_CATEGORY_GAP_PIXELS;
                submitTextLine(ps, collector, PAGE_TEXT_MARGIN_X, y, text, INDEX_TEXT_SCALE, PAGE_INK_SOFT);
            } else {
                submitTextLine(ps, collector, PAGE_TEXT_MARGIN_X + entryIndent, y, text, INDEX_TEXT_SCALE, PAGE_INK);
            }
            y += lineHeight;
            if (y > PAGE_HEIGHT - 18) {
                return;
            }
        }
    }

    private static void submitTextPage(BookTextPage page, PoseStack ps, SubmitNodeCollector collector, int lightCoords) {
        int textY;
        if (page.hasTitle()) {
            submitTitle(page.getTitle(), page.getBook().getDefaultTitleColor(), ps, collector, lightCoords);
            textY = page.showTitleSeparator() ? 17 : 7;
            if (page.showTitleSeparator()) {
                submitRect(ps, collector, lightCoords, 18, 12, PAGE_WIDTH - 18, 13, PAGE_INK_SOFT);
            }
        } else {
            textY = -4;
        }

        int x = page.getBook().getBookTextOffsetX();
        int y = textY + page.getBook().getBookTextOffsetY();
        int width = PAGE_WIDTH + page.getBook().getBookTextOffsetWidth() - x;
        int height = PAGE_HEIGHT + page.getBook().getBookTextOffsetHeight() - y;
        submitTextHolder(page.getText(), x, y, width, height, ps, collector, lightCoords);
    }

    private static void submitSpotlightPage(BookSpotlightPage page, PoseStack ps,
                                            SubmitNodeCollector collector, int lightCoords) {
        if (page.hasTitle()) {
            submitTitle(page.getTitle(), page.getBook().getDefaultTitleColor(), ps, collector, lightCoords);
        }

        int frameX = (PAGE_WIDTH - 42) / 2;
        int frameY = page.hasTitle() ? 16 : 8;
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 42, frameY + 24, 0x2253341A);
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 42, frameY + 1, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX, frameY + 23, frameX + 42, frameY + 24, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 1, frameY + 24, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX + 41, frameY, frameX + 42, frameY + 24, PAGE_INK_SOFT);

        submitItem(firstStack(page.getItem()), (PAGE_WIDTH - 16) / 2, frameY + 4,
                page.getParentEntry().getId().hashCode(), ps, collector, lightCoords, 16.0F);

        int textY = frameY + 32;
        int x = page.getBook().getBookTextOffsetX();
        int y = textY + page.getBook().getBookTextOffsetY();
        int width = PAGE_WIDTH + page.getBook().getBookTextOffsetWidth() - x;
        int height = PAGE_HEIGHT + page.getBook().getBookTextOffsetHeight() - y;
        submitTextHolder(page.getText(), x, y, width, height, ps, collector, lightCoords);
    }

    private static void submitEntityPage(BookEntityPage page, PoseStack ps,
                                         SubmitNodeCollector collector, int lightCoords, CameraRenderState camera) {
        if (!page.getEntityName().isEmpty()) {
            submitTitle(page.getEntityName(), page.getBook().getDefaultTitleColor(), ps, collector, lightCoords);
        }

        int frameX = (PAGE_WIDTH - 52) / 2;
        int frameY = 16;
        int frameSize = 42;
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 52, frameY + frameSize, 0x1853341A);
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 52, frameY + 1, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX, frameY + frameSize - 1, frameX + 52, frameY + frameSize, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX, frameY, frameX + 1, frameY + frameSize, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, frameX + 51, frameY, frameX + 52, frameY + frameSize, PAGE_INK_SOFT);

        if (!submitFramedEntity(page, frameX, frameY, frameSize, ps, collector, lightCoords, camera)) {
            // Fallback when the entity cannot be loaded/rendered: keep the old spawn-egg icon.
            ItemStack entityIcon = spawnEggFor(page.getEntityId());
            if (entityIcon.isEmpty()) {
                submitCentered(Component.literal(page.getEntityId()), frameY + 16, PAGE_WIDTH, ps, collector, lightCoords, PAGE_INK);
            } else {
                submitItem(entityIcon, (PAGE_WIDTH - 16) / 2, frameY + 11,
                        page.getEntityId().hashCode(), ps, collector, lightCoords, 18.0F);
            }
        }

        int x = page.getBook().getBookTextOffsetX();
        int y = 68 + page.getBook().getBookTextOffsetY();
        int width = PAGE_WIDTH + page.getBook().getBookTextOffsetWidth() - x;
        int height = PAGE_HEIGHT + page.getBook().getBookTextOffsetHeight() - y;
        submitTextHolder(page.getText(), x, y, width, height, ps, collector, lightCoords);
    }

    /**
     * Renders the page's actual entity model inside the portrait frame, in place of the spawn egg.
     *
     * <p>The placed book is a {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}, so
     * the screen-space {@code GuiGraphics.submitEntityRenderState} the item-opened book uses is not
     * available; instead we drive {@link EntityRenderer#submit} into the page's own
     * {@link SubmitNodeCollector} (the same collector the flat block/item models already use). The
     * entity is fit to the frame by its bounding box, flipped from page (Y-down) into model (Y-up)
     * orientation with a proper Z rotation (a negative scale would invert face winding and cull the
     * model), centred vertically, and turned to face the reader.
     *
     * <p><b>First-pass / QA-tunable:</b> the facing, fit factor, and vertical centring constants are
     * best-effort without in-client verification — adjust {@link #ENTITY_FIT_FACTOR},
     * {@link #ENTITY_FACING_Y_DEGREES}, and the offset handling from screenshots. The cached entity is
     * built once per id from the client level. Returns {@code false} (→ egg fallback) on any failure.
     */
    private static boolean submitFramedEntity(BookEntityPage page, int frameX, int frameY, int frameSize,
                                              PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                              CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return false;
        }
        try {
            LivingEntity entity = resolveFramedEntity(page.getEntityId(), mc);
            if (entity == null) {
                return false;
            }

            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
            @SuppressWarnings({"unchecked", "rawtypes"})
            EntityRenderState renderState = ((EntityRenderer) renderer).createRenderState(entity, 1.0F);
            renderState.lightCoords = LightTexture.FULL_BRIGHT;
            renderState.shadowPieces.clear();
            renderState.outlineColor = 0;

            float bbWidth = entity.getBbWidth();
            float bbHeight = entity.getBbHeight();
            float sizeUnits = Math.max(1.0F, Math.max(bbWidth, bbHeight));
            float scalePx = (frameSize * ENTITY_FIT_FACTOR) / sizeUnits * Math.max(0.05F, page.getScale());
            float centerX = frameX + 26.0F;
            float centerY = frameY + frameSize / 2.0F;

            ps.pushPose();
            try {
                ps.translate(centerX, centerY, PAGE_ICON_Z_PIXELS);
                ps.scale(scalePx, scalePx, scalePx);
                // Flip from page space (Y down) into model space (Y up) with a proper rotation so face
                // winding is preserved (a negative Y scale would cull the model), then turn to the reader.
                ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
                ps.mulPose(Axis.YP.rotationDegrees(ENTITY_FACING_Y_DEGREES));
                // Centre the standing model (feet at y=0, head at y=bbHeight) about the frame midline.
                // The page's `offset` is intentionally not applied here — it tunes the item-opened book's
                // separate Modonomicon renderer, and the placed frame centres purely by bounding box.
                ps.translate(0.0F, -bbHeight / 2.0F, 0.0F);
                @SuppressWarnings({"unchecked", "rawtypes"})
                EntityRenderer rawRenderer = renderer;
                rawRenderer.submit(renderState, ps, new PageItemCollector(collector, PAGE_ICON_ORDER), camera);
            } finally {
                ps.popPose();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static LivingEntity resolveFramedEntity(String entityId, Minecraft mc) {
        LivingEntity cached = FRAMED_ENTITY_CACHE.get(entityId);
        if (cached != null && cached.isAlive()) {
            return cached;
        }
        Entity loaded = EntityUtil.getEntityLoader(entityId).apply(mc.level);
        if (!(loaded instanceof LivingEntity living)) {
            return null;
        }
        FRAMED_ENTITY_CACHE.put(entityId, living);
        return living;
    }

    private static void submitCraftingRecipePage(BookCraftingRecipePage page, PoseStack ps,
                                                 SubmitNodeCollector collector, int lightCoords) {
        int recipeCount = page.getRecipeDisplayEntry2() == null ? 1 : 2;
        submitCraftingRecipe(page.getTitle1(), page.getRecipeDisplayEntry1(), 0,
                recipeCount == 1, page, ps, collector, lightCoords);
        if (recipeCount == 2) {
            submitCraftingRecipe(page.getTitle2(), page.getRecipeDisplayEntry2(), 78,
                    false, page, ps, collector, lightCoords);
        } else if (!page.getText().isEmpty()) {
            submitSafePageTextHolder(page.getText(), 104, PAGE_HEIGHT - 106, ps, collector, lightCoords);
        }
    }

    private static void submitCraftingRecipe(BookTextHolder title, @Nullable RecipeDisplayEntry recipeEntry,
                                             int yOffset, boolean roomy, BookRecipePage<?> page, PoseStack ps,
                                             SubmitNodeCollector collector, int lightCoords) {
        if (recipeEntry == null) {
            return;
        }

        if (!title.isEmpty()) {
            submitTitleAt(title, page.getBook().getDefaultTitleColor(), yOffset, ps, collector, lightCoords);
        }

        ContextMap context = recipeContext();
        int top = yOffset + (title.isEmpty() ? 6 : 16);
        int gridX = 9;
        int gridY = top + (roomy ? 10 : 6);
        int slotSize = 12;
        int slotStep = 13;

        RecipeDisplay display = recipeEntry.display();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            submitShapedIngredients(shaped, context, gridX, gridY, slotSize, slotStep, ps, collector, lightCoords);
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            submitShapelessIngredients(shapeless, context, gridX, gridY, slotSize, slotStep, ps, collector, lightCoords);
        }

        int arrowY = gridY + slotStep + 6;
        submitPointedArrow(ps, collector, lightCoords, 53, arrowY, 65, 76);

        ItemStack result = firstStack(recipeEntry.resultItems(context));
        if (result.isEmpty()) {
            result = firstStack(display.result(), context);
        }
        submitSlotFrame(ps, collector, lightCoords, 91, gridY + slotStep, slotSize);
        submitItem(result, 89, gridY + slotStep - 2, recipeEntry.id().hashCode(), ps, collector, lightCoords, 14.0F);
    }

    private static void submitShapedIngredients(ShapedCraftingRecipeDisplay shaped, ContextMap context, int gridX,
                                                int gridY, int slotSize, int slotStep, PoseStack ps,
                                                SubmitNodeCollector collector, int lightCoords) {
        int width = Math.min(3, shaped.width());
        int height = Math.min(3, shaped.height());
        int xOffset = (3 - width) * slotStep / 2;
        int yOffset = (3 - height) * slotStep / 2;
        List<SlotDisplay> ingredients = shaped.ingredients();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * shaped.width() + col;
                if (index < ingredients.size()) {
                    submitRecipeSlot(ingredients.get(index), context,
                            gridX + xOffset + col * slotStep,
                            gridY + yOffset + row * slotStep,
                            index, slotSize, ps, collector, lightCoords);
                }
            }
        }
    }

    private static void submitShapelessIngredients(ShapelessCraftingRecipeDisplay shapeless, ContextMap context,
                                                   int gridX, int gridY, int slotSize, int slotStep, PoseStack ps,
                                                   SubmitNodeCollector collector, int lightCoords) {
        List<SlotDisplay> ingredients = shapeless.ingredients();
        int count = Math.min(9, ingredients.size());
        for (int index = 0; index < count; index++) {
            int row = index / 3;
            int col = index % 3;
            submitRecipeSlot(ingredients.get(index), context, gridX + col * slotStep, gridY + row * slotStep,
                    index, slotSize, ps, collector, lightCoords);
        }
    }

    private static void submitRecipeSlot(SlotDisplay display, ContextMap context, int x, int y, int seed,
                                         int slotSize, PoseStack ps, SubmitNodeCollector collector, int lightCoords) {
        submitSlotFrame(ps, collector, lightCoords, x, y, slotSize);
        submitItem(firstStack(display, context), x - 2, y - 2, seed, ps, collector, lightCoords, 14.0F);
    }

    private static void submitSlotFrame(PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                        int x, int y, int slotSize) {
        submitRect(ps, collector, lightCoords, x, y, x + slotSize, y + slotSize, 0x1653341A);
        submitRect(ps, collector, lightCoords, x, y, x + slotSize, y + 1, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, x, y + slotSize - 1, x + slotSize, y + slotSize, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, x, y, x + 1, y + slotSize, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, x + slotSize - 1, y, x + slotSize, y + slotSize, PAGE_INK_SOFT);
    }

    private static void submitStationRecipePage(StationRecipePage page, PoseStack ps,
                                                SubmitNodeCollector collector, int lightCoords) {
        if (page.hasTitle()) {
            submitTitle(page.getTitle(), page.getBook().getDefaultTitleColor(), ps, collector, lightCoords);
        }

        int bgWidth = page.getBgWidth();
        int bgHeight = page.getBgHeight();
        float scale = Math.min(1.0F, PAGE_WIDTH / (float) bgWidth);
        int drawnW = Math.round(bgWidth * scale);
        int drawnH = Math.round(bgHeight * scale);
        int bgX = (PAGE_WIDTH - drawnW) / 2;
        int panelTop = page.hasTitle() ? STATION_PANEL_TOP : STATION_PANEL_TOP_NO_TITLE;

        if (page.hasOvenCues()) {
            submitOvenCues(ps, collector, lightCoords, bgX, panelTop, scale);
        }
        if (page.hasDistilleryCues()) {
            submitDistilleryCues(ps, collector, lightCoords, bgX, panelTop, scale);
        }

        int seed = page.getParentEntry().getId().hashCode();
        for (StationRecipePage.Slot slot : page.getSlots()) {
            int itemX = bgX + Math.round(slot.x() * scale);
            int itemY = panelTop + Math.round(slot.y() * scale);
            submitItem(slot.stack(), itemX, itemY, seed++, ps, collector, lightCoords);
        }

        int textY = panelTop + stationVisibleContentBottom(page, scale, drawnH) + STATION_TEXT_GAP;
        int textHeight = PAGE_TEXT_BOTTOM - textY;
        submitSafePageTextHolder(page.getText(), textY, textHeight, ps, collector, lightCoords, STATION_TEXT_SCALE);
    }

    private static int stationVisibleContentBottom(StationRecipePage page, float scale, int drawnH) {
        if (page.shouldShowBackground()) {
            return drawnH;
        }

        int bottom = 0;
        for (StationRecipePage.Slot slot : page.getSlots()) {
            bottom = Math.max(bottom, Math.round((slot.y() + STATION_ITEM_SIZE) * scale));
        }
        if (page.hasOvenCues()) {
            bottom = Math.max(bottom, Math.round((31 + 14) * scale));
            bottom = Math.max(bottom, Math.round((21 + 6) * scale));
        }
        if (page.hasDistilleryCues()) {
            bottom = Math.max(bottom, Math.round((45 + 5) * scale));
            bottom = Math.max(bottom, Math.round((27 + 6) * scale));
        }
        return bottom == 0 ? drawnH : bottom;
    }

    private static void submitItemReferencePage(ItemReferencePage page, PoseStack ps,
                                                SubmitNodeCollector collector, int lightCoords) {
        if (page.hasTitle()) {
            submitTitle(page.getTitle(), page.getBook().getDefaultTitleColor(), ps, collector, lightCoords);
        }

        int y = page.hasTitle() ? 18 : 2;
        if (!page.getText().isEmpty()) {
            submitSafePageTextHolder(page.getText(), y, 24, ps, collector, lightCoords, STATION_TEXT_SCALE);
            y += 28;
        }

        int seed = page.getParentEntry().getId().hashCode();
        for (ItemReferencePage.Row row : page.getRows()) {
            if (y + ITEM_REFERENCE_ROW_HEIGHT > PAGE_TEXT_BOTTOM) {
                return;
            }
            submitItem(row.stack(), 4, y + 2, seed++, ps, collector, lightCoords, ITEM_REFERENCE_ICON_SIZE);
            submitTextHolder(row.text(), 22, y, PAGE_WIDTH - 25, ITEM_REFERENCE_ROW_HEIGHT - 1,
                    ps, collector, lightCoords, ITEM_REFERENCE_TEXT_SCALE);
            submitRect(ps, collector, lightCoords, 22, y + ITEM_REFERENCE_ROW_HEIGHT - 1,
                    PAGE_WIDTH - 3, y + ITEM_REFERENCE_ROW_HEIGHT, 0x3353341A);
            y += ITEM_REFERENCE_ROW_HEIGHT;
        }
    }

    private static void submitFallbackPage(BookPage page, BookContentEntry entry, PoseStack ps,
                                           SubmitNodeCollector collector, int lightCoords) {
        submitCentered(Component.literal(entry.getName()), 0, PAGE_WIDTH, ps, collector, lightCoords, PAGE_INK);
        submitText(Component.literal(page.getType().toString()), 0, 18, PAGE_WIDTH, PAGE_HEIGHT - 18,
                ps, collector, lightCoords);
    }

    private static void submitTitle(BookTextHolder title, int color, PoseStack ps,
                                    SubmitNodeCollector collector, int lightCoords) {
        submitTitleAt(title, color, 0, ps, collector, lightCoords);
    }

    private static void submitTitleAt(BookTextHolder title, int color, int y, PoseStack ps,
                                      SubmitNodeCollector collector, int lightCoords) {
        List<Component> components = componentsFor(title);
        if (components.isEmpty()) {
            return;
        }
        int safeWidth = PAGE_WIDTH - PAGE_TEXT_MARGIN_X * 2;
        for (String line : wrapText(components.get(0).getString(), safeWidth, TITLE_TEXT_SCALE)) {
            if (line.isBlank()) {
                continue;
            }
            int x = PAGE_TEXT_MARGIN_X + Math.round((safeWidth - textWidth(line, TITLE_TEXT_SCALE)) / 2.0F);
            submitTextLine(ps, collector, x, y, line, TITLE_TEXT_SCALE, PAGE_INK);
            return;
        }
    }

    private static void submitCentered(Component text, int y, int width, PoseStack ps,
                                       SubmitNodeCollector collector, int lightCoords, int color) {
        submitCentered(text, y, width, BODY_TEXT_SCALE, ps, collector, lightCoords, color);
    }

    private static void submitCentered(Component text, int y, int width, float scale, PoseStack ps,
                                       SubmitNodeCollector collector, int lightCoords, int color) {
        int safeWidth = Math.max(1, width - PAGE_TEXT_MARGIN_X * 2);
        for (String line : wrapText(text.getString(), safeWidth, scale)) {
            int x = PAGE_TEXT_MARGIN_X + Math.round((safeWidth - textWidth(line, scale)) / 2.0F);
            submitTextLine(ps, collector, x, y, line, scale, color);
            return;
        }
    }

    private static void submitTextHolder(BookTextHolder holder, int x, int y, int width, int height,
                                         PoseStack ps, SubmitNodeCollector collector, int lightCoords) {
        submitTextHolder(holder, x, y, width, height, ps, collector, lightCoords, BODY_TEXT_SCALE);
    }

    private static void submitTextHolder(BookTextHolder holder, int x, int y, int width, int height,
                                         PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                         float textScale) {
        if (holder.isEmpty()) {
            return;
        }

        int safeX = Math.max(PAGE_TEXT_MARGIN_X, x);
        int safeRight = PAGE_WIDTH - PAGE_TEXT_MARGIN_X;
        width = Math.min(width - (safeX - x), safeRight - safeX);
        x = safeX;
        if (width <= 0 || height <= 0) {
            return;
        }

        int lineY = y;
        int maxY = Math.min(y + height, PAGE_TEXT_BOTTOM);
        int lineHeight = lineHeight(textScale);
        if (lineY + lineHeight > maxY) {
            return;
        }

        if (holder instanceof RenderedBookTextHolder rendered) {
            Font font = Minecraft.getInstance().font;
            int firstLineWrapWidth = Math.max(1, Mth.floor(width / textScale));
            int followingLineWrapWidth = Math.max(1, Mth.floor((width - 10) / textScale));
            for (MutableComponent component : rendered.getRenderedText()) {
                for (FormattedCharSequence line : MarkdownComponentRenderUtils.wrapComponents(
                        component, firstLineWrapWidth, followingLineWrapWidth, font)) {
                    if (lineY + lineHeight > maxY) {
                        return;
                    }
                    submitTextLine(ps, collector, x, lineY, line, textScale, PAGE_INK);
                    lineY += lineHeight;
                }
            }
            return;
        }

        if (holder.hasComponent()) {
            Font font = Minecraft.getInstance().font;
            for (FormattedCharSequence line : font.split(holder.getComponent(),
                    Math.max(1, Mth.floor(width / textScale)))) {
                if (lineY + lineHeight > maxY) {
                    return;
                }
                submitTextLine(ps, collector, x, lineY, line, textScale, PAGE_INK);
                lineY += lineHeight;
            }
            return;
        }

        for (String line : wrapText(holder.getString(), width, textScale)) {
            if (lineY + lineHeight > maxY) {
                return;
            }
            submitTextLine(ps, collector, x, lineY, line, textScale, PAGE_INK);
            lineY += lineHeight;
        }
    }

    private static void submitSafePageTextHolder(BookTextHolder holder, int y, int height, PoseStack ps,
                                                 SubmitNodeCollector collector, int lightCoords) {
        submitTextHolder(holder, PAGE_TEXT_MARGIN_X, y, PAGE_WIDTH - PAGE_TEXT_MARGIN_X * 2, height,
                ps, collector, lightCoords);
    }

    private static void submitSafePageTextHolder(BookTextHolder holder, int y, int height, PoseStack ps,
                                                 SubmitNodeCollector collector, int lightCoords, float textScale) {
        submitTextHolder(holder, PAGE_TEXT_MARGIN_X, y, PAGE_WIDTH - PAGE_TEXT_MARGIN_X * 2, height,
                ps, collector, lightCoords, textScale);
    }

    private static void submitText(Component text, int x, int y, int width, int height, PoseStack ps,
                                   SubmitNodeCollector collector, int lightCoords) {
        int lineY = y;
        int maxY = Math.min(y + height, PAGE_TEXT_BOTTOM);
        int lineHeight = lineHeight(BODY_TEXT_SCALE);
        for (String line : wrapText(text.getString(), width, BODY_TEXT_SCALE)) {
            if (lineY + lineHeight > maxY) {
                return;
            }
            submitTextLine(ps, collector, x, lineY, line, BODY_TEXT_SCALE, PAGE_INK);
            lineY += lineHeight;
        }
    }

    private static void submitTextLine(PoseStack ps, SubmitNodeCollector collector, int x, int y,
                                       String line, float scale, int color) {
        submitNativeTextLine(ps, collector, x, y, Component.literal(line).getVisualOrderText(), scale, color);
    }

    private static void submitTextLine(PoseStack ps, SubmitNodeCollector collector, int x, int y,
                                       FormattedCharSequence line, float scale, int color) {
        submitNativeTextLine(ps, collector, x, y, line, scale, color);
    }

    private static void submitNativeTextLine(PoseStack ps, SubmitNodeCollector collector, int x, int y,
                                             FormattedCharSequence line, float scale, int color) {
        float nativeScale = Math.max(0.35F, scale * NATIVE_TEXT_SCALE_FACTOR);
        ps.pushPose();
        ps.translate(0.0F, 0.0F, PAGE_TEXT_Z_PIXELS + 1.0F);
        ps.scale(nativeScale, nativeScale, nativeScale);
        collector.order(PAGE_NATIVE_TEXT_ORDER).submitText(ps,
                x / nativeScale,
                y / nativeScale,
                line,
                false,
                Font.DisplayMode.NORMAL,
                PAGE_LIGHT,
                color,
                0,
                0);
        ps.popPose();
    }

    private static List<Component> componentsFor(BookTextHolder holder) {
        List<Component> components = new ArrayList<>();
        if (holder.isEmpty()) {
            return components;
        }
        if (holder instanceof RenderedBookTextHolder rendered) {
            components.addAll(rendered.getRenderedText());
        } else if (holder.hasComponent()) {
            components.add(holder.getComponent());
        } else {
            components.add(Component.literal(cleanPlainText(holder.getString())));
        }
        return components;
    }

    private static String cleanPlainText(String text) {
        return text.replaceAll("\\[#\\]\\([^)]*\\)", "")
                .replace("**", "")
                .replace("*", "")
                .replace("__", "")
                .replace("_", "");
    }

    private static List<String> wrapText(String text, int width, float scale) {
        List<String> lines = new ArrayList<>();
        String cleaned = cleanPlainText(text).replace('\r', '\n');
        for (String paragraph : cleaned.split("\\n", -1)) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String word : trimmed.split("\\s+")) {
                if (textWidth(word, scale) > width) {
                    if (line.length() > 0) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    StringBuilder chunk = new StringBuilder();
                    for (int index = 0; index < word.length(); index++) {
                        String candidate = chunk.toString() + word.charAt(index);
                        if (chunk.length() > 0 && textWidth(candidate, scale) > width) {
                            lines.add(chunk.toString());
                            chunk.setLength(0);
                        }
                        chunk.append(word.charAt(index));
                    }
                    if (chunk.length() > 0) {
                        lines.add(chunk.toString());
                    }
                    continue;
                }

                String candidate = line.length() == 0 ? word : line + " " + word;
                if (textWidth(candidate, scale) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }

                if (line.length() > 0) {
                    line.append(' ');
                }
                line.append(word);
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
        }
        return lines;
    }

    private static int lineHeight(float scale) {
        return Math.max(1, Mth.ceil(Minecraft.getInstance().font.lineHeight * scale) + BODY_LINE_PADDING);
    }

    private static float textWidth(String text, float scale) {
        if (text.isEmpty()) {
            return 0.0F;
        }
        return Minecraft.getInstance().font.width(cleanPlainText(text)) * scale;
    }

    private static String localized(String text) {
        if (text.indexOf('.') >= 0 && text.indexOf(' ') < 0) {
            return Component.translatable(text).getString();
        }
        return text;
    }

    private static void submitItem(ItemStack stack, int x, int y, int seed, PoseStack ps,
                                   SubmitNodeCollector collector, int lightCoords) {
        submitItem(stack, x, y, seed, ps, collector, lightCoords, 16.0F);
    }

    private static void submitItem(ItemStack stack, int x, int y, int seed, PoseStack ps,
                                   SubmitNodeCollector collector, int lightCoords, float size) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        ItemStackRenderState itemState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(itemState, stack, ItemDisplayContext.GUI,
                mc.level, mc.player, seed);
        if (itemState.isEmpty()) {
            return;
        }

        ps.pushPose();
        try {
            ps.translate(x + size / 2.0F, y + size / 2.0F, PAGE_ICON_Z_PIXELS);
            ps.scale(size, -size, size);
            Matrix4f pageIconPose = new Matrix4f(ps.last().pose());
            itemState.submit(ps, new PageItemCollector(collector, PAGE_ICON_ORDER,
                    shouldFlattenItemDepth(mc, stack), pageIconPose), PAGE_LIGHT, OverlayTexture.NO_OVERLAY, 0);
        } finally {
            ps.popPose();
        }
        submitItemDecorations(stack, x, y, size, ps, collector);
    }

    private static boolean shouldFlattenItemDepth(Minecraft mc, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return false;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ITEM_DEPTH_FLATTEN_CACHE.computeIfAbsent(itemId,
                id -> shouldFlattenBlockItemDepth(mc, id));
    }

    private static boolean shouldFlattenBlockItemDepth(Minecraft mc, Identifier itemId) {
        if (itemDefinitionUsesSpriteArt(mc, itemId)) {
            return false;
        }

        // A `minecraft:special` item model (e.g. the Beartrap/Wolftrap entity-machine renderer) draws
        // real 3D geometry, so it must be flattened like a block model. Its `base` icon model parents to
        // item/generated (sprite art), which would otherwise trip the sprite-art check below and skip
        // flattening, leaving the trap protruding from the page.
        if (itemDefinitionUsesSpecialModel(mc, itemId)) {
            return true;
        }

        Identifier legacyItemModel = Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
        if (modelUsesSpriteArt(mc, legacyItemModel, 0)) {
            return false;
        }

        return true;
    }

    private static boolean itemDefinitionUsesSpecialModel(Minecraft mc, Identifier itemId) {
        Optional<JsonObject> definition = readJsonObject(mc,
                Identifier.fromNamespaceAndPath(itemId.getNamespace(), "items/" + itemId.getPath() + ".json"));
        if (definition.isEmpty()) {
            return false;
        }
        JsonElement modelElement = definition.get().get("model");
        return modelElement instanceof JsonObject modelObject
                && "minecraft:special".equals(jsonString(modelObject, "type"));
    }

    private static boolean itemDefinitionUsesSpriteArt(Minecraft mc, Identifier itemId) {
        Optional<JsonObject> definition = readJsonObject(mc,
                Identifier.fromNamespaceAndPath(itemId.getNamespace(), "items/" + itemId.getPath() + ".json"));
        if (definition.isEmpty()) {
            return false;
        }

        JsonElement modelElement = definition.get().get("model");
        if (!(modelElement instanceof JsonObject modelObject)) {
            return false;
        }

        String type = jsonString(modelObject, "type");
        if ("minecraft:special".equals(type)) {
            return false;
        }
        if (!"minecraft:model".equals(type)) {
            return false;
        }

        String model = jsonString(modelObject, "model");
        Identifier modelId = model == null ? null : Identifier.tryParse(model);
        return modelId != null && modelUsesSpriteArt(mc, modelId, 0);
    }

    private static boolean modelUsesSpriteArt(Minecraft mc, Identifier modelId, int depth) {
        if (depth > 12) {
            return false;
        }

        Optional<JsonObject> model = readJsonObject(mc,
                Identifier.fromNamespaceAndPath(modelId.getNamespace(), "models/" + modelId.getPath() + ".json"));
        if (model.isEmpty()) {
            return false;
        }

        JsonObject modelObject = model.get();
        String parent = jsonString(modelObject, "parent");
        if (parent != null) {
            if (isSpriteArtParent(parent)) {
                return true;
            }

            Identifier parentId = Identifier.tryParse(parent);
            return parentId != null && modelUsesSpriteArt(mc, parentId, depth + 1);
        }

        JsonElement textures = modelObject.get("textures");
        return textures instanceof JsonObject textureObject
                && textureObject.has("layer0")
                && !modelObject.has("elements");
    }

    static boolean isSpriteArtParent(String parent) {
        return parent.equals("minecraft:item/generated")
                || parent.equals("minecraft:item/handheld")
                || parent.equals("minecraft:block/cross")
                || parent.equals("minecraft:block/tinted_cross")
                || parent.equals("minecraft:block/crop")
                || parent.equals("minecraft:block/stem_growth")
                || parent.equals("minecraft:block/flowerbed_1")
                || parent.equals("minecraft:block/flowerbed_2")
                || parent.equals("minecraft:block/flowerbed_3")
                || parent.equals("minecraft:block/flowerbed_4");
    }

    private static Optional<JsonObject> readJsonObject(Minecraft mc, Identifier resourceId) {
        return mc.getResourceManager().getResource(resourceId).flatMap(resource -> {
            try (Reader reader = resource.openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                return root instanceof JsonObject object ? Optional.of(object) : Optional.empty();
            } catch (Exception ignored) {
                return Optional.empty();
            }
        });
    }

    private static @Nullable String jsonString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static void submitItemDecorations(ItemStack stack, int x, int y, float size, PoseStack ps,
                                              SubmitNodeCollector collector) {
        if (stack.getCount() == 1) {
            return;
        }

        String count = String.valueOf(stack.getCount());
        int textX = Math.round(x + size - textWidth(count, ITEM_COUNT_TEXT_SCALE) - 1.0F);
        int textY = Math.round(y + size - 7.0F);
        submitTextLine(ps, collector, textX + 1, textY + 1, count, ITEM_COUNT_TEXT_SCALE, 0xFF1F160F);
        submitTextLine(ps, collector, textX, textY, count, ITEM_COUNT_TEXT_SCALE, 0xFFFFFFFF);
    }

    static void flattenItemDepthForPage(Matrix4f matrix) {
        flattenItemDepthForPage(matrix, null);
    }

    static void flattenItemDepthForPage(Matrix4f matrix, @Nullable Matrix4f pageIconPose) {
        Matrix4f planePose = pageIconPose == null ? new Matrix4f() : new Matrix4f(pageIconPose);
        Matrix4f localPose = new Matrix4f(planePose).invert().mul(matrix);
        matrix.set(planePose)
                .scale(1.0F, 1.0F, FLAT_ITEM_DEPTH_SCALE)
                .mul(localPose);
    }

    private static ItemStack firstStack(Either<ItemStack, Ingredient> item) {
        return item.map(ItemStack::copy, ingredient -> firstStack(ingredient.display(), recipeContext()));
    }

    private static ItemStack firstStack(SlotDisplay display, ContextMap context) {
        return firstStack(display.resolveForStacks(context));
    }

    private static ItemStack firstStack(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static ContextMap recipeContext() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? ContextMap.EMPTY : SlotDisplayContext.fromLevel(mc.level);
    }

    private static ItemStack spawnEggFor(String entityId) {
        EntityType<?> type = EntityType.byString(entityId).orElse(null);
        if (type == null) {
            return ItemStack.EMPTY;
        }
        SpawnEggItem spawnEgg = SpawnEggItem.byId(type);
        return spawnEgg == null ? ItemStack.EMPTY : new ItemStack(spawnEgg);
    }

    private static void submitOvenCues(PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                       int bgX, int panelTop, float scale) {
        int arrowY = panelTop + Math.round(21 * scale);
        submitPointedArrow(ps, collector, lightCoords,
                bgX + Math.round(31 * scale), arrowY,
                bgX + Math.round(49 * scale), bgX + Math.round(63 * scale));

        int flameX = bgX + Math.round(9 * scale);
        int flameY = panelTop + Math.round(31 * scale);
        submitGuiSprite(ps, collector, lightCoords, FURNACE_FLAME, flameX, flameY,
                Math.max(1, Math.round(14 * scale)), Math.max(1, Math.round(14 * scale)));
    }

    private static void submitDistilleryCues(PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                             int bgX, int panelTop, float scale) {
        submitPointedArrow(ps, collector, lightCoords,
                bgX + Math.round(39 * scale),
                panelTop + Math.round(27 * scale),
                bgX + Math.round(52 * scale),
                bgX + Math.round(63 * scale));

        submitBubble(ps, collector, lightCoords, bgX + Math.round(13 * scale), panelTop + Math.round(5 * scale), 3);
        submitBubble(ps, collector, lightCoords, bgX + Math.round(9 * scale), panelTop + Math.round(13 * scale), 4);
        submitBubble(ps, collector, lightCoords, bgX + Math.round(15 * scale), panelTop + Math.round(22 * scale), 3);
        submitBubble(ps, collector, lightCoords, bgX + Math.round(10 * scale), panelTop + Math.round(32 * scale), 5);
        submitBubble(ps, collector, lightCoords, bgX + Math.round(16 * scale), panelTop + Math.round(45 * scale), 3);
    }

    private static void submitPointedArrow(PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                           int left, int y, int base, int tip) {
        submitRect(ps, collector, lightCoords, left, y - 1, base + 1, y + 2, PAGE_INK_SOFT);
        for (int row = -5; row <= 5; row++) {
            int rowEnd = tip - Math.abs(row) * 2;
            submitRect(ps, collector, lightCoords, base, y + row, rowEnd, y + row + 1, PAGE_INK_SOFT);
        }
    }

    private static void submitNavigationArrow(boolean next, PoseStack ps, SubmitNodeCollector collector, int lightCoords) {
        int y = PAGE_HEIGHT - 14;
        if (next) {
            submitPointedArrow(ps, collector, lightCoords, PAGE_WIDTH - 38, y, PAGE_WIDTH - 21, PAGE_WIDTH - 8);
            return;
        }

        int lineLeft = 8;
        int lineRight = 25;
        int tip = 5;
        submitRect(ps, collector, lightCoords, lineLeft, y - 1, lineRight, y + 2, PAGE_INK_SOFT);
        for (int row = -5; row <= 5; row++) {
            int rowStart = tip + Math.abs(row) * 2;
            submitRect(ps, collector, lightCoords, rowStart, y + row, lineLeft, y + row + 1, PAGE_INK_SOFT);
        }
    }

    private static void submitIndexButton(PageSide side, PoseStack ps, SubmitNodeCollector collector, int lightCoords) {
        int x = side == PageSide.LEFT ? PAGE_WIDTH - 15 : 5;
        int y = PAGE_HEIGHT - 17;
        submitRect(ps, collector, lightCoords, x, y, x + 10, y + 9, 0x1153341A);
        submitRect(ps, collector, lightCoords, x + 2, y + 2, x + 8, y + 3, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, x + 2, y + 4, x + 8, y + 5, PAGE_INK_SOFT);
        submitRect(ps, collector, lightCoords, x + 2, y + 6, x + 8, y + 7, PAGE_INK_SOFT);
    }

    private static void submitPageNumber(PageSide side, int pageNumber, int pageCount, PoseStack ps,
                                         SubmitNodeCollector collector, int lightCoords) {
        String label = pageNumber + "/" + pageCount;
        int y = PAGE_HEIGHT - 7;
        int x = side == PageSide.LEFT
                ? 8
                : Math.round(PAGE_WIDTH - textWidth(label, FOOTER_TEXT_SCALE) - 8);
        submitTextLine(ps, collector, x, y, label, FOOTER_TEXT_SCALE, PAGE_INK_SOFT);
    }

    private static void submitBubble(PoseStack ps, SubmitNodeCollector collector, int lightCoords, int x, int y, int size) {
        submitRect(ps, collector, lightCoords, x + 1, y, x + size - 1, y + 1, BUBBLE);
        submitRect(ps, collector, lightCoords, x + 1, y + size - 1, x + size - 1, y + size, BUBBLE);
        submitRect(ps, collector, lightCoords, x, y + 1, x + 1, y + size - 1, BUBBLE);
        submitRect(ps, collector, lightCoords, x + size - 1, y + 1, x + size, y + size - 1, BUBBLE);
        if (size > 3) {
            submitRect(ps, collector, lightCoords, x + size - 2, y + 1, x + size - 1, y + 2, BUBBLE_LIGHT);
        }
    }

    private static void submitGuiSprite(PoseStack ps, SubmitNodeCollector collector, int lightCoords, Identifier spriteId,
                                        int x, int y, int width, int height) {
        AbstractTexture atlasTexture = Minecraft.getInstance().getTextureManager().getTexture(GUI_ATLAS);
        if (!(atlasTexture instanceof TextureAtlas atlas)) {
            return;
        }
        TextureAtlasSprite sprite = atlas.getSprite(spriteId);
        submitTextureSprite(ps, collector, PAGE_ICON_ORDER, lightCoords, sprite, x, y, width, height);
    }

    private static void submitTextureSprite(PoseStack ps, SubmitNodeCollector collector, int order, int lightCoords,
                                            TextureAtlasSprite sprite, int x, int y, int width, int height) {
        ps.pushPose();
        ps.translate(0.0F, 0.0F, PAGE_ICON_Z_PIXELS);
        collector.order(order).submitCustomGeometry(ps, RenderTypes.text(sprite.atlasLocation()),
                (pose, vertexConsumer) -> submitSpriteQuad(pose, vertexConsumer, lightCoords,
                        x, y, x + width, y + height, sprite, 0xFFFFFFFF));
        ps.popPose();
    }

    private static void submitRect(PoseStack ps, SubmitNodeCollector collector, int lightCoords,
                                   float x0, float y0, float x1, float y1, int color) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        collector.order(PAGE_FRAME_ORDER).submitCustomGeometry(ps, PAGE_INK_RENDER_TYPE,
                (pose, vertexConsumer) -> submitQuad(pose, vertexConsumer, PAGE_LIGHT, x0, y0, x1, y1, color));
    }

    private static void submitQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer, int lightCoords,
                                   float x0, float y0, float x1, float y1, int color) {
        submitSolidQuad(pose, vertexConsumer, lightCoords, x0, y0, x1, y1, 1.0F, color);
    }

    private static void submitSolidQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer, int lightCoords,
                                        float x0, float y0, float x1, float y1, float z, int color) {
        vertexConsumer.addVertex(pose, x0, y0, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y1, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y1, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y0, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y0, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y0, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y1, z).setColor(color).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y1, z).setColor(color).setLight(lightCoords);
    }

    private static void submitSpriteQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer, int lightCoords,
                                         float x0, float y0, float x1, float y1,
                                         TextureAtlasSprite sprite, int color) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        vertexConsumer.addVertex(pose, x0, y0, 1.0F).setColor(color).setUv(u0, v0).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y1, 1.0F).setColor(color).setUv(u0, v1).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y1, 1.0F).setColor(color).setUv(u1, v1).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y0, 1.0F).setColor(color).setUv(u1, v0).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y0, 1.0F).setColor(color).setUv(u0, v0).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y0, 1.0F).setColor(color).setUv(u1, v0).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x1, y1, 1.0F).setColor(color).setUv(u1, v1).setLight(lightCoords);
        vertexConsumer.addVertex(pose, x0, y1, 1.0F).setColor(color).setUv(u0, v1).setLight(lightCoords);
    }

    private static final class PageItemCollector implements SubmitNodeCollector {
        private final SubmitNodeCollector target;
        private final int baseOrder;
        private final boolean flattenItems;
        private final @Nullable Matrix4f pageIconPose;

        private PageItemCollector(SubmitNodeCollector target, int baseOrder) {
            this(target, baseOrder, false);
        }

        private PageItemCollector(SubmitNodeCollector target, int baseOrder, boolean flattenItems) {
            this(target, baseOrder, flattenItems, null);
        }

        private PageItemCollector(SubmitNodeCollector target, int baseOrder, boolean flattenItems,
                                  @Nullable Matrix4f pageIconPose) {
            this.target = target;
            this.baseOrder = baseOrder;
            this.flattenItems = flattenItems;
            this.pageIconPose = pageIconPose == null ? null : new Matrix4f(pageIconPose);
        }

        private OrderedSubmitNodeCollector ordered() {
            return this.target.order(this.baseOrder);
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return this.target.order(this.baseOrder + order);
        }

        @Override
        public void submitShadow(PoseStack poseStack, float shadowRadius, List<EntityRenderState.ShadowPiece> pieces) {
            this.ordered().submitShadow(poseStack, shadowRadius, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 position, int verticalShift, Component text,
                                  boolean seeThrough, int backgroundColor, double distanceToCameraSq,
                                  CameraRenderState camera) {
            this.ordered().submitNameTag(poseStack, position, verticalShift, text, seeThrough, backgroundColor,
                    distanceToCameraSq, camera);
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow,
                               Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor,
                               int outlineColor) {
            this.ordered().submitText(poseStack, x, y, text, dropShadow, displayMode, lightCoords, color,
                    backgroundColor, outlineColor);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf cameraRotation) {
            this.ordered().submitFlame(poseStack, state, cameraRotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            this.ordered().submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                     int lightCoords, int overlayCoords, int color,
                                     @Nullable TextureAtlasSprite texture, int outlineColor,
                                     ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords,
                            color, texture, outlineColor, crumblingOverlay));
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int lightCoords,
                                    int overlayCoords, @Nullable TextureAtlasSprite texture, boolean skipDraw,
                                    boolean skipChildren, int color,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay,
                                    int outlineColor) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords,
                            texture, skipDraw, skipChildren, color, crumblingOverlay, outlineColor));
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState state, int lightCoords, int overlayCoords,
                                int outlineColor) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitBlock(poseStack, state, lightCoords, overlayCoords, outlineColor));
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitMovingBlock(poseStack, state));
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model, float red,
                                     float green, float blue, int lightCoords, int overlayCoords, int outlineColor) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitBlockModel(poseStack, renderType, model, red, green, blue, lightCoords,
                            overlayCoords, outlineColor));
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords,
                               int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads,
                               RenderType renderType, ItemStackRenderState.FoilType foilType) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitItem(poseStack, displayContext, lightCoords, overlayCoords,
                            outlineColor, tintLayers, quads, renderType, foilType));
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                         SubmitNodeCollector.CustomGeometryRenderer renderer) {
            this.submitWithItemDepthFlattened(poseStack,
                    () -> this.ordered().submitCustomGeometry(poseStack, renderType, renderer));
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
            this.ordered().submitParticleGroup(renderer);
        }

        private void submitWithItemDepthFlattened(PoseStack poseStack, Runnable submitter) {
            if (!this.flattenItems) {
                submitter.run();
                return;
            }

            poseStack.pushPose();
            try {
                // Keep the GUI isometric projection, then squeeze only its protrusion from the page.
                flattenItemDepthForPage(poseStack.last().pose(), this.pageIconPose);
                submitter.run();
            } finally {
                poseStack.popPose();
            }
        }
    }

    private enum PageSide {
        LEFT,
        RIGHT
    }

    private record Spread(PlacedModonomiconBookResolver.PlacedPage leftPage,
                          @Nullable PlacedModonomiconBookResolver.PlacedPage rightPage,
                          int openPagesIndex, int pageCount) {}
}
