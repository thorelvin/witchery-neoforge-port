package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Source-shaped insanity hallucination. It looks like a normal monster, is visible only to its
 * victim, makes private fake sounds, cannot be harmed, and slowly decays away.
 */
public abstract class IllusionEntity extends Monster {
    private static final EntityDataAccessor<String> DATA_VICTIM_UUID =
            SynchedEntityData.defineId(IllusionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_VICTIM_NAME =
            SynchedEntityData.defineId(IllusionEntity.class, EntityDataSerializers.STRING);

    private static final int MIN_SPAWN_RANGE = 4;
    private static final int MAX_SPAWN_RANGE = 9;
    private static final int MAX_SPAWN_ATTEMPTS = 8;
    private static final double MAX_VICTIM_DISTANCE_SQR = 32.0D * 32.0D;

    protected IllusionEntity(EntityType<? extends IllusionEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VICTIM_UUID, "");
        builder.define(DATA_VICTIM_NAME, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, Player.class, 0, true, false,
                (entity, serverLevel) -> entity instanceof Player player && matchesVictim(player)));
    }

    public void setVictim(ServerPlayer player) {
        this.entityData.set(DATA_VICTIM_UUID, player.getUUID().toString());
        this.entityData.set(DATA_VICTIM_NAME, player.getGameProfile().name());
        setTarget(player);
    }

    public String getVictimName() {
        return this.entityData.get(DATA_VICTIM_NAME);
    }

    public boolean isVisibleTo(Player player) {
        return matchesVictim(player);
    }

    private boolean matchesVictim(Player player) {
        if (player == null) {
            return false;
        }
        String uuid = this.entityData.get(DATA_VICTIM_UUID);
        if (!uuid.isBlank()) {
            try {
                if (UUID.fromString(uuid).equals(player.getUUID())) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to legacy name matching.
            }
        }
        String name = this.entityData.get(DATA_VICTIM_NAME);
        return !name.isBlank() && name.equalsIgnoreCase(player.getGameProfile().name());
    }

    @Nullable
    private ServerPlayer findVictim(ServerLevel level) {
        String uuid = this.entityData.get(DATA_VICTIM_UUID);
        if (!uuid.isBlank()) {
            try {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(UUID.fromString(uuid));
                if (player != null) {
                    return player;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to legacy name lookup.
            }
        }
        String name = this.entityData.get(DATA_VICTIM_NAME);
        return name.isBlank() ? null : level.getServer().getPlayerList().getPlayerByName(name);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer victim = findVictim(serverLevel);
        if (victim == null || !victim.isAlive() || victim.level() != level()
                || victim.distanceToSqr(this) > MAX_VICTIM_DISTANCE_SQR) {
            discard();
            return;
        }

        if (getTarget() != victim) {
            setTarget(victim);
        }

        if (random.nextInt(15) == 0) {
            float newHealth = getHealth() - 1.0F;
            if (newHealth <= 0.5F) {
                discard();
                return;
            }
            setHealth(newHealth);
        }

        SoundEvent sound = fakeLivingSound();
        if (sound != null && random.nextInt(40) == 0 && victim.distanceToSqr(this) < 64.0D) {
            playOnlyTo(victim, sound, 1.0F, 1.0F);
        }
    }

    @Nullable
    protected SoundEvent fakeLivingSound() {
        return null;
    }

    protected void playOnlyTo(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        player.connection.send(new ClientboundSoundEntityPacket(
                holder, SoundSource.HOSTILE, this, volume, pitch, random.nextLong()));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Player player && matchesVictim(player) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living && canAttack(living)) {
            setLastHurtMob(target);
            swing(getUsedItemHand());
            return true;
        }
        return false;
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
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // No collision jostling: this is a hallucination.
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
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Source illusions drop nothing.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("VictimUUID", this.entityData.get(DATA_VICTIM_UUID));
        tag.putString("VictimName", this.entityData.get(DATA_VICTIM_NAME));
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_VICTIM_UUID, tag.getStringOr("VictimUUID", ""));
        this.entityData.set(DATA_VICTIM_NAME, tag.getStringOr("VictimName", ""));
    }

    public static boolean spawnRandomNear(ServerLevel level, ServerPlayer victim, int strength) {
        EntityType<? extends IllusionEntity> type = switch (level.random.nextInt(3)) {
            case 1 -> WitcheryEntities.ILLUSION_SPIDER.get();
            case 2 -> WitcheryEntities.ILLUSION_ZOMBIE.get();
            default -> WitcheryEntities.ILLUSION_CREEPER.get();
        };
        return spawnNear(level, victim, type);
    }

    private static boolean spawnNear(ServerLevel level, ServerPlayer victim, EntityType<? extends IllusionEntity> type) {
        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            BlockPos ground = sourceLikeSpawnGround(level, victim.blockPosition());
            if (ground == null) {
                continue;
            }
            IllusionEntity illusion = type.create(level, EntitySpawnReason.TRIGGERED);
            if (illusion == null) {
                return false;
            }
            illusion.setVictim(victim);
            illusion.setHealth(illusion.getMaxHealth());
            illusion.snapTo(ground.getX() + 0.5D, ground.getY() + 1.05D, ground.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(illusion);
            return true;
        }
        return false;
    }

    @Nullable
    private static BlockPos sourceLikeSpawnGround(ServerLevel level, BlockPos origin) {
        int activeRadius = MAX_SPAWN_RANGE - MIN_SPAWN_RANGE;
        int ax = level.random.nextInt(activeRadius * 2 + 1);
        if (ax > activeRadius) {
            ax += MIN_SPAWN_RANGE * 2;
        }
        int az = level.random.nextInt(activeRadius * 2 + 1);
        if (az > activeRadius) {
            az += MIN_SPAWN_RANGE * 2;
        }

        int nx = origin.getX() - MAX_SPAWN_RANGE + ax;
        int nz = origin.getZ() - MAX_SPAWN_RANGE + az;
        int startY = origin.getY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(nx, startY, nz);

        int maxY = Math.min(level.getMaxY() - 2, startY + 8);
        while (!level.isEmptyBlock(pos) && pos.getY() < maxY) {
            pos.move(Direction.UP);
        }

        int minY = level.getMinY();
        while (level.isEmptyBlock(pos) && pos.getY() > minY) {
            pos.move(Direction.DOWN);
        }

        BlockPos ground = pos.immutable();
        if (level.isEmptyBlock(ground) || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
            return null;
        }
        BlockPos feet = ground.above();
        BlockPos head = feet.above();
        return level.isEmptyBlock(feet) && level.isEmptyBlock(head) ? ground : null;
    }

    public static final class Creeper extends IllusionEntity {
        public Creeper(EntityType<? extends Creeper> type, Level level) {
            super(type, level);
        }
    }

    public static final class Spider extends IllusionEntity {
        public Spider(EntityType<? extends Spider> type, Level level) {
            super(type, level);
        }

        @Override
        protected SoundEvent fakeLivingSound() {
            return SoundEvents.SPIDER_AMBIENT;
        }
    }

    public static final class Zombie extends IllusionEntity {
        public Zombie(EntityType<? extends Zombie> type, Level level) {
            super(type, level);
        }

        @Override
        protected SoundEvent fakeLivingSound() {
            return SoundEvents.ZOMBIE_AMBIENT;
        }
    }
}
