package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class BrewFluidBlockEntity extends BlockEntity {
    private static final String KEY_BREW = "Effect";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_DURATION = "Duration";
    private static final String KEY_EXPANSION = "Expansion";
    private static final String KEY_UPDATE_COUNT = "UpdateCount";
    private static final String KEY_RUN_TICKS = "RunTicks";
    private static final String KEY_MAX_RUN_TICKS = "MaxRunTicks";
    private static final String KEY_PERSISTENT_SOURCE = "PersistentSource";
    private static final String KEY_RITUALISED = "Ritualised";
    private static final String KEY_RITUAL_COVEN_SIZE = "RitualCovenSize";
    private static final String KEY_THROWER = "Thrower";
    private static final String KEY_THROWER_UUID = "ThrowerUUID";

    private BrewContents contents = BrewContents.EMPTY;
    private int color;
    private int duration;
    private int expansion = Brews.sourceBrewFluidExpansion(0);
    private int updateCount;
    private int runTicks;
    private int maxRunTicks = Brews.sourceBrewGasMaxRunTicks(0);
    private boolean persistentSource;
    private boolean ritualised;
    private int ritualCovenSize;
    private String throwerName = "";
    private UUID throwerUuid;

    public BrewFluidBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BREW_FLUID.get(), pos, state);
    }

    public void initialise(List<net.minecraft.world.item.Item> items, Entity source) {
        initialise(items, source, false);
    }

    public void initialise(List<net.minecraft.world.item.Item> items, Entity source, boolean persistentSource) {
        initialise(items, source, persistentSource, false, 0);
    }

    public void initialise(List<net.minecraft.world.item.Item> items, Entity source, boolean persistentSource,
                           boolean ritualised, int ritualCovenSize) {
        this.contents = new BrewContents(List.copyOf(items));
        this.color = Brews.color(items);
        int lifetime = Brews.impactLifetime(items);
        this.duration = Brews.sourceBrewFluidDecayDenominator(lifetime);
        this.maxRunTicks = Brews.sourceBrewGasMaxRunTicks(lifetime);
        this.expansion = Brews.sourceBrewFluidExpansion(Brews.impactExtent(items));
        this.updateCount = 0;
        this.runTicks = 0;
        this.persistentSource = persistentSource;
        this.ritualised = ritualised;
        this.ritualCovenSize = ritualised ? Math.max(0, ritualCovenSize) : 0;
        if (source != null) {
            this.throwerName = source.getName().getString();
            this.throwerUuid = source.getUUID();
        } else {
            this.throwerName = "";
            this.throwerUuid = null;
        }
        sync();
    }

    public void copyFrom(BrewFluidBlockEntity source) {
        this.contents = source.contents;
        this.color = source.color;
        this.duration = source.duration;
        this.expansion = source.expansion;
        this.maxRunTicks = source.maxRunTicks;
        this.throwerName = source.throwerName;
        this.throwerUuid = source.throwerUuid;
        this.persistentSource = source.persistentSource;
        this.ritualised = source.ritualised;
        this.ritualCovenSize = source.ritualCovenSize;
        this.updateCount = 0;
        this.runTicks = 0;
        sync();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public BrewContents getContents() {
        return contents;
    }

    public int getColor() {
        return color;
    }

    public int getDuration() {
        return duration;
    }

    public int getExpansion() {
        return expansion;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public int incUpdateCount() {
        updateCount++;
        setChanged();
        return updateCount;
    }

    public int incRunTicks() {
        runTicks++;
        setChanged();
        return runTicks;
    }

    public int getMaxRunTicks() {
        return maxRunTicks;
    }

    public boolean isPersistentSource() {
        return persistentSource;
    }

    public void applyContactEffect(LivingEntity target) {
        if (!(level instanceof ServerLevel serverLevel) || contents.isEmpty()) {
            return;
        }
        Brews.applyFluidContact(serverLevel, target, contents.items(), findThrower(serverLevel), worldPosition);
    }

    /** Source liquid-only adjacent-block action pulse: occasionally affects neighbouring blocks. */
    public void applyAdjacentBlockPulse(ServerLevel serverLevel) {
        if (contents.isEmpty()) {
            return;
        }
        Brews.tickAdjacentBlockPulse(serverLevel, worldPosition, contents.items(), findThrower(serverLevel),
                ritualised, ritualCovenSize);
    }

    @Nullable
    private Entity findThrower(ServerLevel serverLevel) {
        if (throwerUuid != null) {
            Entity entity = serverLevel.getEntity(throwerUuid);
            if (entity != null) {
                return entity;
            }
        }
        return throwerName.isEmpty() ? null : serverLevel.getServer().getPlayerList().getPlayerByName(throwerName);
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
        if (!contents.isEmpty()) {
            tag.store(KEY_BREW, BrewContents.CODEC, contents);
        }
        tag.putInt(KEY_COLOR, color);
        tag.putInt(KEY_DURATION, duration);
        tag.putInt(KEY_EXPANSION, expansion);
        tag.putInt(KEY_UPDATE_COUNT, updateCount);
        tag.putInt(KEY_RUN_TICKS, runTicks);
        tag.putInt(KEY_MAX_RUN_TICKS, maxRunTicks);
        tag.putBoolean(KEY_PERSISTENT_SOURCE, persistentSource);
        tag.putBoolean(KEY_RITUALISED, ritualised);
        tag.putInt(KEY_RITUAL_COVEN_SIZE, ritualCovenSize);
        if (!throwerName.isEmpty()) {
            tag.putString(KEY_THROWER, throwerName);
        }
        if (throwerUuid != null) {
            tag.putString(KEY_THROWER_UUID, throwerUuid.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        contents = tag.read(KEY_BREW, BrewContents.CODEC).orElse(BrewContents.EMPTY);
        color = tag.getIntOr(KEY_COLOR, contents.isEmpty() ? 0 : contents.color());
        duration = tag.getIntOr(KEY_DURATION, Brews.sourceBrewFluidDecayDenominator(0));
        expansion = Math.clamp(tag.getIntOr(KEY_EXPANSION, Brews.sourceBrewFluidExpansion(0)), 0, 10);
        updateCount = tag.getIntOr(KEY_UPDATE_COUNT, 0);
        runTicks = tag.getIntOr(KEY_RUN_TICKS, 0);
        maxRunTicks = Math.max(1, tag.getIntOr(KEY_MAX_RUN_TICKS, Brews.sourceBrewGasMaxRunTicks(0)));
        persistentSource = tag.getBooleanOr(KEY_PERSISTENT_SOURCE, false);
        ritualised = tag.getBooleanOr(KEY_RITUALISED, false);
        ritualCovenSize = Math.max(0, tag.getIntOr(KEY_RITUAL_COVEN_SIZE, 0));
        throwerName = tag.getStringOr(KEY_THROWER, "");
        String uuid = tag.getStringOr(KEY_THROWER_UUID, "");
        try {
            throwerUuid = uuid.isEmpty() ? null : UUID.fromString(uuid);
        } catch (IllegalArgumentException ex) {
            throwerUuid = null;
        }
    }

    private void sync() {
        setChanged();
        Level level = this.level;
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
