package com.emoniph.witchery.brewing;

import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.BrewContainers;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the generic composed-brew container/serializer architecture: a brew is persisted only as its
 * ordered ingredient list ({@link BrewContents}) and rebuilt on demand, the bottled dispersal is
 * derived from the contents' modifiers, and that dispersal selects the matching container item. These
 * are the invariants the cauldron, thrown brew, bucket and bag all rely on.
 */
class BrewContainerArchitectureTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Brews.bootstrap();
    }

    @Test
    void contentsSerializerRoundTrips() {
        BrewContents original = new BrewContents(List.of(Items.SPIDER_EYE, Items.GUNPOWDER, Items.GLOWSTONE_DUST));
        Tag encoded = BrewContents.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        BrewContents decoded = BrewContents.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original.items(), decoded.items());
    }

    @Test
    void bottledDispersalReflectsModifiers() {
        assertSame(Brews.Dispersal.DRINK, Brews.bottledDispersal(List.of(Items.SPIDER_EYE)));
        assertSame(Brews.Dispersal.SPLASH, Brews.bottledDispersal(List.of(Items.SPIDER_EYE, Items.GUNPOWDER)));
        assertSame(Brews.Dispersal.GAS, Brews.bottledDispersal(List.of(Items.SPIDER_EYE, WitcheryItems.BAT_WOOL.get())));
        assertSame(Brews.Dispersal.LIQUID, Brews.bottledDispersal(List.of(Items.SPIDER_EYE, WitcheryItems.WORMWOOD.get())));
        assertSame(Brews.Dispersal.TRIGGERED, Brews.bottledDispersal(List.of(Items.SPIDER_EYE, Items.ZOMBIE_HEAD)));
        assertSame(Brews.Dispersal.TRIGGERED, Brews.bottledDispersal(List.of(Items.WRITABLE_BOOK, Items.ZOMBIE_HEAD)));
        assertTrue(Brews.isIngredient(Items.WRITABLE_BOOK));
    }

    @Test
    void dispersalSelectsTheRightContainer() {
        assertSame(WitcheryItems.BREW_BOTTLE.get(), BrewContainers.forDispersal(Brews.Dispersal.DRINK));
        assertSame(WitcheryItems.SPLASH_BREW_BOTTLE.get(), BrewContainers.forDispersal(Brews.Dispersal.SPLASH));
        assertSame(WitcheryItems.SPLASH_BREW_BOTTLE.get(), BrewContainers.forDispersal(Brews.Dispersal.TRIGGERED));
        assertSame(WitcheryItems.LINGERING_BREW_BOTTLE.get(), BrewContainers.forDispersal(Brews.Dispersal.GAS));
        assertSame(WitcheryItems.LIQUID_BREW_BOTTLE.get(), BrewContainers.forDispersal(Brews.Dispersal.LIQUID));
    }

    @Test
    void bottleStampsContentsAndPicksContainer() {
        BrewContents splash = new BrewContents(List.of(Items.SPIDER_EYE, Items.GUNPOWDER));
        ItemStack bottle = BrewContainers.bottle(splash);
        assertSame(WitcheryItems.SPLASH_BREW_BOTTLE.get(), bottle.getItem());
        assertEquals(splash, bottle.get(WitcheryDataComponents.BREW_CONTENTS.get()));

        ItemStack bucket = BrewContainers.bucket(splash);
        assertSame(WitcheryItems.BREW_BUCKET.get(), bucket.getItem());
        assertEquals(splash, bucket.get(WitcheryDataComponents.BREW_CONTENTS.get()));
    }

    @Test
    void warningBrewUsesTriggeredSplashContainer() {
        BrewContents warning = new BrewContents(List.of(Items.WRITABLE_BOOK, Items.ZOMBIE_HEAD));
        ItemStack bottle = BrewContainers.bottle(warning);

        assertSame(WitcheryItems.SPLASH_BREW_BOTTLE.get(), bottle.getItem());
        assertEquals(warning, bottle.get(WitcheryDataComponents.BREW_CONTENTS.get()));
    }
}
