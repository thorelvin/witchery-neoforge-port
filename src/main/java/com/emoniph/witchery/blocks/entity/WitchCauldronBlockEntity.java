package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.BrewWarningMessages;
import com.emoniph.witchery.brewing.BrewWarningText;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.blocks.WitchCauldronBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the in-progress custom brew composed in a heated, water-filled {@link WitchCauldronBlock}.
 * Brew ingredients dropped into the bowl are stirred in one at a time (server tick); collecting with
 * a Glass Bottle bottles the composed {@link BrewContents}.
 */
public class WitchCauldronBlockEntity extends BlockEntity {

    public static final String STATION_OFFERING_TAG = "WitcheryCauldronStationOffering";

    private static final String KEY_BREW = "Brew";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_WARNING = "Warning";
    private static final String KEY_RITUAL_TARGET = "RitualTarget";
    private static final String KEY_FLUID_UNITS = "FluidUnits";
    private static final int STIR_INTERVAL = 6;

    private final List<Item> ingredients = new ArrayList<>();
    private int color;
    private ItemStack ritualTarget = ItemStack.EMPTY;
    private int fluidUnits = -1;
    @Nullable
    private BrewWarningText warningText;

    public WitchCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.WITCH_CAULDRON.get(), pos, state);
    }

    public List<Item> getIngredients() {
        return ingredients;
    }

    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    public int size() {
        return ingredients.size();
    }

    public void clear() {
        if (!ingredients.isEmpty() || color != 0 || warningText != null || !ritualTarget.isEmpty()) {
            ingredients.clear();
            color = 0;
            warningText = null;
            ritualTarget = ItemStack.EMPTY;
            sync();
        }
    }

    public boolean tryAddIngredient(Item item) {
        return tryAddIngredient(new ItemStack(item));
    }

    public boolean tryAddIngredient(ItemStack stack) {
        if (ingredients.size() >= Brews.MAX_CAPACITY
                || Brews.triggersRitual(ingredients)
                || !Brews.canAdd(ingredients, stack)) {
            return false;
        }
        Item item = stack.getItem();
        ingredients.add(item);
        if (Brews.isRitualEntityTarget(stack)) {
            ritualTarget = stack.copyWithCount(1);
        }
        if (BrewWarningMessages.isBindingItem(item) && level instanceof ServerLevel serverLevel) {
            warningText = BrewWarningMessages.findNearby(serverLevel, worldPosition, null).orElse(null);
        }
        color = Brews.color(ingredients);
        sync();
        return true;
    }

    @Nullable
    public BrewWarningText getWarningText() {
        return warningText;
    }

    public void setWarningText(@Nullable BrewWarningText warningText) {
        if (warningText != null && warningText.isEmpty()) {
            warningText = null;
        }
        this.warningText = warningText;
        sync();
    }

    public ItemStack getRitualTarget() {
        return ritualTarget.copy();
    }

    public void removeIngredients(List<Item> items) {
        boolean changed = false;
        for (Item item : items) {
            changed |= ingredients.remove(item);
            if (item == ritualTarget.getItem()) {
                ritualTarget = ItemStack.EMPTY;
            }
            if (BrewWarningMessages.isBindingItem(item)) {
                warningText = null;
            }
        }
        if (changed) {
            color = ingredients.isEmpty() ? 0 : Brews.color(ingredients);
            sync();
        }
    }

    public int getColor() {
        return color;
    }

    public int getFluidUnits(int visibleFillLevel) {
        return fluidUnits >= 0 ? fluidUnits : Math.clamp(visibleFillLevel, 0, WitchCauldronBlock.MAX_FILL_LEVEL) * 1000;
    }

    public void setFluidUnits(int amount) {
        fluidUnits = Math.clamp(amount, 0, com.emoniph.witchery.brewing.BrewingYield.FULL_CAULDRON_UNITS);
        sync();
    }

    public int drainFluidUnits(int amount, int visibleFillLevel) {
        fluidUnits = Math.max(0, getFluidUnits(visibleFillLevel) - Math.max(0, amount));
        sync();
        return fluidUnits;
    }

    public int adjustFluidUnits(int change, int visibleFillLevel) {
        fluidUnits = Math.clamp(getFluidUnits(visibleFillLevel) + change, 0,
                com.emoniph.witchery.brewing.BrewingYield.FULL_CAULDRON_UNITS);
        sync();
        return fluidUnits;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!ingredients.isEmpty()) {
            tag.store(KEY_BREW, BrewContents.CODEC, new BrewContents(List.copyOf(ingredients)));
        }
        if (warningText != null && !warningText.isEmpty()) {
            tag.store(KEY_WARNING, BrewWarningText.CODEC, warningText);
        }
        if (!ritualTarget.isEmpty()) {
            tag.store(KEY_RITUAL_TARGET, ItemStack.CODEC, ritualTarget);
        }
        tag.putInt(KEY_COLOR, color);
        if (fluidUnits >= 0) {
            tag.putInt(KEY_FLUID_UNITS, fluidUnits);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ingredients.clear();
        tag.read(KEY_BREW, BrewContents.CODEC).ifPresent(contents -> ingredients.addAll(contents.items()));
        warningText = tag.read(KEY_WARNING, BrewWarningText.CODEC).orElse(null);
        ritualTarget = tag.read(KEY_RITUAL_TARGET, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        color = tag.getIntOr(KEY_COLOR, ingredients.isEmpty() ? 0 : Brews.color(ingredients));
        fluidUnits = tag.getIntOr(KEY_FLUID_UNITS, -1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WitchCauldronBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (state.getValue(WitchCauldronBlock.LEVEL) <= 0
                || !WitchCauldronBlock.isHeated(level.getBlockState(pos.below()))
                || be.size() >= Brews.MAX_CAPACITY
                || level.getGameTime() % STIR_INTERVAL != 0) {
            return;
        }

        AABB bowl = new AABB(
                pos.getX() + 0.125, pos.getY() + 0.2, pos.getZ() + 0.125,
                pos.getX() + 0.875, pos.getY() + 1.0, pos.getZ() + 0.875);
        for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, bowl)) {
            if (!entity.isAlive()) {
                continue;
            }
            if (entity.getPersistentData().getBooleanOr(STATION_OFFERING_TAG, false)) {
                continue;
            }
            ItemStack itemStack = entity.getItem();
            if (!Brews.canAddAutomatically(be.getIngredients(), itemStack)) {
                continue;
            }
            if (!be.tryAddIngredient(itemStack)) {
                continue;
            }
            itemStack.shrink(1);
            if (itemStack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(itemStack);
            }
            serverLevel.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);
            int color = be.getColor();
            if (color != 0) {
                serverLevel.sendParticles(new DustParticleOptions(color, 0.8F),
                        pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                        10, 0.18, 0.05, 0.18, 0.01);
            }
            serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    8, 0.2, 0.05, 0.2, 0.02);
            return;
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

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
