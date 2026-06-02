package com.emoniph.witchery.symbol;

/**
 * One recognised air-glyph stroke sequence and the spell power level it casts at.
 *
 * Source parity: {@code com.emoniph.witchery.infusion.infusions.symbols.StrokeSet}
 * (1.7.10-0.24.1). Stroke bytes are generated identically to {@code ItemMysticBranch}:
 * {@code 0 = pitch-up, 1 = pitch-down, 2 = yaw (>=7 toward -), 3 = yaw (>=7 toward +)}.
 * The first constructor argument is the spell level; the rest are the stroke bytes.
 */
public final class StrokeSet {

    private final byte[] strokes;
    private final int level;

    public StrokeSet(int level, int... strokes) {
        this.level = level;
        this.strokes = new byte[strokes.length];
        for (int i = 0; i < strokes.length; i++) {
            this.strokes[i] = (byte) strokes[i];
        }
    }

    public byte[] strokes() {
        return strokes;
    }

    public int level() {
        return level;
    }
}
