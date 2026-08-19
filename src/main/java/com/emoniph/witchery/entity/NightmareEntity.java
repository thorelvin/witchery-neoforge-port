package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Personal Spirit World hunter, ported from the 1.7.10 EntityNightmare.
 */
public class NightmareEntity extends Monster {
    public static final float SOURCE_SHADOW_RADIUS = 0.5F;
    public static final int SOURCE_NORMAL_ALPHA = 0xE6FFFFFF;
    public static final int SOURCE_DEFENDED_ALPHA = 0x99FFFFFF;
    public static final double SOURCE_VICTIM_RANGE_SQR = 256.0D;
    public static final float SOURCE_DAMAGE_CAP = 15.0F;
    public static final int SOURCE_ATTACK_TIMER_TICKS = 15;
    private static final double SOURCE_FOLLOW_RANGE = 16.0D;
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TIMER =
            SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_VICTIM_NAME =
            SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SCREAMING =
            SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DEFENDED =
            SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.BOOLEAN);

    private int defenseTimer = 0;
    private UUID victimUUID = null;

    public NightmareEntity(EntityType<? extends NightmareEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanFloat(true);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TIMER, 0);
        builder.define(DATA_VICTIM_NAME, "");
        builder.define(DATA_SCREAMING, false);
        builder.define(DATA_DEFENDED, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, SOURCE_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setVictim(ServerPlayer player) {
        this.victimUUID = player.getUUID();
        setVictim(player.getGameProfile().name());
    }

    public void setVictim(String victimName) {
        entityData.set(DATA_VICTIM_NAME, victimName == null ? "" : victimName);
    }

    public String getVictimName() {
        return entityData.get(DATA_VICTIM_NAME);
    }

    public UUID getVictimUUID() {
        return victimUUID;
    }

    public boolean matchesVictim(Player player) {
        if (player == null) {
            return false;
        }
        String victimName = getVictimName();
        if (!victimName.isBlank()) {
            return player.getGameProfile().name().equalsIgnoreCase(victimName);
        }
        return victimUUID == null || player.getUUID().equals(victimUUID);
    }

    public boolean hasAssignedVictim() {
        return victimUUID != null || !getVictimName().isBlank();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BreakDoorGoal(this, difficulty -> true));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
        this.goalSelector.addGoal(5, new MoveBackToVillageGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> true));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 0, false, true,
                (entity, serverLevel) -> entity instanceof Player player && matchesVictim(player)));
    }

    @Override
    public void tick() {
        super.tick();
        if (getAttackTimer() > 0 && !level().isClientSide()) {
            setAttackTimer(getAttackTimer() - 1);
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (defenseTimer > 0 && --defenseTimer == 0) {
            setDefended(false);
        }
        setScreaming(getTarget() != null);

        if (!getVictimName().isBlank()) {
            LivingEntity target = getTarget();
            boolean lostVictim = target == null
                    || !target.isAlive()
                    || distanceToSqr(target) > SOURCE_VICTIM_RANGE_SQR;
            boolean nightmareEnded = !lostVictim
                    && random.nextInt(5) == 0
                    && target instanceof ServerPlayer player
                    && !playerStillHasNightmare(player);
            if (lostVictim || nightmareEnded) {
                sendSourceRemovalParticles(serverLevel);
                discard();
            }
        }
    }

    private boolean playerStillHasNightmare(ServerPlayer player) {
        var data = WitcheryAttachments.get(player);
        return data.hasWakingNightmare()
                || data.isInSpiritWorld() && !data.isSpiritWorldDream();
    }

    private void sendSourceRemovalParticles(ServerLevel level) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                getX(), getY() + getBbHeight() * 0.5D, getZ(),
                16, 0.5D, 1.0D, 0.5D, 0.0D);
    }

    public boolean isScreaming() {
        return entityData.get(DATA_SCREAMING);
    }

    private void setScreaming(boolean screaming) {
        entityData.set(DATA_SCREAMING, screaming);
    }

    public boolean isDefended() {
        return entityData.get(DATA_DEFENDED);
    }

    private void setDefended(boolean defended) {
        entityData.set(DATA_DEFENDED, defended);
    }

    static int sourceDefenseTicks(boolean spiritWorld, boolean weakeningWeapon) {
        if (spiritWorld) {
            return weakeningWeapon ? 40 : 80;
        }
        return weakeningWeapon ? 30 : 40;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isDefended()) {
            return false;
        }

        if (!standingInFlowingSpirit()) {
            boolean weakeningWeapon = isWeakeningWeapon(source);
            defenseTimer = sourceDefenseTicks(
                    this.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD), weakeningWeapon);
            setDefended(true);
        }

        return super.hurtServer(level, source, Math.min(amount, SOURCE_DAMAGE_CAP));
    }

    static boolean isWeakeningWeapon(net.minecraft.world.damagesource.DamageSource source) {
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
        setAttackTimer(SOURCE_ATTACK_TIMER_TICKS);
        if (target instanceof Player player) {
            stripRandomArmor(player);
        }

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
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
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

    static int sourceHungerRollBound(int lootingLevel) {
        return Math.max(10 - Math.max(0, lootingLevel), 5);
    }

    static int sourceHungerQuantity(int lootingLevel, int roll) {
        return lootingLevel > 0 && roll == 0 ? 2 : 1;
    }

    public static int sourceWakingSpawnBound(int curseLevel) {
        return curseLevel > 4 ? 30 : curseLevel > 2 ? 60 : 180;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            int lootingLevel = sourceLootingLevel(level, source);
            int roll = level.random.nextInt(sourceHungerRollBound(lootingLevel));
            int quantity = sourceHungerQuantity(lootingLevel, roll);
            spawnAtLocation(level, new ItemStack(WitcheryItems.MELLIFLUOUS_HUNGER.get(), quantity));
        }
    }

    private static int sourceLootingLevel(ServerLevel level, DamageSource source) {
        ItemStack weapon = source == null ? ItemStack.EMPTY : source.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            return 0;
        }
        try {
            return weapon.getEnchantmentLevel(level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Victim", getVictimName());
        if (victimUUID != null) {
            tag.putString("VictimUUID", victimUUID.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String victimUuidString = tag.getStringOr("VictimUUID", "");
        try {
            victimUUID = victimUuidString.isEmpty() ? null : UUID.fromString(victimUuidString);
        } catch (IllegalArgumentException ignored) {
            victimUUID = null;
        }
        String victimName = tag.getStringOr("Victim", "");
        if (victimName.isEmpty()) {
            victimName = tag.getStringOr("VictimName", "");
        }
        setVictim(victimName);
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public int getAmbientSoundInterval() {
        return super.getAmbientSoundInterval() * 2;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
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
