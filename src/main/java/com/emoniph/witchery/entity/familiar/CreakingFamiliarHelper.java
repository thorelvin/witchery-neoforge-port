package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.data.CreakingFamiliarData;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Familiar behaviour for vanilla {@link Creaking} mobs bound through the Pale Garden rite. */
public final class CreakingFamiliarHelper {

    public static final int FAMILIAR_TYPE_ID = 5;
    public static final int WATCH_SCAN_INTERVAL_TICKS = 10;
    public static final int ROOTED_WARD_INTERVAL_TICKS = 200;
    public static final int ALERT_COOLDOWN_TICKS = 600;
    public static final int RESIN_DROP_INTERVAL_TICKS = 6000;
    public static final int MAX_INTRUDERS_REPORTED = 8;
    public static final int MAX_MONSTERS_REPELLED = 6;

    private static final double WATCH_RANGE = 16.0D;
    private static final double WATCH_RANGE_SQ = WATCH_RANGE * WATCH_RANGE;
    private static final double FOLLOW_DISTANCE = 10.0D;
    private static final double TELEPORT_DISTANCE_SQ = 12.0D * 12.0D;
    private static final double FOLLOW_DEFENSE_RANGE_SQ = 12.0D * 12.0D;
    private static final double SCARE_RANGE = 8.0D;
    private static final double INTRUSION_RANGE = 24.0D;
    private static final double RESIN_CHECK_RANGE = 3.0D;
    private static final double ANCHOR_DRIFT_SQ = 0.25D * 0.25D;
    private static final int FOLLOW_DEFENSE_INTERVAL_TICKS = 20;

    private static final String[] CREAKING_NAMES = {
            "Paleguard", "Creak", "Thornwood", "Watcher", "Mossknell", "Eyebloom",
            "Heartwood", "Stillborn", "Glimmer", "Rootling", "Ashwood", "Hollow"
    };

    private CreakingFamiliarHelper() {
    }

    public static CreakingFamiliarData getData(Creaking creaking) {
        return creaking.getData(WitcheryAttachments.CREAKING_FAMILIAR.get());
    }

    public static void setData(Creaking creaking, CreakingFamiliarData data) {
        creaking.setData(WitcheryAttachments.CREAKING_FAMILIAR.get(), data);
    }

    public static boolean isBound(Creaking creaking) {
        return getData(creaking).isBound();
    }

    public static boolean isRootedGuard(Creaking creaking) {
        return isBound(creaking) && getData(creaking).isRootedGuard();
    }

    public static boolean isStoredRootedGuard(Player player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        return matchesStoredType(data.getFamiliarType()) && data.isFamiliarSitting();
    }

    @Nullable
    public static ServerPlayer findOwner(Creaking creaking) {
        Optional<UUID> ownerUuid = getData(creaking).owner();
        if (ownerUuid.isEmpty() || !(creaking.level() instanceof ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid.get());
    }

    @Nullable
    public static Creaking findBoundCreaking(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Optional<UUID> familiarUuid = WitcheryAttachments.get(player).getFamiliarUUID();
        if (familiarUuid.isEmpty() || serverLevel.getServer() == null) {
            return null;
        }

        for (ServerLevel level : serverLevel.getServer().getAllLevels()) {
            Entity entity = level.getEntity(familiarUuid.get());
            if (entity instanceof Creaking creaking && isBound(creaking) && isOwnedBy(creaking, player)) {
                return creaking;
            }
        }
        return null;
    }

    public static boolean isOwnedBy(Creaking creaking, Player player) {
        return getData(creaking).owner().map(player.getUUID()::equals).orElse(false);
    }

    @Nullable
    public static Creaking bind(ServerPlayer player, ServerLevel level, Creaking creaking) {
        if (isBound(creaking) || FamiliarHelper.findBoundFamiliarEntity(player) != null) {
            return null;
        }

        releaseFromHeart(creaking);
        creaking.setPersistenceRequired();

        AttributeInstance maxHealth = creaking.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() < FamiliarHelper.BOUND_FAMILIAR_MAX_HEALTH) {
            maxHealth.setBaseValue(FamiliarHelper.BOUND_FAMILIAR_MAX_HEALTH);
            creaking.setHealth((float) FamiliarHelper.BOUND_FAMILIAR_MAX_HEALTH);
        }

        setData(creaking, CreakingFamiliarData.EMPTY.withOwner(player.getUUID()));
        ensureDefaultName(creaking, level.getRandom());
        syncPlayerRecord(player, creaking);
        return creaking;
    }

    public static void syncPlayerRecord(ServerPlayer player, Creaking creaking) {
        WitcheryPlayerData playerData = WitcheryAttachments.get(player);
        playerData.setFamiliarUUID(creaking.getUUID());
        playerData.setFamiliarType("creaking");
        playerData.setFamiliarName(creaking.hasCustomName() ? creaking.getCustomName().getString() : "");
        playerData.setFamiliarSitting(getData(creaking).sitting());
        playerData.setFamiliarNameVisible(creaking.isCustomNameVisible());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, playerData));
    }

    public static void markDismissed(ServerPlayer player, @Nullable Creaking creaking, boolean removeEntity) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (creaking != null) {
            if (creaking.hasCustomName()) {
                data.setFamiliarName(creaking.getCustomName().getString());
            }
            data.setFamiliarType("creaking");
            data.setFamiliarSitting(getData(creaking).sitting());
            data.setFamiliarNameVisible(creaking.isCustomNameVisible());
            setData(creaking, getData(creaking).cleared());
        }

        data.setFamiliarUUID(null);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));

        if (removeEntity && creaking != null && creaking.isAlive()) {
            Level level = creaking.level();
            level.playSound(null, creaking.blockPosition(), SoundEvents.CREAKING_DEATH,
                    creaking.getSoundSource(), 1.0F, 0.8F);
            creaking.discard();
        }
    }

    @Nullable
    public static Creaking restoreBound(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        String familiarType = WitcheryAttachments.get(player).getFamiliarType();
        if (familiarType == null || !familiarType.equalsIgnoreCase("creaking")) {
            return null;
        }

        Creaking creaking = EntityType.CREAKING.create(level, EntitySpawnReason.TRIGGERED);
        if (creaking == null) {
            return null;
        }

        CreakingFamiliarData restored = CreakingFamiliarData.EMPTY
                .withOwner(player.getUUID())
                .withSitting(WitcheryAttachments.get(player).isFamiliarSitting());
        if (restored.isRootedGuard()) {
            restored = restored.rootedAt(level.dimension(), BlockPos.containing(x, y, z),
                    level.getGameTime() + RESIN_DROP_INTERVAL_TICKS);
        }
        setData(creaking, restored);
        releaseFromHeart(creaking);
        creaking.setPersistenceRequired();

        AttributeInstance maxHealth = creaking.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(FamiliarHelper.BOUND_FAMILIAR_MAX_HEALTH);
            creaking.setHealth((float) FamiliarHelper.BOUND_FAMILIAR_MAX_HEALTH);
        }

        String familiarName = WitcheryAttachments.get(player).getFamiliarName();
        if (!familiarName.isBlank()) {
            creaking.setCustomName(Component.literal(familiarName));
            creaking.setCustomNameVisible(WitcheryAttachments.get(player).isFamiliarNameVisible());
        } else {
            ensureDefaultName(creaking, level.getRandom());
        }

        creaking.snapTo(x, y, z, player.getYRot(), 0.0F);
        level.addFreshEntity(creaking);
        WitcheryAttachments.get(player).setFamiliarUUID(creaking.getUUID());
        return creaking;
    }

    public static void ensureDefaultName(Creaking creaking, RandomSource random) {
        if (creaking.hasCustomName()) {
            return;
        }
        String name = CREAKING_NAMES[random.nextInt(CREAKING_NAMES.length)];
        creaking.setCustomName(Component.literal(name));
        creaking.setCustomNameVisible(false);
        setData(creaking, getData(creaking).withCustomName(name, false));
    }

    public static void serverTick(ServerLevel level, Creaking creaking) {
        CreakingFamiliarData data = getData(creaking);
        if (!data.isBound()) {
            return;
        }

        if (creaking.isTearingDown()) {
            creaking.setIsActive(true);
        }

        ServerPlayer owner = findOwner(creaking);
        if (owner == null) {
            keepStill(creaking, data.isRootedGuard());
            return;
        }

        if (data.isRootedGuard() && !data.hasGuardAnchor(level.dimension())) {
            data = data.rootedAt(level.dimension(), creaking.blockPosition(),
                    level.getGameTime() + RESIN_DROP_INTERVAL_TICKS);
            setData(creaking, data);
            syncPlayerRecord(owner, creaking);
        }

        boolean watched = data.watched();
        if (creaking.tickCount % WATCH_SCAN_INTERVAL_TICKS == 0) {
            watched = isWatchedByNonOwner(level, creaking, owner);
            if (watched != data.watched()) {
                data = data.withWatched(watched);
                setData(creaking, data);
            }
        }
        creaking.setIsActive(!watched);

        if (data.isRootedGuard()) {
            tickRootedGuard(level, creaking, owner, data, watched);
        } else {
            tickFollowing(creaking, owner, watched);
        }
    }

    private static void tickFollowing(Creaking creaking, ServerPlayer owner, boolean watched) {
        creaking.setNoAi(false);
        if (creaking.level() != owner.level() || watched) {
            creaking.getNavigation().stop();
            creaking.setTarget(null);
            return;
        }

        followOwner(creaking, owner);
        if (creaking.getTarget() != null && !isValidDefensiveTarget(owner, creaking.getTarget())) {
            creaking.setTarget(null);
        }
        if (creaking.tickCount % FOLLOW_DEFENSE_INTERVAL_TICKS == 0) {
            defendOwner(creaking, owner);
        }
    }

    private static void tickRootedGuard(ServerLevel level, Creaking creaking, ServerPlayer owner,
                                        CreakingFamiliarData data, boolean watched) {
        creaking.setNoAi(true);
        creaking.getNavigation().stop();
        creaking.setTarget(null);

        Optional<BlockPos> guardPosition = data.guardPosition();
        if (guardPosition.isEmpty() || !data.hasGuardAnchor(level.dimension())) {
            return;
        }

        BlockPos anchor = guardPosition.get();
        creaking.setHomePos(anchor);
        if (!level.hasChunkAt(anchor)) {
            return;
        }
        Vec3 anchored = Vec3.atBottomCenterOf(anchor);
        if (creaking.distanceToSqr(anchored) > ANCHOR_DRIFT_SQ) {
            creaking.snapTo(anchored.x, anchored.y, anchored.z, creaking.getYRot(), creaking.getXRot());
        }

        if (creaking.tickCount % ROOTED_WARD_INTERVAL_TICKS != 0) {
            return;
        }

        CreakingFamiliarData updated = updateIntrusionAlert(level, creaking, owner, data);
        if (!watched) {
            repelNearbyMonsters(creaking, owner);
            updated = tryProduceResin(level, creaking, updated);
        }
        if (updated != data) {
            setData(creaking, updated);
        }
    }

    private static void keepStill(Creaking creaking, boolean rooted) {
        creaking.setNoAi(rooted);
        creaking.setIsActive(false);
        creaking.getNavigation().stop();
        creaking.setTarget(null);
    }

    public static boolean isWatchedByNonOwner(ServerLevel level, Creaking creaking, ServerPlayer owner) {
        Vec3 creakingEye = creaking.getEyePosition();
        AABB watchArea = creaking.getBoundingBox().inflate(WATCH_RANGE);
        List<ServerPlayer> possibleWatchers = level.getEntitiesOfClass(ServerPlayer.class, watchArea,
                player -> player.isAlive() && !player.isSpectator()
                        && !isAuthorizedPlayer(owner, player));
        for (ServerPlayer other : possibleWatchers) {
            if (other.distanceToSqr(creaking) > WATCH_RANGE_SQ || !other.hasLineOfSight(creaking)) {
                continue;
            }
            Vec3 look = other.getViewVector(1.0F).normalize();
            Vec3 toCreaking = creakingEye.subtract(other.getEyePosition()).normalize();
            if (look.dot(toCreaking) > 0.5D) {
                return true;
            }
        }
        return false;
    }

    public static void toggleMode(ServerPlayer owner, Creaking creaking) {
        CreakingFamiliarData current = getData(creaking);
        CreakingFamiliarData updated;
        String message;
        if (current.isRootedGuard()) {
            updated = current.following();
            creaking.setNoAi(false);
            creaking.setIsActive(true);
            message = "witchery.familiar.creaking.mode.follow";
        } else {
            updated = current.rootedAt(creaking.level().dimension(), creaking.blockPosition(),
                    creaking.level().getGameTime() + RESIN_DROP_INTERVAL_TICKS);
            creaking.setNoAi(true);
            creaking.setIsActive(false);
            creaking.getNavigation().stop();
            creaking.setTarget(null);
            message = "witchery.familiar.creaking.mode.rooted_guard";
        }
        setData(creaking, updated);
        syncPlayerRecord(owner, creaking);
        owner.displayClientMessage(Component.translatable(message, creaking.getDisplayName()), true);
        creaking.level().playSound(null, creaking.blockPosition(), SoundEvents.CREAKING_SWAY,
                creaking.getSoundSource(), 0.7F, updated.isRootedGuard() ? 0.8F : 1.15F);
    }

    private static void followOwner(Creaking creaking, ServerPlayer owner) {
        double distanceSq = creaking.distanceToSqr(owner);
        if (distanceSq > TELEPORT_DISTANCE_SQ) {
            creaking.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            creaking.getNavigation().stop();
            return;
        }
        if (distanceSq > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
            creaking.getNavigation().moveTo(owner, 1.0D);
        } else {
            creaking.getNavigation().stop();
        }
        BlockPos ownerPos = owner.blockPosition();
        if (!ownerPos.equals(creaking.getHomePos())) {
            creaking.setHomePos(ownerPos);
        }
    }

    private static void defendOwner(Creaking creaking, ServerPlayer owner) {
        LivingEntity current = creaking.getTarget();
        if (current != null && !isValidDefensiveTarget(owner, current)) {
            creaking.setTarget(null);
        }

        LivingEntity lastHurt = owner.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive() && lastHurt != creaking
                && lastHurt.level() == creaking.level()
                && lastHurt.distanceToSqr(owner) <= FOLLOW_DEFENSE_RANGE_SQ
                && !isAuthorizedTarget(owner, lastHurt) && creaking.canAttack(lastHurt)) {
            creaking.setTarget(lastHurt);
            return;
        }
        if (creaking.getTarget() != null) {
            return;
        }

        AABB area = owner.getBoundingBox().inflate(12.0D);
        List<Monster> threats = creaking.level().getEntitiesOfClass(Monster.class, area,
                mob -> mob.isAlive() && mob != creaking && creaking.canAttack(mob)
                        && !isAuthorizedTarget(owner, mob) && mob.getTarget() == owner);
        if (!threats.isEmpty()) {
            creaking.setTarget(threats.getFirst());
        }
    }

    private static boolean isValidDefensiveTarget(ServerPlayer owner, LivingEntity target) {
        if (!target.isAlive() || target.level() != owner.level()
                || target.distanceToSqr(owner) > FOLLOW_DEFENSE_RANGE_SQ
                || isAuthorizedTarget(owner, target)) {
            return false;
        }
        if (owner.getLastHurtByMob() == target) {
            return true;
        }
        return target instanceof Monster monster && monster.getTarget() == owner;
    }

    private static CreakingFamiliarData updateIntrusionAlert(ServerLevel level, Creaking creaking,
                                                              ServerPlayer owner,
                                                              CreakingFamiliarData data) {
        int intruders = countNearbyIntruders(level, creaking, owner);
        if (intruders == 0) {
            return data.lastAlertCount() == 0 ? data : data.withAlertCount(0);
        }

        long gameTime = level.getGameTime();
        boolean cooldownElapsed = data.lastAlertGameTime() == CreakingFamiliarData.NO_SCHEDULED_TICK
                || gameTime - data.lastAlertGameTime() >= ALERT_COOLDOWN_TICKS;
        if (intruders == data.lastAlertCount() && !cooldownElapsed) {
            return data;
        }

        owner.displayClientMessage(Component.translatable(
                "witchery.familiar.creaking.sense", creaking.getDisplayName(), intruders), true);
        return data.withAlertState(gameTime, intruders);
    }

    private static int countNearbyIntruders(ServerLevel level, Creaking creaking, ServerPlayer owner) {
        AABB area = creaking.getBoundingBox().inflate(INTRUSION_RANGE);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, area,
                player -> player.isAlive() && !player.isSpectator() && !isAuthorizedPlayer(owner, player));
        return Math.min(players.size(), MAX_INTRUDERS_REPORTED);
    }

    private static CreakingFamiliarData tryProduceResin(ServerLevel level, Creaking creaking,
                                                        CreakingFamiliarData data) {
        long gameTime = level.getGameTime();
        if (data.nextResinGameTime() == CreakingFamiliarData.NO_SCHEDULED_TICK) {
            return data.withNextResinGameTime(gameTime + RESIN_DROP_INTERVAL_TICKS);
        }
        if (gameTime < data.nextResinGameTime()) {
            return data;
        }

        AABB nearby = creaking.getBoundingBox().inflate(RESIN_CHECK_RANGE);
        boolean resinWaiting = !level.getEntitiesOfClass(ItemEntity.class, nearby,
                item -> item.isAlive() && item.getItem().is(Items.RESIN_CLUMP)).isEmpty();
        if (!resinWaiting) {
            dropResinClump(level, creaking);
        }
        return data.withNextResinGameTime(gameTime + RESIN_DROP_INTERVAL_TICKS);
    }

    private static void dropResinClump(ServerLevel level, Creaking creaking) {
        ItemEntity drop = new ItemEntity(level, creaking.getX(), creaking.getY() + 0.5D,
                creaking.getZ(), new ItemStack(Items.RESIN_CLUMP));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                creaking.getX(), creaking.getY() + 1.0D, creaking.getZ(),
                4, 0.2D, 0.2D, 0.2D, 0.01D);
    }

    private static void repelNearbyMonsters(Creaking creaking, ServerPlayer owner) {
        AABB area = creaking.getBoundingBox().inflate(SCARE_RANGE);
        List<Monster> mobs = creaking.level().getEntitiesOfClass(Monster.class, area,
                mob -> mob.isAlive() && mob != creaking && !isAuthorizedTarget(owner, mob));
        int repelled = 0;
        for (Monster mob : mobs) {
            if (repelled++ >= MAX_MONSTERS_REPELLED) {
                break;
            }
            mob.getNavigation().moveTo(
                    mob.getX() + (mob.getX() - creaking.getX()), mob.getY(),
                    mob.getZ() + (mob.getZ() - creaking.getZ()), 1.1D);
        }
    }

    private static boolean isAuthorizedPlayer(ServerPlayer owner, Player player) {
        return owner.getUUID().equals(player.getUUID())
                || owner.isAlliedTo(player) || player.isAlliedTo(owner);
    }

    private static boolean isAuthorizedTarget(ServerPlayer owner, LivingEntity target) {
        return target == owner || owner.isAlliedTo(target) || target.isAlliedTo(owner);
    }

    /** Detach a creaking from its pale-garden heart so it can live as a familiar. */
    private static void releaseFromHeart(Creaking creaking) {
        creaking.setNoAi(false);
        creaking.setHomePos(creaking.blockPosition());
        creaking.setIsActive(true);
    }

    public static String describeName(Creaking creaking) {
        return creaking.hasCustomName()
                ? creaking.getCustomName().getString()
                : getData(creaking).customName();
    }

    public static boolean matchesStoredType(String familiarType) {
        return familiarType != null && familiarType.toLowerCase(Locale.ROOT).equals("creaking");
    }
}
