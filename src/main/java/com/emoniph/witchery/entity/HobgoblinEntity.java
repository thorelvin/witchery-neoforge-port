package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
 * Hobgoblin: forest-dwelling merchant.
 * Neutral to players, hostile to villagers, immune to fall damage.
 * When leashed: picks up nearby dropped items and deposits them in chests.
 * When leashed + holding a pickaxe: occasionally mines, yielding Koboldite Dust.
 */
public class HobgoblinEntity extends AbstractVillager {

    public HobgoblinEntity(EntityType<? extends HobgoblinEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 1024.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8f, 0.6, 0.6));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        var offers = getOffers();
        if (!offers.isEmpty()) {
            return;
        }

        offers.add(new MerchantOffer(new ItemCost(Items.COAL, 17), java.util.Optional.empty(),
                new ItemStack(WitcheryItems.KOBOLDITE_DUST.get(), 1), 12, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_DUST.get(), 9),
                java.util.Optional.of(new ItemCost(Items.GOLD_NUGGET, 5)),
                new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 1), 8, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_DUST.get(), 16),
                java.util.Optional.of(new ItemCost(Items.GOLD_INGOT, 1)),
                new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 2), 8, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(WitcheryItems.KOBOLDITE_NUGGET.get(), 9),
                java.util.Optional.of(new ItemCost(Items.EMERALD, 1)),
                new ItemStack(WitcheryItems.KOBOLDITE_INGOT.get(), 1), 4, 0, 0.05f));
        offers.add(new MerchantOffer(new ItemCost(Items.GOLD_INGOT, 8), java.util.Optional.empty(),
                new ItemStack(Items.EMERALD, 1), 16, 0, 0.05f));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionResult vanillaResult = super.mobInteract(player, hand);
        if (vanillaResult.consumesAction()) {
            return vanillaResult;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (isLeashed() && held.is(net.minecraft.tags.ItemTags.PICKAXES)) {
            if (!level().isClientSide()) {
                setItemInHand(InteractionHand.MAIN_HAND, held.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (!isAlive() || isTrading() || player.isSecondaryUseActive() || isLeashed()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        setTradingPlayer(player);
        openTradingScreen(player, Component.translatable("witchery.hobgoblin.trade"), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0) return;
        if (!isLeashed()) return;

        AABB pickupRange = getBoundingBox().inflate(4);
        List<ItemEntity> nearby = level().getEntitiesOfClass(ItemEntity.class, pickupRange);
        for (ItemEntity itemEntity : nearby) {
            if (itemEntity.isAlive() && !itemEntity.getItem().isEmpty()) {
                depositToNearbyChest(itemEntity.getItem().copy());
                itemEntity.discard();
                break;
            }
        }

        ItemStack mainHand = getMainHandItem();
        if (mainHand.is(net.minecraft.tags.ItemTags.PICKAXES) && tickCount % 100 == 0) {
            if (getRandom().nextFloat() < 0.15f) {
                ItemStack dust = new ItemStack(WitcheryItems.KOBOLDITE_DUST.get());
                if (!depositToNearbyChest(dust)) {
                    spawnAtLocation((ServerLevel) level(), dust);
                }
            }
        }
    }

    private boolean depositToNearbyChest(ItemStack stack) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(
                blockPosition().offset(-8, -3, -8),
                blockPosition().offset(8, 3, 8))) {
            if (serverLevel.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
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

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }
}
