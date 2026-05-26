package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Death — 1000 HP boss.
 * Neutral until a player looks directly at him. Then teleports to and attacks them.
 * Constantly heals. Drops Death's outfit on death.
 */
public class DeathEntity extends Monster {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.death"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_20);

    private static final int HEAL_INTERVAL   = 20;   // heal every second
    private static final int TELEPORT_DELAY  = 30;   // ticks before teleport when player out of range
    private static final int STARE_TICKS     = 5;    // ticks a player must look to trigger aggro
    private static final double DETECT_RANGE = 64.0;
    private static final Identifier ATTACKING_SPEED_BOOST_ID = Identifier.fromNamespaceAndPath("witchery", "death_attacking_speed_boost");
    private static final AttributeModifier ATTACKING_SPEED_BOOST = new AttributeModifier(
            ATTACKING_SPEED_BOOST_ID,
            0.15,
            AttributeModifier.Operation.ADD_VALUE);

    private int teleportTimer  = 0;
    private int stareTimer     = 0;
    private boolean isAngered  = false;

    public DeathEntity(EntityType<? extends DeathEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,         1000.0)
                .add(Attributes.MOVEMENT_SPEED,        0.3)
                .add(Attributes.ATTACK_DAMAGE,          7.0)
                .add(Attributes.FOLLOW_RANGE,          64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE,   1.0);
    }

    // Death uses no AI goals — all behaviour is tick-driven
    @Override
    protected void registerGoals() {}

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        AttributeInstance movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        movement.removeModifier(ATTACKING_SPEED_BOOST_ID);
        if (getTarget() != null) {
            movement.addTransientModifier(ATTACKING_SPEED_BOOST);
        }
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnAmbientPortalParticles();
            return;
        }

        bossEvent.setProgress(getHealth() / getMaxHealth());

        // Constant healing
        if (tickCount % HEAL_INTERVAL == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }

        // Stare detection: if any player is looking at Death, trigger aggro
        if (!isAngered) {
            List<Player> nearby = level().getEntitiesOfClass(Player.class,
                    new AABB(getX() - DETECT_RANGE, getY() - DETECT_RANGE, getZ() - DETECT_RANGE,
                             getX() + DETECT_RANGE, getY() + DETECT_RANGE, getZ() + DETECT_RANGE));
            for (Player player : nearby) {
                if (isPlayerLookingAt(player)) {
                    stareTimer++;
                    if (stareTimer >= STARE_TICKS) {
                        isAngered = true;
                        setTarget(player);
                        playSound(SoundEvents.WITHER_AMBIENT, 1.0f, 0.7f);
                        stareTimer = 0;
                    }
                    break;
                }
            }
        }

        // Teleport to target if angered and target out of melee range
        if (isAngered && getTarget() instanceof LivingEntity target) {
            double dist = distanceTo(target);
            if (dist > 4.0) {
                teleportTimer++;
                if (teleportTimer >= TELEPORT_DELAY) {
                    teleportTowardsTarget(target);
                    teleportTimer = 0;
                }
            } else {
                teleportTimer = 0;
                // Melee: handled by base Monster hurt logic via doHurtTarget
                // Force a melee swing each second
                if (tickCount % 20 == 0) doHurtTarget((net.minecraft.server.level.ServerLevel) level(), target);
            }
        }
    }

    private boolean isPlayerLookingAt(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();
        Vec3 toMe = new Vec3(
                getX() - eye.x,
                getBoundingBox().minY + getBbHeight() * 0.5 - eye.y,
                getZ() - eye.z);
        double distance = toMe.length();
        if (distance <= 0.0) {
            return false;
        }
        // Source cone tightens with distance and aims at Death's body center.
        return look.dot(toMe.normalize()) > 1.0 - 0.025 / distance;
    }

    private void spawnAmbientPortalParticles() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25,
                    getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                    (random.nextDouble() - 0.5) * 2.0,
                    -random.nextDouble(),
                    (random.nextDouble() - 0.5) * 2.0);
        }
    }

    private void teleportTowardsTarget(LivingEntity target) {
        double tx = target.getX() + (random.nextDouble() - 0.5) * 2;
        double tz = target.getZ() + (random.nextDouble() - 0.5) * 2;
        double ty = target.getY();
        teleportTo(tx, ty, tz);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    getX(), getY() + 1.0, getZ(), 20, 0.5, 1.0, 0.5, 0.1);
        }
        playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.0f);
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource src, float amount) {
        if (src.getEntity() instanceof Player attacker) {
            isAngered = true;
            setTarget(attacker);
        }
        return super.hurtServer(level, src, amount);
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(Items.SKELETON_SKULL));
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(Items.BONE, 3 + random.nextInt(4)));
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(Items.ENCHANTED_BOOK));
        // Rare drops (20% each)
        if (random.nextInt(5) == 0) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.DEATHS_HAND.get()));
        if (random.nextInt(5) == 0) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.DEATHS_HOOD.get()));
        if (random.nextInt(5) == 0) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.DEATHS_ROBE.get()));
        if (random.nextInt(5) == 0) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.DEATHS_SHOES.get()));
        if (random.nextInt(10) == 0) spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.BINKYS_SKULL.get()));
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Angered", isAngered);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        isAngered = tag.getBooleanOr("Angered", false);
    }
}
