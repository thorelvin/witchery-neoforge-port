package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Refilling Chest: a 27-slot container that repopulates on the original
 * life-tick cadence when left empty in Torment.
 */
public class RefillingChestBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final int SLOTS = 27;
    private static final long REFILL_TICKS = 72000L;
    public static final Identifier LOOT_TABLE =
            Identifier.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private long lifeTicks = 0L;

    public RefillingChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.REFILLING_CHEST.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RefillingChestBlockEntity be) {
        if (be.lifeTicks == 0L) {
            be.tryRefill(level, pos);
        } else if (be.lifeTicks >= Long.MAX_VALUE) {
            be.lifeTicks = 1L;
        }

        be.lifeTicks++;
        be.tryRefill(level, pos);
    }

    private void tryRefill(Level level, BlockPos pos) {
        if (!level.dimension().equals(WitcheryDimensions.TORMENT)) {
            return;
        }
        if (!isEmpty()) {
            return;
        }
        if (lifeTicks % REFILL_TICKS != 0L) {
            return;
        }
        fillFromLootTable(level, pos);
    }

    private void fillFromLootTable(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LootTable table = serverLevel.getServer().reloadableRegistries()
                .getLootTable(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE, LOOT_TABLE));
        if (table == LootTable.EMPTY) {
            return;
        }

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = new ArrayList<>(table.getRandomItems(params));
        int maxStacks = 2 + serverLevel.getRandom().nextInt(4);

        clearContent();

        List<Integer> slots = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, new Random(serverLevel.getRandom().nextLong()));

        int slotIndex = 0;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            if (slotIndex >= maxStacks || slotIndex >= SLOTS) {
                break;
            }
            items.set(slots.get(slotIndex++), stack);
        }

        setChanged();
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.replaceAll(stack -> ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.witchery.refilling_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putLong("WITCLifeTicks", lifeTicks);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        lifeTicks = tag.getLongOr("WITCLifeTicks", (long) tag.getIntOr("RefillCooldown", 0));
    }
}
