package com.emoniph.witchery.resources;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.ImmortalsObservationsBookItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationsBookParityTest {
    private static final Path PROJECT_ROOT = projectRoot();
    private static final Path ASSETS = PROJECT_ROOT.resolve(
            Path.of("src", "main", "resources", "assets", "witchery"));
    private static final List<String> SOURCE_PAGES = List.of(
            "[next=ritual1][h1 Observations of an Immortal][br][br][br][darkred A doomed scholar's account of discourse with the undead]",
            "[next=ritual2]It is with some reluctance I commit these observations to paper, for what I have witnessed is not for the weak of mind. Instead, take my words as a warning...",
            "[next=ritual3][br]...he was reminiscing over dinner this evening about his \"birth\", a demonic pact of sorts...[br][br]...butchering a chicken over a skull with a boline and holding a glass goblet to collect the blood is barbaric,\" I told him...",
            "[next=ritual4][br]...apparently start of a long forgotten rite (I made a sketch...[br][img=witchery:textures/gui/vritual.png|center|middle|64|64][br]...night, open to the moon, string, red dust, torches and skull...",
            "[next=ritual5][br][br]...pouring the blood onto the skull...[br][br]...mumbling about taking her to the lakes of lava...   ...underworld...",
            "[next=transfix|1][br]...proving his worth to Her...[br][br][br]...that glass goblet again, could someone really drink such a thing? ...",
            "[next=knockback|2][br]...Today, or should I say this evening, he told me of his first kill...[br][br][br]...the thirst that first night, he said, was overwhelming, he had to fully sait his hunger...",
            "[next=speed|3]...he found he was able to transfix his victims...[br][br]...was now able to drink as he needed without others realizing, so long as he did not drain more than half...[br][br]...did so, from five oblivious souls...",
            "[next=resistsun|4][br]...strength was flowing into him, the more he drank, as the nights progressed, the stronger he became...[br][br]...it was on the forth night after his mastery of drinking, that the world slowed down...",
            "[next=smashstone|5]...his greatest foe, the sun, was ever present, tormenting and instantly deadly to him...[br][br]...became his obsession... ...found a way to collect sunlight and burnt himself with it ten times during the night...",
            "[next=bats|6]...first walk in the sun after his rebirth brought him to bloody tears, he felt his blood burning, but no longer instantly...[br][br]...he needed more strength, and extingishing creatures of pure fire was his solution...[br]...twenty died.",
            "[next=mesmerize|7]...he could smash solid stone, but bound to the earth, however fast, he was still limited...[br][br]...he called on Her once more, repeating the rite of his rebirth...[br][br]...gifted Her a flower, the color of the blood She so craves...",
            "[next=maker|8]...he smiled, a rare event, when he told me of his first flight...[br][br]...he flew from village to village, untill he knew the full extent of his domain, there was now nowhere he could not go... ",
            "[next=maker2|8]...the weak minded would now not only let him drink his fill, but would also follow like faithful hounds...[br][br]...horror of all horrors, he lured five of them to specially prepared iron cages, topped with wood and with a gap at the front. He sealed them inside...",
            "[next=finalpowers|9][img=witchery:textures/gui/vcage.png|center|middle|64|64][br][br]...he began feeding from each of them; mezmerising them first, then carefully he drank all he could, without damaging any...",
            "At last he knew his blood was strong enough to replicate what She had done for him... ...left me weak, close to oblivion, but I watched him fill a glass goblet and hand it to me... ...we both sat next to a coffin, far from the sun's gaze, \"drink!\" is all he said...");

    @Test
    void sourcePageGraphAndRevealThresholdsRemainExact() {
        assertEquals(List.of("toc", "ritual1", "ritual2", "ritual3", "ritual4", "ritual5", "transfix",
                        "knockback", "speed", "resistsun", "smashstone", "bats", "mesmerize", "maker",
                        "maker2", "finalpowers"),
                ImmortalsObservationsBookItem.PAGE_NAMES);
        int[] visiblePages = {6, 7, 8, 9, 10, 11, 12, 13, 15, 16};
        int[] unlockLevels = {0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 9};
        for (int level = 0; level <= ImmortalsObservationsBookItem.MAX_LEVEL; level++) {
            assertEquals(visiblePages[level], ImmortalsObservationsBookItem.getVisiblePageCount(level),
                    "visible pages at research level " + level);
        }
        for (int page = 0; page < unlockLevels.length; page++) {
            assertEquals(unlockLevels[page], ImmortalsObservationsBookItem.getPageUnlockLevel(page),
                    "unlock level for " + ImmortalsObservationsBookItem.PAGE_NAMES.get(page));
        }
    }

    @Test
    void pageStackUsesSourceNamesAndCannotAdvanceBeyondResearch() {
        ItemStack book = new ItemStack(WitcheryItems.VAMPIRE_BOOK.get());
        assertEquals(0, ImmortalsObservationsBookItem.getStoredPage(book));
        ImmortalsObservationsBookItem.setStoredPage(book, 15);
        assertEquals(5, ImmortalsObservationsBookItem.getStoredPage(book));
        assertEquals(List.of("ritual1", "ritual2", "ritual3", "ritual4", "ritual5"), pageStack(book));

        for (int level = 1; level <= 8; level++) {
            assertTrue(ImmortalsObservationsBookItem.addTornPage(book));
        }
        ImmortalsObservationsBookItem.setStoredPage(book, 15);
        assertEquals(14, ImmortalsObservationsBookItem.getStoredPage(book));
        assertTrue(ImmortalsObservationsBookItem.addTornPage(book));
        ImmortalsObservationsBookItem.setStoredPage(book, 15);
        assertEquals(15, ImmortalsObservationsBookItem.getStoredPage(book));
        assertFalse(ImmortalsObservationsBookItem.addTornPage(book));
    }

    @Test
    void englishPagesAreTheLiteralSourceMarkup() throws IOException {
        Path language = ASSETS.resolve(Path.of("lang", "en_us.json"));
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(language)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (int page = 0; page < SOURCE_PAGES.size(); page++) {
            assertEquals(SOURCE_PAGES.get(page),
                    root.get(ImmortalsObservationsBookItem.getPageTranslationKey(page)).getAsString(),
                    "source markup page " + (page + 1));
        }
    }

    @Test
    void readerUsesTheThreeExactSourceTextures() throws IOException, NoSuchAlgorithmException {
        Path gui = ASSETS.resolve(Path.of("textures", "gui"));
        assertTexture(gui.resolve("book_single.png"),
                "427fd8b5846d5792ec3eee2c36ef4552d5bd668a2772da929d6236474ec58076");
        assertTexture(gui.resolve("vritual.png"),
                "aec6b7a0712de4d5e035f5e5e02025b082ed0255f9d6ed819df8a5ba60ff27b8");
        assertTexture(gui.resolve("vcage.png"),
                "8669d955509d97d12c1ed5c91caeb4294519bfadf1e7f7003eb3a7eb9f47797a");
    }

    private static List<String> pageStack(ItemStack book) {
        ListTag pages = book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getListOrEmpty(ImmortalsObservationsBookItem.TAG_PAGE_STACK);
        java.util.ArrayList<String> names = new java.util.ArrayList<>(pages.size());
        for (int index = 0; index < pages.size(); index++) {
            names.add(pages.getStringOr(index, ""));
        }
        return names;
    }

    private static void assertTexture(Path path, String sha256)
            throws IOException, NoSuchAlgorithmException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, path.getFileName() + " is a readable PNG");
        assertEquals(256, image.getWidth(), path.getFileName() + " width");
        assertEquals(256, image.getHeight(), path.getFileName() + " height");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        assertEquals(sha256, HexFormat.of().formatHex(digest), path.getFileName() + " source hash");
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "assets", "witchery")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate Witchery project root");
    }
}
