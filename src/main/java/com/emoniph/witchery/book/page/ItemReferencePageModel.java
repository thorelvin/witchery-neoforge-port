package com.emoniph.witchery.book.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.api.datagen.book.BookTextHolderModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/** Datagen model for a compact item-icon reference list page. */
public class ItemReferencePageModel extends BookPageModel<ItemReferencePageModel> {

    private record RowModel(Identifier item, BookTextHolderModel text) {}

    private BookTextHolderModel title = new BookTextHolderModel("");
    private BookTextHolderModel text = new BookTextHolderModel("");
    private final List<RowModel> rows = new ArrayList<>();

    protected ItemReferencePageModel() {
        super(ItemReferencePage.TYPE);
    }

    public static ItemReferencePageModel create() {
        return new ItemReferencePageModel();
    }

    public ItemReferencePageModel withTitle(Component title) {
        this.title = new BookTextHolderModel(title);
        return this;
    }

    public ItemReferencePageModel withText(String text) {
        this.text = new BookTextHolderModel(text);
        return this;
    }

    public ItemReferencePageModel withRow(ItemLike item, String text) {
        this.rows.add(new RowModel(BuiltInRegistries.ITEM.getKey(item.asItem()), new BookTextHolderModel(text)));
        return this;
    }

    @Override
    public JsonObject toJson(Identifier bookId, HolderLookup.Provider provider) {
        JsonObject json = super.toJson(bookId, provider);
        json.add("title", this.title.toJson(provider));
        json.add("text", this.text.toJson(provider));
        JsonArray rowArray = new JsonArray();
        for (RowModel row : this.rows) {
            JsonObject r = new JsonObject();
            r.addProperty("item", row.item().toString());
            r.add("text", row.text().toJson(provider));
            rowArray.add(r);
        }
        json.add("rows", rowArray);
        return json;
    }
}
