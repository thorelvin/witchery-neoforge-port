package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.crafting.SpinningRecipe;
import com.emoniph.witchery.crafting.SpinningRecipeInput;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.emoniph.witchery.menu.SpinningWheelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpinningWheelBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int SLOT_FIBRE = 0;
    public static final int SLOT_MODIFIER_1 = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_MODIFIER_2 = 3;
    public static final int SLOT_MODIFIER_3 = 4;
    public static final int SLOT_COUNT = 5;

    private static final int[] SLOTS_TOP = {SLOT_FIBRE};
    private static final int[] SLOTS_BOTTOM = {SLOT_MODIFIER_3, SLOT_MODIFIER_1, SLOT_MODIFIER_2};
    private static final int[] SLOTS_SIDES = {SLOT_MODIFIER_2, SLOT_OUTPUT, SLOT_MODIFIER_3, SLOT_MODIFIER_1};

    private static final int DEFAULT_SPIN_TIME = 300;
    private static final int ALTAR_RADIUS = 16;
    private static final int POWER_SCALE = 1000;
    private static final int POWER_PER_TICK_SCALED = 600;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int spinProgress = 0;
    private int spinTotal = DEFAULT_SPIN_TIME;
    private int powerLevel = 0;
    private int pendingPowerScaled = 0;
    private int scanCooldown = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> spinProgress;
                case 1 -> spinTotal;
                case 2 -> powerLevel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> spinProgress = value;
                case 1 -> spinTotal = value;
                case 2 -> powerLevel = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public SpinningWheelBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.SPINNING_WHEEL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpinningWheelBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = false;
        Optional<RecipeHolder<SpinningRecipe>> recipe = getRecipe(serverLevel, be);

        if (recipe.isEmpty()) {
            if (be.scanCooldown-- <= 0) {
                be.scanCooldown = 40;
                int newPowerLevel = be.findNearbyAltar(level) != null ? 1 : 0;
                if (newPowerLevel != be.powerLevel) {
                    be.powerLevel = newPowerLevel;
                    changed = true;
                }
            }
            if (be.spinProgress != 0 || be.pendingPowerScaled != 0) {
                be.spinProgress = 0;
                be.pendingPowerScaled = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }

        be.spinTotal = recipe.get().value().cookingTime();
        AltarBlockEntity altar = be.findNearbyAltar(level);
        boolean canProgress = altar != null && canSpin(be, recipe.get().value());

        int newPowerLevel = altar != null ? 1 : 0;
        if (newPowerLevel != be.powerLevel) {
            be.powerLevel = newPowerLevel;
            changed = true;
        }

        if (!canProgress) {
            if (be.spinProgress != 0 || be.pendingPowerScaled != 0) {
                be.spinProgress = 0;
                be.pendingPowerScaled = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }

        be.pendingPowerScaled += POWER_PER_TICK_SCALED;
        int powerUnits = be.pendingPowerScaled / POWER_SCALE;
        if (powerUnits > 0) {
            if (!altar.consumePower(powerUnits)) {
                be.powerLevel = 0;
                if (changed) {
                    be.setChanged();
                }
                return;
            }
            be.pendingPowerScaled -= powerUnits * POWER_SCALE;
        }

        be.spinProgress++;
        changed = true;
        if (be.spinProgress >= be.spinTotal) {
            be.spinProgress = 0;
            spinItem(be, recipe.get().value());
        }

        if (changed) {
            be.setChanged();
        }
    }

    private static Optional<RecipeHolder<SpinningRecipe>> getRecipe(ServerLevel level, SpinningWheelBlockEntity be) {
        return level.getServer().getRecipeManager().getRecipeFor(
                WitcheryRecipeTypes.SPINNING.get(),
                new SpinningRecipeInput(be.items.get(SLOT_FIBRE), List.of(
                        be.items.get(SLOT_MODIFIER_1),
                        be.items.get(SLOT_MODIFIER_2),
                        be.items.get(SLOT_MODIFIER_3)
                )),
                level
        );
    }

    private static boolean canSpin(SpinningWheelBlockEntity be, SpinningRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = be.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), be.getMaxStackSize());
    }

    private static void spinItem(SpinningWheelBlockEntity be, SpinningRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        ItemStack output = be.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            be.items.set(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        shrinkIfPresent(be.items.get(SLOT_FIBRE), recipe.fibre().getCount());
        consumeModifiers(be.items, recipe.modifiers());
    }

    private static void shrinkIfPresent(ItemStack stack, int count) {
        if (!stack.isEmpty() && count > 0) {
            stack.shrink(count);
        }
    }

    private static void consumeModifiers(NonNullList<ItemStack> items, List<ItemStack> recipeModifiers) {
        List<ItemStack> remaining = new ArrayList<>(recipeModifiers.stream().map(ItemStack::copy).toList());
        for (int slot : new int[]{SLOT_MODIFIER_1, SLOT_MODIFIER_2, SLOT_MODIFIER_3}) {
            ItemStack slotStack = items.get(slot);
            if (slotStack.isEmpty()) {
                continue;
            }
            for (int index = 0; index < remaining.size(); index++) {
                ItemStack required = remaining.get(index);
                if (ItemStack.isSameItemSameComponents(slotStack, required)) {
                    slotStack.shrink(required.getCount());
                    remaining.remove(index);
                    break;
                }
            }
        }
    }

    @Nullable
    private AltarBlockEntity findNearbyAltar(Level level) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                worldPosition.offset(-ALTAR_RADIUS, -ALTAR_RADIUS, -ALTAR_RADIUS),
                worldPosition.offset(ALTAR_RADIUS, ALTAR_RADIUS, ALTAR_RADIUS))) {
            if (level.getBlockEntity(checkPos) instanceof AltarBlockEntity altar && altar.hasValidStructure()) {
                return altar;
            }
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.witchery.spinning_wheel");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new SpinningWheelMenu(windowId, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        setChanged();
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_BOTTOM;
        }
        return side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(index, itemStack) && (direction == null || slotExposedToFace(index, direction));
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return direction.getAxis().isHorizontal() && index == SLOT_OUTPUT;
    }

    private static boolean slotExposedToFace(int index, Direction direction) {
        for (int slot : direction == Direction.DOWN ? SLOTS_BOTTOM : direction == Direction.UP ? SLOTS_TOP : SLOTS_SIDES) {
            if (slot == index) {
                return true;
            }
        }
        return false;
    }

    public int getSpinProgress() {
        return spinTotal > 0 ? spinProgress * 24 / spinTotal : 0;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("SpinProgress", spinProgress);
        tag.putInt("SpinTotal", spinTotal);
        tag.putInt("PowerLevel", powerLevel);
        tag.putInt("PendingPowerScaled", pendingPowerScaled);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        spinProgress = tag.getIntOr("SpinProgress", 0);
        spinTotal = tag.getIntOr("SpinTotal", DEFAULT_SPIN_TIME);
        powerLevel = tag.getIntOr("PowerLevel", 0);
        pendingPowerScaled = tag.getIntOr("PendingPowerScaled", 0);
    }
}
