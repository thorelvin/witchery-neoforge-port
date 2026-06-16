package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.DistilleryBlock;
import com.emoniph.witchery.crafting.DistilleryRecipe;
import com.emoniph.witchery.crafting.DistilleryRecipeInput;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.emoniph.witchery.menu.DistilleryMenu;
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

import java.util.List;
import java.util.Optional;

public class DistilleryBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int SLOT_INPUT_1 = 0;
    public static final int SLOT_INPUT_2 = 1;
    public static final int SLOT_JARS = 2;
    public static final int SLOT_OUTPUT_1 = 3;
    public static final int SLOT_OUTPUT_2 = 4;
    public static final int SLOT_OUTPUT_3 = 5;
    public static final int SLOT_OUTPUT_4 = 6;
    public static final int SLOT_COUNT = 7;

    private static final int[] SLOTS_TOP_AND_BOTTOM = {SLOT_INPUT_1, SLOT_INPUT_2, SLOT_JARS};
    private static final int[] SLOTS_SIDES = {
            SLOT_INPUT_1, SLOT_INPUT_2, SLOT_JARS,
            SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_OUTPUT_3, SLOT_OUTPUT_4
    };

    private static final int DEFAULT_COOK_TIME = 800;
    private static final int ALTAR_RADIUS = 16;
    private static final int POWER_SCALE = 1000;
    private static final int POWER_PER_TICK_SCALED = 600;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookTime = 0;
    private int cookTimeTotal = DEFAULT_COOK_TIME;
    private int powerLevel = 0;
    private int pendingPowerScaled = 0;
    private int scanCooldown = 0;
    // Live readout of the powering altar, sampled each tick for the GUI power gauge/tooltip.
    private int altarPower = 0;
    private int altarMaxPower = 0;

    // ContainerData (DataSlots) sync as shorts, so the absolute power/max are clamped to fit;
    // the gauge fill is sent as a 0..1000 permille that never overflows.
    private static final int DATA_POWER_LEVEL = 0;
    private static final int DATA_COOK_TIME = 2;
    private static final int DATA_COOK_TOTAL = 3;
    private static final int DATA_ALTAR_FILL = 4;
    private static final int DATA_ALTAR_POWER = 5;
    private static final int DATA_ALTAR_MAX = 6;
    private static final int DATA_SLOT_COUNT = 7;
    private static final int SHORT_CLAMP = 32000;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_POWER_LEVEL -> powerLevel;
                case 1 -> 1;
                case DATA_COOK_TIME -> cookTime;
                case DATA_COOK_TOTAL -> cookTimeTotal;
                case DATA_ALTAR_FILL -> altarMaxPower > 0
                        ? Math.min(1000, Math.max(0, altarPower) * 1000 / altarMaxPower)
                        : 0;
                case DATA_ALTAR_POWER -> Math.min(SHORT_CLAMP, Math.max(0, altarPower));
                case DATA_ALTAR_MAX -> Math.min(SHORT_CLAMP, Math.max(0, altarMaxPower));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_POWER_LEVEL -> powerLevel = value;
                case DATA_COOK_TIME -> cookTime = value;
                case DATA_COOK_TOTAL -> cookTimeTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_SLOT_COUNT;
        }
    };

    /** Records the powering altar's current/effective-max power for the GUI gauge (0 when none). */
    private void readAltar(@Nullable AltarBlockEntity altar) {
        altarPower = altar != null ? altar.getPower() : 0;
        altarMaxPower = altar != null ? altar.getEffectiveMaxPower() : 0;
    }

    public DistilleryBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.DISTILLERY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DistilleryBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = false;
        Optional<RecipeHolder<DistilleryRecipe>> recipe = getRecipe(serverLevel, be);

        if (recipe.isEmpty()) {
            if (be.scanCooldown-- <= 0) {
                be.scanCooldown = 40;
                AltarBlockEntity found = be.findNearbyAltar(level);
                be.readAltar(found);
                int newPowerLevel = found != null ? 1 : 0;
                if (newPowerLevel != be.powerLevel) {
                    be.powerLevel = newPowerLevel;
                    changed = true;
                }
            }
            if (be.cookTime != 0 || be.pendingPowerScaled != 0) {
                be.cookTime = 0;
                be.pendingPowerScaled = 0;
                changed = true;
            }
            syncLitState(level, pos, state, false);
            if (changed) {
                be.setChanged();
            }
            return;
        }

        be.cookTimeTotal = recipe.get().value().getCookingTime();
        AltarBlockEntity altar = be.findNearbyAltar(level);
        be.readAltar(altar);
        boolean canProgress = altar != null && canDistill(be, recipe.get().value());

        int newPowerLevel = altar != null ? 1 : 0;
        if (newPowerLevel != be.powerLevel) {
            be.powerLevel = newPowerLevel;
            changed = true;
        }

        if (!canProgress) {
            if (be.cookTime != 0 || be.pendingPowerScaled != 0) {
                be.cookTime = 0;
                be.pendingPowerScaled = 0;
                changed = true;
            }
            syncLitState(level, pos, state, false);
            if (changed) {
                be.setChanged();
            }
            return;
        }

        syncLitState(level, pos, state, true);

        be.pendingPowerScaled += POWER_PER_TICK_SCALED;
        int powerUnits = be.pendingPowerScaled / POWER_SCALE;
        if (powerUnits > 0) {
            if (!altar.consumePower(powerUnits)) {
                be.powerLevel = 0;
                syncLitState(level, pos, state, false);
                if (changed) {
                    be.setChanged();
                }
                return;
            }
            be.pendingPowerScaled -= powerUnits * POWER_SCALE;
        }

        be.cookTime++;
        changed = true;
        if (be.cookTime >= be.cookTimeTotal) {
            be.cookTime = 0;
            distillItem(be, recipe.get().value());
        }

        if (changed) {
            be.setChanged();
        }
    }

    private static void syncLitState(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(DistilleryBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(DistilleryBlock.LIT, lit), 3);
        }
    }

    private static Optional<RecipeHolder<DistilleryRecipe>> getRecipe(ServerLevel level, DistilleryBlockEntity be) {
        return level.getServer().getRecipeManager().getRecipeFor(
                WitcheryRecipeTypes.DISTILLERY.get(),
                new DistilleryRecipeInput(be.items.get(SLOT_INPUT_1), be.items.get(SLOT_INPUT_2), be.items.get(SLOT_JARS)),
                level
        );
    }

    private static boolean canDistill(DistilleryBlockEntity be, DistilleryRecipe recipe) {
        List<ItemStack> outputs = recipe.outputs();
        for (int index = 0; index < 4; index++) {
            ItemStack recipeOutput = index < outputs.size() ? outputs.get(index) : ItemStack.EMPTY;
            if (recipeOutput.isEmpty()) {
                continue;
            }
            ItemStack current = be.items.get(SLOT_OUTPUT_1 + index);
            if (current.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, recipeOutput)) {
                return false;
            }
            if (current.getCount() + recipeOutput.getCount() > Math.min(current.getMaxStackSize(), be.getMaxStackSize())) {
                return false;
            }
        }
        return true;
    }

    private static void distillItem(DistilleryBlockEntity be, DistilleryRecipe recipe) {
        List<ItemStack> outputs = recipe.outputs();
        for (int index = 0; index < outputs.size() && index < 4; index++) {
            ItemStack recipeOutput = outputs.get(index);
            if (recipeOutput.isEmpty()) {
                continue;
            }
            int slot = SLOT_OUTPUT_1 + index;
            ItemStack current = be.items.get(slot);
            if (current.isEmpty()) {
                be.items.set(slot, recipeOutput.copy());
            } else {
                current.grow(recipeOutput.getCount());
            }
        }

        shrinkIfPresent(be.items.get(SLOT_INPUT_1), recipe.getIngredient1().getCount());
        shrinkIfPresent(be.items.get(SLOT_INPUT_2), recipe.getIngredient2().getCount());
        shrinkIfPresent(be.items.get(SLOT_JARS), recipe.jars());
    }

    private static void shrinkIfPresent(ItemStack stack, int count) {
        if (!stack.isEmpty() && count > 0) {
            stack.shrink(count);
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
        return Component.translatable("container.witchery.distillery");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DistilleryMenu(id, inventory, this, data);
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
        if (slot >= SLOT_OUTPUT_1) {
            return false;
        }
        if (slot == SLOT_JARS) {
            return stack.is(WitcheryItems.CLAY_JAR.get());
        }
        return !stack.is(WitcheryItems.CLAY_JAR.get());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side.getAxis().isVertical() ? SLOTS_TOP_AND_BOTTOM : SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(index, itemStack) && (direction == null || slotExposedToFace(index, direction));
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return direction.getAxis().isHorizontal() && index >= SLOT_OUTPUT_1 && index <= SLOT_OUTPUT_4;
    }

    private static boolean slotExposedToFace(int index, Direction direction) {
        for (int slot : direction.getAxis().isVertical() ? SLOTS_TOP_AND_BOTTOM : SLOTS_SIDES) {
            if (slot == index) {
                return true;
            }
        }
        return false;
    }

    public int getBurnProgress() {
        return powerLevel > 0 ? 13 : 0;
    }

    public int getCookProgress() {
        return cookTimeTotal > 0 ? cookTime * 24 / cookTimeTotal : 0;
    }

    public boolean isLitPublic() {
        return powerLevel > 0;
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
        tag.putInt("CookTime", cookTime);
        tag.putInt("CookTimeTotal", cookTimeTotal);
        tag.putInt("PowerLevel", powerLevel);
        tag.putInt("PendingPowerScaled", pendingPowerScaled);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        cookTime = tag.getIntOr("CookTime", 0);
        cookTimeTotal = tag.getIntOr("CookTimeTotal", DEFAULT_COOK_TIME);
        powerLevel = tag.getIntOr("PowerLevel", 0);
        pendingPowerScaled = tag.getIntOr("PendingPowerScaled", 0);
    }
}
