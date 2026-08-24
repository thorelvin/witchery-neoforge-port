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

/** Datagen model that emits a {@link StationRecipePage} ({@code witchery:station_recipe}) JSON. */
public class StationRecipePageModel extends BookPageModel<StationRecipePageModel> {

    private record SlotModel(Identifier item, int count, int x, int y, boolean optional) {}
    private record RingModel(String color, String size) {}

    private BookTextHolderModel title = new BookTextHolderModel("");
    private BookTextHolderModel text = new BookTextHolderModel("");
    private Identifier background;
    private int bgWidth;
    private int bgHeight;
    private boolean showBackground = true;
    private boolean ovenCues = false;
    private boolean distilleryCues = false;
    private final List<SlotModel> slots = new ArrayList<>();
    private final List<RingModel> rings = new ArrayList<>();
    private int covenFaces = 0;
    private int sourceId = 0;

    protected StationRecipePageModel() {
        super(StationRecipePage.TYPE);
    }

    public static StationRecipePageModel create() {
        return new StationRecipePageModel();
    }

    public StationRecipePageModel withTitle(Component title) {
        this.title = new BookTextHolderModel(title);
        return this;
    }

    public StationRecipePageModel withText(String text) {
        this.text = new BookTextHolderModel(text);
        return this;
    }

    public StationRecipePageModel withBackground(Identifier background, int width, int height) {
        this.background = background;
        this.bgWidth = width;
        this.bgHeight = height;
        return this;
    }

    public StationRecipePageModel withShowBackground(boolean showBackground) {
        this.showBackground = showBackground;
        return this;
    }

    public StationRecipePageModel withOvenCues(boolean ovenCues) {
        this.ovenCues = ovenCues;
        return this;
    }

    public StationRecipePageModel withDistilleryCues(boolean distilleryCues) {
        this.distilleryCues = distilleryCues;
        return this;
    }

    public StationRecipePageModel withSlot(ItemLike item, int count, int x, int y) {
        return this.withSlot(item, count, x, y, false);
    }

    /** Adds an offering whose gold frame marks it as optional in ritual cards. */
    public StationRecipePageModel withOptionalSlot(ItemLike item, int count, int x, int y) {
        return this.withSlot(item, count, x, y, true);
    }

    private StationRecipePageModel withSlot(ItemLike item, int count, int x, int y, boolean optional) {
        this.slots.add(new SlotModel(BuiltInRegistries.ITEM.getKey(item.asItem()), count, x, y, optional));
        return this;
    }

    /** Add one chalk circle to the ring diagram. {@code color}: white/purple/red; {@code size}: small/medium/large. */
    public StationRecipePageModel withRing(String color, String size) {
        this.rings.add(new RingModel(color, size));
        return this;
    }

    /** Number of coven witches the rite requires; rendered as that many witch faces (0 = none). */
    public StationRecipePageModel withCovenFaces(int count) {
        this.covenFaces = count;
        return this;
    }

    /** Original Witchery registration ID, retained in generated JSON for parity auditing. */
    public StationRecipePageModel withSourceId(int sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    @Override
    public JsonObject toJson(Identifier bookId, HolderLookup.Provider provider) {
        JsonObject json = super.toJson(bookId, provider); // type, anchor, condition
        json.add("title", this.title.toJson(provider));
        json.add("text", this.text.toJson(provider));
        json.addProperty("background", this.background.toString());
        json.addProperty("bg_width", this.bgWidth);
        json.addProperty("bg_height", this.bgHeight);
        if (!this.showBackground) {
            json.addProperty("show_background", false);
        }
        if (this.ovenCues) {
            json.addProperty("oven_cues", true);
        }
        if (this.distilleryCues) {
            json.addProperty("distillery_cues", true);
        }
        JsonArray slotArray = new JsonArray();
        for (SlotModel slot : this.slots) {
            JsonObject s = new JsonObject();
            s.addProperty("item", slot.item().toString());
            s.addProperty("count", slot.count());
            s.addProperty("x", slot.x());
            s.addProperty("y", slot.y());
            if (slot.optional()) {
                s.addProperty("optional", true);
            }
            slotArray.add(s);
        }
        json.add("slots", slotArray);
        if (!this.rings.isEmpty()) {
            JsonArray ringArray = new JsonArray();
            for (RingModel ring : this.rings) {
                JsonObject r = new JsonObject();
                r.addProperty("color", ring.color());
                r.addProperty("size", ring.size());
                ringArray.add(r);
            }
            json.add("rings", ringArray);
        }
        if (this.covenFaces > 0) {
            json.addProperty("coven_faces", this.covenFaces);
        }
        if (this.sourceId > 0) {
            json.addProperty("source_id", this.sourceId);
        }
        return json;
    }
}
