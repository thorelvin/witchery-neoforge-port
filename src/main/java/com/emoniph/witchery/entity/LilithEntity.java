package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.GlassGobletItem;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class LilithEntity extends Monster implements RangedAttackMob {

    private static final float MAX_DAMAGE_PER_HIT = 12.0F;
    private static final float HEAL_FRIENDLY_PER_SEC = 5.0F;
    private static final float HOSTILE_HEAL_PER_SEC = 5.0F;
    private static final float WEAKENED_HEAL_PER_SEC = 1.0F;
    private static final int INTRO_TICKS = 150;
    private static final int FIREBALL_WEAKNESS_TICKS = 200;
    private static final int FIRE_RAIN_RADIUS = 32;

    private static final EntityDataAccessor<Boolean> IS_FRIENDLY =
            SynchedEntityData.defineId(LilithEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_TIMER =
            SynchedEntityData.defineId(LilithEntity.class, EntityDataSerializers.INT);

    private int invulnerableTicks;
    private int weaknessTicks;
    private int lifetime;

    public LilithEntity(EntityType<? extends LilithEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_FRIENDLY, false);
        builder.define(ATTACK_TIMER, 0);
    }

    public boolean isFriendly() {
        return entityData.get(IS_FRIENDLY);
    }

    public void setFriendly(boolean friendly) {
        entityData.set(IS_FRIENDLY, friendly);
    }

    public int getAttackTimer() {
        return entityData.get(ATTACK_TIMER);
    }

    public void setInvulnerableStart() {
        invulnerableTicks = INTRO_TICKS;
        setHealth(getMaxHealth() * 0.25F);
        setTarget(null);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 60, 30.0F));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    @Override
    @Nullable
    public LivingEntity getTarget() {
        return isFriendly() || invulnerableTicks > 0 ? null : super.getTarget();
    }

    @Override
    public void tick() {
        super.tick();
        int timer = getAttackTimer();
        if (timer > 0) {
            entityData.set(ATTACK_TIMER, timer - 1);
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (invulnerableTicks > 0) {
            tickIntro(serverLevel);
            return;
        }
        if (isFriendly()) {
            if (tickCount % 20 == 0) {
                heal(HEAL_FRIENDLY_PER_SEC);
            }
            return;
        }
        tickHostile(serverLevel);
    }

    private void tickIntro(ServerLevel level) {
        invulnerableTicks--;
        setTarget(null);
        if (tickCount % 10 == 0) {
            heal(getMaxHealth() * 0.75F / (INTRO_TICKS / 10.0F));
            level.sendParticles(ParticleTypes.PORTAL,
                    getX(), getY() + 1.0D, getZ(),
                    24, 0.6D, 0.8D, 0.6D, 0.08D);
        }
        if (invulnerableTicks <= 0) {
            setHealth(getMaxHealth());
            level.levelEvent(null, 1013, blockPosition(), 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 0.8D, getZ(),
                    18, 0.5D, 0.5D, 0.5D, 0.02D);
        }
    }

    private void tickHostile(ServerLevel level) {
        lifetime++;
        if (weaknessTicks > 0) {
            weaknessTicks--;
        }
        if (tickCount % 20 != 0) {
            return;
        }
        heal(isWeakened() ? WEAKENED_HEAL_PER_SEC : HOSTILE_HEAL_PER_SEC);
        if ((getTarget() != null || getLastHurtByMob() != null) && random.nextInt(5) == 0) {
            pressureNearbyPlayers(level);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isFriendly() || invulnerableTicks > 0) return false;
        if (source.getDirectEntity() instanceof LargeFireball && source.getEntity() instanceof Player) {
            weaknessTicks = FIREBALL_WEAKNESS_TICKS;
            addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FIREBALL_WEAKNESS_TICKS, 0, true, true));
            level.sendParticles(ParticleTypes.WITCH,
                    getX(), getY() + 1.0D, getZ(),
                    32, 0.6D, 0.8D, 0.6D, 0.02D);
            level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE,
                    1.0F, 0.7F + random.nextFloat() * 0.2F);
        }
        float capped = Math.min(amount, MAX_DAMAGE_PER_HIT);
        if (getHealth() - capped <= 0) {
            transitionToFriendly(source);
            return false;
        }
        return super.hurtServer(level, source, capped);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        entityData.set(ATTACK_TIMER, 10);
        this.level().broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.setDeltaMovement(living.getDeltaMovement().add(0, 0.4, 0));
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            entityData.set(ATTACK_TIMER, 10);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (isFriendly() || invulnerableTicks > 0) {
            return;
        }
        entityData.set(ATTACK_TIMER, 10);
        level().broadcastEntityEvent(this, (byte) 4);
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1) return;
        Vec3 shot = new Vec3(dx / dist, dy / dist, dz / dist);
        if (random.nextInt(3) == 0) {
            LargeFireball fireball = new LargeFireball(level(), this, shot, 1);
            fireball.setPos(getX(), getEyeY(), getZ());
            level().addFreshEntity(fireball);
        } else {
            castSpellLikeAttack(target, shot);
        }
        playSound(SoundEvents.GHAST_SHOOT, 1.0f, random.nextFloat() * 0.4f + 0.8f);
    }

    private boolean isWeakened() {
        return weaknessTicks > 0 || hasEffect(MobEffects.WEAKNESS);
    }

    private void pressureNearbyPlayers(ServerLevel level) {
        AABB bounds = getBoundingBox().inflate(FIRE_RAIN_RADIUS, 16.0D, FIRE_RAIN_RADIUS);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, bounds);
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        if (random.nextBoolean()) {
            return;
        }
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE,
                1.0F, 0.6F + random.nextFloat() * 0.3F);
        int fireballs = 3 + random.nextInt(4);
        for (int i = 0; i < fireballs; i++) {
            double x = getX() + (random.nextDouble() - 0.5D) * FIRE_RAIN_RADIUS;
            double z = getZ() + (random.nextDouble() - 0.5D) * FIRE_RAIN_RADIUS;
            double y = getY() + 10.0D + random.nextDouble() * 6.0D;
            SmallFireball fireball = new SmallFireball(level, x, y, z, new Vec3(0.0D, -0.25D, 0.0D));
            fireball.setOwner(this);
            level.addFreshEntity(fireball);
        }
    }

    private void castSpellLikeAttack(LivingEntity target, Vec3 shot) {
        switch (random.nextInt(5)) {
            case 0 -> {
                SmallFireball fireball = new SmallFireball(level(), getX(), getEyeY(), getZ(), shot);
                fireball.setOwner(this);
                level().addFreshEntity(fireball);
            }
            case 1 -> target.setDeltaMovement(target.getDeltaMovement().add(shot.scale(1.25D)).add(0.0D, 0.45D, 0.0D));
            case 2 -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 0));
            }
            case 3 -> {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            default -> {
                Vec3 pull = position().subtract(target.position());
                if (pull.lengthSqr() > 0.001D) {
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.normalize().scale(1.1D)).add(0.0D, 0.25D, 0.0D));
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isFriendly() || level().isClientSide()) return super.mobInteract(player, hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        WitcheryPlayerData data = WitcheryAttachments.get(serverPlayer);
        ((ServerLevel) level()).playSound(null, blockPosition(), WitcherySoundEvents.MOB_LILITH_SAY.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);

        // Empty goblet receives Lilith's blood; drinking it applies or feeds the curse.
        if (held.is(WitcheryItems.GLASS_GOBLET.get())) {
            if (data.isWerewolf()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("rite.witchery.wolf_curse.hybridsnotallow"), false);
                return InteractionResult.SUCCESS_SERVER;
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.vampire_blood"), false);
            player.setItemInHand(hand, GlassGobletItem.withLilithBlood());
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        // Garlic asks Lilith to lift vampirism.
        if (held.is(WitcheryItems.GARLIC.get()) && data.isVampire()) {
            data.setVampire(false);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.cured"), false);
            held.shrink(1);
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        // Source uses a poppy for the level 6 to 7 mastery boon.
        if (held.is(Items.POPPY) && data.isVampire()
                && data.getVampireLevel() == 6 && data.canIncreaseVampireLevel()) {
            data.setVampireLevel(7);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.level7"), false);
            held.shrink(1);
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        if (tryBlessHeldItem(serverPlayer, hand, held)) {
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        serverPlayer.displayClientMessage(
                Component.translatable("entity.witchery.lilith.friendly"), false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private boolean tryBlessHeldItem(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (held.isEmpty() || !canBless(held) || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        ItemStack blessingTarget = held.copyWithCount(1);
        ItemStack blessed = EnchantmentHelper.enchantItem(random, blessingTarget, 40, serverLevel.registryAccess(), Optional.empty());
        if (blessed.isDamageableItem()) {
            blessed.setDamageValue(0);
        }

        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, blessed);
        } else if (!player.getInventory().add(blessed)) {
            player.drop(blessed, false);
        }

        player.displayClientMessage(Component.translatable("entity.witchery.lilith.quest.enchanted"), false);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                1.0F, 0.7F + random.nextFloat() * 0.4F);
        return true;
    }

    private static boolean canBless(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.isEnchantable() || stack.isEnchanted();
    }

    private void transitionToFriendly(DamageSource source) {
        setFriendly(true);
        setHealth(getMaxHealth());
        setTarget(null);
        weaknessTicks = 0;
        invulnerableTicks = 0;
        removeAllEffects();

        ServerPlayer player = findNearestPlayerForFriendly(source);
        if (player != null) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 1.0 + random.nextDouble();
            setPos(player.getX() + Math.cos(angle) * dist, player.getY() + 0.05,
                    player.getZ() + Math.sin(angle) * dist);
            player.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.transition"), false);
        }
        level().broadcastEntityEvent(this, (byte) 60);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    getX(), getY() + 1.0D, getZ(),
                    40, 0.5D, 0.8D, 0.5D, 0.05D);
            serverLevel.playSound(null, blockPosition(), WitcherySoundEvents.MOB_LILITH_SAY.get(), SoundSource.HOSTILE,
                    1.0F, 1.0F);
        }
    }

    @Nullable
    private ServerPlayer findNearestPlayerForFriendly(DamageSource source) {
        if (source != null && source.getEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        AABB bounds = new AABB(blockPosition()).inflate(32, 16, 32);
        List<ServerPlayer> nearby = level().getEntitiesOfClass(ServerPlayer.class, bounds);
        if (nearby.isEmpty()) return null;
        ServerPlayer closest = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer sp : nearby) {
            double d = distanceToSqr(sp);
            if (d < bestDist) {
                bestDist = d;
                closest = sp;
            }
        }
        return closest;
    }

    private void vanish() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    getX(), getY() + 0.7D, getZ(),
                    24, 0.4D, 0.5D, 0.4D, 0.02D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
                    1.0F, 1.1F);
        }
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isFriendly() ? null : WitcherySoundEvents.MOB_LILITH_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_LILITH_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isFriendly() ? WitcherySoundEvents.MOB_LILITH_HIT.get() : WitcherySoundEvents.MOB_LILITH_DEATH.get();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        // No drops; Lilith transitions into her reward state instead of dying.
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Friendly", isFriendly());
        tag.putInt("InvulnerableTicks", invulnerableTicks);
        tag.putInt("WeaknessTicks", weaknessTicks);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setFriendly(tag.getBooleanOr("Friendly", false));
        invulnerableTicks = tag.getIntOr("InvulnerableTicks", 0);
        weaknessTicks = tag.getIntOr("WeaknessTicks", 0);
        lifetime = tag.getIntOr("Lifetime", 0);
    }
}
