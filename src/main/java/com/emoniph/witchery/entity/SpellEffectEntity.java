package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.MysticBranchItem;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import com.emoniph.witchery.symbol.SymbolEffectProjectile;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The flying orb launched by a {@link SymbolEffectProjectile} Mystic Branch spell. It resolves
 * the spell on impact via {@link SymbolEffectProjectile#onCollision}.
 *
 * Source parity: {@code com.emoniph.witchery.entity.EntitySpellEffect} (1.7.10-0.24.1),
 * simplified to a {@link ThrowableProjectile} with a no-op renderer plus a client particle trail.
 */
public class SpellEffectEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(SpellEffectEntity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_COLOR = 0xB388E8;
    static final int CAVE_INIMICUM_EFFECT_ID = 5;
    private static final double DEFLECT_SPEED = 1.2D;
    private static final int OWNER_COLLISION_GRACE_TICKS = 25;

    private int effectId = -1;
    private int effectLevel = 1;
    private int lifeTime = 80;

    public SpellEffectEntity(EntityType<? extends SpellEffectEntity> type, Level level) {
        super(type, level);
    }

    public SpellEffectEntity(Level level, LivingEntity owner, SymbolEffect effect, int effectLevel) {
        super(WitcheryEntities.SPELL_EFFECT.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
        this.effectId = effect.getEffectId();
        this.effectLevel = effectLevel;
        if (effect instanceof SymbolEffectProjectile projectile) {
            if (projectile.getTimeToLive() > 0) {
                this.lifeTime = projectile.getTimeToLive();
            }
            // colour 0 (e.g. Morsmordre's black) reads as invisible — fall back to the default tint.
            setTrailColorFromEffect();
        }
        setNoGravity(!effect.fallsToEarth());
    }

    public int getEffectLevel() {
        return effectLevel;
    }

    private int trailColor() {
        return entityData.get(DATA_COLOR);
    }

    static boolean isDeflectableEffect(int effectId) {
        return effectId != CAVE_INIMICUM_EFFECT_ID;
    }

    static Vec3 deflectedVelocity(Vec3 look) {
        if (look == null || look.lengthSqr() < 1.0E-7D) {
            return Vec3.ZERO;
        }
        return look.normalize().scale(DEFLECT_SPEED);
    }

    static boolean canHitOwnerAtAge(int ticksInAir) {
        return ticksInAir >= OWNER_COLLISION_GRACE_TICKS;
    }

    static int colorForEffect(int effectId) {
        SymbolEffect effect = EffectRegistry.instance().getById(effectId);
        if (effect instanceof SymbolEffectProjectile projectile) {
            // Color 0, such as Morsmordre's black, reads as invisible in dust particles.
            int color = projectile.getColor();
            return color == 0 ? DEFAULT_COLOR : (color & 0xFFFFFF);
        }
        return DEFAULT_COLOR;
    }

    private void setTrailColorFromEffect() {
        entityData.set(DATA_COLOR, colorForEffect(effectId));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, DEFAULT_COLOR);
    }

    @Override
    public boolean isPickable() {
        return true; // so a player can swat it out of the air to deflect it
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (target == getOwner() && !canHitOwnerAtAge(tickCount)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    /**
     * Deflect — a player who swings the Mystic Branch at the flying orb sends it back along their
     * own look direction, and it becomes theirs (it will now resolve on whatever they aim it at).
     * Source parity: {@code EntitySpellEffect.attackEntityFrom} (1.7.10-0.24.1). Cave Inimicum
     * (effect 5) cannot be deflected.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isDeflectableEffect(effectId) && source.getEntity() instanceof Player player
                && player.getMainHandItem().getItem() instanceof MysticBranchItem) {
            setDeltaMovement(deflectedVelocity(player.getLookAngle()));
            setOwner(player);
            level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.7F, 0.8F);
            return true;
        }
        return false;
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        SymbolEffect effect = EffectRegistry.instance().getById(effectId);
        LivingEntity caster = getOwner() instanceof LivingEntity living ? living : null;
        if (effect instanceof SymbolEffectProjectile projectile) {
            try {
                projectile.onCollision(serverLevel, caster, result, this);
            } catch (RuntimeException ex) {
                // A misbehaving spell effect must never crash the projectile tick.
            }
        }
        serverLevel.sendParticles(new DustParticleOptions(trailColor(), 1.6F),
                getX(), getY(), getZ(), 18, 0.25, 0.25, 0.25, 0.0);
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            DustParticleOptions dust = new DustParticleOptions(trailColor(), 1.3F);
            level().addParticle(dust, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
            if (level().random.nextInt(2) == 0) {
                level().addParticle(dust, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (tickCount > lifeTime && !isRemoved()) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EffectId", effectId);
        tag.putInt("EffectLevel", effectLevel);
        tag.putInt("LifeTime", lifeTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        effectId = tag.getIntOr("EffectId", -1);
        effectLevel = tag.getIntOr("EffectLevel", 1);
        lifeTime = tag.getIntOr("LifeTime", 80);
        setTrailColorFromEffect();
    }
}
