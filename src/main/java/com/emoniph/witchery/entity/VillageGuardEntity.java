package com.emoniph.witchery.entity;

import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.mojang.serialization.Dynamic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Village Guard — archer by default; draws a sword when the target closes
 * within {@link #SWORD_RANGE} blocks.  Shouts Witchery-flavoured war-cries
 * to nearby players when it spots a threat, draws a blade, or takes a hit.
 */
public class VillageGuardEntity extends PathfinderMob implements RangedAttackMob {

    private static final float SWORD_RANGE = 3.5F;
    private static final int   SHOUT_RADIUS = 20;
    private static final int MAX_BLOOD = 500;
    private static final int LOW_BLOOD = 250;
    private static final int VILLAGE_RECOVERY_MIN_TICKS = 70;
    private static final int VILLAGE_RECOVERY_RANDOM_TICKS = 50;
    private static final int MESMERISE_SLOWNESS_AMPLIFIER = 4;
    private static final String ORIGIN_PERSISTENT_DATA_TAG = "witchery_origin_villager_persistent_data";
    private static final String ORIGIN_OFFERS_TAG = "witchery_origin_villager_offers";

    // ── Quote pools ───────────────────────────────────────────────────────────

    private static final String[] Q_VAMPIRE = {
        "entity.witchery.village_guard.shout.vampire.blood",
        "entity.witchery.village_guard.shout.vampire.stake",
        "entity.witchery.village_guard.shout.vampire.night",
    };
    private static final String[] Q_WEREWOLF = {
        "entity.witchery.village_guard.shout.werewolf.silver",
        "entity.witchery.village_guard.shout.werewolf.beast",
        "entity.witchery.village_guard.shout.werewolf.moon",
    };
    private static final String[] Q_WITCH = {
        "entity.witchery.village_guard.shout.witch.burn",
        "entity.witchery.village_guard.shout.witch.sorcery",
        "entity.witchery.village_guard.shout.witch.foul",
    };
    private static final String[] Q_DEMON = {
        "entity.witchery.village_guard.shout.demon.void",
        "entity.witchery.village_guard.shout.demon.hellspawn",
    };
    private static final String[] Q_GENERIC = {
        "entity.witchery.village_guard.shout.generic.arms",
        "entity.witchery.village_guard.shout.generic.defend",
        "entity.witchery.village_guard.shout.generic.hold",
    };
    private static final String[] Q_SWORD = {
        "entity.witchery.village_guard.shout.sword.close",
        "entity.witchery.village_guard.shout.sword.fight",
        "entity.witchery.village_guard.shout.sword.arrows",
    };
    private static final String[] Q_HURT = {
        "entity.witchery.village_guard.shout.hurt.firm",
        "entity.witchery.village_guard.shout.hurt.brothers",
    };
    private static final String[] Q_GREET = {
        "entity.witchery.village_guard.greet.move_along",
        "entity.witchery.village_guard.greet.eyes_open",
        "entity.witchery.village_guard.greet.trouble",
        "entity.witchery.village_guard.greet.watch",
        "entity.witchery.village_guard.greet.safe",
        "entity.witchery.village_guard.greet.patrol",
        "entity.witchery.village_guard.greet.stranger",
        "entity.witchery.village_guard.greet.wall",
        "entity.witchery.village_guard.greet.night",
        "entity.witchery.village_guard.greet.business",
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private static final EntityDataAccessor<Integer> GUARD_TYPE =
            SynchedEntityData.defineId(VillageGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BLOOD_LEVEL =
            SynchedEntityData.defineId(VillageGuardEntity.class, EntityDataSerializers.INT);

    private RangedBowAttackGoal<VillageGuardEntity> bowGoal;
    private MeleeAttackGoal                         swordGoal;

    @Nullable private LivingEntity prevTarget;
    private int shoutCooldown = 0;
    private int villageRecoveryCooldown = VILLAGE_RECOVERY_MIN_TICKS;
    private boolean playerAppointedGuard = false;
    private String originVillagerType = "";
    private String originVillagerProfession = "";
    private int originVillagerLevel = VillagerData.MIN_VILLAGER_LEVEL;
    private int originVillagerXp = 0;
    private String originVillagerName = "";
    private boolean originVillagerNameVisible = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public VillageGuardEntity(EntityType<? extends VillageGuardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.LEATHER_BOOTS));
        setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.LEATHER_LEGGINGS));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.LEATHER_HELMET));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0);
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GUARD_TYPE, 0);
        builder.define(BLOOD_LEVEL, MAX_BLOOD);
    }

    public int getGuardType() { return entityData.get(GUARD_TYPE); }
    public void setGuardType(int type) { entityData.set(GUARD_TYPE, Math.floorMod(type, 2)); }
    public int getBlood() { return entityData.get(BLOOD_LEVEL); }
    public void setBlood(int blood) { entityData.set(BLOOD_LEVEL, Math.clamp(blood, 0, MAX_BLOOD)); }
    public void giveBlood(int quantity) { setBlood(getBlood() + Math.max(0, quantity)); }

    public boolean hasVillagerOrigin() {
        return playerAppointedGuard && !originVillagerProfession.isBlank();
    }

    public void captureVillagerOrigin(Villager villager) {
        if (villager == null) {
            return;
        }
        playerAppointedGuard = true;
        originVillagerType = villager.getVillagerData().type().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
        originVillagerProfession = villager.getVillagerData().profession().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
        originVillagerLevel = clampVillagerLevel(villager.getVillagerData().level());
        originVillagerXp = Math.max(0, villager.getVillagerXp());
        Component customName = villager.getCustomName();
        originVillagerName = customName == null ? "" : customName.getString();
        originVillagerNameVisible = villager.isCustomNameVisible();
        copyPersistentData(villager.getPersistentData(), getPersistentData(), ORIGIN_PERSISTENT_DATA_TAG);
        saveOriginOffers(villager.getOffers());
    }

    public void restoreVillagerOrigin(Villager villager) {
        if (villager == null || !hasVillagerOrigin()) {
            return;
        }
        VillagerData data = villager.getVillagerData();
        ResourceKey<VillagerType> typeKey = villagerTypeKey(originVillagerType);
        if (typeKey != null) {
            data = data.withType(villager.level().registryAccess(), typeKey);
        }
        ResourceKey<VillagerProfession> professionKey = villagerProfessionKey(originVillagerProfession);
        if (professionKey != null) {
            data = data.withProfession(villager.level().registryAccess(), professionKey);
        }
        data = data.withLevel(clampVillagerLevel(originVillagerLevel));
        villager.setVillagerData(data);
        villager.setVillagerXp(Math.max(0, originVillagerXp));
        if (!originVillagerName.isBlank()) {
            villager.setCustomName(Component.literal(originVillagerName));
            villager.setCustomNameVisible(originVillagerNameVisible);
        }
        restorePersistentData(villager.getPersistentData());
        restoreOriginOffers(villager);
    }

    public int takeBlood(int quantity, LivingEntity attacker) {
        MobEffectInstance slowness = getEffect(MobEffects.SLOWNESS);
        boolean transfixed = slowness != null && slowness.getAmplifier() >= MESMERISE_SLOWNESS_AMPLIFIER;
        int blood = getBlood();
        int adjustedQuantity = (int) Math.ceil(quantity * 0.66F);
        int remaining = Math.max(blood - adjustedQuantity, 0);
        int taken = blood - remaining;
        setBlood(remaining);
        if (level() instanceof ServerLevel sl) {
            DamageSource source = attacker instanceof Player player
                    ? attacker.damageSources().playerAttack(player)
                    : attacker.damageSources().mobAttack(attacker);
            if (blood < LOW_BLOOD) {
                hurtServer(sl, source, 2.0F);
            } else if (!transfixed) {
                hurtServer(sl, source, 0.5F);
            }
        }
        return taken;
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.bowGoal   = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F);
        this.swordGoal = new MeleeAttackGoal(this, 1.2, true);

        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, bowGoal);
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // HurtByTargetGoal with VillageGuardEntity as alert-ally type:
        // when this guard is hurt, nearby guards also acquire the attacker as target.
        targetSelector.addGoal(1, new HurtByTargetGoal(this, VillageGuardEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, false, true, (e, sl) -> isGuardTarget(e)));
    }

    private boolean isGuardTarget(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) return false;
        if (entity instanceof VillageGuardEntity || entity instanceof WitchHunterEntity) return false;
        if (entity instanceof Player player) {
            if (!(player instanceof ServerPlayer sp)) return false;
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire() || data.isWerewolf();
        }
        return entity instanceof Monster
                || entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    // ── Tick — weapon switch + quote system ───────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (shoutCooldown > 0) shoutCooldown--;

        // Detect newly acquired target and shout
        LivingEntity target = getTarget();
        if (target != prevTarget) {
            if (target != null) shoutForTarget(target);
            prevTarget = target;
        }

        if (tickCount % 10 == 0) reassessWeapon();
        tickVillageRecovery();
    }

    private void tickVillageRecovery() {
        if (--villageRecoveryCooldown > 0) {
            return;
        }
        villageRecoveryCooldown = sourceVillageRecoveryCooldown(random.nextInt(VILLAGE_RECOVERY_RANDOM_TICKS));
        if (!(level() instanceof ServerLevel serverLevel) || !serverLevel.isVillage(blockPosition()) || getTarget() != null) {
            return;
        }
        heal(1.0F);
        if (random.nextInt(4) == 0) {
            setBlood(sourceVillageRecoveryBlood(getBlood(), true, false, true));
        }
    }

    static int sourceVillageRecoveryCooldown(int randomOffset) {
        return VILLAGE_RECOVERY_MIN_TICKS + Math.clamp(randomOffset, 0, VILLAGE_RECOVERY_RANDOM_TICKS - 1);
    }

    static int sourceVillageRecoveryBlood(int currentBlood, boolean inVillage, boolean hasTarget, boolean bloodRoll) {
        int clamped = Math.clamp(currentBlood, 0, MAX_BLOOD);
        return inVillage && !hasTarget && bloodRoll ? Math.clamp(clamped + 1, 0, MAX_BLOOD) : clamped;
    }

    // ── Weapon switching ──────────────────────────────────────────────────────

    private void reassessWeapon() {
        LivingEntity target = getTarget();
        boolean withinSwordRange = target != null && distanceTo(target) < SWORD_RANGE;
        boolean holdingSword = getMainHandItem().is(Items.IRON_SWORD);
        if (withinSwordRange == holdingSword) return;

        if (withinSwordRange) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            goalSelector.removeGoal(bowGoal);
            goalSelector.addGoal(2, swordGoal);
            shout(pick(Q_SWORD), 300);
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            goalSelector.removeGoal(swordGoal);
            goalSelector.addGoal(2, bowGoal);
        }
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
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(projectile);
    }

    // ── Damage — shout on hurt ────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hit = super.hurtServer(level, source, Math.min(amount, 9.0F));
        if (hit && random.nextInt(3) == 0) {
            shout(pick(Q_HURT), 250);
        }
        return hit;
    }

    // ── Quote helpers ─────────────────────────────────────────────────────────

    private void shoutForTarget(LivingEntity target) {
        String key = quoteKeyForTarget(target);
        if (key != null) shout(key, 200);
    }

    @Nullable
    private String quoteKeyForTarget(LivingEntity target) {
        if (target.getType().is(WitcheryEntityTags.VAMPIRES)) return pick(Q_VAMPIRE);
        if (target instanceof Player player && player instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            if (data.isVampire()) return pick(Q_VAMPIRE);
            if (data.isWerewolf()) return pick(Q_WEREWOLF);
        }
        if (target instanceof CovenWitchEntity || target instanceof Witch) return pick(Q_WITCH);
        if (target instanceof DemonEntity) return pick(Q_DEMON);
        if (target instanceof Monster) return pick(Q_GENERIC);
        return null;
    }

    /** Broadcast a war-cry to all players within {@link #SHOUT_RADIUS} blocks. */
    private void shout(String translationKey, int cooldownTicks) {
        if (shoutCooldown > 0 || !(level() instanceof ServerLevel sl)) return;
        shoutCooldown = cooldownTicks;
        Component msg = Component.translatable(translationKey)
                .withStyle(ChatFormatting.GOLD);
        AABB area = getBoundingBox().inflate(SHOUT_RADIUS, 8, SHOUT_RADIUS);
        List<ServerPlayer> nearby = sl.getEntitiesOfClass(ServerPlayer.class, area);
        nearby.forEach(p -> p.displayClientMessage(msg, true));
    }

    private String pick(String[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    private void saveOriginOffers(MerchantOffers offers) {
        getPersistentData().remove(ORIGIN_OFFERS_TAG);
        if (offers == null || offers.isEmpty()) {
            return;
        }
        MerchantOffers.CODEC.encodeStart(NbtOps.INSTANCE, offers)
                .result()
                .ifPresent(tag -> getPersistentData().put(ORIGIN_OFFERS_TAG, tag));
    }

    private void restoreOriginOffers(Villager villager) {
        if (!getPersistentData().contains(ORIGIN_OFFERS_TAG)) {
            return;
        }
        Tag stored = getPersistentData().get(ORIGIN_OFFERS_TAG);
        if (stored == null) {
            return;
        }
        MerchantOffers.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, stored))
                .result()
                .ifPresent(villager::setOffers);
    }

    private static void copyPersistentData(CompoundTag source, CompoundTag target, String targetKey) {
        target.remove(targetKey);
        if (source == null || source.isEmpty()) {
            return;
        }
        CompoundTag copy = new CompoundTag();
        for (String key : source.keySet()) {
            Tag value = source.get(key);
            if (value != null) {
                copy.put(key, value.copy());
            }
        }
        if (!copy.isEmpty()) {
            target.put(targetKey, copy);
        }
    }

    private void restorePersistentData(CompoundTag target) {
        if (target == null || !getPersistentData().contains(ORIGIN_PERSISTENT_DATA_TAG)) {
            return;
        }
        CompoundTag stored = getPersistentData().getCompoundOrEmpty(ORIGIN_PERSISTENT_DATA_TAG);
        for (String key : stored.keySet()) {
            Tag value = stored.get(key);
            if (value != null) {
                target.put(key, value.copy());
            }
        }
    }

    @Nullable
    private static ResourceKey<VillagerType> villagerTypeKey(String id) {
        Identifier identifier = parseIdentifier(id);
        return identifier == null ? null : ResourceKey.create(Registries.VILLAGER_TYPE, identifier);
    }

    @Nullable
    private static ResourceKey<VillagerProfession> villagerProfessionKey(String id) {
        Identifier identifier = parseIdentifier(id);
        return identifier == null ? null : ResourceKey.create(Registries.VILLAGER_PROFESSION, identifier);
    }

    @Nullable
    private static Identifier parseIdentifier(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Identifier.parse(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int clampVillagerLevel(int level) {
        return Math.max(VillagerData.MIN_VILLAGER_LEVEL, Math.min(VillagerData.MAX_VILLAGER_LEVEL, level));
    }

    // ── Greet on right-click ──────────────────────────────────────────────────

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player,
                                                             net.minecraft.world.InteractionHand hand) {
        if (hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && !level().isClientSide()
                && getTarget() == null) {
            if (shoutCooldown > 0) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            String key = pick(Q_GREET);
            shoutCooldown = 100;
            Component fallback = NpcBrainService.speechComponent(NpcPersonas.VILLAGE_GUARD, this,
                    Component.translatable(key));
            boolean queued = player instanceof ServerPlayer serverPlayer
                    && NpcBrainService.INSTANCE.requestVillageGuardGreeting(this, serverPlayer, fallback);
            if (!queued) {
                player.displayClientMessage(fallback, false);
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }

    // ── Spawn init ────────────────────────────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        setGuardType(this.random.nextInt(2));
        if (this.random.nextInt(5) == 0) setItemSlot(EquipmentSlot.FEET,  new ItemStack(Items.IRON_BOOTS));
        if (this.random.nextInt(5) == 0) setItemSlot(EquipmentSlot.LEGS,  new ItemStack(Items.IRON_LEGGINGS));
        if (this.random.nextInt(5) == 0) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                    this.random.nextInt(4) == 0 ? Items.CHAINMAIL_CHESTPLATE : Items.IRON_CHESTPLATE));
        }
        if (this.random.nextInt(5) == 0) setItemSlot(EquipmentSlot.HEAD,  new ItemStack(Items.IRON_HELMET));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardType", getGuardType());
        tag.putInt("BloodLevel", getBlood());
        tag.putBoolean("PlayerAppointedGuard", playerAppointedGuard);
        if (playerAppointedGuard) {
            tag.putString("OriginVillagerType", originVillagerType);
            tag.putString("OriginVillagerProfession", originVillagerProfession);
            tag.putInt("OriginVillagerLevel", originVillagerLevel);
            tag.putInt("OriginVillagerXp", originVillagerXp);
            tag.putString("OriginVillagerName", originVillagerName);
            tag.putBoolean("OriginVillagerNameVisible", originVillagerNameVisible);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setGuardType(tag.getIntOr("GuardType", 0));
        setBlood(tag.getIntOr("BloodLevel", MAX_BLOOD));
        playerAppointedGuard = tag.getBooleanOr("PlayerAppointedGuard", false);
        originVillagerType = tag.getStringOr("OriginVillagerType", "");
        originVillagerProfession = tag.getStringOr("OriginVillagerProfession", "");
        originVillagerLevel = clampVillagerLevel(tag.getIntOr("OriginVillagerLevel", VillagerData.MIN_VILLAGER_LEVEL));
        originVillagerXp = Math.max(0, tag.getIntOr("OriginVillagerXp", 0));
        originVillagerName = tag.getStringOr("OriginVillagerName", "");
        originVillagerNameVisible = tag.getBooleanOr("OriginVillagerNameVisible", false);
    }
}
