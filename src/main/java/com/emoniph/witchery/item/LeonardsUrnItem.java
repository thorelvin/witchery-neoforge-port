package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "removal"})
public class LeonardsUrnItem extends Item {
    private static final String TAG_CAPACITY = "Capacity";
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_STACK = "Stack";
    private static final int DEFAULT_CAPACITY = 1;
    private static final int MAX_CAPACITY = 4;

    public LeonardsUrnItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack urn = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSelectedSlot(urn);
                int selected = selectedSlot(urn);
                int capacity = getCapacity(urn);
                player.displayClientMessage(Component.translatable(
                        "item.witchery.leonards_urn.selected", selected + 1, capacity), true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        if (isStorableBrew(other)) {
            if (!level.isClientSide()) {
                storeBrew(urn, other, player);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            castStoredBrew(level, player, urn);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.witchery.leonards_urn.tip")
                .withStyle(ChatFormatting.BLUE));

        int selected = selectedSlot(stack);
        int capacity = getCapacity(stack);
        tooltip.accept(Component.translatable("item.witchery.leonards_urn.tooltip.selected",
                selected + 1, capacity).withStyle(ChatFormatting.GRAY));

        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stored = getStored(stack, slot);
            Component value = stored.isEmpty()
                    ? Component.translatable("item.witchery.leonards_urn.tooltip.empty")
                    : stored.getHoverName();
            tooltip.accept(Component.translatable("item.witchery.leonards_urn.tooltip.slot",
                    slot + 1, value).withStyle(slot == selected ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
    }

    private static void storeBrew(ItemStack urn, ItemStack brew, Player player) {
        int slot = firstAvailableSlot(urn);
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.full"), true);
            return;
        }

        ItemStack stored = brew.copyWithCount(1);
        setStored(urn, slot, stored);
        setSelectedSlot(urn, slot);
        if (!player.getAbilities().instabuild) {
            brew.shrink(1);
        }
        player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.stored",
                stored.getHoverName()), true);
        playUrnStoreEffects(player);
    }

    private static void castStoredBrew(Level level, Player player, ItemStack urn) {
        int slot = selectedSlot(urn);
        ItemStack stored = getStored(urn, slot);
        if (!ThrowableBrewSupport.isThrowableBrew(stored)) {
            player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.empty"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            playUrnFailEffects(level, player);
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        ThrownBrewEntity projectile = ThrowableBrewSupport.createProjectile(level, player, stored);
        if (projectile == null) {
            player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.empty"), true);
            return;
        }
        projectile.shootFromSourceThrower(player);
        level.addFreshEntity(projectile);
        playUrnCastEffects(level, player);
        player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.cast",
                stored.getHoverName()), true);
    }

    /**
     * Cast the brew held in {@code slot} of the first Leonard's Urn in the player's inventory, used
     * by the Mystic Branch Caelum/Baratrum/Ortus/Occasus glyphs. Returns true if a brew was thrown.
     */
    public static boolean castSlotFromInventory(net.minecraft.server.level.ServerLevel level, Player player, int slot) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack urn = player.getInventory().getItem(i);
            if (!(urn.getItem() instanceof LeonardsUrnItem)) {
                continue;
            }
            ItemStack stored = getStored(urn, slot);
            if (!ThrowableBrewSupport.isThrowableBrew(stored)) {
                player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.empty"), true);
                playUrnFailEffects(level, player);
                return false;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                    0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
            ThrownBrewEntity projectile = ThrowableBrewSupport.createProjectile(level, player, stored);
            if (projectile == null) {
                player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.empty"), true);
                return false;
            }
            projectile.shootFromSourceSpell(player);
            level.addFreshEntity(projectile);
            playUrnCastEffects(level, player);
            player.displayClientMessage(Component.translatable("item.witchery.leonards_urn.cast",
                    stored.getHoverName()), true);
            return true;
        }
        return false;
    }

    private static boolean isStorableBrew(ItemStack stack) {
        return ThrowableBrewSupport.isThrowableBrew(stack);
    }

    private static void playUrnStoreEffects(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 0.85F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                10, 0.25D, 0.35D, 0.25D, 0.02D);
    }

    private static void playUrnCastEffects(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.25D, 0.45D, 0.25D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                8, 0.18D, 0.25D, 0.18D, 0.02D);
    }

    private static void playUrnFailEffects(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                6, 0.18D, 0.2D, 0.18D, 0.01D);
    }

    private static int firstAvailableSlot(ItemStack urn) {
        int selected = selectedSlot(urn);
        if (getStored(urn, selected).isEmpty()) {
            return selected;
        }
        int capacity = getCapacity(urn);
        for (int slot = 0; slot < capacity; slot++) {
            if (getStored(urn, slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static ItemStack getStored(ItemStack urn, int slot) {
        CompoundTag root = customData(urn);
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

    public static ItemStack getStoredBrew(ItemStack urn, int slot) {
        return getStored(urn, slot);
    }

    public static boolean tryInsertStoredBrew(ItemStack urn, ItemStack brew) {
        if (!isStorableBrew(brew)) {
            return false;
        }
        int slot = firstAvailableSlot(urn);
        if (slot < 0) {
            return false;
        }
        setStored(urn, slot, brew.copyWithCount(1));
        return true;
    }

    public static ItemStack createWithCapacity(int capacity) {
        ItemStack urn = new ItemStack(WitcheryItems.LEONARDS_URN.get());
        setCapacity(urn, capacity);
        return urn;
    }

    private static void setStored(ItemStack urn, int slot, ItemStack stored) {
        int capacity = getCapacity(urn);
        int clampedSlot = clamp(slot, 0, capacity - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, urn, root -> {
            root.putInt(TAG_CAPACITY, capacity);
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
                entry.store(TAG_STACK, ItemStack.CODEC, stored.copyWithCount(1));
                updated.add(entry);
            }

            inventory.put(TAG_ITEMS, updated);
            root.put(TAG_INVENTORY, inventory);
        });
    }

    private static void cycleSelectedSlot(ItemStack urn) {
        int capacity = getCapacity(urn);
        setSelectedSlot(urn, (selectedSlot(urn) + 1) % capacity);
    }

    private static void setSelectedSlot(ItemStack urn, int slot) {
        int capacity = getCapacity(urn);
        CustomData.update(DataComponents.CUSTOM_DATA, urn, root -> {
            root.putInt(TAG_CAPACITY, capacity);
            root.putInt(TAG_SELECTED, clamp(slot, 0, capacity - 1));
        });
    }

    private static int selectedSlot(ItemStack urn) {
        CompoundTag tag = customData(urn);
        return clamp(tag.getIntOr(TAG_SELECTED, 0), 0, getCapacity(urn) - 1);
    }

    public static int getCapacity(ItemStack urn) {
        CompoundTag tag = customData(urn);
        return clamp(tag.getIntOr(TAG_CAPACITY, DEFAULT_CAPACITY), DEFAULT_CAPACITY, MAX_CAPACITY);
    }

    public static void setCapacity(ItemStack urn, int capacity) {
        int clamped = clamp(capacity, DEFAULT_CAPACITY, MAX_CAPACITY);
        CustomData.update(DataComponents.CUSTOM_DATA, urn, root -> {
            root.putInt(TAG_CAPACITY, clamped);
            root.putInt(TAG_SELECTED, clamp(root.getIntOr(TAG_SELECTED, 0), 0, clamped - 1));
        });
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
