package com.emoniph.witchery.entity;

import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hobgoblin: forest-dwelling merchant.
 * Neutral to players, hostile to villagers, immune to fall damage.
 * When leashed: picks up nearby dropped items and deposits them in chests.
 * When leashed + holding a pickaxe: occasionally mines, yielding Koboldite Dust.
 */
public class HobgoblinEntity extends AbstractVillager {

    private static final String TAG_PROFESSION = "Profession";
    private static final long ATTENTION_PENDING_TICKS = 45L * 20L;
    private static final long ATTENTION_AFTER_REPLY_TICKS = 45L * 20L;
    private static final long ATTENTION_BRIEF_TICKS = 20L * 20L;
    private static final double ATTENTION_MAX_DISTANCE_SQR = 18.0 * 18.0;
    private static final int WORSHIP_SEARCH_RADIUS = 24;
    private static final int PASSIVE_WORSHIP_SEARCH_RADIUS = 16;
    private static final int MINING_SEARCH_RADIUS = 5;
    private static final long WORSHIP_COMMAND_TICKS = 60L * 20L;
    private static final long WORSHIP_PASSIVE_TICKS = 30L * 20L;
    private static final long WORSHIP_PRAYER_INTERVAL_TICKS = 5L * 20L;
    private static final long MINING_COMMAND_TICKS = 90L * 20L;
    private static final long FOLLOW_COMMAND_TICKS = 120L * 20L;
    private static final long GUARD_COMMAND_TICKS = 180L * 20L;
    private static final long MINING_COMMENT_INTERVAL_TICKS = 18L * 20L;
    private static final double NEARBY_SPEECH_RANGE = 10.0D;
    private static final String TAG_WORK_STATE = "WorkState";
    private static final String TAG_WORSHIP_TARGET = "WorshipTarget";
    private static final String TAG_WORSHIP_UNTIL = "WorshipUntil";
    private static final String TAG_MINING_UNTIL = "MiningUntil";
    private static final String TAG_MINE_TARGET = "MineTarget";
    private static final String TAG_COMMAND_OWNER_UUID = "CommandOwnerUUID";
    private static final String TAG_COMMAND_OWNER_NAME = "CommandOwnerName";
    private static final String[] MINING_COMMENTS = {
            "Work, work... rope bossy.",
            "*shrugs* \"Pickaxe heavy. Shiny better be worth it.\"",
            "Dig dirt, find dust, hear big-one brag.",
            "*taps a finger* \"Leash pulls. Hobgoblin remembers.\""
    };
    private static final String[] WORSHIP_COMMENTS = {
            "*nods* \"Small prayer. Big ears hear.\"",
            "Mog and Gulg smell this stone.",
            "*gestures* \"Patron gets words. Hobgoblin keeps skin.\""
    };
    private static final EntityDataAccessor<Integer> PROFESSION_VARIANT =
            SynchedEntityData.defineId(HobgoblinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WORK_STATE =
            SynchedEntityData.defineId(HobgoblinEntity.class, EntityDataSerializers.INT);
    @Nullable
    private UUID attentionPlayer;
    private long attentionUntil;
    @Nullable
    private BlockPos worshipTarget;
    private long worshipUntil;
    private long nextWorshipPrayerAt;
    private long nextMiningCommentAt;
    private long miningUntil;
    private long followUntil;
    private long guardUntil;
    @Nullable
    private BlockPos mineTarget;
    @Nullable
    private UUID commandOwnerUuid;
    private String commandOwnerName = "";
    @Nullable
    private UUID commandPlayer;

    public HobgoblinEntity(EntityType<? extends HobgoblinEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROFESSION_VARIANT, 0);
        builder.define(WORK_STATE, 0);
    }

    public int getProfessionVariant() {
        return this.entityData.get(PROFESSION_VARIANT);
    }

    public void setProfessionVariant(int profession) {
        this.entityData.set(PROFESSION_VARIANT, Math.floorMod(profession, 4));
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
        this.goalSelector.addGoal(1, new PlayerAttentionGoal());
        this.goalSelector.addGoal(2, new HobgoblinFollowGoal());
        this.goalSelector.addGoal(3, new HobgoblinGuardGoal());
        this.goalSelector.addGoal(4, new HobgoblinWorshipGoal());
        this.goalSelector.addGoal(5, new HobgoblinMiningGoal());
        this.goalSelector.addGoal(6, new AvoidEntityGoal<>(this, Player.class, 8f, 0.6, 0.6) {
            @Override
            public boolean canUse() {
                return activeAttentionPlayer() == null && activeWorshipTarget() == null
                        && activeFollowPlayer() == null && activeGuardPlayer() == null
                        && !isWorking() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return activeAttentionPlayer() == null && activeWorshipTarget() == null
                        && activeFollowPlayer() == null && activeGuardPlayer() == null
                        && !isWorking() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new MeleeAttackGoal(this, 0.8, true));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Villager.class, true) {
            @Override
            public boolean canUse() {
                return activeFollowPlayer() == null && activeGuardPlayer() == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return activeFollowPlayer() == null && activeGuardPlayer() == null && super.canContinueToUse();
            }
        });
    }

    public void beginPlayerConversation(Player player) {
        holdPlayerAttention(player, ATTENTION_PENDING_TICKS, true);
    }

    public void completePlayerConversation(Player player) {
        holdPlayerAttention(player, ATTENTION_AFTER_REPLY_TICKS, false);
    }

    public void nudgePlayerAttention(Player player) {
        holdPlayerAttention(player, ATTENTION_BRIEF_TICKS, true);
    }

    public boolean hasCommandOwner() {
        return commandOwnerUuid != null;
    }

    public boolean isCommandOwner(Player player) {
        return player != null && commandOwnerUuid != null && commandOwnerUuid.equals(player.getUUID());
    }

    public boolean canAcceptCommandFrom(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (WitcheryProtectionService.isAdmin(player)) {
            return true;
        }
        if (isCommandOwner(player)) {
            return true;
        }
        return !hasCommandOwner() && isLeashedBy(player);
    }

    public boolean claimCommandOwner(Player player) {
        if (player == null || hasCommandOwner() || !isLeashedBy(player)) {
            return false;
        }
        commandOwnerUuid = player.getUUID();
        commandOwnerName = player.getName().getString();
        setPersistenceRequired();
        return true;
    }

    public String commandOwnerName() {
        return commandOwnerName == null ? "" : commandOwnerName;
    }

    private boolean isLeashedBy(Player player) {
        if (player == null || !isLeashed()) {
            return false;
        }
        Entity holder = getLeashHolder();
        return holder != null && holder.getUUID().equals(player.getUUID());
    }

    @Nullable
    private ServerPlayer responsibleCommandPlayer(ServerLevel level) {
        if (commandPlayer != null) {
            ServerPlayer commander = level.getServer().getPlayerList().getPlayer(commandPlayer);
            if (commander != null && commander.isAlive()) {
                return commander;
            }
        }
        if (commandOwnerUuid != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(commandOwnerUuid);
            if (owner != null && owner.isAlive()) {
                return owner;
            }
        }
        Entity holder = isLeashed() ? getLeashHolder() : null;
        return holder instanceof ServerPlayer player && player.isAlive() ? player : null;
    }

    private void holdPlayerAttention(Player player, long ticks, boolean extendOnly) {
        if (level().isClientSide() || player == null || !player.isAlive() || player.level() != level()) {
            return;
        }
        long until = level().getGameTime() + ticks;
        if (extendOnly && player.getUUID().equals(attentionPlayer) && attentionUntil >= until) {
            faceAttentionPlayer(player);
            stopMovingForAttention();
            return;
        }
        attentionPlayer = player.getUUID();
        attentionUntil = until;
        faceAttentionPlayer(player);
        stopMovingForAttention();
    }

    @Nullable
    private Player activeAttentionPlayer() {
        if (attentionPlayer == null || level().getGameTime() > attentionUntil) {
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

    public boolean hasMiningPickaxe() {
        return getMainHandItem().is(ItemTags.PICKAXES);
    }

    public boolean canMineKoboldite() {
        return isLeashed() && hasMiningPickaxe();
    }

    public boolean isWorking() {
        return this.entityData.get(WORK_STATE) == 1;
    }

    public boolean isWorshipping() {
        return this.entityData.get(WORK_STATE) == 2;
    }

    private void setWorkingState(int state) {
        this.entityData.set(WORK_STATE, Mth.clamp(state, 0, 2));
    }

    public boolean commandMine(Player player) {
        if (!canMineKoboldite()) {
            return false;
        }
        claimCommandOwner(player);
        clearMovementOrders();
        setTarget(null);
        miningUntil = level().getGameTime() + MINING_COMMAND_TICKS;
        mineTarget = null;
        clearWorshipTarget();
        commandPlayer = player == null ? null : player.getUUID();
        if (player != null) {
            faceAttentionPlayer(player);
        }
        return true;
    }

    private boolean hasActiveMiningOrder() {
        if (level().getGameTime() > miningUntil) {
            miningUntil = 0L;
            return false;
        }
        return miningUntil > 0L;
    }

    public String workSceneInfo() {
        ItemStack hand = getMainHandItem();
        String held = hand.isEmpty() ? "holding nothing visible" : "holding " + hand.getHoverName().getString();
        String leash = isLeashed() ? "leashed" : "free";
        String state = isWorshipping() ? "worshipping a goblin patron"
                : isWorking() ? "actively mining nearby stone and earth"
                : activeGuardPlayer() != null ? "guarding near the command holder"
                : activeFollowPlayer() != null ? "following the command holder"
                : hasActiveMiningOrder() ? "ordered to mine, searching for a safe block"
                : "idle";
        String mining = canMineKoboldite()
                ? "can mine while the leash and pickaxe remain"
                : "will not mine unless leashed and given a pickaxe";
        return "%s; %s; %s; %s".formatted(leash, held, state, mining);
    }

    public boolean commandWorship(Player player) {
        Optional<BlockPos> patron = nearestPatronStatue(WORSHIP_SEARCH_RADIUS);
        if (patron.isEmpty()) {
            return false;
        }
        claimCommandOwner(player);
        clearMovementOrders();
        setTarget(null);
        startWorship(patron.get(), WORSHIP_COMMAND_TICKS, player == null ? null : player.getUUID());
        if (player != null) {
            faceAttentionPlayer(player);
        }
        return true;
    }

    public void commandStopWork(Player player) {
        claimCommandOwner(player);
        miningUntil = 0L;
        mineTarget = null;
        clearMovementOrders();
        clearWorshipTarget();
        setWorkingState(0);
        commandPlayer = player == null ? null : player.getUUID();
        setTarget(null);
        getNavigation().stop();
        if (player != null) {
            faceAttentionPlayer(player);
        }
    }

    public boolean commandFollow(Player player) {
        if (player == null || !player.isAlive() || player.level() != level()) {
            return false;
        }
        claimCommandOwner(player);
        miningUntil = 0L;
        mineTarget = null;
        clearWorshipTarget();
        guardUntil = 0L;
        followUntil = level().getGameTime() + FOLLOW_COMMAND_TICKS;
        commandPlayer = player.getUUID();
        setWorkingState(0);
        setTarget(null);
        clearAttention();
        faceAttentionPlayer(player);
        return true;
    }

    public boolean commandGuard(Player player) {
        if (player == null || !player.isAlive() || player.level() != level()) {
            return false;
        }
        claimCommandOwner(player);
        miningUntil = 0L;
        mineTarget = null;
        clearWorshipTarget();
        followUntil = 0L;
        guardUntil = level().getGameTime() + GUARD_COMMAND_TICKS;
        commandPlayer = player.getUUID();
        setWorkingState(0);
        setTarget(null);
        clearAttention();
        faceAttentionPlayer(player);
        return true;
    }

    public Optional<BlockPos> nearestPatronStatue(int radius) {
        return findNearestPatron(Math.max(1, radius));
    }

    public String patronSceneInfo(int radius) {
        Optional<BlockPos> patron = nearestPatronStatue(radius);
        if (patron.isEmpty() || !(level().getBlockEntity(patron.get())
                instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return "no goblin altar within " + radius + " blocks";
        }
        double distance = Math.sqrt(position().distanceToSqr(Vec3.atCenterOf(patron.get())));
        return "nearby goblin altar: Statue of Hobgoblin Patron %.1f blocks away; stored prayers %d; lifetime prayers %d%s"
                .formatted(distance, statue.storedPrayers(), statue.lifetimePrayers(),
                        activeWorshipTarget() != null ? "; currently worshipping" : "");
    }

    public void sayToNearby(String speech) {
        if (!(level() instanceof ServerLevel serverLevel) || speech == null || speech.isBlank()) {
            return;
        }
        Component component = NpcBrainService.speechComponent(NpcPersonas.HOBGOBLIN, this, speech);
        AABB range = getBoundingBox().inflate(NEARBY_SPEECH_RANGE, 4.0D, NEARBY_SPEECH_RANGE);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, range)) {
            player.displayClientMessage(component, false);
        }
    }

    @Nullable
    private BlockPos activeWorshipTarget() {
        if (worshipTarget == null || level().getGameTime() > worshipUntil) {
            clearWorshipTarget();
            return null;
        }
        if (!(level().getBlockEntity(worshipTarget) instanceof StatueHobgoblinPatronBlockEntity)) {
            clearWorshipTarget();
            return null;
        }
        return worshipTarget;
    }

    private void startWorship(BlockPos patron, long ticks, @Nullable UUID commander) {
        clearMovementOrders();
        worshipTarget = patron.immutable();
        worshipUntil = level().getGameTime() + ticks;
        nextWorshipPrayerAt = 0L;
        commandPlayer = commander;
        mineTarget = null;
        miningUntil = 0L;
        clearAttention();
        setWorkingState(2);
        getNavigation().stop();
    }

    private void clearWorshipTarget() {
        worshipTarget = null;
        worshipUntil = 0L;
        nextWorshipPrayerAt = 0L;
        commandPlayer = null;
        if (isWorshipping()) {
            setWorkingState(0);
        }
    }

    private void clearMovementOrders() {
        followUntil = 0L;
        guardUntil = 0L;
    }

    @Nullable
    private LivingEntity findGuardThreat(Player player) {
        LivingEntity attacker = player.getLastHurtByMob();
        if (isValidGuardThreat(player, attacker)) {
            return attacker;
        }
        AABB range = player.getBoundingBox().inflate(10.0D, 4.0D, 10.0D);
        List<Mob> threats = level().getEntitiesOfClass(Mob.class, range,
                mob -> mob != this && mob.isAlive() && mob.getTarget() == player && isValidGuardThreat(player, mob));
        if (threats.isEmpty()) {
            return null;
        }
        threats.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
        return threats.getFirst();
    }

    private boolean isValidGuardThreat(Player player, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != this
                && target != player
                && target.level() == level()
                && distanceToSqr(target) <= 144.0D
                && canAttack(target);
    }

    @Nullable
    private Player activeFollowPlayer() {
        if (followUntil <= 0L) {
            return null;
        }
        if (level().getGameTime() > followUntil) {
            followUntil = 0L;
            return null;
        }
        return activeCommandPlayer();
    }

    @Nullable
    private Player activeGuardPlayer() {
        if (guardUntil <= 0L) {
            return null;
        }
        if (level().getGameTime() > guardUntil) {
            guardUntil = 0L;
            return null;
        }
        return activeCommandPlayer();
    }

    @Nullable
    private Player activeCommandPlayer() {
        if (commandPlayer == null) {
            return null;
        }
        Player player = level().getPlayerByUUID(commandPlayer);
        if (player == null || !player.isAlive() || player.level() != level()) {
            return null;
        }
        return player;
    }

    private Optional<BlockPos> findNearestPatron(int radius) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        BlockPos center = blockPosition();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-radius, -4, -radius),
                center.offset(radius, 4, radius))) {
            if (!(serverLevel.getBlockEntity(candidate) instanceof StatueHobgoblinPatronBlockEntity)) {
                continue;
            }
            long dx = candidate.getX() - center.getX();
            long dy = candidate.getY() - center.getY();
            long dz = candidate.getZ() - center.getZ();
            long distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }
        return Optional.ofNullable(best);
    }

    private void faceWorshipTarget(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
        double dx = center.x - getX();
        double dz = center.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
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
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (isLeashed() && held.is(ItemTags.PICKAXES)) {
            if (!level().isClientSide()) {
                if (player instanceof ServerPlayer serverPlayer && !canAcceptCommandFrom(serverPlayer)) {
                    player.displayClientMessage(NpcBrainService.speechComponent(NpcPersonas.HOBGOBLIN, this,
                            "*clutches the rope* \"Not your leash. Not your work.\""), false);
                    return InteractionResult.CONSUME;
                }
                claimCommandOwner(player);
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

        Component fallback = NpcBrainService.speechComponent(NpcPersonas.HOBGOBLIN, this,
                "*studies your coin* \"Trade? Coal, gold, koboldite. Show shiny.\"");
        NpcBrainService.INSTANCE.requestHobgoblinGreeting(this, (net.minecraft.server.level.ServerPlayer) player,
                fallback);
        setTradingPlayer(player);
        openTradingScreen(player, Component.translatable("witchery.hobgoblin.trade"), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % 20 != 0) return;
        if (!isLeashed()) {
            miningUntil = 0L;
            mineTarget = null;
            if (isWorking()) {
                setWorkingState(0);
            }
            return;
        }

        AABB pickupRange = getBoundingBox().inflate(4);
        List<ItemEntity> nearby = level().getEntitiesOfClass(ItemEntity.class, pickupRange);
        for (ItemEntity itemEntity : nearby) {
            if (itemEntity.isAlive() && !itemEntity.getItem().isEmpty()) {
                if (depositToNearbyChest(itemEntity.getItem().copy())) {
                    itemEntity.discard();
                }
                break;
            }
        }

        ItemStack mainHand = getMainHandItem();
        if (mainHand.is(ItemTags.PICKAXES) && !isWorshipping()) {
            maybeSayMiningComment();
        }
    }

    private void maybeSayMiningComment() {
        long now = level().getGameTime();
        if (now < nextMiningCommentAt) {
            return;
        }
        nextMiningCommentAt = now + MINING_COMMENT_INTERVAL_TICKS + getRandom().nextInt(10 * 20);
        if (getRandom().nextFloat() >= 0.35F) {
            return;
        }
        sayToNearby(MINING_COMMENTS[getRandom().nextInt(MINING_COMMENTS.length)]);
    }

    private Optional<BlockPos> findMineTarget(int radius) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            BlockPos candidate = mutable.immutable();
            BlockState state = serverLevel.getBlockState(candidate);
            if (!isMineableLaborBlock(serverLevel, candidate, state)
                    || !hasOpenMineFace(serverLevel, candidate)
                    || !canLaborModify(serverLevel, candidate, WitcheryProtectionAction.BLOCK_BREAK)) {
                continue;
            }
            double distance = position().distanceToSqr(Vec3.atCenterOf(candidate));
            double score = distance + getRandom().nextDouble() * 5.0D;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isMineableLaborBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.liquid() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.endsWith("_ore") || path.contains("_ore_");
    }

    private boolean hasOpenMineFace(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if ((neighbor.isAir() || neighbor.canBeReplaced()) && neighbor.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean harvestMineTarget(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState state = serverLevel.getBlockState(pos);
        if (!isMineableLaborBlock(serverLevel, pos, state)) {
            return false;
        }
        if (!canLaborModify(serverLevel, pos, WitcheryProtectionAction.BLOCK_BREAK)) {
            sayToNearby("*sniffs* \"Land says no. Not your stone.\"");
            return false;
        }
        ItemStack pickaxe = getMainHandItem();
        serverLevel.levelEvent(null, 2001, pos, Block.getId(state));
        Block.dropResources(state, serverLevel, pos, null, this, pickaxe);
        if (!serverLevel.removeBlock(pos, false)) {
            return false;
        }
        if (!pickaxe.isEmpty()) {
            pickaxe.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
        maybeYieldKobolditeDust(serverLevel);
        if (getRandom().nextFloat() < 0.25F) {
            sayToNearby("*nods* \"Block gone. Dust maybe. Rope still rude.\"");
        }
        return true;
    }

    private boolean canLaborModify(ServerLevel level, BlockPos pos, WitcheryProtectionAction action) {
        return WitcheryProtectionService.canModifyBlock(responsibleCommandPlayer(level), level, pos, action);
    }

    private void maybeYieldKobolditeDust(ServerLevel level) {
        float chance = getMainHandItem().is(WitcheryItems.KOBOLDITE_PICKAXE.get()) ? 0.08F : 0.05F;
        if (getRandom().nextFloat() >= chance) {
            return;
        }
        ItemStack dust = new ItemStack(WitcheryItems.KOBOLDITE_DUST.get());
        if (!depositToNearbyChest(dust)) {
            spawnAtLocation(level, dust);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 0.8D, getZ(), 5, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    private final class PlayerAttentionGoal extends Goal {
        PlayerAttentionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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

    private final class HobgoblinFollowGoal extends Goal {
        HobgoblinFollowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isTrading() && getTarget() == null && activeAttentionPlayer() == null
                    && activeWorshipTarget() == null && activeFollowPlayer() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !isTrading() && getTarget() == null && activeAttentionPlayer() == null
                    && activeWorshipTarget() == null && activeFollowPlayer() != null;
        }

        @Override
        public void tick() {
            Player player = activeFollowPlayer();
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (distanceToSqr(player) > 16.0D) {
                getNavigation().moveTo(player, 0.85D);
            } else {
                getNavigation().stop();
            }
        }
    }

    private final class HobgoblinGuardGoal extends Goal {
        HobgoblinGuardGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isTrading() && getTarget() == null && activeAttentionPlayer() == null
                    && activeWorshipTarget() == null && activeGuardPlayer() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !isTrading() && getTarget() == null && activeAttentionPlayer() == null
                    && activeWorshipTarget() == null && activeGuardPlayer() != null;
        }

        @Override
        public void tick() {
            Player player = activeGuardPlayer();
            if (player == null) {
                return;
            }
            LivingEntity threat = findGuardThreat(player);
            if (threat != null) {
                setTarget(threat);
                getNavigation().stop();
                return;
            }
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            double distance = distanceToSqr(player);
            if (distance > 36.0D) {
                getNavigation().moveTo(player, 0.75D);
            } else if (distance < 4.0D) {
                Vec3 away = position().subtract(player.position());
                if (away.lengthSqr() > 1.0E-4D) {
                    Vec3 target = position().add(away.normalize().scale(2.0D));
                    getNavigation().moveTo(target.x, target.y, target.z, 0.6D);
                }
            } else {
                getNavigation().stop();
            }
        }
    }

    private final class HobgoblinWorshipGoal extends Goal {
        @Nullable
        private BlockPos target;
        private long nextPassiveSearchAt;

        HobgoblinWorshipGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isTrading() || getTarget() != null || activeAttentionPlayer() != null) {
                return false;
            }
            target = activeWorshipTarget();
            if (target != null) {
                return true;
            }
            if (isLeashed() && hasMiningPickaxe()) {
                return false;
            }
            long now = level().getGameTime();
            if (now < nextPassiveSearchAt) {
                return false;
            }
            nextPassiveSearchAt = now + 100L + getRandom().nextInt(80);
            if (getRandom().nextFloat() > 0.08F) {
                return false;
            }
            Optional<BlockPos> patron = nearestPatronStatue(PASSIVE_WORSHIP_SEARCH_RADIUS);
            if (patron.isEmpty()) {
                return false;
            }
            startWorship(patron.get(), WORSHIP_PASSIVE_TICKS, null);
            target = patron.get();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !isTrading() && getTarget() == null && activeAttentionPlayer() == null
                    && (target = activeWorshipTarget()) != null;
        }

        @Override
        public void stop() {
            if (activeWorshipTarget() == null) {
                target = null;
            }
        }

        @Override
        public void tick() {
            BlockPos patron = activeWorshipTarget();
            if (patron == null) {
                return;
            }
            target = patron;
            Vec3 center = Vec3.atCenterOf(patron);
            double distance = position().distanceToSqr(center);
            if (distance > 6.25D) {
                getNavigation().moveTo(center.x, patron.getY(), center.z, 0.62D);
                return;
            }
            getNavigation().stop();
            faceWorshipTarget(patron);
            offerPrayer(patron);
        }

        private void offerPrayer(BlockPos patron) {
            long now = level().getGameTime();
            if (now < nextWorshipPrayerAt
                    || !(level() instanceof ServerLevel serverLevel)
                    || !(serverLevel.getBlockEntity(patron) instanceof StatueHobgoblinPatronBlockEntity statue)) {
                return;
            }
            nextWorshipPrayerAt = now + WORSHIP_PRAYER_INTERVAL_TICKS + getRandom().nextInt(40);
            if (statue.addPrayer(1) <= 0) {
                return;
            }
            serverLevel.playSound(null, patron, SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS, 0.35F, 0.7F + getRandom().nextFloat() * 0.25F);
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    patron.getX() + 0.5D, patron.getY() + 1.15D, patron.getZ() + 0.5D,
                    8, 0.35D, 0.25D, 0.35D, 0.02D);
            if (getRandom().nextFloat() < (commandPlayer == null ? 0.18F : 0.35F)) {
                sayToNearby(WORSHIP_COMMENTS[getRandom().nextInt(WORSHIP_COMMENTS.length)]);
            }
        }
    }

    private final class HobgoblinMiningGoal extends Goal {
        private long nextSearchAt;
        private long nextBreakAt;

        HobgoblinMiningGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canMineKoboldite() || isTrading() || getTarget() != null
                    || activeAttentionPlayer() != null || activeWorshipTarget() != null) {
                return false;
            }
            if (!(level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            long now = level().getGameTime();
            boolean ordered = hasActiveMiningOrder();
            if (!ordered && now < nextSearchAt) {
                return false;
            }
            nextSearchAt = now + (ordered ? 40L : 120L + getRandom().nextInt(80));
            if (!ordered && getRandom().nextFloat() > 0.28F) {
                return false;
            }
            if (mineTarget == null || !isMineableLaborBlock(serverLevel, mineTarget, serverLevel.getBlockState(mineTarget))) {
                mineTarget = findMineTarget(MINING_SEARCH_RADIUS).orElse(null);
            }
            return mineTarget != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canMineKoboldite() && !isTrading() && getTarget() == null
                    && activeAttentionPlayer() == null && activeWorshipTarget() == null
                    && mineTarget != null
                    && level() instanceof ServerLevel serverLevel
                    && isMineableLaborBlock(serverLevel, mineTarget, serverLevel.getBlockState(mineTarget));
        }

        @Override
        public void start() {
            setWorkingState(1);
            nextBreakAt = level().getGameTime() + (hasActiveMiningOrder() ? 35L : 60L);
        }

        @Override
        public void stop() {
            mineTarget = null;
            if (isWorking()) {
                setWorkingState(0);
            }
        }

        @Override
        public void tick() {
            if (mineTarget == null) {
                return;
            }
            Vec3 center = Vec3.atCenterOf(mineTarget);
            double distance = position().distanceToSqr(center);
            if (distance > 6.25D) {
                getNavigation().moveTo(center.x, mineTarget.getY(), center.z, 0.62D);
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
            setWorkingState(1);
            if (level().getGameTime() < nextBreakAt) {
                if (tickCount % 10 == 0 && level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            center.x, center.y, center.z, 2, 0.15D, 0.15D, 0.15D, 0.01D);
                }
                return;
            }
            if (!harvestMineTarget(mineTarget)) {
                mineTarget = findMineTarget(MINING_SEARCH_RADIUS).orElse(null);
            } else {
                mineTarget = null;
            }
            nextBreakAt = level().getGameTime() + (hasActiveMiningOrder() ? 45L : 75L);
        }
    }

    private boolean depositToNearbyChest(ItemStack stack) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(
                blockPosition().offset(-8, -3, -8),
                blockPosition().offset(8, 3, 8))) {
            if (serverLevel.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                if (!canLaborModify(serverLevel, pos, WitcheryProtectionAction.ITEM_TRANSFER)) {
                    continue;
                }
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack existing = chest.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, stack)
                            && existing.getCount() < existing.getMaxStackSize()) {
                        int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        existing.grow(moved);
                        stack.shrink(moved);
                        chest.setChanged();
                        if (stack.isEmpty()) {
                            return true;
                        }
                    }
                    if (existing.isEmpty()) {
                        chest.setItem(i, stack);
                        chest.setChanged();
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

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        setProfessionVariant(this.random.nextInt(4));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_PROFESSION, getProfessionVariant());
        tag.putInt(TAG_WORK_STATE, this.entityData.get(WORK_STATE));
        if (worshipTarget != null) {
            tag.putLong(TAG_WORSHIP_TARGET, worshipTarget.asLong());
            tag.putLong(TAG_WORSHIP_UNTIL, worshipUntil);
        }
        if (miningUntil > 0L) {
            tag.putLong(TAG_MINING_UNTIL, miningUntil);
        }
        if (mineTarget != null) {
            tag.putLong(TAG_MINE_TARGET, mineTarget.asLong());
        }
        if (commandOwnerUuid != null) {
            tag.putString(TAG_COMMAND_OWNER_UUID, commandOwnerUuid.toString());
        }
        if (commandOwnerName != null && !commandOwnerName.isBlank()) {
            tag.putString(TAG_COMMAND_OWNER_NAME, commandOwnerName);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setProfessionVariant(tag.getIntOr(TAG_PROFESSION, 0));
        setWorkingState(tag.getIntOr(TAG_WORK_STATE, 0));
        long worshipPacked = tag.getLongOr(TAG_WORSHIP_TARGET, Long.MIN_VALUE);
        worshipTarget = worshipPacked == Long.MIN_VALUE ? null : BlockPos.of(worshipPacked);
        worshipUntil = tag.getLongOr(TAG_WORSHIP_UNTIL, 0L);
        miningUntil = tag.getLongOr(TAG_MINING_UNTIL, 0L);
        long minePacked = tag.getLongOr(TAG_MINE_TARGET, Long.MIN_VALUE);
        mineTarget = minePacked == Long.MIN_VALUE ? null : BlockPos.of(minePacked);
        String owner = tag.getStringOr(TAG_COMMAND_OWNER_UUID, "");
        try {
            commandOwnerUuid = owner.isBlank() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            commandOwnerUuid = null;
        }
        commandOwnerName = tag.getStringOr(TAG_COMMAND_OWNER_NAME, "");
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
