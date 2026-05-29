package com.emoniph.witchery.entity;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class LilithEntity extends Monster implements RangedAttackMob {

    private static final float MAX_DAMAGE_PER_HIT = 12.0F;
    private static final float HEAL_FRIENDLY_PER_SEC = 5.0F;

    private static final EntityDataAccessor<Boolean> IS_FRIENDLY =
            SynchedEntityData.defineId(LilithEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_TIMER =
            SynchedEntityData.defineId(LilithEntity.class, EntityDataSerializers.INT);

    public LilithEntity(EntityType<? extends LilithEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0);
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
        return isFriendly() ? null : super.getTarget();
    }

    @Override
    public void tick() {
        super.tick();
        int timer = getAttackTimer();
        if (timer > 0) {
            entityData.set(ATTACK_TIMER, timer - 1);
        }
        if (!level().isClientSide() && isFriendly() && tickCount % 20 == 0) {
            heal(HEAL_FRIENDLY_PER_SEC);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isFriendly()) return false;
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
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1) return;
        Vec3 shot = new Vec3(dx / dist, dy / dist, dz / dist);
        LargeFireball fireball = new LargeFireball(level(), this, shot, 1);
        fireball.setPos(getX(), getEyeY(), getZ());
        level().addFreshEntity(fireball);
        playSound(SoundEvents.GHAST_SHOOT, 1.0f, random.nextFloat() * 0.4f + 0.8f);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isFriendly() || level().isClientSide()) return super.mobInteract(player, hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        WitcheryPlayerData data = WitcheryAttachments.get(serverPlayer);

        // Full goblet → become vampire (non-vampire, non-werewolf player)
        if (held.is(WitcheryItems.FULL_GLASS_GOBLET.get()) && !data.isVampire() && !data.isWerewolf()) {
            data.setVampire(true);
            data.setBloodLevel(250);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.vampire"), false);
            player.setItemInHand(hand, new ItemStack(WitcheryItems.GLASS_GOBLET.get()));
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        // Garlic → cure vampire
        if (held.is(WitcheryItems.GARLIC.get()) && data.isVampire()) {
            data.setVampire(false);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.cured"), false);
            player.setItemInHand(hand, ItemStack.EMPTY);
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        // Brown mushroom → level 6→7 quest
        if (held.is(Items.BROWN_MUSHROOM) && data.isVampire()
                && data.getVampireLevel() == 6 && data.canIncreaseVampireLevel()) {
            data.setVampireLevel(7);
            PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            serverPlayer.displayClientMessage(
                    Component.translatable("entity.witchery.lilith.quest.level7"), false);
            player.setItemInHand(hand, ItemStack.EMPTY);
            vanish();
            return InteractionResult.SUCCESS_SERVER;
        }

        // Default dialogue
        serverPlayer.displayClientMessage(
                Component.translatable("entity.witchery.lilith.friendly"), false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private void transitionToFriendly(DamageSource source) {
        setFriendly(true);
        setHealth(getMaxHealth());
        setTarget(null);

        // Teleport near the nearest or killing player
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
        level().broadcastEntityEvent(this, (byte) 60);
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isFriendly() ? null : SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        // no drops — Lilith transitions, doesn't die
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Friendly", isFriendly());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setFriendly(tag.getBooleanOr("Friendly", false));
    }
}
