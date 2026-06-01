package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

public class SunGrenadeEntity extends ThrowableProjectile {

    private static final float BLAST_RADIUS = 5.0f;
    private static final float VAMPIRE_DAMAGE = 8.0f;
    private static final int DUPLICATION_DECOY_TICKS = 20 * 10;
    private static final double DUPLICATION_ATTENTION_RADIUS = 12.0;

    private boolean duplicationMode;
    private UUID duplicationOwnerUUID;
    private String duplicationOwnerName = "";

    public SunGrenadeEntity(EntityType<? extends SunGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SunGrenadeEntity(Level level, LivingEntity owner) {
        super(WitcheryEntities.SUN_GRENADE.get(), owner.getX(), owner.getEyeY() - 0.1, owner.getZ(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // no extra data needed
    }

    public void setDuplicationMode(UUID ownerUUID, String ownerName) {
        this.duplicationMode = true;
        this.duplicationOwnerUUID = ownerUUID;
        this.duplicationOwnerName = ownerName == null ? "" : ownerName;
    }

    @Override
    protected void onHit(HitResult result) {
        if (level() instanceof ServerLevel sl) {
            if (duplicationMode) {
                spawnDuplicationDecoy(sl);
            } else {
                explode(sl);
            }
        }
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DuplicationMode", duplicationMode);
        if (duplicationOwnerUUID != null) {
            tag.putString("DuplicationOwnerUUID", duplicationOwnerUUID.toString());
        }
        if (!duplicationOwnerName.isEmpty()) {
            tag.putString("DuplicationOwnerName", duplicationOwnerName);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        duplicationMode = tag.getBooleanOr("DuplicationMode", false);
        String uuid = tag.getStringOr("DuplicationOwnerUUID", "");
        if (!uuid.isEmpty()) {
            try {
                duplicationOwnerUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException ignored) {
                duplicationOwnerUUID = null;
            }
        }
        duplicationOwnerName = tag.getStringOr("DuplicationOwnerName", "");
    }

    private void explode(ServerLevel level) {
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 4, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 32,
                BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, 0.1);

        AABB box = new AABB(getX() - BLAST_RADIUS, getY() - BLAST_RADIUS, getZ() - BLAST_RADIUS,
                getX() + BLAST_RADIUS, getY() + BLAST_RADIUS, getZ() + BLAST_RADIUS);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : nearby) {
            if (!isVampire(entity)) continue;
            if (entity == getOwner()) continue;
            entity.hurt(level.damageSources().inFire(), VAMPIRE_DAMAGE);
            entity.igniteForSeconds(5.0f);
        }

        Block.popResource(level, blockPosition(), new ItemStack(WitcheryItems.QUARTZ_SPHERE.get()));
    }

    private void spawnDuplicationDecoy(ServerLevel level) {
        Entity owner = getOwner();
        ReflectionEntity reflection = new ReflectionEntity(WitcheryEntities.REFLECTION.get(), level);
        reflection.snapTo(getX(), getY(), getZ(), getYRot(), 0.0F);

        if (owner instanceof ServerPlayer player
                && (duplicationOwnerUUID == null || duplicationOwnerUUID.equals(player.getUUID()))) {
            reflection.setOwner(player);
        } else {
            reflection.setOwnerIdentity(duplicationOwnerUUID, resolvedDuplicationOwnerName(owner));
        }

        String displayName = resolvedDuplicationOwnerName(owner);
        if (!displayName.isEmpty()) {
            reflection.setCustomName(Component.literal(displayName));
        }
        reflection.setTemporaryDecoy(DUPLICATION_DECOY_TICKS);

        if (level.addFreshEntity(reflection)) {
            attractAttention(level, reflection, owner);
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 24, 0.35, 0.45, 0.35, 0.02);
            level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 16, 0.4, 0.5, 0.4, 0.03);
        }
        Block.popResource(level, blockPosition(), new ItemStack(WitcheryItems.QUARTZ_SPHERE.get()));
    }

    private void attractAttention(ServerLevel level, ReflectionEntity reflection, Entity owner) {
        UUID ownerUuid = duplicationOwnerUUID;
        if (ownerUuid == null && owner != null) {
            ownerUuid = owner.getUUID();
        }

        AABB box = reflection.getBoundingBox().inflate(DUPLICATION_ATTENTION_RADIUS, 6.0, DUPLICATION_ATTENTION_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (mob == reflection || mob.isAlliedTo(reflection) || !mob.canAttack(reflection)) {
                continue;
            }
            LivingEntity target = mob.getTarget();
            if (target == null || ownerUuid != null && target.getUUID().equals(ownerUuid)) {
                mob.setTarget(reflection);
            }
        }
    }

    private String resolvedDuplicationOwnerName(Entity owner) {
        if (!duplicationOwnerName.isBlank()) {
            return duplicationOwnerName;
        }
        if (owner != null) {
            return owner.getName().getString();
        }
        return "Steve";
    }

    private static boolean isVampire(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire();
        }
        return entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }
}
