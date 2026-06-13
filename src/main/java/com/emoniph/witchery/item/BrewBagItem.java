package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "removal"})
public class BrewBagItem extends Item {
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final int SLOT_COUNT = 8;

    public BrewBagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSelectedSlot(bag);
                player.displayClientMessage(Component.translatable(
                        "item.witchery.brew_bag.selected", selectedSlot(bag) + 1, SLOT_COUNT), true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        if (isStorableBrew(other)) {
            if (!level.isClientSide()) {
                storeBrew(bag, other, player);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            throwSelectedBrew(level, player, bag);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.witchery.brew_bag.tip")
                .withStyle(ChatFormatting.GRAY));

        int selected = selectedSlot(stack);
        tooltip.accept(Component.translatable("item.witchery.brew_bag.tooltip.selected",
                selected + 1, SLOT_COUNT).withStyle(ChatFormatting.GOLD));
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stored = getStored(stack, slot);
            if (!stored.isEmpty()) {
                tooltip.accept(Component.translatable("item.witchery.brew_bag.tooltip.slot",
                        slot + 1, stored.getCount(), stored.getHoverName())
                        .withStyle(slot == selected ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static void storeBrew(ItemStack bag, ItemStack brew, Player player) {
        int slot = firstAvailableSlot(bag, brew);
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("item.witchery.brew_bag.full"), true);
            return;
        }

        ItemStack stored = getStored(bag, slot);
        if (stored.isEmpty()) {
            stored = brew.copyWithCount(1);
        } else {
            stored = stored.copy();
            stored.grow(1);
        }
        setStored(bag, slot, stored);
        setSelectedSlot(bag, slot);
        if (!player.getAbilities().instabuild) {
            brew.shrink(1);
        }
        player.displayClientMessage(Component.translatable("item.witchery.brew_bag.stored",
                stored.getHoverName(), slot + 1), true);
    }

    private static void throwSelectedBrew(Level level, Player player, ItemStack bag) {
        int slot = selectedSlot(bag);
        ItemStack stored = getStored(bag, slot);
        if (!ThrowableBrewSupport.isThrowableBrew(stored)) {
            player.displayClientMessage(Component.translatable("item.witchery.brew_bag.empty"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
            return;
        }

        Component thrownName = stored.getHoverName();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        ThrownBrewEntity projectile = ThrowableBrewSupport.createProjectile(level, player, stored);
        if (projectile == null) {
            player.displayClientMessage(Component.translatable("item.witchery.brew_bag.empty"), true);
            return;
        }
        projectile.shootFromSourceThrower(player);
        level.addFreshEntity(projectile);

        if (!player.getAbilities().instabuild) {
            stored.shrink(1);
            setStored(bag, slot, stored);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        player.displayClientMessage(Component.translatable("item.witchery.brew_bag.thrown",
                thrownName, slot + 1), true);
    }

    private static boolean isStorableBrew(ItemStack stack) {
        return ThrowableBrewSupport.isThrowableBrew(stack);
    }

    private static int firstAvailableSlot(ItemStack bag, ItemStack brew) {
        int selected = selectedSlot(bag);
        ItemStack selectedStack = getStored(bag, selected);
        if (canMerge(selectedStack, brew)) {
            return selected;
        }
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (slot == selected) {
                continue;
            }
            ItemStack stored = getStored(bag, slot);
            if (canMerge(stored, brew)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean canMerge(ItemStack stored, ItemStack brew) {
        return stored.isEmpty()
                || ItemStack.isSameItemSameComponents(stored, brew)
                && stored.getCount() < Math.min(stored.getMaxStackSize(), brew.getMaxStackSize());
    }

    private static ItemStack getStored(ItemStack bag, int slot) {
        CompoundTag root = customData(bag);
        ListTag items = root.getCompoundOrEmpty(TAG_INVENTORY).getListOrEmpty(TAG_ITEMS);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompoundOrEmpty(i);
            int entrySlot = entry.getByteOr(TAG_SLOT, (byte) 0) & 255;
            if (entrySlot == slot) {
                return entry.read(TAG_STACK, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    private static void setStored(ItemStack bag, int slot, ItemStack stored) {
        int clampedSlot = clamp(slot, 0, SLOT_COUNT - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, bag, root -> {
            CompoundTag inventory = root.getCompoundOrEmpty(TAG_INVENTORY);
            ListTag existing = inventory.getListOrEmpty(TAG_ITEMS);
            ListTag updated = new ListTag();

            for (int i = 0; i < existing.size(); i++) {
                CompoundTag entry = existing.getCompoundOrEmpty(i);
                int entrySlot = entry.getByteOr(TAG_SLOT, (byte) 0) & 255;
                if (entrySlot != clampedSlot) {
                    updated.add(entry.copy());
                }
            }

            if (!stored.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte(TAG_SLOT, (byte) clampedSlot);
                entry.store(TAG_STACK, ItemStack.CODEC, stored.copy());
                updated.add(entry);
            }

            inventory.put(TAG_ITEMS, updated);
            root.put(TAG_INVENTORY, inventory);
        });
    }

    private static void cycleSelectedSlot(ItemStack bag) {
        setSelectedSlot(bag, (selectedSlot(bag) + 1) % SLOT_COUNT);
    }

    private static void setSelectedSlot(ItemStack bag, int slot) {
        CustomData.update(DataComponents.CUSTOM_DATA, bag,
                root -> root.putInt(TAG_SELECTED, clamp(slot, 0, SLOT_COUNT - 1)));
    }

    private static int selectedSlot(ItemStack bag) {
        return clamp(customData(bag).getIntOr(TAG_SELECTED, 0), 0, SLOT_COUNT - 1);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
