package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Nightmare - personal Spirit World hunter.
 * Targets only the assigned victim player.
 * Becomes temporarily defended after taking damage (unless standing in Flowing Spirit).
 * Defense timer: 80t in Spirit World, 40t outside.
 * Strips random armor on hit (unless victim has Charm of Disrupted Dreams).
 * Caps incoming damage at 15 per hit.
 * Drops Mellifluous Hunger only in Spirit World.
 */
public class NightmareEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TIMER =
            SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.INT);

    private int defenseTimer = 0;
    private boolean defended = false;

    // Victim tracking - only this player is targeted.
    private UUID victimUUID = null;
    private String victimName = "";

    public NightmareEntity(EntityType<? extends NightmareEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TIMER, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setVictim(ServerPlayer player) {
        this.victimUUID = player.getUUID();
        this.victimName = player.getGameProfile().name();
    }

    public UUID getVictimUUID() {
        return victimUUID;
    }

    public boolean matchesVictim(Player player) {
        if (player == null) return false;
        if (victimUUID != null) {
            return player.getUUID().equals(victimUUID);
        }
        return victimName != null
                && !victimName.isBlank()
                && player.getGameProfile().name().equalsIgnoreCase(victimName);
    }

    private boolean isMyVictim(Player player) {
        if (victimUUID == null && (victimName == null || victimName.isBlank())) {
            return true;
        }
        return matchesVictim(player);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, true, (entity, serverLevel) -> entity instanceof Player player && isMyVictim(player)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (defenseTimer > 0 && --defenseTimer == 0) defended = false;
            int attackTimer = getAttackTimer();
            if (attackTimer > 0) setAttackTimer(attackTimer - 1);

            if (tickCount % 40 == 0 && victimUUID != null && level() instanceof ServerLevel serverLevel) {
                Player victim = serverLevel.getPlayerByUUID(victimUUID);
                if (victim == null || !victim.isAlive() || distanceToSqr(victim) > 1024) {
                    discard();
                    return;
                }
            }

            if (tickCount % 100 == 0) {
                LivingEntity target = this.getTarget();
                if (target instanceof Player && distanceToSqr(target) > 144.0) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double dist = 2 + random.nextDouble() * 3;
                    teleportTo(target.getX() + dist * Math.cos(angle),
                            target.getY(),
                            target.getZ() + dist * Math.sin(angle));
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (defended) return false;

        if (!standingInFlowingSpirit()) {
            boolean weakeningWeapon = isWeakeningWeapon(source);
            defended = true;
            defenseTimer = this.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                    ? (weakeningWeapon ? 40 : 80)
                    : (weakeningWeapon ? 30 : 40);
        }

        return super.hurtServer(level, source, Math.min(amount, 15.0f));
    }

    private boolean isWeakeningWeapon(net.minecraft.world.damagesource.DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity living)) {
            return false;
        }
        ItemStack mainHand = living.getMainHandItem();
        return mainHand.is(WitcheryItems.HUNTSMAN_SPEAR.get());
    }

    private boolean standingInFlowingSpirit() {
        BlockPos feet = this.blockPosition();
        return this.level().getBlockState(feet).is(WitcheryBlocks.FLOWING_SPIRIT.get());
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        setAttackTimer(15);
        boolean weakenedOutsideSpiritWorld = !level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD);
        AttributeInstance attackAttribute = this.getAttribute(Attributes.ATTACK_DAMAGE);
        double oldBaseAttack = attackAttribute != null ? attackAttribute.getBaseValue() : 0.0;

        if (weakenedOutsideSpiritWorld && attackAttribute != null) {
            attackAttribute.setBaseValue(0.5);
        }

        boolean result;
        try {
            result = super.doHurtTarget(level, target);
        } finally {
            if (weakenedOutsideSpiritWorld && attackAttribute != null) {
                attackAttribute.setBaseValue(oldBaseAttack);
            }
        }

        if (result && target instanceof Player player && !player.level().isClientSide()) {
            stripRandomArmor(player);
        }
        return result;
    }

    private void stripRandomArmor(Player player) {
        if (hasCharmOfDisruptedDreams(player)) {
            return;
        }
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        EquipmentSlot slot = slots[random.nextInt(slots.length)];
        ItemStack armor = player.getItemBySlot(slot);
        if (!armor.isEmpty()) {
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.drop(armor, true);
        }
    }

    private boolean hasCharmOfDisruptedDreams(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(WitcheryItems.CHARM_OF_DISRUPTED_DREAMS.get())
                    || stack.is(WitcheryItems.FANCIFUL_THINKING_CHARM.get())) {
                return true;
            }
        }
        return false;
    }

    public int getAttackTimer() {
        return entityData.get(DATA_ATTACK_TIMER);
    }

    private void setAttackTimer(int attackTimer) {
        entityData.set(DATA_ATTACK_TIMER, Math.max(0, attackTimer));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            int looting = recentlyHit ? 1 : 0;
            int cap = Math.max(10 - looting, 5);
            int qty = level.random.nextInt(cap) == 0 ? 2 : 1;
            spawnAtLocation((ServerLevel) level(), new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get(), qty));
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (victimUUID != null) tag.putString("VictimUUID", victimUUID.toString());
        tag.putString("VictimName", victimName != null ? victimName : "");
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String victimUuidString = tag.getStringOr("VictimUUID", "");
        victimUUID = victimUuidString.isEmpty() ? null : UUID.fromString(victimUuidString);
        victimName = tag.getStringOr("VictimName", "");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_NIGHTMARE_LIVE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_NIGHTMARE_DEAD.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_NIGHTMARE_HIT.get();
    }
}
