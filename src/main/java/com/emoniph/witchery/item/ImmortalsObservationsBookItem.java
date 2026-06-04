package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImmortalsObservationsBookItem extends Item {
    public static final String TAG_LEVEL = "Level";
    public static final int MAX_LEVEL = 9;
    public static final int BASE_VISIBLE_PAGES = 6;
    public static final int TOTAL_PAGES = 16;
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
        // Client: open the native Modonomicon "Observations of an Immortal" book (datapack-synced).
        if (level.isClientSide()) {
            com.emoniph.witchery.client.WitcheryBookOpener.open(com.emoniph.witchery.book.WitcheryBooks.OBSERVATIONS);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            player.awardStat(Stats.ITEM_USED.get(this));
            // Unlocks the next vampire level cap when a vampire reads this book (preserved from the
            // old level-gated reader; Torn Pages still raise the research level that drives the cap).
            var data = WitcheryAttachments.get(serverPlayer);
            if (data.isVampire()) {
                int newCap = getResearchLevel(stack) + 1;
                if (newCap > data.getVampireLevelCap()) {
                    data.increaseVampireLevelCap(newCap);
                    PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
                }
            }
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
        int level = getResearchLevel(stack);
        int bonusPage = level >= 8 ? 1 : 0;
        return Math.min(TOTAL_PAGES, BASE_VISIBLE_PAGES + level + bonusPage);
    }

    public static List<Component> createReadablePages(int visiblePages) {
        int clampedPages = Math.max(1, Math.min(TOTAL_PAGES, visiblePages));
        List<Component> pages = new ArrayList<>(clampedPages);
        for (int i = 1; i <= clampedPages; i++) {
            pages.add(Component.translatable(PAGE_KEY_PREFIX + i));
        }
        return pages;
    }
}
