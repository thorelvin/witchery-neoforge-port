package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.WitchesOvenBlock;
import com.emoniph.witchery.crafting.OvenRecipe;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMenuTypes;
import com.emoniph.witchery.init.WitcheryRecipeTypes;
import com.emoniph.witchery.menu.WitchesOvenMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WitchesOvenBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_COOKED = 2;
    public static final int SLOT_BYPRODUCT = 3;
    public static final int SLOT_JARS = 4;
    public static final int SLOT_COUNT = 5;

    private static final double BASE_BYPRODUCT_CHANCE = 0.3D;
    private static final double FUNNEL_CHANCE = 0.25D;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookTime = 0;
    private int cookTimeTotal = 180;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
                case 2 -> cookTime;
                case 3 -> cookTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> cookTime = value;
                case 3 -> cookTimeTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public WitchesOvenBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.WITCHES_OVEN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WitchesOvenBlockEntity be) {
        boolean wasBurning = be.isLit();

        if (!be.isLit() && canSmelt(level, be)) {
            be.burnTime = be.items.get(SLOT_FUEL).getBurnTime(WitcheryRecipeTypes.OVEN.get(), level.fuelValues());
            if (be.burnTime > 0) {
                be.maxBurnTime = be.burnTime;
                be.items.get(SLOT_FUEL).shrink(1);
                if (be.items.get(SLOT_FUEL).isEmpty()) {
                    be.items.set(SLOT_FUEL, ItemStack.EMPTY);
                }
            }
        }

        if (be.isLit()) {
            be.burnTime--;
            if (canSmelt(level, be)) {
                be.cookTime++;
                if (be.cookTime >= be.getCookTime()) {
                    be.cookTime = 0;
                    be.cookTimeTotal = be.getCookTime();
                    smeltItem(level, be);
                    be.setChanged();
                }
            } else {
                be.cookTime = 0;
            }
        } else if (!canSmelt(level, be)) {
            be.cookTime = Math.max(0, be.cookTime - 2);
        }

        if (wasBurning != be.isLit()) {
            syncLitState(level, pos, state, be.isLit());
            be.setChanged();
        }
    }

    private static void syncLitState(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(WitchesOvenBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(WitchesOvenBlock.LIT, lit), 3);
        }
    }

    private boolean isLit() {
        return burnTime > 0;
    }

    private static boolean canSmelt(Level level, WitchesOvenBlockEntity be) {
        ItemStack input = be.items.get(SLOT_INPUT);
        if (input.isEmpty()) return false;

        ItemStack cookedResult = be.getCookedResult(level, input);
        if (cookedResult.isEmpty()) return false;

        ItemStack cookedSlot = be.items.get(SLOT_COOKED);
        if (cookedSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(cookedSlot, cookedResult)) return false;
        return cookedSlot.getCount() + cookedResult.getCount() <= cookedSlot.getMaxStackSize();
    }

    private static void smeltItem(Level level, WitchesOvenBlockEntity be) {
        ItemStack input = be.items.get(SLOT_INPUT);
        if (input.isEmpty()) return;

        ItemStack cookedResult = be.getCookedResult(level, input);
        if (cookedResult.isEmpty()) return;

        ItemStack cookedSlot = be.items.get(SLOT_COOKED);
        if (cookedSlot.isEmpty()) {
            be.items.set(SLOT_COOKED, cookedResult.copy());
        } else if (ItemStack.isSameItemSameComponents(cookedSlot, cookedResult)) {
            cookedSlot.grow(cookedResult.getCount());
        }

        be.tryCreateByproduct(level, input);
        input.shrink(1);
        if (input.isEmpty()) {
            be.items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
    }

    private ItemStack getCookedResult(Level level, ItemStack input) {
        Optional<RecipeHolder<OvenRecipe>> ovenRecipe = getOvenRecipe(level, input);
        if (isSaplingInput(input)) {
            return new ItemStack(WitcheryItems.WOOD_ASH.get());
        }
        if (ovenRecipe.isPresent()) {
            return ovenRecipe.get().value().getResultItem().copy();
        }
        return getVanillaSmeltingResult(level, input);
    }

    private void tryCreateByproduct(Level level, ItemStack input) {
        if (items.get(SLOT_JARS).isEmpty()) return;
        if (level.random.nextDouble() > Math.min(BASE_BYPRODUCT_CHANCE + getFumeFunnelsChance(level), 1.0D)) return;

        ItemStack byproduct = getByproductResult(level, input);
        if (byproduct.isEmpty()) return;

        ItemStack byproductSlot = items.get(SLOT_BYPRODUCT);
        if (byproductSlot.isEmpty()) {
            items.set(SLOT_BYPRODUCT, byproduct.copy());
            consumeJar();
        } else if (ItemStack.isSameItemSameComponents(byproductSlot, byproduct)
                && byproductSlot.getCount() + byproduct.getCount() <= byproductSlot.getMaxStackSize()) {
            byproductSlot.grow(byproduct.getCount());
            consumeJar();
        }
    }

    private void consumeJar() {
        ItemStack jars = items.get(SLOT_JARS);
        jars.shrink(1);
        if (jars.isEmpty()) {
            items.set(SLOT_JARS, ItemStack.EMPTY);
        }
    }

    private ItemStack getByproductResult(Level level, ItemStack input) {
        Optional<RecipeHolder<OvenRecipe>> ovenRecipe = getOvenRecipe(level, input);
        if (isSaplingInput(input) && ovenRecipe.isPresent()) {
            return ovenRecipe.get().value().getResultItem().copy();
        }

        if (BuiltInRegistries.ITEM.getKey(input.getItem()).getPath().equals("nether_wart")) {
            return new ItemStack(WitcheryItems.FOUL_FUME.get());
        }

        return ItemStack.EMPTY;
    }

    private static Optional<RecipeHolder<OvenRecipe>> getOvenRecipe(Level level, ItemStack input) {
        return ((net.minecraft.server.level.ServerLevel) level).getServer().getRecipeManager()
                .getRecipeFor(WitcheryRecipeTypes.OVEN.get(), new SingleRecipeInput(input), level);
    }

    private static ItemStack getVanillaSmeltingResult(Level level, ItemStack input) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe = ((net.minecraft.server.level.ServerLevel) level).getServer()
                .getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        if (recipe.isEmpty()) return ItemStack.EMPTY;
        return recipe.get().value().assemble(new SingleRecipeInput(input), level.registryAccess());
    }

    private static boolean isSaplingInput(ItemStack input) {
        return input.is(ItemTags.SAPLINGS);
    }

    private int getFumeFunnels() {
        int funnels = 0;
        for (BlockPos checkPos : new BlockPos[]{
                worldPosition.above(),
                worldPosition.north(),
                worldPosition.south(),
                worldPosition.east(),
                worldPosition.west()}) {
            if (level != null && level.getBlockEntity(checkPos) instanceof FumeFunnelBlockEntity) {
                funnels++;
            }
        }
        return Math.min(funnels, 3);
    }

    private double getFumeFunnelsChance(Level level) {
        return getFumeFunnels() * FUNNEL_CHANCE;
    }

    private int getCookTime() {
        return Math.max(120, 180 - 20 * getFumeFunnels());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.witchery.witches_oven");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new WitchesOvenMenu(windowId, inventory, this, dataAccess);
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
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_COOKED || slot == SLOT_BYPRODUCT) return false;
        if (slot == SLOT_FUEL) return stack.getBurnTime(WitcheryRecipeTypes.OVEN.get(), level.fuelValues()) > 0;
        if (slot == SLOT_JARS) return stack.is(WitcheryItems.CLAY_JAR.get());
        return true;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookTime", cookTime);
        tag.putInt("CookTimeTotal", cookTimeTotal);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, items);
        burnTime = tag.getIntOr("BurnTime", 0);
        maxBurnTime = tag.getIntOr("MaxBurnTime", 0);
        cookTime = tag.getIntOr("CookTime", 0);
        cookTimeTotal = tag.getIntOr("CookTimeTotal", 180);
    }

    public int getBurnProgress() {
        return maxBurnTime > 0 ? burnTime * 13 / maxBurnTime : 0;
    }

    public int getCookProgress() {
        return cookTimeTotal > 0 ? cookTime * 24 / cookTimeTotal : 0;
    }
}
