package com.emoniph.witchery.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Spectral Witch — a translucent, conjured member of the Gathered Lives coven. Has no AI: it floats
 * in a slow orbit around the altar it was bound to (its anchor) and never moves its feet. Counts as a
 * {@link CovenMember} so a solo caster can meet the coven requirement of higher rites while the
 * spectral coven persists. Fades away on its own after {@link #DEFAULT_LIFETIME} ticks.
 */
public class SpectralWitchEntity extends Mob implements CovenMember {

    private static final String TAG_ANCHOR = "Anchor";
    private static final String TAG_PHASE = "Phase";
    private static final String TAG_LIFE = "Life";

    /** ~5 minutes — long enough to chain a coven-gated rite, short enough to feel summoned. */
    public static final int DEFAULT_LIFETIME = 6000;

    private static final double ORBIT_RADIUS = 2.4;
    private static final float ORBIT_SPEED = 0.035F;   // radians/tick (~9s per lap)
    private static final double BOB_AMPLITUDE = 0.25;
    private static final double BASE_HEIGHT = 1.1;      // above the anchor block

    /** Skin index 0-4 → covenwitch1-5.png, mirrored from {@link CovenWitchEntity}. */
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(SpectralWitchEntity.class, EntityDataSerializers.INT);

    private BlockPos anchor = BlockPos.ZERO;
    private float phase;          // per-witch angular offset around the ring
    private int remainingLife = DEFAULT_LIFETIME;

    public SpectralWitchEntity(EntityType<? extends SpectralWitchEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSkin(int skin) {
        this.entityData.set(SKIN, Math.floorMod(skin, 5));
    }

    /** Binds this witch to orbit {@code anchor}; {@code index} fans the coven evenly around the ring. */
    public void bindToAltar(BlockPos anchor, int index, int total) {
        this.anchor = anchor.immutable();
        this.phase = (float) (2.0 * Math.PI * index / Math.max(1, total));
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            // The anchor lives only on the server, so the orbit is server-authoritative and the client
            // simply interpolates the synced positions. Here we only add the drifting witch-mote ambience.
            if (this.random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.WITCH,
                        getX() + (this.random.nextDouble() - 0.5) * 0.4,
                        getY() + this.random.nextDouble() * 1.6,
                        getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                        0.0, 0.01, 0.0);
            }
            return;
        }

        if (--this.remainingLife <= 0) {
            fadeAway();
            return;
        }

        // Hover in a slow ring around the bound altar; the entity tracker syncs the motion to clients.
        float angle = this.phase + this.tickCount * ORBIT_SPEED;
        double cx = this.anchor.getX() + 0.5;
        double cz = this.anchor.getZ() + 0.5;
        double x = cx + Math.cos(angle) * ORBIT_RADIUS;
        double z = cz + Math.sin(angle) * ORBIT_RADIUS;
        double y = this.anchor.getY() + BASE_HEIGHT + Math.sin(this.tickCount * 0.08) * BOB_AMPLITUDE;

        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(x, y, z);

        // Face the altar at the centre of the ring.
        float facing = (float) (Math.toDegrees(Math.atan2(cz - z, cx - x)) - 90.0);
        this.setYRot(facing);
        this.yBodyRot = facing;
        this.yHeadRot = facing;
    }

    private void fadeAway() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WITCH,
                    getX(), getY() + 1.0, getZ(), 24, 0.3, 0.6, 0.3, 0.02);
        }
        level().playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.7F);
        discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // A ghost does not jostle the living.
    }

    @Override
    public void push(double x, double y, double z) {
        // Immovable — orbit is fixed.
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_ANCHOR + "X", this.anchor.getX());
        tag.putInt(TAG_ANCHOR + "Y", this.anchor.getY());
        tag.putInt(TAG_ANCHOR + "Z", this.anchor.getZ());
        tag.putFloat(TAG_PHASE, this.phase);
        tag.putInt(TAG_LIFE, this.remainingLife);
        tag.putInt("Skin", getSkin());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.anchor = new BlockPos(
                tag.getIntOr(TAG_ANCHOR + "X", 0),
                tag.getIntOr(TAG_ANCHOR + "Y", 0),
                tag.getIntOr(TAG_ANCHOR + "Z", 0));
        this.phase = tag.getFloatOr(TAG_PHASE, 0.0F);
        this.remainingLife = tag.getIntOr(TAG_LIFE, DEFAULT_LIFETIME);
        setSkin(tag.getIntOr("Skin", 0));
    }
}
