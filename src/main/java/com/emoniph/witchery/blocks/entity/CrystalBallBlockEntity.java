package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CrystalBallBlockEntity extends BlockEntity {

    private static final int COOLDOWN_TICKS = 100;  // 5 s between uses
    private static final int POWER_COST     = 500;
    private static final int ALTAR_RADIUS   = 16;
    private static final double WAYSTONE_SCRY_MAX_DISTANCE_SQR = 22500.0D; // original 150-block same-dimension limit

    private int cooldown = 0;

    /** Animated glow intensity, updated client-side from age. */
    public float glowPhase = 0f;

    public CrystalBallBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.CRYSTAL_BALL.get(), pos, state);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalBallBlockEntity be) {
        if (be.cooldown > 0) be.cooldown--;
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
        if (!tryConsumePower(player, sl)) {
            return false;
        }

        cooldown = COOLDOWN_TICKS;
        setChanged();

        Player subject = findPredictionSubject(player, sl);
        if (subject != player) {
            player.displayClientMessage(Component.literal("The crystal ball turns its vision toward "
                    + subject.getName().getString() + ".").withStyle(ChatFormatting.DARK_PURPLE), false);
        }

        // Generate and send prediction
        String prediction = getPrediction(subject, sl);
        player.displayClientMessage(Component.literal("§5§o" + prediction), false);
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

        cooldown = COOLDOWN_TICKS;
        setChanged();
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

    private boolean tryConsumePower(Player player, ServerLevel level) {
        if (cooldown > 0) {
            fail(player, level, Component.translatable("witchery.crystal_ball.cooldown"));
            return false;
        }

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

    private String getPrediction(Player player, ServerLevel level) {
        long time = level.getDayTime() % 24000;
        boolean isDay  = time < 12300;
        boolean isRain = level.isRaining();

        // Check for nearby threats
        long threats = level.getEntitiesOfClass(Monster.class,
                new AABB(player.blockPosition()).inflate(20)).size();

        // Deterministic but varied — cycle through categories based on use count
        int seed = (int)(level.getGameTime() / COOLDOWN_TICKS) % 12;
        return switch (seed) {
            case 0  -> isDay  ? "The sun grows weary. Darkness gathers strength."
                              : "Before dawn's light the old spirits stir.";
            case 1  -> isRain ? "The rain carries whispers from the beyond."
                              : "Dry skies hide no secrets from those who seek.";
            case 2  -> threats > 3 ? "Danger walks the shadows near you. Be wary."
                                   : "The immediate dark holds no great menace.";
            case 3  -> "The veil between worlds grows thin where you stand.";
            case 4  -> "Power flows through the earth beneath your feet.";
            case 5  -> "An old one watches. You have been noticed.";
            case 6  -> "Not all who wander in darkness are lost — some are hunting.";
            case 7  -> "The moon remembers what the sun forgets.";
            case 8  -> "A convergence of energies approaches. Prepare your circle.";
            case 9  -> "The spirits speak of a soul in torment, seeking release.";
            case 10 -> "Fortune favours the witch who works by moonlight.";
            default -> "The crystal ball swirls and reveals... nothing of consequence.";
        };
    }

    public boolean isReady() { return cooldown == 0; }
    public int getCooldown()  { return cooldown; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        cooldown = tag.getIntOr("Cooldown", 0);
    }
}
