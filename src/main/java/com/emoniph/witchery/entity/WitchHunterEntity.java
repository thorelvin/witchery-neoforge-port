package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Witch Hunter — a hostile NPC that tracks and attacks vampires, werewolves, and vanilla witches.
 * Uses a crossbow/bow ranged attack plus melee at close range.
 * Skin type (0-2 → witchhunter1-3.png) assigned at spawn.
 */
public class WitchHunterEntity extends Monster implements RangedAttackMob {
    private static final double HUNTER_NOTICE_CHANCE = 0.1D;
    private static final long HUNTER_DELAY_TICKS = 20L * 60L * 2L;
    private static final double HUNTER_TRIGGER_CHANCE = 0.01D;
    private static final int HUNTER_SPAWN_ROLL_PERIOD = 100;
    private static final int HUNTER_SPAWN_TRIES = 3;
    private static final int HUNTER_MAX_SPAWNS = 2;

    private static final EntityDataAccessor<Integer> HUNTER_TYPE =
            SynchedEntityData.defineId(WitchHunterEntity.class, EntityDataSerializers.INT);
    @Nullable
    private UUID targetPlayerUuid;

    public WitchHunterEntity(EntityType<? extends WitchHunterEntity> type, Level level) {
        super(type, level);
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUNTER_TYPE, 0);
    }

    public int getHunterType() { return this.entityData.get(HUNTER_TYPE); }

    // ── Attributes + goals ────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, false, true, (e, sl) -> isHunterTarget(e)));
    }

    private boolean isHunterTarget(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return isWitchyPlayer(entity);
        }
        return entity instanceof Witch
                || entity instanceof CovenWitchEntity
                || entity.getType().is(EntityTypeTags.UNDEAD)
                || isDemonic(entity);
    }

    private boolean isWitchyPlayer(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp)) return false;
        if (targetPlayerUuid != null && targetPlayerUuid.equals(sp.getUUID())) {
            return true;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        return data.isVampire()
                || data.isWerewolf()
                || hasInventoryItem(sp, WitcheryItems.VOODOO_POPPET.get())
                || hasInventoryItem(sp, WitcheryItems.VAMPIRIC_POPPET.get());
    }

    private static boolean isDemonic(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        return entity instanceof DemonEntity
                || type == EntityType.GHAST
                || type == EntityType.BLAZE
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.WITHER;
    }

    private static boolean hasInventoryItem(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public void setTargetPlayer(@Nullable ServerPlayer player) {
        this.targetPlayerUuid = player == null ? null : player.getUUID();
        if (player != null) {
            this.setTarget(player);
        }
    }

    public static void blackMagicPerformed(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWitchHunterTriggerGameTime() != Long.MIN_VALUE) {
            return;
        }
        if (player.level().random.nextDouble() < HUNTER_NOTICE_CHANCE) {
            data.setWitchHunterTriggerGameTime(player.level().getGameTime());
        }
    }

    public static void tickBlackMagicHunterPressure(ServerPlayer player) {
        if (player.tickCount % HUNTER_SPAWN_ROLL_PERIOD != 1) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        long triggerTime = data.getWitchHunterTriggerGameTime();
        if (triggerTime == Long.MIN_VALUE || player.level().getGameTime() < triggerTime + HUNTER_DELAY_TICKS) {
            return;
        }
        if (player.level().random.nextDouble() >= HUNTER_TRIGGER_CHANCE) {
            return;
        }

        data.clearWitchHunterTrigger();
        int spawned = 0;
        for (int attempt = 0; attempt < HUNTER_SPAWN_TRIES && spawned < HUNTER_MAX_SPAWNS; attempt++) {
            if (spawnHunterNear(player)) {
                spawned++;
            }
        }
        if (spawned > 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(),
                    SoundSource.HOSTILE, 0.45F, 1.35F);
        }
    }

    private static boolean spawnHunterNear(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        WitchHunterEntity hunter = WitcheryEntities.WITCH_HUNTER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (hunter == null) {
            return false;
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 3.0D + level.random.nextInt(6);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, player.blockPosition().getY(), z));
            if (!level.getWorldBorder().isWithinBounds(spawnPos)) {
                continue;
            }
            hunter.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(hunter)) {
                continue;
            }
            hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);
            hunter.setPersistenceRequired();
            hunter.setTargetPlayer(player);
            level.addFreshEntity(hunter);
            return true;
        }

        hunter.discard();
        return false;
    }

    // ── Ranged attack ─────────────────────────────────────────────────────────

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack arrow = new ItemStack(Items.ARROW);
        var projectile = ProjectileUtil.getMobArrow(this, arrow, distanceFactor, this.getMainHandItem());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz) * 0.2;
        projectile.shoot(dx, dy + dist, dz, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(projectile);
    }

    // ── Spawn init ────────────────────────────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        this.entityData.set(HUNTER_TYPE, this.random.nextInt(3));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayerUuid != null) {
            tag.putString("HunterTargetUUID", targetPlayerUuid.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String target = tag.getStringOr("HunterTargetUUID", "");
        try {
            targetPlayerUuid = target.isEmpty() ? null : UUID.fromString(target);
        } catch (IllegalArgumentException ignored) {
            targetPlayerUuid = null;
        }
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.PILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PILLAGER_DEATH; }
}
