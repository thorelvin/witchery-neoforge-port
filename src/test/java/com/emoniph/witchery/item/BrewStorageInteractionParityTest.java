package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.menu.BrewStorageLayout;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrewStorageInteractionParityTest {
    private static final Path GUI = projectRoot().resolve(
            Path.of("src", "main", "resources", "assets", "witchery", "textures", "gui"));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Brews.bootstrap();
    }

    @Test
    void radialDirectionsMatchTheSourceClockwiseSlotOrder() {
        assertEquals(0, BrewBagItem.slotForMovement(7.0F, 0.0F));
        assertEquals(1, BrewBagItem.slotForMovement(3.5F, -3.5F));
        assertEquals(2, BrewBagItem.slotForMovement(0.0F, -7.0F));
        assertEquals(3, BrewBagItem.slotForMovement(-3.5F, -3.5F));
        assertEquals(4, BrewBagItem.slotForMovement(-7.0F, 0.0F));
        assertEquals(5, BrewBagItem.slotForMovement(-3.5F, 3.5F));
        assertEquals(6, BrewBagItem.slotForMovement(0.0F, 7.0F));
        assertEquals(7, BrewBagItem.slotForMovement(3.5F, 3.5F));
        assertEquals(-1, BrewBagItem.slotForMovement(3.49F, 3.49F));
    }

    @Test
    void bagStoragePreservesCountsAndComposedBrewComponents() {
        ItemStack bag = new ItemStack(WitcheryItems.BREW_BAG.get());
        BrewContents contents = new BrewContents(List.of(Items.SPIDER_EYE, Items.GUNPOWDER,
                Items.GLOWSTONE_DUST));
        ItemStack brew = BrewContainers.bottle(contents);
        brew.setCount(3);

        BrewBagItem.setStoredBrew(bag, 2, brew);
        ItemStack stored = BrewBagItem.getStoredBrew(bag, 2);
        assertEquals(3, stored.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(brew, stored));
        assertEquals(contents, stored.get(WitcheryDataComponents.BREW_CONTENTS.get()));

        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, bag).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        ItemStack decodedBrew = BrewBagItem.getStoredBrew(decoded, 2);
        assertEquals(3, decodedBrew.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(brew, decodedBrew));
    }

    @Test
    void urnCapacityAndFourOneStackSlotsRoundTrip() {
        ItemStack urn = LeonardsUrnItem.createWithCapacity(4);
        BrewContents contents = new BrewContents(List.of(Items.SPIDER_EYE, Items.GUNPOWDER));
        ItemStack brew = BrewContainers.bottle(contents);
        brew.setCount(12);

        assertEquals(4, LeonardsUrnItem.getCapacity(urn));
        for (int slot = 0; slot < 4; slot++) {
            assertTrue(LeonardsUrnItem.tryInsertStoredBrew(urn, brew));
            ItemStack stored = LeonardsUrnItem.getStoredBrew(urn, slot);
            assertEquals(1, stored.getCount());
            assertEquals(contents, stored.get(WitcheryDataComponents.BREW_CONTENTS.get()));
        }
        assertFalse(LeonardsUrnItem.tryInsertStoredBrew(urn, brew));

        Tag encoded = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, urn).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(4, LeonardsUrnItem.getCapacity(decoded));
        for (int slot = 0; slot < 4; slot++) {
            assertTrue(ItemStack.isSameItemSameComponents(
                    LeonardsUrnItem.getStoredBrew(urn, slot),
                    LeonardsUrnItem.getStoredBrew(decoded, slot)));
        }
    }

    @Test
    void sourceMenuCoordinatesStayExact() {
        assertEquals(176, BrewStorageLayout.Bag.WIDTH);
        assertEquals(132, BrewStorageLayout.Bag.HEIGHT);
        assertEquals(18, BrewStorageLayout.Bag.STORAGE_Y);
        assertEquals(16, BrewStorageLayout.Bag.storageX(0));
        assertEquals(142, BrewStorageLayout.Bag.storageX(7));
        assertEquals(49, BrewStorageLayout.Bag.PLAYER_INVENTORY_Y);
        assertEquals(107, BrewStorageLayout.Bag.HOTBAR_Y);

        assertEquals(176, BrewStorageLayout.Urn.WIDTH);
        assertEquals(184, BrewStorageLayout.Urn.HEIGHT);
        assertEquals(List.of(80, 80, 103, 56), List.of(
                BrewStorageLayout.Urn.storageX(0), BrewStorageLayout.Urn.storageX(1),
                BrewStorageLayout.Urn.storageX(2), BrewStorageLayout.Urn.storageX(3)));
        assertEquals(List.of(22, 70, 46, 46), List.of(
                BrewStorageLayout.Urn.storageY(0), BrewStorageLayout.Urn.storageY(1),
                BrewStorageLayout.Urn.storageY(2), BrewStorageLayout.Urn.storageY(3)));
        assertEquals(103, BrewStorageLayout.Urn.PLAYER_INVENTORY_Y);
        assertEquals(161, BrewStorageLayout.Urn.HOTBAR_Y);
    }

    @Test
    void sourceGuiTexturesRemainByteExact() throws Exception {
        assertTexture("radial.png", 256, 256,
                "60d32dd6ebffa4ccbc530c83bd39aacb46613b0f96b90aefc77427fb2200765b");
        assertTexture("generic_48.png", 256, 256,
                "9873603dbf63cbcc28747dc31524a5a559202e2587a25be94771295c06afb115");
        assertTexture("urn.png", 256, 256,
                "d2750055e6c5a930c30a07cc0b4c7b5e09df47018c7143e595c8d61ae3def7fd");
    }

    private static void assertTexture(String name, int width, int height, String sha256) throws Exception {
        Path texture = GUI.resolve(name);
        assertTrue(Files.isRegularFile(texture), name + " must be packaged");
        var image = ImageIO.read(texture.toFile());
        assertEquals(width, image.getWidth(), name + " width");
        assertEquals(height, image.getHeight(), name + " height");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture));
        assertEquals(sha256, HexFormat.of().formatHex(digest), name + " source hash");
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "assets", "witchery")))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
