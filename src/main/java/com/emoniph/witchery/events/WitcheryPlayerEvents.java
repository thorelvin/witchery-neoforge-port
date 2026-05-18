package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.emoniph.witchery.blocks.CoffinBlock;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber
public class WitcheryPlayerEvents {

    // Sync player data to client when they log in or change dimension
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToClient(sp);
        }
    }

    // Carry witch data through death/respawn
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        WitcheryPlayerData original = WitcheryAttachments.get(event.getOriginal());
        WitcheryPlayerData fresh    = WitcheryAttachments.get(event.getEntity());
        fresh.copyFrom(original, !event.isWasDeath());
    }

    // Re-sync after respawn
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncToClient(sp);
        }
    }

    // Death in Spirit World → exit back to Overworld instead of dying
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;
        event.setCanceled(true);
        player.setHealth(1.0f);
        SpiritWorldHelper.exit(player);
    }

    // Vampire day-sleep: allow vampires to sleep in coffins during the day.
    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        // Only override the "not the right time" problem
        if (event.getProblem() == null) return; // already allowed
        BlockPos pos = event.getPos();
        if (!event.getLevel().getBlockState(pos).is(WitcheryBlocks.COFFIN.get())) return;
        WitcheryPlayerData data = WitcheryAttachments.get(event.getEntity());
        if (data.isVampire()) {
            event.setProblem(null); // allow daytime sleep in coffin for vampires
        }
    }

    // Backup path: SleepFinishedTimeEvent fires when time skips due to sleeping.
    // The BE ticker handles the normal case; this ensures single-player and edge cases fire too.
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
                if (level.getBlockEntity(scan) instanceof DreamCatcherBlockEntity be && be.getWeave() != null) {
                    be.applyEffectsToNearby(level);
                }
            }
        }
    }

    // Passive effects: infusion types, vampirism, lycanthropy — runs every 20 ticks server-side
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Tick transient per-tick counters
        WitcheryPlayerData dataEveryTick = WitcheryAttachments.get(player);
        if (dataEveryTick.hasSoaringBuff()) dataEveryTick.tickSoaring();
        if (dataEveryTick.getHowlCooldown() > 0) dataEveryTick.setHowlCooldown(dataEveryTick.getHowlCooldown() - 1);
        dataEveryTick.tickTormentPortal();

        // Nightmare periodic events — every 100 ticks for nightmare players in Spirit World
        if (player.tickCount % 100 == 0 && dataEveryTick.isInSpiritWorld() && !dataEveryTick.isSpiritWorldDream()
                && player.level() instanceof ServerLevel sw
                && sw.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            com.emoniph.witchery.spiritworld.SpiritWorldHelper.tickNightmareRespawn(player, sw, dataEveryTick);
            if (dataEveryTick.isDemonicNightmare()) {
                tickDemonicNightmare(player, sw);
            }
        }

        if (player.tickCount % 20 != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType infusion = data.getInfusionType();

        // Infusion passives
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

        // Vampire passives
        if (data.isVampire()) {
            if ((level.getDayTime() % 24000 < 13000) && level.canSeeSky(player.blockPosition()) && !player.isInWater()) {
                player.hurt(level.damageSources().onFire(), 1.0f);
            }
            if (!(level.getDayTime() % 24000 < 13000)) {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 0, true, false));
            }
        }

        // Werewolf passives — Strength+Speed on full moon
        if (data.isWerewolf()) {
            long time = level.getDayTime() % 24000;
            boolean isNight = time > 13000 && time < 23000;
            if (isNight && (level.getDayTime() / 24000L) % 8L == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false));
            }
        }
    }

    // Creature power acquisition: killing a mob near an altar grants charges
    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) player.level();
        WitcheryPlayerData data = WitcheryAttachments.get(player);

        // Determine power type from killed mob (entity is the thing dying)
        var killed = event.getEntity();
        CreaturePowerType power = null;
        if (killed instanceof Zombie)   power = CreaturePowerType.SPEED;
        else if (killed instanceof Spider)   power = CreaturePowerType.JUMP;
        else if (killed instanceof Creeper)  power = CreaturePowerType.HEAL;
        else if (killed instanceof EnderMan) power = CreaturePowerType.ENDERMAN;
        else if (killed instanceof Bat)      power = CreaturePowerType.BAT;

        if (power == null) return;

        // Check for nearby altar within 8 blocks
        BlockPos center = player.blockPosition();
        AltarBlockEntity altar = null;
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (level.getBlockEntity(scan) instanceof AltarBlockEntity be) {
                altar = be;
                break;
            }
        }
        if (altar == null || altar.getPower() < 50) return;

        // Set or add charges; switching power type resets
        String currentType = data.getCreaturePowerType();
        if (!power.name().equals(currentType)) {
            data.setCreaturePower(power.name(), 3);
        } else {
            data.addCreaturePowerCharges(3);
        }
        altar.consumePower(50);
        syncToClient(player);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5Absorbed " + power.displayName() + " power! Charges: " + data.getCreaturePowerCharges()), false);
    }

    // Nightmare killed in Spirit World nightmare mode → 60 % chance to respawn a replacement
    @SubscribeEvent
    public static void onNightmareDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof NightmareEntity)) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return;

        // Find a nearby nightmare-mode player
        net.minecraft.world.phys.AABB bb = new net.minecraft.world.phys.AABB(event.getEntity().blockPosition()).inflate(32);
        for (ServerPlayer player : sl.getEntitiesOfClass(ServerPlayer.class, bb)) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.isInSpiritWorld() && !data.isSpiritWorldDream()) {
                if (sl.random.nextFloat() < 0.6f) {
                    NightmareEntity spawn = WitcheryEntities.NIGHTMARE.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                    if (spawn != null) {
                        double angle = sl.random.nextDouble() * Math.PI * 2;
                        double dist  = 10 + sl.random.nextDouble() * 6;
                        spawn.setPos(player.getX() + dist * Math.cos(angle), player.getY(), player.getZ() + dist * Math.sin(angle));
                        sl.addFreshEntity(spawn);
                    }
                }
                break;
            }
        }
    }

    // Lord of Torment defeated in Torment dimension → return all Torment players home
    @SubscribeEvent
    public static void onLordOfTormentDefeated(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LordOfTormentEntity)) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(WitcheryDimensions.TORMENT)) return;

        for (ServerPlayer player : new ArrayList<>(sl.players())) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            Optional<BlockPos> returnOpt = data.getReturnPos();
            String returnDim = data.getReturnDimension();

            if (returnOpt.isPresent() && !returnDim.isEmpty()) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                        Identifier.parse(returnDim));
                ServerLevel dest = sl.getServer().getLevel(key);
                if (dest != null) {
                    BlockPos rp = returnOpt.get();
                    player.teleportTo(dest, rp.getX() + 0.5, (double)(rp.getY()), rp.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
                    data.clearReturnPos();
                    syncToClient(player);
                    player.displayClientMessage(Component.literal("§5The Lord of Torment falls. You are returned home."), false);
                    continue;
                }
            }
            // No saved position — fall back to overworld shared spawn
            ServerLevel overworld = sl.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawn = new net.minecraft.core.BlockPos(0, 64, 0);
                player.teleportTo(overworld, spawn.getX() + 0.5, (double)(spawn.getY()), spawn.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), 0f, 0f, false);
                player.displayClientMessage(Component.literal("§5The Lord of Torment falls. You are returned home."), false);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Demonic nightmare events (§13) ────────────────────────────────────────

    private static void tickDemonicNightmare(ServerPlayer player, ServerLevel sw) {
        var rand = sw.getRandom();
        double px = player.getX(), py = player.getY(), pz = player.getZ();

        // 50 % — small fireball spawned 3–6 blocks above, aimed at player
        if (rand.nextFloat() < 0.50f) {
            spawnFireballAt(sw, px, py + 3 + rand.nextDouble() * 3, pz, px, py + 1.6, pz, 1);
        }

        // 65 % — large fireball from a random direction, aimed at player
        if (rand.nextFloat() < 0.65f) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double r = 6 + rand.nextDouble() * 4;
            spawnFireballAt(sw, px + r * Math.cos(angle), py + 8, pz + r * Math.sin(angle),
                    px, py + 1.6, pz, 3);
        }

        // 75 % — spawn demon if area not overcrowded
        if (rand.nextFloat() < 0.75f) {
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(px - 16, py - 8, pz - 16, px + 16, py + 8, pz + 16);
            int hostiles = sw.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, area).size();
            long demons  = sw.getEntitiesOfClass(com.emoniph.witchery.entity.DemonEntity.class, area).stream().count();
            if (hostiles < 10 && demons < 2) {
                com.emoniph.witchery.entity.DemonEntity demon =
                        WitcheryEntities.DEMON.get().create(sw, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (demon != null) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double r = 4 + rand.nextDouble() * 4;
                    demon.setPos(px + r * Math.cos(angle), py, pz + r * Math.sin(angle));
                    demon.setTarget(player);
                    sw.addFreshEntity(demon);
                }
            }
        }
    }

    /** Spawns a fireball entity aimed from (ox,oy,oz) toward (tx,ty,tz). power = explosion power. */
    private static void spawnFireballAt(ServerLevel level,
                                         double ox, double oy, double oz,
                                         double tx, double ty, double tz,
                                         int power) {
        double dx = tx - ox, dy = ty - oy, dz = tz - oz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) return;
        dx /= len; dy /= len; dz /= len;

        var dir = new net.minecraft.world.phys.Vec3(dx, dy, dz);
        if (power == 1) {
            var fb = new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(level, ox, oy, oz, dir);
            level.addFreshEntity(fb);
        } else {
            // LargeFireball requires a LivingEntity shooter; use the nearest player as proxy
            var fb = new net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball(
                    level, level.getNearestPlayer(ox, oy, oz, 32, false), dir, power);
            fb.setPos(ox, oy, oz);
            level.addFreshEntity(fb);
        }
    }
}
