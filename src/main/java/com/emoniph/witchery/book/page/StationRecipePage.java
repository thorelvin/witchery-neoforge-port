package com.emoniph.witchery.book.page;

import com.emoniph.witchery.WitcheryMod;
import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.RenderedBookTextHolder;
import com.klikli_dev.modonomicon.book.conditions.BookCondition;
import com.klikli_dev.modonomicon.book.conditions.BookNoneCondition;
import com.klikli_dev.modonomicon.book.page.BookPage;
import com.klikli_dev.modonomicon.client.gui.book.markdown.BookTextRenderer;
import com.klikli_dev.modonomicon.util.BookGsonHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom Modonomicon page that renders a station GUI background (Witches' Oven / Distillery /
 * Witch's Cauldron) with <b>live {@link ItemStack}s</b> placed at slot coordinates. Because the items
 * are real, the renderer shows proper hover tooltips and stack counts — which the baked recipe-image
 * pages could not. Modonomicon only renders vanilla 3x3 crafting recipes natively, so this fills the
 * gap for the data-driven station recipes.
 *
 * <p>Registered via {@code LoaderRegistry.registerPageLoader} (data) and
 * {@code PageRendererRegistry.registerPageRenderer} (client).
 */
public class StationRecipePage extends BookPage {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "station_recipe");

    /** One item placed at (x,y) pixels relative to the background's top-left. */
    public record Slot(ItemStack stack, int x, int y) {}

    protected BookTextHolder title;
    protected BookTextHolder text;
    protected final Identifier background;
    protected final int bgWidth;
    protected final int bgHeight;
    protected final List<Slot> slots;

    public StationRecipePage(BookTextHolder title, BookTextHolder text, Identifier background,
                             int bgWidth, int bgHeight, List<Slot> slots, String anchor, BookCondition condition) {
        super(anchor, condition);
        this.title = title;
        this.text = text;
        this.background = background;
        this.bgWidth = bgWidth;
        this.bgHeight = bgHeight;
        this.slots = slots;
    }

    @Override
    public Identifier getType() {
        return TYPE;
    }

    public BookTextHolder getTitle() {
        return title;
    }

    public BookTextHolder getText() {
        return text;
    }

    public boolean hasTitle() {
        return !this.title.isEmpty();
    }

    public Identifier getBackground() {
        return background;
    }

    public int getBgWidth() {
        return bgWidth;
    }

    public int getBgHeight() {
        return bgHeight;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    @Override
    public void prerenderMarkdown(BookTextRenderer textRenderer) {
        super.prerenderMarkdown(textRenderer);
        if (!this.title.hasComponent()) {
            this.title = new RenderedBookTextHolder(this.title, textRenderer.render(this.title.getString()));
        }
        if (!this.text.hasComponent()) {
            this.text = new RenderedBookTextHolder(this.text, textRenderer.render(this.text.getString()));
        }
    }

    public static StationRecipePage fromJson(Identifier id, JsonObject json, HolderLookup.Provider provider) {
        BookTextHolder title = BookGsonHelper.getAsBookTextHolder(json, "title", BookTextHolder.EMPTY, provider);
        BookTextHolder text = BookGsonHelper.getAsBookTextHolder(json, "text", BookTextHolder.EMPTY, provider);
        Identifier background = Identifier.parse(GsonHelper.getAsString(json, "background"));
        int w = GsonHelper.getAsInt(json, "bg_width");
        int h = GsonHelper.getAsInt(json, "bg_height");
        List<Slot> slots = new ArrayList<>();
        for (var element : GsonHelper.getAsJsonArray(json, "slots")) {
            JsonObject s = element.getAsJsonObject();
            Item item = BuiltInRegistries.ITEM.getOptional(
                    Identifier.parse(GsonHelper.getAsString(s, "item"))).orElse(Items.AIR);
            int count = GsonHelper.getAsInt(s, "count", 1);
            slots.add(new Slot(new ItemStack(item, count), GsonHelper.getAsInt(s, "x"), GsonHelper.getAsInt(s, "y")));
        }
        BookCondition condition = json.has("condition")
                ? BookCondition.fromJson(id, GsonHelper.getAsJsonObject(json, "condition"), provider)
                : new BookNoneCondition();
        String anchor = GsonHelper.getAsString(json, "anchor", "");
        return new StationRecipePage(title, text, background, w, h, slots, anchor, condition);
    }

    public static StationRecipePage fromNetwork(RegistryFriendlyByteBuf buffer) {
        BookTextHolder title = BookTextHolder.fromNetwork(buffer);
        BookTextHolder text = BookTextHolder.fromNetwork(buffer);
        Identifier background = Identifier.parse(buffer.readUtf());
        int w = buffer.readVarInt();
        int h = buffer.readVarInt();
        int n = buffer.readVarInt();
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            slots.add(new Slot(stack, buffer.readVarInt(), buffer.readVarInt()));
        }
        // Base BookPage.toNetwork writes anchor then condition, last — read them in that order.
        String anchor = buffer.readUtf();
        BookCondition condition = BookCondition.fromNetwork(buffer);
        return new StationRecipePage(title, text, background, w, h, slots, anchor, condition);
    }

    @Override
    public boolean matchesQuery(String query, net.minecraft.world.level.Level level) {
        return this.title.getString().toLowerCase().contains(query)
                || this.text.getString().toLowerCase().contains(query);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer) {
        this.title.toNetwork(buffer);
        this.text.toNetwork(buffer);
        buffer.writeUtf(this.background.toString());
        buffer.writeVarInt(this.bgWidth);
        buffer.writeVarInt(this.bgHeight);
        buffer.writeVarInt(this.slots.size());
        for (Slot slot : this.slots) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, slot.stack());
            buffer.writeVarInt(slot.x());
            buffer.writeVarInt(slot.y());
        }
        super.toNetwork(buffer); // writes anchor + condition
    }
}
