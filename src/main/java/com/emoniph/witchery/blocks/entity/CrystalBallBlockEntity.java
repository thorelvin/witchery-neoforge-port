package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CrystalBallBlockEntity extends BlockEntity {

    private static final int COOLDOWN_TICKS = 100;  // 5 s between uses
    private static final int POWER_COST     = 500;
    private static final int ALTAR_RADIUS   = 16;
    private static final double WAYSTONE_SCRY_MAX_DISTANCE_SQR = 22500.0D; // original 150-block same-dimension limit
    private static final PredictionReading[] PREDICTION_ROSTER = {
            new PredictionReading("witchery.prediction.zombie", 13),
            new PredictionReading("witchery.prediction.arrowhit", 13),
            new PredictionReading("witchery.prediction.ent", 3),
            new PredictionReading("witchery.prediction.fall", 13),
            new PredictionReading("witchery.prediction.iron", 8),
            new PredictionReading("witchery.prediction.diamond", 3),
            new PredictionReading("witchery.prediction.emerald", 3),
            new PredictionReading("witchery.prediction.treasure", 2),
            new PredictionReading("witchery.prediction.love", 2),
            new PredictionReading("witchery.prediction.bababad", 2),
            new PredictionReading("witchery.prediction.babagood", 2),
            new PredictionReading("witchery.prediction.friend", 3),
            new PredictionReading("witchery.prediction.rescued", 13),
            new PredictionReading("witchery.prediction.rescued", 13),
            new PredictionReading("witchery.prediction.wet", 13),
            new PredictionReading("witchery.prediction.tothenether", 3),
            new PredictionReading("witchery.prediction.coal", 13)
    };
    private static final int PREDICTION_TOTAL_WEIGHT = totalPredictionWeight();

    private long lastUsedTime = 0L;

    /** Animated glow intensity, updated client-side from age. */
    public float glowPhase = 0f;

    public CrystalBallBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.CRYSTAL_BALL.get(), pos, state);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalBallBlockEntity be) {
        // Cooldown is source-style absolute game time, so no server tick countdown is needed.
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrystalBallBlockEntity be) {
        be.glowPhase = (be.glowPhase + 0.05f) % (2f * (float) Math.PI);
    }

    // ── Activation ────────────────────────────────────────────────────────────

    /**
     * Called when a player right-clicks the crystal ball.
     * Scans for a nearby altar, consumes 500 power, then sends a prediction message.
     */
    public boolean tryActivate(Player player, Level level) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!tryCheckReady(player, sl) || !tryConsumePower(player, sl)) {
            return false;
        }

        markUsed(sl);

        Player subject = findPredictionSubject(player, sl);
        if (subject != player) {
            player.displayClientMessage(Component.literal("The crystal ball turns its vision toward "
                    + subject.getName().getString() + ".").withStyle(ChatFormatting.DARK_PURPLE), false);
        }

        player.displayClientMessage(getPrediction(subject, sl), false);
        sl.sendParticles(ParticleTypes.EXPLOSION,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.2,
                worldPosition.getZ() + 0.5,
                16, 0.2, 0.2, 0.2, 0.0);
        sl.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.75f, 1.0f);
        return true;
    }

    /**
     * First practical restoration of the old bound-waystone crystal-ball hook:
     * same-dimension waystones within 150 blocks can be scryed through the ball.
     */
    public boolean tryScryWaystone(Player player, Level level, ItemStack waystone) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!tryConsumePower(player, sl)) {
            return false;
        }

        Optional<BlockPos> targetPos = WaystoneItem.getBoundPos(waystone);
        Optional<net.minecraft.resources.ResourceKey<Level>> targetDim = WaystoneItem.getBoundDim(waystone);
        if (targetPos.isEmpty() || targetDim.isEmpty()) {
            fail(player, sl, Component.translatable("witchery.ritual.transposition.no_waystone"));
            return false;
        }

        BlockPos pos = targetPos.get();
        if (!targetDim.get().equals(level.dimension())
                || player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > WAYSTONE_SCRY_MAX_DISTANCE_SQR) {
            fail(player, sl, Component.literal("The crystal ball cannot focus on that waystone from here.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }

        player.displayClientMessage(Component.literal("The crystal ball fixes on the waystone at "
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sl.sendParticles(ParticleTypes.PORTAL,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5,
                32, 0.25, 0.35, 0.25, 0.2);
        sl.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                16, 0.4, 0.5, 0.4, 0.05);
        sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.2f);
        return true;
    }

    /**
     * Restores the old filled Taglock Kit + Crystal Ball route in practical form.
     * The item-use path consumes altar power and starts the taglock's held scry cycle
     * without applying the normal prediction recharge gate, matching the source hook.
     */
    public boolean tryScryTaglock(Player player, Level level, ItemStack taglock) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!TaglockKitItem.isFilled(taglock)) {
            fail(player, sl, Component.translatable("witchery.crystal_ball.taglock_unbound"));
            return false;
        }
        if (!tryConsumePower(player, sl)) {
            return false;
        }

        Optional<UUID> uuid = TaglockKitItem.getUUID(taglock);
        if (uuid.isEmpty()) {
            fail(player, sl, Component.translatable("witchery.crystal_ball.taglock_missing",
                    TaglockKitItem.getBoundName(taglock)));
            return false;
        }

        Optional<LivingEntity> target = PoppetItem.findLivingEntity(sl.getServer(), uuid.get());
        if (target.isEmpty() || !target.get().isAlive()) {
            fail(player, sl, Component.translatable("witchery.crystal_ball.taglock_missing",
                    TaglockKitItem.getBoundName(taglock)));
            return false;
        }
        LivingEntity living = target.get();
        if (!living.level().dimension().equals(level.dimension())) {
            fail(player, sl, Component.translatable("witchery.crystal_ball.taglock_other_dimension",
                    living.getDisplayName()));
            return false;
        }

        player.displayClientMessage(Component.translatable("witchery.crystal_ball.taglock_scry",
                living.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sl.sendParticles(ParticleTypes.PORTAL,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5,
                32, 0.25, 0.35, 0.25, 0.2);
        sl.sendParticles(ParticleTypes.WITCH,
                living.getX(),
                living.getY() + living.getBbHeight() * 0.5,
                living.getZ(),
                16, 0.4, 0.5, 0.4, 0.05);
        sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 1.1f);
        return true;
    }

    private boolean tryCheckReady(Player player, ServerLevel level) {
        if (getCooldown(level) > 0) {
            fail(player, level, Component.translatable("witchery.crystal_ball.cooldown"));
            return false;
        }
        return true;
    }

    private boolean tryConsumePower(Player player, ServerLevel level) {
        Optional<AltarBlockEntity> altar = findAltar(level);
        if (altar.isEmpty()) {
            fail(player, level, Component.translatable("witchery.crystal_ball.no_power"));
            return false;
        }
        if (!altar.get().consumePower(POWER_COST)) {
            fail(player, level, Component.translatable("witchery.crystal_ball.insufficient_power"));
            return false;
        }
        return true;
    }

    private static void fail(Player player, ServerLevel level, Component message) {
        player.displayClientMessage(message, false);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private Optional<AltarBlockEntity> findAltar(ServerLevel level) {
        BlockPos center = worldPosition;
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-ALTAR_RADIUS, -ALTAR_RADIUS, -ALTAR_RADIUS),
                center.offset( ALTAR_RADIUS,  ALTAR_RADIUS,  ALTAR_RADIUS))) {
            if (level.getBlockEntity(bp) instanceof AltarBlockEntity altar && altar.getPower() >= POWER_COST) {
                return Optional.of(altar);
            }
        }
        return Optional.empty();
    }

    private void markUsed(ServerLevel level) {
        lastUsedTime = level.getGameTime();
        setChanged();
    }

    private Player findPredictionSubject(Player caster, ServerLevel level) {
        AABB bounds = new AABB(
                worldPosition.getX() - 4.5, worldPosition.getY() - 1.5, worldPosition.getZ() - 4.5,
                worldPosition.getX() + 5.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 5.5);
        Player subject = caster;
        double closest = Double.MAX_VALUE;
        for (Player nearbyPlayer : level.getEntitiesOfClass(Player.class, bounds, nearby -> nearby != caster)) {
            double distance = nearbyPlayer.distanceToSqr(
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            if (distance < closest) {
                subject = nearbyPlayer;
                closest = distance;
            }
        }
        return subject;
    }

    private Component getPrediction(Player player, ServerLevel level) {
        PredictionReading reading = choosePrediction(level.random);
        return Component.translatable(reading.translationKey(), player.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
    }

    private static PredictionReading choosePrediction(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(PREDICTION_TOTAL_WEIGHT);
        for (PredictionReading reading : PREDICTION_ROSTER) {
            roll -= reading.weight();
            if (roll < 0) {
                return reading;
            }
        }
        return PREDICTION_ROSTER[PREDICTION_ROSTER.length - 1];
    }

    // ── Test hooks (package-private) ────────────────────────────────────────
    static int predictionRosterSize() {
        return PREDICTION_ROSTER.length;
    }

    static String chooseTestPredictionKey(net.minecraft.util.RandomSource random) {
        return choosePrediction(random).translationKey();
    }

    private static int totalPredictionWeight() {
        int total = 0;
        for (PredictionReading reading : PREDICTION_ROSTER) {
            total += reading.weight();
        }
        return total;
    }

    private record PredictionReading(String translationKey, int weight) {}

    public boolean isReady() { return level == null || getCooldown(level) == 0; }
    public int getCooldown()  { return level == null ? 0 : getCooldown(level); }

    private int getCooldown(Level level) {
        long elapsed = level.getGameTime() - lastUsedTime;
        return (int) Math.max(0L, COOLDOWN_TICKS + 1L - elapsed);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putLong("LastUsedTime", lastUsedTime);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        lastUsedTime = tag.getLongOr("LastUsedTime", 0L);
    }
}
