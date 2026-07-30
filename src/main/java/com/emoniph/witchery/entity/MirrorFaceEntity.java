package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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

/**
 * The short-lived face that appears in a normal mirror during its divination response.
 * Source parity: {@code EntityMirrorFace} from Witchery 1.7.10.
 */
public class MirrorFaceEntity extends Mob {
    static final int SOURCE_LIFETIME_TICKS = 200;
    private static final String SOURCE_AGE_TAG = "WITCTicksAlive";

    private long ticksAlive;

    public MirrorFaceEntity(EntityType<? extends MirrorFaceEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public static boolean show(ServerLevel level, BlockPos mirrorFootPos, Direction facing) {
        Vec3 position = sourcePosition(mirrorFootPos, facing);
        AABB existingFaceBounds = AABB.ofSize(position, 1.25D, 2.25D, 1.25D);
        if (!level.getEntitiesOfClass(MirrorFaceEntity.class, existingFaceBounds).isEmpty()) {
            return false;
        }

        MirrorFaceEntity face = WitcheryEntities.MIRROR_FACE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (face == null) {
            return false;
        }
        face.snapTo(position.x, position.y, position.z, facing.toYRot(), 0.0F);
        face.setYHeadRot(facing.toYRot());
        return level.addFreshEntity(face);
    }

    static Vec3 sourcePosition(BlockPos mirrorFootPos, Direction facing) {
        return new Vec3(
                mirrorFootPos.getX() + 0.5D + facing.getStepX() * 0.4D,
                mirrorFootPos.getY() + 0.1D,
                mirrorFootPos.getZ() + 0.5D + facing.getStepZ() * 0.4D);
    }

    static boolean sourceShouldDiscard(long age) {
        return age > SOURCE_LIFETIME_TICKS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0F, 0.4F));
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        ticksAlive = Math.max(tickCount, ticksAlive + 1L);
        if (!level().isClientSide() && sourceShouldDiscard(ticksAlive)) {
            discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Source Mirror Faces never collide with transported entities.
    }

    @Override
    protected void pushEntities() {
        // Source Mirror Faces never jostle nearby entities.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 80;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong(SOURCE_AGE_TAG, ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ticksAlive = input.getLongOr(SOURCE_AGE_TAG, 0L);
    }
}
