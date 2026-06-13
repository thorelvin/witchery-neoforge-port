package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class ScryingEyeEntity extends Mob {
    private static final int DEFAULT_LIFETIME = 1200;
    private static final double RISE_SPEED = 0.05D;

    private int remainingLife = DEFAULT_LIFETIME;

    public ScryingEyeEntity(EntityType<? extends ScryingEyeEntity> type, Level level) {
        super(type, level);
        setInvisible(true);
        setInvulnerable(true);
        setNoGravity(true);
        setNoAi(true);
        noPhysics = true;
    }

    public static ScryingEyeEntity spawn(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        ScryingEyeEntity eye = WitcheryEntities.SCRYING_EYE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (eye == null) {
            return null;
        }
        eye.setPos(x, y, z);
        eye.setYRot(yaw);
        eye.setYHeadRot(yaw);
        eye.setXRot(pitch);
        level.addFreshEntity(eye);
        return eye;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void refreshLife() {
        remainingLife = DEFAULT_LIFETIME;
    }

    @Override
    protected void registerGoals() {
        // No AI; the eye is only a temporary camera anchor.
    }

    @Override
    public void tick() {
        super.tick();
        setInvisible(true);
        setDeltaMovement(new Vec3(0.0D, RISE_SPEED, 0.0D));
        setPos(getX(), getY() + RISE_SPEED, getZ());
        if (!level().isClientSide() && --remainingLife <= 0) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // A viewpoint should never shove or be shoved.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", remainingLife);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        remainingLife = tag.getIntOr("Life", DEFAULT_LIFETIME);
    }
}
