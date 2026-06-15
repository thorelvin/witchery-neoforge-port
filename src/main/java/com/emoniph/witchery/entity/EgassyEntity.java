package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class EgassyEntity extends AbstractVillager {

    // ── Quote pools ───────────────────────────────────────────────────────────

    private static final String[] Q_FART = {
        "entity.witchery.egassy.fart.0",
        "entity.witchery.egassy.fart.1",
        "entity.witchery.egassy.fart.2",
        "entity.witchery.egassy.fart.3",
        "entity.witchery.egassy.fart.4",
        "entity.witchery.egassy.fart.5",
        "entity.witchery.egassy.fart.6",
        "entity.witchery.egassy.fart.7",
        "entity.witchery.egassy.fart.8",
        "entity.witchery.egassy.fart.9",
        "entity.witchery.egassy.fart.10",
        "entity.witchery.egassy.fart.11",
        "entity.witchery.egassy.fart.12",
        "entity.witchery.egassy.fart.13",
        "entity.witchery.egassy.fart.14",
    };

    private static final String[] Q_HURT = {
        "entity.witchery.egassy.hurt.0",
        "entity.witchery.egassy.hurt.1",
        "entity.witchery.egassy.hurt.2",
        "entity.witchery.egassy.hurt.3",
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private int shoutCooldown = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EgassyEntity(EntityType<? extends EgassyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // ── Tick — fart + quote ───────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (shoutCooldown > 0) shoutCooldown--;
        if (tickCount % 20 != 0 || random.nextInt(50) != 0) return;

        level().playSound(null, getX(), getY(), getZ(),
                WitcherySoundEvents.ENTITY_EGASSY_FART.get(),
                getSoundSource(), 1.5f, 0.5f + random.nextFloat() * 0.25f);

        shout(pick(Q_FART), 100);
    }

    // ── Hurt — embarrassed reaction ───────────────────────────────────────────

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        super.actuallyHurt(level, source, amount);
        if (random.nextInt(2) == 0) shout(pick(Q_HURT), 150);
    }

    // ── Quote helper ──────────────────────────────────────────────────────────

    private void shout(String key, int cooldownTicks) {
        if (shoutCooldown > 0 || !(level() instanceof ServerLevel sl)) return;
        shoutCooldown = cooldownTicks;
        // Speak in green to nearby players via the action bar, the same way Village Guards shout.
        Component msg = Component.translatable(key).withStyle(ChatFormatting.GREEN);
        AABB area = getBoundingBox().inflate(12, 6, 12);
        sl.getEntitiesOfClass(ServerPlayer.class, area)
                .forEach(p -> p.displayClientMessage(msg, true));
    }

    private String pick(String[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        List<Item> pool = List.of(
                WitcheryItems.WOOD_ASH.get(),
                WitcheryItems.MANDRAKE_ROOT.get(),
                WitcheryItems.WORMWOOD.get(),
                WitcheryItems.BELLADONNA.get(),
                WitcheryItems.SNOWBELL.get(),
                WitcheryItems.EXHALE_OF_HORNED_ONE.get(),
                WitcheryItems.ARTICHOKE.get()
        );
        Item chosen = pool.get(random.nextInt(pool.size()));
        spawnAtLocation(level, new ItemStack(chosen, 1 + random.nextInt(3)));
    }

    // ── Villager boilerplate ──────────────────────────────────────────────────

    @Override protected void updateTrades(ServerLevel level) {}
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean showProgressBar() { return false; }
    @Override public int getVillagerXp() { return 0; }
    @Override public boolean canRestock() { return false; }
    @Override protected void rewardTradeXp(MerchantOffer offer) {}
    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return null; }
}
