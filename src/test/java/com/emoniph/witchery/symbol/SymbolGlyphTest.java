package com.emoniph.witchery.symbol;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Mystic Branch glyph → spell mapping against the source {@code StrokeSet} registrations
 * (1.7.10-0.24.1), keyed by the canonical level-1 stroke sequence. Stroke bytes: 0=up, 1=down,
 * 2=right, 3=left. This guards the level-vs-stroke transcription that broke Morsmordre/Nox/Lumos/
 * Confundus. Pure logic over {@link EffectRegistry}; runs headlessly.
 */
class SymbolGlyphTest {

    private static void assertGlyph(String nameKey, int level, int... strokes) {
        byte[] bytes = new byte[strokes.length];
        for (int i = 0; i < strokes.length; i++) {
            bytes[i] = (byte) strokes[i];
        }
        SymbolEffect effect = EffectRegistry.instance().getEffect(bytes);
        assertNotNull(effect, "glyph should resolve to a spell: " + java.util.Arrays.toString(strokes));
        assertEquals(nameKey, effect.getNameKey(), "wrong spell for glyph " + java.util.Arrays.toString(strokes));
        assertEquals(level, EffectRegistry.instance().getLevel(bytes), "wrong level for glyph");
    }

    @Test
    void knownGlyphsResolveToTheCorrectSpell() {
        EffectRegistry.bootstrap();
        // Level-1 sequences, transcribed from the source RiteRegistry/EffectRegistry.
        assertGlyph("witchery.pott.accio",        1, 3, 0, 2, 2, 1);        // L U R R D
        assertGlyph("witchery.pott.confundus",    1, 3, 3, 0, 0, 2);        // L L U U R
        assertGlyph("witchery.pott.episkey",      1, 2, 0, 3, 1, 1, 2);     // R U L D D R
        assertGlyph("witchery.pott.expelliarmus", 1, 0, 0, 1);              // U U D
        assertGlyph("witchery.pott.flipendo",     1, 2, 2, 3);              // R R L
        assertGlyph("witchery.pott.impedimenta",  1, 3, 3, 2);              // L L R
        assertGlyph("witchery.pott.incendio",     1, 3, 0, 0, 1, 1);        // L U U D D
        assertGlyph("witchery.pott.lumos",        1, 1, 1, 1, 2);           // D D D R
        assertGlyph("witchery.pott.nox",          1, 0, 0, 2, 1, 2, 0);     // U U R D R U
        assertGlyph("witchery.pott.stupefy",      1, 2, 2, 0, 3, 0, 2);     // R R U L U R
        assertGlyph("witchery.pott.morsmordre",   1, 0, 0, 3, 2, 2);        // U U L R R
        // Phase A utility spells.
        assertGlyph("witchery.pott.aguamenti",    1, 0, 0, 2, 2, 1);        // U U R R D
        assertGlyph("witchery.pott.alohomora",    1, 2, 0, 2, 2, 1);        // R U R R D
        assertGlyph("witchery.pott.defodio",      1, 0, 0, 3, 1);           // U U L D
        assertGlyph("witchery.pott.ennervate",    1, 0, 3, 0, 2, 3, 0, 2);  // U L U R L U R
        assertGlyph("witchery.pott.attraho",      1, 0, 0, 0, 2, 2, 1, 3);  // U U U R R D L
        assertGlyph("witchery.pott.caveinimicum", 1, 0, 3, 0, 0, 2);        // U L U U R
        assertGlyph("witchery.pott.meteolojinxrecanto", 1, 0, 0, 0, 2, 2, 1, 0, 2, 2, 1, 1);
        // Phase B demon-soul spells.
        assertGlyph("witchery.pott.ignianima",   1, 3, 3, 0, 1, 1);        // L L U D D
        assertGlyph("witchery.pott.carnosadiem", 1, 2, 2, 0, 1, 1);        // R R U D D
        assertGlyph("witchery.pott.tormentum",   1, 1, 1, 3, 2, 2);        // D D L R R
        // Phase C unforgivable curses.
        assertGlyph("witchery.pott.crucio",      1, 1, 3, 1, 1, 2);        // D L D D R
        assertGlyph("witchery.pott.imperio",     1, 2, 1, 1, 1, 1);        // R D D D D
        assertGlyph("witchery.pott.avadakedavra", 1, 1, 1, 2, 2, 0, 0, 3, 3, 3, 3, 1, 1, 2);
        assertGlyph("witchery.pott.protego",     1, 1, 1, 0);                 // D D U
        // Higher-level variants (bigger effect).
        assertGlyph("witchery.pott.aguamenti",    2, 0, 0, 0, 2, 2, 1, 1);
        assertGlyph("witchery.pott.defodio",      3, 0, 0, 0, 0, 3, 3, 3, 1, 1, 1, 2, 2);
        assertGlyph("witchery.pott.ennervate",    3, 0, 3, 3, 3, 0, 2, 2, 2, 3, 3, 3, 0, 2, 2, 2);
        assertGlyph("witchery.pott.caveinimicum", 2, 0, 3, 3, 0, 0, 0, 2, 2);
        // System-blocked spells, now live.
        assertGlyph("witchery.pott.flagrate",    1, 2, 0, 2, 3, 0, 2);     // R U R L U R
        assertGlyph("witchery.pott.colloportus", 1, 3, 3, 1, 1, 2);        // L L D D R
        assertGlyph("witchery.pott.caelum",      1, 2, 0, 3, 3, 1);        // R U L L D
        assertGlyph("witchery.pott.baratrum",    1, 3, 1, 2, 2, 0);        // L D R R U
        assertGlyph("witchery.pott.ortus",       1, 1, 2, 0, 0, 3);        // D R U U L
        assertGlyph("witchery.pott.occasus",     1, 0, 3, 1, 1, 2);        // U L D D R
    }

    @Test
    void unknownGlyphResolvesToNothing() {
        EffectRegistry.bootstrap();
        assertNull(EffectRegistry.instance().getEffect(new byte[]{0}), "a single up is not a spell");
        assertNull(EffectRegistry.instance().getEffect(new byte[]{0, 1}), "up-down is not a spell");
    }

    @Test
    void registryOnlyAcceptsLevelsBackedByRegisteredStrokeSets() {
        EffectRegistry.bootstrap();
        assertTrue(EffectRegistry.instance().isCastableLevel(EffectRegistry.ACCIO.getEffectId(), 1));
        assertFalse(EffectRegistry.instance().isCastableLevel(EffectRegistry.ACCIO.getEffectId(), 99));
        assertFalse(EffectRegistry.instance().isCastableLevel(Integer.MAX_VALUE, 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void registeredGlyphsNeverHideLongerGlyphsBehindAValidPrefix() throws ReflectiveOperationException {
        EffectRegistry.bootstrap();
        Field field = EffectRegistry.class.getDeclaredField("byStroke");
        field.setAccessible(true);
        Map<String, SymbolEffect> glyphs = (Map<String, SymbolEffect>) field.get(EffectRegistry.instance());

        for (String shorter : glyphs.keySet()) {
            for (String longer : glyphs.keySet()) {
                if (!shorter.equals(longer)) {
                    assertFalse(longer.startsWith(shorter),
                            () -> "client locks glyph " + shorter + " before longer glyph " + longer + " can finish");
                }
            }
        }
    }

    @Test
    void sourceBytecodeGlyphVariantsResolveToTheCorrectSpell() {
        EffectRegistry.bootstrap();
        assertGlyph("witchery.pott.aguamenti", 1, 0, 0, 2, 2, 2, 1);
        assertGlyph("witchery.pott.aguamenti", 2, 0, 0, 0, 2, 2, 2, 1, 1);
        assertGlyph("witchery.pott.aguamenti", 3, 0, 0, 0, 0, 2, 2, 2, 1, 1, 1);
        assertGlyph("witchery.pott.alohomora", 1, 2, 0, 2, 2, 2, 1);
        assertGlyph("witchery.pott.alohomora", 1, 2, 0, 0, 2, 2, 1, 1);
        assertGlyph("witchery.pott.alohomora", 1, 2, 0, 0, 2, 2, 2, 1, 1);
        assertGlyph("witchery.pott.defodio", 1, 0, 0, 0, 3, 1, 1);
        assertGlyph("witchery.pott.defodio", 1, 0, 0, 3, 3, 1, 2);
        assertGlyph("witchery.pott.defodio", 2, 0, 0, 0, 0, 3, 3, 1, 1, 1, 2);
        assertGlyph("witchery.pott.defodio", 2, 0, 0, 0, 3, 3, 3, 1, 1, 2, 2);
        assertGlyph("witchery.pott.caveinimicum", 1, 0, 3, 0, 0, 0, 2);
        assertGlyph("witchery.pott.caveinimicum", 1, 0, 3, 3, 0, 0, 2, 2);
        assertGlyph("witchery.pott.ignianima", 1, 3, 3, 0, 0, 1, 1, 1, 1);
        assertGlyph("witchery.pott.ignianima", 1, 3, 3, 3, 0, 1, 1);
        assertGlyph("witchery.pott.ignianima", 1, 3, 3, 3, 3, 0, 0, 1, 1, 1, 1);
        assertGlyph("witchery.pott.carnosadiem", 1, 2, 2, 0, 0, 1, 1, 1, 1);
        assertGlyph("witchery.pott.carnosadiem", 1, 2, 2, 2, 0, 1, 1);
        assertGlyph("witchery.pott.carnosadiem", 1, 2, 2, 2, 2, 0, 0, 1, 1, 1, 1);
        assertGlyph("witchery.pott.tormentum", 1, 1, 1, 3, 3, 2, 2, 2, 2);
        assertGlyph("witchery.pott.tormentum", 1, 1, 1, 1, 3, 2, 2);
        assertGlyph("witchery.pott.tormentum", 1, 1, 1, 1, 1, 3, 3, 2, 2, 2, 2);
        assertGlyph("witchery.pott.colloportus", 1, 3, 3, 1, 1, 1, 2);
        assertGlyph("witchery.pott.colloportus", 1, 3, 3, 3, 1, 1, 2, 2);
        assertGlyph("witchery.pott.colloportus", 1, 3, 3, 3, 1, 1, 2, 1, 2);
        assertGlyph("witchery.pott.protego", 1, 1, 1, 1, 0, 0);
        assertGlyph("witchery.pott.protego", 1, 1, 1, 1, 1, 0, 0, 0);

        assertNull(EffectRegistry.instance().getEffect(new byte[]{1, 1, 0, 0}),
                "shifted old port Protego glyph should not shadow the source short form");
        assertNull(EffectRegistry.instance().getEffect(new byte[]{0, 2, 3, 0, 2}),
                "shifted old port Flagrate glyph should not shadow the source bytecode form");
    }

    @Test
    void tormentumProtectsBossTypesButNotOrdinaryMobs() {
        assertFalse(EffectRegistry.isTormentumProtectedType(EntityType.ZOMBIE));
        assertFalse(EffectRegistry.isTormentumProtectedType(EntityType.WITCH));
        assertTrue(EffectRegistry.isTormentumProtectedType(EntityType.WITHER));
        assertTrue(EffectRegistry.isTormentumProtectedType(EntityType.ENDER_DRAGON));
    }

    @Test
    void aguamentiWaterShapeMatchesSourceLevels() {
        assertEquals(java.util.List.of(BlockPos.ZERO), EffectRegistry.aguamentiWaterOffsets(1, false));
        assertEquals(7, EffectRegistry.aguamentiWaterOffsets(2, false).size());
        assertTrue(EffectRegistry.aguamentiWaterOffsets(2, false).contains(BlockPos.ZERO.above()));
        assertTrue(EffectRegistry.aguamentiWaterOffsets(3, false).contains(BlockPos.ZERO.south()));

        assertEquals(java.util.List.of(), EffectRegistry.aguamentiWaterOffsets(1, true));
        assertEquals(java.util.List.of(), EffectRegistry.aguamentiWaterOffsets(2, true));
        assertEquals(java.util.List.of(BlockPos.ZERO), EffectRegistry.aguamentiWaterOffsets(3, true));
    }

    @Test
    void protegoBarrierShapeMatchesSourceShieldColumns() {
        BlockPos hit = new BlockPos(10, 64, -3);
        java.util.List<BlockPos> solidHit = EffectRegistry.protegoShieldPositions(hit, Direction.NORTH, true);
        assertEquals(9, solidHit.size());
        assertTrue(solidHit.contains(hit.above()));
        assertTrue(solidHit.contains(hit.above(3)));
        assertTrue(solidHit.contains(hit.above().east()));
        assertTrue(solidHit.contains(hit.above(3).west()));
        assertFalse(solidHit.contains(hit), "solid block hits should build the shield above the struck block");

        java.util.List<BlockPos> nonSolidHit = EffectRegistry.protegoShieldPositions(hit, Direction.NORTH, false);
        assertTrue(nonSolidHit.contains(hit));
        assertTrue(nonSolidHit.contains(hit.above(2).east()));
    }

    @Test
    void flagrateUsesSourceTopFaceGlyphPlacement() {
        BlockPos hit = new BlockPos(4, 70, 8);
        BlockHitResult topHit = new BlockHitResult(Vec3.ZERO, Direction.UP, hit, false);
        BlockHitResult sideHit = new BlockHitResult(Vec3.ZERO, Direction.NORTH, hit, false);

        assertEquals(hit.above(), EffectRegistry.flagrateGlyphPosition(topHit, false));
        assertNull(EffectRegistry.flagrateGlyphPosition(sideHit, false),
                "new Flagrate glyphs should only draw from top-face hits like source ItemChalk");
        assertEquals(hit, EffectRegistry.flagrateGlyphPosition(sideHit, true),
                "existing glyphs can be converted or rerolled at their own position");
    }
}
