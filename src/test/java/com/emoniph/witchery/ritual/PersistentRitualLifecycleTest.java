package com.emoniph.witchery.ritual;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentRitualLifecycleTest {
    private static final String FIXTURE = "/witchery/ritual/source_ritual_registrations.json";
    private static final Pattern EFFECT_CLASS = Pattern.compile("new ([A-Za-z0-9_]+)");

    @Test
    void lifecycleOwnsEveryPersistentSourceRegistrationAndQueueType() {
        Set<Integer> actualIds = new HashSet<>();
        Set<String> actualEffectClasses = new HashSet<>();
        for (var element : fixture().getAsJsonArray("registrations")) {
            JsonObject row = element.getAsJsonObject();
            int sourceId = row.get("sourceId").getAsInt();
            if (!PersistentRitualLifecycle.SOURCE_REGISTRATION_IDS.contains(sourceId)) {
                continue;
            }
            actualIds.add(sourceId);
            Matcher matcher = EFFECT_CLASS.matcher(row.get("effect").getAsString());
            assertTrue(matcher.find(), "unparsed persistent source effect " + sourceId);
            actualEffectClasses.add(matcher.group(1));
        }

        Set<String> expectedEffectClasses = new HashSet<>();
        for (ActiveRitualType type : PersistentRitualLifecycle.types()) {
            expectedEffectClasses.add(PersistentRitualLifecycle.spec(type).sourceEffect());
        }
        assertEquals(PersistentRitualLifecycle.SOURCE_REGISTRATION_IDS, actualIds);
        assertEquals(expectedEffectClasses, actualEffectClasses);
        assertEquals(EnumSet.allOf(ActiveRitualType.class), PersistentRitualLifecycle.types());
        assertEquals(33, actualIds.size());
        assertEquals(21, PersistentRitualLifecycle.types().size());
    }

    @Test
    void sourceBoundariesAndPulseCadencesStayExact() {
        assertEquals(60L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.WEATHER_CALL_STORM));
        assertEquals(60L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.FERTILITY));
        assertEquals(50L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.TRANSPOSE_ORES));
        assertEquals(60L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.MOVING_EARTH));
        assertEquals(45L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.RAISE_COLUMN));
        assertEquals(45L, PersistentRitualLifecycle.nextActivationTime(41L,
                ActiveRitualType.VOLCANO));
        assertEquals(60L, PersistentRitualLifecycle.nextActivationTime(40L,
                ActiveRitualType.FERTILITY), "an activation always waits for the next boundary");

        for (ActiveRitualType type : PersistentRitualLifecycle.types()) {
            int pulse = PersistentRitualLifecycle.spec(type).pulseInterval();
            PersistentRitualLifecycle.requirePulseInterval(type, pulse);
            assertThrows(IllegalArgumentException.class,
                    () -> PersistentRitualLifecycle.requirePulseInterval(type, pulse + 1));
        }
    }

    @Test
    void protectionUpkeepAndWorldCadenceRemainSeparate() {
        assertEquals(1, PersistentRitualLifecycle.spec(
                ActiveRitualType.PROTECTION_BARRIER).pulseInterval());
        assertEquals(20, PersistentRitualLifecycle.spec(
                ActiveRitualType.PROTECTION_BARRIER).worldEffectInterval());
        assertFalse(PersistentRitualLifecycle.shouldApplyWorldEffect(
                ActiveRitualType.PROTECTION_BARRIER, 39L));
        assertTrue(PersistentRitualLifecycle.shouldApplyWorldEffect(
                ActiveRitualType.PROTECTION_BARRIER, 40L));
        assertTrue(PersistentRitualLifecycle.shouldApplyWorldEffect(
                ActiveRitualType.SANCTITY_CIRCLE, 39L));
    }

    @Test
    void brokenEarthHeadingComesFromTheFirstSacrifice() {
        BlockPos center = new BlockPos(10, 64, 10);
        assertEquals(0, RitualExecutor.headingFromSacrifice(center, center.north()));
        assertEquals(1, RitualExecutor.headingFromSacrifice(center, center.north().east()));
        assertEquals(2, RitualExecutor.headingFromSacrifice(center, center.east()));
        assertEquals(3, RitualExecutor.headingFromSacrifice(center, center.south().east()));
        assertEquals(4, RitualExecutor.headingFromSacrifice(center, center.south()));
        assertEquals(5, RitualExecutor.headingFromSacrifice(center, center.south().west()));
        assertEquals(6, RitualExecutor.headingFromSacrifice(center, center.west()));
        assertEquals(7, RitualExecutor.headingFromSacrifice(center, center.north().west()));
    }

    private static JsonObject fixture() {
        var stream = PersistentRitualLifecycleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull(stream, "missing source ritual fixture");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Unable to read source ritual fixture", exception);
        }
    }
}
