package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

public class SunGrenadeEntity extends ThrowableProjectile
        implements net.minecraft.world.entity.projectile.ItemSupplier {

    // Synced so the client (ThrownItemRenderer) can show the correct thrown item — a Sun Grenade,
    // or the Duplication Grenade when used in duplication mode.
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_DUPLICATION =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    SunGrenadeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private static final int MAX_RELEASE_TICKS = 200;
    private static final double ACTIVE_UNDEAD_RADIUS = 1.0;
    private static final double FINAL_XZ_RADIUS = 3.0;
    private static final double FINAL_Y_RADIUS = 2.0;
    private static final int DUPLICATION_DECOY_TICKS = 20 * 10;
    private static final double DUPLICATION_ATTENTION_RADIUS = 12.0;

    private boolean impacted;
    private boolean lightBlockPlaced;
    private BlockPos lightBlockPos;
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
        builder.define(DATA_DUPLICATION, false);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(this.entityData.get(DATA_DUPLICATION)
                ? WitcheryItems.DUPLICATION_GRENADE.get()
                : WitcheryItems.SUN_GRENADE.get());
    }

    public void setDuplicationMode(UUID ownerUUID, String ownerName) {
        this.duplicationMode = true;
        this.duplicationOwnerUUID = ownerUUID;
        this.duplicationOwnerName = ownerName == null ? "" : ownerName;
        this.entityData.set(DATA_DUPLICATION, true);
    }

    @Override
    protected void onHit(HitResult result) {
        if (duplicationMode) {
            if (level() instanceof ServerLevel sl) {
                spawnDuplicationDecoy(sl);
            }
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }

        if (!impacted) {
            impacted = true;
            setDeltaMovement(0.0, 0.0, 0.0);
            setNoGravity(true);
            if (level() instanceof ServerLevel sl) {
                playImpactEffects(sl);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (!duplicationMode && impacted && level().random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.END_ROD,
                        getX() - 0.1 + level().random.nextDouble() * 0.2,
                        getY() + 0.05 + level().random.nextDouble() * 0.2,
                        getZ() - 0.1 + level().random.nextDouble() * 0.2,
                        0.0, 0.02, 0.0);
            }
            return;
        }

        if (!(level() instanceof ServerLevel sl) || isRemoved()) {
            return;
        }

        if (duplicationMode) {
            if (tickCount >= MAX_RELEASE_TICKS) {
                spawnDuplicationDecoy(sl);
                discard();
            }
            return;
        }

        maintainLightBlock(sl);
        if (impacted) {
            igniteNearbyUndead(sl, ACTIVE_UNDEAD_RADIUS, ACTIVE_UNDEAD_RADIUS, 3.0f);
            if (tickCount % 20 == 0) {
                playChargingEffects(sl);
            }
        }

        if (tickCount >= MAX_RELEASE_TICKS) {
            explode(sl);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Impacted", impacted);
        if (lightBlockPlaced && lightBlockPos != null) {
            tag.putBoolean("BlockPlaced", true);
            tag.putInt("BlockPlacedX", lightBlockPos.getX());
            tag.putInt("BlockPlacedY", lightBlockPos.getY());
            tag.putInt("BlockPlacedZ", lightBlockPos.getZ());
        }
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
        impacted = tag.getBooleanOr("Impacted", false);
        lightBlockPlaced = tag.getBooleanOr("BlockPlaced", false);
        if (lightBlockPlaced) {
            lightBlockPos = new BlockPos(
                    tag.getIntOr("BlockPlacedX", 0),
                    tag.getIntOr("BlockPlacedY", 0),
                    tag.getIntOr("BlockPlacedZ", 0));
        } else {
            lightBlockPos = null;
        }
        duplicationMode = tag.getBooleanOr("DuplicationMode", false);
        this.entityData.set(DATA_DUPLICATION, duplicationMode);
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

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            clearLightBlock(sl);
        }
        super.remove(reason);
    }

    private void explode(ServerLevel level) {
        clearLightBlock(level);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.55F, 1.45F);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 1.8F);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 4, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 32,
                FINAL_XZ_RADIUS * 0.5, FINAL_Y_RADIUS * 0.5, FINAL_XZ_RADIUS * 0.5, 0.1);

        AABB box = new AABB(getX() - FINAL_XZ_RADIUS, getY() - FINAL_Y_RADIUS, getZ() - FINAL_XZ_RADIUS,
                getX() + FINAL_XZ_RADIUS, getY() + FINAL_Y_RADIUS, getZ() + FINAL_XZ_RADIUS);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : nearby) {
            if (entity == getOwner()) continue;
            if (isSunGrenadeUndead(entity)) {
                entity.igniteForSeconds(5.0f);
                level.sendParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                        8, 0.25D, 0.35D, 0.25D, 0.02D);
            }
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * (10 + level.random.nextInt(3)), 0, true, true));
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

    private void maintainLightBlock(ServerLevel level) {
        if (!lightBlockPlaced && tickCount % 5 == 4) {
            placeLightBlock(level);
            return;
        }

        if (!lightBlockPlaced || tickCount % 5 != 2 && !impacted) {
            return;
        }

        BlockPos target = pickLightBlockPos(level);
        if (target == null) {
            clearLightBlock(level);
            return;
        }

        boolean refreshMissing = tickCount % 30 == 4 && level.getBlockState(target).isAir();
        if (!target.equals(lightBlockPos) || refreshMissing) {
            clearLightBlock(level);
            placeLightBlock(level, target);
        }
    }

    private void placeLightBlock(ServerLevel level) {
        placeLightBlock(level, pickLightBlockPos(level));
    }

    private void placeLightBlock(ServerLevel level, BlockPos pos) {
        if (pos == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.is(WitcheryBlocks.LIGHT.get())) {
            return;
        }

        if (!state.is(WitcheryBlocks.LIGHT.get())) {
            level.setBlock(pos, WitcheryBlocks.LIGHT.get().defaultBlockState(), 3);
        }
        lightBlockPlaced = true;
        lightBlockPos = pos.immutable();
    }

    private BlockPos pickLightBlockPos(ServerLevel level) {
        BlockPos base = blockPosition();
        if (canHoldLight(level, base)) {
            return base;
        }

        BlockPos above = base.above();
        return canHoldLight(level, above) ? above : null;
    }

    private boolean canHoldLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(WitcheryBlocks.LIGHT.get());
    }

    private void clearLightBlock(ServerLevel level) {
        if (lightBlockPlaced && lightBlockPos != null
                && level.getBlockState(lightBlockPos).is(WitcheryBlocks.LIGHT.get())) {
            level.removeBlock(lightBlockPos, false);
        }
        lightBlockPlaced = false;
        lightBlockPos = null;
    }

    private void igniteNearbyUndead(ServerLevel level, double radiusXZ, double radiusY, float seconds) {
        AABB box = getBoundingBox().inflate(radiusXZ, radiusY, radiusXZ);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity == getOwner() || !isSunGrenadeUndead(entity)) {
                continue;
            }
            entity.igniteForSeconds(seconds);
            if (tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                        4, 0.2D, 0.25D, 0.2D, 0.02D);
            }
        }
    }

    private void playImpactEffects(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.45F, 1.6F);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                18, 0.25D, 0.25D, 0.25D, 0.05D);
    }

    private void playChargingEffects(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.18F, 1.9F);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.05D, getZ(),
                8, 0.18D, 0.18D, 0.18D, 0.03D);
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

    private static boolean isSunGrenadeUndead(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire();
        }
        return entity.getType().is(EntityTypeTags.UNDEAD)
                || entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }
}
