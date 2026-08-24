package com.emoniph.witchery.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Audited, book-ready view of the original 96 Circle Magic registrations. */
final class RitualCompendium {

    static final String RESOURCE = "/data/witchery/ritual/ritual_compendium.json";

    record Offering(Identifier item, String label, boolean optional) {}
    record Circle(String color, String size) {}

    record Variant(int sourceId, int sourceLine, String translationKey, String title, String variantLabel,
                   String description, String modernHome, String mode, int power, int coven,
                   List<String> conditions, List<Circle> circles, List<Offering> required,
                   List<Offering> optional) {
        List<Offering> offerings() {
            List<Offering> result = new ArrayList<>(required.size() + optional.size());
            result.addAll(required);
            result.addAll(optional);
            return List.copyOf(result);
        }
    }

    record Family(String id, String title, Identifier icon, List<Integer> sourceIds, List<Variant> variants) {}

    private static final List<Family> FAMILIES = load();

    private RitualCompendium() {}

    static List<Family> families() {
        return FAMILIES;
    }

    private static List<Family> load() {
        var stream = RitualCompendium.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing ritual compendium resource " + RESOURCE);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1 || root.get("registrationCount").getAsInt() != 96) {
                throw new IllegalStateException("Unsupported or incomplete ritual compendium resource");
            }
            List<Family> families = new ArrayList<>();
            for (var familyElement : root.getAsJsonArray("families")) {
                JsonObject family = familyElement.getAsJsonObject();
                List<Integer> sourceIds = new ArrayList<>();
                for (var sourceId : family.getAsJsonArray("sourceIds")) {
                    sourceIds.add(sourceId.getAsInt());
                }
                List<Variant> variants = new ArrayList<>();
                for (var variantElement : family.getAsJsonArray("variants")) {
                    variants.add(parseVariant(variantElement.getAsJsonObject()));
                }
                families.add(new Family(
                        family.get("id").getAsString(),
                        family.get("title").getAsString(),
                        Identifier.parse(family.get("icon").getAsString()),
                        List.copyOf(sourceIds),
                        List.copyOf(variants)));
            }
            if (families.size() != root.get("familyCount").getAsInt()) {
                throw new IllegalStateException("Ritual compendium family count does not match its payload");
            }
            return List.copyOf(families);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read ritual compendium resource", exception);
        }
    }

    private static Variant parseVariant(JsonObject json) {
        return new Variant(
                json.get("sourceId").getAsInt(),
                json.get("sourceLine").getAsInt(),
                json.get("translationKey").getAsString(),
                json.get("title").getAsString(),
                json.get("variantLabel").getAsString(),
                json.get("description").getAsString(),
                json.get("modernHome").getAsString(),
                json.get("mode").getAsString(),
                json.get("power").getAsInt(),
                json.get("coven").getAsInt(),
                strings(json.getAsJsonArray("conditions")),
                circles(json.getAsJsonArray("circles")),
                offerings(json.getAsJsonArray("required")),
                offerings(json.getAsJsonArray("optional")));
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (var element : array) {
            values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static List<Circle> circles(JsonArray array) {
        List<Circle> values = new ArrayList<>();
        for (var element : array) {
            JsonObject json = element.getAsJsonObject();
            values.add(new Circle(json.get("color").getAsString(), json.get("size").getAsString()));
        }
        return List.copyOf(values);
    }

    private static List<Offering> offerings(JsonArray array) {
        List<Offering> values = new ArrayList<>();
        for (var element : array) {
            JsonObject json = element.getAsJsonObject();
            values.add(new Offering(
                    Identifier.parse(json.get("item").getAsString()),
                    json.get("label").getAsString(),
                    json.get("optional").getAsBoolean()));
        }
        return List.copyOf(values);
    }
}
