package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.UUID;

/** Treefyd - wooden golem summoned by ritual. Guards the caster's base. */
public class TreefydEntity extends Monster {
    @Nullable
    private UUID ownerUuid;

    public TreefydEntity(EntityType<? extends TreefydEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false,
                (entity, serverLevel) -> shouldTarget(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false,
                (entity, serverLevel) -> entity != this && !(entity instanceof TreefydEntity) && shouldTarget(entity)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
    }

    public boolean isOwnedBy(Player player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    private boolean shouldTarget(@Nullable LivingEntity entity) {
        return !isFriendly(entity);
    }

    private boolean isFriendly(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (ownerUuid != null && ownerUuid.equals(entity.getUUID())) {
            return true;
        }
        if (entity instanceof TreefydEntity other) {
            return ownerUuid != null && ownerUuid.equals(other.ownerUuid);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getTarget() != null && isFriendly(getTarget())) {
            setTarget(null);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isFriendly(target) && super.canAttack(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(WitcheryItems.TREEFYD_SEEDS.get()) || !isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        spawnClone((ServerLevel) level(), player);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private void spawnClone(ServerLevel level, Player owner) {
        TreefydEntity clone = WitcheryEntities.TREEFYD.get().create(level, EntitySpawnReason.TRIGGERED);
        if (clone == null) {
            return;
        }
        clone.snapTo(getX() + 0.5D, getY(), getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        clone.finalizeSpawn(level, level.getCurrentDifficultyAt(clone.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        clone.setOwner(owner);
        clone.setPersistenceRequired();
        level.addFreshEntity(clone);
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
        String owner = tag.getStringOr("OwnerUUID", "");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
    }
}
