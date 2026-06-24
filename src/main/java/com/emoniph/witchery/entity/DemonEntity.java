package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Demon — large infernal creature summoned through the Infernal ritual circle.
 * Uses melee and throws fireballs at range.
 */
public class DemonEntity extends Monster implements RangedAttackMob, Merchant {
    private static final String OFFERS_TAG = "WitcheryDemonOffers";
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

    public DemonEntity(EntityType<? extends DemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedBowAttackGoal<DemonEntity>(this, 1.0, 30, 20f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
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
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BLAZE_POWDER) || held.is(Items.MAGMA_CREAM)) {
            return super.mobInteract(player, hand);
        }
        if (!isAlive() || isTrading() || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        setTradingPlayer(player);
        setTarget(null);
        getNavigation().stop();
        openTradingScreen(player, getDisplayName(), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        return InteractionResult.CONSUME;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
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
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
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
        if (level().isClientSide() || tradingPlayer == null) {
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
        boolean volatileCurrency = cost.is(Items.BLAZE_POWDER) || cost.is(Items.MAGMA_CREAM);
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
        if (data.contains(OFFERS_TAG)) {
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
    }

    private MerchantOffers createDemonOffers() {
        List<MerchantOffer> pool = new ArrayList<>();
        int bookOffers = 6 + random.nextInt(4);
        for (int i = 0; i < bookOffers; i++) {
            ItemStack book = EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK),
                    10 + random.nextInt(20), level().registryAccess(), Optional.empty());
            pool.add(offer(price(2 + random.nextInt(18)), book, 1));
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
                    new ItemStack(WitcheryItems.REDSTONE_SOUP.get()), 1));
        }
        if (random.nextDouble() < 0.15D) {
            pool.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 1),
                    new ItemStack(Items.BLAZE_POWDER, 2), 2, TRADE_XP, PRICE_MULTIPLIER));
        }
        if (random.nextDouble() < 0.15D) {
            pool.add(new MerchantOffer(new ItemCost(Items.DIAMOND, 1),
                    new ItemStack(Items.MAGMA_CREAM, 2), 2, TRADE_XP, PRICE_MULTIPLIER));
        }
        Collections.shuffle(pool);

        MerchantOffer heart = new MerchantOffer(heartPrice(),
                new ItemStack(WitcheryItems.DEMON_HEART.get()), 1, TRADE_XP, PRICE_MULTIPLIER);
        pool.add(Math.min(random.nextInt(3), pool.size()), heart);

        MerchantOffers offers = new MerchantOffers();
        int count = Math.min(pool.size(), 6 + random.nextInt(4));
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
        double chance = random.nextDouble();
        if (chance < 0.2D) return Items.BLAZE_POWDER;
        if (chance < 0.4D) return Items.MAGMA_CREAM;
        if (chance < 0.5D) return Items.DIAMOND;
        return chance < 0.75D ? Items.EMERALD : Items.GOLD_INGOT;
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
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
}
