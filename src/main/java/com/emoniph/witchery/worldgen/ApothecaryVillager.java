package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.WorldGenLevel;

public final class ApothecaryVillager {
    public static final String APOTHECARY_TAG = "witchery_apothecary";

    private static final String TRADE_VERSION_TAG = "witchery_apothecary_trade_version";
    private static final int TRADE_VERSION = 1;
    private static final int CUSTOM_TRADE_XP = 10;
    private static final int TRADE_XP = 0;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private ApothecaryVillager() {
    }

    static void configure(Villager villager, WorldGenLevel level) {
        villager.getPersistentData().putBoolean(APOTHECARY_TAG, true);
        villager.setCustomName(Component.literal("Apothecary"));
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.getLevel().registryAccess(), VillagerProfession.LIBRARIAN)
                .withLevel(1));
        villager.setVillagerXp(0);
        replaceOffers(villager, 1);
    }

    public static boolean isApothecary(Villager villager) {
        return villager.getPersistentData().getBooleanOr(APOTHECARY_TAG, false);
    }

    public static boolean refreshIfApothecary(Villager villager) {
        if (!isApothecary(villager) || villager.level().isClientSide()) {
            return false;
        }

        int level = clampLevel(villager.getVillagerData().level());
        int xp = Math.max(villager.getVillagerXp(), minXpForLevel(level));
        villager.setCustomName(Component.literal("Apothecary"));
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.level().registryAccess(), VillagerProfession.LIBRARIAN)
                .withLevel(level));
        villager.setVillagerXp(xp);

        MerchantOffers expected = createOffers(level);
        if (villager.getPersistentData().getIntOr(TRADE_VERSION_TAG, 0) != TRADE_VERSION
                || !hasExpectedResults(villager.getOffers(), expected)) {
            villager.setOffers(expected);
            villager.getPersistentData().putInt(TRADE_VERSION_TAG, TRADE_VERSION);
            return true;
        }
        return false;
    }

    public static boolean handleTrade(Villager villager) {
        if (!isApothecary(villager) || villager.level().isClientSide()) {
            return false;
        }

        int level = clampLevel(villager.getVillagerData().level());
        if (!VillagerData.canLevelUp(level)) {
            return false;
        }

        int xp = villager.getVillagerXp() + CUSTOM_TRADE_XP;
        boolean leveled = false;
        while (VillagerData.canLevelUp(level) && xp >= VillagerData.getMaxXpPerLevel(level)) {
            level++;
            leveled = true;
        }

        villager.setVillagerXp(xp);
        if (leveled) {
            villager.setVillagerData(villager.getVillagerData().withLevel(level));
            replaceOffers(villager, level);
            villager.level().broadcastEntityEvent(villager, (byte) 14);
        }
        return true;
    }

    public static void syncOffers(Villager villager, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.sendMerchantOffers(
                serverPlayer.containerMenu.containerId,
                villager.getOffers(),
                villager.getVillagerData().level(),
                villager.getVillagerXp(),
                villager.showProgressBar(),
                villager.canRestock());
    }

    private static void replaceOffers(Villager villager, int level) {
        villager.setOffers(createOffers(level));
        villager.getPersistentData().putInt(TRADE_VERSION_TAG, TRADE_VERSION);
    }

    private static MerchantOffers createOffers(int level) {
        MerchantOffers offers = new MerchantOffers();
        addNoviceOffers(offers);
        if (level >= 2) {
            addApprenticeOffers(offers);
        }
        if (level >= 3) {
            addJourneymanOffers(offers);
        }
        if (level >= 4) {
            addExpertOffers(offers);
        }
        if (level >= 5) {
            addMasterOffers(offers);
        }
        return offers;
    }

    private static void addNoviceOffers(MerchantOffers offers) {
        addBuyTrade(offers, WitcheryItems.BELLADONNA.get(), 4, 1, 16);
        addBuyTrade(offers, WitcheryItems.MANDRAKE_ROOT.get(), 3, 1, 16);
        addSellTrade(offers, 1, WitcheryItems.GARLIC.get(), 2, 12);
        addSellTrade(offers, 1, WitcheryItems.ARTICHOKE.get(), 2, 12);
        addSellTrade(offers, 2, WitcheryItems.CLAY_JAR.get(), 4, 12);
        addSellTrade(offers, 2, WitcheryItems.BAT_WOOL.get(), 2, 10);
    }

    private static void addApprenticeOffers(MerchantOffers offers) {
        addBuyTrade(offers, WitcheryItems.WORMWOOD.get(), 4, 1, 16);
        addBuyTrade(offers, WitcheryItems.SNOWBELL.get(), 4, 1, 16);
        addSellTrade(offers, 1, WitcheryItems.WORMWOOD.get(), 2, 12);
        addSellTrade(offers, 1, WitcheryItems.SNOWBELL.get(), 2, 12);
        addSellTrade(offers, 2, WitcheryItems.ROWAN_BERRIES.get(), 4, 12);
        addSellTrade(offers, 3, WitcheryItems.WOOD_ASH.get(), 4, 12);
    }

    private static void addJourneymanOffers(MerchantOffers offers) {
        addSellTrade(offers, 4, WitcheryItems.TAGLOCK_KIT.get(), 1, 8);
        addSellTrade(offers, 3, WitcheryItems.TONGUE_OF_DOG.get(), 2, 8);
        addSellTrade(offers, 3, WitcheryItems.TOE_OF_FROG.get(), 2, 8);
        addSellTrade(offers, 4, WitcheryItems.SPECTRAL_DUST.get(), 2, 8);
        addSellTrade(offers, 5, WitcheryItems.EXHALE_OF_HORNED_ONE.get(), 1, 8);
        addSellTrade(offers, 5, WitcheryItems.HINT_OF_REBIRTH.get(), 1, 8);
    }

    private static void addExpertOffers(MerchantOffers offers) {
        addSellTrade(offers, 5, WitcheryItems.WHIFF_OF_MAGIC.get(), 1, 8);
        addSellTrade(offers, 6, WitcheryItems.FOUL_FUME.get(), 1, 8);
        addSellTrade(offers, 6, WitcheryItems.ODOUR_OF_PURITY.get(), 1, 8);
        addSellTrade(offers, 6, WitcheryItems.REEK_OF_MISFORTUNE.get(), 1, 8);
        addSellTrade(offers, 7, WitcheryItems.BREATH_OF_GODDESS.get(), 1, 6);
        addSellTrade(offers, 7, WitcheryItems.DIAMOND_VAPOUR.get(), 1, 6);
    }

    private static void addMasterOffers(MerchantOffers offers) {
        addSellTrade(offers, 7, WitcheryItems.MUTANDIS.get(), 1, 8);
        addSellTrade(offers, 12, WitcheryItems.MUTANDIS_EXTREMIS.get(), 1, 4);
        addSellTrade(offers, 8, WitcheryItems.ANOINTING_PASTE.get(), 1, 6);
        addSellTrade(offers, 6, WitcheryItems.BREW_OF_REVEALING.get(), 1, 6);
        addSellTrade(offers, 8, WitcheryItems.BREW_OF_SLEEPING.get(), 1, 6);
        addSellTrade(offers, 10, WitcheryItems.BREW_OF_LOVE.get(), 1, 4);
    }

    private static void addSellTrade(MerchantOffers offers, int emeralds, ItemLike result, int count, int maxUses) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                new ItemStack(result, count),
                maxUses,
                TRADE_XP,
                PRICE_MULTIPLIER));
    }

    private static void addBuyTrade(MerchantOffers offers, ItemLike cost, int costCount, int emeralds, int maxUses) {
        offers.add(new MerchantOffer(
                new ItemCost(cost, costCount),
                new ItemStack(Items.EMERALD, emeralds),
                maxUses,
                TRADE_XP,
                PRICE_MULTIPLIER));
    }

    private static boolean hasExpectedResults(MerchantOffers current, MerchantOffers expected) {
        if (current.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!ItemStack.matches(current.get(i).getResult(), expected.get(i).getResult())) {
                return false;
            }
        }
        return true;
    }

    private static int clampLevel(int level) {
        return Math.max(VillagerData.MIN_VILLAGER_LEVEL, Math.min(VillagerData.MAX_VILLAGER_LEVEL, level));
    }

    private static int minXpForLevel(int level) {
        return level <= VillagerData.MIN_VILLAGER_LEVEL ? 0 : VillagerData.getMaxXpPerLevel(level - 1);
    }
}
