package com.emoniph.witchery.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreakingFamiliarDataTest {

    private static final UUID OWNER = UUID.fromString("4c6eef0f-96e4-4b51-ad63-829e6e83e0f2");

    @Test
    void legacySittingDataMigratesToRootedGuardWithoutLosingIdentity() {
        String legacy = """
                {
                  "owner": "%s",
                  "sitting": true,
                  "custom_name": "Mossknell",
                  "name_visible": true
                }
                """.formatted(OWNER);

        CreakingFamiliarData decoded = CreakingFamiliarData.CODEC.codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString(legacy)).getOrThrow();

        assertEquals(CreakingFamiliarData.Mode.ROOTED_GUARD, decoded.mode());
        assertTrue(decoded.sitting());
        assertTrue(decoded.guardPosition().isEmpty(), "first server tick owns legacy anchor acquisition");
        assertEquals(OWNER, decoded.owner().orElseThrow());
        assertEquals("Mossknell", decoded.customName());
        assertTrue(decoded.nameVisible());
    }

    @Test
    void missingOptionalFieldsLoadAsUnboundFollowState() {
        CreakingFamiliarData decoded = CreakingFamiliarData.CODEC.codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{}"))
                .getOrThrow();

        assertFalse(decoded.isBound());
        assertEquals(CreakingFamiliarData.Mode.FOLLOW, decoded.mode());
        assertFalse(decoded.sitting());
        assertTrue(decoded.guardPosition().isEmpty());
    }

    @Test
    void rootedModeAnchorAndRateLimitsSurviveCodecRoundTrip() {
        BlockPos anchor = new BlockPos(120, 71, -44);
        CreakingFamiliarData original = CreakingFamiliarData.EMPTY.withOwner(OWNER)
                .rootedAt(Level.OVERWORLD, anchor, 14_000L)
                .withWatched(true)
                .withAlertState(8_000L, 3);

        var encoded = CreakingFamiliarData.CODEC.codec()
                .encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        CreakingFamiliarData decoded = CreakingFamiliarData.CODEC.codec()
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(CreakingFamiliarData.Mode.ROOTED_GUARD, decoded.mode());
        assertEquals(anchor, decoded.guardPosition().orElseThrow());
        assertTrue(decoded.hasGuardAnchor(Level.OVERWORLD));
        assertTrue(decoded.watched());
        assertEquals(8_000L, decoded.lastAlertGameTime());
        assertEquals(3, decoded.lastAlertCount());
        assertEquals(14_000L, decoded.nextResinGameTime());
    }

    @Test
    void returningToFollowClearsEveryGuardOnlyField() {
        CreakingFamiliarData following = CreakingFamiliarData.EMPTY.withOwner(OWNER)
                .rootedAt(Level.OVERWORLD, BlockPos.ZERO, 200L)
                .withAlertState(100L, 2)
                .following();

        assertEquals(CreakingFamiliarData.Mode.FOLLOW, following.mode());
        assertTrue(following.guardPosition().isEmpty());
        assertEquals("", following.guardDimension());
        assertEquals(0, following.lastAlertCount());
        assertEquals(CreakingFamiliarData.NO_SCHEDULED_TICK, following.nextResinGameTime());
    }
}
