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
import net.minecraft.world.entity.Mob;
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

/**
 * Familiar behaviour for vanilla {@link Creaking} mobs bound through the Pale Garden rite.
 */
public final class CreakingFamiliarHelper {

    public static final int FAMILIAR_TYPE_ID = 5;

    private static final double WATCH_RANGE = 16.0;
    private static final double WATCH_RANGE_SQ = WATCH_RANGE * WATCH_RANGE;
    private static final double FOLLOW_DISTANCE = 10.0;
    private static final double TELEPORT_DISTANCE_SQ = 12.0 * 12.0;
    private static final double SCARE_RANGE = 8.0;
    private static final int RESIN_DROP_INTERVAL = 2400;
    private static final int SCARE_INTERVAL = 100;
    private static final int PLAYER_SENSE_INTERVAL = 100;

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

    @Nullable
    public static ServerPlayer findOwner(Creaking creaking) {
        Optional<UUID> ownerUuid = getData(creaking).owner();
        if (ownerUuid.isEmpty() || !(creaking.level() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
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

        CreakingFamiliarData data = CreakingFamiliarData.EMPTY.withOwner(player.getUUID());
        setData(creaking, data);
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
            level.playSound(null, creaking.blockPosition(), SoundEvents.CREAKING_DEATH, creaking.getSoundSource(), 1.0F, 0.8F);
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
        if (!isBound(creaking)) {
            return;
        }

        ServerPlayer owner = findOwner(creaking);
        if (owner == null) {
            return;
        }

        if (creaking.isTearingDown()) {
            creaking.setIsActive(true);
        }

        CreakingFamiliarData data = getData(creaking);
        releaseFromHeart(creaking);

        if (data.sitting()) {
            creaking.setIsActive(false);
            creaking.getNavigation().stop();
            creaking.setTarget(null);
            return;
        }

        boolean watched = isWatchedByNonOwner(level, creaking, owner);
        if (watched) {
            creaking.setIsActive(false);
            creaking.getNavigation().stop();
        } else {
            creaking.setIsActive(true);
            followOwner(creaking, owner);
            defendOwner(creaking, owner);
        }

        if (creaking.tickCount % RESIN_DROP_INTERVAL == 0 && creaking.distanceToSqr(owner) <= 144.0) {
            dropResinClump(level, creaking);
        }
        if (creaking.tickCount % SCARE_INTERVAL == 0 && !watched) {
            scareNearbyMonsters(creaking, owner);
        }
        if (creaking.tickCount % PLAYER_SENSE_INTERVAL == 0) {
            senseNearbyPlayers(creaking, owner);
        }
    }

    public static boolean isWatchedByNonOwner(ServerLevel level, Creaking creaking, ServerPlayer owner) {
        Vec3 creakingEye = creaking.getEyePosition();
        for (ServerPlayer other : level.players()) {
            if (other.getUUID().equals(owner.getUUID()) || other.isSpectator()) {
                continue;
            }
            if (other.distanceToSqr(creaking) > WATCH_RANGE_SQ) {
                continue;
            }
            if (!other.hasLineOfSight(creaking)) {
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

    public static void toggleSitting(ServerPlayer owner, Creaking creaking) {
        boolean sitting = !getData(creaking).sitting();
        setData(creaking, getData(creaking).withSitting(sitting));
        WitcheryAttachments.get(owner).setFamiliarSitting(sitting);
        PacketDistributor.sendToPlayer(owner, SyncPlayerDataPayload.from(owner, WitcheryAttachments.get(owner)));
        if (sitting) {
            creaking.setIsActive(false);
            creaking.getNavigation().stop();
            creaking.setTarget(null);
        }
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
        LivingEntity lastHurt = owner.getLastHurtByMob();
        if (lastHurt != null && lastHurt.isAlive() && lastHurt != creaking && creaking.canAttack(lastHurt)) {
            creaking.setTarget(lastHurt);
            return;
        }
        if (creaking.getTarget() == null || !creaking.getTarget().isAlive()) {
            AABB area = creaking.getBoundingBox().inflate(12.0D);
            List<Monster> threats = creaking.level().getEntitiesOfClass(Monster.class, area,
                    mob -> mob.isAlive() && mob != creaking && creaking.canAttack(mob)
                            && mob.getTarget() == owner);
            if (!threats.isEmpty()) {
                creaking.setTarget(threats.getFirst());
            }
        }
    }

    private static void dropResinClump(ServerLevel level, Creaking creaking) {
        ItemEntity drop = new ItemEntity(level,
                creaking.getX(), creaking.getY() + 0.5, creaking.getZ(),
                new ItemStack(Items.RESIN_CLUMP));
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                creaking.getX(), creaking.getY() + 1.0, creaking.getZ(),
                4, 0.2, 0.2, 0.2, 0.01);
    }

    private static void scareNearbyMonsters(Creaking creaking, ServerPlayer owner) {
        AABB area = creaking.getBoundingBox().inflate(SCARE_RANGE);
        List<Monster> mobs = creaking.level().getEntitiesOfClass(Monster.class, area, mob -> mob != creaking);
        for (Monster mob : mobs) {
            mob.getNavigation().moveTo(
                    mob.getX() + (mob.getX() - creaking.getX()),
                    mob.getY(),
                    mob.getZ() + (mob.getZ() - creaking.getZ()),
                    1.2D);
        }
    }

    private static void senseNearbyPlayers(Creaking creaking, ServerPlayer owner) {
        AABB area = creaking.getBoundingBox().inflate(24.0D);
        List<Player> players = creaking.level().getEntitiesOfClass(Player.class, area,
                player -> player != owner && player.isAlive() && !player.isSpectator());
        if (!players.isEmpty()) {
            owner.displayClientMessage(
                    Component.translatable("witchery.familiar.creaking.sense", players.size()), true);
        }
    }

    /**
     * Detach a creaking from its pale-garden heart so it can follow the witch.
     */
    private static void releaseFromHeart(Creaking creaking) {
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
