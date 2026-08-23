package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import com.emoniph.witchery.world.MirrorHelper;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Source-faithful Mirror World guardian and summonable Reflection boss. */
public class ReflectionEntity extends Monster implements RangedAttackMob {
    public static final float SOURCE_DAMAGE_CAP = 6.0F;
    private static final int OWNER_REFRESH_TICKS = 30;
    private static final int EFFECT_REFRESH_TICKS = 60;

    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_WOLFMAN =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_VAMPIRE =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.BOOLEAN);

    private MeleeAttackGoal meleeGoal;
    private RangedAttackGoal rangedGoal;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.reflection"), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    @Nullable private UUID ownerUUID;
    private String ownerName = "";
    private boolean freeSpawn;
    private int livingIntervals = -1;
    private int guardianRoomIndex = -1;
    private boolean completionHandled;
    private boolean rangedTask;
    private int attackTimer;

    public ReflectionEntity(EntityType<? extends ReflectionEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 50.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_WOLFMAN, false);
        builder.define(DATA_VAMPIRE, false);
    }

    @Override
    protected void registerGoals() {
        meleeGoal = new MeleeAttackGoal(this, 1.2D, false);
        rangedGoal = new RangedAttackGoal(this, 1.0D, 20, 60, 15.0F);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, meleeGoal);
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    public void setOwner(ServerPlayer player) {
        setOwnerIdentity(player.getUUID(), player.getScoreboardName());
        copyOwnerState(player, true);
    }

    public void setOwnerIdentity(@Nullable UUID uuid, @Nullable String name) {
        ownerUUID = uuid;
        ownerName = name == null ? "" : name;
        entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
        entityData.set(DATA_OWNER_NAME, ownerName);
        bossEvent.setName(ownerName.isBlank()
                ? Component.translatable("entity.witchery.reflection")
                : Component.literal(ownerName));
    }

    @Nullable
    public UUID getOwnerUUID() {
        String synced = entityData.get(DATA_OWNER_UUID);
        if (!synced.isBlank()) {
            try {
                return UUID.fromString(synced);
            } catch (IllegalArgumentException ignored) {
                // Keep the server-side identity if malformed sync data is ever observed.
            }
        }
        return ownerUUID;
    }

    public String getOwnerName() {
        String synced = entityData.get(DATA_OWNER_NAME);
        return synced.isBlank() ? ownerName : synced;
    }

    public boolean isWolfmanReflection() {
        return entityData.get(DATA_WOLFMAN);
    }

    public boolean isVampireReflection() {
        return entityData.get(DATA_VAMPIRE);
    }

    public void setGuardianRoom(int roomIndex) {
        guardianRoomIndex = Math.max(0, roomIndex);
        freeSpawn = false;
        livingIntervals = -1;
    }

    public int getGuardianRoomIndex() {
        return guardianRoomIndex;
    }

    public boolean isRoomGuardianFor(int roomIndex) {
        return guardianRoomIndex == roomIndex;
    }

    public void setLifetime(int sourceTicks) {
        livingIntervals = sourceTicks;
    }

    public void setFreeSpawn(boolean freeSpawn) {
        this.freeSpawn = freeSpawn;
        if (freeSpawn) guardianRoomIndex = -1;
    }

    public boolean isFreeSpawn() {
        return freeSpawn;
    }

    @Override
    public void tick() {
        super.tick();
        if (attackTimer > 0) attackTimer--;
        if (level().isClientSide()) return;

        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % OWNER_REFRESH_TICKS == 1) {
            if (!freeSpawn && !WitcheryDimensions.isMirrorWorld(level())) {
                discard();
                return;
            }
            if (livingIntervals > -1 && --livingIntervals == 0) {
                discard();
                return;
            }
            refreshOwnerAndCombat();
            pullTargetWhenPathFails();
        }

        LivingEntity target = getTarget();
        if (target != null && random.nextDouble() < 0.05D
                && (target.isPassenger() || target instanceof Player player && player.getAbilities().flying)
                && !target.hasEffect(MobEffects.SLOWNESS)) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 5));
        }
    }

    private void refreshOwnerAndCombat() {
        AABB bounds = getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
        List<Player> nearby = level().getEntitiesOfClass(Player.class, bounds, Player::isAlive);
        Player owner = ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
        if (owner == null || !nearby.contains(owner)) {
            owner = nearby.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setOwnerIdentity(owner == null ? null : owner.getUUID(),
                    owner == null ? "" : owner.getScoreboardName());
        }

        if (owner == null) {
            clearCopiedEquipment();
            entityData.set(DATA_WOLFMAN, false);
            entityData.set(DATA_VAMPIRE, false);
        } else {
            copyOwnerState(owner, tickCount % EFFECT_REFRESH_TICKS == 1);
        }
        updateCombatTask();
    }

    private void copyOwnerState(Player owner, boolean copyEffects) {
        for (EquipmentSlot slot : List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.OFFHAND)) {
            setCopiedItem(slot, owner.getItemBySlot(slot));
        }

        ItemStack strongest = ItemStack.EMPTY;
        double strongestDamage = 0.0D;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = owner.getInventory().getItem(slot);
            double damage = candidate.isEmpty() ? 0.0D
                    : candidate.getAttributeModifiers().compute(
                            Attributes.ATTACK_DAMAGE, 0.0D, EquipmentSlot.MAINHAND);
            if (damage > strongestDamage) {
                strongestDamage = damage;
                strongest = candidate;
            }
        }
        if (strongest.isEmpty()) strongest = owner.getMainHandItem();

        boolean wolfman = false;
        boolean vampire = false;
        if (owner instanceof ServerPlayer serverPlayer) {
            WitcheryPlayerData data = WitcheryAttachments.get(serverPlayer);
            wolfman = data.isWerewolf() && data.getWolfForm() == WitcheryPlayerData.WolfForm.WOLFMAN;
            vampire = data.isVampire();
        }
        entityData.set(DATA_WOLFMAN, wolfman);
        entityData.set(DATA_VAMPIRE, vampire);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(wolfman ? 6.0D : 2.0D);
        setCopiedItem(EquipmentSlot.MAINHAND, wolfman ? ItemStack.EMPTY : strongest);

        if (copyEffects) {
            removeAllEffects();
            for (MobEffectInstance effect : owner.getActiveEffects()) {
                addEffect(new MobEffectInstance(effect));
            }
        }
    }

    private void setCopiedItem(EquipmentSlot slot, ItemStack stack) {
        setItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        setDropChance(slot, 0.0F);
    }

    private void clearCopiedEquipment() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, ItemStack.EMPTY);
            setDropChance(slot, 0.0F);
        }
    }

    private void updateCombatTask() {
        ItemStack held = getMainHandItem();
        boolean shouldRange = held.is(WitcheryItems.MYSTIC_BRANCH.get())
                || held.is(WitcheryItems.CROSSBOW_PISTOL.get())
                || held.getItem() instanceof BowItem;
        if (shouldRange == rangedTask) return;
        if (shouldRange) {
            goalSelector.removeGoal(meleeGoal);
            goalSelector.addGoal(2, rangedGoal);
        } else {
            goalSelector.removeGoal(rangedGoal);
            goalSelector.addGoal(2, meleeGoal);
        }
        rangedTask = shouldRange;
    }

    private void pullTargetWhenPathFails() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !getNavigation().isDone()
                || !getSensing().hasLineOfSight(target)) {
            return;
        }
        Vec3 toward = position().subtract(target.position());
        if (toward.lengthSqr() > 1.0D) {
            Vec3 pull = toward.normalize().scale(0.8D);
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.18D, pull.z));
            target.hurtMarked = true;
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        attackTimer = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        ItemStack held = getMainHandItem();
        if (held.is(WitcheryItems.MYSTIC_BRANCH.get())) {
            if (random.nextBoolean()) castMirrorSpell(target);
        } else if (held.is(WitcheryItems.CROSSBOW_PISTOL.get())) {
            fireBolt(target, distanceFactor);
        } else if (held.getItem() instanceof BowItem) {
            fireArrow(target, distanceFactor);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackTimer = 10;
        else super.handleEntityEvent(id);
    }

    public int getAttackTimer() {
        return attackTimer;
    }

    private void castMirrorSpell(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        SymbolEffect spell = pickSourceSpell();
        SpellEffectEntity orb = new SpellEffectEntity(serverLevel, this, spell, 1);
        orb.setPos(getX(), getY() + getBbHeight() * 0.5D, getZ());
        orb.shoot(target.getX() - getX(), target.getEyeY() - orb.getY(), target.getZ() - getZ(),
                1.0F, 6.0F);
        serverLevel.addFreshEntity(orb);
        serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getEyeY(), getZ(),
                random.nextInt(10) == 0 ? 9 : 3, 0.25D, 0.25D, 0.25D, 0.0D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT,
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private SymbolEffect pickSourceSpell() {
        int roll = random.nextInt(21);
        if (roll < 14) return EffectRegistry.IGNIANIMA;
        if (roll < 16) return EffectRegistry.EXPELLIARMUS;
        if (roll < 18) return EffectRegistry.FLIPENDO;
        if (roll < 20) return EffectRegistry.IMPEDIMENTA;
        return EffectRegistry.CONFUNDUS;
    }

    private void fireBolt(LivingEntity target, float distanceFactor) {
        BoltEntity bolt = new BoltEntity(level(), this, boltTypeFor(target));
        bolt.setBaseDamage(distanceFactor * 2.0F + random.nextGaussian() * 0.25D
                + level().getDifficulty().getId() * 0.11D);
        shootAt(target, bolt, 1.6F, 14 - level().getDifficulty().getId() * 4);
    }

    private int boltTypeFor(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.isWerewolf()) return BoltEntity.SILVER;
            if (data.isVampire()) return BoltEntity.BONE;
        }
        if (target.getType().is(EntityTypeTags.UNDEAD)) return BoltEntity.BONE;
        return random.nextInt(4) == 0 ? BoltEntity.NULLIFYING : BoltEntity.WOODEN;
    }

    private void fireArrow(LivingEntity target, float distanceFactor) {
        var arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW),
                distanceFactor, getMainHandItem());
        shootAt(target, arrow, 1.6F, 14 - level().getDifficulty().getId() * 3);
    }

    private void shootAt(LivingEntity target, net.minecraft.world.entity.projectile.Projectile projectile,
                         float velocity, float inaccuracy) {
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333D) - projectile.getY();
        double dz = target.getZ() - getZ();
        double arc = Math.sqrt(dx * dx + dz * dz) * 0.2D;
        projectile.shoot(dx, dy + arc, dz, velocity, inaccuracy);
        playSound(SoundEvents.ARROW_SHOOT, 1.0F,
                1.0F / (random.nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, Math.min(amount, SOURCE_DAMAGE_CAP));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide() || completionHandled || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        completionHandled = true;
        spawnAtLocation(serverLevel, new ItemStack(WitcheryItems.DEMON_HEART.get()));
        MirrorHelper.clearReflectionRoom(serverLevel, this);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        freeSpawn = true;
        guardianRoomIndex = -1;
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putString("OwnerUUID", ownerUUID.toString());
        tag.putString("OwnerName", ownerName);
        tag.putBoolean("FreeSpawn", freeSpawn);
        tag.putBoolean("Vampire", isVampireReflection());
        tag.putBoolean("Wolfman", isWolfmanReflection());
        tag.putInt("LivingIntervals", livingIntervals);
        if (guardianRoomIndex >= 0) tag.putInt("GuardianRoom", guardianRoomIndex);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String uuid = tag.getStringOr("OwnerUUID", "");
        try {
            ownerUUID = uuid.isBlank() ? null : UUID.fromString(uuid);
        } catch (IllegalArgumentException ignored) {
            ownerUUID = null;
        }
        setOwnerIdentity(ownerUUID, tag.getStringOr("OwnerName", ""));
        freeSpawn = tag.getBooleanOr("FreeSpawn", false);
        livingIntervals = tag.getIntOr("LivingIntervals", -1);
        guardianRoomIndex = tag.getIntOr("GuardianRoom", -1);
        entityData.set(DATA_VAMPIRE, tag.getBooleanOr("Vampire", false));
        entityData.set(DATA_WOLFMAN, tag.getBooleanOr("Wolfman", false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_REFLECTION_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_REFLECTION_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_REFLECTION_DEATH.get();
    }
}
