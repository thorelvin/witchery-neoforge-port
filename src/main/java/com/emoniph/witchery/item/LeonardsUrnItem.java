package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.menu.LeonardsUrnMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
        if (player instanceof ServerPlayer serverPlayer) {
            int capacity = getCapacity(urn);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new LeonardsUrnMenu(containerId, inventory, hand),
                    urn.getHoverName()), data -> {
                        data.writeEnum(hand);
                        data.writeVarInt(capacity);
                    });
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

        int capacity = getCapacity(stack);
        for (int slot = 0; slot < capacity; slot++) {
            ItemStack stored = getStored(stack, slot);
            Component value = stored.isEmpty()
                    ? Component.translatable("item.witchery.leonards_urn.tooltip.empty")
                    : stored.getHoverName();
            tooltip.accept(Component.translatable("item.witchery.leonards_urn.tooltip.slot",
                    slot + 1, value).withStyle(ChatFormatting.DARK_GRAY));
        }
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

    public static boolean isStorableBrew(ItemStack stack) {
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
        int capacity = getCapacity(urn);
        for (int slot = 0; slot < capacity; slot++) {
            if (getStored(urn, slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static ItemStack getStored(ItemStack urn, int slot) {
        return slot >= 0 && slot < getCapacity(urn) ? StoredBrewInventory.get(urn, slot) : ItemStack.EMPTY;
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
        StoredBrewInventory.set(urn, clampedSlot, stored.copyWithCount(1));
    }

    public static int getCapacity(ItemStack urn) {
        CompoundTag tag = customData(urn);
        return clamp(tag.getIntOr(TAG_CAPACITY, DEFAULT_CAPACITY), DEFAULT_CAPACITY, MAX_CAPACITY);
    }

    public static void setCapacity(ItemStack urn, int capacity) {
        int clamped = clamp(capacity, DEFAULT_CAPACITY, MAX_CAPACITY);
        CustomData.update(DataComponents.CUSTOM_DATA, urn, root -> root.putInt(TAG_CAPACITY, clamped));
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
