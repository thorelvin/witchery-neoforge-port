package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.OpenObservationsBookPayload;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

public class ImmortalsObservationsBookItem extends Item {
    public static final String TAG_LEVEL = "Level";
    public static final String TAG_PAGE_STACK = "pageStack";
    public static final int MAX_LEVEL = 9;
    public static final List<String> PAGE_NAMES = List.of(
            "toc", "ritual1", "ritual2", "ritual3", "ritual4", "ritual5", "transfix", "knockback",
            "speed", "resistsun", "smashstone", "bats", "mesmerize", "maker", "maker2", "finalpowers");
    public static final int TOTAL_PAGES = PAGE_NAMES.size();
    private static final int[] PAGE_UNLOCK_LEVELS = {0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 8, 9};
    private static final String PAGE_KEY_PREFIX = "item.witchery.immortals_observations_book.page_";
    private final String tooltipKey;

    public ImmortalsObservationsBookItem(Properties properties) {
        this(properties, "item.witchery.immortals_observations_book.tip");
    }

    public ImmortalsObservationsBookItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            player.awardStat(Stats.ITEM_USED.get(this));
            PacketDistributor.sendToPlayer(serverPlayer, new OpenObservationsBookPayload(
                    getResearchLevel(stack), getStoredPage(stack), hand == InteractionHand.OFF_HAND));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable(tooltipKey)
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean canAddTornPage(ItemStack stack) {
        return isObservationsBook(stack) && getResearchLevel(stack) < MAX_LEVEL;
    }

    public static boolean isObservationsBook(ItemStack stack) {
        return stack.is(WitcheryItems.VAMPIRE_BOOK.get());
    }

    public static boolean addTornPage(ItemStack stack) {
        if (!canAddTornPage(stack)) {
            return false;
        }
        int nextLevel = Math.min(MAX_LEVEL, getResearchLevel(stack) + 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_LEVEL, nextLevel));
        return true;
    }

    public static int getResearchLevel(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(MAX_LEVEL, tag.getIntOr(TAG_LEVEL, 0)));
    }

    public static int getVisiblePages(ItemStack stack) {
        return getVisiblePageCount(getResearchLevel(stack));
    }

    public static int getVisiblePageCount(int researchLevel) {
        int clampedLevel = Math.clamp(researchLevel, 0, MAX_LEVEL);
        int count = 0;
        for (int unlockLevel : PAGE_UNLOCK_LEVELS) {
            if (unlockLevel <= clampedLevel) {
                count++;
            }
        }
        return count;
    }

    public static int getPageUnlockLevel(int pageIndex) {
        return PAGE_UNLOCK_LEVELS[Math.clamp(pageIndex, 0, TOTAL_PAGES - 1)];
    }

    public static String getPageTranslationKey(int pageIndex) {
        return PAGE_KEY_PREFIX + (Math.clamp(pageIndex, 0, TOTAL_PAGES - 1) + 1);
    }

    public static int getStoredPage(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag pages = tag.getListOrEmpty(TAG_PAGE_STACK);
        if (pages.isEmpty()) {
            return 0;
        }
        int pageIndex = PAGE_NAMES.indexOf(pages.getStringOr(pages.size() - 1, ""));
        return Math.clamp(pageIndex, 0, getVisiblePages(stack) - 1);
    }

    public static void setStoredPage(ItemStack stack, int pageIndex) {
        int clampedPage = Math.clamp(pageIndex, 0, getVisiblePages(stack) - 1);
        ListTag pages = new ListTag(clampedPage);
        for (int index = 1; index <= clampedPage; index++) {
            pages.add(StringTag.valueOf(PAGE_NAMES.get(index)));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_PAGE_STACK, pages));
    }

    public static void finishReading(ServerPlayer player, ItemStack stack, int pageIndex) {
        if (!isObservationsBook(stack)) {
            return;
        }
        setStoredPage(stack, pageIndex);
        var data = WitcheryAttachments.get(player);
        if (!data.isVampire()) {
            return;
        }
        int newCap = getResearchLevel(stack) + 1;
        if (newCap > data.getVampireLevelCap()) {
            data.increaseVampireLevelCap(newCap);
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        }
    }
}
