package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitchCropMechanicsParityTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void wormwoodExtensionUsesTheExactSourceGates() {
        assertTrue(WormwoodBlock.sourceCanExtend(true, false, true, 9));
        assertFalse(WormwoodBlock.sourceCanExtend(false, false, true, 9));
        assertFalse(WormwoodBlock.sourceCanExtend(true, true, true, 9));
        assertFalse(WormwoodBlock.sourceCanExtend(true, false, false, 9));
        assertFalse(WormwoodBlock.sourceCanExtend(true, false, true, 8));
    }

    @Test
    void mindrakeUsesTheSourceGrowthAndBonemealFactors() {
        assertEquals(4.0F, MindrakeBlock.sourceGrowthSpeed(6.0F), 0.0001F);
        assertEquals(1, WitcheryBlocks.MINDRAKE.get().getBonemealAgeIncrease(null));
        assertEquals(1, WitcheryBlocks.WOLFSBANE_CROP.get().getBonemealAgeIncrease(null));
    }

    @Test
    void matureMindrakeExtraBulbUsesAnExactQuarterChance() throws IOException {
        Path lootTable = projectRoot().resolve(Path.of("src", "main", "resources", "data", "witchery",
                "loot_table", "blocks", "mindrake.json"));
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(lootTable)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonObject extraPool = root.getAsJsonArray("pools").get(1).getAsJsonObject();
        assertEquals(1, extraPool.get("rolls").getAsInt());
        JsonArray conditions = extraPool.getAsJsonArray("conditions");
        JsonObject chance = conditions.get(1).getAsJsonObject();
        assertEquals("minecraft:random_chance", chance.get("condition").getAsString());
        assertEquals(0.25D, chance.get("chance").getAsDouble(), 0.00001D);
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources", "data", "witchery")))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
