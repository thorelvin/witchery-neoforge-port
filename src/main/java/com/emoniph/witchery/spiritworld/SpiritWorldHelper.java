package com.emoniph.witchery.spiritworld;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SpiritWorldHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("witchery");

    /**
     * Items that always stay with the player when entering the Spirit World (not stripped).
     * Source ({@code WorldProviderDreamWorld.sendPlayerToSpiritWorld}) carries the Icy Needle
     * and Mutandis in; the port also keeps Mutandis Extremis and — as a deliberate addition —
     * the Witches' Primer, so the player can read the guide while dreaming.
     */
    private static boolean isAlwaysCarried(ItemStack stack) {
        return stack.is(WitcheryItems.ICY_NEEDLE.get())
                || stack.is(WitcheryItems.MUTANDIS.get())
                || stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get())
                || stack.is(WitcheryItems.WITCHES_PRIMER.get());
    }

    /**
     * Items that transfer back to the Overworld on exit. Source
     * ({@code WorldProviderDreamWorld.returnPlayerToOverworld}) returns Wispy Cotton, Disturbed
     * Cotton, Mellifluous Hunger, Icy Needle, Brew of Flowing Spirit and Subdued Spirit; the port
     * also returns the Subdued Village Spirit and the brought-in Witches' Primer.
     */
    private static boolean isTransferable(ItemStack stack) {
        Item it = stack.getItem();
        return it == WitcheryItems.ICY_NEEDLE.get()
                || it == WitcheryItems.WISPY_COTTON.get()
                || it == WitcheryItems.DISTURBED_COTTON.get()
                || it == WitcheryItems.MELLIFLUOUS_HUNGER.get()
                || WitcheryItems.isBrewOfFlowingSpirit(stack)
                || it == WitcheryItems.SUBDUED_SPIRIT.get()
                || it == WitcheryItems.SUBDUED_VILLAGE_SPIRIT.get()
                || it == WitcheryItems.WITCHES_PRIMER.get();
    }

    /**
     * Items that are both kept on entry and carried back on exit (Icy Needle, Witches' Primer).
     * These must be excluded from the saved Overworld snapshot: the live copy is re-added from the
     * transferable list on exit, so leaving it in the snapshot too would duplicate it. Excluding it
     * means the player returns with the actual leftover count (e.g. only the unused needles).
     */
    private static boolean isCarriedRoundTrip(ItemStack stack) {
        return isAlwaysCarried(stack) && isTransferable(stack);
    }

    private SpiritWorldHelper() {}

    // ── Entry ────────────────────────────────────────────────────────────────

    public static void enter(ServerPlayer player) {
        enter(player, WitcheryConfig.NIGHTMARE_CHANCE_PERCENT.get() / 100.0);
    }

    public static void enter(ServerPlayer player, double nightmareChance) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!player.level().dimension().equals(Level.OVERWORLD)
                || data.isInSpiritWorld()
                || data.isManifestedGhost()) {
            return;
        }

        ServerLevel sw = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (sw == null) return;

        data.clearManifestationState();
        data.setReturnPos(player.blockPosition(), player.level().dimension());

        // Nightmare chance — block scan around sleeping position
        NightmareResult nightmare = calculateNightmare(player, nightmareChance);
        data.setSpiritWorldIsDream(!nightmare.isNightmare);
        data.setDemonicNightmare(nightmare.isDemonic);
        data.setInSpiritWorld(true);

        // Save overworld vitals before stripping inventory / teleporting
        data.saveSpiritWorldVitals(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel());

        // Reset to full health/hunger in Spirit World
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);

        // Spawn sleeping corpse in the player's current dimension
        spawnCorpse(player, data);

        saveAndStripInventory(player, data);

        double[] pos = prepareSpiritWorldEntry(sw, player);
        player.teleportTo(sw, pos[0], (double)(pos[1]), pos[2], java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);

        if (!nightmare.isNightmare) {
            player.displayClientMessage(Component.literal("You drift into a peaceful dream..."), false);
        } else if (nightmare.isDemonic) {
            player.displayClientMessage(Component.literal("A demonic nightmare engulfs you..."), false);
            spawnNightmare(player, sw, pos);
        } else {
            player.displayClientMessage(Component.literal("A nightmare closes in around you..."), false);
            spawnNightmare(player, sw, pos);
        }

        sync(player, data);
    }

    // ── Exit ─────────────────────────────────────────────────────────────────

    public static void exit(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isInSpiritWorld()) return;

        // Remove the sleeping corpse from the world
        removeCorpse(player, data);

        // Collect transferable items before restoring overworld inventory
        List<ItemStack> transferable = collectTransferable(player);

        restoreInventory(player, data);

        // Restore overworld vitals
        player.setHealth(Math.min(data.getSavedHealth(), player.getMaxHealth()));
        player.getFoodData().setFoodLevel(data.getSavedFoodLevel());
        player.getFoodData().setSaturation(data.getSavedSaturation());

        // Add transferable Spirit items into restored Overworld inventory
        Inventory inv = player.getInventory();
        for (ItemStack ts : transferable) {
            if (!inv.add(ts)) player.drop(ts, false);
        }

        BlockPos returnPos = data.getReturnPos().orElse(null);
        String returnDimStr = data.getReturnDimension();

        data.setInSpiritWorld(false);
        data.setSpiritWorldIsDream(false);
        data.setDemonicNightmare(false);
        data.clearManifestationState();
        data.clearReturnPos();
        sync(player, data);

        ServerLevel returnLevel = resolveReturnLevel(player, returnDimStr);
        if (returnPos != null) {
            player.teleportTo(returnLevel, returnPos.getX() + 0.5, (double)(returnPos.getY()), returnPos.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        } else {
            BlockPos spawn = new net.minecraft.core.BlockPos(0, 64, 0);
            player.teleportTo(returnLevel, spawn.getX() + 0.5, (double)(spawn.getY()), spawn.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        }

        player.displayClientMessage(Component.literal("You wake up."), false);
    }

    public static boolean canManifest(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        return data.isInSpiritWorld()
                && !data.isManifestedGhost()
                && player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                && data.getManifestationTime() >= 5;
    }

    public static void manifestPlayerInOverworldAsGhost(ServerPlayer player) {
        if (!canManifest(player)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        ServerLevel overworld = resolveReturnLevel(player, Level.OVERWORLD.identifier().toString());
        if (overworld == null) {
            return;
        }

        data.setManifestedGhost(true);
        data.setSkipManifestationTick(true);
        data.setManifestationReturnPos(player.blockPosition());

        BlockPos target = findSafeStandingPos(overworld, player.blockPosition());
        player.teleportTo(overworld,
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                false);
        sync(player, data);
    }

    public static void returnGhostPlayerToSpiritWorld(ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isManifestedGhost()) {
            return;
        }

        ServerLevel spiritWorld = ((ServerLevel) player.level()).getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spiritWorld == null) {
            return;
        }

        BlockPos anchor = data.getManifestationReturnPos().orElse(new BlockPos(0, 65, 0));
        BlockPos target = findSafeStandingPos(spiritWorld, anchor);
        data.setManifestedGhost(false);
        data.setSkipManifestationTick(false);
        data.setManifestationReturnPos(null);
        player.teleportTo(spiritWorld,
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                false);
        sync(player, data);
    }

    // ── Nightmare chance calculation ──────────────────────────────────────────

    private record NightmareResult(boolean isNightmare, boolean isDemonic) {}

    private static NightmareResult calculateNightmare(ServerPlayer player, double baseChance) {
        double chance = Mth.clamp(baseChance, 0.0, 1.0);
        boolean nightmareCatcherFound = false;
        int flowingSpirits = 0, wispyCottons = 0, demonHearts = 0, fires = 0;

        if (chance > 0.0 && chance < 1.0) {
            BlockPos origin = player.blockPosition();
            for (BlockPos bp : BlockPos.betweenClosed(
                    origin.offset(-8, -8, -8), origin.offset(8, 8, 8))) {
                BlockState bs = player.level().getBlockState(bp);
                if (bs.is(WitcheryBlocks.DREAM_CATCHER.get())) {
                    if (player.level().getBlockEntity(bp) instanceof DreamCatcherBlockEntity catcher
                            && catcher.getWeave() == DreamWeaveType.NIGHTMARES) {
                        nightmareCatcherFound = true;
                    }
                } else if (bs.is(WitcheryBlocks.FLOWING_SPIRIT.get())) {
                    flowingSpirits++;
                } else if (bs.is(WitcheryBlocks.WISPY_COTTON.get())) {
                    wispyCottons++;
                } else if (bs.is(WitcheryBlocks.DEMON_HEART.get())) {
                    demonHearts++;
                } else if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) {
                    fires++;
                }
            }

            if (nightmareCatcherFound) {
                chance -= 0.50;
                chance -= Math.min(flowingSpirits, 3) * 0.10;
                chance -= Math.min(wispyCottons, 2)   * 0.10;
                chance += Math.min(demonHearts, 2)    * 0.35;
                chance += Math.min(fires, 3)          * 0.10;
                chance  = Mth.clamp(chance, 0.0, 1.0);
            }
        }

        boolean isNightmare = player.level().random.nextDouble() < chance;

        // Demonic Nightmare: needs nightmare + NIGHTMARES catcher + flowing spirit + demon heart
        boolean isDemonic = isNightmare && nightmareCatcherFound && flowingSpirits > 0 && demonHearts > 0
                && player.level().random.nextDouble() < demonHearts * 0.35 + fires * 0.10;

        return new NightmareResult(isNightmare, isDemonic);
    }

    // ── Inventory ────────────────────────────────────────────────────────────

    private static void saveAndStripInventory(ServerPlayer player, WitcheryPlayerData data) {
        Inventory inv = player.getInventory();
        // Serialize inventory using ItemStack.CODEC
        net.minecraft.nbt.ListTag invList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            // Round-trip items (Icy Needle, Primer) are re-added from the transferable list on exit,
            // so they must not also be restored from this snapshot or they would be duplicated.
            if (!stack.isEmpty() && !isCarriedRoundTrip(stack)) {
                net.minecraft.nbt.CompoundTag itemTag = new net.minecraft.nbt.CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                net.minecraft.nbt.Tag saved2 = ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack).result().orElse(null);
                if (saved2 != null) itemTag.put("tag", saved2);
                invList.add(itemTag);
            }
        }
        net.minecraft.nbt.CompoundTag wrapper = new net.minecraft.nbt.CompoundTag();
        wrapper.put("inv", invList);
        data.setSavedInventorySnbt(wrapper.toString());

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && !isAlwaysCarried(stack)) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void restoreInventory(ServerPlayer player, WitcheryPlayerData data) {
        String snbt = data.getSavedInventorySnbt();
        if (snbt.isEmpty()) return;
        try {
            net.minecraft.nbt.CompoundTag wTag = net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
            net.minecraft.nbt.ListTag invList = wTag.getListOrEmpty("inv");
            player.getInventory().clearContent();
            for (int i = 0; i < invList.size(); i++) {
                net.minecraft.nbt.CompoundTag itemTag = invList.getCompound(i).orElse(null);
                if (itemTag == null) continue;
                int slot = itemTag.getByte("Slot").orElse((byte)0) & 0xFF;
                if (itemTag.contains("tag")) {
                    ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, itemTag.get("tag"))
                        .result().ifPresent(s -> player.getInventory().setItem(slot, s));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[Witchery] Failed to restore Spirit World inventory for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
        data.setSavedInventorySnbt("");
    }

    /** Collect transferable items from the player's current (Spirit) inventory before restoring Overworld. */
    private static List<ItemStack> collectTransferable(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && isTransferable(s)) {
                result.add(s.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return result;
    }

    // ── Nightmare spawning ────────────────────────────────────────────────────

    /** Initial nightmare spawn when entering Spirit World with nightmare state. */
    public static void spawnNightmare(ServerPlayer player, ServerLevel sw, double[] pos) {
        spawnNightmare(player, sw, pos, 4.0D, 8.0D);
    }

    private static void spawnNightmare(ServerPlayer player, ServerLevel level, double[] pos,
                                       double minimumDistance, double maximumDistance) {
        NightmareEntity nightmare = WitcheryEntities.NIGHTMARE.get().create(
                level, EntitySpawnReason.TRIGGERED);
        if (nightmare == null) return;
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        double dist = minimumDistance
                + level.getRandom().nextDouble() * (maximumDistance - minimumDistance);
        nightmare.setPos(pos[0] + dist * Math.cos(angle), pos[1], pos[2] + dist * Math.sin(angle));
        nightmare.setVictim(player);
        nightmare.setTarget(player);
        nightmare.finalizeSpawn(level, level.getCurrentDifficultyAt(nightmare.blockPosition()),
                EntitySpawnReason.TRIGGERED, null);
        level.addFreshEntity(nightmare);
    }

    /**
     * Spawns a replacement Nightmare for a nightmare-state player if one isn't already nearby.
     * Called from the nightmare tick in WitcheryPlayerEvents (every 100 ticks).
     */
    public static void tickNightmareRespawn(ServerPlayer player, ServerLevel sw, WitcheryPlayerData data) {
        if (!data.isInSpiritWorld() || data.isSpiritWorldDream()) return;

        // 30-second kill cooldown
        long now = sw.getGameTime();
        if (data.getLastNightmareKillGameTime() != Long.MIN_VALUE
                && now - data.getLastNightmareKillGameTime() < 600) return;

        // Check if one of this player's nightmares is already nearby (18-block AABB)
        boolean hasOne = sw.getEntitiesOfClass(NightmareEntity.class,
                player.getBoundingBox().inflate(18),
                n -> n.hasAssignedVictim() && n.matchesVictim(player)
        ).size() > 0;
        if (hasOne) return;

        double[] pos = { player.getX(), player.getY(), player.getZ() };
        spawnNightmare(player, sw, pos);
    }

    /**
     * Source Curse of Waking Nightmare pressure. The entity query only runs after
     * the rare level-dependent spawn roll succeeds.
     */
    public static void tickWakingNightmare(ServerPlayer player, ServerLevel level, WitcheryPlayerData data) {
        int curseLevel = data.getWakingNightmareLevel();
        if (curseLevel <= 0
                || level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || level.random.nextInt(NightmareEntity.sourceWakingSpawnBound(curseLevel)) != 0) {
            return;
        }

        AABB bounds = new AABB(
                player.getX() - 16.0D, player.getY() - 8.0D, player.getZ() - 16.0D,
                player.getX() + 16.0D, player.getY() + 8.0D, player.getZ() + 16.0D);
        boolean hasOne = !level.getEntitiesOfClass(NightmareEntity.class, bounds,
                nightmare -> nightmare.hasAssignedVictim() && nightmare.matchesVictim(player)).isEmpty();
        if (!hasOne) {
            spawnNightmare(player, level,
                    new double[]{player.getX(), player.getY(), player.getZ()}, 2.0D, 6.0D);
        }
    }

    // ── Position ─────────────────────────────────────────────────────────────

    private static double[] prepareSpiritWorldEntry(ServerLevel sw, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        BlockPos anchor = BlockPos.containing(x, y, z);
        sw.getChunk(anchor.getX() >> 4, anchor.getZ() >> 4, ChunkStatus.FULL, true);
        clearCopiedBed(sw, anchor);
        return new double[]{ x, safeEntryY(sw, anchor, y), z };
    }

    private static double safeEntryY(ServerLevel level, BlockPos anchor, double originalY) {
        if (isValidEntryPos(level, anchor)) {
            return originalY;
        }

        int minFeetY = level.getMinY() + 1;
        int maxFeetY = level.getMaxY() - 2;
        int maxOffset = Math.max(maxFeetY - anchor.getY(), anchor.getY() - minFeetY);
        for (int offset = 1; offset <= maxOffset; offset++) {
            int yUp = anchor.getY() + offset;
            if (yUp <= maxFeetY) {
                BlockPos up = new BlockPos(anchor.getX(), yUp, anchor.getZ());
                if (isValidEntryPos(level, up)) {
                    return yUp + 0.1D;
                }
            }

            int yDown = anchor.getY() - offset;
            if (yDown >= minFeetY) {
                BlockPos down = new BlockPos(anchor.getX(), yDown, anchor.getZ());
                if (isValidEntryPos(level, down)) {
                    return yDown + 0.1D;
                }
            }
        }

        return Math.max(level.getSeaLevel() + 1, 65);
    }

    private static boolean isValidEntryPos(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY() - 1) {
            return false;
        }
        return isStandableFloor(level, feet.below())
                && isOpenForPlayer(level, feet)
                && isOpenForPlayer(level, feet.above());
    }

    private static boolean isStandableFloor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isOpenForPlayer(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static void clearCopiedBed(ServerLevel level, BlockPos anchor) {
        List<BlockPos> copiedBedParts = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-1, -1, -1), anchor.offset(1, 1, 1))) {
            if (level.getBlockState(pos).getBlock() instanceof BedBlock) {
                copiedBedParts.add(pos.immutable());
            }
        }
        for (BlockPos pos : copiedBedParts) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static BlockPos findSafeStandingPos(ServerLevel level, BlockPos anchor) {
        level.getChunk(anchor.getX() >> 4, anchor.getZ() >> 4, ChunkStatus.FULL, true);
        for (int y = Math.min(level.getMaxY() - 2, 200); y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(anchor.getX(), y, anchor.getZ());
            if (!level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above().above()).isAir()) {
                return pos.above();
            }
        }
        return new BlockPos(anchor.getX(), Math.max(level.getSeaLevel() + 1, 65), anchor.getZ());
    }

    private static ServerLevel resolveReturnLevel(ServerPlayer player, String dimStr) {
        if (!dimStr.isEmpty()) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                    Identifier.parse(dimStr));
            ServerLevel found = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(key);
            if (found != null) return found;
        }
        ServerLevel ow = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);
        return ow != null ? ow : ((net.minecraft.server.level.ServerLevel) player.level());
    }

    // ── Corpse helpers ────────────────────────────────────────────────────────

    private static void spawnCorpse(ServerPlayer player, WitcheryPlayerData data) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        com.emoniph.witchery.entity.CorpseEntity corpse =
                com.emoniph.witchery.init.WitcheryEntities.CORPSE.get()
                        .create(sl, EntitySpawnReason.TRIGGERED);
        if (corpse == null) return;
        corpse.setOwner(player);
        corpse.setHealth(Math.min(corpse.getMaxHealth(), Math.max(1.0f, player.getHealth())));
        corpse.setPos(player.getX(), player.getY(), player.getZ());
        // Facing direction from yaw
        corpse.setYRot(player.getYRot());
        corpse.setYBodyRot(player.getYRot());
        corpse.setYHeadRot(player.getYRot());
        corpse.setXRot(0f);
        sl.addFreshEntity(corpse);
        data.setCorpseUUID(corpse.getUUID());
        LOGGER.debug("[Witchery] Spawned corpse {} for {}", corpse.getUUID(), player.getName().getString());
    }

    private static void removeCorpse(ServerPlayer player, WitcheryPlayerData data) {
        data.getCorpseUUID().ifPresent(uuid -> {
            var server = ((ServerLevel) player.level()).getServer();

            // The corpse spawned at the player's entry position (== the return position). If that
            // chunk has since unloaded — the player wandered far in the Spirit World — the corpse is
            // not in any level's entity lookup, so it would survive and reappear on return. Force-load
            // its chunk first so it is back in memory and can be discarded.
            ServerLevel returnLevel = resolveReturnLevel(player, data.getReturnDimension());
            BlockPos returnPos = data.getReturnPos().orElse(null);
            if (returnLevel != null && returnPos != null) {
                returnLevel.getChunk(returnPos.getX() >> 4, returnPos.getZ() >> 4, ChunkStatus.FULL, true);
            }

            boolean removed = false;
            for (var level : server.getAllLevels()) {
                var entity = level.getEntity(uuid);
                if (entity instanceof com.emoniph.witchery.entity.CorpseEntity c) {
                    c.discard();
                    removed = true;
                    LOGGER.debug("[Witchery] Removed corpse {} for {}",
                            uuid, player.getName().getString());
                    break;
                }
            }
            if (!removed) {
                LOGGER.warn("[Witchery] Corpse {} for {} could not be found to remove",
                        uuid, player.getName().getString());
            }
        });
        data.clearCorpseUUID();
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }
}
