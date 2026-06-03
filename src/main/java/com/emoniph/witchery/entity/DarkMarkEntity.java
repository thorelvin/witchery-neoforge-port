package com.emoniph.witchery.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dark Mark — the ominous skull-cloud conjured by the Morsmordre symbol spell. Port of
 * EntityDarkMark (1.7.10-0.24.1).
 *
 *  - A 2×2 floating, fire-immune, invulnerable, immovable mark that hangs in the air for 5 minutes.
 *  - Trails a churning cloud of large smoke and cackles periodically.
 *  - Wards the area: every few ticks it shoves nearby mobs (not players) outward within 10 blocks.
 */
public class DarkMarkEntity extends Mob {

    private static final int LIFETIME_TICKS = 6000; // 5 minutes
    private static final double WARD_RADIUS = 10.0;

    private int life = 0;

    public DarkMarkEntity(EntityType<? extends DarkMarkEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0F));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // The mark itself shoves nothing by collision; its ward push is handled in tick().
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // invulnerable
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // Hang in place.
        setDeltaMovement(getDeltaMovement().x * 0.8, 0.0, getDeltaMovement().z * 0.8);

        if (level() instanceof ServerLevel server) {
            if (++life > LIFETIME_TICKS) {
                discard();
                return;
            }
            if (life % 4 == 0) {
                wardArea(server);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE,
                        getX() - 1.4 + random.nextDouble() * 2.8,
                        getY() + random.nextDouble() * 2.0,
                        getZ() - 1.4 + random.nextDouble() * 2.8,
                        0.0, 0.0, 0.0);
            }
        }
    }

    /** Shove nearby mobs (never players) away from the mark. */
    private void wardArea(ServerLevel level) {
        AABB bounds = new AABB(getX() - WARD_RADIUS, level.getMinY(), getZ() - WARD_RADIUS,
                getX() + WARD_RADIUS, level.getMaxY(), getZ() + WARD_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, bounds, m -> m != this && m.isAlive());
        for (Mob mob : mobs) {
            double dx = mob.getX() - getX();
            double dz = mob.getZ() - getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz > 0.0 && horiz <= WARD_RADIUS) {
                Vec3 push = new Vec3(dx, 0.0, dz).normalize().scale(0.4);
                mob.push(push.x, 0.1, push.z);
                mob.hurtMarked = true;
            }
        }
    }

    // ── Sounds (a distant cackle) ────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_CELEBRATE; }
    @Override public int getAmbientSoundInterval() { return 80; }
    @Override protected float getSoundVolume() { return 0.8F; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return null; }
    @Override protected SoundEvent getDeathSound() { return null; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("WitcLife", life);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        life = in.getIntOr("WitcLife", 0);
    }
}
