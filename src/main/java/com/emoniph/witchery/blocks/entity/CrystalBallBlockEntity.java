package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
        if (cooldown > 0) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.cooldown"), false);
            return false;
        }

        // Find nearest altar within ALTAR_RADIUS
        Optional<AltarBlockEntity> altar = findAltar(sl);
        if (altar.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.no_power"), false);
            return false;
        }
        if (!altar.get().consumePower(POWER_COST)) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.insufficient_power"), false);
            return false;
        }

        cooldown = COOLDOWN_TICKS;
        setChanged();

        // Generate and send prediction
        String prediction = getPrediction(player, sl);
        player.displayClientMessage(Component.literal("§5§o" + prediction), false);
        return true;
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
