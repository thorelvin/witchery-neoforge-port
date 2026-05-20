package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.world.TormentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LordOfTormentEntity extends Monster implements RangedAttackMob {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.lord_of_torment"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int attackTimer = 0;
    private boolean teleportTriggered = false;
    private final Set<UUID> attackerUuids = new HashSet<>();

    public LordOfTormentEntity(EntityType<? extends LordOfTormentEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      500.0)
                .add(Attributes.MOVEMENT_SPEED,    0.2)
                .add(Attributes.ATTACK_DAMAGE,     8.0)
                .add(Attributes.FOLLOW_RANGE,      50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 60, 12.0f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player>(this, Player.class, true));
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (attackTimer > 0) attackTimer--;

        if (!level().isClientSide()) {
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // Random Blindness on target (5% chance, 10 sec, amp 5)
            LivingEntity target = this.getTarget();
            if (target instanceof Player player && this.random.nextDouble() < 0.05
                    && !player.hasEffect(MobEffects.BLINDNESS)) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 5));
            }

            // Flame particles (server-side position only — client handles visuals)
        }
    }

    // ── Ranged attack — fires 3 (or 9) soulfire projectiles ──────────────────

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        attackTimer = 10;
        if (level().isClientSide()) return;

        int count = this.random.nextInt(10) == 0 ? 9 : 3;
        double spread = Mth.sqrt(power) * 0.5;

        for (int i = 0; i < count; i++) {
            double dx = target.getX() - this.getX() + this.random.nextGaussian() * spread;
            double dy = target.getEyeY() - (this.getY() + this.getEyeHeight()) + this.random.nextGaussian() * 0.1;
            double dz = target.getZ() - this.getZ() + this.random.nextGaussian() * spread;

            SoulfireProjectile fireball = new SoulfireProjectile(level(), this, dx, dy, dz);
            fireball.setPos(this.getX(), this.getY() + this.getEyeHeight() + 0.5, this.getZ());
            level().addFreshEntity(fireball);
        }
    }

    // ── Damage — capped at 5 per hit; track attackers; 50% HP teleport ───────

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLvl, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return false;

        if (source.getEntity() instanceof Player attacker) {
            attackerUuids.add(attacker.getUUID());
        }

        boolean result = super.hurtServer(serverLvl, source, Math.min(amount, 5.0f));

        // At 50% HP, send everyone to Torment and vanish
        if (!teleportTriggered && getHealth() <= getMaxHealth() * 0.5f
                && !serverLvl.dimension().equals(WitcheryDimensions.TORMENT)) {
            teleportTriggered = true;
            triggerTormentTeleport(serverLvl);
        }

        return result;
    }

    private void triggerTormentTeleport(ServerLevel serverLevel) {
        ServerLevel torment = serverLevel.getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) return;
        int tormentLevel = serverLevel.getRandom().nextInt(TormentHelper.NUM_LEVELS);

        double x = this.getX(), y = this.getY(), z = this.getZ();
        AABB bb = new AABB(x - 16, y - 32, z - 16, x + 16, y + 32, z + 16);

        // Collect all players to transport (nearby + tracked attackers)
        List<ServerPlayer> toTransport = new ArrayList<>(serverLevel.getEntitiesOfClass(ServerPlayer.class, bb));
        for (UUID uuid : attackerUuids) {
            ServerPlayer attacker = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (attacker != null && attacker.level() == serverLevel && !toTransport.contains(attacker)) {
                toTransport.add(attacker);
            }
        }

        // Save each player's return position, then teleport them to Torment
        for (ServerPlayer player : toTransport) {
            TormentHelper.sendToTorment(player, tormentLevel);
        }

        // Restore the old fixed phase-2 spawn deeper in the maze at half health.
        BlockPos phase2Pos = TormentHelper.getBossPos(tormentLevel);
        torment.getChunkAt(phase2Pos);
        AABB phase2Band = new AABB(phase2Pos).inflate(1.0, 3.0, 1.0);
        boolean existingPhase2 = !torment.getEntitiesOfClass(LordOfTormentEntity.class, phase2Band).isEmpty();
        if (!existingPhase2) {
            LordOfTormentEntity phase2 = WitcheryEntities.LORD_OF_TORMENT.get().create(torment, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (phase2 != null) {
                phase2.setPos(phase2Pos.getX() + 0.5, phase2Pos.getY(), phase2Pos.getZ() + 0.5);
                phase2.finalizeSpawn(torment, torment.getCurrentDifficultyAt(phase2Pos),
                        EntitySpawnReason.MOB_SUMMONED, null);
                phase2.setPersistenceRequired();
                phase2.setHealth((float) (phase2.getMaxHealth() * 0.5F));
                torment.addFreshEntity(phase2);
            }
        }

        this.discard();
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // 2 random enchanted books
        this.spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(Items.ENCHANTED_BOOK));
        this.spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(Items.ENCHANTED_BOOK));
        // Demon Heart + Soul of Torment
        this.spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.DEMON_HEART.get()));
        this.spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.SOUL_OF_TORMENT.get()));
    }

    // ── Fire immunity ─────────────────────────────────────────────────────────

    @Override
    public boolean fireImmune() { return true; }

    // ── Model animation helper ────────────────────────────────────────────────

    public int getAttackTimer() { return attackTimer; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TeleportTriggered", teleportTriggered);
        tag.putString("Attackers", attackerUuids.stream().map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(",")));
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        teleportTriggered = tag.getBooleanOr("TeleportTriggered", false);
        String atk = tag.getStringOr("Attackers", "");
        if (!atk.isEmpty()) for (String s : atk.split(",")) {
            try { attackerUuids.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }
}
