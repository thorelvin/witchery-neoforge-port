package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.data.BedTaglockSavedData;
import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BroomEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber
public class WitcheryPlayerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        WitcheryPlayerData original = WitcheryAttachments.get(event.getOriginal());
        WitcheryPlayerData fresh = WitcheryAttachments.get(event.getEntity());
        fresh.copyFrom(original, !event.isWasDeath());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.isManifestedGhost()) {
            event.setCanceled(true);
            player.setHealth(1.0f);
            SpiritWorldHelper.returnGhostPlayerToSpiritWorld(player);
            return;
        }
        if (!player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;
        event.setCanceled(true);
        player.setHealth(1.0f);
        SpiritWorldHelper.exit(player);
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        BlockPos pos = event.getPos();
        if (pos == null) return;
        BlockState state = event.getLevel().getBlockState(pos);
        if (event.getProblem() == null) {
            if (!(event.getLevel() instanceof ServerLevel level) || !(state.getBlock() instanceof BedBlock)) {
                return;
            }
            BlockPos headPos = state.getValue(BedBlock.PART) == BedPart.HEAD
                    ? pos
                    : pos.relative(BedBlock.getConnectedDirection(state));
            BedTaglockSavedData.setEntry(level, headPos, event.getEntity().getUUID(), event.getEntity().getName().getString());
            return;
        }
        if (!state.is(WitcheryBlocks.COFFIN.get())) return;
        WitcheryPlayerData data = WitcheryAttachments.get(event.getEntity());
        if (data.isVampire()) {
            event.setProblem(null);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getVehicle() instanceof BroomEntity) {
            event.setDistance(0.0F);
        }
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        Set<BlockPos> processed = new HashSet<>();
        for (ServerPlayer player : players) {
            BlockPos center = player.blockPosition();
            for (BlockPos scan : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                if (!processed.add(scan.immutable())) continue;
                if (!level.getBlockState(scan).is(WitcheryBlocks.DREAM_CATCHER.get())) continue;
                if (level.getBlockEntity(scan) instanceof DreamCatcherBlockEntity catcher
                        && catcher.getWeave() != null
                        && catcher.hasPendingBuff()) {
                    catcher.consumePendingBuff(level);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WitcheryPlayerData dataEveryTick = WitcheryAttachments.get(player);
        if (dataEveryTick.hasSoaringBuff()) dataEveryTick.tickSoaring();
        if (dataEveryTick.getHowlCooldown() > 0) dataEveryTick.setHowlCooldown(dataEveryTick.getHowlCooldown() - 1);
        dataEveryTick.tickTormentPortal();

        if (player.tickCount % 100 == 0
                && dataEveryTick.isManifestedGhost()
                && !player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            if (dataEveryTick.shouldSkipManifestationTick()) {
                dataEveryTick.setSkipManifestationTick(false);
            } else {
                int remaining = Math.max(0, dataEveryTick.getManifestationTime() - 5);
                dataEveryTick.setManifestationTime(remaining);
                if (remaining == 60 || remaining == 30 || remaining == 15) {
                    player.displayClientMessage(Component.translatable("witchery.ritual.manifestation.countdown", remaining), false);
                }
            }

            if (dataEveryTick.getManifestationTime() == 0) {
                SpiritWorldHelper.returnGhostPlayerToSpiritWorld(player);
            } else {
                syncToClient(player);
            }
        }

        if (player.tickCount % 100 == 0
                && dataEveryTick.isInSpiritWorld()
                && !dataEveryTick.isSpiritWorldDream()
                && player.level() instanceof ServerLevel spiritWorld
                && spiritWorld.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            SpiritWorldHelper.tickNightmareRespawn(player, spiritWorld, dataEveryTick);
            if (dataEveryTick.isDemonicNightmare()) {
                tickDemonicNightmare(player, spiritWorld);
            }
        }

        if (player.tickCount % 20 != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType infusion = data.getInfusionType();

        if (infusion == InfusionType.LIGHT) {
            if ((level.getDayTime() % 24000 < 13000) && level.canSeeSky(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, true, false));
            }
        } else if (infusion == InfusionType.OVERWORLD) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 30, 0, true, false));
        } else if (infusion == InfusionType.OTHERWHERE) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false));
        } else if (infusion == InfusionType.INFERNAL) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false));
            if (!(level.getDayTime() % 24000 < 13000)) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 0, true, false));
            }
        }

        if (data.isVampire()) {
            if ((level.getDayTime() % 24000 < 13000) && level.canSeeSky(player.blockPosition()) && !player.isInWater()) {
                player.hurt(level.damageSources().onFire(), 1.0f);
            }
            if (!(level.getDayTime() % 24000 < 13000)) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 0, true, false));
            }
        }

        if (data.isWerewolf()) {
            long time = level.getDayTime() % 24000;
            boolean isNight = time > 13000 && time < 23000;
            if (isNight && (level.getDayTime() / 24000L) % 8L == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false));
            }
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) player.level();
        WitcheryPlayerData data = WitcheryAttachments.get(player);

        var killed = event.getEntity();
        CreaturePowerType power = null;
        if (killed instanceof Zombie) power = CreaturePowerType.SPEED;
        else if (killed instanceof Spider) power = CreaturePowerType.JUMP;
        else if (killed instanceof Creeper) power = CreaturePowerType.HEAL;
        else if (killed instanceof EnderMan) power = CreaturePowerType.ENDERMAN;
        else if (killed instanceof Bat) power = CreaturePowerType.BAT;

        if (power == null) return;

        BlockPos center = player.blockPosition();
        AltarBlockEntity altar = null;
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (level.getBlockEntity(scan) instanceof AltarBlockEntity foundAltar) {
                altar = foundAltar;
                break;
            }
        }
        if (altar == null || altar.getPower() < 50) return;

        String currentType = data.getCreaturePowerType();
        if (!power.name().equals(currentType)) {
            data.setCreaturePower(power.name(), 3);
        } else {
            data.addCreaturePowerCharges(3);
        }
        altar.consumePower(50);
        syncToClient(player);
        player.displayClientMessage(Component.literal(
                "§5Absorbed " + power.displayName() + " power! Charges: " + data.getCreaturePowerCharges()), false);
    }

    @SubscribeEvent
    public static void onNightmareDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof NightmareEntity nightmare)) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        if (event.getSource().getEntity() instanceof ServerPlayer player && nightmare.matchesVictim(player)) {
            WitcheryAttachments.get(player).setLastNightmareKillGameTime(level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLordOfTormentDefeated(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LordOfTormentEntity)) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(WitcheryDimensions.TORMENT)) return;

        for (ServerPlayer player : new ArrayList<>(level.players())) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            Optional<BlockPos> returnPos = data.getReturnPos();
            String returnDim = data.getReturnDimension();

            if (returnPos.isPresent() && !returnDim.isEmpty()) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(returnDim));
                ServerLevel destination = level.getServer().getLevel(key);
                if (destination != null) {
                    BlockPos pos = returnPos.get();
                    player.teleportTo(destination, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                            java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                    data.clearReturnPos();
                    syncToClient(player);
                    player.displayClientMessage(Component.literal("§5The Lord of Torment falls. You are returned home."), false);
                    continue;
                }
            }

            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawn = new BlockPos(0, 64, 0);
                player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        java.util.Set.of(), 0f, 0f, false);
                player.displayClientMessage(Component.literal("§5The Lord of Torment falls. You are returned home."), false);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    private static void tickDemonicNightmare(ServerPlayer player, ServerLevel spiritWorld) {
        var random = spiritWorld.getRandom();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double chance = random.nextDouble();

        // Old Witchery chose one event branch from a single roll instead of stacking several at once.
        if (chance < 0.50) {
            spawnFallingFireball(spiritWorld,
                    px - 2.0 + random.nextInt(5),
                    py + 15.0,
                    pz - 2.0 + random.nextInt(5),
                    false);
        } else if (chance < 0.65) {
            spawnFallingFireball(spiritWorld,
                    px - 2.0 + random.nextInt(5),
                    py + 15.0,
                    pz - 2.0 + random.nextInt(5),
                    true);
        } else if (chance < 0.75) {
            var area = new net.minecraft.world.phys.AABB(px - 16, py - 8, pz - 16, px + 16, py + 8, pz + 16);
            int hostiles = spiritWorld.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, area).size();
            long demons = spiritWorld.getEntitiesOfClass(com.emoniph.witchery.entity.DemonEntity.class, area).stream().count();
            if (hostiles < 10 && demons < 2) {
                com.emoniph.witchery.entity.DemonEntity demon =
                        WitcheryEntities.DEMON.get().create(spiritWorld, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (demon != null) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 4 + random.nextDouble() * 4;
                    demon.setPos(px + radius * Math.cos(angle), py, pz + radius * Math.sin(angle));
                    demon.setTarget(player);
                    spiritWorld.addFreshEntity(demon);
                }
            }
        }
    }

    private static void spawnFallingFireball(ServerLevel level, double x, double y, double z, boolean large) {
        var dir = new net.minecraft.world.phys.Vec3(0.0, -0.2, 0.0);
        if (!large) {
            var fireball = new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(level, x, y, z, dir);
            level.addFreshEntity(fireball);
        } else {
            var fireball = new net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball(
                    level, level.getNearestPlayer(x, y, z, 32, false), dir, 1);
            fireball.setPos(x, y, z);
            level.addFreshEntity(fireball);
        }
    }
}
