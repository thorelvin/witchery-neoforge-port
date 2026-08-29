package com.emoniph.witchery.entity;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Bounded activation-time specialty bonuses; this is not a second coven roster. */
public final class CovenSpecialtyContributions {
    public static final double PARTICIPATION_RADIUS = 16.0D;
    public static final int HEDGE_FERTILITY_RADIUS_BONUS = 1;
    public static final int HEDGE_FORESTATION_DURATION_BONUS = 2;
    public static final int PALE_MANIFESTATION_DURATION_BONUS = 10;
    public static final int STORM_STAGE_BONUS = 2;
    public static final float HEARTH_UPKEEP_REDUCTION = 0.1F;
    public static final int GRAVE_POWER_PERCENT = 95;
    private static final Identifier CONCORD_ADVANCEMENT =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "guide/coven_concord");

    private CovenSpecialtyContributions() {
    }

    public static CovenContributionCounter.SpecialtyRoster roster(
            ServerLevel level, BlockPos center, Player caster) {
        return CovenContributionCounter.livingSwornRoster(
                level, new AABB(center).inflate(PARTICIPATION_RADIUS), caster.getUUID());
    }

    public static void revealContribution(Player caster, ServerLevel level,
                                          CovenContributionCounter.SpecialtyRoster roster,
                                          CovenSpecialty specialty) {
        if (caster instanceof ServerPlayer serverPlayer) {
            revealContribution(serverPlayer, level, roster, specialty);
        }
    }

    public static int gravePower(int basePower, CovenContributionCounter.SpecialtyRoster roster) {
        return roster.has(CovenSpecialty.GRAVE_WITCH)
                ? Math.max(0, basePower * GRAVE_POWER_PERCENT / 100)
                : basePower;
    }

    public static float hearthUpkeep(float baseUpkeep, CovenContributionCounter.SpecialtyRoster roster) {
        return roster.has(CovenSpecialty.HEARTH_WITCH)
                ? Math.max(0.0F, baseUpkeep - HEARTH_UPKEEP_REDUCTION)
                : baseUpkeep;
    }

    public static void revealContribution(ServerPlayer caster, ServerLevel level,
                                          CovenContributionCounter.SpecialtyRoster roster,
                                          CovenSpecialty specialty) {
        CovenWitchEntity contributor = roster.first(specialty);
        if (contributor == null) {
            return;
        }
        level.sendParticles(new DustParticleOptions(specialty.color(), 0.8F),
                contributor.getX(), contributor.getY() + 1.2D, contributor.getZ(),
                14, 0.28D, 0.45D, 0.28D, 0.01D);
        caster.displayClientMessage(Component.translatable("witchery.coven.specialty.contribution",
                        contributor.getDisplayName(), specialty.displayName())
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        revealConcord(caster, level, roster);
    }

    private static void revealConcord(ServerPlayer caster, ServerLevel level,
                                      CovenContributionCounter.SpecialtyRoster roster) {
        if (roster.livingCount() < CovenWitchEntity.MAX_COVEN) {
            return;
        }
        for (CovenWitchEntity witch : roster.participants()) {
            level.sendParticles(new DustParticleOptions(witch.getSpecialty().color(), 0.7F),
                    witch.getX(), witch.getY() + 0.9D, witch.getZ(),
                    8, 0.2D, 0.32D, 0.2D, 0.0D);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 0.9F);
        caster.displayClientMessage(Component.translatable("witchery.coven.concord")
                .withStyle(ChatFormatting.GOLD), false);
        AdvancementHolder advancement = level.getServer().getAdvancements().get(CONCORD_ADVANCEMENT);
        if (advancement != null) {
            caster.getAdvancements().award(advancement, "concord");
        }
    }
}
