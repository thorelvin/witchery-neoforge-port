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

/** A compact Modonomicon reference page with live item icons and text rows. */
public class ItemReferencePage extends BookPage {

    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "item_reference");

    public record Row(ItemStack stack, BookTextHolder text) {}

    protected BookTextHolder title;
    protected BookTextHolder text;
    protected final List<Row> rows;

    public ItemReferencePage(BookTextHolder title, BookTextHolder text, List<Row> rows,
                             String anchor, BookCondition condition) {
        super(anchor, condition);
        this.title = title;
        this.text = text;
        this.rows = rows;
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

    public List<Row> getRows() {
        return rows;
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
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            if (!row.text().hasComponent()) {
                this.rows.set(i, new Row(row.stack(),
                        new RenderedBookTextHolder(row.text(), textRenderer.render(row.text().getString()))));
            }
        }
    }

    public static ItemReferencePage fromJson(Identifier id, JsonObject json, HolderLookup.Provider provider) {
        BookTextHolder title = BookGsonHelper.getAsBookTextHolder(json, "title", BookTextHolder.EMPTY, provider);
        BookTextHolder text = BookGsonHelper.getAsBookTextHolder(json, "text", BookTextHolder.EMPTY, provider);
        List<Row> rows = new ArrayList<>();
        for (var element : GsonHelper.getAsJsonArray(json, "rows")) {
            JsonObject r = element.getAsJsonObject();
            Item item = BuiltInRegistries.ITEM.getOptional(
                    Identifier.parse(GsonHelper.getAsString(r, "item"))).orElse(Items.AIR);
            BookTextHolder rowText = BookGsonHelper.getAsBookTextHolder(r, "text", BookTextHolder.EMPTY, provider);
            rows.add(new Row(new ItemStack(item), rowText));
        }
        BookCondition condition = json.has("condition")
                ? BookCondition.fromJson(id, GsonHelper.getAsJsonObject(json, "condition"), provider)
                : new BookNoneCondition();
        String anchor = GsonHelper.getAsString(json, "anchor", "");
        return new ItemReferencePage(title, text, rows, anchor, condition);
    }

    public static ItemReferencePage fromNetwork(RegistryFriendlyByteBuf buffer) {
        BookTextHolder title = BookTextHolder.fromNetwork(buffer);
        BookTextHolder text = BookTextHolder.fromNetwork(buffer);
        int n = buffer.readVarInt();
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            rows.add(new Row(stack, BookTextHolder.fromNetwork(buffer)));
        }
        String anchor = buffer.readUtf();
        BookCondition condition = BookCondition.fromNetwork(buffer);
        return new ItemReferencePage(title, text, rows, anchor, condition);
    }

    @Override
    public boolean matchesQuery(String query, net.minecraft.world.level.Level level) {
        if (this.title.getString().toLowerCase().contains(query)
                || this.text.getString().toLowerCase().contains(query)) {
            return true;
        }
        for (Row row : this.rows) {
            if (row.text().getString().toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer) {
        this.title.toNetwork(buffer);
        this.text.toNetwork(buffer);
        buffer.writeVarInt(this.rows.size());
        for (Row row : this.rows) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, row.stack());
            row.text().toNetwork(buffer);
        }
        super.toNetwork(buffer);
    }
}
