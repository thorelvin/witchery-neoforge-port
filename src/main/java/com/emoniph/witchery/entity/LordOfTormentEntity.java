package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.world.TormentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
    private final Set<UUID> tormentParticipants = new HashSet<>();
    private UUID tormentSessionId;

    public LordOfTormentEntity(EntityType<? extends LordOfTormentEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 80;
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

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        navigation.setCanFloat(true);
        return navigation;
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

        if (level().isClientSide()) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX() - getBbWidth() * 0.5D + random.nextDouble() * getBbWidth(),
                    getY() + 0.1D + random.nextDouble() * 2.0D,
                    getZ() - getBbWidth() * 0.5D + random.nextDouble() * getBbWidth(),
                    0.0D, 0.0D, 0.0D);
        }

        if (!level().isClientSide()) {
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            // Random Blindness on target (5% chance, 10 sec, amp 5)
            LivingEntity target = this.getTarget();
            if (target instanceof Player player && this.random.nextDouble() < 0.05
                    && player.getAbilities().flying && !player.hasEffect(MobEffects.BLINDNESS)) {
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

        level().broadcastEntityEvent(this, (byte) 4);
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);

        int count = this.random.nextInt(10) == 0 ? 9 : 3;
        double spread = Mth.sqrt(power) * 0.5;

        SpellEffectEntity ignianima = new SpellEffectEntity(level(), this, EffectRegistry.IGNIANIMA, 1);
        ignianima.setPos(getX(), getY() + getEyeHeight() * 0.5D, getZ());
        ignianima.shoot(target.getX() - getX(), target.getEyeY() - getEyeY(), target.getZ() - getZ(),
                1.05F, 2.0F);
        level().addFreshEntity(ignianima);

        for (int i = 0; i < count; i++) {
            double dx = target.getX() - this.getX() + this.random.nextGaussian() * spread;
            double dy = target.getEyeY() - (this.getY() + this.getEyeHeight()) + this.random.nextGaussian() * 0.1;
            double dz = target.getZ() - this.getZ() + this.random.nextGaussian() * spread;

            SoulfireProjectile fireball = new SoulfireProjectile(level(), this, dx, dy, dz);
            fireball.setPos(this.getX(), this.getY() + this.getEyeHeight() + 0.5, this.getZ());
            level().addFreshEntity(fireball);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackTimer = 10;
        } else {
            super.handleEntityEvent(id);
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
            teleportTriggered = triggerTormentTeleport(serverLvl);
        }

        return result;
    }

    private boolean triggerTormentTeleport(ServerLevel serverLevel) {
        ServerLevel torment = serverLevel.getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) return false;
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
        if (toTransport.isEmpty()) return false;

        BlockPos phase2Pos = TormentHelper.getBossPos(tormentLevel);
        torment.getChunkAt(phase2Pos);
        AABB phase2Band = new AABB(phase2Pos).inflate(1.0, 3.0, 1.0);
        List<LordOfTormentEntity> existing = torment.getEntitiesOfClass(
                LordOfTormentEntity.class, phase2Band, LordOfTormentEntity::isAlive);
        LordOfTormentEntity phase2 = existing.isEmpty() ? null : existing.getFirst();
        boolean createdPhase2 = phase2 == null;
        if (phase2 == null) {
            phase2 = WitcheryEntities.LORD_OF_TORMENT.get().create(torment, EntitySpawnReason.MOB_SUMMONED);
            if (phase2 == null) return false;
            phase2.setPos(phase2Pos.getX() + 0.5, phase2Pos.getY(), phase2Pos.getZ() + 0.5);
            phase2.finalizeSpawn(torment, torment.getCurrentDifficultyAt(phase2Pos),
                    EntitySpawnReason.MOB_SUMMONED, null);
            phase2.setPersistenceRequired();
            phase2.setHealth((float) (phase2.getMaxHealth() * 0.5F));
            if (!torment.addFreshEntity(phase2)) return false;
        }

        UUID sessionId = phase2.ensureTormentSession();
        List<ServerPlayer> transferred = new ArrayList<>();
        for (ServerPlayer player : toTransport) {
            if (TormentHelper.sendToTorment(player, tormentLevel, sessionId)) {
                transferred.add(player);
            }
        }
        if (transferred.isEmpty()) {
            if (createdPhase2) phase2.discard();
            return false;
        }
        phase2.addTormentParticipants(transferred);
        this.discard();
        return true;
    }

    private UUID ensureTormentSession() {
        if (tormentSessionId == null) tormentSessionId = UUID.randomUUID();
        return tormentSessionId;
    }

    private void addTormentParticipants(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            tormentParticipants.add(player.getUUID());
            attackerUuids.add(player.getUUID());
        }
    }

    public UUID getTormentSessionId() {
        return tormentSessionId;
    }

    public Set<UUID> getTormentParticipants() {
        return Set.copyOf(tormentParticipants);
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        this.spawnAtLocation(level, createSourceEnchantedBook(level, 3));
        this.spawnAtLocation(level, createSourceEnchantedBook(level, 1));
        this.spawnAtLocation(level, new ItemStack(WitcheryItems.DEMON_HEART.get()));
        this.spawnAtLocation(level, new ItemStack(WitcheryItems.SOUL_OF_TORMENT.get()));
    }

    private ItemStack createSourceEnchantedBook(ServerLevel level, int levelOffset) {
        List<Holder<Enchantment>> choices = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(EnchantmentTags.ON_RANDOM_LOOT)
                .stream()
                .toList();
        if (choices.isEmpty()) return new ItemStack(Items.ENCHANTED_BOOK);

        Holder<Enchantment> enchantment = choices.get(random.nextInt(choices.size()));
        int maximum = enchantment.value().getMaxLevel();
        int minimum = Math.min(enchantment.value().getMinLevel() + levelOffset, maximum);
        int enchantmentLevel = Mth.nextInt(random, minimum, maximum);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(enchantment, enchantmentLevel));
        return book;
    }

    // ── Fire immunity ─────────────────────────────────────────────────────────

    @Override
    public boolean fireImmune() { return true; }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return WitcherySoundEvents.MOB_TORMENT_LAUGH.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return WitcherySoundEvents.MOB_TORMENT_HIT.get(); }
    @Override protected SoundEvent getDeathSound() { return WitcherySoundEvents.MOB_TORMENT_DEATH.get(); }
    @Override public int getAmbientSoundInterval() { return 200; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ── Model animation helper ────────────────────────────────────────────────

    public int getAttackTimer() { return attackTimer; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("TeleportTriggered", teleportTriggered);
        tag.putString("Attackers", attackerUuids.stream().map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(",")));
        tag.putString("TormentSession", tormentSessionId == null ? "" : tormentSessionId.toString());
        tag.putString("TormentParticipants", tormentParticipants.stream().map(UUID::toString)
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
        String session = tag.getStringOr("TormentSession", "");
        if (!session.isEmpty()) {
            try { tormentSessionId = UUID.fromString(session); } catch (IllegalArgumentException ignored) {}
        }
        String participants = tag.getStringOr("TormentParticipants", "");
        if (!participants.isEmpty()) for (String value : participants.split(",")) {
            try { tormentParticipants.add(UUID.fromString(value)); } catch (IllegalArgumentException ignored) {}
        }
    }
}
