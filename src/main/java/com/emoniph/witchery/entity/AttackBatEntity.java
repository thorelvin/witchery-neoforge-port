package com.emoniph.witchery.entity;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Source-style attack bat ({@code EntityAttackBat}): a short-lived bat bound to a player that rushes
 * the living entity the owner is looking at, hits once for four damage, then vanishes.
 */
public class AttackBatEntity extends Bat {
    private static final int MAX_AGE_TICKS = 300;
    private static final double OWNER_LOOK_RANGE = 32.0D;
    private static final double CONTACT_INFLATE = 0.2D;
    private static final double HOMING_ACCELERATION = 0.1D;
    private static final float CONTACT_DAMAGE = 4.0F;

    @Nullable
    private UUID ownerUuid;

    public AttackBatEntity(EntityType<? extends Bat> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            setResting(false);
            getPersistentData().putBoolean(Brews.SUMMONED_NO_DROPS_TAG, true);
            getPersistentData().putBoolean("WITCNoDrops", true);
        }
    }

    public static AttackBatEntity create(ServerLevel level, Player owner, double x, double y, double z) {
        AttackBatEntity bat = com.emoniph.witchery.init.WitcheryEntities.ATTACK_BAT.get()
                .create(level, EntitySpawnReason.MOB_SUMMONED);
        if (bat != null) {
            bat.setOwner(owner);
            bat.snapTo(x, y, z, owner.getYRot(), 0.0F);
            bat.setResting(false);
        }
        return bat;
    }

    public void setOwner(Player owner) {
        ownerUuid = owner.getUUID();
    }

    @Nullable
    public Player getOwnerPlayer() {
        if (ownerUuid == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            checkContact();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        Player owner = getOwnerPlayer();
        if (tickCount > MAX_AGE_TICKS || owner == null || owner.level() != level()) {
            vanish();
            return;
        }

        setResting(false);
        LivingEntity target = findOwnerLookTarget(owner);
        if (target == null || target == owner || target instanceof Bat || !target.isAlive()) {
            super.customServerAiStep(level);
            return;
        }

        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = targetPos.subtract(position());
        double distance = toTarget.length();
        if (distance <= 0.001D || !hasClearCourse(targetPos)) {
            super.customServerAiStep(level);
            return;
        }

        Vec3 velocity = getDeltaMovement().add(toTarget.normalize().scale(HOMING_ACCELERATION));
        setDeltaMovement(velocity);
        float yaw = (float)(Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        zza = 0.5F;
    }

    private void checkContact() {
        AABB contact = getBoundingBox().inflate(CONTACT_INFLATE, 0.0D, CONTACT_INFLATE);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, contact,
                entity -> entity != this && !(entity instanceof Bat) && entity.isAlive())) {
            Player owner = getOwnerPlayer();
            if (target == owner) {
                continue;
            }
            if (level() instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, damageSources().indirectMagic(this, owner), CONTACT_DAMAGE);
                serverLevel.sendParticles(new DustParticleOptions(0x4A0010, 1.0F),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        12, target.getBbWidth(), target.getBbHeight() * 0.35D, target.getBbWidth(), 0.0D);
                serverLevel.playSound(null, target.blockPosition(), WitcherySoundEvents.RANDOM_DRINK.get(),
                        SoundSource.HOSTILE, 0.5F, 1.0F);
            }
            discard();
            return;
        }
    }

    @Nullable
    private LivingEntity findOwnerLookTarget(Player owner) {
        Vec3 eye = owner.getEyePosition(1.0F);
        Vec3 reach = owner.getViewVector(1.0F).scale(OWNER_LOOK_RANGE);
        Vec3 end = eye.add(reach);
        AABB searchBox = owner.getBoundingBox().expandTowards(reach).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(owner, eye, end, searchBox,
                entity -> entity != owner && entity != this && entity.isPickable()
                        && entity instanceof LivingEntity && !(entity instanceof Bat),
                OWNER_LOOK_RANGE * OWNER_LOOK_RANGE);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }

    private boolean hasClearCourse(Vec3 targetPos) {
        BlockHitResult hit = level().clip(new ClipContext(position(), targetPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(targetPos) < 0.6D;
    }

    private void vanish() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2D, getZ(),
                    12, getBbWidth(), getBbHeight() * 0.35D, getBbWidth(), 0.02D);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putString("OwnerUUID", ownerUuid.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String raw = tag.getStringOr("OwnerUUID", "");
        if (!raw.isEmpty()) {
            try {
                ownerUuid = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                ownerUuid = null;
            }
        }
    }
}
