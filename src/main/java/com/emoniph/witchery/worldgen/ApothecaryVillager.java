package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class ApothecaryVillager {
    public static final String APOTHECARY_TAG = "witchery_apothecary";

    private static final String TRADE_VERSION_TAG = "witchery_apothecary_trade_version";
    private static final String JOB_SITE_X_TAG = "witchery_apothecary_job_site_x";
    private static final String JOB_SITE_Y_TAG = "witchery_apothecary_job_site_y";
    private static final String JOB_SITE_Z_TAG = "witchery_apothecary_job_site_z";
    private static final ResourceKey<VillagerProfession> APOTHECARY_PROFESSION = VillagerProfession.LEATHERWORKER;
    private static final int TRADE_VERSION = 1;
    private static final int PROFESSION_LOCK_XP = 1;
    private static final int CUSTOM_TRADE_XP = 10;
    private static final int TRADE_XP = 0;
    private static final int JOB_SITE_SEARCH_RADIUS = 8;
    private static final int JOB_SITE_SEARCH_VERTICAL = 3;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private ApothecaryVillager() {
    }

    static void configure(Villager villager, WorldGenLevel level) {
        configure(villager, level, null);
    }

    static void configure(Villager villager, WorldGenLevel level, BlockPos jobSite) {
        villager.getPersistentData().putBoolean(APOTHECARY_TAG, true);
        villager.setCustomName(Component.literal("Apothecary"));
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.getLevel().registryAccess(), APOTHECARY_PROFESSION)
                .withLevel(1));
        villager.setVillagerXp(PROFESSION_LOCK_XP);
        if (jobSite != null) {
            rememberJobSite(villager, jobSite);
            ensureJobSite(villager, level.getLevel(), jobSite);
        }
        replaceOffers(villager, 1);
    }

    public static boolean isApothecary(Villager villager) {
        return villager.getPersistentData().getBooleanOr(APOTHECARY_TAG, false);
    }

    public static boolean refreshIfApothecary(Villager villager) {
        if (!isApothecary(villager) || villager.level().isClientSide()) {
            return false;
        }

        VillagerData currentData = villager.getVillagerData();
        int level = clampLevel(currentData.level());
        int xp = Math.max(villager.getVillagerXp(), minXpForLevel(level));
        ServerLevel serverLevel = villager.level() instanceof ServerLevel sl ? sl : null;
        Optional<BlockPos> jobSite = rememberedJobSite(villager)
                .or(() -> serverLevel == null ? Optional.empty() : findNearbyJobSite(villager, serverLevel));
        boolean repaired = false;

        if (!villager.hasCustomName()
                || villager.getCustomName() == null
                || !"Apothecary".equals(villager.getCustomName().getString())) {
            villager.setCustomName(Component.literal("Apothecary"));
            repaired = true;
        }

        if (!currentData.profession().is(APOTHECARY_PROFESSION) || currentData.level() != level) {
            villager.setVillagerData(currentData
                    .withProfession(villager.level().registryAccess(), APOTHECARY_PROFESSION)
                    .withLevel(level));
            if (serverLevel != null) {
                villager.refreshBrain(serverLevel);
            }
            repaired = true;
        }

        if (serverLevel != null && jobSite.isPresent() && ensureJobSite(villager, serverLevel, jobSite.get())) {
            repaired = true;
        }

        if (villager.getVillagerXp() < xp) {
            villager.setVillagerXp(xp);
            repaired = true;
        }

        MerchantOffers expected = createOffers(level);
        if (villager.getPersistentData().getIntOr(TRADE_VERSION_TAG, 0) != TRADE_VERSION
                || !hasExpectedResults(villager.getOffers(), expected)) {
            villager.setOffers(expected);
            villager.getPersistentData().putInt(TRADE_VERSION_TAG, TRADE_VERSION);
            return true;
        }
        return repaired;
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

    private static void rememberJobSite(Villager villager, BlockPos jobSite) {
        villager.getPersistentData().putInt(JOB_SITE_X_TAG, jobSite.getX());
        villager.getPersistentData().putInt(JOB_SITE_Y_TAG, jobSite.getY());
        villager.getPersistentData().putInt(JOB_SITE_Z_TAG, jobSite.getZ());
    }

    private static Optional<BlockPos> rememberedJobSite(Villager villager) {
        if (!villager.getPersistentData().contains(JOB_SITE_X_TAG)
                || !villager.getPersistentData().contains(JOB_SITE_Y_TAG)
                || !villager.getPersistentData().contains(JOB_SITE_Z_TAG)) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(
                villager.getPersistentData().getIntOr(JOB_SITE_X_TAG, 0),
                villager.getPersistentData().getIntOr(JOB_SITE_Y_TAG, 0),
                villager.getPersistentData().getIntOr(JOB_SITE_Z_TAG, 0)));
    }

    private static Optional<BlockPos> findNearbyJobSite(Villager villager, ServerLevel level) {
        BlockPos origin = villager.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-JOB_SITE_SEARCH_RADIUS, -JOB_SITE_SEARCH_VERTICAL, -JOB_SITE_SEARCH_RADIUS),
                origin.offset(JOB_SITE_SEARCH_RADIUS, JOB_SITE_SEARCH_VERTICAL, JOB_SITE_SEARCH_RADIUS))) {
            if (!isCauldron(level.getBlockState(pos))) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        if (best != null) {
            rememberJobSite(villager, best);
        }
        return Optional.ofNullable(best);
    }

    private static boolean ensureJobSite(Villager villager, ServerLevel level, BlockPos jobSite) {
        if (!isCauldron(level.getBlockState(jobSite))) {
            return false;
        }

        GlobalPos globalPos = GlobalPos.of(level.dimension(), jobSite);
        Optional<GlobalPos> current = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (current.isPresent() && current.get().equals(globalPos)) {
            return false;
        }

        PoiManager poiManager = level.getPoiManager();
        if (poiManager.exists(jobSite, holder -> holder.is(PoiTypes.LEATHERWORKER))) {
            poiManager.take(holder -> holder.is(PoiTypes.LEATHERWORKER),
                    (holder, pos) -> pos.equals(jobSite), jobSite, 1);
        }
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalPos);
        rememberJobSite(villager, jobSite);
        return true;
    }

    private static boolean isCauldron(BlockState state) {
        return state.is(Blocks.CAULDRON)
                || state.is(Blocks.WATER_CAULDRON)
                || state.is(Blocks.LAVA_CAULDRON)
                || state.is(Blocks.POWDER_SNOW_CAULDRON);
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
        return level <= VillagerData.MIN_VILLAGER_LEVEL
                ? PROFESSION_LOCK_XP
                : Math.max(PROFESSION_LOCK_XP, VillagerData.getMaxXpPerLevel(level - 1));
    }
}
