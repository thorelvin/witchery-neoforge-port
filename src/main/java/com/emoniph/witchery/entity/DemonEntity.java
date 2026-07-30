package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Demon — large infernal creature summoned through the Infernal ritual circle.
 * Uses melee and throws fireballs at range.
 */
public class DemonEntity extends Monster implements RangedAttackMob, Merchant {
    public static final float SOURCE_SHADOW_RADIUS = 0.5F;
    public static final int SOURCE_ATTACK_TIMER_TICKS = 10;
    public static final int SOURCE_ESCAPE_DELAY_TICKS = 50;
    public static final float SOURCE_ESCAPE_EXPLOSION_POWER = 3.0F;
    private static final EntityDataAccessor<Boolean> PLAYER_CREATED =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACK_TIMER =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);
    private static final String OFFERS_TAG = "WitcheryDemonOffers";
    private static final String OFFERS_VERSION_TAG = "WitcheryDemonOffersVersion";
    private static final int OFFERS_VERSION = 2;
    private static final String NIGHTMARE_OWNER_TAG = "DemonicNightmareOwner";
    private static final int TRADE_XP = 0;
    private static final float PRICE_MULTIPLIER = 0.05F;
    private static final int MIN_MELEE_DAMAGE = 7;
    private static final int MELEE_DAMAGE_ROLL = 15;

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers baseOffers;
    @Nullable
    private MerchantOffers displayedOffers;
    private int escapeTicks = -1;
    @Nullable
    private UUID demonicNightmareOwner;

    public DemonEntity(EntityType<? extends DemonEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLAYER_CREATED, false);
        builder.define(ATTACK_TIMER, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CloseMeleeGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 60, 15.0F));
        this.goalSelector.addGoal(5, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 0.6, true, 4, () -> true));
        this.goalSelector.addGoal(7, new MoveBackToVillageGoal(this, 1.0, false));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false,
                (entity, serverLevel) -> !hasDevilsTongueCharm(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG) || held.getItem() instanceof SpawnEggItem) {
            return super.mobInteract(player, hand);
        }
        if (!isAlive() || isTrading() || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        return openDemonTrade(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public boolean openDevilsTongueTrade(Player player, InteractionHand hand, ItemStack stack) {
        if (level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || !isDevilsTongueCharm(stack) || !isAlive() || isTrading() || player.isSecondaryUseActive()) {
            return false;
        }
        if (level().isClientSide()) {
            return true;
        }
        return openDemonTrade(player);
    }

    private boolean openDemonTrade(Player player) {
        if (!isAlive() || isTrading()) {
            return false;
        }
        setTradingPlayer(player);
        setTarget(null);
        getNavigation().stop();
        openTradingScreen(player, getDisplayName(), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        return true;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (level().isClientSide()) {
            return;
        }
        if (hasDevilsTongueCharm(target) && random.nextDouble() >= 0.05D) {
            playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.4F, 0.7F);
            return;
        }
        double dx = target.getX() - this.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0 - (this.getY() + this.getBbHeight() / 2.0);
        double dz = target.getZ() - this.getZ();
        float spread = (float) Math.sqrt(distanceFactor) * 0.5F;
        Vec3 shot = new Vec3(
                dx + random.nextGaussian() * spread,
                dy,
                dz + random.nextGaussian() * spread);
        Vec3 look = this.getLookAngle();
        LargeFireball fireball = new LargeFireball(level(), this, shot, 1);
        fireball.setPos(getX() + look.x, getY() + getBbHeight() / 2.0 + 0.5, getZ() + look.z);
        level().addFreshEntity(fireball);
        level().playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        setAttackTimer(SOURCE_ATTACK_TIMER_TICKS);
        if (target instanceof LivingEntity living
                && hasDevilsTongueCharm(living)
                && random.nextDouble() >= 0.05D) {
            playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.4F, 0.7F);
            setTarget(null);
            return false;
        }
        target.hurt(damageSources().mobAttack(this), MIN_MELEE_DAMAGE + random.nextInt(MELEE_DAMAGE_ROLL));
        setLastHurtMob(target);
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnWalkingParticle();
            return;
        }
        if (getAttackTimer() > 0) {
            setAttackTimer(getAttackTimer() - 1);
        }
        if (shouldLeaveDemonicNightmare()) {
            discard();
            return;
        }
        if (escapeTicks == 0) {
            escapeTicks = -1;
            level().explode(this, getX(), getY(), getZ(),
                    SOURCE_ESCAPE_EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
        } else if (escapeTicks > 0) {
            escapeTicks--;
        }
        if (tradingPlayer == null) {
            return;
        }
        if (!stillValid(tradingPlayer) || tradingPlayer.containerMenu == tradingPlayer.inventoryMenu) {
            setTradingPlayer(null);
            return;
        }
        setTarget(null);
        getNavigation().stop();
        getLookControl().setLookAt(tradingPlayer, 30.0F, 30.0F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, Math.min(amount, 15.0F));
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean isOnFire()   { return false; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 4));
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PlayerCreated", isPlayerCreated());
        if (demonicNightmareOwner != null) {
            tag.putString(NIGHTMARE_OWNER_TAG, demonicNightmareOwner.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setPlayerCreated(tag.getBooleanOr("PlayerCreated", false));
        String nightmareOwner = tag.getStringOr(NIGHTMARE_OWNER_TAG, "");
        if (!nightmareOwner.isEmpty()) {
            try {
                demonicNightmareOwner = UUID.fromString(nightmareOwner);
            } catch (IllegalArgumentException ignored) {
                demonicNightmareOwner = null;
            }
        }
    }

    public boolean isPlayerCreated() {
        return entityData.get(PLAYER_CREATED);
    }

    public void setPlayerCreated(boolean playerCreated) {
        entityData.set(PLAYER_CREATED, playerCreated);
        if (playerCreated) {
            setPersistenceRequired();
        }
    }

    public void setDemonicNightmareOwner(UUID owner) {
        demonicNightmareOwner = owner;
    }

    private boolean shouldLeaveDemonicNightmare() {
        if (demonicNightmareOwner == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(demonicNightmareOwner);
        return owner == null
                || !owner.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || !WitcheryAttachments.get(owner).isDemonicNightmare();
    }

    public int getAttackTimer() {
        return entityData.get(ATTACK_TIMER);
    }

    private void setAttackTimer(int ticks) {
        entityData.set(ATTACK_TIMER, Math.max(0, ticks));
    }

    private void spawnWalkingParticle() {
        Vec3 movement = getDeltaMovement();
        if (movement.x * movement.x + movement.z * movement.z <= 2.5000003E-7D
                || random.nextInt(5) != 0) {
            return;
        }
        BlockState state = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2D, getZ()));
        if (state.isAir()) {
            return;
        }
        level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                getBoundingBox().minY + 0.1D,
                getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                4.0D * (random.nextDouble() - 0.5D),
                0.5D,
                4.0D * (random.nextDouble() - 0.5D));
    }

    private static boolean hasDevilsTongueCharm(LivingEntity entity) {
        return isDevilsTongueCharm(entity.getMainHandItem()) || isDevilsTongueCharm(entity.getOffhandItem());
    }

    private static boolean isDevilsTongueCharm(ItemStack stack) {
        return stack.is(WitcheryItems.DEVILS_TONGUE_CHARM.get());
    }

    public boolean isTrading() {
        return tradingPlayer != null;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        tradingPlayer = player;
        displayedOffers = null;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        MerchantOffers offers = loadOrCreateOffers();
        if (!hasDiscountCharm(tradingPlayer)) {
            return offers;
        }
        if (displayedOffers == null) {
            displayedOffers = discountedCopy(offers);
        }
        return displayedOffers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        baseOffers = offers;
        displayedOffers = null;
        saveOffers(offers);
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        MerchantOffer baseOffer = findMatchingBaseOffer(offer);
        if (baseOffer != null && baseOffer != offer) {
            baseOffer.increaseUses();
        }
        saveOffers(loadOrCreateOffers());
        damageTradingCharm();

        ItemStack cost = offer.getBaseCostA();
        boolean volatileCurrency = isSourceVolatileCurrency(cost);
        if (volatileCurrency && !level().isClientSide()) {
            escapeTicks = SOURCE_ESCAPE_DELAY_TICKS;
        }
        playSound(volatileCurrency ? SoundEvents.WITHER_SHOOT : SoundEvents.GHAST_AMBIENT,
                getSoundVolume(), getVoicePitch());
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!stack.isEmpty()) {
            playSound(SoundEvents.GHAST_AMBIENT, getSoundVolume(), getVoicePitch());
        }
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return isAlive() && player.isAlive() && distanceToSqr(player) <= 64.0D;
    }

    private MerchantOffers loadOrCreateOffers() {
        if (baseOffers != null) {
            return baseOffers;
        }
        CompoundTag data = getPersistentData();
        if (data.contains(OFFERS_TAG) && data.getIntOr(OFFERS_VERSION_TAG, 0) == OFFERS_VERSION) {
            Tag stored = data.get(OFFERS_TAG);
            baseOffers = MerchantOffers.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, stored))
                    .result()
                    .orElseGet(MerchantOffers::new);
            if (!baseOffers.isEmpty()) {
                return baseOffers;
            }
        }

        baseOffers = createDemonOffers();
        saveOffers(baseOffers);
        return baseOffers;
    }

    private void saveOffers(MerchantOffers offers) {
        MerchantOffers.CODEC.encodeStart(NbtOps.INSTANCE, offers)
                .result()
                .ifPresent(tag -> getPersistentData().put(OFFERS_TAG, tag));
        getPersistentData().putInt(OFFERS_VERSION_TAG, OFFERS_VERSION);
    }

    private MerchantOffers createDemonOffers() {
        List<MerchantOffer> pool = new ArrayList<>();
        int offerCount = 6 + random.nextInt(4);
        var enchantments = level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .listElements()
                .toList();
        for (int i = 0; i < offerCount; i++) {
            var enchantment = enchantments.get(random.nextInt(enchantments.size()));
            int enchantmentLevel = net.minecraft.util.Mth.nextInt(random,
                    enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
            ItemStack book = EnchantmentHelper.createBook(
                    new EnchantmentInstance(enchantment, enchantmentLevel));
            int basePrice = 2 + random.nextInt(5 + enchantmentLevel * 10) + 3 * enchantmentLevel;
            pool.add(offer(price(basePrice), book, 2));
        }
        if (random.nextDouble() < 0.25D) {
            pool.add(offer(price(8 + random.nextInt(3)),
                    new ItemStack(WitcheryItems.SPECTRAL_DUST.get(), 3 + random.nextInt(4)), 2));
        }
        if (random.nextDouble() < 0.25D) {
            pool.add(offer(price(8 + random.nextInt(3)),
                    new ItemStack(WitcheryItems.TONGUE_OF_DOG.get(), 4 + random.nextInt(4)), 2));
        }
        if (random.nextDouble() < 0.15D) {
            pool.add(offer(price(20 + random.nextInt(10)),
                    new ItemStack(WitcheryItems.REDSTONE_SOUP.get()), 2));
        }
        if (random.nextDouble() < 0.15D) {
            pool.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 1),
                    new ItemStack(Items.GHAST_TEAR, 2), 2, TRADE_XP, PRICE_MULTIPLIER));
        }
        if (random.nextDouble() < 0.15D) {
            pool.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 1),
                    new ItemStack(Items.ENDER_PEARL, 2), 2, TRADE_XP, PRICE_MULTIPLIER));
        }
        Collections.shuffle(pool);

        MerchantOffer heart = new MerchantOffer(heartPrice(),
                new ItemStack(WitcheryItems.DEMON_HEART.get()), 2, TRADE_XP, PRICE_MULTIPLIER);
        pool.add(Math.min(random.nextInt(3), pool.size()), heart);

        MerchantOffers offers = new MerchantOffers();
        int count = Math.min(pool.size(), offerCount);
        for (int i = 0; i < count; i++) {
            offers.add(pool.get(i));
        }
        return offers;
    }

    private MerchantOffer offer(ItemCost cost, ItemStack result, int maxUses) {
        return new MerchantOffer(cost, result, maxUses, TRADE_XP, PRICE_MULTIPLIER);
    }

    private ItemCost price(int emeraldBase) {
        Item currency = randomCurrency();
        int multiplier = currency == Items.GOLD_INGOT ? 1
                : currency == Items.EMERALD ? 3
                : currency == Items.DIAMOND ? 5
                : 4;
        return new ItemCost(currency, Math.max(1, emeraldBase / multiplier));
    }

    private ItemCost heartPrice() {
        Item currency = randomCurrency();
        return new ItemCost(currency, currency == Items.GOLD_INGOT ? 30 : 3);
    }

    private Item randomCurrency() {
        return sourceCurrency(random.nextDouble());
    }

    static Item sourceCurrency(double chance) {
        if (chance < 0.2D) return Items.BLAZE_ROD;
        if (chance < 0.4D) return Items.MAGMA_CREAM;
        if (chance < 0.5D) return Items.DIAMOND;
        return chance < 0.75D ? Items.EMERALD : Items.GOLD_INGOT;
    }

    public static boolean isSourceVolatileCurrency(ItemStack stack) {
        return stack.is(Items.BLAZE_ROD) || stack.is(Items.MAGMA_CREAM);
    }

    private MerchantOffers discountedCopy(MerchantOffers offers) {
        MerchantOffers copy = offers.copy();
        for (MerchantOffer offer : copy) {
            ItemStack cost = offer.getBaseCostA();
            int discount = cost.is(Items.GOLD_INGOT) ? 5
                    : cost.is(Items.EMERALD) ? 2
                    : cost.is(Items.DIAMOND) ? 0
                    : 1;
            if (discount > 0) {
                offer.setSpecialPriceDiff(-discount);
            }
        }
        return copy;
    }

    @Nullable
    private MerchantOffer findMatchingBaseOffer(MerchantOffer offer) {
        for (MerchantOffer candidate : loadOrCreateOffers()) {
            if (ItemStack.isSameItemSameComponents(candidate.getResult(), offer.getResult())
                    && ItemStack.isSameItem(candidate.getBaseCostA(), offer.getBaseCostA())) {
                return candidate;
            }
        }
        return null;
    }

    private void damageTradingCharm() {
        if (tradingPlayer == null || !hasDiscountCharm(tradingPlayer)) {
            return;
        }
        ItemStack main = tradingPlayer.getMainHandItem();
        if (isDevilsTongueCharm(main)) {
            main.hurtAndBreak(5, tradingPlayer, EquipmentSlot.MAINHAND);
            return;
        }
        ItemStack offhand = tradingPlayer.getOffhandItem();
        if (isDevilsTongueCharm(offhand)) {
            offhand.hurtAndBreak(5, tradingPlayer, EquipmentSlot.OFFHAND);
        }
    }

    private static boolean hasDiscountCharm(@Nullable Player player) {
        return player != null
                && (isDevilsTongueCharm(player.getMainHandItem()) || isDevilsTongueCharm(player.getOffhandItem()));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    private final class CloseMeleeGoal extends MeleeAttackGoal {
        private static final double SOURCE_MAX_DISTANCE_SQR = 9.0D;

        private CloseMeleeGoal() {
            super(DemonEntity.this, 1.0D, true);
        }

        @Override
        public boolean canUse() {
            return sourceTargetIsNearby() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return sourceTargetIsNearby() && super.canContinueToUse();
        }

        private boolean sourceTargetIsNearby() {
            LivingEntity target = DemonEntity.this.getTarget();
            return target != null
                    && DemonEntity.this.distanceToSqr(target) <= SOURCE_MAX_DISTANCE_SQR
                    && !hasDevilsTongueCharm(target);
        }
    }
}
