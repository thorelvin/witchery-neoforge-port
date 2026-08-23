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
    void bothFumeFunnelItemsUseTheSourceStyleSpecialRenderer() throws IOException {
        assertSpecialItemRenderer("fume_funnel", "witchery:fume_funnel");
        assertSpecialItemRenderer("filtered_fume_funnel", "witchery:fume_funnel");
    }

    @Test
    void crystalBallUsesSourceTextureAndSpecialItemRenderer() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("crystal_ball", "witchery:crystal_ball");
        assertTexture("crystalball.png", 32, 32,
                "f94fc75397ed24c0beb2cbcedef4c7d0016eac106f0255d620cd439a063375c7");
    }

    @Test
    void dreamCatcherUsesSourceTextureAndSpecialItemRenderer() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("dream_catcher", "witchery:dream_catcher");
        assertTexture("dream_catcher.png", 32, 32,
                "baf6db1bbc84b759dcf311244a951e0744f2a0e92c68eeeb8f9c353aeb0e9414");
    }

    @Test
    void coffinUsesSourceTextureAndSpecialItemRenderer() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("coffin", "witchery:coffin");
        assertTexture("coffin.png", 128, 64,
                "0628aa62984d9dae0552883239b12f84eaf87706563e43928d47278a6e9788c3");
    }

    @Test
    void fetishesUseSourceTexturesAndSpecialItemRenderers() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("fetish_scarecrow", "witchery:fetish_scarecrow",
                "witchery:item/entity_machine_base");
        assertSpecialItemRenderer("fetish_trent_effigy", "witchery:fetish_trent_effigy",
                "witchery:item/entity_machine_base");
        assertTexture("scarecrow.png", 64, 64,
                "1a8500bc13480d59a6bdba0a8ccc4bd6ae245212b600484a6a8341d8a267e0ca");
        assertTexture("trent.png", 64, 64,
                "1b69b4f7b1f721e5125b0aeb4f1765d207a733f4f16e4d86a27256c5cdec0aed");
    }

    @Test
    void alluringSkullAndDemonHeartUseExactSourceBlockTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture("alluring_skull.png", 64, 32,
                "73c8eed3e1cd63c3dd7f9ec3941bda4c5a1884fe62afabd33614042839232f11");
        assertTexture("alluring_skull2.png", 64, 32,
                "f529612649ce5ffa3f29ad44005b2211ed70389ee7df3a051b3f14e87782a59b");
        assertTexture("demon_heart.png", 32, 32,
                "d33dec30a82ce17e2f4bf03421aa812259dde62ccda7b468b1354936175f6c1d");
    }

    @Test
    void hellhoundHeadUsesTheExactSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "hellhound.png"), 64, 32,
                "d861a36d3248a55c2aef77792044657bdd43e9db7c471ed97b1d3d4507f79ed5");
    }

    @Test
    void wolfAltarUsesSourceTextureAndSpecialItemRenderer() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("wolf_altar", "witchery:wolf_altar");
        assertTexture("wolfaltar.png", 128, 128,
                "78de0ce23aec4ece101a6e245f38360287bde14229db00df63605197fb34a703");
    }

    @Test
    void grassperAndGoddessUseSourceTexturesAndSpecialItemRenderers() throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("grassper", "witchery:grassper");
        assertSpecialItemRenderer("statue_goddess", "witchery:statue_goddess",
                "witchery:item/entity_machine_base");
        assertTexture("grassper.png", 64, 64,
                "ee4fc4018bc953266854c5d35eaff2846affd1fef4299364b0223068e74ca63d");
        assertTexture("statuegoddess.png", 64, 64,
                "ecb88c387268957600f59477e06c96e76075184a7b8931b076c02afa636706d2");
    }

    @Test
    void decurseStatuesAndDarkMarkUseExactSourceTexturesAndRoutes()
            throws IOException, NoSuchAlgorithmException {
        assertSpecialItemRenderer("decurseteleport", "witchery:statue_mandrake");
        assertSpecialItemRenderer("decursedirected", "witchery:statue_wolf");
        assertTexture("statuemandrake.png", 64, 32,
                "d5dc5743fb73ca6dcf0c6947b7b99b7adce788b871dd1b3f91c823054c5c97df");
        assertTexture("statuewolf.png", 64, 32,
                "e273a44174f059d53a6e6496d057e4bbdb5d1c48de6d513f03cb4e490f9e0752");
        assertTexture(Path.of("entity", "darkmark.png"), 64, 64,
                "c428b8ae6878b550282238366c518cbbfb230b18ce3bb0937c53da74205e599f");
    }

    @Test
    void broomUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "broom.png"), 32, 32,
                "196f2387ef9543a0655a3c949842bf2db50d40fdf3ab962e8f8d470d21d7559d");
    }

    @Test
    void deathAndDemonUseExactSourceTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "death.png"), 64, 64,
                "21d9c607d0be4950ea0f314ca99ed55feb5754c88ea1e0ec878b9c099d544865");
        assertTexture(Path.of("entity", "demon.png"), 128, 32,
                "1b00271a363e46f6fc6932774e72e46624ed6abd953bb27cbf17becc183432b6");
    }

    @Test
    void flameImpUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "imp.png"), 64, 64,
                "18335af8f7a9af9daeef9f14b6e4d702e9aa4fcf6d234f5378c0c4b5eb905094");
    }

    @Test
    void leonardUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "leonard.png"), 64, 64,
                "52924d840a3a10520a9e2de969da463987c0fe2d8469ccaa38286b5da06069eb");
    }

    @Test
    void lordOfTormentUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "lordoftorment.png"), 128, 128,
                "01bffdd34c1924f6b9b23a3901b8719cc1ced20d3d4b44f6dac75849a00c9e34");
    }

    @Test
    void hornedHuntsmanUsesTheExactSourceAvatarTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "avatar.png"), 128, 128,
                "eeabaa1302ae229ca69c3b1cfd7fe34ac6f7af9f49436ec6fbcc48f9db329190");
    }

    @Test
    void lilithUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "lilith.png"), 64, 64,
                "d242d9ee7514c2e98084fa1040faa9ef485e87b3974bb4df7841a3fbccdd3a06");
    }

    @Test
    void mindrakeUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "mindrake.png"), 64, 32,
                "45f73b63b4b12362f8d2bccdbc4c9fa5a424260251832b96c74cf518358a7b32");
    }

    @Test
    void mirrorFaceUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "mirrorface.png"), 64, 32,
                "63366aecd30d4ad3590aa940096f6390d696a62fb81b55e52fcbbc1bd8c98ccc");
    }

    @Test
    void nightmareUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "nightmare.png"), 64, 64,
                "fcaa502aa68c051fd082b14c42be2e186066dbdee2fb996803485edae15dc202");
    }

    @Test
    void parasyticLouseUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "louse.png"), 64, 32,
                "a6864bf636ea9a9c6c5820128c3897c3f814cca598c90eb4a011f359a52033b6");
    }

    @Test
    void treefydUsesTheExactSourceTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "treefyd.png"), 64, 32,
                "01f69ceede963112d8ec5220722908ad5e945917472005946ec5fab03b75f3aa");
    }

    @Test
    void wolfmanAndPlayerWolfUseExactLegacyTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "wolfman_entity.png"), 64, 64,
                "8fb637fcb03d23385962350f35f636b4b32fc34c066f79a3c700e06a3aea4ccb");
        assertTexture(Path.of("entity", "werewolf_wolf.png"), 64, 32,
                "53d7a333f011baba7af8722054ad601c754517ebdab69dfedf2c5e86028bd619");
    }

    @Test
    void witchHuntersUseTheThreeExactSourceTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "witchhunter1.png"), 64, 64,
                "8ca9b28856478235677f116ddbb38a27edf4b491609753dab09a0a7f858a6b18");
        assertTexture(Path.of("entity", "witchhunter2.png"), 64, 64,
                "ae68bdde9b3e17e434706693f3e2166d8aee5302b1e120ad17a861124d6784ac");
        assertTexture(Path.of("entity", "witchhunter3.png"), 64, 64,
                "b83cf7e40bed55b4765f7be38f4264e4856229fcd6e1604c36a0147920a9e554");
    }

    @Test
    void elleAndReflectionUseExactSourceTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "elle.png"), 64, 32,
                "48049c9f04ab86932ac41446ba6457531beabe8a7c1978f07bab69c235202ee2");
        assertTexture(Path.of("entity", "reflection.png"), 64, 32,
                "0f7c256b3029f6ec57f037c2f3cb8c509f8fabe779b54326da72a74b41b97dcf");
    }

    @Test
    void auditedWearablesRetainTheirExactSourceSheets() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "earmuffs.png"), 64, 64,
                "18ba65558d4d70f10e6e0c49baca715682bc795719dad83431746a91a1dc93c0");
        assertTexture(Path.of("entity", "deathsclothes.png"), 128, 64,
                "f8d9bc641b7092ef970c55f378c5ffe3df427763825b1892794fd131fcbb5b66");
        assertTexture(Path.of("entity", "hunterclothes.png"), 128, 64,
                "476fd9c153ad634eb449dc81c03c97198a6ba180a64d3cb687192584ed28e2ac");
        assertTexture(Path.of("entity", "hunterclothes2.png"), 128, 64,
                "c91c6246a48ebf21b1b7b9b45bc7de6d245eaaf71b2e89e97f15f9e7b3c16364");
    }

    @Test
    void vampireArmorUsesExactLegacySheets() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "vampirearmor.png"), 64, 96,
                "9a5432facc56cf5115530889b947721a5e4e7f7ee245db1838ab163128bf4b5e");
        assertTexture(Path.of("entity", "vampirearmor_chain.png"), 64, 96,
                "e3f616ed9c58976c1e128d3849c7a5dc5bfca8b5b8ee5a16d1b641fb5f5a7c21");
        assertTexture(Path.of("entity", "vampirearmor_over.png"), 64, 96,
                "f7d0e9fcddd061dd3506f6b6298b44852923fa614e37d2e93465f0848c804dbc");
        assertTexture(Path.of("entity", "vampirearmor_over_first.png"), 64, 96,
                "49b620e201fdfdd6fd366dbd04db785b7d6d106970e4f689083cfa686d47e982");
    }

    @Test
    void garlicGarlandUsesTheExactSourceModelAndInventoryTextures()
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("block", "garlic_garland.png"), 32, 32,
                "6145f77912ee1f7bd09e5e225ae240c76f429f7d91a3c2ebc960234c210fd0ff");
        assertTexture(Path.of("item", "garlic_garland.png"), 16, 16,
                "a830b5ce8e5956de0f6ac813b6d6a716c88720099530cf87fc53320bf7a8eec5");
    }

    @Test
    void mirrorUsesTheExactSourceBaseAndOccupiedTextures()
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("block", "mirror.png"), 32, 32,
                "577b6257cc0c0783572a0096af0d02bda27ad53aa96d82b19b7effa53cdfce95");
        assertTexture(Path.of("block", "mirror2.png"), 32, 32,
                "35b76c7072ea56298e83b6487c612a57935e84c1feb68666e263ec6c27adebf7");
    }

    @Test
    void spectralFamiliarUsesTheExactMinecraftSeventeenPigTexture()
            throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "spectral_familiar.png"), 64, 32,
                "c6bcc41d48803df2697574f1e1d52f743b435fa4a0c853e1e5d19849e6c96d3b");
    }

    @Test
    void witchAndGoblinClothesUseExactLegacySheets() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "witchclothes.png"), 128, 64,
                "ca0e5f6e62ea563a2d535af9570b65bb7d12613eb448fada9347ed688014992e");
        assertTexture(Path.of("entity", "witchclothes_overlay.png"), 64, 64,
                "c920607518651be19d27cc3dfa6cada85b412216db56cb0e82d0dc0560271c7e");
        assertTexture(Path.of("entity", "witchclothes_legs.png"), 128, 64,
                "8a6ce42545f14695d3d1d79708ec2165ee6d6ac9de531a0c97f9bd9ebca1ea3f");
        assertTexture(Path.of("entity", "witchclothes2_legs.png"), 128, 64,
                "6aba15d861544ad7d520100a74d76c93c77008cef2578cbbe9ac4e7d411290df");
        assertTexture(Path.of("entity", "goblinclothes.png"), 64, 32,
                "8ecd549bbf8712fec7b075151eaa5d4b4edb97ef4b1cba24d7d27e177f9becc4");
        assertTexture(Path.of("entity", "goblinclothes_head.png"), 64, 32,
                "e02a8c6ff0c8230a522c65a20d3de33430cd878c28a19082c97db0c246833253");
        assertTexture(Path.of("entity", "goblinclothes_legs.png"), 64, 32,
                "d1d1d8c5cc07e3d56dfd7d0cf0fcbcca24ad30c7b5238573546b09f8266be9ed");
    }

    @Test
    void barkBeltEquipmentLayersRetainTheTwoExactSourcePasses()
            throws IOException, NoSuchAlgorithmException {
        String baseHash = "6aba15d861544ad7d520100a74d76c93c77008cef2578cbbe9ac4e7d411290df";
        String overlayHash = "c920607518651be19d27cc3dfa6cada85b412216db56cb0e82d0dc0560271c7e";
        assertTexture(Path.of("entity", "equipment", "humanoid", "bark_belt_base.png"),
                128, 64, baseHash);
        assertTexture(Path.of("entity", "equipment", "humanoid", "bark_belt_overlay.png"),
                64, 64, overlayHash);
        assertTexture(Path.of("entity", "equipment", "humanoid_leggings", "bark_belt_base.png"),
                128, 64, baseHash);
        assertTexture(Path.of("entity", "equipment", "humanoid_leggings", "bark_belt_overlay.png"),
                64, 64, overlayHash);

        JsonObject equipment = readJson(ASSETS.resolve(Path.of("equipment", "bark_belt.json")));
        JsonObject layers = equipment.getAsJsonObject("layers");
        for (String layerType : List.of("humanoid", "humanoid_leggings")) {
            var passes = layers.getAsJsonArray(layerType);
            assertEquals("witchery:bark_belt_base",
                    passes.get(0).getAsJsonObject().get("texture").getAsString());
            assertEquals(6968628, passes.get(0).getAsJsonObject()
                    .getAsJsonObject("dyeable").get("color_when_undyed").getAsInt());
            assertEquals("witchery:bark_belt_overlay",
                    passes.get(1).getAsJsonObject().get("texture").getAsString());
        }
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
    void deathsHandUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "deathshand.png"), 64, 64,
                "a033fc70977bfc106b75040f16d75be3c6eefc115713460f163ff6c2aa9ca8da");
    }

    @Test
    void babaYagaUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "babayaga.png"), 128, 128,
                "337342d4d3f37dea1bd44b395558fff09a0c8c4f8217d28843bf64c809ec7961");
    }

    @Test
    void catFamiliarUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "blackcat.png"), 64, 32,
                "b625f47364e18b1dd8827a3d6440257049ab684bc73372a8a135f80ba70acf37");
    }

    @Test
    void owlFamiliarUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "owl.png"), 64, 32,
                "bcad090286670bda268dc5a64d10c5f6aad0d4bba44631b6265a3d5e795c4f47");
    }

    @Test
    void toadFamiliarUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "toad.png"), 32, 32,
                "aeb393bacb1729509c0e95e6bf6f3437888452d2d09b1a1e326a36b3f3550069");
    }

    @Test
    void entUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "ent.png"), 256, 256,
                "2bc3d52074404146d057853df42c64b956dea15a3bff1357f07bc0566ecddaa4");
    }

    @Test
    void flyingMonkeyUsesSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "flying_monkey.png"), 64, 32,
                "534cca3f43612b2e2d3d5d3b4fffe7fc216bf9823768a685600ca036aec2f707");
    }

    @Test
    void ghostFamilyUsesExactSourceEntityTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "spectre.png"), 64, 32,
                "b6a18bfdc4b817d20db4adc72b492346f6ab3cb38ff139e5aca978c5d807610c");
        assertTexture(Path.of("entity", "banshee.png"), 64, 32,
                "7bb244214d63046013d06846ae0a274f35681d9afcb7fcafc93e097c773ef50b");
        assertTexture(Path.of("entity", "poltergeist.png"), 64, 32,
                "f688c3709a33981227de197456f52e6cdafd1ffa7156bd0bc9bd6575ac2eb355");
        assertTexture(Path.of("entity", "spirit.png"), 32, 32,
                "4c2ea1d52d0cd5a0912ecd3be9844bda3355d6e201c636530ea0e6090d6fc735");
    }

    @Test
    void goblinGulgUsesExactSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "gulg.png"), 64, 64,
                "343faae2ee910feb690d5afe49bc5bae0f1121389fa9f01b53c7a0fbc5ed41e1");
    }

    @Test
    void goblinMogUsesExactSourceEntityTexture() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "mog.png"), 64, 64,
                "d2922c959e0450df68e0d84548ebc43dc26c06a66e241ff5cf90545eff5c941b");
    }

    @Test
    void hobgoblinUsesExactSourceProfessionTextures() throws IOException, NoSuchAlgorithmException {
        assertTexture(Path.of("entity", "hobgoblin.png"), 64, 32,
                "3ac17df4de84cd39a5440f5b89c9468a36888e5e66a7cf0345683202426fc3ad");
        assertTexture(Path.of("entity", "hobgoblin2.png"), 64, 32,
                "9786af0e8612b4b3c001d457ca81357b4928a307e6e08e82695a8e33d0c02c2b");
        assertTexture(Path.of("entity", "hobgoblin3.png"), 64, 32,
                "50bb8e5b26b7242f8707bbb78ed809bc460d87ffde21602124c414e3fc774b8a");
        assertTexture(Path.of("entity", "hobgoblin4.png"), 64, 32,
                "4c5b96934163a9c497fb157254129f616e415b616860959df559d899e6c9a262");
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
        assertSpecialItemRenderer(itemId, rendererType, "witchery:item/" + itemId);
    }

    private static void assertSpecialItemRenderer(String itemId, String rendererType, String baseModel)
            throws IOException {
        JsonObject root = readJson(ITEM_DEFINITIONS.resolve(itemId + ".json"));
        JsonObject model = root.getAsJsonObject("model");
        assertEquals("minecraft:special", model.get("type").getAsString());
        assertEquals(baseModel, model.get("base").getAsString());
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
