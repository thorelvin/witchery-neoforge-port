package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.world.VanishingHutData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** One unusual, restart-stable reagent exchange for each Vanishing Hut relocation cycle. */
public final class VanishingHutBargains {

    public enum Bargain {
        FORTUNE_FOR_EVIL(WitcheryItems.REFINED_EVIL, 2, WitcheryItems.DROP_OF_LUCK, 1),
        WILL_FOR_DEW(WitcheryItems.ENDER_DEW, 4, WitcheryItems.FOCUSED_WILL, 2),
        DUST_FOR_VAPOUR(WitcheryItems.DIAMOND_VAPOUR, 3, WitcheryItems.SPECTRAL_DUST, 4),
        TEARS_FOR_TWINE(WitcheryItems.TEAR_OF_GODDESS, 3, WitcheryItems.TORMENTED_TWINE, 1),
        PURITY_FOR_BREATH(WitcheryItems.BREATH_OF_GODDESS, 3, WitcheryItems.ODOUR_OF_PURITY, 2);

        private final Supplier<? extends Item> cost;
        private final int costCount;
        private final Supplier<? extends Item> reward;
        private final int rewardCount;

        Bargain(Supplier<? extends Item> cost, int costCount,
                Supplier<? extends Item> reward, int rewardCount) {
            this.cost = cost;
            this.costCount = costCount;
            this.reward = reward;
            this.rewardCount = rewardCount;
        }

        public Item cost() {
            return cost.get();
        }

        public int costCount() {
            return costCount;
        }

        public Item reward() {
            return reward.get();
        }

        public int rewardCount() {
            return rewardCount;
        }
    }

    private VanishingHutBargains() {
    }

    /** UUID and cycle are the only inputs, so save/reload and interaction spam cannot reroll an offer. */
    public static Bargain forCycle(UUID hutId, int cycle) {
        long mixed = hutId.getMostSignificantBits()
                ^ Long.rotateLeft(hutId.getLeastSignificantBits(), 21)
                ^ (0x9E3779B97F4A7C15L * (cycle + 1L));
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return Bargain.values()[Math.floorMod((int) mixed, Bargain.values().length)];
    }

    /** Handles a hut resident before ordinary Coven-Witch recruitment or merchant interaction. */
    public static InteractionResult interact(CovenWitchEntity witch, Player player) {
        if (!(witch.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Optional<VanishingHutData.Entry> located = locate(level, witch);
        if (located.isEmpty()) {
            return InteractionResult.PASS;
        }

        VanishingHutData data = VanishingHutData.get(level);
        VanishingHutData.Entry entry = located.get();
        if (VanishingHut.isHeldBySalt(level, entry.pos())) {
            entry = data.observeSalt(entry.hutId(), level.getGameTime()).orElse(entry);
        }
        if (entry.restraintStage() > 0) {
            Component line = Component.translatable(entry.restraintStage() >= 2
                            ? "witchery.vanishing_hut.bargain.angry"
                            : "witchery.vanishing_hut.bargain.salted",
                    witch.getDisplayName()).withStyle(ChatFormatting.DARK_RED);
            player.displayClientMessage(line, false);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    witch.getX(), witch.getY() + witch.getBbHeight(), witch.getZ(),
                    5, 0.25D, 0.25D, 0.25D, 0.0D);
            level.playSound(null, witch.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.NEUTRAL, 0.9F, 0.8F);
            return InteractionResult.CONSUME;
        }
        if (entry.bargainCompleted()) {
            player.displayClientMessage(Component.translatable(
                    "witchery.vanishing_hut.bargain.completed", witch.getDisplayName())
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
            return InteractionResult.CONSUME;
        }

        Bargain bargain = forCycle(entry.hutId(), entry.bargainCycle());
        ItemStack held = player.getMainHandItem();
        if (!held.is(bargain.cost()) || held.getCount() < bargain.costCount()) {
            player.displayClientMessage(Component.translatable(
                    "witchery.vanishing_hut.bargain.offer", witch.getDisplayName(),
                    bargain.costCount(), bargain.cost().getDefaultInstance().getHoverName(),
                    bargain.rewardCount(), bargain.reward().getDefaultInstance().getHoverName())
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
            return InteractionResult.CONSUME;
        }

        data.completeBargain(entry.hutId());
        if (!player.getAbilities().instabuild) {
            held.shrink(bargain.costCount());
        }
        ItemStack reward = new ItemStack(bargain.reward(), bargain.rewardCount());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        player.displayClientMessage(Component.translatable(
                "witchery.vanishing_hut.bargain.accepted", witch.getDisplayName(),
                bargain.rewardCount(), bargain.reward().getDefaultInstance().getHoverName())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        level.sendParticles(ParticleTypes.WITCH,
                witch.getX(), witch.getY() + witch.getBbHeight() * 0.7D, witch.getZ(),
                18, 0.35D, 0.45D, 0.35D, 0.02D);
        level.playSound(null, witch.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 1.0F, 1.15F);
        return InteractionResult.CONSUME;
    }

    private static Optional<VanishingHutData.Entry> locate(ServerLevel level, CovenWitchEntity witch) {
        VanishingHutData data = VanishingHutData.get(level);
        Optional<VanishingHutData.Entry> byResident = data.byResident(witch.getUUID());
        if (byResident.isPresent()) {
            return byResident;
        }
        BlockPos home = witch.getHutHome();
        if (home == null) {
            return Optional.empty();
        }
        BlockPos anchor = VanishingHut.anchorFromResidentHome(home);
        if (!(level.getBlockEntity(anchor) instanceof VanishingHutAnchorBlockEntity hut)) {
            return Optional.empty();
        }
        data.upsert(hut.hutId(), level, anchor, witch.getUUID());
        return data.get(hut.hutId());
    }
}
