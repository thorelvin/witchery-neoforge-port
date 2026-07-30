package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.CoffinBlock;
import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.BedTaglockSavedData;
import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BroomEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WitchHunterEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.GlassGobletItem;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
@SuppressWarnings({"deprecation", "removal"})
public class WitcheryPlayerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveFirstJoinPrimer(player);
            syncKnownRecipeGuideAdvancements(player);
            syncToClient(player);
        }
    }

    /** Hands every player the Witches' Primer once — the entry point of the guide-book progression. */
    private static void giveFirstJoinPrimer(ServerPlayer player) {
        if (WitcheryAttachments.hasReceivedPrimer(player)) return;
        WitcheryAttachments.setReceivedPrimer(player);
        ItemStack primer = new ItemStack(WitcheryItems.WITCHES_PRIMER.get());
        if (!player.getInventory().add(primer)) {
            player.drop(primer, false);
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
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
                && event.getTarget() instanceof ServerPlayer target) {
            PacketDistributor.sendToPlayer(viewer,
                    SyncPlayerDataPayload.from(target, WitcheryAttachments.get(target)));
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathsRobeFireDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0.0F) return;
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.DEATHS_ROBE.get())) return;

        if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
        }
        event.setNewDamage(0.0F);
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        BlockPos pos = event.getPos();
        if (pos == null) return;
        BlockState state = event.getLevel().getBlockState(pos);
        WitcheryPlayerData data = WitcheryAttachments.get(event.getEntity());
        if (data.isWerewolf() && WerewolfShiftUtil.isTransformed(data)) {
            event.getEntity().displayClientMessage(Component.translatable("witchery.nosleep.wolf")
                    .withStyle(ChatFormatting.RED), true);
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            return;
        }
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
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof IronGolem
                && event.getNewAboutToBeSetTarget() instanceof VillageGuardEntity) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        if (WitcheryConfig.TOWN_ZOMBIE_ATTACK_REDUCTION_MODE.get() >= 2
                && event.getEntity() instanceof Zombie
                && event.getNewAboutToBeSetTarget() instanceof Villager) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;

        if (player.isInvisible()) {
            if (event.getEntity().level().getNearestPlayer(event.getEntity(), 16.0D) != player) {
                event.setNewAboutToBeSetTarget(null);
            }
            return;
        }

        if (event.getEntity().hasEffect(MobEffects.BLINDNESS)) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        if (event.getEntity() instanceof Creeper
                && WitcheryItems.isWitchesRobe(player.getItemBySlot(EquipmentSlot.CHEST))) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        if (event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
            if (event.getEntity() instanceof Zombie
                    && WitcheryAttachments.get(player).getVampireLevel() >= 10) {
                event.setNewAboutToBeSetTarget(null);
                return;
            }
            if (player.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.NECROMANCER_ROBES.get())) {
                event.setNewAboutToBeSetTarget(null);
            }
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

        if (player.tickCount % 40 == 0) {
            tickStoryBookCollection(player);
        }
        if (player.tickCount % 200 == 0) {
            syncKnownRecipeGuideAdvancements(player);
        }

        WitcheryPlayerData dataEveryTick = WitcheryAttachments.get(player);
        if (dataEveryTick.hasSoaringBuff()) dataEveryTick.tickSoaring();
        if (dataEveryTick.hasGraveBrew()) dataEveryTick.tickGrave();
        if (dataEveryTick.getHowlCooldown() > 0) dataEveryTick.setHowlCooldown(dataEveryTick.getHowlCooldown() - 1);
        dataEveryTick.tickTormentPortal();
        tickGrotesqueAura(player, dataEveryTick);

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
        WitchHunterEntity.tickBlackMagicHunterPressure(player);

        ServerLevel level = (ServerLevel) player.level();
        tickDeathsFootwearWaterWalk(player);
        tickIcySlippersEffect(player, level);

        if (player.tickCount % 20 != 0) return;

        tickDeathsHoodEffect(player, level);

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        tickDepthsBrew(player, data);
        tickSeepingShoesEffect(player, level);
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
        GlassGobletItem.tryFillChickenBloodFromRitual(level, player, killed);
        // A Witch-Hand creature sacrifice already granted the power; don't also grant it via altar-kill.
        if (killed.getPersistentData().getBooleanOr(com.emoniph.witchery.util.Enslaved.SACRIFICED_TAG, false)) return;
        CreaturePowerType power = creaturePowerFor(killed);

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
        int charges = creaturePowerChargesFor(killed, power);
        if (!power.name().equals(currentType)) {
            data.setCreaturePower(power.name(), charges);
        } else {
            data.addCreaturePowerCharges(charges);
        }
        altar.consumePower(50);
        syncToClient(player);
        player.displayClientMessage(Component.translatable("witchery.creature_power.absorbed",
                power.displayName(), data.getCreaturePowerCharges()).withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    public static CreaturePowerType creaturePowerFor(LivingEntity killed) {
        if (killed instanceof CaveSpider || killed instanceof Spider) return CreaturePowerType.SPIDER;
        if (killed instanceof Creeper) return CreaturePowerType.CREEPER;
        if (killed instanceof Bat || killed instanceof OwlFamiliarEntity) return CreaturePowerType.BAT;
        if (killed instanceof Squid) return CreaturePowerType.SQUID;
        if (killed instanceof Ghast) return CreaturePowerType.GHAST;
        if (killed instanceof Blaze) return CreaturePowerType.BLAZE;
        if (killed instanceof ZombifiedPiglin) return CreaturePowerType.PIGMAN;
        if (killed instanceof Zombie) return CreaturePowerType.ZOMBIE;
        if (killed instanceof Skeleton) return CreaturePowerType.SKELETON;
        if (killed instanceof MagmaCube || killed instanceof Slime || killed instanceof ToadFamiliarEntity) {
            return CreaturePowerType.JUMP;
        }
        if (killed instanceof Silverfish
                || killed instanceof Ocelot
                || killed instanceof Cat
                || killed instanceof Wolf
                || killed instanceof Horse) {
            return CreaturePowerType.SPEED;
        }
        if (killed instanceof EnderMan) return CreaturePowerType.ENDERMAN;
        if (killed instanceof Sheep
                || killed instanceof Cow
                || killed instanceof Chicken
                || killed instanceof Pig
                || killed instanceof Villager
                || killed instanceof MushroomCow) {
            return CreaturePowerType.HEAL;
        }
        return null;
    }

    public static int creaturePowerChargesFor(LivingEntity killed, CreaturePowerType power) {
        if (power == CreaturePowerType.HEAL) {
            return killed instanceof Villager || killed instanceof MushroomCow ? 2 : 1;
        }
        return 10;
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
                    player.displayClientMessage(Component.translatable("witchery.lord_of_torment.defeated")
                            .withStyle(ChatFormatting.DARK_PURPLE), false);
                    continue;
                }
            }

            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawn = new BlockPos(0, 64, 0);
                player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        java.util.Set.of(), 0f, 0f, false);
                player.displayClientMessage(Component.translatable("witchery.lord_of_torment.defeated")
                        .withStyle(ChatFormatting.DARK_PURPLE), false);
            }
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (com.emoniph.witchery.item.RubySlippersItem.tryActivate(player, event.getRawText())) {
            event.setCanceled(true);
        }
    }

    private static void tickDeathsHoodEffect(ServerPlayer player, ServerLevel level) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.DEATHS_HOOD.get())) return;

        int offset = (int)(level.getGameTime() % 10L);
        if (offset == 1) {
            tickDeathsHoodFreeze(player);
        } else if (offset == 2
                && level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, player.blockPosition()) < 8) {
            MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
            if (nightVision == null || nightVision.getDuration() <= 300) {
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
            }
        }
    }

    private static void tickDeathsHoodFreeze(ServerPlayer player) {
        if (!WitcheryConfig.ALLOW_DEATHS_HOOD_FREEZE.get()) return;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookEnd = eyePos.add(player.getLookAngle().scale(16.0));
        AABB searchBox = player.getBoundingBox().expandTowards(player.getLookAngle().scale(16.0)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, lookEnd, searchBox,
                e -> e instanceof LivingEntity && e != player,
                16.0 * 16.0);
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity victim)) return;
        if (!victim.hasLineOfSight(player)) return;
        if (!victim.hasEffect(MobEffects.SLOWNESS)) {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        }
    }

    private static void tickDeathsFootwearWaterWalk(ServerPlayer player) {
        if (!player.isInWater()) return;
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.DEATHS_SHOES.get())) return;

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y >= 0.0D) return;

        player.setDeltaMovement(velocity.x, velocity.y + 0.1D, velocity.z);
        player.fallDistance = 0.0F;
    }

    private static void tickIcySlippersEffect(ServerPlayer player, ServerLevel level) {
        ItemStack shoes = player.getItemBySlot(EquipmentSlot.FEET);
        if (!WitcheryItems.isIcySlippers(shoes)) return;

        int y = Mth.floor(player.getY() - 1.0D);
        boolean frozeWater = false;
        boolean hardenedLava = false;
        for (int i = 0; i < 4; i++) {
            int x = Mth.floor(player.getX() + (double) ((i % 2 * 2 - 1) * 0.5F));
            int z = Mth.floor(player.getZ() + (double) (((i / 2) % 2 * 2 - 1) * 0.5F));
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.is(Blocks.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                frozeWater = true;
            } else if (state.is(Blocks.LAVA)) {
                level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                hardenedLava = true;
                if (level.random.nextInt(10) == 0) {
                    shoes.hurtAndBreak(1, player, EquipmentSlot.FEET);
                }
            }
        }
        if (frozeWater || hardenedLava) {
            level.playSound(null, player.blockPosition(),
                    hardenedLava ? net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH : net.minecraft.sounds.SoundEvents.GLASS_PLACE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, hardenedLava ? 1.5F : 1.25F);
            level.sendParticles(hardenedLava ? net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE
                            : net.minecraft.core.particles.ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1D, player.getZ(),
                    hardenedLava ? 6 : 4, 0.25D, 0.05D, 0.25D, 0.0D);
        }
    }

    /** Distinct story books a player must collect to unlock the Spectral Coven. */
    private static final int STORY_UNLOCK_THRESHOLD = 5;

    private static final List<GuideRecipeGate> GUIDE_RECIPE_GATES = List.of(
            guideRecipeGate("book_oven", "guide/primer", "has_book"),
            guideRecipeGate("book_herbology", "guide/oven", "has_book"),
            guideRecipeGate("book_infusions", "guide/herbology", "has_book"),
            guideRecipeGate("book_distilling", "guide/infusions", "has_book"),
            guideRecipeGate("book_circle_magic", "guide/distilling", "has_book"),
            guideRecipeGate("book_burning", "guide/circle_magic", "has_book"),
            guideRecipeGate("book_wands", "guide/burning", "has_book"),
            guideRecipeGate("book_poppets", "guide/wands", "has_book"),
            guideRecipeGate("book_spirit_world", "guide/poppets", "has_book"),
            guideRecipeGate("book_creatures", "guide/spirit_world", "has_book"),
            guideRecipeGate("book_transformations", "guide/creatures", "has_book"),
            guideRecipeGate("book_transformations", "guide/transformations", "has_book"),
            guideRecipeGate("book_herbology", "guide/obtained/exhale_of_horned_one", "obtained"),
            guideRecipeGate("book_infusions", "guide/obtained/mutandis", "obtained"),
            guideRecipeGate("book_distilling", "guide/obtained/redstone_soup", "obtained"),
            guideRecipeGate("book_circle_magic", "guide/obtained/diamond_vapour", "obtained"),
            guideRecipeGate("book_burning", "guide/obtained/chalk", "obtained"),
            guideRecipeGate("book_wands", "guide/obtained/fetish_scarecrow", "obtained"),
            guideRecipeGate("book_poppets", "guide/obtained/mystic_branch", "obtained"),
            guideRecipeGate("book_spirit_world", "guide/obtained/poppet", "obtained"),
            guideRecipeGate("book_creatures", "guide/obtained/brew_of_flowing_spirit", "obtained")
    );

    private static GuideRecipeGate guideRecipeGate(String recipePath, String advancementPath, String criterion) {
        return new GuideRecipeGate(ResourceKey.create(Registries.RECIPE, id(recipePath)), id(advancementPath), criterion);
    }

    /**
     * Old worlds can have the vanilla recipe-book unlock without the newer hidden guide advancement
     * that Modonomicon checks. Mirror known subject-book recipes back into those guide advancements so
     * the Primer never hides pages the player has already earned.
     */
    private static void syncKnownRecipeGuideAdvancements(ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }

        var advancements = server.getAdvancements();
        var recipeBook = player.getRecipeBook();
        for (GuideRecipeGate gate : GUIDE_RECIPE_GATES) {
            if (!recipeBook.contains(gate.recipeId())) {
                continue;
            }
            var holder = advancements.get(gate.advancementId());
            if (holder != null) {
                player.getAdvancements().award(holder, gate.criterion());
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    private record GuideRecipeGate(ResourceKey<Recipe<?>> recipeId, Identifier advancementId, String criterion) {
    }

    /**
     * Scans the player's inventory for in-world story books (vanilla written books tagged with a hidden
     * {@code witchery_story} id 1-12) and records which distinct stories have been found. Crossing the
     * {@link #STORY_UNLOCK_THRESHOLD} unlocks the Rite of the Gathered Lives and reveals the Spectral
     * Coven entry in the Primer. Stops scanning once unlocked.
     */
    private static void tickStoryBookCollection(ServerPlayer player) {
        if (WitcheryAttachments.isGatheredLivesUnlocked(player)) return;

        int before = WitcheryAttachments.storiesFoundCount(player);
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) continue;
            int id = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                            net.minecraft.world.item.component.CustomData.EMPTY)
                    .copyTag().getIntOr("witchery_story", 0);
            if (id >= 1 && id <= 12) {
                WitcheryAttachments.addStoryFound(player, id);
            }
        }

        int after = WitcheryAttachments.storiesFoundCount(player);
        if (after > before && after < STORY_UNLOCK_THRESHOLD) {
            player.displayClientMessage(Component.translatable("witchery.gathered_lives.progress",
                    after, STORY_UNLOCK_THRESHOLD).withStyle(ChatFormatting.GRAY), true);
        }
        if (before < STORY_UNLOCK_THRESHOLD && after >= STORY_UNLOCK_THRESHOLD) {
            unlockGatheredLives(player);
        }
    }

    private static void unlockGatheredLives(ServerPlayer player) {
        WitcheryAttachments.setGatheredLivesUnlocked(player);
        net.minecraft.advancements.AdvancementHolder holder = player.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath("witchery", "guide/gathered_lives"));
        if (holder != null) {
            player.getAdvancements().award(holder, "unlocked");
        }
        player.displayClientMessage(Component.translatable("witchery.gathered_lives.unlocked")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        player.level().playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
    }

    public static void syncToClient(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    /**
     * Source: Infusion.handleBrewGrotesqueEffect (1.7.10) — runs every tick while
     * grotesqueTicks > 0. Pushes nearby living entities away from the player
     * within a 4-block radius, skipping bosses, golems, and witches.
     */
    private static void tickGrotesqueAura(ServerPlayer player, WitcheryPlayerData data) {
        if (!data.hasGrotesqueAura()) return;
        data.tickGrotesque();

        AABB bounds = AABB.ofSize(player.position(), 8.0, 8.0, 8.0); // ±4 blocks each side
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (entity == player) continue;
            // Source exclusions: Demon, Wither, Horned Huntsman, Dragon, Golem, Witch
            if (entity instanceof DemonEntity
                    || entity instanceof WitherBoss
                    || entity instanceof HornedHuntsmanEntity
                    || entity instanceof EnderDragon
                    || entity instanceof IronGolem
                    || entity instanceof SnowGolem
                    || entity instanceof Witch
                    || entity instanceof CovenWitchEntity) continue;

            double dx = player.getX() - entity.getX();
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.001 || dist > 4.0) continue;

            // Same formula as pushProtectionMob in RitualExecutor
            double scale = 0.22 / dist;
            entity.push(
                    Mth.clamp(-dx * scale, -0.22, 0.22),
                    Mth.clamp(-dy * scale, -0.10, 0.12),
                    Mth.clamp(-dz * scale, -0.22, 0.22));
            entity.hurtMarked = true;
        }
        if (!data.hasGrotesqueAura()) {
            syncToClient(player);
        }
    }

    private static void tickDepthsBrew(ServerPlayer player, WitcheryPlayerData data) {
        if (!data.hasDepthsBuff()) return;

        if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 6000, 0));
        }

        if (player.isInWater()) {
            if (player.hasEffect(MobEffects.WITHER)) {
                player.removeEffect(MobEffects.WITHER);
            }
        } else if (!player.hasEffect(MobEffects.WITHER)) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }

        int remaining = data.getDepthsTicks() - 20;
        data.setDepthsTicks(remaining);
        if (remaining <= 0) {
            if (player.hasEffect(MobEffects.WATER_BREATHING)) {
                player.removeEffect(MobEffects.WATER_BREATHING);
            }
            if (player.hasEffect(MobEffects.POISON)) {
                player.removeEffect(MobEffects.POISON);
            }
        }
    }

    private static void tickSeepingShoesEffect(ServerPlayer player, ServerLevel level) {
        if (!player.onGround()) return;
        if (!player.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.SEEPING_SHOES.get())) return;

        boolean removed = false;
        if (player.hasEffect(MobEffects.POISON)) {
            player.removeEffect(MobEffects.POISON);
            removed = true;
        }
        if (player.hasEffect(MobEffects.WITHER)) {
            player.removeEffect(MobEffects.WITHER);
            removed = true;
        }
        if (!removed) return;

        BlockPos center = player.blockPosition();
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > 9) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock bonemealable
                    && bonemealable.isValidBonemealTarget(level, pos, state)
                    && bonemealable.isBonemealSuccess(level, level.random, pos, state)) {
                bonemealable.performBonemeal(level, level.random, pos, state);
                grown++;
            }
        }
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GRASS_PLACE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.45F, 0.9F + level.random.nextFloat() * 0.25F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 0.2D, player.getZ(),
                Math.max(6, Math.min(18, grown + 6)), 0.35D, 0.08D, 0.35D, 0.0D);
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
                    demon.setDemonicNightmareOwner(player.getUUID());
                    demon.finalizeSpawn(spiritWorld, spiritWorld.getCurrentDifficultyAt(demon.blockPosition()),
                            net.minecraft.world.entity.EntitySpawnReason.EVENT, null);
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
