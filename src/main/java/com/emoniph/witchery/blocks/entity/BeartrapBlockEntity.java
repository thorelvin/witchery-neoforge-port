package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.BeartrapBlock;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BeartrapBlockEntity extends BlockEntity {
    private static final String KEY_OWNER_UUID = "OwnerUUID";
    private static final String KEY_OWNER_NAME = "OwnerName";
    private static final String KEY_SOURCE_OWNER = "Owner";
    private static final String KEY_SOURCE_OWNER_ID = "Id";
    private static final String KEY_SOURCE_OWNER_NAME = "Name";
    private static final String KEY_SOURCE_SPRUNG = "Sprung";
    private static final String KEY_WOLFTRAP_START = "WolftrapStart";
    private static final String KEY_WOLF_UUID = "Wolf";
    private static final String KEY_SOURCE_WOLF_LEAST = "WolfLeast";
    private static final String KEY_SOURCE_WOLF_MOST = "WolfMost";
    private static final int WOLFTRAP_CHECK_INTERVAL = 200;
    private static final double BAIT_RANGE = 8.0D;
    private static final double BAIT_RANGE_SQUARED = BAIT_RANGE * BAIT_RANGE;

    private String ownerUuid = "";
    private String ownerName = "";
    private boolean sourceClosed;
    private long wolftrapStartTime;
    @Nullable
    private UUID spawnedWolfUuid;

    public BeartrapBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BEARTRAP.get(), pos, state);
        sourceClosed = state.hasProperty(BeartrapBlock.CLOSED) && state.getValue(BeartrapBlock.CLOSED);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        applySourceClosedToBlockState();
    }

    public void setOwner(Player player) {
        ownerUuid = player.getStringUUID();
        ownerName = player.getScoreboardName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getStringUUID().equals(ownerUuid);
    }

    public boolean hasOwner() {
        return !ownerUuid.isEmpty();
    }

    @Nullable
    public UUID ownerUuid() {
        if (ownerUuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(ownerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String ownerName() {
        return ownerName;
    }

    public void setSourceClosed(boolean closed) {
        if (sourceClosed != closed) {
            sourceClosed = closed;
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BeartrapBlockEntity trap) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof BeartrapBlock beartrapBlock)
                || !beartrapBlock.isSilvered()) {
            return;
        }
        if (state.getValue(BeartrapBlock.CLOSED)) {
            return;
        }

        if (trap.spawnedWolfUuid != null) {
            trap.tickLuredWolfman(serverLevel, pos);
            return;
        }

        if (serverLevel.getGameTime() % WOLFTRAP_CHECK_INTERVAL != 0L) {
            return;
        }

        Animal bait = trap.findWolftrapBait(serverLevel, pos);
        if (bait != null && isFullMoonNight(serverLevel)) {
            long time = serverLevel.getGameTime();
            if (trap.wolftrapStartTime > 0L && time > trap.wolftrapStartTime) {
                trap.trySummonBaitWerewolf(serverLevel, pos);
            } else {
                trap.setWolftrapStartTime(time);
                trap.playWolftrapBaitEffects(serverLevel, pos);
            }
        } else {
            trap.setWolftrapStartTime(0L);
        }
    }

    @Nullable
    private Animal findWolftrapBait(ServerLevel level, BlockPos pos) {
        if (!hasAdjacentWolfAltar(level, pos)) {
            return null;
        }
        AABB area = new AABB(pos).inflate(BAIT_RANGE);
        for (Animal animal : level.getEntitiesOfClass(Animal.class, area, animal -> animal.getType() == EntityType.SHEEP
                && animal.isLeashed()
                && animal.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= BAIT_RANGE_SQUARED)) {
            return animal;
        }
        return null;
    }

    private static boolean hasAdjacentWolfAltar(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(WitcheryBlocks.WOLF_ALTAR.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFullMoonNight(ServerLevel level) {
        long dayTime = level.getDayTime();
        long timeOfDay = dayTime % 24000L;
        return (dayTime / 24000L) % 8L == 0L && timeOfDay > 13000L && timeOfDay < 23000L;
    }

    /**
     * Source {@code TileEntityBeartrap}: a baited Wolftrap under a full moon lures a fresh Wolfman to
     * the area (16-32 blocks away). When that exact Wolfman later steps on this trap it is made
     * infectious (see {@link #tryCaptureWolf}), spreading the curse to the next villager it bites.
     */
    private void trySummonBaitWerewolf(ServerLevel level, BlockPos pos) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 16.0D + level.random.nextInt(17);
            int x = pos.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = pos.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            WolfmanEntity wolf = WitcheryEntities.WOLFMAN.get().create(level, EntitySpawnReason.EVENT);
            if (wolf == null) {
                return;
            }
            wolf.snapTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(wolf)) {
                wolf.discard();
                continue;
            }
            wolf.setPersistenceRequired();
            wolf.finalizeSpawn(level, level.getCurrentDifficultyAt(wolf.blockPosition()),
                    EntitySpawnReason.EVENT, null);
            level.addFreshEntity(wolf);
            Animal bait = findWolftrapBait(level, pos);
            if (bait != null) {
                wolf.setTarget(bait);
                wolf.getNavigation().moveTo(bait, 1.05D);
            }
            level.playSound(null, pos, SoundEvents.WOLF_SHAKE, SoundSource.HOSTILE, 1.0F, 0.65F);
            level.sendParticles(ParticleTypes.SMOKE, x + 0.5D, y + 0.5D, z + 0.5D,
                    24, 0.5D, 0.5D, 0.5D, 0.02D);
            level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                    16, 0.45D, 0.2D, 0.45D, 0.02D);
            spawnedWolfUuid = wolf.getUUID();
            setChanged();
            return;
        }
    }

    /** True when {@code living} is this trap's own lured Wolfman; it is made infectious on capture. */
    public boolean tryCaptureWolf(LivingEntity living) {
        if (spawnedWolfUuid != null && living instanceof WolfmanEntity wolf
                && wolf.getUUID().equals(spawnedWolfUuid)) {
            wolf.setInfectious();
            spawnedWolfUuid = null;
            wolftrapStartTime = 0L;
            if (living.level() instanceof ServerLevel level) {
                level.playSound(null, living.blockPosition(), SoundEvents.WOLF_SHAKE, SoundSource.HOSTILE, 1.0F, 0.9F);
                level.sendParticles(ParticleTypes.SOUL,
                        living.getX(), living.getY() + 0.7D, living.getZ(),
                        20, 0.35D, 0.35D, 0.35D, 0.03D);
                level.sendParticles(ParticleTypes.WITCH,
                        worldPosition.getX() + 0.5D, worldPosition.getY() + 0.25D, worldPosition.getZ() + 0.5D,
                        12, 0.25D, 0.2D, 0.25D, 0.02D);
            }
            setChanged();
            return true;
        }
        return false;
    }

    private void tickLuredWolfman(ServerLevel level, BlockPos pos) {
        if (spawnedWolfUuid == null || level.getGameTime() % 20L != 0L) {
            return;
        }

        if (!(level.getEntity(spawnedWolfUuid) instanceof WolfmanEntity wolf) || !wolf.isAlive()) {
            spawnedWolfUuid = null;
            wolftrapStartTime = 0L;
            setChanged();
            return;
        }

        Animal bait = findWolftrapBait(level, pos);
        if (bait != null && bait.isAlive()) {
            wolf.setTarget(bait);
            wolf.getNavigation().moveTo(bait, 1.05D);
        } else if (wolf.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) > 6.25D) {
            wolf.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.95D);
        }

        if (level.getGameTime() % 80L == 0L) {
            level.sendParticles(ParticleTypes.SOUL,
                    pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D,
                    4, 0.3D, 0.1D, 0.3D, 0.01D);
        }
    }

    private void playWolftrapBaitEffects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.35F, 0.7F);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                8, 0.35D, 0.15D, 0.35D, 0.01D);
    }

    private void setWolftrapStartTime(long startTime) {
        if (wolftrapStartTime != startTime) {
            wolftrapStartTime = startTime;
            setChanged();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString(KEY_OWNER_UUID, ownerUuid);
        tag.putString(KEY_OWNER_NAME, ownerName);
        tag.putBoolean(KEY_SOURCE_SPRUNG, currentClosedState());
        saveSourceOwner(tag);
        tag.putLong(KEY_WOLFTRAP_START, wolftrapStartTime);
        if (spawnedWolfUuid != null) {
            tag.putString(KEY_WOLF_UUID, spawnedWolfUuid.toString());
            tag.putLong(KEY_SOURCE_WOLF_LEAST, spawnedWolfUuid.getLeastSignificantBits());
            tag.putLong(KEY_SOURCE_WOLF_MOST, spawnedWolfUuid.getMostSignificantBits());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ownerUuid = tag.getStringOr(KEY_OWNER_UUID, "");
        ownerName = tag.getStringOr(KEY_OWNER_NAME, "");
        loadSourceOwner(tag);
        sourceClosed = tag.getBooleanOr(KEY_SOURCE_SPRUNG, currentClosedState());
        wolftrapStartTime = tag.getLongOr(KEY_WOLFTRAP_START, 0L);
        spawnedWolfUuid = readSourceWolfUuid(tag);
        if (spawnedWolfUuid == null) {
            spawnedWolfUuid = readUuid(tag.getStringOr(KEY_WOLF_UUID, ""));
        }
        applySourceClosedToBlockState();
    }

    private void saveSourceOwner(ValueOutput tag) {
        if (ownerUuid.isEmpty() && ownerName.isEmpty()) {
            return;
        }
        ValueOutput owner = tag.child(KEY_SOURCE_OWNER);
        if (!ownerUuid.isEmpty()) {
            owner.putString(KEY_SOURCE_OWNER_ID, ownerUuid);
        }
        if (!ownerName.isEmpty()) {
            owner.putString(KEY_SOURCE_OWNER_NAME, ownerName);
        }
    }

    private void loadSourceOwner(ValueInput tag) {
        tag.child(KEY_SOURCE_OWNER).ifPresent(owner -> {
            String sourceUuid = owner.getStringOr(KEY_SOURCE_OWNER_ID, "");
            String sourceName = owner.getStringOr(KEY_SOURCE_OWNER_NAME, "");
            if (!sourceUuid.isBlank()) {
                ownerUuid = sourceUuid;
            }
            if (!sourceName.isBlank()) {
                ownerName = sourceName;
            }
        });
    }

    private void applySourceClosedToBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(BeartrapBlock.CLOSED) || !state.hasProperty(BeartrapBlock.ARMED)) {
            return;
        }
        if (state.getValue(BeartrapBlock.CLOSED) != sourceClosed) {
            level.setBlock(worldPosition, state
                    .setValue(BeartrapBlock.CLOSED, sourceClosed)
                    .setValue(BeartrapBlock.ARMED, !sourceClosed), 3);
        }
    }

    private boolean currentClosedState() {
        BlockState state = getBlockState();
        return state.hasProperty(BeartrapBlock.CLOSED) && state.getValue(BeartrapBlock.CLOSED);
    }

    @Nullable
    private static UUID readUuid(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    private static UUID readSourceWolfUuid(ValueInput tag) {
        if (!tag.keySet().contains(KEY_SOURCE_WOLF_LEAST) || !tag.keySet().contains(KEY_SOURCE_WOLF_MOST)) {
            return null;
        }
        return new UUID(tag.getLongOr(KEY_SOURCE_WOLF_MOST, 0L), tag.getLongOr(KEY_SOURCE_WOLF_LEAST, 0L));
    }
}
