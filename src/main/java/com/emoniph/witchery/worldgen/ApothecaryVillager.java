package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.WorldGenLevel;

final class ApothecaryVillager {
    static final String APOTHECARY_TAG = "witchery_apothecary";

    private static final int MAX_USES = 16;
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
        villager.setOffers(createOffers());
    }

    private static MerchantOffers createOffers() {
        MerchantOffers offers = new MerchantOffers();
        addEmeraldTrade(offers, 2, WitcheryItems.TONGUE_OF_DOG.get(), 2);
        addEmeraldTrade(offers, 1, WitcheryItems.BAT_WOOL.get(), 3);
        addEmeraldTrade(offers, 4, WitcheryItems.SPECTRAL_DUST.get(), 1);
        addEmeraldTrade(offers, 5, WitcheryItems.GARLIC.get(), 1);
        addEmeraldTrade(offers, 6, WitcheryItems.ARTICHOKE.get(), 1);
        addEmeraldTrade(offers, 7, Items.POPPY, 5);
        addEmeraldTrade(offers, 8, Items.SLIME_BALL, 1);
        addEmeraldTrade(offers, 3, Items.CLAY_BALL, 5);
        return offers;
    }

    private static void addEmeraldTrade(MerchantOffers offers, int emeralds, ItemLike result, int count) {
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                new ItemStack(result, count),
                MAX_USES,
                TRADE_XP,
                PRICE_MULTIPLIER));
    }
}
