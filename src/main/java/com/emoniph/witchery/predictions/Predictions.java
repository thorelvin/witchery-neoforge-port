package com.emoniph.witchery.predictions;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.world.PredictionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.ArrayList;
import java.util.List;

/**
 * The crystal-ball fortune system. Right-clicking the ball stores one random prophecy on the player
 * (see {@link PredictionData}); it then comes true either NATURALLY (you really do get hit by an
 * arrow, fall, get wet, or mine the ore) or, once past-due, is FORCE-FULFILLED — the ball makes it
 * happen (spawns the skeleton/zombie/ent, buries treasure, drops a vein, floods you, digs a pit).
 *
 * Source parity: {@code com.emoniph.witchery.predictions.*} + {@code PredictionManager}
 * (1.7.10-0.24.1). IDs and weights match the original registration table.
 */
public final class Predictions {

    // Prophecy IDs (match source).
    public static final int ZOMBIE = 1;
    public static final int ARROW = 2;
    public static final int ENT = 3;
    public static final int FALL = 4;
    public static final int IRON = 5;
    public static final int DIAMOND = 6;
    public static final int EMERALD = 7;
    public static final int TREASURE = 8;
    public static final int LOVE = 9;
    public static final int BABA_BAD = 10;
    public static final int BABA_GOOD = 11;
    public static final int FRIEND = 12;
    public static final int RESCUE = 13;
    public static final int WET = 15;
    public static final int NETHER = 16;
    public static final int COAL = 17;
    // Port additions (source stopped at 17; 14 was unused). Sky/venom/visitor/fire omens.
    public static final int LIGHTNING = 18;
    public static final int SERPENT = 19;
    public static final int WITCH_VISITOR = 20;
    public static final int FLAMES = 21;

    /** Debug command name → prophecy id (insertion-ordered for suggestions). See {@code /witchery crystal}. */
    public static final java.util.LinkedHashMap<String, Integer> DEBUG_NAMES = new java.util.LinkedHashMap<>();
    static {
        DEBUG_NAMES.put("zombie", ZOMBIE);
        DEBUG_NAMES.put("arrow", ARROW);
        DEBUG_NAMES.put("ent", ENT);
        DEBUG_NAMES.put("fall", FALL);
        DEBUG_NAMES.put("iron", IRON);
        DEBUG_NAMES.put("diamond", DIAMOND);
        DEBUG_NAMES.put("emerald", EMERALD);
        DEBUG_NAMES.put("treasure", TREASURE);
        DEBUG_NAMES.put("love", LOVE);
        DEBUG_NAMES.put("baba_bad", BABA_BAD);
        DEBUG_NAMES.put("baba_good", BABA_GOOD);
        DEBUG_NAMES.put("friend", FRIEND);
        DEBUG_NAMES.put("rescue", RESCUE);
        DEBUG_NAMES.put("wet", WET);
        DEBUG_NAMES.put("nether", NETHER);
        DEBUG_NAMES.put("coal", COAL);
        DEBUG_NAMES.put("lightning", LIGHTNING);
        DEBUG_NAMES.put("serpent", SERPENT);
        DEBUG_NAMES.put("witch_visitor", WITCH_VISITOR);
        DEBUG_NAMES.put("flames", FLAMES);
    }

    /** Once a prophecy is this old without coming true, the ball starts forcing it. */
    private static final long PAST_DUE_TICKS = 1200L;        // 1 minute
    /** Per-second chance to force-fulfill once past-due (tick handler runs once a second). */
    private static final double FORCE_CHANCE_PER_SECOND = 0.15;
    private static final int SPAWN_MIN = 2;
    private static final int SPAWN_MAX = 4;

    public record Reading(int id, int weight, String key) {}

    private static final Reading[] ROSTER = {
            new Reading(ZOMBIE, 13, "witchery.prediction.zombie"),
            new Reading(ARROW, 13, "witchery.prediction.arrowhit"),
            new Reading(ENT, 3, "witchery.prediction.ent"),
            new Reading(FALL, 13, "witchery.prediction.fall"),
            new Reading(IRON, 8, "witchery.prediction.iron"),
            new Reading(DIAMOND, 3, "witchery.prediction.diamond"),
            new Reading(EMERALD, 3, "witchery.prediction.emerald"),
            new Reading(TREASURE, 2, "witchery.prediction.treasure"),
            new Reading(LOVE, 2, "witchery.prediction.love"),
            new Reading(BABA_BAD, 2, "witchery.prediction.bababad"),
            new Reading(BABA_GOOD, 2, "witchery.prediction.babagood"),
            new Reading(FRIEND, 3, "witchery.prediction.friend"),
            new Reading(RESCUE, 26, "witchery.prediction.rescued"),
            new Reading(WET, 13, "witchery.prediction.wet"),
            new Reading(NETHER, 3, "witchery.prediction.tothenether"),
            new Reading(COAL, 13, "witchery.prediction.coal"),
            new Reading(LIGHTNING, 3, "witchery.prediction.lightning"),
            new Reading(SERPENT, 6, "witchery.prediction.serpent"),
            new Reading(WITCH_VISITOR, 3, "witchery.prediction.witch_visitor"),
            new Reading(FLAMES, 6, "witchery.prediction.flames"),
    };
    private static final int TOTAL_WEIGHT;

    static {
        int total = 0;
        for (Reading r : ROSTER) {
            total += r.weight();
        }
        TOTAL_WEIGHT = total;
    }

    private Predictions() {
    }

    private static Reading byId(int id) {
        for (Reading r : ROSTER) {
            if (r.id() == id) {
                return r;
            }
        }
        return null;
    }

    static Reading weightedPick(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (Reading r : ROSTER) {
            roll -= r.weight();
            if (roll < 0) {
                return r;
            }
        }
        return ROSTER[ROSTER.length - 1];
    }

    /** Test hook. */
    public static String chooseTestKey(RandomSource random) {
        return weightedPick(random).key();
    }

    public static int rosterSize() {
        return ROSTER.length;
    }

    // ── Activation (from the Crystal Ball) ──────────────────────────────────

    /** Give {@code subject} a prophecy (or re-read the one they already have). */
    public static void makePrediction(ServerLevel level, ServerPlayer subject) {
        PredictionData data = PredictionData.get(level);
        PredictionData.Active existing = data.get(subject.getUUID());
        Reading reading;
        if (existing != null && byId(existing.id()) != null) {
            reading = byId(existing.id());
        } else {
            reading = weightedPick(level.random);
            data.set(subject.getUUID(), new PredictionData.Active(reading.id(), now(level)));
        }
        subject.displayClientMessage(Component.translatable(reading.key(), subject.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
    }

    /**
     * Debug: give {@code player} the prophecy with the given id, show its reading, and force it to
     * come true immediately (the force-fulfilment path). Returns whether the event actually fired
     * (some need open ground nearby). Used by {@code /witchery crystal <prophecy>}.
     */
    public static boolean debugForce(ServerLevel level, ServerPlayer player, int id) {
        Reading reading = byId(id);
        if (reading == null) {
            return false;
        }
        PredictionData.get(level).set(player.getUUID(), new PredictionData.Active(id, now(level)));
        player.displayClientMessage(Component.translatable(reading.key(), player.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
        boolean fired = forceFulfill(level, player, id);
        if (fired) {
            fulfilled(level, player);
        }
        return fired;
    }

    private static long now(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    private static void fulfilled(ServerLevel level, ServerPlayer player) {
        PredictionData.get(level).remove(player.getUUID());
    }

    // ── Natural + forced fulfillment hooks (called from PredictionEvents) ────

    /** Server player tick (once a second): natural wet check + past-due forcing. */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PredictionData.Active active = PredictionData.get(level).get(player.getUUID());
        if (active == null) {
            return;
        }
        if (active.id() == WET && player.isInWaterOrRain()) {
            fulfilled(level, player);
            return;
        }
        if (active.id() == SERPENT && player.hasEffect(MobEffects.POISON)) {
            // The serpent's venom courses through them — by any source.
            fulfilled(level, player);
            return;
        }
        if (now(level) - active.time() >= PAST_DUE_TICKS && level.random.nextDouble() < FORCE_CHANCE_PER_SECOND) {
            if (forceFulfill(level, player, active.id())) {
                fulfilled(level, player);
            }
        }
    }

    /** Damage hook: arrow / fall / rescued prophecies come true here. */
    public static void onDamage(ServerPlayer player, DamageSource source) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        PredictionData.Active active = PredictionData.get(level).get(player.getUUID());
        if (active == null) {
            return;
        }
        if (active.id() == ARROW && source.is(DamageTypes.ARROW)) {
            fulfilled(level, player);
        } else if (active.id() == FALL && source.is(DamageTypes.FALL)) {
            fulfilled(level, player);
        } else if (active.id() == LIGHTNING && source.is(DamageTypes.LIGHTNING_BOLT)) {
            fulfilled(level, player);
        } else if (active.id() == FLAMES
                && (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA))) {
            fulfilled(level, player);
        } else if (active.id() == RESCUE && source.getEntity() instanceof LivingEntity) {
            // A stranger comes to the rescue.
            if (spawnRescuer(level, player)) {
                fulfilled(level, player);
            }
        }
    }

    /** Mining hook: ore / treasure prophecies come true when the right block is broken. */
    public static void onMine(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        PredictionData.Active active = PredictionData.get(level).get(player.getUUID());
        if (active == null) {
            return;
        }
        switch (active.id()) {
            case IRON -> {
                if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
                    dropAt(level, pos, new ItemStack(Items.RAW_IRON, 8 + level.random.nextInt(13)));
                    fulfilled(level, player);
                }
            }
            case COAL -> {
                if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) {
                    dropAt(level, pos, new ItemStack(Items.COAL, 10 + level.random.nextInt(11)));
                    fulfilled(level, player);
                }
            }
            case DIAMOND -> {
                if ((state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) && level.random.nextFloat() < 0.04F) {
                    dropAt(level, pos, new ItemStack(Items.DIAMOND));
                    fulfilled(level, player);
                }
            }
            case EMERALD -> {
                if ((state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) && level.random.nextFloat() < 0.04F) {
                    dropAt(level, pos, new ItemStack(Items.EMERALD));
                    fulfilled(level, player);
                }
            }
            case TREASURE -> {
                if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.SAND))
                        && pos.getY() > level.getMinY() + 6 && level.random.nextFloat() < 0.10F
                        && buryTreasure(level, pos)) {
                    fulfilled(level, player);
                }
            }
            default -> { }
        }
    }

    // ── Forced effects ──────────────────────────────────────────────────────

    private static boolean forceFulfill(ServerLevel level, ServerPlayer player, int id) {
        return switch (id) {
            case ZOMBIE -> spawnHostile(level, player, EntityType.ZOMBIE);
            case ARROW -> spawnHostile(level, player, EntityType.SKELETON);
            case ENT -> spawnHostile(level, player, WitcheryEntities.ENT.get());
            case BABA_BAD -> spawnHostile(level, player, WitcheryEntities.BABA_YAGA.get());
            case BABA_GOOD -> spawnFriendlyBabaYaga(level, player);
            case FRIEND -> spawnTamedWolf(level, player);
            case RESCUE -> spawnRescuer(level, player);
            case LOVE -> spawnNeutral(level, player, EntityType.VILLAGER);
            case NETHER -> spawnHostile(level, player, EntityType.ZOMBIFIED_PIGLIN);
            case FALL -> digPit(level, player);
            case WET -> floodAround(level, player);
            case LIGHTNING -> summonLightning(level, player);
            case SERPENT -> spawnHostile(level, player, EntityType.CAVE_SPIDER);
            case WITCH_VISITOR -> level.random.nextBoolean()
                    ? spawnHostile(level, player, WitcheryEntities.COVEN_WITCH.get())
                    : spawnNeutral(level, player, WitcheryEntities.COVEN_WITCH.get());
            case FLAMES -> igniteRing(level, player);
            // Ore/treasure prophecies were mining-only; once past-due, just gift the reward.
            case IRON -> giftAt(level, player, new ItemStack(Items.RAW_IRON, 8 + level.random.nextInt(13)));
            case COAL -> giftAt(level, player, new ItemStack(Items.COAL, 10 + level.random.nextInt(11)));
            case DIAMOND -> giftAt(level, player, new ItemStack(Items.DIAMOND));
            case EMERALD -> giftAt(level, player, new ItemStack(Items.EMERALD));
            case TREASURE -> giftAt(level, player, new ItemStack(Items.GOLD_INGOT, 2 + level.random.nextInt(3)));
            default -> false;
        };
    }

    private static boolean spawnHostile(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type) {
        Mob mob = spawnNearby(level, player, type);
        if (mob == null) {
            return false;
        }
        mob.setTarget(player);
        return true;
    }

    private static boolean spawnNeutral(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type) {
        return spawnNearby(level, player, type) != null;
    }

    private static boolean spawnFriendlyBabaYaga(ServerLevel level, ServerPlayer player) {
        if (spawnNearby(level, player, WitcheryEntities.BABA_YAGA.get()) instanceof BabaYagaEntity baba) {
            baba.beginFriendlyVisit(player);
            return true;
        }
        return false;
    }

    private static boolean spawnTamedWolf(ServerLevel level, ServerPlayer player) {
        if (spawnNearby(level, player, EntityType.WOLF) instanceof Wolf wolf) {
            wolf.tame(player);
            wolf.setOrderedToSit(false);
            return true;
        }
        return false;
    }

    private static boolean spawnRescuer(ServerLevel level, ServerPlayer player) {
        // A tamed wolf comes to the player's defence, targeting whatever they're fighting.
        if (spawnNearby(level, player, EntityType.WOLF) instanceof Wolf wolf) {
            wolf.tame(player);
            wolf.setOrderedToSit(false);
            if (player.getLastHurtByMob() != null) {
                wolf.setTarget(player.getLastHurtByMob());
            }
            return true;
        }
        return false;
    }

    private static Mob spawnNearby(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = Mth.nextInt(level.random, -SPAWN_MAX, SPAWN_MAX);
            int dz = Mth.nextInt(level.random, -SPAWN_MAX, SPAWN_MAX);
            if (Math.abs(dx) < SPAWN_MIN && Math.abs(dz) < SPAWN_MIN) {
                continue;
            }
            BlockPos floor = findFloor(level, origin.getX() + dx, origin.getZ() + dz, origin.getY());
            if (floor == null) {
                continue;
            }
            Mob mob = type.create(level, EntitySpawnReason.EVENT);
            if (mob == null) {
                return null;
            }
            mob.snapTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(floor), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.WITCH, floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5,
                    12, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, floor, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.7F, 1.0F);
            return mob;
        }
        return null;
    }

    /** Find a solid floor with two air blocks above, near the given column and centre Y. */
    private static BlockPos findFloor(ServerLevel level, int x, int z, int centerY) {
        for (int y = centerY + 2; y >= centerY - 4; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (level.getBlockState(below).blocksMotion()
                    && level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static boolean digPit(ServerLevel level, ServerPlayer player) {
        BlockPos base = player.blockPosition().below();
        if (!isThreeByThreeSolid(level, base)) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int depth = 0; depth < 6; depth++) {
                    BlockPos p = base.offset(dx, -depth, dz);
                    if (depth == 0) {
                        level.setBlock(p, Blocks.GRAVEL.defaultBlockState(), 3);
                    } else if (level.getBlockState(p).getDestroySpeed(level, p) >= 0) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
        return true;
    }

    private static boolean floodAround(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (!level.getBlockState(feet.below()).blocksMotion()) {
            return false;
        }
        boolean placed = false;
        for (BlockPos p : new BlockPos[]{feet, feet.north(), feet.south(), feet.east(), feet.west()}) {
            if (level.getBlockState(p).canBeReplaced()) {
                level.setBlock(p, Blocks.WATER.defaultBlockState(), 3);
                placed = true;
            }
        }
        return placed;
    }

    /** The sky's wrath: a bolt cracks down a few blocks away (near, not point-blank). */
    private static boolean summonLightning(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = Mth.nextInt(level.random, -SPAWN_MAX, SPAWN_MAX);
            int dz = Mth.nextInt(level.random, -SPAWN_MAX, SPAWN_MAX);
            if (Math.abs(dx) < SPAWN_MIN && Math.abs(dz) < SPAWN_MIN) {
                continue;
            }
            BlockPos floor = findFloor(level, origin.getX() + dx, origin.getZ() + dz, origin.getY());
            if (floor == null) {
                continue;
            }
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (bolt == null) {
                return false;
            }
            bolt.snapTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
            level.addFreshEntity(bolt);
            return true;
        }
        return false;
    }

    /** Flames at your heels: a ring of fire flares up around the player (not under them). */
    private static boolean igniteRing(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        boolean placed = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos p = feet.offset(dx, 0, dz);
                if (level.getBlockState(p).canBeReplaced() && level.getBlockState(p.below()).blocksMotion()) {
                    level.setBlock(p, Blocks.FIRE.defaultBlockState(), 3);
                    placed = true;
                }
            }
        }
        if (placed) {
            level.playSound(null, feet, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return placed;
    }

    private static boolean isThreeByThreeSolid(ServerLevel level, BlockPos base) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.getBlockState(base.offset(dx, 0, dz)).blocksMotion()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean buryTreasure(ServerLevel level, BlockPos minedPos) {
        BlockPos chestPos = minedPos.below();
        if (!level.getBlockState(chestPos.below()).blocksMotion()) {
            return false;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT, level.random.nextLong());
        }
        level.playSound(null, chestPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    private static void dropAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        Block.popResource(level, pos, stack);
    }

    private static boolean giftAt(ServerLevel level, ServerPlayer player, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.3, 0.3, 0.3, 0.05);
        return true;
    }
}
