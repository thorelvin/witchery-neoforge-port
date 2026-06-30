package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shade of Leonard — source {@code EntityLeonard} (1.7.10-0.24.1).
 *
 * <p>A demon so powerful it perceives no physical being as a threat: summoned by a coven's
 * cauldron, it is neutral until struck, then a skilled brewer turns its whole arsenal on the
 * attackers. It is immune to everything but a player's own blows (and even those are heavily
 * capped), regenerates, lays the lethal {@link WitcheryMobEffects#MORTAL_COIL Death Coil} on
 * nearby players, hurls Mystic Branch spells, clears the field of brew clouds, and — below half
 * health — shields itself behind waves of {@link LostSoulEntity Lost Souls} while it cannot be
 * harmed. Defeating it lifts the Death Coil from everyone in range.</p>
 */
public class LeonardEntity extends Monster implements RangedAttackMob {

    private static final int   SPAWN_INVULNERABLE_TICKS = 150;
    private static final int   MORTAL_COIL_DURATION = 90 * 20;
    private static final int   HEX_DURATION = 60 * 20;
    private static final double HEX_RADIUS = 40.0;
    private static final double HEX_RADIUS_SQ = HEX_RADIUS * HEX_RADIUS;
    private static final int   ANTI_BREW_RADIUS = 4;
    private static final int   SOUL_BOX_R = 15;
    private static final int   SOUL_BOX_RY = 5;
    private static final int   SOUL_SPAWN_DELAY = 10;
    private static final float SOURCE_DEMONBANE_HEALTH_GATE = 0.4F;
    private static final float SOURCE_DEMONBANE_DAMAGE_CAP = 15.0F;
    private static final long ATTENTION_PENDING_TICKS = 20L * 20L;
    private static final long ATTENTION_AFTER_REPLY_TICKS = 35L * 20L;
    private static final long ATTENTION_BRIEF_TICKS = 12L * 20L;
    private static final double ATTENTION_MAX_DISTANCE_SQR = 18.0 * 18.0;

    private static final Identifier GIANT_SCALE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "leonard.giant_scale");

    private static final EntityDataAccessor<Integer> ATTACK_TIMER =
            SynchedEntityData.defineId(LeonardEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.leonard"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int invulnerableStartTicks;
    private long lifetime;
    private int soulSpawnDelay = SOUL_SPAWN_DELAY;
    private boolean large;
    @Nullable
    private UUID attentionPlayer;
    private long attentionUntil;

    public LeonardEntity(EntityType<? extends LeonardEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 1.0)   // melee uses a fixed 7+rand(15); see doHurtTarget
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_TIMER, 0);
    }

    @Override
    protected void registerGoals() {
        // Source: a ranged spell-caster — no melee-pursuit goal; it kites and casts.
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 60, 30.0F));
        goalSelector.addGoal(3, new PlayerAttentionGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Neutral: only retaliates when attacked.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public int getAttackTimer() {
        return entityData.get(ATTACK_TIMER);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Dramatic, source-faithful summon: a short invulnerable phase as the Shade coalesces. */
    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason == EntitySpawnReason.MOB_SUMMONED || reason == EntitySpawnReason.SPAWN_ITEM_USE
                || reason == EntitySpawnReason.COMMAND || reason == EntitySpawnReason.EVENT) {
            setInvulnerableStart();
        }
        return result;
    }

    public void setInvulnerableStart() {
        invulnerableStartTicks = SPAWN_INVULNERABLE_TICKS;
        setHealth(getMaxHealth() / 4.0F);
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

    @Override
    public void tick() {
        super.tick();
        int timer = getAttackTimer();
        if (timer > 0) {
            entityData.set(ATTACK_TIMER, timer - 1);
        }
        if (level() instanceof ServerLevel serverLevel) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
            serverAiTick(serverLevel);
        }
        Player player = activeAttentionPlayer();
        if (player != null) {
            stopMovingForAttention();
            faceAttentionPlayer(player);
        }
    }

    // ── Server AI tick (source func_70619_bc) ──────────────────────────────────

    private void serverAiTick(ServerLevel level) {
        if (invulnerableStartTicks > 0) {
            // Coalescing: freeze offence and heal in. Goals are suppressed by dropping any target.
            invulnerableStartTicks--;
            setTarget(null);
            getNavigation().stop();
            if (tickCount % 10 == 0) {
                heal(getMaxHealth() * 0.75F / 15.0F);
            }
            if (invulnerableStartTicks <= 0) {
                level.levelEvent(1013, blockPosition(), 0); // Wither-spawn ambience
            }
            return;
        }

        lifetime++;

        if (tickCount % 20 == 0) {
            heal(1.0F);
            doMortalCoilHex(level);
        }
        if (tickCount % 20 == 2) {
            doAntiBrewSphere(level);
            doLowHealthBehaviour(level);
        }
    }

    private boolean hasCombatInterest() {
        return getTarget() != null || getLastHurtByMob() != null;
    }

    public boolean isInCombat() {
        return hasCombatInterest();
    }

    public boolean isCoalescing() {
        return invulnerableStartTicks > 0;
    }

    public boolean isLargeForm() {
        return large;
    }

    public boolean hasNearbyLostSouls(ServerLevel level) {
        return soulsShielding(level);
    }

    public void beginPlayerConversation(Player player) {
        holdPlayerAttention(player, ATTENTION_PENDING_TICKS, true);
    }

    public void completePlayerConversation(Player player) {
        holdPlayerAttention(player, ATTENTION_AFTER_REPLY_TICKS, false);
    }

    public void nudgePlayerAttention(Player player) {
        holdPlayerAttention(player, ATTENTION_BRIEF_TICKS, true);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), WitcherySoundEvents.ENTITY_LEONARD_SAY.get(),
                    SoundSource.HOSTILE, 0.42F, 0.82F);
        }
    }

    private void holdPlayerAttention(Player player, long ticks, boolean extendOnly) {
        if (level().isClientSide() || player == null || !player.isAlive() || player.level() != level()) {
            return;
        }
        faceAttentionPlayer(player);
        if (hasCombatInterest()) {
            return;
        }
        long until = level().getGameTime() + ticks;
        if (extendOnly && player.getUUID().equals(attentionPlayer) && attentionUntil >= until) {
            stopMovingForAttention();
            return;
        }
        attentionPlayer = player.getUUID();
        attentionUntil = until;
        stopMovingForAttention();
    }

    @Nullable
    private Player activeAttentionPlayer() {
        if (attentionPlayer == null || level().getGameTime() > attentionUntil || hasCombatInterest()) {
            clearAttention();
            return null;
        }
        Player player = level().getPlayerByUUID(attentionPlayer);
        if (player == null || !player.isAlive() || player.level() != level()
                || distanceToSqr(player) > ATTENTION_MAX_DISTANCE_SQR) {
            clearAttention();
            return null;
        }
        return player;
    }

    private void clearAttention() {
        attentionPlayer = null;
        attentionUntil = 0L;
    }

    private void stopMovingForAttention() {
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }

    private void faceAttentionPlayer(Player player) {
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
    }

    private final class PlayerAttentionGoal extends Goal {
        PlayerAttentionGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return activeAttentionPlayer() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return activeAttentionPlayer() != null;
        }

        @Override
        public void start() {
            stopMovingForAttention();
        }

        @Override
        public void stop() {
            if (activeAttentionPlayer() == null) {
                clearAttention();
            }
        }

        @Override
        public void tick() {
            Player player = activeAttentionPlayer();
            if (player == null) {
                return;
            }
            stopMovingForAttention();
            faceAttentionPlayer(player);
        }
    }

    // ── Mortal Coil hex + random hexes ─────────────────────────────────────────

    private void doMortalCoilHex(ServerLevel level) {
        if (random.nextInt(5) != 0 || !hasCombatInterest()) {
            return;
        }
        AABB box = new AABB(blockPosition()).inflate(HEX_RADIUS, HEX_RADIUS, HEX_RADIUS);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        boolean hexed = false;

        for (Player player : players) {
            if (isHexablePlayer(player) && !player.hasEffect(WitcheryMobEffects.MORTAL_COIL)) {
                hexed = true;
                applyIncurable(player, WitcheryMobEffects.MORTAL_COIL, MORTAL_COIL_DURATION, 0);
                level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getEyeY(), player.getZ(),
                        12, 0.3, 0.4, 0.3, 0.0);
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 0.6F, 1.4F);
            }
        }

        if (hexed) {
            spellBurst(level, 0x009900); // source green Death Coil note burst
        } else if (random.nextInt(5) == 1 && !players.isEmpty()) {
            Player victim = players.get(random.nextInt(players.size()));
            if (isHexablePlayer(victim)) {
                castRandomHex(level, victim);
            }
        }
    }

    private boolean isHexablePlayer(Player player) {
        return player.isAlive() && !player.isRemoved() && player.getHealth() > 0.0F
                && !player.isSpectator()
                && distanceToSqr(player.getX(), getY(), player.getZ()) <= HEX_RADIUS_SQ;
    }

    private void castRandomHex(ServerLevel level, Player victim) {
        level.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getEyeY(), victim.getZ(),
                12, 0.3, 0.4, 0.3, 0.0);
        switch (random.nextInt(10)) {
            case 0, 1, 2 -> stripBeneficialEffects(victim); // source mob_spell only, no colour burst
            case 3, 4, 5 -> {
                applyIncurable(victim, WitcheryMobEffects.SINKING, HEX_DURATION, 3);
                spellBurst(level, 0x990000); // source dark-red burst
            }
            case 6, 7, 8 -> {
                applyIncurable(victim, WitcheryMobEffects.INSANITY, HEX_DURATION, 3);
                spellBurst(level, 0x000099); // source blue burst
            }
            default -> {
                applyIncurable(victim, WitcheryMobEffects.OVERHEATING, HEX_DURATION, 3);
                spellBurst(level, 0x009999); // source teal burst
            }
        }
    }

    /** A coloured note-harp flourish at the Shade as it lands a hex (source SPELL_COLORED + NOTE_HARP). */
    private void spellBurst(ServerLevel level, int rgb) {
        level.sendParticles(new DustParticleOptions(rgb, 1.2F), getX(), getY() + 1.2, getZ(),
                18, 0.4, 0.5, 0.4, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void stripBeneficialEffects(Player victim) {
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance instance : victim.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                toRemove.add(instance.getEffect());
            }
        }
        for (Holder<MobEffect> effect : toRemove) {
            victim.removeEffect(effect);
        }
    }

    /**
     * Source effects are {@code setIncurable()}. This port (consistent with its other incurable
     * Witchery effects such as Diseased/Paralysed) does not yet milk-gate incurability, so the
     * hex is applied as a normal effect; Death Coil's danger still holds in normal play and is
     * lifted on the Shade's death.
     */
    private static void applyIncurable(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    // ── Anti-brew sphere (source BlockActionSphere) ─────────────────────────────

    private void doAntiBrewSphere(ServerLevel level) {
        if (random.nextInt(5) != 0) {
            return;
        }
        BlockPos centre = BlockPos.containing(getX(), getY() + 2, getZ());
        int rSq = ANTI_BREW_RADIUS * ANTI_BREW_RADIUS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -ANTI_BREW_RADIUS; dx <= ANTI_BREW_RADIUS; dx++) {
            for (int dy = -ANTI_BREW_RADIUS; dy <= ANTI_BREW_RADIUS; dy++) {
                for (int dz = -ANTI_BREW_RADIUS; dz <= ANTI_BREW_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) {
                        continue;
                    }
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    var block = level.getBlockState(pos).getBlock();
                    if (block == com.emoniph.witchery.init.WitcheryBlocks.BREW_GAS.get()
                            || block == com.emoniph.witchery.init.WitcheryBlocks.BREW_LIQUID.get()) {
                        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    // ── Low-health behaviour: giant phase + Lost Soul waves ─────────────────────

    private void doLowHealthBehaviour(ServerLevel level) {
        if (getHealth() >= getMaxHealth() * 0.5F || !hasCombatInterest()) {
            return;
        }
        if (getHealth() < getMaxHealth() * 0.25F && random.nextInt(3) == 1 && !large) {
            enterGiantForm();
        }

        AABB soulBox = new AABB(blockPosition()).inflate(SOUL_BOX_R, SOUL_BOX_RY, SOUL_BOX_R);
        List<LostSoulEntity> souls = level.getEntitiesOfClass(LostSoulEntity.class, soulBox);
        if (souls.isEmpty() && --soulSpawnDelay <= 0) {
            removeCoilEffects(level, SOUL_BOX_R, SOUL_BOX_RY);
            soulSpawnDelay = SOUL_SPAWN_DELAY;
            spawnSoulWave(level);
        }
    }

    private void enterGiantForm() {
        large = true;
        AttributeInstance scale = getAttribute(Attributes.SCALE);
        if (scale != null && scale.getModifier(GIANT_SCALE_MODIFIER_ID) == null) {
            scale.addPermanentModifier(new AttributeModifier(
                    GIANT_SCALE_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        applyIncurable(this, WitcheryMobEffects.RESIZING, HEX_DURATION, 3); // source RESIZING IV marker
    }

    private void spawnSoulWave(ServerLevel level) {
        int count = 4 + random.nextInt(2);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnLostSoul(level)) {
                spawned++;
            }
        }
        for (int i = spawned; i < 3; i++) {
            spawnLostSoul(level);
        }
    }

    private boolean spawnLostSoul(ServerLevel level) {
        LostSoulEntity soul = WitcheryEntities.LOST_SOUL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (soul == null) {
            return false;
        }
        soul.setSoulType(random.nextInt(3));
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 2.0 + random.nextDouble() * 3.0;
        soul.setPos(getX() + Math.cos(angle) * radius, getY() + 1.0, getZ() + Math.sin(angle) * radius);
        soul.finalizeSpawn(level, level.getCurrentDifficultyAt(soul.blockPosition()),
                EntitySpawnReason.MOB_SUMMONED, null);
        LivingEntity target = getTarget();
        if (target != null) {
            soul.setTarget(target);
        }
        level.sendParticles(ParticleTypes.SMOKE, soul.getX(), soul.getY() + 0.3, soul.getZ(),
                10, 0.2, 0.3, 0.2, 0.01);
        level.playSound(null, soul.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                SoundSource.HOSTILE, 0.8F, 0.6F);
        return level.addFreshEntity(soul);
    }

    /** Clear the Death Coil from every nearby player (on soul spawn, and on death). */
    private void removeCoilEffects(ServerLevel level, int r, int ry) {
        AABB box = new AABB(blockPosition()).inflate(r, ry, r);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player.isAlive() && player.hasEffect(WitcheryMobEffects.MORTAL_COIL)) {
                player.removeEffect(WitcheryMobEffects.MORTAL_COIL);
            }
        }
    }

    // ── Damage rules ────────────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (invulnerableStartTicks > 0 || soulsShielding(level)) {
            return false;
        }
        // A demon untouchable by the mortal world: only a player's own melee blow can harm it.
        // Source gates on the "player" damage type, so arrows and other projectiles bounce off.
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        float cap;
        if (getHealth() >= getMaxHealth() * 0.25F) {
            cap = 12.0F;
        } else {
            cap = large ? 1.0F : 4.0F;
        }
        return super.hurtServer(level, source, Math.min(amount, cap));
    }

    private boolean soulsShielding(ServerLevel level) {
        AABB soulBox = new AABB(blockPosition()).inflate(SOUL_BOX_R, SOUL_BOX_RY, SOUL_BOX_R);
        return !level.getEntitiesOfClass(LostSoulEntity.class, soulBox).isEmpty();
    }

    /** Demonbane brew's Weakness path — bypasses the player-only gate (source attackEntityFromWeakness). */
    public boolean hurtFromDemonbaneWeakness(ServerLevel level, int damage) {
        if (damage <= 0 || invulnerableStartTicks > 0 || soulsShielding(level)
                || !sourceDemonbaneWeaknessCanHurt(getHealth(), getMaxHealth())) {
            return false;
        }
        return super.hurtServer(level, level.damageSources().magic(),
                Math.min((float) damage, sourceDemonbaneWeaknessDamageCap(large)));
    }

    public static boolean sourceDemonbaneWeaknessCanHurt(float health, float maxHealth) {
        return health < maxHealth * SOURCE_DEMONBANE_HEALTH_GATE;
    }

    public static float sourceDemonbaneWeaknessDamageCap(boolean largeResizing) {
        return largeResizing ? 8.0F : SOURCE_DEMONBANE_DAMAGE_CAP;
    }

    // ── Melee (source func_70652_k) ─────────────────────────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        entityData.set(ATTACK_TIMER, 10);
        this.level().broadcastEntityEvent(this, (byte) 4);
        float damage = 7.0F + random.nextInt(15);
        boolean hit = target.hurtServer(level, damageSources().mobAttack(this), damage);
        if (hit && target instanceof LivingEntity living) {
            living.setDeltaMovement(living.getDeltaMovement().add(0.0, 0.4, 0.0));
            living.hurtMarked = true;
        }
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return hit;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        beginPlayerConversation(player);
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.LEONARD, this,
                hasCombatInterest()
                        ? "Your violence is already speaking."
                        : "A bargain dragged me here. Speak, if you must.");
        boolean queued = player instanceof ServerPlayer serverPlayer
                && NpcBrainService.INSTANCE.requestLeonardGreeting(this, serverPlayer, fallback);
        if (!queued) {
            player.displayClientMessage(fallback, false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            entityData.set(ATTACK_TIMER, 10);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ── Ranged spell attack (source func_82196_d) ───────────────────────────────

    private static final SymbolEffect[] SPELLS;
    private static final int[] SPELL_WEIGHTS;
    private static final int SPELL_WEIGHT_TOTAL;

    static {
        SPELLS = new SymbolEffect[]{
                EffectRegistry.IGNIANIMA, EffectRegistry.EXPELLIARMUS,
                EffectRegistry.FLIPENDO, EffectRegistry.IMPEDIMENTA, EffectRegistry.CONFUNDUS};
        SPELL_WEIGHTS = new int[]{14, 2, 2, 2, 1};
        int total = 0;
        for (int weight : SPELL_WEIGHTS) {
            total += weight;
        }
        SPELL_WEIGHT_TOTAL = total;
    }

    private SymbolEffect pickSpell() {
        int roll = random.nextInt(SPELL_WEIGHT_TOTAL);
        for (int i = 0; i < SPELLS.length; i++) {
            roll -= SPELL_WEIGHTS[i];
            if (roll < 0) {
                return SPELLS[i];
            }
        }
        return EffectRegistry.IGNIANIMA;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        // Source: only a coin-flip chance to actually loose a spell on each attack window.
        if (!random.nextBoolean() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        entityData.set(ATTACK_TIMER, 10);
        this.level().broadcastEntityEvent(this, (byte) 4);

        double dx = target.getX() - getX();
        double dy = (target.getBoundingBox().minY + target.getBbHeight() / 2.0) - (getY() + getBbHeight() / 2.0);
        double dz = target.getZ() - getZ();

        SpellEffectEntity orb = new SpellEffectEntity(serverLevel, this, pickSpell(), 1);
        orb.setPos(getX(), getY() + getBbHeight() / 2.0, getZ());
        orb.shoot(dx, dy, dz, 1.0F, 6.0F);
        serverLevel.addFreshEntity(orb);
        serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.6, getZ(),
                8, 0.25, 0.25, 0.25, 0.0);
        serverLevel.playSound(null, blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE,
                1.0F, 1.0F);
    }

    // ── Death ───────────────────────────────────────────────────────────────────

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel serverLevel) {
            removeCoilEffects(serverLevel, 40, 40); // winners do not die to the Coil
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, randomEnchantedBook(level));
        spawnAtLocation(level, new ItemStack(WitcheryItems.DEMON_HEART.get()));
        spawnAtLocation(level, new ItemStack(WitcheryItems.LEONARDS_URN.get()));
    }

    private ItemStack randomEnchantedBook(ServerLevel level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        try {
            var lookup = level.registryAccess().lookup(Registries.ENCHANTMENT);
            if (lookup.isPresent()) {
                List<Holder.Reference<Enchantment>> all = lookup.get().listElements().toList();
                if (!all.isEmpty()) {
                    Holder<Enchantment> enchantment = all.get(random.nextInt(all.size()));
                    int lvl = Mth.nextInt(random, 1, Math.max(1, enchantment.value().getMaxLevel()));
                    book.enchant(enchantment, lvl);
                }
            }
        } catch (RuntimeException ignored) {
            // a missing enchantment registry must never block the drop
        }
        return book;
    }

    // ── Sounds (authentic source mob.leonard.say/hit/death) ──────────────────────

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.ENTITY_LEONARD_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.ENTITY_LEONARD_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.ENTITY_LEONARD_DEATH.get();
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Invul", invulnerableStartTicks);
        tag.putLong("Lifetime", lifetime);
        tag.putBoolean("Large", large);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        invulnerableStartTicks = tag.getIntOr("Invul", 0);
        lifetime = tag.getLongOr("Lifetime", 0L);
        large = tag.getBooleanOr("Large", false);
    }
}
