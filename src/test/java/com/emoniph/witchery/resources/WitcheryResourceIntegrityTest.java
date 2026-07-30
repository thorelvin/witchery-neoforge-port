package com.emoniph.witchery.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WitcheryResourceIntegrityTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path ASSETS = PROJECT_ROOT.resolve(Path.of("src", "main", "resources", "assets", "witchery"));
    private static final Path DATA = PROJECT_ROOT.resolve(Path.of("src", "main", "resources", "data", "witchery"));
    private static final Path GENERATED_DATA = PROJECT_ROOT.resolve(Path.of(
            "src", "generated", "resources", "data", "witchery"));
    private static final Path ITEM_DEFINITIONS = ASSETS.resolve("items");
    private static final Path ITEM_MODELS = ASSETS.resolve("models").resolve("item");
    private static final Path MODEL_ROOT = ASSETS.resolve("models");
    private static final Path TEXTURE_ROOT = ASSETS.resolve("textures");
    private static final Set<String> REMOVED_ALIAS_IDS = Set.of(
            "witchery:bats_brew",
            "witchery:blight_brew",
            "witchery:bolt_splitting",
            "witchery:cleansing_brew",
            "witchery:contract_blaze",
            "witchery:contract_evaporate",
            "witchery:contract_fiery_touch",
            "witchery:contract_ownership",
            "witchery:contract_resist_fire",
            "witchery:contract_smelting",
            "witchery:contract_torment",
            "witchery:cursed_leaping_brew",
            "witchery:demonic_blood",
            "witchery:depths_brew",
            "witchery:diviner",
            "witchery:endless_water_brew",
            "witchery:erosion_brew",
            "witchery:flowing_spirit_brew",
            "witchery:frogs_tongue_brew",
            "witchery:glass_goblet_full",
            "witchery:grotesque_brew",
            "witchery:gulgs_grudle",
            "witchery:hitchcock_brew",
            "witchery:hollow_tears_brew",
            "witchery:hunter_boots_silvered",
            "witchery:hunter_chestplate",
            "witchery:hunter_chestplate_silvered",
            "witchery:hunter_helmet",
            "witchery:hunter_helmet_silvered",
            "witchery:hunter_leggings_silvered",
            "witchery:hunts_horn",
            "witchery:huntsmans_spear",
            "witchery:ice_brew",
            "witchery:ice_slippers",
            "witchery:infection_brew",
            "witchery:infused_brew_of_soaring",
            "witchery:infused_brew_of_the_grave",
            "witchery:ink_brew",
            "witchery:love_brew",
            "witchery:raising_brew",
            "witchery:revealing_brew",
            "witchery:sleeping_brew",
            "witchery:solidifying_dirt_brew",
            "witchery:solidifying_erosion_brew",
            "witchery:solidifying_sand_brew",
            "witchery:solidifying_sandstone_brew",
            "witchery:solidifying_stone_brew",
            "witchery:sprouting_brew",
            "witchery:substitution_brew",
            "witchery:thorns_brew",
            "witchery:torment_contract",
            "witchery:vines_brew",
            "witchery:wasting_brew",
            "witchery:webs_brew",
            "witchery:witch_hunter_dawn_boots",
            "witchery:witch_hunter_dawn_coat",
            "witchery:witch_hunter_dawn_hat",
            "witchery:witch_hunter_dawn_trousers",
            "witchery:witches_robe");

    @Test
    void itemDefinitionModelReferencesResolve() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path itemDefinition : jsonFiles(ITEM_DEFINITIONS)) {
            JsonObject root = readJson(itemDefinition);
            for (ModelReference reference : collectItemModelReferences(root)) {
                if (!modelExists(reference.id())) {
                    failures.add(itemDefinition + " references missing model " + reference.id()
                            + " at " + reference.path());
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> "Missing item definition model references:\n" + joinFailures(failures));
    }

    @Test
    void itemModelTextureReferencesResolve() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path itemModel : jsonFiles(ITEM_MODELS)) {
            JsonObject root = readJson(itemModel);
            for (ModelReference parent : collectParentReferences(root)) {
                if (!modelExists(parent.id())) {
                    failures.add(itemModel + " uses missing parent " + parent.id()
                            + " at " + parent.path());
                }
            }
            for (ModelReference texture : collectTextureReferences(root)) {
                if (!textureExists(texture.id())) {
                    failures.add(itemModel + " uses missing texture " + texture.id()
                            + " at " + texture.path());
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> "Missing item model resources:\n" + joinFailures(failures));
    }

    @Test
    void dataResourcesDoNotReferenceRemovedAliasItemIds() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path root : List.of(DATA, GENERATED_DATA)) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            for (Path file : jsonFiles(root)) {
                String text = Files.readString(file);
                for (String id : REMOVED_ALIAS_IDS) {
                    if (containsExactIdentifier(text, id)) {
                        failures.add(file + " still references removed item id " + id);
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), () -> "Removed alias ids still appear in data resources:\n"
                + joinFailures(failures));
    }

    @Test
    void summonedEntityLangKeysExist() throws IOException {
        JsonObject lang = readJson(ASSETS.resolve(Path.of("lang", "en_us.json")));
        assertTrue(lang.has("entity.witchery.attack_bat"), "Attack Bat entity is missing an en_us lang key");
        assertTrue(lang.has("witchery.vampire.ultimate.swarm"), "Vampire swarm ultimate is missing an en_us lang key");
        assertTrue(lang.has("witchery.vampire.ultimate.no_charges"), "Vampire ultimate no-charge feedback is missing an en_us lang key");
    }

    @Test
    void decurseStatuesUseSourceTexturesAndSpecialItemRenderers() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("decurseteleport", "witchery:statue_mandrake");
        assertSpecialItemRenderer("decursedirected", "witchery:statue_wolf");
        assertTexture("statuemandrake.png", 64, 32,
                "d5dc5743fb73ca6dcf0c6947b7b99b7adce788b871dd1b3f91c823054c5c97df");
        assertTexture("statuewolf.png", 64, 32,
                "e273a44174f059d53a6e6496d057e4bbdb5d1c48de6d513f03cb4e490f9e0752");
    }

    @Test
    void silverVatUsesSourceTextureAndSpecialItemRenderer() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("silver_vat", "witchery:silver_vat");
        assertTexture("silvervat.png", 64, 32,
                "4a7f35fcfd7cdda8fd4e731a85f69c4d1881daa04ee09ef509a271b52e374e6b");
        assertTexture("silvervat_liquid.png", 64, 16,
                "9d80249fba30d0d7ec065e830b0bd093eba6cc4bbc1795e8589461cb4c3d71e1");
    }

    @Test
    void mandrakeMobUsesSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "mandrake.png"), 64, 32,
                "63edc96e105e7da2d801b59dd6880ee2032c851c9c104a689218b01b51b9cb13");
    }

    @Test
    void demonMobUsesSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "demon.png"), 128, 32,
                "1b00271a363e46f6fc6932774e72e46624ed6abd953bb27cbf17becc183432b6");
    }

    @Test
    void flameImpUsesSourceTextureAndHasNoOrdinaryDeathLoot()
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "imp.png"), 64, 64,
                "18335af8f7a9af9daeef9f14b6e4d702e9aa4fcf6d234f5378c0c4b5eb905094");
        JsonObject loot = readJson(DATA.resolve(Path.of("loot_table", "entities", "flame_imp.json")));
        assertEquals(0, loot.getAsJsonArray("pools").size(),
                "EntityImp has no ordinary death-drop pools");
    }

    @Test
    void nightmareUsesSourceTextureAndHasNoInventedOrdinaryLoot()
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "nightmare.png"), 64, 64,
                "fcaa502aa68c051fd082b14c42be2e186066dbdee2fb996803485edae15dc202");
        JsonObject loot = readJson(DATA.resolve(Path.of("loot_table", "entities", "nightmare.json")));
        assertEquals(0, loot.getAsJsonArray("pools").size(),
                "EntityNightmare only drops Mellifluous Hunger through its Spirit World rule");
    }

    @Test
    void sleepingBodyHasANameAndNoOrdinaryLoot() throws IOException {
        JsonObject lang = readJson(ASSETS.resolve(Path.of("lang", "en_us.json")));
        assertEquals("Body", lang.get("entity.witchery.corpse").getAsString());
        JsonObject loot = readJson(DATA.resolve(Path.of("loot_table", "entities", "corpse.json")));
        assertEquals(0, loot.getAsJsonArray("pools").size(),
                "EntityCorpse has no ordinary death-drop pools");
    }

    private static void assertSpecialItemRenderer(String itemId, String rendererType) throws IOException {
        JsonObject root = readJson(ITEM_DEFINITIONS.resolve(itemId + ".json"));
        JsonObject model = root.getAsJsonObject("model");
        assertEquals("minecraft:special", model.get("type").getAsString());
        assertEquals("witchery:item/" + itemId, model.get("base").getAsString());
        assertEquals(rendererType, model.getAsJsonObject("model").get("type").getAsString());
    }

    private static void assertTexture(String fileName, int expectedWidth, int expectedHeight, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("block", fileName), expectedWidth, expectedHeight, expectedSha256);
    }

    private static void assertTexture(Path relativePath, int expectedWidth, int expectedHeight, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
        Path texture = TEXTURE_ROOT.resolve(relativePath);
        String fileName = relativePath.toString();
        BufferedImage image = ImageIO.read(texture.toFile());
        assertNotNull(image, fileName + " is not a readable PNG");
        assertEquals(expectedWidth, image.getWidth(), fileName + " width changed");
        assertEquals(expectedHeight, image.getHeight(), fileName + " height changed");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture));
        assertEquals(expectedSha256, HexFormat.of().formatHex(digest), fileName + " is no longer the source texture");
    }

    private static List<ModelReference> collectItemModelReferences(JsonElement root) {
        List<ModelReference> references = new ArrayList<>();
        collectStringValues(root, "", references, Set.of("model", "base"));
        references.removeIf(reference -> !reference.id().startsWith("witchery:"));
        return references;
    }

    private static List<ModelReference> collectParentReferences(JsonElement root) {
        List<ModelReference> references = new ArrayList<>();
        collectStringValues(root, "", references, Set.of("parent"));
        references.removeIf(reference -> !reference.id().startsWith("witchery:"));
        return references;
    }

    private static List<ModelReference> collectTextureReferences(JsonElement root) {
        List<ModelReference> references = new ArrayList<>();
        collectTextureValues(root, "", false, references);
        references.removeIf(reference -> reference.id().startsWith("#")
                || !reference.id().startsWith("witchery:"));
        return references;
    }

    private static void collectStringValues(JsonElement element, String path, List<ModelReference> references,
                                            Set<String> fieldNames) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (var entry : object.entrySet()) {
                String childPath = path + "/" + entry.getKey();
                JsonElement value = entry.getValue();
                if (fieldNames.contains(entry.getKey()) && value.isJsonPrimitive()
                        && value.getAsJsonPrimitive().isString()) {
                    references.add(new ModelReference(value.getAsString(), childPath));
                } else {
                    collectStringValues(value, childPath, references, fieldNames);
                }
            }
        } else if (element.isJsonArray()) {
            for (int i = 0; i < element.getAsJsonArray().size(); i++) {
                collectStringValues(element.getAsJsonArray().get(i), path + "[" + i + "]", references, fieldNames);
            }
        }
    }

    private static void collectTextureValues(JsonElement element, String path, boolean inTextures,
                                             List<ModelReference> references) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (var entry : object.entrySet()) {
                boolean childInTextures = inTextures || "textures".equals(entry.getKey());
                collectTextureValues(entry.getValue(), path + "/" + entry.getKey(), childInTextures, references);
            }
        } else if (element.isJsonArray()) {
            for (int i = 0; i < element.getAsJsonArray().size(); i++) {
                collectTextureValues(element.getAsJsonArray().get(i), path + "[" + i + "]", inTextures, references);
            }
        } else if (inTextures && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            references.add(new ModelReference(element.getAsString(), path));
        }
    }

    private static boolean modelExists(String id) {
        IdentifierParts parts = parse(id);
        if (!"witchery".equals(parts.namespace())) {
            return true;
        }
        return Files.isRegularFile(MODEL_ROOT.resolve(parts.path() + ".json"));
    }

    private static boolean textureExists(String id) {
        IdentifierParts parts = parse(id);
        if (!"witchery".equals(parts.namespace())) {
            return true;
        }
        return Files.isRegularFile(TEXTURE_ROOT.resolve(parts.path() + ".png"));
    }

    private static IdentifierParts parse(String id) {
        int namespaceEnd = id.indexOf(':');
        if (namespaceEnd < 0) {
            return new IdentifierParts("minecraft", id);
        }
        String namespace = id.substring(0, namespaceEnd);
        String path = id.substring(namespaceEnd + 1);
        return new IdentifierParts(namespace, path);
    }

    private static boolean containsExactIdentifier(String text, String id) {
        return Pattern.compile(Pattern.quote(id) + "(?![a-z0-9_./-])").matcher(text).find();
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "assets", "witchery", "items")))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static List<Path> jsonFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static String joinFailures(List<String> failures) {
        return String.join("\n", failures.stream().limit(40).toList())
                + (failures.size() > 40 ? "\n... and " + (failures.size() - 40) + " more" : "");
    }

    private record ModelReference(String id, String path) {
    }

    private record IdentifierParts(String namespace, String path) {
    }
}
