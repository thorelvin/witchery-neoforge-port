package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.SilverVatBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class SilverVatBlockEntity extends BlockEntity implements Container {

    private static final int SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int STACK_LIMIT = 64;
    private static final int SILVER_CHANCE_ONE_IN = 5;
    private static final int TICK_INTERVAL = 10;
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final int[] lastStackSizes = new int[Direction.values().length];
    private ItemStack stored = ItemStack.EMPTY;

    public SilverVatBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.SILVER_VAT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SilverVatBlockEntity be) {
        if (level.getGameTime() % TICK_INTERVAL != 0) return;
        be.syncVisualState(level);
        be.checkAdjacentGoldOutputs(level, pos);
    }

    public void checkAdjacentGoldOutputs(Level level, BlockPos pos) {
        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            BlockPos neighbor = pos.relative(dir);
            Direction sideFacingVat = dir.getOpposite();
            if (level.getBlockEntity(neighbor) instanceof WorldlyContainer inventory) {
                checkWorldlyInventory(level, inventory, sideFacingVat);
            }
        }
    }

    private void checkWorldlyInventory(Level level, WorldlyContainer inventory, Direction sideFacingVat) {
        ItemStack goldIngot = new ItemStack(Items.GOLD_INGOT);
        int sideIndex = sideFacingVat.ordinal();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(Items.GOLD_INGOT)) {
                continue;
            }
            if (!inventory.canTakeItemThroughFace(slot, goldIngot, sideFacingVat)
                    || inventory.canPlaceItemThroughFace(slot, goldIngot, sideFacingVat)) {
                continue;
            }

            int current = stack.getCount();
            if (current > lastStackSizes[sideIndex]
                    && hasRoomForSilverDeposit()
                    && level.random.nextInt(SILVER_CHANCE_ONE_IN) == 0) {
                addSilverDeposit(level);
            }
            lastStackSizes[sideIndex] = current;
            return;
        }

        lastStackSizes[sideIndex] = 0;
    }

    private boolean hasRoomForSilverDeposit() {
        return stored.isEmpty()
                || (stored.is(WitcheryItems.SILVER_DEPOSITS.get())
                && stored.getCount() < Math.min(stored.getMaxStackSize(), STACK_LIMIT));
    }

    private void addSilverDeposit(Level level) {
        if (stored.isEmpty()) {
            stored = new ItemStack(WitcheryItems.SILVER_DEPOSITS.get());
        } else {
            stored.grow(1);
        }
        sync(level);
    }

    public ItemStack takeStored() {
        ItemStack result = stored;
        stored = ItemStack.EMPTY;
        sync(level);
        return result;
    }

    public ItemStack getStored() {
        return stored;
    }

    public static int visibleDepositParts(int count) {
        return count <= 0 ? 0 : Math.min(8, Math.max(count / 8, 1));
    }

    private void sync(@Nullable Level level) {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = syncVisualState(level);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private BlockState syncVisualState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(SilverVatBlock.HAS_DEPOSITS)) {
            return state;
        }

        boolean hasDeposits = !stored.isEmpty();
        if (state.getValue(SilverVatBlock.HAS_DEPOSITS) != hasDeposits) {
            BlockState updated = state.setValue(SilverVatBlock.HAS_DEPOSITS, hasDeposits);
            level.setBlock(worldPosition, updated, 3);
            return updated;
        }
        return state;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return stored.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == SLOT ? stored : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT || amount <= 0 || stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stored.split(amount);
        if (stored.isEmpty()) {
            stored = ItemStack.EMPTY;
        }
        sync(level);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != SLOT || stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stored;
        stored = ItemStack.EMPTY;
        sync(level);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT) {
            return;
        }
        stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        sync(level);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        stored = ItemStack.EMPTY;
        sync(level);
    }

    @Override
    public int getMaxStackSize() {
        return STACK_LIMIT;
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
        if (!stored.isEmpty()) {
            tag.store("Item", ItemStack.CODEC, stored);
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            items.set(SLOT, stored);
            ContainerHelper.saveAllItems(tag, items);
        }
        for (int i = 0; i < lastStackSizes.length; i++) {
            tag.putInt("LastStackSize" + i, lastStackSizes[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        stored = tag.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (stored.isEmpty()) {
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
            stored = items.get(SLOT);
        }
        int legacyStoredSilver = tag.getIntOr("StoredSilver", 0);
        if (stored.isEmpty() && legacyStoredSilver > 0) {
            stored = new ItemStack(WitcheryItems.SILVER_DEPOSITS.get(), Math.min(legacyStoredSilver, STACK_LIMIT));
        }
        for (int i = 0; i < lastStackSizes.length; i++) {
            lastStackSizes[i] = tag.getIntOr("LastStackSize" + i, 0);
        }
    }
}
