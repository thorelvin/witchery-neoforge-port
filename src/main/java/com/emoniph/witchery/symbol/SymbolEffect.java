package com.emoniph.witchery.symbol;

import com.emoniph.witchery.data.InfusionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A single Mystic Branch symbol spell.
 *
 * Source parity: {@code com.emoniph.witchery.infusion.infusions.symbols.SymbolEffect}
 * (1.7.10-0.24.1). Instant spells override {@link #perform}; projectile spells extend
 * {@link SymbolEffectProjectile}, whose {@code perform} launches a {@link
 * com.emoniph.witchery.entity.SpellEffectEntity}.
 *
 * Port note: the original gated power above level 1 behind the Worship potion and gated
 * "unforgivable" curses behind the Beast infusion. The port has neither yet, so
 * {@code MysticBranchItem} clamps enhanced stroke sets back to level 1 and unforgivable
 * curses stay creative-only. {@link #hasValidInfusion} still requires any infusion, and
 * branch cooldowns are enforced by {@code MysticBranchItem} using the player's learned-symbol
 * attachment.
 */
public abstract class SymbolEffect {

    private final int effectId;
    private final String nameKey;
    private final int baseChargeCost;
    private final boolean curse;
    private final boolean fallsToEarth;
    private final String knowledgeKey;
    private final int cooldownTicks;
    private byte[] defaultStrokes = new byte[0];

    protected SymbolEffect(int effectId, String nameKey) {
        this(effectId, nameKey, 1, false, false, null, 0);
    }

    protected SymbolEffect(int effectId, String nameKey, int baseChargeCost,
                           boolean curse, boolean fallsToEarth, String knowledgeKey, int cooldownTicks) {
        this.effectId = effectId;
        this.nameKey = nameKey;
        this.baseChargeCost = baseChargeCost;
        this.curse = curse;
        this.fallsToEarth = fallsToEarth;
        this.knowledgeKey = knowledgeKey;
        this.cooldownTicks = cooldownTicks;
    }

    public int getEffectId() {
        return effectId;
    }

    /** Translation key for the spell's display name (e.g. {@code witchery.pott.incendio}). */
    public String getNameKey() {
        return nameKey;
    }

    public boolean isCurse() {
        return curse;
    }

    /** Curses with no learnable knowledge key are "unforgivable" (Beast-infusion only in source). */
    public boolean isUnforgivable() {
        return curse && knowledgeKey == null;
    }

    /** A spell learned from a Demon soul has a knowledge key; {@code null} means it needs no learning. */
    public String getKnowledgeKey() {
        return knowledgeKey;
    }

    public boolean fallsToEarth() {
        return fallsToEarth;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /** Source cost curve: {@code floor(2^(level-1) * baseCost)}. */
    public int getChargeCost(int level) {
        return (int) Math.floor(Math.pow(2.0, level - 1) * baseChargeCost);
    }

    public boolean hasValidInfusion(boolean creative, InfusionType infusion) {
        if (creative) {
            return true;
        }
        if (infusion == null || infusion == InfusionType.NONE) {
            return false;
        }
        // Port has no Beast infusion; unforgivable curses stay creative-only and are not
        // registered in this slice, so any infusion satisfies the registered spells.
        return !isUnforgivable();
    }

    void setDefaultStrokes(byte[] strokes) {
        this.defaultStrokes = strokes;
    }

    public byte[] getDefaultStrokes() {
        return defaultStrokes;
    }

    /** Run the spell. Instant spells implement directly; projectile spells launch an entity. */
    public abstract void perform(ServerLevel level, ServerPlayer caster, int effectLevel);
}
