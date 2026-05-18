package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hobgoblin — forest-dwelling merchant.
 * Neutral to players, hostile to villagers, immune to fall damage.
 * When leashed: picks up nearby dropped items and deposits in chests.
 * When leashed + holding a pickaxe: occasionally mines, yielding Koboldite Dust.
 */
public class HobgoblinEntity extends AbstractVillager {

    public HobgoblinEntity(EntityType<? extends HobgoblinEntity> type, Level level) {
        super(type, level);
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 1024.0); // immune to fall damage
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8f, 0.6, 0.6));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Target villagers
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<net.minecraft.world.entity.npc.villager.Villager>(this, Villager.class, true));
    }

    // ── Trades ────────────────────────────────────────────────────────────────
    // Sequential: must purchase trade N before N+1 unlocks. Simplified: all available from start.

    @Override
    protected void updateTrades(net.minecraft.server.level.ServerLevel _level) {
        var offers = getOffers();
        if (!offers.isEmpty()) return; // already set

        // Trade 1: 17 Coal → 1 Koboldite Dust (single ingredient)
        offers.add(new MerchantOffer(new ItemCost(Items.COAL, 17), java.util.Optional.empty(),
                new ItemStack(WitcheryItems.KOBOLDITE_DUST.get(), 1), 12, 0, 0.05f));
        // Trade 2: 9 Koboldite Dust + 5 Gold Nuggets → 1 Koboldite Nugget
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_DUST.get(), 9),
                java.util.Optional.of(new ItemCost(Items.GOLD_NUGGET, 5)),
                new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 1), 8, 0, 0.05f));
        // Trade 3: 16 Koboldite Dust + 1 Gold Ingot → 2 Koboldite Nuggets
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_DUST.get(), 16),
                java.util.Optional.of(new ItemCost(Items.GOLD_INGOT, 1)),
                new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 2), 8, 0, 0.05f));
        // Trade 4: 9 Koboldite Nuggets + 1 Emerald → 1 Koboldite Ingot
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_NUGGET.get(), 9),
                java.util.Optional.of(new ItemCost(Items.EMERALD, 1)),
                new ItemStack(WitcheryItems.KOBOLDITE_INGOT.get(), 1), 4, 0, 0.05f));
        // Trade 5: 8 Gold Ingots → 1 Emerald (single ingredient)
        offers.add(new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 8), java.util.Optional.empty(),
                new ItemStack(Items.EMERALD, 1), 16, 0, 0.05f));
    }

    // ── Interaction ────────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Give a pickaxe → hobgoblin holds it for mining duty
        if (!level().isClientSide() && isLeashed() && held.is(net.minecraft.tags.ItemTags.PICKAXES)) {
            setItemInHand(InteractionHand.MAIN_HAND, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide() && !isLeashed()) {
            openTradingScreen(player, Component.translatable("witchery.hobgoblin.trade"), 1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ── Lead behavior ──────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0) return;

        if (!isLeashed()) return;

        // Pick up nearby dropped items
        AABB pickupRange = getBoundingBox().inflate(4);
        List<ItemEntity> nearby = level().getEntitiesOfClass(ItemEntity.class, pickupRange);
        for (ItemEntity ie : nearby) {
            if (ie.isAlive() && !ie.getItem().isEmpty()) {
                ie.getItem(); // don't actually take it — just wander toward chests
                depositToNearbyChest(ie.getItem().copy());
                ie.discard();
                break; // one per tick cycle
            }
        }

        // If holding pickaxe, occasionally mine for koboldite dust
        ItemStack mainHand = getMainHandItem();
        if (mainHand.is(net.minecraft.tags.ItemTags.PICKAXES) && tickCount % 100 == 0) {
            if (getRandom().nextFloat() < 0.15f) {
                ItemStack dust = new ItemStack(WitcheryItems.KOBOLDITE_DUST.get());
                if (!depositToNearbyChest(dust)) {
                    spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), dust);
                }
            }
        }
    }

    /** Try to deposit an item into a nearby chest (8-block radius). Returns true if deposited. */
    private boolean depositToNearbyChest(ItemStack stack) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (BlockPos bp : BlockPos.betweenClosed(
                blockPosition().offset(-8, -3, -8),
                blockPosition().offset( 8,  3,  8))) {
            if (sl.getBlockEntity(bp) instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (chest.getItem(i).isEmpty()) {
                        chest.setItem(i, stack);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Fall damage immunity ───────────────────────────────────────────────────

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource src) {
        return false; // immune to falling
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return null; }

    @Override protected SoundEvent getAmbientSound()              { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound()                { return SoundEvents.VILLAGER_DEATH; }
    @Override public SoundEvent getNotifyTradeSound()             { return SoundEvents.VILLAGER_YES; }
    @Override public boolean showProgressBar()                    { return false; }
    @Override public int getVillagerXp()                          { return 0; }
    @Override public boolean canRestock()                         { return true; }
    @Override protected void rewardTradeXp(MerchantOffer offer)   {}
}
