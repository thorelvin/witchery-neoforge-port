package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Baba Yaga — 500 HP boss witch. Source parity: EntityBabaYaga 1.7.10-0.24.1.
 *
 * - Throws approximated witch brews (1/3) or vanilla splash potions (2/3) at range
 * - Caps incoming damage at 15; magic damage reduced to 15%
 * - Reflects 25% physical damage back to player attackers (wooden attacks exempt)
 * - Spawns cave spiders when hit by undead or untamed pets
 * - Teleports toward target (~2% chance/tick when chasing)
 * - Applies Blindness V to falling/sprinting targets (5% chance/tick)
 * - Ownership: despawns after 600 ticks, drops witch ingredients near owner
 * - Drops: witch ingredients, enchanted book, Baba's Hat (guaranteed)
 */
public class BabaYagaEntity extends Monster implements RangedAttackMob {
    public static final int FRIENDLY_VISIT_TICKS = 20 * 60 * 3;
    public static final int FRIENDLY_GIFT_INTERVAL_TICKS = 100;
    public static final int FRIENDLY_GIFT_LIMIT = 6;

    private static final String FRIENDLY_ARRIVAL_LINE =
            "Curiosity brought me, %s. Do not waste it.";
    private static final String FRIENDLY_DEPARTURE_LINE =
            "Enough. Keep your bargains clean; next time, curiosity may bring teeth.";
    private static final String[] FRIENDLY_GIFT_LINES = {
            "Take this. Even scraps remember old magic.",
            "A little payment for interesting dreams.",
            "Keep it close. Useful things have sharp edges.",
            "There. The pot says you will need that.",
            "Do not thank me yet.",
            "Last gift, %s. Curiosity is not charity."
    };

    // ── Potion self-drinking state ────────────────────────────────────────────
    private boolean isDrinking = false;
    private int drinkTimer    = 0;
    @Nullable private MobEffectInstance pendingDrinkEffect = null;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.baba_yaga"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private long ticksSinceTeleport = 0L;
    @Nullable private String ownerName = null;
    private int ownerVisitTicks = 0;
    private int ownerGiftDrops = 0;

    private static final String[] WITCH_DROP_IDS = {
        "spectral_dust", "bat_wool", "toe_of_frog", "owlets_wing",
        "wood_ash", "exhale_of_horned_one", "whiff_of_magic"
    };

    public BabaYagaEntity(EntityType<? extends BabaYagaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     500.0)
                .add(Attributes.MOVEMENT_SPEED,   0.3)
                .add(Attributes.ARMOR,            4.0)
                .add(Attributes.FOLLOW_RANGE,    40.0);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        // Custom ranged goal — RangedBowAttackGoal requires a BowItem in hand,
        // but Baba Yaga holds an enchanted broom. This goal fires performRangedAttack
        // directly every 60 ticks when a target is in range (source parity: EntityAIArrowAttack).
        goalSelector.addGoal(2, potionAttackGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 0, false, true,
                (entity, sl) -> ownerName == null || !entity.getName().getString().equalsIgnoreCase(ownerName)));
    }

    /** Fires performRangedAttack every 60 ticks when target is within 10 blocks — bypasses bow check. */
    private Goal potionAttackGoal() {
        return new Goal() {
            private int cooldown = 0;

            { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }

            @Override public boolean canUse() {
                return getTarget() != null && getTarget().isAlive();
            }
            @Override public boolean canContinueToUse() { return canUse(); }

            @Override public void start() { cooldown = 20; }

            @Override
            public void tick() {
                LivingEntity target = getTarget();
                if (target == null) return;
                getLookControl().setLookAt(target, 30, 30);
                double dist = distanceTo(target);

                // Walk toward target if too far, back off if too close
                if (dist > 10.0) {
                    getNavigation().moveTo(target, 1.0);
                } else if (dist < 5.0) {
                    getNavigation().moveTo(
                        getX() - (target.getX() - getX()),
                        getY(),
                        getZ() - (target.getZ() - getZ()),
                        1.0);
                } else {
                    getNavigation().stop();
                }

                if (--cooldown <= 0 && hasLineOfSight(target) && dist < 15.0) {
                    cooldown = 60;
                    performRangedAttack(target, 1.0f);
                }
            }
        };
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
        if (level().isClientSide()) return;

        bossEvent.setProgress(getHealth() / getMaxHealth());

        // ── Witch self-potion-drinking mechanic (source: EntityBabaYaga.func_70636_d) ──
        if (isDrinking) {
            drinkTimer--;
            if (drinkTimer <= 0) {
                isDrinking = false;
                if (pendingDrinkEffect != null) {
                    addEffect(pendingDrinkEffect);
                    pendingDrinkEffect = null;
                }
                removeEffect(MobEffects.SLOWNESS); // remove drinking speed penalty
                playSound(SoundEvents.GENERIC_DRINK.value(), 1.0f, 1.0f);
            }
        } else {
            // Decide whether to drink a self-potion this tick
            LivingEntity tgt = getTarget();
            int potionChoice = -1;
            if (random.nextFloat() < 0.15F && isOnFire() && !hasEffect(MobEffects.FIRE_RESISTANCE)) {
                potionChoice = 0; // fire resistance
            } else if (random.nextFloat() < 0.01F && getHealth() < getMaxHealth()) {
                potionChoice = 1; // healing
            } else if (random.nextFloat() < 0.25F && tgt != null
                    && !hasEffect(MobEffects.SPEED)
                    && distanceToSqr(tgt) > 121.0) {
                potionChoice = 2; // speed
            }
            if (potionChoice >= 0) {
                startDrinking(potionChoice);
            }
        }

        LivingEntity target = getTarget();

        // Teleport toward target
        if (target != null && random.nextDouble() < 0.02
                && (long) tickCount - ticksSinceTeleport > 100L) {
            ticksSinceTeleport = tickCount;
            teleportTowardTarget(target);
        }

        // Blindness on falling/sprinting targets
        if (target != null && random.nextDouble() < 0.05
                && (target.isFallFlying() || (target instanceof Player p && p.isSprinting()))
                && !target.hasEffect(MobEffects.BLINDNESS)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 5));
        }

        // Ownership mechanic
        if (ownerName != null && level() instanceof ServerLevel sl) {
            ownerVisitTicks++;
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayerByName(ownerName);
            if (owner != null && getTarget() == owner) {
                setTarget(null);
            }
            if (owner != null
                    && distanceTo(owner) < 64
                    && ownerGiftDrops < FRIENDLY_GIFT_LIMIT
                    && ownerVisitTicks % FRIENDLY_GIFT_INTERVAL_TICKS == 0) {
                dropFriendlyVisitGift(sl, owner);
            }
            if (ownerVisitTicks >= FRIENDLY_VISIT_TICKS) {
                if (owner != null && distanceTo(owner) < 96) {
                    sayToOwner(owner, FRIENDLY_DEPARTURE_LINE);
                }
                farewellEffects(sl);
                discard();
            }
        }
    }

    /** Start a potion-drinking phase. Source: EntityBabaYaga (choices 0/1/2). */
    private void startDrinking(int choice) {
        isDrinking = true;
        drinkTimer = 40;
        pendingDrinkEffect = switch (choice) {
            case 0 -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0);
            case 1 -> new MobEffectInstance(MobEffects.INSTANT_HEALTH,             1,   1);
            default -> new MobEffectInstance(MobEffects.SPEED,  600, 1);
        };
        addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0));
        playSound(SoundEvents.GENERIC_DRINK.value(), 0.5f, 1.0f);
    }

    private void teleportTowardTarget(LivingEntity target) {
        double range = 8.0;
        double nx = getX() + (random.nextDouble() - 0.5) * range;
        double ny = getY() + (random.nextInt(16) - 8);
        double nz = getZ() + (random.nextDouble() - 0.5) * range;
        boolean success = tryTeleportTo(nx, ny, nz);
        if (success) {
            gameEvent(GameEvent.TELEPORT);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    private boolean tryTeleportTo(double x, double y, double z) {
        double ox = getX(), oy = getY(), oz = getZ();
        setPos(x, y, z);
        if (!level().noCollision(this)) {
            setPos(ox, oy, oz);
            return false;
        }
        return true;
    }

    // ── Ranged attack — brews + splash potions ────────────────────────────────

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (level().isClientSide()) return;
        if (isDrinking) return; // source: "if (!this.getAggressive())" check

        List<MobEffectInstance> effects;
        if (random.nextInt(3) == 0) {
            // Witch brew approximations (1/3 of attacks)
            effects = switch (random.nextInt(12)) {
                case 0, 1  -> List.of(new MobEffectInstance(MobEffects.SLOWNESS, 80, 4));
                case 2, 3  -> List.of(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                case 4, 5  -> List.of(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                case 6, 7  -> List.of(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                case 8, 9  -> List.of(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
                case 10    -> List.of(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                default    -> List.of(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            };
        } else {
            // Vanilla potions by situation (2/3 of attacks)
            float dist = (float) distanceTo(target);
            if (dist >= 8.0f && !target.hasEffect(MobEffects.BLINDNESS)) {
                effects = List.of(new MobEffectInstance(MobEffects.SLOWNESS, 160, 1));
            } else if (dist <= 3.0f && !target.hasEffect(MobEffects.POISON) && random.nextFloat() < 0.25f) {
                effects = List.of(new MobEffectInstance(MobEffects.POISON, 160, 0));
            } else {
                effects = List.of(new MobEffectInstance(MobEffects.POISON, 80, 0));
            }
        }

        throwSplashPotion(target, effects);
    }

    private void throwSplashPotion(LivingEntity target, List<MobEffectInstance> effects) {
        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty()));

        ThrownSplashPotion thrown = new ThrownSplashPotion(level(), this, potionStack);
        thrown.setPos(getX(), getEyeY(), getZ());

        double dx = target.getX() + target.getDeltaMovement().x - getX();
        double dy = target.getEyeY() - 1.1 - getY();
        double dz = target.getZ() + target.getDeltaMovement().z - getZ();
        float flatDist = Mth.sqrt((float)(dx * dx + dz * dz));
        thrown.shoot(dx, dy + flatDist * 0.2, dz, 0.75F, 8.0F);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        level().addFreshEntity(thrown);
    }

    // ── Damage — cap, magic reduction, thorns, cave spiders ──────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Magic damage reduced to 15%
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 0.15f;
        }

        boolean result = super.hurtServer(level, source, Math.min(amount, 15.0f));
        if (!result) return false;

        Entity attacker = source.getEntity();

        // Reflect 25% damage back to player (not wooden attacks)
        if (attacker instanceof Player player) {
            boolean wooden = player.getMainHandItem().is(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS);
            if (!wooden) {
                player.hurtServer(level, level.damageSources().thorns(this), amount * 0.25f);
            }
        }

        // Spawn cave spider when hit by undead or untamed pets
        if (attacker instanceof LivingEntity living) {
            boolean isUndead = living.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
            boolean isUntamedPet = living instanceof TamableAnimal pet && !pet.isTame();
            if ((isUndead || isUntamedPet) && !living.isRemoved()) {
                CaveSpider spider = EntityType.CAVE_SPIDER.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (spider != null) {
                    spider.setPos(living.getX(), living.getY(), living.getZ());
                    spider.setTarget(getTarget());
                    level.addFreshEntity(spider);
                    living.discard();
                }
            }
        }

        return true;
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // 2-4 random stacks of witch ingredients
        int stackCount = 2 + random.nextInt(3);
        for (int i = 0; i < stackCount; i++) {
            String dropId = WITCH_DROP_IDS[random.nextInt(WITCH_DROP_IDS.length)];
            Item item = BuiltInRegistries.ITEM
                    .get(Identifier.fromNamespaceAndPath("witchery", dropId))
                    .map(net.minecraft.core.Holder.Reference::value)
                    .orElse(null);
            if (item != null && item != Items.AIR) {
                spawnAtLocation(level, new ItemStack(item, 1 + random.nextInt(3)));
            }
        }

        // Enchanted book (plain for now; enchant selection requires complex API)
        spawnAtLocation(level, new ItemStack(Items.ENCHANTED_BOOK));

        // Baba's Hat — guaranteed
        spawnAtLocation(level, new ItemStack(WitcheryItems.BABAS_HAT.get()));
    }

    // ── Ownership ─────────────────────────────────────────────────────────────

    private void dropFriendlyVisitGift(ServerLevel level, ServerPlayer owner) {
        String id = WITCH_DROP_IDS[random.nextInt(4)]; // first 4 are basic ingredients
        Item item = BuiltInRegistries.ITEM
                .get(Identifier.fromNamespaceAndPath("witchery", id))
                .map(net.minecraft.core.Holder.Reference::value)
                .orElse(null);
        if (item == null || item == Items.AIR) {
            return;
        }

        ItemStack stack = new ItemStack(item, 1 + random.nextInt(2));
        ItemEntity gift = new ItemEntity(level, getX(), getY() + 0.7D, getZ(), stack);
        gift.setDeltaMovement((random.nextDouble() - 0.5D) * 0.08D, 0.18D,
                (random.nextDouble() - 0.5D) * 0.08D);
        level.addFreshEntity(gift);

        ownerGiftDrops++;
        giftEffects(level);
        sayFriendlyGift(owner);
    }

    public void setOwner(String name) { this.ownerName = name; }
    @Nullable public String getOwnerName() { return ownerName; }

    public int getOwnerVisitTicks() { return ownerVisitTicks; }
    public int getOwnerGiftDrops() { return ownerGiftDrops; }

    public void beginFriendlyVisit(ServerPlayer owner) {
        setOwner(owner.getName().getString());
        ownerVisitTicks = 0;
        ownerGiftDrops = 0;
        setTarget(null);
        getNavigation().stop();
        getLookControl().setLookAt(owner, 30.0F, 30.0F);
        arrivalEffects((ServerLevel) owner.level());
        sayToOwner(owner, FRIENDLY_ARRIVAL_LINE.formatted(owner.getName().getString()));
    }

    private void sayFriendlyGift(ServerPlayer owner) {
        String line = FRIENDLY_GIFT_LINES[Math.min(ownerGiftDrops - 1, FRIENDLY_GIFT_LINES.length - 1)];
        if (line.contains("%s")) {
            line = line.formatted(owner.getName().getString());
        }
        sayToOwner(owner, line);
    }

    private void sayToOwner(ServerPlayer owner, String line) {
        owner.displayClientMessage(NpcBrainService.speechComponent(NpcPersonas.BABA_YAGA, this, line), false);
    }

    private void arrivalEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0D, getZ(),
                24, 0.45D, 0.7D, 0.45D, 0.05D);
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.6D, getZ(),
                32, 0.6D, 0.8D, 0.6D, 0.08D);
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.HOSTILE, 0.8F, 0.8F);
        playSound(SoundEvents.WITCH_AMBIENT, 0.9F, 0.75F + random.nextFloat() * 0.25F);
    }

    private void giftEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.8D, getZ(),
                12, 0.25D, 0.35D, 0.25D, 0.03D);
        level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 0.6D, getZ(),
                8, 0.35D, 0.35D, 0.35D, 0.0D);
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 0.45F, 0.8F + random.nextFloat() * 0.25F);
    }

    private void farewellEffects(ServerLevel level) {
        level.broadcastEntityEvent(this, (byte) 15);
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.8D, getZ(),
                48, 0.55D, 0.9D, 0.55D, 0.08D);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 0.9F, 0.8F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        getNavigation().stop();
        getLookControl().setLookAt(player, 30.0F, 30.0F);
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.BABA_YAGA, this,
                "Speak quickly. My pot is patient, but I am not.");
        boolean queued = player instanceof ServerPlayer serverPlayer
                && NpcBrainService.INSTANCE.requestBabaYagaGreeting(this, serverPlayer, fallback);
        if (!queued) {
            player.displayClientMessage(fallback, false);
        }
        return InteractionResult.CONSUME;
    }

    // ── Spawn init — wield enchanted broom ───────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        @javax.annotation.Nullable SpawnGroupData spawnGroupData) {
        // Baba Yaga wields an enchanted broom — her "pestle" weapon
        setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(WitcheryItems.ENCHANTED_BROOM.get()));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITCH_DEATH; }

    // ── Client events — witch magic particles ─────────────────────────────────

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 15) {
            for (int i = 0; i < random.nextInt(35) + 10; i++) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.WITCH,
                        getX() + random.nextGaussian() * 0.13,
                        getBoundingBox().maxY + 0.5 + random.nextGaussian() * 0.13,
                        getZ() + random.nextGaussian() * 0.13, 0, 0, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Owner", ownerName == null ? "" : ownerName);
        tag.putInt("OwnerVisitTicks", ownerVisitTicks);
        tag.putInt("OwnerGiftDrops", ownerGiftDrops);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String s = tag.getStringOr("Owner", "");
        if (!s.isEmpty()) ownerName = s;
        ownerVisitTicks = tag.getIntOr("OwnerVisitTicks", 0);
        ownerGiftDrops = tag.getIntOr("OwnerGiftDrops", 0);
    }
}
