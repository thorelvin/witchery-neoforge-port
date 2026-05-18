package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.crafting.KettleBrewingRecipe;
import com.emoniph.witchery.crafting.KettleRecipeInput;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class KettleBlockEntity extends BlockEntity {
    private static final int INGREDIENT_SLOTS = 6;
    public static final int RESULT_SLOT = 6;
    public static final int BOTTLE_SLOT = 7;

    private final ItemStack[] items = new ItemStack[8];
    private int waterLevel = 0; // 0=empty, 1-3 levels
    private boolean isRuined = false;
    private boolean isPowered = false;
    private int color = 0;
    private int tickCount = 0;

    public KettleBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.KETTLE.get(), pos, state);
        for (int i = 0; i < items.length; i++) items[i] = ItemStack.EMPTY;
    }

    // ── Network sync ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── Water ─────────────────────────────────────────────────────────────────

    public int getWaterLevel() { return waterLevel; }
    public int getColor()      { return color; }
    public boolean isFilled()  { return waterLevel > 0; }

    public boolean fillWater() {
        if (waterLevel >= 3) return false;
        waterLevel++;
        sync();
        return true;
    }

    public boolean drainWater() {
        if (waterLevel <= 0) return false;
        waterLevel--;
        sync();
        return true;
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    public ItemStack getItem(int slot) { return items[slot]; }

    public void setItem(int slot, ItemStack stack) {
        items[slot] = stack == null ? ItemStack.EMPTY : stack;
        setChanged();
    }

    public boolean addIngredient(ItemStack stack) {
        if (!isFilled() || isRuined || !getItem(RESULT_SLOT).isEmpty()) return false;
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            if (getItem(i).isEmpty()) {
                items[i] = stack.copyWithCount(1);
                sync();
                if (level != null)
                    level.playSound(null, worldPosition, SoundEvents.SPLASH_POTION_THROW,
                            SoundSource.BLOCKS, 0.4f, 0.9f + level.random.nextFloat() * 0.2f);
                return true;
            }
        }
        setRuined();
        return false;
    }

    public void setRuined() {
        isRuined = true;
        color = 0;
        items[RESULT_SLOT] = ItemStack.EMPTY;
        sync();
        if (level != null)
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.6f, 0.8f);
    }

    public void reset(boolean drainWater) {
        if (level != null && level.isClientSide()) return;
        isRuined = false;
        isPowered = false;
        color = 0;
        for (int i = 0; i < items.length - 1; i++) items[i] = ItemStack.EMPTY;
        if (drainWater) waterLevel = 0;
        sync();
    }

    public boolean isRuined() { return isRuined; }
    public boolean isReady() { return !getItem(RESULT_SLOT).isEmpty() && !isRuined; }
    public boolean isBrewing() { return isFilled() && !isRuined && getItem(RESULT_SLOT).isEmpty() && someFilled(); }

    private boolean someFilled() {
        for (int i = 0; i < INGREDIENT_SLOTS; i++) if (!getItem(i).isEmpty()) return true;
        return false;
    }

    private boolean allFilled() {
        for (int i = 0; i < INGREDIENT_SLOTS; i++) if (getItem(i).isEmpty()) return false;
        return true;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, KettleBlockEntity be) {
        be.tick(level, pos);
    }

    private void tick(net.minecraft.world.level.Level world, BlockPos pos) {
        // Ambient bubbling every 3 seconds while brewing
        if (++tickCount % 60 == 0 && isFilled() && !isRuined && someFilled() && getItem(RESULT_SLOT).isEmpty()) {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 0.3f, 0.8f + level.random.nextFloat() * 0.4f);
        }
        if (tickCount % 20 != 0) return;
        if (isRuined || !isFilled()) return;
        if (!someFilled() && getItem(RESULT_SLOT).isEmpty()) return;
        if (!getItem(RESULT_SLOT).isEmpty()) return; // waiting for extraction

        // Require heat below
        BlockState below = world.getBlockState(pos.below());
        boolean hasHeat = below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)
                || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)
                || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)
                || below.is(BlockTags.FIRE);

        if (!hasHeat) { setRuined(); return; }

        KettleRecipeInput input = KettleRecipeInput.of(items, INGREDIENT_SLOTS);
        // Use getAllRecipesFor so we can apply our own matching logic (both full and partial)
        List<net.minecraft.world.item.crafting.RecipeHolder<KettleBrewingRecipe>> allRecipes =
                new java.util.ArrayList<>(((net.minecraft.server.level.ServerLevel) world).recipeAccess().recipeMap().byType(WitcheryRecipeTypes.KETTLE_BREWING.get()));

        if (allFilled()) {
            // Attempt full match
            KettleBrewingRecipe match = allRecipes.stream()
                    .filter(h -> h.value().matches(input, world))
                    .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                    .findFirst().orElse(null);

            if (match == null) {
                setRuined();
            } else {
                color = match.getColor();
                float requiredPower = match.getPower();
                if (requiredPower == 0f) {
                    isPowered = true;
                } else {
                    // Find a nearby altar with enough power
                    AltarBlockEntity altar = findNearbyAltar(world, pos, 8);
                    isPowered = altar != null && altar.getPower() >= (int) requiredPower;
                }
                if (isPowered) {
                    // Consume altar power if required
                    if (requiredPower > 0f) {
                        AltarBlockEntity altar = findNearbyAltar(world, pos, 8);
                        if (altar != null) altar.consumePower((int) requiredPower);
                    }
                    items[RESULT_SLOT] = match.createStoredResult();
                    for (int i = 0; i < INGREDIENT_SLOTS; i++) items[i] = ItemStack.EMPTY;
                    sync();
                    world.playSound(null, pos, SoundEvents.BREWING_STAND_BREW,
                            SoundSource.BLOCKS, 1f, 1f);
                    world.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.BLOCKS, 0.5f, 0.9f + world.random.nextFloat() * 0.2f);
                }
            }
        } else if (someFilled()) {
            // Partial match — just update colour preview, no ruining yet
            KettleBrewingRecipe partial = allRecipes.stream()
                    .filter(h -> h.value().matchesPartial(input, world))
                    .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                    .findFirst().orElse(null);

            if (partial == null) {
                // Only ruin if there's clearly a wrong ingredient (nothing partially matches)
                setRuined();
            } else {
                int newColor = partial.getColor();
                if (newColor != color) { color = newColor; sync(); }
            }
        }
    }

    // ── Altar power ───────────────────────────────────────────────────────────

    @Nullable
    private static AltarBlockEntity findNearbyAltar(Level world, BlockPos kettlePos, int radius) {
        for (BlockPos bp : BlockPos.betweenClosed(
                kettlePos.offset(-radius, -radius, -radius),
                kettlePos.offset( radius,  radius,  radius))) {
            if (world.getBlockEntity(bp) instanceof AltarBlockEntity altar) return altar;
        }
        return null;
    }

    // ── Brew extraction ───────────────────────────────────────────────────────

    public ItemStack extractBrew(@Nullable net.minecraft.world.entity.player.Player player) {
        if (!isReady()) return ItemStack.EMPTY;
        KettleBrewingRecipe recipe = findRecipeForResult();
        if (recipe == null || !recipe.isBrewableBy(player)) return ItemStack.EMPTY;

        ItemStack result = getItem(RESULT_SLOT);
        ItemStack brew = recipe.createExtractedResult(player);
        result.shrink(1);
        items[RESULT_SLOT] = result.isEmpty() ? ItemStack.EMPTY : result;
        sync();
        return brew;
    }

    @Nullable
    private KettleBrewingRecipe findRecipeForResult() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;
        ItemStack result = getItem(RESULT_SLOT);
        if (result.isEmpty()) return null;
        for (net.minecraft.world.item.crafting.RecipeHolder<KettleBrewingRecipe> holder :
                new java.util.ArrayList<>(serverLevel.recipeAccess().recipeMap().byType(WitcheryRecipeTypes.KETTLE_BREWING.get()))) {
            if (holder.value().matchesResult(result)) {
                return holder.value();
            }
        }
        return null;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("WaterLevel", waterLevel);
        tag.putBoolean("IsRuined", isRuined);
        tag.putBoolean("IsPowered", isPowered);
        tag.putInt("Color", color);
        var itemListOut = tag.childrenList("Items");
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                net.minecraft.world.level.storage.ValueOutput child = itemListOut.addChild();
                child.putByte("Slot", (byte) i);
                child.store("Item", net.minecraft.world.item.ItemStack.CODEC, items[i]);
            }
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        waterLevel = tag.getIntOr("WaterLevel", 0);
        isRuined = tag.getBooleanOr("IsRuined", false);
        isPowered = tag.getBooleanOr("IsPowered", false);
        color = tag.getIntOr("Color", 0);
        for (int i = 0; i < items.length; i++) items[i] = ItemStack.EMPTY;
        for (net.minecraft.world.level.storage.ValueInput child : tag.childrenListOrEmpty("Items")) {
            int slot = child.getByteOr("Slot", (byte)0) & 0xFF;
            if (slot < items.length)
                items[slot] = child.read("Item", net.minecraft.world.item.ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }
    }

    public int bottleCount() {
        ItemStack bottles = getItem(BOTTLE_SLOT);
        return bottles.isEmpty() ? 0 : bottles.getCount();
    }
}
