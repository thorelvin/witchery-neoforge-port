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

    private static final String KEY_BREW = "Brew";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_WARNING = "Warning";
    private static final int STIR_INTERVAL = 6;

    private final List<Item> ingredients = new ArrayList<>();
    private int color;
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
        if (!ingredients.isEmpty() || color != 0 || warningText != null) {
            ingredients.clear();
            color = 0;
            warningText = null;
            sync();
        }
    }

    public boolean tryAddIngredient(Item item) {
        if (ingredients.size() >= Brews.MAX_CAPACITY || !Brews.canAdd(ingredients, item)) {
            return false;
        }
        ingredients.add(item);
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

    public void removeIngredients(List<Item> items) {
        boolean changed = false;
        for (Item item : items) {
            changed |= ingredients.remove(item);
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

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!ingredients.isEmpty()) {
            tag.store(KEY_BREW, BrewContents.CODEC, new BrewContents(List.copyOf(ingredients)));
        }
        if (warningText != null && !warningText.isEmpty()) {
            tag.store(KEY_WARNING, BrewWarningText.CODEC, warningText);
        }
        tag.putInt(KEY_COLOR, color);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ingredients.clear();
        tag.read(KEY_BREW, BrewContents.CODEC).ifPresent(contents -> ingredients.addAll(contents.items()));
        warningText = tag.read(KEY_WARNING, BrewWarningText.CODEC).orElse(null);
        color = tag.getIntOr(KEY_COLOR, ingredients.isEmpty() ? 0 : Brews.color(ingredients));
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
            ItemStack itemStack = entity.getItem();
            if (!Brews.canAddAutomatically(be.getIngredients(), itemStack.getItem())) {
                continue;
            }
            if (!be.tryAddIngredient(itemStack.getItem())) {
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
