package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Coven Witch: a friendly wandering merchant that sells witchery ingredients.
 * Skin is randomly assigned on spawn (0-4 -> covenwitch1-5.png).
 */
public class CovenWitchEntity extends AbstractVillager implements CovenMember, RangedAttackMob {

    private static final String TAG_SKIN = "Skin";
    /** Source {@code WITCCoven}: a player's coven holds at most 6 recruited witches. */
    public static final int MAX_COVEN = com.emoniph.witchery.world.CovenData.MAX_COVEN;
    private static final String TAG_COVEN_OWNER = "WITCCovenOwner";
    private static final String TAG_QUEST_FOR = "WITCQuestFor";
    private static final String TAG_QUEST_ITEM = "WITCQuestItem";
    private static final String TAG_QUEST_LEFT = "WITCQuestLeft";
    private static final String TAG_QUEST_KIND = "WITCQuestKind";
    /** Persistent-data tag the witch stamps onto a fight-quest monster (and its drop) — source {@code WITCQuestOwnerID}. */
    public static final String TAG_QUEST_WITCH_UUID = "WITCQuestWitchUUID";
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(CovenWitchEntity.class, EntityDataSerializers.INT);

    /** Source {@code Quest} subtypes: fetch an item, or slay a conjured pet and return its tagged remains. */
    public enum QuestKind {
        FETCH, FIGHT_SPIDER, FIGHT_ZOMBIE;
        public static QuestKind fromOrdinal(int i) {
            QuestKind[] vals = values();
            return i >= 0 && i < vals.length ? vals[i] : FETCH;
        }
    }

    /** A quest a witch may set before joining a coven. For FIGHT_* the item is the tagged drop to return. */
    private record CovenQuest(QuestKind kind, net.minecraft.world.item.Item item, int count, @Nullable String descKey) {}

    /** Built at runtime (not a static field) so the deferred {@code WitcheryItems} holders are populated. */
    private static java.util.List<CovenQuest> quests() {
        return java.util.List.of(
                new CovenQuest(QuestKind.FIGHT_SPIDER, Items.SPIDER_EYE, 1, "witchery.witch.quest.fightspider"),
                new CovenQuest(QuestKind.FIGHT_ZOMBIE, Items.ROTTEN_FLESH, 1, "witchery.witch.quest.fightzombie"),
                new CovenQuest(QuestKind.FETCH, WitcheryItems.DEMON_HEART.get(), 1, null),
                new CovenQuest(QuestKind.FETCH, Items.BONE, 30, null),
                new CovenQuest(QuestKind.FETCH, WitcheryItems.MANDRAKE_ROOT.get(), 16, null),
                new CovenQuest(QuestKind.FETCH, WitcheryItems.GRAVEYARD_DUST.get(), 8, null),
                new CovenQuest(QuestKind.FETCH, WitcheryItems.QUARTZ_SPHERE.get(), 1, null));
    }

    @Nullable
    private java.util.UUID covenOwner;
    private String questFor = "";
    @Nullable
    private net.minecraft.world.item.Item questItem;
    private int questRemaining;
    private int questKind; // QuestKind ordinal
    private boolean rosterReconciled; // transient — heal legacy recruited witches into CovenData once

    public CovenWitchEntity(EntityType<? extends CovenWitchEntity> type, Level level) {
        super(type, level);
    }

    @Nullable
    public java.util.UUID getCovenOwner() {
        return covenOwner;
    }

    public boolean isRecruited() {
        return covenOwner != null;
    }

    public boolean isRecruitedBy(Player player) {
        return covenOwner != null && covenOwner.equals(player.getUUID());
    }

    public void setCovenOwner(@Nullable java.util.UUID owner) {
        this.covenOwner = owner;
        setPersistenceRequired(); // a recruited (or once-recruited) witch never despawns
    }

    /** Size of {@code owner}'s persisted coven roster (the cap basis — source counted the player {@code WITCCoven} list). */
    public static int covenSize(ServerLevel level, java.util.UUID owner) {
        return com.emoniph.witchery.world.CovenData.get(level).size(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    public void setSkin(int skin) {
        this.entityData.set(SKIN, Math.floorMod(skin, 5));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        // A recruited witch fights for her coven leader — throw splash potions at the leader's foes.
        this.goalSelector.addGoal(2, new CovenAttackGoal());
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // Targeting: defend the coven leader and retaliate against attackers (never the leader herself).
        this.targetSelector.addGoal(1, new CovenDefendTargetGoal());
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        var offers = this.getOffers();
        addTrade(offers, Items.EMERALD, 1, WitcheryItems.BELLADONNA.get(), 2, 8, 5);
        addTrade(offers, Items.EMERALD, 1, WitcheryItems.MANDRAKE_ROOT.get(), 1, 8, 5);
        addTrade(offers, Items.EMERALD, 1, WitcheryItems.ARTICHOKE.get(), 2, 12, 3);
        addTrade(offers, Items.EMERALD, 1, WitcheryItems.WORMWOOD.get(), 2, 12, 3);
        addTrade(offers, Items.EMERALD, 1, WitcheryItems.SNOWBELL.get(), 2, 10, 3);
        addTrade(offers, Items.EMERALD, 2, WitcheryItems.ROWAN_BERRIES.get(), 4, 10, 4);
        addTrade(offers, Items.EMERALD, 3, WitcheryItems.WOOD_ASH.get(), 4, 12, 4);
        addTrade(offers, Items.EMERALD, 4, WitcheryItems.TAGLOCK_KIT.get(), 1, 6, 6);
        addTrade(offers, Items.EMERALD, 5, WitcheryItems.EXHALE_OF_HORNED_ONE.get(), 1, 6, 8);
        addTrade(offers, Items.EMERALD, 5, WitcheryItems.HINT_OF_REBIRTH.get(), 1, 6, 8);
        addTrade(offers, Items.EMERALD, 8, WitcheryItems.BAT_WOOL.get(), 2, 5, 10);
        addTrade(offers, Items.EMERALD, 6, WitcheryItems.BREW_OF_REVEALING.get(), 2, 5, 8);
        addTrade(offers, Items.EMERALD, 8, WitcheryItems.BREW_OF_SLEEPING.get(), 2, 5, 10);
        addTrade(offers, WitcheryItems.BELLADONNA.get(), 3, Items.EMERALD, 1, 16, 2);
        addTrade(offers, WitcheryItems.MANDRAKE_ROOT.get(), 2, Items.EMERALD, 1, 16, 2);
        addTrade(offers, WitcheryItems.WORMWOOD.get(), 3, Items.EMERALD, 1, 16, 2);
    }

    private static void addTrade(net.minecraft.world.item.trading.MerchantOffers offers,
                                 net.minecraft.world.item.Item costItem, int costCount,
                                 net.minecraft.world.item.Item resultItem, int resultCount,
                                 int maxUses, int xp) {
        offers.add(new MerchantOffer(
                new ItemCost(costItem, costCount),
                new ItemStack(resultItem, resultCount),
                maxUses, xp, 0.05f));
    }

    private static final String[] GREET_KEYS = {
            "witchery.witch.say.greet1",
            "witchery.witch.say.greet2",
            "witchery.witch.say.greet3",
            "witchery.witch.say.greet4",
            "witchery.witch.say.greet5",
    };

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!isAlive() || isTrading()) {
            return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            // Sneak-interact recruits the witch into your coven (or dismisses her if already yours).
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return handleCovenInteraction(player);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        String witchName = hasCustomName() ? getCustomName().getString() : "Witch";
        player.displayClientMessage(
                Component.literal("\u00a79<" + witchName + "> \u00a7r")
                        .append(Component.translatable(GREET_KEYS[random.nextInt(GREET_KEYS.length)])),
                false);
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        return InteractionResult.CONSUME;
    }

    /** The outcome of a sneak-interact on a free witch (before the fetch quest runs). */
    public enum CovenRecruitOutcome { DISMISS, SERVES_ANOTHER, FULL, NO_FAMILIAR, OFFER_QUEST, RUN_QUEST }

    /** Pure gate decision for a sneak-interact (testable). */
    public static CovenRecruitOutcome recruitOutcome(boolean ownedByPlayer, boolean owned, boolean covenFull,
                                                     boolean hasFamiliar, boolean questOfferedToPlayer) {
        if (ownedByPlayer) return CovenRecruitOutcome.DISMISS;
        if (owned) return CovenRecruitOutcome.SERVES_ANOTHER;
        if (covenFull) return CovenRecruitOutcome.FULL;
        if (!hasFamiliar) return CovenRecruitOutcome.NO_FAMILIAR;       // source: only a witch with a familiar may recruit
        return questOfferedToPlayer ? CovenRecruitOutcome.RUN_QUEST : CovenRecruitOutcome.OFFER_QUEST;
    }

    /** Step of an in-progress fetch quest when the player sneak-interacts (testable). */
    public enum QuestStep { NOT_FINISHED, PROGRESS, COMPLETE }

    public static QuestStep questStep(boolean holdingQuestItem, int remainingBefore) {
        if (!holdingQuestItem) return QuestStep.NOT_FINISHED;
        return remainingBefore <= 1 ? QuestStep.COMPLETE : QuestStep.PROGRESS;
    }

    @Nullable
    public net.minecraft.world.item.Item questItem() {
        return questItem;
    }

    private InteractionResult handleCovenInteraction(Player player) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }
        String witchName = hasCustomName() ? getCustomName().getString() : "Witch";
        boolean full = com.emoniph.witchery.world.CovenData.get(serverLevel).isFull(player.getUUID());
        boolean hasFamiliar = !com.emoniph.witchery.init.WitcheryAttachments.get(player).getFamiliarType().isBlank();
        boolean offeredToYou = questItem != null && questFor.equals(player.getUUID().toString());

        switch (recruitOutcome(isRecruitedBy(player), isRecruited(), full, hasFamiliar, offeredToYou)) {
            case DISMISS -> {
                com.emoniph.witchery.world.CovenData.get(serverLevel).remove(player.getUUID(), getUUID());
                setCovenOwner(null);
                covenSay(player, witchName, "witchery.witch.say.leftcoven");
            }
            case SERVES_ANOTHER -> covenSay(player, witchName, "witchery.witch.say.servesanother");
            case FULL -> {
                clearQuest();
                covenSay(player, witchName, "witchery.witch.say.covenfull");
            }
            case NO_FAMILIAR -> covenSay(player, witchName, "witchery.witch.say.notinterested");
            case OFFER_QUEST -> offerQuest(serverLevel, player, witchName);
            case RUN_QUEST -> runQuest(serverLevel, player, witchName);
        }
        return InteractionResult.CONSUME;
    }

    private void offerQuest(ServerLevel serverLevel, Player player, String witchName) {
        java.util.List<CovenQuest> roster = quests();
        CovenQuest quest = roster.get(random.nextInt(roster.size()));
        questFor = player.getUUID().toString();
        questItem = quest.item();
        questRemaining = quest.count();
        questKind = quest.kind().ordinal();
        if (quest.kind() == QuestKind.FETCH) {
            player.displayClientMessage(Component.literal("§9<" + witchName + "> §r").append(
                    Component.translatable("witchery.witch.say.questoffer",
                            questRemaining, questItem.getDefaultInstance().getHoverName())), false);
        } else {
            spawnFightQuestMonster(serverLevel, player, quest.kind(), witchName);
            player.displayClientMessage(Component.literal("§9<" + witchName + "> §r")
                    .append(Component.translatable(quest.descKey())), false);
        }
    }

    private void runQuest(ServerLevel serverLevel, Player player, String witchName) {
        boolean holding = holdingQuestItem(player);
        switch (questStep(holding, questRemaining)) {
            case NOT_FINISHED -> covenSay(player, witchName, "witchery.witch.say.questnotfinished");
            case PROGRESS -> {
                player.getMainHandItem().shrink(1);
                questRemaining--;
                player.displayClientMessage(Component.literal("§9<" + witchName + "> §r").append(
                        Component.translatable("witchery.witch.say.questitemsremaining", questRemaining)), false);
            }
            case COMPLETE -> {
                player.getMainHandItem().shrink(1);
                clearQuest();
                com.emoniph.witchery.world.CovenData.get(serverLevel).add(player.getUUID(), getUUID());
                setCovenOwner(player.getUUID());
                covenSay(player, witchName, "witchery.witch.say.joinedcoven");
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                        getX(), getY() + getBbHeight() * 0.75, getZ(), 16, 0.4, 0.5, 0.4, 0.0);
                level().playSound(null, blockPosition(), SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.2f);
            }
        }
    }

    /**
     * Is the player holding the quest deliverable? Fetch quests take any matching item; fight quests take
     * only the slain pet's remains, recognised by the witch-UUID stamp the kill drop carries (source
     * {@code isQuestItemForEntity} — the plain item never satisfies a fight quest).
     */
    private boolean holdingQuestItem(Player player) {
        ItemStack held = player.getMainHandItem();
        if (questItem == null || !held.is(questItem)) {
            return false;
        }
        if (QuestKind.fromOrdinal(questKind) == QuestKind.FETCH) {
            return true;
        }
        net.minecraft.world.item.component.CustomData data = held.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null
                && data.copyTag().getStringOr(TAG_QUEST_WITCH_UUID, "").equals(getStringUUID());
    }

    /** Conjures the buffed pet (source FightPetQuest/FightZombiePetQuest): 100 HP, targets the player, UUID-stamped. */
    private void spawnFightQuestMonster(ServerLevel serverLevel, Player player, QuestKind kind, String witchName) {
        net.minecraft.world.entity.Mob monster = kind == QuestKind.FIGHT_SPIDER
                ? net.minecraft.world.entity.EntityType.SPIDER.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT)
                : net.minecraft.world.entity.EntityType.ZOMBIE.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (monster == null) {
            return;
        }
        // Materialise the pet a couple of blocks away from the witch (toward the player) so it does not
        // spawn embedded in her, then aim it at the player; CovenQuestEvents stops it ever turning on her.
        net.minecraft.world.phys.Vec3 away = new net.minecraft.world.phys.Vec3(
                player.getX() - getX(), 0.0, player.getZ() - getZ());
        away = away.lengthSqr() < 1.0E-4 ? new net.minecraft.world.phys.Vec3(2.0, 0.0, 0.0)
                : away.normalize().scale(2.5);
        monster.snapTo(getX() + away.x, getY(), getZ() + away.z, getYRot(), getXRot());
        var hp = monster.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(100.0);
            monster.setHealth(100.0f);
        }
        var atk = monster.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(5.0);
        }
        monster.setCustomName(Component.translatable("witchery.witch.pet", witchName));
        monster.getPersistentData().putString(TAG_QUEST_WITCH_UUID, getStringUUID());
        monster.setTarget(player);
        serverLevel.addFreshEntity(monster);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                monster.getX(), monster.getY() + 1.0, monster.getZ(), 24, 0.4, 0.6, 0.4, 0.1);
    }

    private void clearQuest() {
        questFor = "";
        questItem = null;
        questRemaining = 0;
        questKind = 0;
    }

    private void covenSay(Player player, String witchName, String key) {
        player.displayClientMessage(
                Component.literal("§9<" + witchName + "> §r").append(Component.translatable(key)), false);
    }

    // ── Coven combat — recruited witches defend their leader (source: EntityCovenWitch fights) ──

    /** The recruited witch's coven leader, if loaded nearby. */
    @Nullable
    private Player covenLeader() {
        return covenOwner == null ? null : level().getPlayerByUUID(covenOwner);
    }

    /** A target is fair game only if it is not the leader and not a fellow member of the same coven. */
    private boolean isHostileToCoven(@Nullable LivingEntity candidate) {
        if (candidate == null || candidate == this || !candidate.isAlive()) {
            return false;
        }
        if (covenOwner != null && covenOwner.equals(candidate.getUUID())) {
            return false; // never the leader
        }
        if (candidate instanceof CovenWitchEntity other && covenOwner != null
                && covenOwner.equals(other.getCovenOwner())) {
            return false; // never a sister witch
        }
        return true;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (level().isClientSide()) {
            return;
        }
        // Witch-style harmful splash potion, weaker than Baba Yaga's roster (source: a coven witch is no boss).
        List<MobEffectInstance> effects = switch (random.nextInt(6)) {
            case 0, 1 -> List.of(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            case 2, 3 -> List.of(new MobEffectInstance(MobEffects.POISON, 100, 0));
            case 4 -> List.of(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            default -> List.of(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
        };

        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects, Optional.empty()));

        ThrownSplashPotion thrown = new ThrownSplashPotion(level(), this, potionStack);
        thrown.setPos(getX(), getEyeY(), getZ());

        double dx = target.getX() + target.getDeltaMovement().x - getX();
        double dy = target.getEyeY() - 1.1 - getY();
        double dz = target.getZ() + target.getDeltaMovement().z - getZ();
        float flatDist = Mth.sqrt((float) (dx * dx + dz * dz));
        thrown.shoot(dx, dy + flatDist * 0.2, dz, 0.75F, 8.0F);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        level().addFreshEntity(thrown);
    }

    /** Throws a splash potion at the leader's foes every ~60 ticks while a target is in range. */
    private final class CovenAttackGoal extends Goal {
        private int cooldown;

        CovenAttackGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isRecruited() && isHostileToCoven(getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            cooldown = 20;
        }

        @Override
        public void stop() {
            setTarget(null);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            double dist = distanceTo(target);
            if (dist > 8.0) {
                getNavigation().moveTo(target, 0.7);
            } else {
                getNavigation().stop();
            }
            if (--cooldown <= 0 && hasLineOfSight(target) && dist < 12.0) {
                cooldown = 60;
                performRangedAttack(target, 1.0F);
            }
        }
    }

    /** Picks a hostile target: the leader's last-hurt foe, or whoever struck this witch (never the leader). */
    private final class CovenDefendTargetGoal extends TargetGoal {
        CovenDefendTargetGoal() {
            super(CovenWitchEntity.this, false);
        }

        @Nullable
        private LivingEntity findTarget() {
            if (!isRecruited()) {
                return null;
            }
            // Retaliate against our own attacker first.
            LivingEntity attacker = getLastHurtByMob();
            if (isHostileToCoven(attacker) && distanceToSqr(attacker) <= 256.0) {
                return attacker;
            }
            // Otherwise defend the leader: attack whatever the leader is fighting or has been hurt by.
            Player leader = covenLeader();
            if (leader != null && distanceToSqr(leader) <= 256.0) {
                LivingEntity leaderFoe = leader.getLastHurtMob();
                if (isHostileToCoven(leaderFoe) && distanceToSqr(leaderFoe) <= 256.0) {
                    return leaderFoe;
                }
                LivingEntity leaderAttacker = leader.getLastHurtByMob();
                if (isHostileToCoven(leaderAttacker) && distanceToSqr(leaderAttacker) <= 256.0) {
                    return leaderAttacker;
                }
            }
            return null;
        }

        @Override
        public boolean canUse() {
            return findTarget() != null;
        }

        @Override
        public void start() {
            setTarget(findTarget());
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity current = getTarget();
            return isRecruited() && isHostileToCoven(current) && distanceToSqr(current) <= 400.0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Heal a recruited witch into the persisted roster once (e.g. one recruited before the roster existed).
        if (!rosterReconciled && covenOwner != null && level() instanceof ServerLevel sl) {
            rosterReconciled = true;
            com.emoniph.witchery.world.CovenData data = com.emoniph.witchery.world.CovenData.get(sl);
            if (!data.contains(covenOwner, getUUID())) {
                data.add(covenOwner, getUUID());
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        // Source EntityCovenWitch removes the dead witch from her owner's WITCCoven list.
        if (!level().isClientSide() && covenOwner != null && level() instanceof ServerLevel sl) {
            com.emoniph.witchery.world.CovenData.get(sl).remove(covenOwner, getUUID());
        }
        super.die(source);
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
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return null;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SKIN, getSkin());
        if (covenOwner != null) {
            tag.putString(TAG_COVEN_OWNER, covenOwner.toString());
        }
        if (questItem != null && !questFor.isEmpty()) {
            tag.putString(TAG_QUEST_FOR, questFor);
            tag.putString(TAG_QUEST_ITEM,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(questItem).toString());
            tag.putInt(TAG_QUEST_LEFT, questRemaining);
            tag.putInt(TAG_QUEST_KIND, questKind);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setSkin(tag.getIntOr(TAG_SKIN, 0));
        String owner = tag.getStringOr(TAG_COVEN_OWNER, "");
        if (!owner.isEmpty()) {
            try {
                covenOwner = java.util.UUID.fromString(owner);
            } catch (IllegalArgumentException ignored) {
                covenOwner = null;
            }
        }
        questFor = tag.getStringOr(TAG_QUEST_FOR, "");
        questRemaining = tag.getIntOr(TAG_QUEST_LEFT, 0);
        questKind = tag.getIntOr(TAG_QUEST_KIND, 0);
        String questItemId = tag.getStringOr(TAG_QUEST_ITEM, "");
        questItem = questItemId.isEmpty() ? null
                : net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.Identifier.parse(questItemId)).orElse(null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    private static final String[] FIRST_NAMES = {
            "Abigail", "Agatha", "Alcina", "Amanita", "Ambrosia", "Anastasia", "Andromeda", "Aradia",
            "Artemis", "Astrid", "Aurora", "Beatrix", "Belladonna", "Callypso", "Carmilla", "Cassandra",
            "Celeste", "Circe", "Cordelia", "Drusilla", "Elphaba", "Elvira", "Esmerelda", "Evangeline",
            "Hecate", "Helena", "Iris", "Isabeau", "Isis", "Isolde", "Jinx", "Katrina", "Lavinia", "Lenore",
            "Lilith", "Luna", "Lydia", "Magdalena", "Medea", "Melantha", "Miranda", "Moira", "Morgana",
            "Morticia", "Nyx", "Ophelia", "Pandora", "Persephone", "Prudence", "Raven", "Rowena", "Sabrina",
            "Selene", "Seraphina", "Sybil", "Tabitha", "Tempest", "Willow", "Zinnia"
    };

    private static final String[] SURNAMES = {
            "Ashwood", "Bathory", "Blackthorn", "Blackwood", "Bloodstone", "Coldwell", "Crowley", "Darke",
            "Deadwood", "DeLioncourt", "Drago", "Eldritch", "Faust", "Grimm", "Grimsby", "Harker",
            "Hemlock", "Hexwood", "Hollow", "Lavinia", "LeFay", "Mandrake", "Montague", "Moone", "Nocton",
            "Poe", "Raven", "Ravencraft", "Reaper", "Rookwood", "Shelley", "Spectre", "Spellman", "Stoker",
            "Stormwind", "Strange", "Thornwood", "Valentine", "Van Helsing", "Vesper", "Widdowes", "Wilde",
            "Winterrose", "Witchwood", "Wolftooth"
    };

    public static String generateWitchName() {
        java.util.Random random = new java.util.Random();
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + SURNAMES[random.nextInt(SURNAMES.length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        net.minecraft.world.entity.EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        setSkin(this.random.nextInt(5));
        if (!hasCustomName()) {
            setCustomName(Component.literal(generateWitchName()));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }
}
