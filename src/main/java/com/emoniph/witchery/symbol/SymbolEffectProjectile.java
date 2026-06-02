package com.emoniph.witchery.symbol;

import com.emoniph.witchery.entity.SpellEffectEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A symbol spell that launches a flying {@link SpellEffectEntity} which resolves on impact.
 *
 * Source parity: {@code SymbolEffectProjectile} (1.7.10-0.24.1) — the branch fires a glowing
 * orb along the player's look vector; {@link #onCollision} runs server-side when it hits.
 */
public abstract class SymbolEffectProjectile extends SymbolEffect {

    private int color = 0xFF0000;
    private float size = 1.0F;
    private int timeToLive = -1;

    protected SymbolEffectProjectile(int effectId, String nameKey) {
        super(effectId, nameKey);
    }

    protected SymbolEffectProjectile(int effectId, String nameKey, int baseChargeCost,
                                     boolean curse, boolean fallsToEarth, String knowledgeKey, int cooldownTicks) {
        super(effectId, nameKey, baseChargeCost, curse, fallsToEarth, knowledgeKey, cooldownTicks);
    }

    public SymbolEffectProjectile setColor(int color) {
        this.color = color;
        return this;
    }

    public SymbolEffectProjectile setSize(float size) {
        this.size = size;
        return this;
    }

    public SymbolEffectProjectile setTimeToLive(int ticks) {
        this.timeToLive = ticks;
        return this;
    }

    public int getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    @Override
    public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.6F, 1.3F + level.random.nextFloat() * 0.2F);

        SpellEffectEntity spell = new SpellEffectEntity(level, caster, this, effectLevel);
        Vec3 look = caster.getLookAngle();
        spell.setPos(caster.getX() + look.x, caster.getEyeY() - 0.1 + look.y, caster.getZ() + look.z);
        spell.setNoGravity(!fallsToEarth());
        spell.shoot(look.x, look.y, look.z, 1.2F, 0.0F);
        level.addFreshEntity(spell);
    }

    /** Resolve the spell where it struck. Always runs on a {@link ServerLevel}. */
    public abstract void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell);
}
