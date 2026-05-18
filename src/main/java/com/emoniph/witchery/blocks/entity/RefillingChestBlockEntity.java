package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Refilling Chest — a 27-slot container that repopulates from the Torment loot table
 * every 3600 seconds (72000 ticks) once the chest is fully emptied.
 * Matches original Witchery TileEntityRefillingChest behaviour.
 */
public class RefillingChestBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final int SLOTS       = 27;
    private static final int REFILL_TICKS = 72000; // 3600 s × 20 TPS
    public  static final Identifier LOOT_TABLE =
            Identifier.fromNamespaceAndPath("witchery", "chests/torment");

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int refillCooldown = 0;
    private boolean filled = false; // prevents double-fill on first placement

    public RefillingChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.REFILLING_CHEST.get(), pos, state);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, RefillingChestBlockEntity be) {
        if (!be.filled) {
            // Initial fill on first tick after placement / world load
            be.fillFromLootTable(level, pos);
            be.filled = true;
            return;
        }
        be.refillCooldown++;
        if (be.refillCooldown >= REFILL_TICKS && be.isEmpty()) {
            be.fillFromLootTable(level, pos);
            be.refillCooldown = 0;
        }
    }

    private void fillFromLootTable(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        LootTable table = sl.getServer().reloadableRegistries()
                .getLootTable(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE, LOOT_TABLE));
        if (table == LootTable.EMPTY) return;

        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = table.getRandomItems(params);
        clearContent();

        // Scatter items randomly across the 27 slots
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) slots.add(i);
        java.util.Collections.shuffle(slots, new java.util.Random(sl.getRandom().nextLong()));

        int idx = 0;
        for (ItemStack stack : drops) {
            if (idx >= SLOTS) break;
            items.set(slots.get(idx++), stack);
        }
        setChanged();
    }

    // ── Container ─────────────────────────────────────────────────────────────

    @Override public int  getContainerSize()       { return SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot)    { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
    @Override public void clearContent() { items.replaceAll(s -> ItemStack.EMPTY); }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.witchery.refilling_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("RefillCooldown", refillCooldown);
        tag.putBoolean("Filled", filled);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        refillCooldown = tag.getIntOr("RefillCooldown", 0);
        filled = tag.getBooleanOr("Filled", false);
    }
}
