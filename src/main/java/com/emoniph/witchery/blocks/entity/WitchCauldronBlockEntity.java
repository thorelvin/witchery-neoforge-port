package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.blocks.WitchCauldronBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the in-progress custom brew composed in a heated, water-filled {@link WitchCauldronBlock}.
 * Brew ingredients dropped into the bowl are stirred in one at a time (server tick); collecting with
 * a Glass Bottle bottles the composed {@link BrewContents}.
 */
public class WitchCauldronBlockEntity extends BlockEntity {

    private static final String KEY_BREW = "Brew";
    private static final int STIR_INTERVAL = 6;

    private final List<Item> ingredients = new ArrayList<>();

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
        if (!ingredients.isEmpty()) {
            ingredients.clear();
            setChanged();
        }
    }

    public boolean tryAddIngredient(Item item) {
        if (ingredients.size() >= Brews.MAX_CAPACITY || !Brews.canAdd(ingredients, item)) {
            return false;
        }
        ingredients.add(item);
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!ingredients.isEmpty()) {
            tag.store(KEY_BREW, BrewContents.CODEC, new BrewContents(List.copyOf(ingredients)));
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ingredients.clear();
        tag.read(KEY_BREW, BrewContents.CODEC).ifPresent(contents -> ingredients.addAll(contents.items()));
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
            serverLevel.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    8, 0.2, 0.05, 0.2, 0.02);
            return;
        }
    }
}
