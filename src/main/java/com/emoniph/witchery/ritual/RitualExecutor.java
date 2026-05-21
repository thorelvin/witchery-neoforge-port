package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.events.PoppetEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.CircleTalismanItem;
import com.emoniph.witchery.item.SpectralStoneItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class RitualExecutor {
    private static final int VARIANT_OTHERWHERE = 2;
    private static final int VARIANT_INFERNAL = 3;
    private static final int VARIANT_RITUAL = 4;

    private static final int RITUAL_PULSE_INTERVAL = 30;
    private static final int VOID_BRAMBLE_NULL_RANGE = 32;
    private static final int VOID_BRAMBLE_NULL_RANGE_SQ = VOID_BRAMBLE_NULL_RANGE * VOID_BRAMBLE_NULL_RANGE;
    private static final double FAMILIAR_BIND_RADIUS = 7.0;
    private static final Map<ResourceKey<Level>, List<ActiveRitual>> ACTIVE_RITUALS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, ClimateChangePlan>> CLIMATE_CHANGE_PLANS = new HashMap<>();
    private static final ThreadLocal<ItemStack> PENDING_RITUAL_INGREDIENT = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    public enum ActivationResult {
        NO_MATCH(false, false),
        MATCHED_FAILED(true, false),
        SUCCESS(true, true),
        SUCCESS_NO_CONSUME(true, false);

        private final boolean matched;
        private final boolean consumeDroppedIngredient;

        ActivationResult(boolean matched, boolean consumeDroppedIngredient) {
            this.matched = matched;
            this.consumeDroppedIngredient = consumeDroppedIngredient;
        }

        public boolean matched() {
            return matched;
        }

        public boolean consumeDroppedIngredient() {
            return consumeDroppedIngredient;
        }
    }

    private enum ClimateFocus {
        FOREST,
        PLAINS,
        MOUNTAIN,
        HILLS,
        SWAMP,
        WATER,
        DESERT,
        FROZEN,
        JUNGLE,
        WASTELAND,
        BEACH,
        MUSHROOM,
        MAGICAL
    }

    private enum ClimateWeather {
        NONE,
        SUN,
        RAIN
    }

    private record ClimateChangePlan(Holder<Biome> biomeHolder, ClimateWeather weather) {}
    private record ClimateChangeScan(ClimateChangePlan plan, List<ItemEntity> itemsToConsume) {}

    private enum ActiveRitualType {
        WEATHER_CALL_STORM,
        ICE_SHELL,
        RAIN_OF_TOADS,
        BLINDNESS,
        BLIGHT,
        HELL_ON_EARTH,
        CALL_CREATURES,
        CLIMATE_CHANGE,
        NATURES_POWER,
        TRANSPOSE_ORES,
        MOVING_EARTH,
        RAISE_COLUMN,
        VOLCANO,
        PROTECTION_BARRIER_MONSTERS,
        PROTECTION_BARRIER_PORTABLE,
        PROTECTION_BARRIER,
        INFUSION_RECHARGE,
        SANCTITY_CIRCLE,
        IMPRISONMENT_CIRCLE
    }

    private static final class ActiveRitual {
        private final ActiveRitualType type;
        private final BlockPos center;
        private final BlockPos effectCenter;
        private final RitualCircleDetector.CircleSize circleSize;
        private final int circleVariant;
        private final int minRadius;
        private final int maxRadius;
        private final int targetStage;
        private final int pulseInterval;
        private final int covenSize;
        private final float upkeepCost;
        private final boolean enhanced;
        private final ItemStack sacrificedIngredient;
        private final List<ItemStack> supplementalSacrifices;
        private final List<BlockPos> path = new ArrayList<>();
        private final int heading;
        private int stage;
        private long nextGameTime;
        private float upkeepDebt;
        private boolean failedOnStop;
        private boolean refundOnStop;

        private ActiveRitual(ActiveRitualType type, BlockPos center, RitualCircleDetector.CircleSize circleSize,
                             BlockPos effectCenter, int circleVariant,
                             int minRadius, int maxRadius, int targetStage, int pulseInterval,
                             int covenSize, float upkeepCost, boolean enhanced, int heading, long nextGameTime,
                             ItemStack sacrificedIngredient, List<ItemStack> supplementalSacrifices) {
            this.type = type;
            this.center = center.immutable();
            this.effectCenter = effectCenter.immutable();
            this.circleSize = circleSize;
            this.circleVariant = circleVariant;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.targetStage = targetStage;
            this.pulseInterval = pulseInterval;
            this.covenSize = covenSize;
            this.upkeepCost = upkeepCost;
            this.enhanced = enhanced;
            this.heading = heading;
            this.nextGameTime = nextGameTime;
            this.sacrificedIngredient = sacrificedIngredient;
            this.supplementalSacrifices = supplementalSacrifices;
        }
    }

    /**
     * Dispatch the correct rite based on the complete rings present at the center
     * plus the dropped ingredient. Until the full recipe registry comes back, new
     * casts retry the present circles from outermost to innermost instead of
     * collapsing through the first detected ring only.
     */
    public static ActivationResult execute(Player player, Level level, BlockPos floorCenter,
                                           List<RitualCircleDetector.DetectionResult> presentCircles, ItemStack ingredient) {
        PENDING_RITUAL_INGREDIENT.set(ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.copyWithCount(1));
        try {
            if (isRitualAreaNulled(level, floorCenter)) {
                level.playSound(null, floorCenter, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
                return ActivationResult.MATCHED_FAILED;
            }

            if (isGlyphicTransformationChalk(ingredient)) {
                return matchedResult(riteOfGlyphicTransformation(player, level, floorCenter, ingredient));
            }

            if (ingredient.is(WitcheryItems.CIRCLE_TALISMAN.get()) && CircleTalismanItem.getCircleData(ingredient) == 0) {
                return matchedResult(riteOfBindCircleToTalisman(player, level, floorCenter));
            }

            for (RitualCircleDetector.DetectionResult result : presentCircles) {
                int circleVariant = RitualCircleDetector.detectVariant(level, floorCenter, result.size());
                ActivationResult activation = switch (result.size()) {
                    case SMALL -> dispatchSmall(player, level, floorCenter, ingredient, circleVariant);
                    case MEDIUM -> dispatchMedium(player, level, floorCenter, ingredient, circleVariant);
                    case LARGE -> dispatchLarge(player, level, floorCenter, ingredient, circleVariant);
                };
                if (activation.matched()) {
                    return activation;
                }
            }

            return ActivationResult.NO_MATCH;
        } finally {
            PENDING_RITUAL_INGREDIENT.remove();
        }
    }

    private static ActivationResult successResult() {
        return ActivationResult.SUCCESS;
    }

    private static ActivationResult successWithoutConsumption() {
        return ActivationResult.SUCCESS_NO_CONSUME;
    }

    private static ActivationResult matchedResult(boolean succeeded) {
        return succeeded ? ActivationResult.SUCCESS : ActivationResult.MATCHED_FAILED;
    }

    private static boolean hasVariant(int currentVariant, int requiredVariant) {
        return currentVariant == requiredVariant;
    }

    private static boolean hasCompanionRing(Level level, BlockPos center, RitualCircleDetector.CircleSize size, int requiredVariant) {
        return RitualCircleDetector.hasRing(level, center, size)
                && RitualCircleDetector.detectVariant(level, center, size) == requiredVariant;
    }

    private static boolean isGlyphicTransformationChalk(ItemStack stack) {
        return stack.is(WitcheryItems.CHALK.get())
                || stack.is(WitcheryItems.CHALK_OTHERWHERE.get())
                || stack.is(WitcheryItems.CHALK_INFERNAL.get());
    }

    private static boolean isPotion(ItemStack stack, Holder<Potion> potion) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(potion);
    }

    private static boolean isRitualAreaNulled(Level level, BlockPos center) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                center.offset(-VOID_BRAMBLE_NULL_RANGE, -VOID_BRAMBLE_NULL_RANGE, -VOID_BRAMBLE_NULL_RANGE),
                center.offset(VOID_BRAMBLE_NULL_RANGE, VOID_BRAMBLE_NULL_RANGE, VOID_BRAMBLE_NULL_RANGE))) {
            if (!level.getBlockState(checkPos).is(WitcheryBlocks.VOID_BRAMBLE.get())) {
                continue;
            }

            long dx = center.getX() - checkPos.getX();
            long dy = center.getY() - checkPos.getY();
            long dz = center.getZ() - checkPos.getZ();
            if (dx * dx + dy * dy + dz * dz < VOID_BRAMBLE_NULL_RANGE_SQ) {
                return true;
            }
        }
        return false;
    }

    // ── Small ring dispatch ────────────────────────────────────────────────────

    private static ActivationResult dispatchSmall(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        // Infusion brew bases take priority — each grants a specific infusion type
        if (item.is(WitcheryItems.GHOST_OF_THE_LIGHT.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL)) {
            riteOfInfusion(player, level, center, InfusionType.LIGHT);
            return successResult();
        } else if (item.is(WitcheryItems.SOUL_OF_THE_WORLD.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL)) {
            riteOfInfusion(player, level, center, InfusionType.OVERWORLD);
            return successResult();
        } else if (item.is(WitcheryItems.SPIRIT_OF_OTHERWHERE.get())
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_OTHERWHERE)) {
            riteOfInfusion(player, level, center, InfusionType.OTHERWHERE);
            return successResult();
        } else if (item.is(WitcheryItems.INFERNAL_ANIMUS.get())
                && hasVariant(circleVariant, VARIANT_INFERNAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_INFERNAL)) {
            riteOfInfusion(player, level, center, InfusionType.INFERNAL);
            return successResult();
        } else if ((item.is(Items.STONE_AXE) || item.is(Items.IRON_AXE))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfTotalEclipse(player, level, center, item));
        } else if (item.is(Items.BONE_MEAL) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            if (findPortableFertilityOfferings(level, center).isPresent()) {
                return matchedResult(riteOfPortableFertility(player, level, center));
            }
            return matchedResult(riteOfFertility(player, level, center));
        } else if (item.is(WitcheryItems.TAGLOCK_KIT.get())
                && TaglockKitItem.isFilled(item)
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && (findCureMisfortuneOfferings(level, center).isPresent()
                    || findCureCreatureOfferings(level, center).isPresent()
                    || findCureInsanityOfferings(level, center).isPresent()
                    || findCureSinkingOfferings(level, center).isPresent()
                    || findCureOverheatingOfferings(level, center).isPresent()
                    || findCureWakingNightmareOfferings(level, center).isPresent())) {
            return matchedResult(riteOfBreathOfTheGoddessCure(player, level, center));
        } else if (item.is(Items.BLAZE_POWDER) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfBlindness(player, level, center));
        } else if (item.is(WitcheryItems.DIAMOND_VAPOUR.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfSummonWitch(player, level, center));
        } else if (item.is(WitcheryItems.DROP_OF_LUCK.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfSummonFamiliar(player, level, center));
        } else if (item.is(WitcheryItems.WAYSTONE.get())
                && WaystoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBindWaystoneCopy(player, level, center, item));
        } else if (item.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBanishDemon(player, level, center, true));
        } else if (item.is(WitcheryItems.ATTUNED_STONE.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBanishDemon(player, level, center, false));
        } else if (item.is(WitcheryItems.WAYSTONE.get())
                && !WaystoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBindWaystone(player, level, center));
        } else if (item.is(WitcheryItems.WAYSTONE.get())
                && WaystoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return matchedResult(riteOfTranspositionToWaystone(player, level, center, item));
        } else if (item.is(WitcheryItems.SPECTRAL_DUST.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfNecromancy(player, level, center));
        } else if (item.is(WitcheryItems.SPECTRAL_DUST.get()) && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return matchedResult(riteOfManifestationDream(player, level, center));
        } else if (item.is(Items.ROTTEN_FLESH) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfPriorIncarnation(player, level, center));
        } else if (item.is(WitcheryItems.FROZEN_HEART.get())) {
            return matchedResult(riteOfIceShell(player, level, center));
        } else if (item.is(Items.SNOWBALL) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfWeatherCallStorm(player, level, center, StormRiteVariant.BASE));
        } else if (item.is(Items.WATER_BUCKET) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfWeatherCallStorm(player, level, center, StormRiteVariant.LARGE));
        } else if (item.is(Items.GUNPOWDER) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfWeatherCallStorm(player, level, center, StormRiteVariant.PORTABLE));
        } else if (item.is(Items.FEATHER) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfSanctity(player, level, center));
        } else if (item.is(Items.SLIME_BALL) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfImprisonment(player, level, center));
        } else if (item.is(Items.OBSIDIAN.asItem()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfProtectionBarrier(player, level, center,
                    ActiveRitualType.PROTECTION_BARRIER_MONSTERS,
                    RitualCircleDetector.CircleSize.SMALL, 4, 5));
        } else if (item.is(Items.SUGAR) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfCookItem(player, level, center));
        } else if ((item.is(Items.COBBLESTONE) || item.is(Items.STONE))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.SMALL));
        } else if (item.is(WitcheryItems.QUARTZ_SPHERE.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfInfuseSeerStone(player, level, center));
        } else {
            return ActivationResult.NO_MATCH;
        }
    }

    // ── Medium ring dispatch ───────────────────────────────────────────────────

    private static ActivationResult dispatchMedium(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        if (item.is(WitcheryItems.WHIFF_OF_MAGIC.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfWhiffOfMagicFamiliar(player, level, center));
        } else if ((item.is(WitcheryItems.TEAR_OF_GODDESS.get())
                || item.is(WitcheryItems.ODOUR_OF_PURITY.get()))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBindFamiliar(player, level, center));
        } else if ((item.is(WitcheryItems.BREATH_OF_GODDESS.get())
                || item.is(WitcheryItems.HINT_OF_REBIRTH.get()))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfCallFamiliar(player, level, center));
        } else if (item.is(ItemTags.SAPLINGS) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfTheForest(player, level, center, item));
        } else if (isPotion(item, Potions.REGENERATION)
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)) {
            return matchedResult(riteOfInfusionRecharge(player, level, center));
        } else if (item.is(WitcheryItems.ATTUNED_STONE.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)) {
            return matchedResult(riteOfChargeStone(player, level, center));
        } else if (item.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfProtectionBarrier(player, level, center,
                    ActiveRitualType.PROTECTION_BARRIER_PORTABLE,
                    RitualCircleDetector.CircleSize.MEDIUM, 6, 4));
        } else if ((item.is(Items.OBSIDIAN) || item.is(Items.CRYING_OBSIDIAN))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfProtection(player, level, center));
        } else if (item.is(WitcheryItems.FLYING_OINTMENT.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)) {
            return matchedResult(riteOfEnchantBroom(player, level, center));
        } else if (item.is(WitcheryItems.ENT_TWIG.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)) {
            return matchedResult(riteOfInfuseMysticBranch(player, level, center));
        } else if (item.is(WitcheryItems.QUARTZ_SPHERE.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            if (findCrystalBallOfferings(level, center).isEmpty()
                    && findSeerStoneOfferings(level, center).isPresent()
                    && RitualCircleDetector.hasRing(level, center, RitualCircleDetector.CircleSize.SMALL)
                    && RitualCircleDetector.detectVariant(level, center, RitualCircleDetector.CircleSize.SMALL) == VARIANT_RITUAL) {
                return ActivationResult.NO_MATCH;
            }
            return matchedResult(riteOfInfuseCrystalBall(player, level, center));
        } else if (item.is(WitcheryItems.INFUSED_BREW_BASE.get())
                && hasVariant(circleVariant, VARIANT_RITUAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)) {
            return matchedResult(riteOfInfuseBrew(player, level, center));
        } else if ((item.is(Items.LILY_PAD) || item.is(Items.FROGSPAWN))
                && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfRainOfToads(player, level, center));
        } else if ((item.is(Items.COBBLESTONE) || item.is(Items.STONE))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.MEDIUM));
        } else if (item.is(WitcheryItems.BREW_OF_SPROUTING.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfNaturesPower(player, level, center));
        } else if (item.is(Items.WITHER_ROSE) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfBlight(player, level, center));
        } else if (item.is(WitcheryItems.VOODOO_PROTECTION_POPPET.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfCorruptPoppet(player, level, center));
        } else if (item.is(WitcheryItems.TAGLOCK_KIT.get())
                && TaglockKitItem.isFilled(item)
                && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            if (findCurseCreatureOfferings(level, center).isPresent()) {
                return matchedResult(riteOfCurseCreature(player, level, center));
            }
            if (findCurseSinkingOfferings(level, center).isPresent()) {
                return matchedResult(riteOfCurseSinking(player, level, center));
            }
            if (findCurseOverheatingOfferings(level, center).isPresent()) {
                return matchedResult(riteOfCurseOverheating(player, level, center));
            }
            if (findCurseInsanityOfferings(level, center).isPresent()) {
                return matchedResult(riteOfCurseInsanity(player, level, center));
            }
            return ActivationResult.NO_MATCH;
        } else if (item.is(WitcheryItems.TAGLOCK_KIT.get())
                && TaglockKitItem.isFilled(item)
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return matchedResult(riteOfTranspositionEntity(player, level, center, item));
        } else if (item.is(WitcheryItems.SPECTRAL_STONE.get())
                && !SpectralStoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBindSpectralStone(player, level, center, item));
        } else if (isFetishItem(item) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfBindSpiritsToFetish(player, level, center, item));
        } else {
            return ActivationResult.NO_MATCH;
        }
    }

    // ── Large ring dispatch ────────────────────────────────────────────────────

    private static ActivationResult dispatchLarge(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        if (isRawFood(item, level) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            riteOfBroiling(player, level, center);
            return successResult();
        } else if (isBeastialCallTrigger(item)
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return matchedResult(riteOfBeastialCall(player, level, center));
        } else if (item.is(WitcheryItems.DIAMOND_VAPOUR.get()) && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return matchedResult(riteOfTransposeOres(player, level, center));
        } else if ((item.is(Items.DIRT) || item.is(Items.GRAVEL) || item.is(Items.SAND))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfMovingEarth(player, level, center));
        } else if (item.is(Items.LAVA_BUCKET) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfVolcano(player, level, center));
        } else if ((item.is(Items.FLINT) || item.is(Items.FLINT_AND_STEEL))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfBrokenEarth(player, level, center);
            return successResult();
        } else if ((item.is(Items.COBBLESTONE) || item.is(Items.STONE))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.LARGE));
        } else if (item.is(Items.NETHER_STAR)
                && hasVariant(circleVariant, VARIANT_INFERNAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_OTHERWHERE)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_INFERNAL)) {
            return matchedResult(riteOfCurseHellOnEarth(player, level, center));
        } else if (item.is(Items.STRING) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfCurseWakingNightmare(player, level, center));
        } else if (item.is(WitcheryItems.ATTUNED_STONE.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return matchedResult(riteOfClimateChange(player, level, center));
        } else if (item.is(WitcheryItems.REFINED_EVIL.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfSummonImp(player, level, center));
        } else if (item.is(WitcheryItems.TORMENTED_TWINE.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return matchedResult(riteOfSummonDemon(player, level, center));
        } else if (item.is(Items.WITHER_SKELETON_SKULL)
                && hasVariant(circleVariant, VARIANT_INFERNAL)
                && hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_INFERNAL)) {
            return matchedResult(riteOfSummonWither(player, level, center));
        } else if (item.is(WitcheryItems.TORMENT_SCROLL.get())) {
            return matchedResult(riteOfSummoningLordOfTorment(player, level, center));
        } else {
            return ActivationResult.NO_MATCH;
        }
    }

    private static boolean isFetishItem(ItemStack item) {
        return item.is(WitcheryItems.FETISH_SCARECROW.get())
            || item.is(WitcheryItems.FETISH_TRENT_EFFIGY.get())
            || item.is(WitcheryItems.FETISH_WITCHS_LADDER.get());
    }

    private static boolean isRawFood(ItemStack stack, Level level) {
        if (stack.isEmpty()) return false;
        return ((net.minecraft.server.level.ServerLevel) level).getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SMALL rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Infusion — applies a typed infusion to the player from a brew base. */
    private static void riteOfInfusion(Player player, Level level, BlockPos center, InfusionType type) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(level instanceof ServerLevel sl)) return;

        int startupCost = switch (type) {
            case LIGHT -> 2000;
            case OVERWORLD, OTHERWHERE, INFERNAL -> 4000;
            default -> 0;
        };
        if (startupCost > 0 && !consumeNearbyRitualAltarPower(sl, center, 16, startupCost)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.infusion.no_power"), false);
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setInfusionType(type);
        data.setInfusionEnergy(200, 200);

        String msgKey = switch (type) {
            case LIGHT      -> "witchery.ritual.infusion.light.success";
            case OVERWORLD  -> "witchery.ritual.infusion.overworld.success";
            case OTHERWHERE -> "witchery.ritual.infusion.otherwhere.success";
            case INFERNAL   -> "witchery.ritual.infusion.infernal.success";
            default         -> "witchery.ritual.infusion.success";
        };

        PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, data));
        success(player, level, center, msgKey, SoundEvents.BEACON_ACTIVATE, 1.2f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Infusion ({}): {} is now infused.", type, sp.getName().getString());
    }

    /** Rite of Bind Familiar — binds nearby tamed familiar-capable creatures instead of spawning a new one. */
    private static boolean riteOfBindFamiliar(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<BindFamiliarOfferings> offerings = findBindFamiliarOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_familiar.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 8000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_familiar.no_power"), false);
            return false;
        }

        AABB playerBounds = new AABB(center).inflate(FAMILIAR_BIND_RADIUS, 1.0, FAMILIAR_BIND_RADIUS);
        List<ServerPlayer> nearbyPlayers = sl.getEntitiesOfClass(ServerPlayer.class, playerBounds);
        boolean boundAny = false;

        for (ServerPlayer nearbyPlayer : nearbyPlayers) {
            if (nearbyPlayer.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)
                    > FAMILIAR_BIND_RADIUS * FAMILIAR_BIND_RADIUS) {
                continue;
            }

            TamableAnimal candidate = findBindableFamiliarCandidate(nearbyPlayer, sl, center, FAMILIAR_BIND_RADIUS);
            if (candidate == null) {
                continue;
            }

            TamableAnimal familiar = bindFamiliarToPlayer(nearbyPlayer, sl, candidate);
            if (familiar == null) {
                continue;
            }

            familiar.setOrderedToSit(false);
            sl.sendParticles(ParticleTypes.HEART,
                    familiar.getX(), familiar.getY() + 0.8, familiar.getZ(),
                    6, 0.25, 0.25, 0.25, 0.02);
            boundAny = true;
        }

        if (!boundAny) {
            return false;
        }

        consumeOfferedItem(offerings.get().tearOfTheGoddess());
        consumeOfferedItem(offerings.get().odourOfPurity());
        consumeOfferedItem(offerings.get().whiffOfMagic());
        consumeOfferedItem(offerings.get().diamond());
        consumeOfferedItem(offerings.get().demonicBlood());

        success(player, level, center, "witchery.ritual.bind_familiar.success",
                SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1, 30);
        WitcheryMod.LOGGER.info("Bound familiar(s) near {}", center);
        return true;
    }

    /**
     * Shared `Whiff of Magic` familiar trigger — bind when a valid familiar candidate is present,
     * otherwise fall through to calling the already bound familiar.
     */
    private static boolean riteOfWhiffOfMagicFamiliar(Player player, Level level, BlockPos center) {
        if (level instanceof ServerLevel sl && hasBindableFamiliarCandidate(sl, center, FAMILIAR_BIND_RADIUS)) {
            return riteOfBindFamiliar(player, level, center);
        }
        return riteOfCallFamiliar(player, level, center);
    }

    /** Rite of Summon Familiar — restores the original small-circle Drop of Luck path as a bound witch's cat summon. */
    private static boolean riteOfSummonFamiliar(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SummonFamiliarOfferings> offerings = findSummonFamiliarOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_familiar.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_familiar.no_power"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var familiar = WitcheryEntities.FAMILIAR_CAT.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (familiar == null) {
            return false;
        }

        familiar.snapTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, player.getYRot(), 0.0F);
        familiar.setOrderedToSit(false);
        familiar.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(familiar);

        TamableAnimal bound = bindFamiliarToPlayer(sp, sl, familiar);
        if (bound == null) {
            familiar.discard();
            return false;
        }

        consumeOfferedItem(offerings.get().egg());
        consumeOfferedItem(offerings.get().goldIngot());
        consumeOfferedItem(offerings.get().arthana());

        success(player, level, center, "witchery.ritual.summon_familiar.success",
                SoundEvents.CAT_AMBIENT, 1.0f, 2, 24);
        WitcheryMod.LOGGER.info("Rite of Summon Familiar cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Total Eclipse — forces midnight on the server. */
    private static boolean riteOfTotalEclipse(Player player, Level level, BlockPos center, ItemStack trigger) {
        if (!requireDay(player, level, center)) {
            return false;
        }
        boolean portable = trigger.is(Items.IRON_AXE);
        Optional<TotalEclipseOfferings> offerings = findTotalEclipseOfferings(level, center, portable);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.eclipse.support"), false);
            return false;
        }
        if (!portable) {
            if (!(level instanceof ServerLevel sl) || !consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.ritual.eclipse.no_power"), false);
                return false;
            }
        }
        if (level instanceof ServerLevel sl) {
            consumeOfferedItem(offerings.get().quicklime());
            if (portable && offerings.get().chargedStone() != null) {
                ItemStack returnedStone = offerings.get().chargedStone().getItem()
                        .copyWithCount(1)
                        .transmuteCopy(WitcheryItems.ATTUNED_STONE.get());
                consumeOfferedItem(offerings.get().chargedStone());
                ItemEntity entity = new ItemEntity(sl,
                        center.getX() + 0.5, center.getY() + 1.05, center.getZ() + 0.5,
                        returnedStone);
                entity.setDefaultPickUpDelay();
                sl.addFreshEntity(entity);
            }
            sl.setDayTime(18000L); // midnight
        }
        success(player, level, center, "witchery.ritual.eclipse.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1, 24);
        WitcheryMod.LOGGER.info("Rite of Total Eclipse cast at {} by {}", center, player.getName().getString());
        return true;
    }

    /** Rite of Beastial Call — restores the staged coven-gated creature-allure loop instead of a one-shot teleport. */
    private static boolean riteOfBeastialCall(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 3)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 6000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.beastial_call.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.CALL_CREATURES, RitualCircleDetector.CircleSize.LARGE,
                center, 0, 0, 250, 60)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.beastial_call.started",
                SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 3, 72);
        return true;
    }

    private static boolean isBeastialCallTrigger(ItemStack item) {
        return item.is(Items.MILK_BUCKET)
                || item.is(Blocks.HAY_BLOCK.asItem())
                || item.is(Items.APPLE)
                || item.is(Items.BEEF)
                || item.is(Items.COD)
                || item.is(Blocks.RED_MUSHROOM.asItem())
                || item.is(Items.CARROT)
                || item.is(Items.WHEAT_SEEDS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Fertility — bone-meals all crops in 10-block radius; cures zombie villagers. */
    private static boolean riteOfFertility(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<FertilityOfferings> offerings = findFertilityOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.fertility.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.fertility.no_power"), false);
            return false;
        }
        int grown = 0;
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-10, -3, -10), center.offset(10, 3, 10))) {
            BlockState s = sl.getBlockState(bp);
            if (s.getBlock() instanceof BonemealableBlock bm
                    && bm.isValidBonemealTarget(sl, bp, s)) {
                if (bm.isBonemealSuccess(sl, sl.random, bp, s)) {
                    bm.performBonemeal(sl, sl.random, bp, s);
                    grown++;
                }
            }
        }

        // Cure zombie villagers: apply Weakness so they can be cured by golden apple
        int cured = 0;
        for (ZombieVillager zv : sl.getEntitiesOfClass(ZombieVillager.class,
                new AABB(center).inflate(15))) {
            zv.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3600, 0));
            // Spawn a golden apple near them so players can complete the cure
            ItemEntity apple = new ItemEntity(sl,
                    zv.getX(), zv.getY() + 0.5, zv.getZ(),
                    new ItemStack(Items.GOLDEN_APPLE));
            sl.addFreshEntity(apple);
            cured++;
        }

        consumeOfferedItem(offerings.get().hintOfRebirth());
        consumeOfferedItem(offerings.get().diamondVapour());
        consumeOfferedItem(offerings.get().quicklime());
        consumeOfferedItem(offerings.get().gypsum());
        consumeOfferedItem(offerings.get().mutandis());

        player.displayClientMessage(Component.translatable("witchery.ritual.fertility.success", grown, cured), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Fertility: grew {}, cured {} zombie villagers", grown, cured);
        return true;
    }

    /** Portable Rite of Fertility — the charged-stone / Mutandis Extremis variant without altar-power drain. */
    private static boolean riteOfPortableFertility(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<PortableFertilityOfferings> offerings = findPortableFertilityOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.fertility.portable_support"), false);
            return false;
        }

        int grown = 0;
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-10, -3, -10), center.offset(10, 3, 10))) {
            BlockState s = sl.getBlockState(bp);
            if (s.getBlock() instanceof BonemealableBlock bm
                    && bm.isValidBonemealTarget(sl, bp, s)) {
                if (bm.isBonemealSuccess(sl, sl.random, bp, s)) {
                    bm.performBonemeal(sl, sl.random, bp, s);
                    grown++;
                }
            }
        }

        int cured = 0;
        for (ZombieVillager zv : sl.getEntitiesOfClass(ZombieVillager.class,
                new AABB(center).inflate(15))) {
            zv.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3600, 0));
            ItemEntity apple = new ItemEntity(sl,
                    zv.getX(), zv.getY() + 0.5, zv.getZ(),
                    new ItemStack(Items.GOLDEN_APPLE));
            sl.addFreshEntity(apple);
            cured++;
        }

        consumeOfferedItem(offerings.get().hintOfRebirth());
        consumeOfferedItem(offerings.get().diamondVapour());
        consumeOfferedItem(offerings.get().quicklime());
        consumeOfferedItem(offerings.get().gypsum());
        consumeOfferedItem(offerings.get().mutandisExtremis());
        consumeOfferedItem(offerings.get().chargedStone());

        player.displayClientMessage(Component.translatable("witchery.ritual.fertility.success", grown, cured), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 2, 48);
        WitcheryMod.LOGGER.info("Portable Rite of Fertility: grew {}, cured {} zombie villagers", grown, cured);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LARGE rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of the Forest — seeds and grows a grove of the offered sapling type around the circle. */
    private static boolean riteOfTheForest(Player player, Level level, BlockPos center, ItemStack triggerSapling) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!(triggerSapling.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SaplingBlock saplingBlock)) {
            return false;
        }

        Optional<ForestationOfferings> offerings = findForestationOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.forest.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 4000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.forest.no_power"), false);
            return false;
        }

        BlockState saplingState = saplingBlock.defaultBlockState();
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        BlockPos effectCenter = relocation.effectCenter();
        int radius = 20 + covenSize * 2;
        int attempts = 60 + covenSize * 5;
        int grown = 0;
        for (int i = 0; i < attempts; i++) {
            BlockPos ground = findForestationAnchor(sl, effectCenter, radius, 8);
            if (ground == null) {
                continue;
            }
            if (seedForestationTree(sl, ground.above(), saplingState)) {
                grown++;
            }
        }

        consumeOfferedItem(offerings.get().wickerBundle());
        consumeOfferedItem(offerings.get().brewOfSprouting());
        consumeOfferedItem(offerings.get().entTwig());
        if (relocation.boundWaystone() != null) {
            consumeOfferedItem(relocation.boundWaystone());
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.forest.success", grown), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of the Forest seeded {} {} grove attempts at {} for {}",
                grown, triggerSapling.getHoverName().getString(), effectCenter, player.getName().getString());
        return true;
    }

    /** Rite of Charge Stone — infuses an Attuned Stone into its charged variant on paired ritual circles. */
    private static boolean riteOfChargeStone(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ChargeStoneOfferings> offerings = findChargeStoneOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.charge_stone.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.charge_stone.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().glowstone());
        consumeOfferedItem(offerings.get().redstone());
        consumeOfferedItem(offerings.get().woodAsh());
        consumeOfferedItem(offerings.get().quicklime());
        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5,
                center.getY() + 1.5,
                center.getZ() + 0.5,
                new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        entity.setDeltaMovement(0.0, 0.3, 0.0);
        level.addFreshEntity(entity);

        success(player, level, center, "witchery.ritual.charge_stone.success",
                SoundEvents.BEACON_ACTIVATE, 1f, 2, 32);
        WitcheryMod.LOGGER.info("Charged Attuned Stone created at {} for {}", center, player.getName().getString());
        return true;
    }

    /** Rite of Infusion Recharge — sustains a charging field for infused players standing in paired ritual circles. */
    private static boolean riteOfInfusionRecharge(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 4000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_recharge.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.INFUSION_RECHARGE, RitualCircleDetector.CircleSize.MEDIUM,
                center, 4, 4, 0, 20, 0, 40.0f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        success(player, level, center, "witchery.ritual.infusion_recharge.success",
                SoundEvents.BEACON_ACTIVATE, 1f, 2, 40);
        WitcheryMod.LOGGER.info("Infusion recharge rite activated at {} by {}", center, player.getName().getString());
        return true;
    }

    /** Rite of Broiling — cooks all raw food item entities in 10-block radius. */
    private static void riteOfBroiling(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        int cooked = 0;
        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(10))) {
            var recipe = sl.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(ie.getItem()), sl);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(ie.getItem()), sl.registryAccess());
                ie.setItem(result.copyWithCount(ie.getItem().getCount()));
                cooked++;
            }
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.broiling.success", cooked), false);
        success(player, level, center, null, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 1f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Broiling cooked {} item stacks", cooked);
    }

    /** Rite of Protection Barrier (large) — sustains the broader monster barrier from the medium ritual ring. */
    private static boolean riteOfProtection(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ItemEntity> glowstone = findNearbyGlowstoneOffering(level, center);
        if (glowstone.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.glowstone_support"), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 1000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, ActiveRitualType.PROTECTION_BARRIER, RitualCircleDetector.CircleSize.MEDIUM,
                center, relocation.effectCenter(), 6, 6, 0, 20, 0, 1.4f, 0, supplementalSacrifices)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeOfferedItem(glowstone.get());
        if (relocation.boundWaystone() != null) {
            consumeOfferedItem(relocation.boundWaystone());
        }
        success(player, level, center, "witchery.ritual.protection.activated",
                SoundEvents.BEACON_ACTIVATE, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Protection barrier raised at {} by {}", center, player.getName().getString());
        return true;
    }

    private static boolean riteOfProtectionBarrier(Player player, Level level, BlockPos center,
                                                   ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                                   int radius, int height) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ItemEntity> supportOffering = switch (type) {
            case PROTECTION_BARRIER_MONSTERS -> findNearbyRedstoneOffering(level, center);
            case PROTECTION_BARRIER_PORTABLE -> findNearbyObsidianOffering(level, center);
            default -> Optional.empty();
        };
        String supportKey = switch (type) {
            case PROTECTION_BARRIER_MONSTERS -> "witchery.ritual.protection.redstone_support";
            case PROTECTION_BARRIER_PORTABLE -> "witchery.ritual.protection.obsidian_support";
            default -> null;
        };
        if (supportKey != null && supportOffering.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable(supportKey), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS
                ? findOptionalWaystoneRelocation(level, center, covenSize)
                : new OptionalWaystoneRelocation(null, center, false);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }

        int targetStage = type == ActiveRitualType.PROTECTION_BARRIER_PORTABLE ? 60 : 0;
        int startupPower = switch (type) {
            case PROTECTION_BARRIER_MONSTERS -> 500;
            default -> 0;
        };
        float upkeepCost = switch (type) {
            case PROTECTION_BARRIER_MONSTERS -> 1.2f;
            case PROTECTION_BARRIER_PORTABLE -> 0.0f;
            default -> 0.0f;
        };
        if (startupPower > 0 && !consumeNearbyRitualAltarPower(sl, center, 16, startupPower)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, type, circleSize, center, relocation.effectCenter(),
                radius, height, targetStage, 20, 0, upkeepCost, 0, supplementalSacrifices)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        supportOffering.ifPresent(RitualExecutor::consumeOfferedItem);
        if (relocation.boundWaystone() != null) {
            consumeOfferedItem(relocation.boundWaystone());
        }
        success(player, level, center, "witchery.ritual.protection.activated",
                SoundEvents.BEACON_ACTIVATE, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Protection variant {} raised at {} by {}", type, center, player.getName().getString());
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // M21 — SMALL rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Prior Incarnation — summons dropped items near the caster's last death spot. */
    private static boolean riteOfPriorIncarnation(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        Optional<PriorIncarnationOfferings> offerings = findPriorIncarnationOfferings(level, center);
        if (offerings.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.support"), false);
            return false;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        Optional<BlockPos> deathOpt = data.getLastDeathPos();
        if (deathOpt.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.no_death"), false);
            return false;
        }
        BlockPos dp = deathOpt.get();
        if (!data.getLastDeathDimension().equals(level.dimension().identifier().toString())) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.wrong_dim"), false);
            return false;
        }
        if (center.distSqr(dp) > 256) { // must be within 16 blocks of death point
            sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.too_far"), false);
            return false;
        }
        // Pull item entities near death point into the player's inventory
        AABB deathArea = new AABB(dp).inflate(5);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, deathArea);
        int recalled = 0;
        for (ItemEntity ie : items) {
            if (!ie.isAlive() || ie.hasPickUpDelay()) continue;
            ItemStack stack = ie.getItem().copy();
            if (sp.getInventory().add(stack)) {
                ie.discard();
            } else {
                ie.teleportTo(sp.getX(), sp.getY(), sp.getZ()); // bring to player if inventory full
            }
            recalled++;
        }
        consumeOfferedItem(offerings.get().dogTongue());
        consumeOfferedItem(offerings.get().bone());
        consumeOfferedItem(offerings.get().spectralDust());
        ItemStack returnedStone = offerings.get().chargedStone().getItem()
                .copyWithCount(1)
                .transmuteCopy(WitcheryItems.ATTUNED_STONE.get());
        consumeOfferedItem(offerings.get().chargedStone());
        ItemEntity stoneEntity = new ItemEntity(level, center.getX() + 0.5, center.getY() + 1.05, center.getZ() + 0.5, returnedStone);
        stoneEntity.setDefaultPickUpDelay();
        level.addFreshEntity(stoneEntity);
        sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.success", recalled), false);
        success(player, level, center, null, SoundEvents.ENDERMAN_TELEPORT, 1f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Prior Incarnation: recalled {} stacks for {}", recalled, sp.getName().getString());
        return true;
    }

    /** Rite of Banish Demon — expels infernal beings within 9 blocks of the circle. */
    private static boolean riteOfBanishDemon(Player player, Level level, BlockPos center, boolean portable) {
        final int radius = 9;
        if (!(level instanceof ServerLevel sl)) return false;
        if (!portable && !consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.banish_demon.no_power"), false);
            return false;
        }

        level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.4f);

        AABB bounds = new AABB(center).inflate(radius);
        int banished = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (!(entity instanceof DemonEntity
                    || entity instanceof DeathEntity
                    || entity instanceof LordOfTormentEntity
                    || entity instanceof FlameImpEntity)) {
                continue;
            }
            if (entity.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) > radius * radius) {
                continue;
            }
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    16, 0.5, 0.5, 0.5, 0.02);
            entity.discard();
            banished++;
        }

        success(player, level, center, "witchery.ritual.banish_demon.success",
                SoundEvents.FIRE_EXTINGUISH, 1.1f, 2, 24);
        WitcheryMod.LOGGER.info("Rite of Banish Demon ({}) banished {} infernal entity(s) around {}",
                portable ? "portable" : "altar", banished, center);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // M21 — MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Sanctity — sustains a repulsive protection circle around the rite. */
    private static boolean riteOfSanctity(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ItemEntity> redstone = findNearbyRedstoneOffering(level, center);
        if (redstone.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.redstone_support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 500)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.SANCTITY_CIRCLE, RitualCircleDetector.CircleSize.SMALL, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        consumeOfferedItem(redstone.get());
        success(player, level, center, "witchery.ritual.sanctity.activated",
                SoundEvents.BEACON_ACTIVATE, 1.2f, 2, 48);
        return true;
    }

    /** Rite of Imprisonment — sustains an attractive circle that keeps creatures penned in. */
    private static boolean riteOfImprisonment(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ItemEntity> redstone = findNearbyRedstoneOffering(level, center);
        if (redstone.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.redstone_support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 500)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.IMPRISONMENT_CIRCLE, RitualCircleDetector.CircleSize.SMALL, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        consumeOfferedItem(redstone.get());
        success(player, level, center, "witchery.ritual.imprisonment.activated",
                SoundEvents.CHAIN_PLACE, 1f, 2, 48);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // M21 — LARGE rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Moving Earth — parts the earth in a staged trench, closer to the old Part Earth rite. */
    private static boolean riteOfMovingEarth(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!queueActiveRitual(sl, ActiveRitualType.MOVING_EARTH, RitualCircleDetector.CircleSize.LARGE,
                center, 1, 10, 60, 1, countNearbyCovenWitches(level, center), headingFromLook(player.getLookAngle()))) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.moving_earth.success",
                SoundEvents.ROOTED_DIRT_BREAK, 0.8f, 3, 72);
        return true;
    }

    /** Rite of Broken Earth — digs a 3-wide trench 20 blocks in the player's facing direction. */
    private static void riteOfBrokenEarth(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        var look = player.getLookAngle();
        int dx, dz;
        if (Math.abs(look.x) >= Math.abs(look.z)) {
            dx = look.x > 0 ? 1 : -1;
            dz = 0;
        } else {
            dx = 0;
            dz = look.z > 0 ? 1 : -1;
        }
        int removed = 0;
        for (int dist = 0; dist < 20; dist++) {
            for (int side = -1; side <= 1; side++) {
                int bx = center.getX() + dx * dist + (dx == 0 ? side : 0);
                int bz = center.getZ() + dz * dist + (dz == 0 ? side : 0);
                for (int dy = 0; dy > -5; dy--) {
                    BlockPos bp = new BlockPos(bx, center.getY() + dy, bz);
                    BlockState bs = sl.getBlockState(bp);
                    if (!bs.isAir() && !bs.liquid() && bs.getDestroySpeed(sl, bp) >= 0) {
                        sl.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
                        removed++;
                    }
                }
            }
        }
        success(player, level, center, "witchery.ritual.broken_earth.success",
                SoundEvents.ROOTED_DIRT_BREAK, 0.6f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Broken Earth removed {} blocks", removed);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — SMALL rites
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — LARGE rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Manifestation — grants a Spirit World walker time to manifest in the Overworld as a ghost. */
    /** Rite of Infusion (Flying Ointment) — enchants the nearest broom from the medium ritual ring when the inner small ritual ring is also present. */
    private static boolean riteOfEnchantBroom(Player player, Level level, BlockPos center) {
        if (!requireNight(player, level, center)) {
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        AABB search = new AABB(center).inflate(5);
        List<com.emoniph.witchery.entity.BroomEntity> brooms =
                sl.getEntitiesOfClass(com.emoniph.witchery.entity.BroomEntity.class, search);
        if (brooms.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("witchery.ritual.enchant_broom.no_broom"), false);
            return false;
        }
        com.emoniph.witchery.entity.BroomEntity broom = brooms.get(0);
        if (broom.isEnchanted()) {
            if (player instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.translatable("witchery.ritual.enchant_broom.already"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.enchant_broom.no_power"), false);
            return false;
        }
        broom.setEnchanted(true);
        success(player, level, center, "witchery.ritual.enchant_broom.success",
                SoundEvents.BEACON_POWER_SELECT, 1f, 3, 48);
        WitcheryMod.LOGGER.info("Broom at {} enchanted by {}", broom.blockPosition(), player.getName().getString());
        return true;
    }

    private static boolean riteOfManifestationDream(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        if (!data.isInSpiritWorld() || !level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.manifestation.unavailable"), false);
            return false;
        }
        Optional<ManifestationOfferings> offerings = findManifestationOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.manifestation.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl) || !consumeNearbyRitualAltarPower(sl, center, 16, 5000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.manifestation.no_power"), false);
            return false;
        }

        int covenSize = countNearbyCovenWitches(level, center);
        int duration = 150 + covenSize * 25;
        consumeOfferedItem(offerings.get().spectralDust());
        consumeOfferedItem(offerings.get().mellifluousHunger());
        consumeOfferedItem(offerings.get().goldenPickaxe());
        consumeOfferedItem(offerings.get().gunpowder());
        data.setManifestationTime(duration);
        data.setSkipManifestationTick(false);
        sp.displayClientMessage(Component.translatable("witchery.ritual.manifestation.duration", duration), false);
        success(player, level, center, "witchery.ritual.manifestation.success",
                SoundEvents.ENDERMAN_TELEPORT, 1.8f, 3, 60);
        WitcheryMod.LOGGER.info("Rite of Manifestation: {} prepared {} seconds of ghost travel.", sp.getName().getString(), duration);
        return true;
    }

    /** Rite of Shifting Seasons - rewrites nearby biome data based on dropped foci and glowstone intensity. */
    private static boolean riteOfClimateChange(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        if (!WitcheryConfig.ALLOW_BIOME_CHANGING.get()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.disabled"), false);
            return false;
        }
        if (!canPerformClimateRite(player, level, center)) {
            return false;
        }

        ClimateChangeScan scan = scanClimateChangePlan(sl, center);
        if (scan == null) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.missingbiomefoci"), false);
            return false;
        }

        int covenSize = countNearbyCovenWitches(level, center);
        int radius = 16 * (covenSize - 3);
        storeClimateChangePlan(sl.dimension(), center, scan.plan());
        List<ItemStack> supplementalSacrifices = copyConsumedItemStacks(scan.itemsToConsume());
        if (!queueActiveRitual(sl, ActiveRitualType.CLIMATE_CHANGE, RitualCircleDetector.CircleSize.LARGE,
                center, radius, 0, 5, 20, covenSize, 0.0f, 0, supplementalSacrifices)) {
            clearClimateChangePlan(sl.dimension(), center);
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeClimateChangeItems(sl, scan.itemsToConsume());
        success(player, level, center, "witchery.ritual.climatechange.success",
                SoundEvents.BEACON_ACTIVATE, 1.2f, 4, 48);
        WitcheryMod.LOGGER.info("Rite of Shifting Seasons primed at {} with radius {}", center, radius);
        return true;
    }

    private static ClimateChangeScan scanClimateChangePlan(ServerLevel level, BlockPos center) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(8.0, 2.0, 8.0));
        ItemEntity selectedFocusEntity = null;
        ClimateFocus selectedFocus = null;
        double bestDistance = Double.MAX_VALUE;
        int glowstone = 0;
        List<ItemEntity> glowstoneEntities = new ArrayList<>();
        double centerX = center.getX() + 0.5;
        double centerY = center.getY() + 0.5;
        double centerZ = center.getZ() + 0.5;

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(Items.GLOWSTONE_DUST)) {
                glowstone += stack.getCount();
                glowstoneEntities.add(itemEntity);
                continue;
            }

            ClimateFocus focus = climateFocusFromStack(stack);
            if (focus == null) {
                continue;
            }

            double distance = itemEntity.distanceToSqr(centerX, centerY, centerZ);
            if (selectedFocusEntity == null
                    || distance < bestDistance
                    || (distance == bestDistance && itemEntity.getId() < selectedFocusEntity.getId())) {
                selectedFocusEntity = itemEntity;
                selectedFocus = focus;
                bestDistance = distance;
            }
        }

        if (selectedFocusEntity == null || selectedFocus == null) {
            return null;
        }

        List<ItemEntity> itemsToConsume = new ArrayList<>(glowstoneEntities.size() + 1);
        itemsToConsume.add(selectedFocusEntity);
        itemsToConsume.addAll(glowstoneEntities);
        ClimateChangePlan plan = new ClimateChangePlan(resolveClimateBiome(level, selectedFocus, glowstone), weatherForFocus(selectedFocus));
        return new ClimateChangeScan(plan, List.copyOf(itemsToConsume));
    }

    private static void consumeClimateChangeItems(ServerLevel level, List<ItemEntity> items) {
        for (ItemEntity itemEntity : items) {
            consumeClimateChangeItem(level, itemEntity);
        }
    }

    private static List<ItemStack> copyConsumedItemStacks(List<ItemEntity> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemEntity itemEntity : items) {
            if (itemEntity == null || !itemEntity.isAlive()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }
        return List.copyOf(copies);
    }

    private static void consumeClimateChangeItem(ServerLevel level, ItemEntity itemEntity) {
        if (itemEntity == null || !itemEntity.isAlive()) {
            return;
        }
        level.sendParticles(ParticleTypes.ENCHANT,
                itemEntity.getX(), itemEntity.getY() + 0.25, itemEntity.getZ(),
                16, 0.15, 0.25, 0.15, 0.0);
        level.playSound(null, itemEntity.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25f, 1.6f);
        itemEntity.discard();
    }

    private static ClimateFocus climateFocusFromStack(ItemStack stack) {
        if (stack.is(Blocks.JUNGLE_SAPLING.asItem()) || stack.is(Blocks.BAMBOO.asItem())) {
            return ClimateFocus.JUNGLE;
        }
        if (stack.is(ItemTags.SAPLINGS)) {
            return ClimateFocus.FOREST;
        }
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Blocks.FERN.asItem())) {
            return ClimateFocus.PLAINS;
        }
        if (stack.is(Blocks.COBBLESTONE_WALL.asItem()) || stack.is(Blocks.COBBLESTONE.asItem())) {
            return ClimateFocus.MOUNTAIN;
        }
        if (stack.is(Blocks.STONE.asItem()) || stack.is(Blocks.STONE_BRICKS.asItem())) {
            return ClimateFocus.HILLS;
        }
        if (stack.is(Items.LILY_PAD) || stack.is(Items.SLIME_BALL)) {
            return ClimateFocus.SWAMP;
        }
        if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.WATER_BUCKET)) {
            return ClimateFocus.WATER;
        }
        if (stack.is(Blocks.CACTUS.asItem())) {
            return ClimateFocus.DESERT;
        }
        if (stack.is(WitcheryItems.ICY_NEEDLE.get())) {
            return ClimateFocus.FROZEN;
        }
        if (stack.is(Blocks.DEAD_BUSH.asItem()) || stack.is(Blocks.NETHERRACK.asItem())) {
            return ClimateFocus.WASTELAND;
        }
        if (stack.is(Blocks.SAND.asItem()) || stack.is(Blocks.SANDSTONE.asItem())) {
            return ClimateFocus.BEACH;
        }
        if (stack.is(Blocks.RED_MUSHROOM.asItem()) || stack.is(Blocks.BROWN_MUSHROOM.asItem()) || stack.is(Blocks.MYCELIUM.asItem())) {
            return ClimateFocus.MUSHROOM;
        }
        if (stack.is(Items.ENDER_EYE) || stack.is(WitcheryItems.FOCUSED_WILL.get())) {
            return ClimateFocus.MAGICAL;
        }
        return null;
    }

    private static Holder<Biome> resolveClimateBiome(ServerLevel level, ClimateFocus focus, int glowstoneCount) {
        return switch (focus) {
            case FOREST -> pickClimateBiome(level, glowstoneCount, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST);
            case PLAINS -> pickClimateBiome(level, glowstoneCount, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
            case MOUNTAIN -> pickClimateBiome(level, glowstoneCount, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS, Biomes.FROZEN_PEAKS);
            case HILLS -> pickClimateBiome(level, glowstoneCount, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
            case SWAMP -> pickClimateBiome(level, glowstoneCount, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
            case WATER -> pickClimateBiome(level, glowstoneCount, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_OCEAN);
            case DESERT -> pickClimateBiome(level, glowstoneCount, Biomes.DESERT);
            case FROZEN -> pickClimateBiome(level, glowstoneCount, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.FROZEN_PEAKS);
            case JUNGLE -> pickClimateBiome(level, glowstoneCount, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE);
            case WASTELAND -> pickClimateBiome(level, glowstoneCount, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
            case BEACH -> pickClimateBiome(level, glowstoneCount, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE);
            case MUSHROOM -> pickClimateBiome(level, glowstoneCount, Biomes.MUSHROOM_FIELDS);
            case MAGICAL -> pickClimateBiome(level, glowstoneCount, Biomes.CHERRY_GROVE, Biomes.FLOWER_FOREST, Biomes.DARK_FOREST);
        };
    }

    @SafeVarargs
    private static Holder<Biome> pickClimateBiome(ServerLevel level, int glowstoneCount, ResourceKey<Biome>... biomeKeys) {
        int index;
        if (glowstoneCount > 0) {
            index = Math.min(glowstoneCount, biomeKeys.length) - 1;
        } else {
            index = biomeKeys.length <= 1 ? 0 : level.random.nextInt(Math.min(3, biomeKeys.length));
        }
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biomeKeys[index]);
    }

    private static ClimateWeather weatherForFocus(ClimateFocus focus) {
        return switch (focus) {
            case DESERT -> ClimateWeather.SUN;
            case FROZEN -> ClimateWeather.RAIN;
            default -> ClimateWeather.NONE;
        };
    }

    private static void storeClimateChangePlan(ResourceKey<Level> dimension, BlockPos center, ClimateChangePlan plan) {
        CLIMATE_CHANGE_PLANS.computeIfAbsent(dimension, key -> new HashMap<>()).put(center.immutable(), plan);
    }

    private static ClimateChangePlan getClimateChangePlan(ResourceKey<Level> dimension, BlockPos center) {
        Map<BlockPos, ClimateChangePlan> plans = CLIMATE_CHANGE_PLANS.get(dimension);
        return plans != null ? plans.get(center) : null;
    }

    private static void clearClimateChangePlan(ResourceKey<Level> dimension, BlockPos center) {
        Map<BlockPos, ClimateChangePlan> plans = CLIMATE_CHANGE_PLANS.get(dimension);
        if (plans == null) {
            return;
        }

        plans.remove(center);
        if (plans.isEmpty()) {
            CLIMATE_CHANGE_PLANS.remove(dimension);
        }
    }

    private static void applyClimateChange(ServerLevel level, BlockPos center, int radius, ClimateChangePlan plan) {
        List<ChunkAccess> chunks = new ArrayList<>();
        int radiusSq = radius * radius;
        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);

        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                chunks.add(chunk);
                chunk.fillBiomesFromNoise(makeClimateChangeResolver(chunk, center, radiusSq, plan), level.getChunkSource().randomState().sampler());
                // This mapping does not expose a clean public dirty-chunk call on ChunkAccess, so reuse a no-op setter that marks the chunk unsaved.
                chunk.setLightCorrect(chunk.isLightCorrect());
            }
        }

        if (!chunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
        }
        applyClimateChangeWeather(level, center, radius, plan.weather());
    }

    private static BiomeResolver makeClimateChangeResolver(ChunkAccess chunk, BlockPos center, int radiusSq, ClimateChangePlan plan) {
        return (quartX, quartY, quartZ, sampler) -> {
            int blockX = QuartPos.toBlock(quartX);
            int blockZ = QuartPos.toBlock(quartZ);
            int dx = blockX - center.getX();
            int dz = blockZ - center.getZ();
            if (dx * dx + dz * dz <= radiusSq) {
                return plan.biomeHolder();
            }
            return chunk.getNoiseBiome(quartX, quartY, quartZ);
        };
    }

    private static void applyClimateChangeWeather(ServerLevel level, BlockPos center, int radius, ClimateWeather weather) {
        switch (weather) {
            case SUN -> {
                if (level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(0, 0, false, false);
                }
                clearClimateSnow(level, center, radius);
            }
            case RAIN -> {
                if (!level.isRaining() && !level.isThundering()) {
                    int rainTicks = (300 + level.random.nextInt(600)) * 20;
                    level.setWeatherParameters(0, rainTicks, true, false);
                }
            }
            case NONE -> {
            }
        }
    }

    private static void clearClimateSnow(ServerLevel level, BlockPos center, int radius) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }

                BlockPos surfacePos = center.offset(dx, 0, dz);
                BlockPos topPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, surfacePos);
                if (level.getBlockState(topPos).is(Blocks.SNOW)) {
                    level.setBlock(topPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean canPerformClimateRite(Player player, Level level, BlockPos center) {
        if (level.dimension().equals(Level.NETHER)
                || level.dimension().equals(Level.END)
                || level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || level.dimension().equals(WitcheryDimensions.TORMENT)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.wrongdimension"), false);
            return false;
        }
        return requireCoven(player, level, center, 4);
    }

    private static boolean requireThunderstorm(Player player, Level level, BlockPos center) {
        if (level instanceof ServerLevel serverLevel && serverLevel.isThundering()) {
            return true;
        }
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable("witchery.rite.needstorm"), false);
        return false;
    }

    private static boolean requireDay(Player player, Level level, BlockPos center) {
        long time = level.getDayTime() % 24000L;
        boolean isDay = time < 12300L;
        if (isDay) {
            return true;
        }

        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable("witchery.rite.needday"), false);
        return false;
    }

    private static boolean requireNight(Player player, Level level, BlockPos center) {
        long time = level.getDayTime() % 24000L;
        boolean isNight = time >= 12300L && time < 23850L;
        if (isNight) {
            return true;
        }

        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable("witchery.rite.neednight"), false);
        return false;
    }

    private static boolean requireNightOverworld(Player player, Level level, BlockPos center) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.wrongdimension"), false);
            return false;
        }

        if (requireNight(player, level, center)) {
            return true;
        }
        return false;
    }

    private static int countNearbyCovenWitches(Level level, BlockPos center) {
        return level.getEntitiesOfClass(
                com.emoniph.witchery.entity.CovenWitchEntity.class,
                new AABB(center).inflate(16.0)
        ).size();
    }

    private static boolean hasNearbyRitualAltar(ServerLevel level, BlockPos center, int radius) {
        AltarBlockEntity altar = findNearbyRitualAltar(level, center, radius);
        return altar != null && altar.hasValidStructure() && altar.getPower() > 0;
    }

    private static boolean consumeNearbyRitualAltarPower(ServerLevel level, BlockPos center, int radius, int powerCost) {
        AltarBlockEntity altar = findNearbyRitualAltar(level, center, radius);
        return altar != null && altar.hasValidStructure() && altar.consumePower(powerCost);
    }

    private static boolean consumeNearbyRitualAltarPower(ServerLevel level, ActiveRitual ritual, int radius) {
        if (ritual.upkeepCost <= 0.0f) {
            return true;
        }

        ritual.upkeepDebt += ritual.upkeepCost;
        int wholePower = Mth.floor(ritual.upkeepDebt);
        if (wholePower <= 0) {
            return hasNearbyRitualAltar(level, ritual.center, radius);
        }

        if (!consumeNearbyRitualAltarPower(level, ritual.center, radius, wholePower)) {
            return false;
        }

        ritual.upkeepDebt -= wholePower;
        return true;
    }

    private static AltarBlockEntity findNearbyRitualAltar(Level level, BlockPos center, int radius) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockEntity(checkPos) instanceof AltarBlockEntity altar && altar.hasValidStructure()) {
                return altar;
            }
        }
        return null;
    }

    private static boolean requireCoven(Player player, Level level, BlockPos center, int requiredSize) {
        int covenSize = countNearbyCovenWitches(level, center);
        if (covenSize >= requiredSize) {
            return true;
        }
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable("witchery.rite.coventoosmall"), false);
        return false;
    }

    public static void tickActiveRituals(ServerLevel level) {
        List<ActiveRitual> rituals = ACTIVE_RITUALS.get(level.dimension());
        if (rituals == null || rituals.isEmpty()) {
            return;
        }

        for (int i = rituals.size() - 1; i >= 0; i--) {
            ActiveRitual ritual = rituals.get(i);
            if (!tickActiveRitual(level, ritual)) {
                if (ritual.failedOnStop) {
                    level.playSound(null, ritual.center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                }
                if (ritual.refundOnStop) {
                    refundRitualIngredient(level, ritual);
                }
                rituals.remove(i);
            }
        }
        if (rituals.isEmpty()) {
            ACTIVE_RITUALS.remove(level.dimension());
        }
    }

    private static boolean tickActiveRitual(ServerLevel level, ActiveRitual ritual) {
        if (!hasSustainingCircle(level, ritual)) {
            clearClimateChangePlan(level.dimension(), ritual.center);
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        long gameTime = level.getGameTime();
        if (gameTime < ritual.nextGameTime) {
            return true;
        }

        ritual.nextGameTime = gameTime + ritual.pulseInterval;
        return switch (ritual.type) {
            case WEATHER_CALL_STORM -> tickStormRitual(level, ritual);
            case ICE_SHELL -> tickIceShellRitual(level, ritual);
            case RAIN_OF_TOADS -> tickRainOfToadsRitual(level, ritual);
            case BLINDNESS -> tickBlindnessRitual(level, ritual);
            case BLIGHT -> tickBlightRitual(level, ritual);
            case HELL_ON_EARTH -> tickHellOnEarthRitual(level, ritual);
            case CALL_CREATURES -> tickCallCreaturesRitual(level, ritual);
            case CLIMATE_CHANGE -> tickClimateChangeRitual(level, ritual);
            case NATURES_POWER -> tickNaturesPowerRitual(level, ritual);
            case TRANSPOSE_ORES -> tickTransposeOresRitual(level, ritual);
            case MOVING_EARTH -> tickMovingEarthRitual(level, ritual);
            case RAISE_COLUMN -> tickRaiseColumnRitual(level, ritual);
            case VOLCANO -> tickVolcanoRitual(level, ritual);
            case PROTECTION_BARRIER_MONSTERS -> tickProtectionBarrierMonstersRitual(level, ritual);
            case PROTECTION_BARRIER_PORTABLE -> tickProtectionBarrierPortableRitual(level, ritual);
            case PROTECTION_BARRIER -> tickProtectionBarrierRitual(level, ritual);
            case INFUSION_RECHARGE -> tickInfusionRechargeRitual(level, ritual);
            case SANCTITY_CIRCLE -> tickSanctityCircleRitual(level, ritual);
            case IMPRISONMENT_CIRCLE -> tickImprisonmentCircleRitual(level, ritual);
        };
    }

    private static boolean tickStormRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        switch (ritual.stage) {
            case 1, 2 -> spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.effectCenter);
            case 3 -> {
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.effectCenter);
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.effectCenter);
            }
            case 4 -> {
                if (!level.isThundering()) {
                    int stormTicks = (300 + level.random.nextInt(600)) * 20;
                    level.setWeatherParameters(0, stormTicks, true, true);
                }
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.effectCenter);
            }
            default -> {
                int extraBolts = level.random.nextInt(4);
                for (int i = 0; i < extraBolts; i++) {
                    spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.effectCenter);
                    if (i > 0) {
                        ritual.stage++;
                    }
                }
            }
        }

        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickIceShellRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        if (ritual.stage == 1) {
            level.playSound(null, ritual.center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.5f);
        }

        int maxRadius = ritual.covenSize <= 2
                ? ritual.minRadius
                : ritual.covenSize <= 5 ? Mth.floor(ritual.minRadius * 1.5f) : ritual.minRadius * 2;
        int currentRadius = ritual.stage + 4;
        if (currentRadius <= maxRadius && ritual.stage % 2 == 0) {
            drawIceShellSphere(level, ritual.center, currentRadius, Blocks.ICE);
            if (currentRadius > 2) {
                drawIceShellSphere(level, ritual.center, currentRadius - 2, Blocks.AIR);
            }
            spawnRingParticles(level, ritual.center, currentRadius, 20);
        }

        if (currentRadius == maxRadius) {
            clearIceShellInterior(level, ritual.center, maxRadius);
        }

        return ritual.stage <= ritual.targetStage && currentRadius < maxRadius;
    }

    private static boolean tickRainOfToadsRitual(ServerLevel level, ActiveRitual ritual) {
        if (ritual.covenSize < 1) {
            level.playSound(null, ritual.center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        ritual.stage++;
        if (ritual.stage <= 3) {
            spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
            return true;
        }

        if (ritual.stage == 4) {
            if (!level.isRaining() || !level.isThundering()) {
                int stormTicks = (300 + level.random.nextInt(600)) * 20;
                level.setWeatherParameters(0, stormTicks, true, true);
            }
            spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
            return true;
        }

        spawnFallingToads(level, ritual.center, ritual.minRadius, ritual.maxRadius, 8 + level.random.nextInt(10));
        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickBlindnessRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        int maxRadius = ritual.maxRadius + ritual.covenSize * 2;
        int currentRadius = ritual.stage + 3;
        if (currentRadius > maxRadius) {
            return false;
        }
        if (!applyBlindnessRing(level, ritual.effectCenter, currentRadius, ritual.enhanced)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, currentRadius, 18);
        }
        return currentRadius < maxRadius;
    }

    private static boolean tickBlightRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        int maxRadius = ritual.maxRadius + ritual.covenSize * 2;
        int currentRadius = ritual.stage + 3;
        if (currentRadius > maxRadius) {
            return false;
        }
        if (!applyBlightRing(level, ritual.effectCenter, currentRadius, ritual.enhanced)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }
        spreadBlightRing(level, ritual.effectCenter, currentRadius, ritual.enhanced);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, currentRadius, 18);
        }
        return currentRadius < maxRadius;
    }

    private static boolean tickHellOnEarthRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        int maxRadius = ritual.maxRadius + ritual.covenSize * 2;
        int currentRadius = ritual.stage + 3;
        if (currentRadius <= maxRadius) {
            spreadHellOnEarthRing(level, ritual.effectCenter, currentRadius, maxRadius, ritual.enhanced);
            if (ritual.stage == 1 || ritual.stage % 4 == 0) {
                spawnRingParticles(level, ritual.effectCenter, currentRadius, 20);
            }
        }

        if (currentRadius >= maxRadius && level.getGameTime() % 40L == 0L) {
            if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
                return false;
            }
            spawnHellOnEarthMob(level, ritual.effectCenter);
        }

        return ritual.stage <= ritual.targetStage;
    }

    private static boolean tickCallCreaturesRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        allureCreatures(level, ritual.center, ritual.stage % 8);
        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickClimateChangeRitual(ServerLevel level, ActiveRitual ritual) {
        ClimateChangePlan plan = getClimateChangePlan(level.dimension(), ritual.center);
        if (plan == null) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        ritual.stage++;
        if (ritual.stage == 1) {
            level.playSound(null, ritual.center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        if (ritual.stage < ritual.targetStage) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    ritual.center.getX() + 0.5, ritual.center.getY() + 1.0, ritual.center.getZ() + 0.5,
                    16, ritual.stage * 1.5, ritual.stage * 1.1, ritual.stage * 1.5, 0.0);
            return true;
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                ritual.center.getX() + 0.5, ritual.center.getY() + 1.0, ritual.center.getZ() + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        applyClimateChange(level, ritual.center, ritual.minRadius, plan);
        clearClimateChangePlan(level.dimension(), ritual.center);
        return false;
    }

    private static boolean tickNaturesPowerRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        if (ritual.stage >= ritual.targetStage + ritual.covenSize * 5) {
            return false;
        }

        BlockPos anchor = findNaturesPowerAnchor(level, ritual.center, ritual.minRadius + ritual.covenSize * 2, ritual.maxRadius);
        if (anchor == null) {
            return true;
        }

        level.levelEvent(2005, ritual.center.above(2), 0);
        level.levelEvent(2005, anchor.above(), 0);
        growNaturePatch(level, anchor, 3);
        return true;
    }

    private static boolean tickTransposeOresRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        int scanY = ritual.center.getY() - ritual.stage;
        if (scanY <= level.getMinY() + 2) {
            return false;
        }

        int transposed = transposeOreLayer(level, ritual.center, scanY, ritual.minRadius, ritual.covenSize >= 6);
        if (transposed > 0) {
            level.playSound(null, ritual.center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.35f, 1.5f);
            level.sendParticles(ParticleTypes.PORTAL,
                    ritual.center.getX() + 0.5, ritual.center.getY() + 1.0, ritual.center.getZ() + 0.5,
                    10 + transposed * 2, ritual.minRadius * 0.4, 0.4, ritual.minRadius * 0.4, 0.08);
        }

        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickMovingEarthRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        if (ritual.stage < 20) {
            return true;
        }
        if (ritual.path.isEmpty()) {
            buildMovingEarthPath(level, ritual);
        }

        int carveStage = ritual.stage - 19;
        int pathIndex = carveStage + 4;
        if (pathIndex >= ritual.path.size()) {
            return false;
        }

        BlockPos carvePos = ritual.path.get(pathIndex);
        int width = ritual.minRadius + (ritual.covenSize > 2 ? 2 : 0) + (level.random.nextInt(3) == 0 ? 1 : 0);
        int depth = ritual.maxRadius - 2 + level.random.nextInt(5);
        carveMovingEarthCircle(level, carvePos, width, depth);
        if (carveStage == 1 || carveStage % 10 == 0) {
            spawnRingParticles(level, ritual.center, 3, 12);
        }
        return carveStage < ritual.path.size() - 5;
    }

    private static boolean tickRaiseColumnRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        BlockPos effectCenter = ritual.effectCenter;
        if (ritual.stage == 1) {
            level.playSound(null, effectCenter, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.8f);
            spawnRingParticles(level, effectCenter, Math.max(ritual.minRadius, 3), 32);
        }

        int baseRadius = ritual.minRadius;
        int height = ritual.maxRadius;
        int effectiveRadius = baseRadius + ritual.covenSize * 2;
        int airSpace = baseRadius * 2;

        for (int y = effectCenter.getY() + airSpace; y >= effectCenter.getY() - height; y--) {
            raiseColumnLayer(level, effectCenter, y, effectiveRadius, y == effectCenter.getY() - 1);
        }

        liftRaiseColumnEntities(level, effectCenter, effectiveRadius, airSpace);
        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickVolcanoRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        BlockPos effectCenter = ritual.effectCenter;
        if (ritual.stage == 1) {
            level.playSound(null, effectCenter, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.0f);
            level.sendParticles(ParticleTypes.PORTAL,
                    effectCenter.getX() + 0.5, effectCenter.getY() + 0.5, effectCenter.getZ() + 0.5,
                    16, 0.5, 1.0, 0.5, 0.0);
        }

        int height = ritual.maxRadius + 4 * ritual.covenSize;
        int radius = ritual.minRadius + 2 * ritual.covenSize;
        if (ritual.stage <= height) {
            for (int yOffset = 1; yOffset <= ritual.stage; yOffset++) {
                float layerRadius = radius - (height - ritual.stage - 1 + yOffset) * radius / (float) height;
                int actualRadius = Math.max((int) Math.ceil(layerRadius), 1);
                drawVolcanoFilledCircle(level, effectCenter, effectCenter.getY() + yOffset - 1, actualRadius, yOffset, true);
                if (ritual.stage == height) {
                    int belowY = effectCenter.getY() - 1;
                    for (int reduce = 0; belowY > effectCenter.getY() - 5; reduce++, belowY--) {
                        drawVolcanoFilledCircle(level, effectCenter, belowY, Math.max(radius - reduce, 2), yOffset, false);
                    }
                }
                liftVolcanoEntities(level, effectCenter, effectCenter.getY() + yOffset, actualRadius);
            }
            return true;
        }

        if (ritual.stage >= height * 2) {
            openVolcanoVent(level, effectCenter);
            return false;
        }

        if (ritual.stage == height * 2 - 1) {
            level.setBlock(effectCenter.above(ritual.stage - height), Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(effectCenter.above(), Blocks.LAVA.defaultBlockState(), 3);
            switch (level.random.nextInt(8)) {
                case 0 -> level.setBlock(effectCenter.offset(1, height - 1, 0), Blocks.AIR.defaultBlockState(), 3);
                case 1 -> level.setBlock(effectCenter.offset(0, height - 1, 1), Blocks.AIR.defaultBlockState(), 3);
                case 2 -> level.setBlock(effectCenter.offset(-1, height - 1, 0), Blocks.AIR.defaultBlockState(), 3);
                case 3 -> level.setBlock(effectCenter.offset(0, height - 1, -1), Blocks.AIR.defaultBlockState(), 3);
                default -> {
                }
            }
        } else {
            level.setBlock(effectCenter.above(), Blocks.SAND.defaultBlockState(), 3);
            level.setBlock(effectCenter.above(ritual.stage - height), Blocks.LAVA.defaultBlockState(), 3);
        }

        if (ritual.stage % 4 == 0) {
            spawnRingParticles(level, effectCenter, Math.max(radius / 2, 4), 18);
        }
        return true;
    }

    private static boolean tickProtectionBarrierRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        ritual.stage++;
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, true);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, ritual.minRadius, 16);
        }
        return ritual.targetStage <= 0 || ritual.stage < ritual.targetStage;
    }

    private static boolean tickProtectionBarrierMonstersRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        ritual.stage++;
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, false);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, ritual.minRadius, 12);
        }
        return true;
    }

    private static boolean tickProtectionBarrierPortableRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, true);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, ritual.minRadius, 12);
        }
        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickInfusionRechargeRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        int radius = ritual.minRadius;
        AABB bounds = new AABB(
                ritual.center.getX() - radius, ritual.center.getY(), ritual.center.getZ() - radius,
                ritual.center.getX() + radius, ritual.center.getY() + 1, ritual.center.getZ() + radius
        );

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            double dx = player.getX() - ritual.center.getX();
            double dy = player.getY() - ritual.center.getY();
            double dz = player.getZ() - ritual.center.getZ();
            if (dx * dx + dy * dy + dz * dz > radius * radius) {
                continue;
            }

            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.getInfusionType() == InfusionType.NONE || data.getInfusionMaxEnergy() <= 0) {
                continue;
            }

            int currentEnergy = data.getInfusionEnergy();
            int maxEnergy = data.getInfusionMaxEnergy();
            if (currentEnergy >= maxEnergy) {
                continue;
            }

            data.setInfusionEnergy(Math.min(currentEnergy + 10, maxEnergy), maxEnergy);
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
            level.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    8, 1.0, 2.0, 1.0, 0.0);
        }
        return true;
    }

    private static void drawProtectionBarrier(ServerLevel level, BlockPos center, int radius, int height, boolean blocksPlayers) {
        int radiusSq = radius * radius;
        int innerSq = Math.max(radius - 1, 0);
        innerSq *= innerSq;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                if (distSq > radiusSq) {
                    continue;
                }

                BlockPos floorPos = center.offset(x, -1, z);
                com.emoniph.witchery.blocks.BarrierBlock.setBarrier(level, floorPos, 30, blocksPlayers);

                BlockPos roofPos = center.offset(x, height, z);
                com.emoniph.witchery.blocks.BarrierBlock.setBarrier(level, roofPos, 30, blocksPlayers);

                if (distSq <= innerSq) {
                    continue;
                }

                for (int y = 0; y < height; y++) {
                    BlockPos wallPos = center.offset(x, y, z);
                    com.emoniph.witchery.blocks.BarrierBlock.setBarrier(level, wallPos, 30, blocksPlayers);
                }
            }
        }
    }

    private static boolean tickSanctityCircleRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        int repelled = 0;
        AABB bounds = new AABB(ritual.center).inflate(ritual.minRadius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds)) {
            if (mob instanceof EnderDragon) {
                continue;
            }
            if (pushProtectionMob(mob, ritual.center, ritual.minRadius)) {
                repelled++;
            }
        }
        if (repelled > 0 && level.getGameTime() % 80L == 0L) {
            spawnRingParticles(level, ritual.center, ritual.minRadius, 12);
        }
        return true;
    }

    private static boolean tickImprisonmentCircleRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        int trapped = 0;
        AABB bounds = new AABB(ritual.center).inflate(ritual.minRadius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds)) {
            if (mob instanceof EnderDragon) {
                continue;
            }
            if (pullProtectionMob(mob, ritual.center, ritual.minRadius)) {
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, true, false));
                trapped++;
            }
        }
        if (trapped > 0 && level.getGameTime() % 80L == 0L) {
            spawnRingParticles(level, ritual.center, ritual.minRadius, 12);
        }
        return true;
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage) {
        return queueActiveRitual(level, type, circleSize, center, minRadius, maxRadius, targetStage, RITUAL_PULSE_INTERVAL);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage, int pulseInterval) {
        return queueActiveRitual(level, type, circleSize, center, minRadius, maxRadius, targetStage, pulseInterval, 0, 0.0f, 0);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize) {
        return queueActiveRitual(level, type, circleSize, center, minRadius, maxRadius, targetStage, pulseInterval, covenSize, 0.0f, 0);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, int heading) {
        return queueActiveRitual(level, type, circleSize, center, minRadius, maxRadius, targetStage, pulseInterval, covenSize, 0.0f, heading);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading) {
        return queueActiveRitual(level, type, circleSize, center, center, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, heading, List.of(), false);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading,
                                             List<ItemStack> supplementalSacrifices) {
        return queueActiveRitual(level, type, circleSize, center, center, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, heading, supplementalSacrifices, false);
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, BlockPos effectCenter, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading,
                                             List<ItemStack> supplementalSacrifices, boolean enhanced) {
        List<ActiveRitual> rituals = ACTIVE_RITUALS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        boolean occupied = rituals.stream().anyMatch(active -> active.center.equals(center));
        if (occupied) {
            return false;
        }

        int circleVariant = RitualCircleDetector.detectVariant(level, center, circleSize);
        ItemStack sacrificedIngredient = PENDING_RITUAL_INGREDIENT.get();
        rituals.add(new ActiveRitual(type, center, circleSize, effectCenter, circleVariant, minRadius, maxRadius, targetStage, pulseInterval,
                covenSize, upkeepCost, enhanced, heading,
                level.getGameTime() + pulseInterval,
                sacrificedIngredient.isEmpty() ? ItemStack.EMPTY : sacrificedIngredient.copy(),
                List.copyOf(supplementalSacrifices)));
        return true;
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, BlockPos effectCenter, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading,
                                             List<ItemStack> supplementalSacrifices) {
        return queueActiveRitual(level, type, circleSize, center, effectCenter, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, heading, supplementalSacrifices, false);
    }

    private static boolean hasSustainingCircle(ServerLevel level, ActiveRitual ritual) {
        if (!RitualCircleDetector.hasRing(level, ritual.center, ritual.circleSize)) {
            return false;
        }
        if (ritual.circleVariant <= 0) {
            return true;
        }
        return RitualCircleDetector.detectVariant(level, ritual.center, ritual.circleSize) == ritual.circleVariant;
    }

    private static void refundRitualIngredient(ServerLevel level, ActiveRitual ritual) {
        if (!ritual.sacrificedIngredient.isEmpty()) {
            spawnRefundItem(level, ritual.center, ritual.sacrificedIngredient);
        }
        for (ItemStack supplemental : ritual.supplementalSacrifices) {
            if (!supplemental.isEmpty()) {
                spawnRefundItem(level, ritual.center, supplemental);
            }
        }
    }

    private static void spawnRefundItem(ServerLevel level, BlockPos center, ItemStack stack) {
        ItemEntity refund = new ItemEntity(level,
                center.getX() + 0.5,
                center.getY() + 1.0,
                center.getZ() + 0.5,
                stack.copy());
        level.addFreshEntity(refund);
    }

    private static void spawnRitualBolt(ServerLevel level, int minRadius, int maxRadius, BlockPos center) {
        BlockPos strikePos = randomRitualPos(level, center, minRadius, maxRadius);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, strikePos);
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        level.addFreshEntity(bolt);
    }

    private static boolean applyBlindnessRing(ServerLevel level, BlockPos center, int radius, boolean enhanced) {
        double radiusSq = (double) radius * radius;
        double minSq = (double) Math.max(0, (radius - 1) * (radius - 1));
        int duration = (enhanced ? 5 : 2) * 1200;
        AABB bounds = new AABB(center).inflate(radius);

        for (ServerPlayer victim : level.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            double distanceSq = victim.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq) {
                continue;
            }
            if (triggerVoodooProtection(victim)) {
                victim.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
                return false;
            }
            if (!victim.hasEffect(MobEffects.BLINDNESS)) {
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            }
        }

        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (victim instanceof Player) {
                continue;
            }
            double distanceSq = victim.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq || victim.hasEffect(MobEffects.BLINDNESS)) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        }

        return true;
    }

    private static boolean applyBlightRing(ServerLevel level, BlockPos center, int radius, boolean enhanced) {
        double radiusSq = (double) radius * radius;
        double minSq = (double) Math.max(0, (radius - 1) * (radius - 1));
        AABB bounds = new AABB(center).inflate(radius);

        for (ServerPlayer victim : level.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            double distanceSq = victim.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq) {
                continue;
            }
            if (triggerVoodooProtection(victim)) {
                victim.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
                return false;
            }
            if (!victim.hasEffect(MobEffects.WITHER)) {
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 2400, 1));
            }
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class, bounds)) {
            double distanceSq = villager.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq || level.random.nextInt(10) != 0) {
                continue;
            }
            ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (zombie == null) {
                continue;
            }
            zombie.setPos(villager.getX(), villager.getY(), villager.getZ());
            zombie.setYRot(villager.getYRot());
            zombie.setBaby(villager.isBaby());
            level.addFreshEntity(zombie);
            villager.discard();
        }

        for (Cow cow : level.getEntitiesOfClass(Cow.class, bounds)) {
            double distanceSq = cow.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq) {
                continue;
            }
            if (level.random.nextInt(20) == 0) {
                MushroomCow mooshroom = EntityType.MOOSHROOM.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (mooshroom != null) {
                    mooshroom.setPos(cow.getX(), cow.getY(), cow.getZ());
                    mooshroom.setYRot(cow.getYRot());
                    mooshroom.setBaby(cow.isBaby());
                    level.addFreshEntity(mooshroom);
                }
                cow.discard();
            } else if (level.random.nextInt(3) == 0) {
                cow.hurt(level.damageSources().magic(), 20.0f);
            }
        }

        for (Animal animal : level.getEntitiesOfClass(Animal.class, bounds)) {
            if (animal instanceof Cow) {
                continue;
            }
            double distanceSq = animal.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distanceSq <= minSq || distanceSq > radiusSq || level.random.nextInt(3) != 0) {
                continue;
            }
            animal.hurt(level.damageSources().magic(), 20.0f);
        }

        return true;
    }

    private static void spreadBlightRing(ServerLevel level, BlockPos center, int radius, boolean enhanced) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;

        while (x >= z) {
            blightPixel(level, center, center.getX() + x, center.getZ() + z, enhanced);
            blightPixel(level, center, center.getX() + z, center.getZ() + x, enhanced);
            blightPixel(level, center, center.getX() - x, center.getZ() + z, enhanced);
            blightPixel(level, center, center.getX() - z, center.getZ() + x, enhanced);
            blightPixel(level, center, center.getX() - x, center.getZ() - z, enhanced);
            blightPixel(level, center, center.getX() - z, center.getZ() - x, enhanced);
            blightPixel(level, center, center.getX() + x, center.getZ() - z, enhanced);
            blightPixel(level, center, center.getX() + z, center.getZ() - x, enhanced);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void blightPixel(ServerLevel level, BlockPos center, int x, int z, boolean enhanced) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, center.getY(), z));
        BlockPos groundPos = surface.below();
        if (groundPos.getY() <= level.getMinY()) {
            return;
        }

        BlockState state = level.getBlockState(groundPos);
        BlockPos abovePos = groundPos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.is(Blocks.SNOW) || aboveState.is(Blocks.SNOW_BLOCK)) {
            level.setBlockAndUpdate(abovePos, Blocks.AIR.defaultBlockState());
        } else if (isBlightPlant(aboveState)) {
            level.setBlockAndUpdate(abovePos, Blocks.DEAD_BUSH.defaultBlockState());
        }

        if (state.is(Blocks.FARMLAND)) {
            level.setBlockAndUpdate(groundPos, Blocks.SOUL_SAND.defaultBlockState());
            return;
        }
        if (state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)) {
            int roll = level.random.nextInt(enhanced ? 4 : 5);
            if (roll == 0) {
                level.setBlockAndUpdate(groundPos, Blocks.SOUL_SAND.defaultBlockState());
            } else if (roll == 1) {
                level.setBlockAndUpdate(groundPos, Blocks.DIRT.defaultBlockState());
            }
        }
    }

    private static boolean isBlightPlant(BlockState state) {
        return state.is(Blocks.DANDELION)
                || state.is(Blocks.POPPY)
                || state.is(Blocks.BLUE_ORCHID)
                || state.is(Blocks.ALLIUM)
                || state.is(Blocks.AZURE_BLUET)
                || state.is(Blocks.RED_TULIP)
                || state.is(Blocks.ORANGE_TULIP)
                || state.is(Blocks.WHITE_TULIP)
                || state.is(Blocks.PINK_TULIP)
                || state.is(Blocks.OXEYE_DAISY)
                || state.is(Blocks.CORNFLOWER)
                || state.is(Blocks.LILY_OF_THE_VALLEY)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.VINE)
                || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SUNFLOWER)
                || state.is(Blocks.LILAC)
                || state.is(Blocks.ROSE_BUSH)
                || state.is(Blocks.PEONY);
    }

    private static void spreadHellOnEarthRing(ServerLevel level, BlockPos center, int radius, int maxRadius, boolean enhanced) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;

        while (x >= z) {
            blightHellOnEarthPixel(level, center, center.getX() + x, center.getZ() + z, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() + z, center.getZ() + x, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() - x, center.getZ() + z, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() - z, center.getZ() + x, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() - x, center.getZ() - z, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() - z, center.getZ() - x, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() + x, center.getZ() - z, radius, maxRadius, enhanced);
            blightHellOnEarthPixel(level, center, center.getX() + z, center.getZ() - x, radius, maxRadius, enhanced);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void blightHellOnEarthPixel(ServerLevel level, BlockPos center, int x, int z, int currentRadius,
                                               int maxRadius, boolean enhanced) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, center.getY(), z));
        BlockPos groundPos = surface.below();
        if (groundPos.getY() <= level.getMinY()) {
            return;
        }

        BlockState groundState = level.getBlockState(groundPos);
        if (groundState.is(Blocks.SNOW) || groundState.is(Blocks.SNOW_BLOCK)) {
            groundPos = groundPos.below();
            groundState = level.getBlockState(groundPos);
        }

        applyHellOnEarthGround(level, groundPos, groundState, currentRadius, maxRadius, enhanced);
    }

    private static void applyHellOnEarthGround(ServerLevel level, BlockPos groundPos, BlockState groundState,
                                               int currentRadius, int maxRadius, boolean enhanced) {
        if (!(groundState.is(Blocks.DIRT)
                || groundState.is(Blocks.GRASS_BLOCK)
                || groundState.is(Blocks.MYCELIUM)
                || groundState.is(Blocks.PODZOL)
                || groundState.is(Blocks.COARSE_DIRT)
                || groundState.is(Blocks.MUD))) {
            return;
        }

        int divisor = currentRadius < maxRadius / 3 ? 2 : (currentRadius < maxRadius / 2 ? 4 : 6);
        if (level.random.nextInt(divisor) != 0) {
            return;
        }

        level.setBlockAndUpdate(groundPos, Blocks.NETHERRACK.defaultBlockState());
        BlockPos firePos = groundPos.above();
        if (enhanced && level.random.nextInt(3) == 0 && level.getBlockState(firePos).canBeReplaced()) {
            level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
        }
    }

    private static void spawnHellOnEarthMob(ServerLevel level, BlockPos center) {
        Mob mob;
        double roll = level.random.nextDouble();
        if (roll < 0.02) {
            mob = WitcheryEntities.DEMON.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        } else if (roll < 0.10) {
            mob = EntityType.GHAST.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        } else if (roll < 0.40) {
            mob = EntityType.BLAZE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        } else if (roll < 0.60) {
            mob = EntityType.MAGMA_CUBE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        } else {
            mob = EntityType.ZOMBIFIED_PIGLIN.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        }
        if (mob == null) {
            return;
        }

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, center);
        mob.setPos(surface.getX() + 0.5, surface.getY() + 2.0, surface.getZ() + 0.5);
        mob.setYRot(level.random.nextFloat() * 360.0f);
        level.addFreshEntity(mob);
        level.playSound(null, surface, SoundEvents.BLAZE_DEATH, SoundSource.HOSTILE, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.EXPLOSION,
                surface.getX() + 0.5, surface.getY() + 2.0, surface.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.01);
    }

    private static void drawIceShellSphere(ServerLevel level, BlockPos center, int radius, Block block) {
        int radiusSq = radius * radius;
        int innerRadius = Math.max(radius - 1, 0);
        int innerRadiusSq = innerRadius * innerRadius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > radiusSq) {
                        continue;
                    }
                    if (block != Blocks.AIR && distSq < innerRadiusSq) {
                        continue;
                    }

                    BlockPos target = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(target);
                    if (block == Blocks.AIR) {
                        if (canIceShellClear(state)) {
                            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                        }
                    } else if (canIceShellReplace(state)) {
                        level.setBlockAndUpdate(target, block.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void clearIceShellInterior(ServerLevel level, BlockPos center, int radius) {
        int innerRadius = Math.max(radius - 1, 0);
        int innerRadiusSq = innerRadius * innerRadius;
        for (int dx = -innerRadius; dx <= innerRadius; dx++) {
            for (int dy = -innerRadius; dy <= innerRadius; dy++) {
                for (int dz = -innerRadius; dz <= innerRadius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > innerRadiusSq) {
                        continue;
                    }
                    BlockPos target = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(target);
                    if (canIceShellClear(state)) {
                        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static boolean canIceShellReplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || !state.getFluidState().isEmpty()
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean canIceShellClear(BlockState state) {
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static void spawnFallingToads(ServerLevel level, BlockPos center, int minRadius, int maxRadius, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos target = randomRitualPos(level, center, minRadius, maxRadius);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target);
            var toad = WitcheryEntities.TOAD.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (toad == null) {
                continue;
            }
            toad.setPos(surface.getX() + 0.5, surface.getY() + 8 + level.random.nextInt(7), surface.getZ() + 0.5);
            level.addFreshEntity(toad);
        }
    }

    private static void allureCreatures(ServerLevel level, BlockPos center, int quadrant) {
        AABB search = switch (quadrant) {
            case 0 -> new AABB(center.getX(), center.getY() - 10.0, center.getZ() - 128.0,
                    center.getX() + 128.0, center.getY(), center.getZ());
            case 1 -> new AABB(center.getX() - 128.0, center.getY() - 10.0, center.getZ() - 128.0,
                    center.getX(), center.getY(), center.getZ());
            case 2 -> new AABB(center.getX(), center.getY() - 10.0, center.getZ(),
                    center.getX() + 128.0, center.getY(), center.getZ() + 128.0);
            case 3 -> new AABB(center.getX() - 128.0, center.getY() - 10.0, center.getZ(),
                    center.getX(), center.getY(), center.getZ() + 128.0);
            case 4 -> new AABB(center.getX() - 128.0, center.getY() + 1.0, center.getZ() - 128.0,
                    center.getX(), center.getY() + 10.0, center.getZ());
            case 5 -> new AABB(center.getX(), center.getY() + 1.0, center.getZ(),
                    center.getX() + 128.0, center.getY() + 10.0, center.getZ() + 128.0);
            case 6 -> new AABB(center.getX() - 128.0, center.getY() + 1.0, center.getZ(),
                    center.getX(), center.getY() + 10.0, center.getZ() + 128.0);
            default -> new AABB(center.getX(), center.getY() + 1.0, center.getZ() - 128.0,
                    center.getX() + 128.0, center.getY() + 10.0, center.getZ());
        };

        int lured = 0;
        for (Animal animal : level.getEntitiesOfClass(Animal.class, search, RitualExecutor::isSummoningCreature)) {
            if (animal.distanceToSqr(center.getX(), center.getY(), center.getZ()) <= 32.0) {
                continue;
            }
            animal.teleportTo(center.getX() - 2.0 + level.random.nextInt(5),
                    center.getY(), center.getZ() - 2.0 + level.random.nextInt(5));
            if (++lured >= 2) {
                break;
            }
        }
    }

    private static boolean isSummoningCreature(Animal animal) {
        EntityType<?> type = animal.getType();
        return type == EntityType.PIG
                || type == EntityType.CHICKEN
                || type == EntityType.COW
                || type == EntityType.SHEEP
                || type == EntityType.MOOSHROOM
                || type == EntityType.WOLF
                || type == EntityType.OCELOT;
    }

    private static BlockPos randomRitualPos(ServerLevel level, BlockPos center, int minRadius, int maxRadius) {
        int activeRadius = maxRadius - minRadius;
        int ax = level.random.nextInt(activeRadius * 2 + 1);
        if (ax > activeRadius) {
            ax += minRadius * 2;
        }

        int az = level.random.nextInt(activeRadius * 2 + 1);
        if (az > activeRadius) {
            az += minRadius * 2;
        }

        return new BlockPos(center.getX() - maxRadius + ax, center.getY(), center.getZ() - maxRadius + az);
    }

    private static void buildMovingEarthPath(ServerLevel level, ActiveRitual ritual) {
        ritual.path.clear();
        BlockPos cursor = ritual.center.below();
        ritual.path.add(cursor);
        int heading = ritual.heading;
        for (int length = 0; length < ritual.targetStage - 1; length++) {
            int probability = Math.max(20 - length / 2, 6);
            MovingEarthStep next = nextMovingEarthStep(level, cursor, heading, probability);
            cursor = next.pos();
            heading = next.heading();
            ritual.path.add(cursor);
        }
    }

    private static void carveMovingEarthCircle(ServerLevel level, BlockPos center, int radius, int depth) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                carveMovingEarthColumn(level, center.offset(dx, 0, dz), depth);
            }
        }
    }

    private static void carveMovingEarthColumn(ServerLevel level, BlockPos center, int depth) {
        for (int offset = 0; offset < depth; offset++) {
            BlockPos pos = center.below(offset);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.liquid() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0f) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static MovingEarthStep nextMovingEarthStep(ServerLevel level, BlockPos current, int heading, int probability) {
        int value = level.random.nextInt(probability);
        return switch (heading) {
            case 0 -> {
                if (value == 0) yield new MovingEarthStep(current.north().east(), 1);
                if (value == 1) yield new MovingEarthStep(current.north().west(), 7);
                yield new MovingEarthStep(current.north(), 0);
            }
            case 1 -> {
                if (value == 0) yield new MovingEarthStep(current.north(), 0);
                if (value == 1) yield new MovingEarthStep(current.east(), 2);
                yield new MovingEarthStep(current.north().east(), 1);
            }
            case 2 -> {
                if (value == 0) yield new MovingEarthStep(current.north().east(), 1);
                if (value == 1) yield new MovingEarthStep(current.south().east(), 3);
                yield new MovingEarthStep(current.east(), 2);
            }
            case 3 -> {
                if (value == 0) yield new MovingEarthStep(current.east(), 2);
                if (value == 1) yield new MovingEarthStep(current.south(), 4);
                yield new MovingEarthStep(current.south().east(), 3);
            }
            case 4 -> {
                if (value == 0) yield new MovingEarthStep(current.south().east(), 3);
                if (value == 1) yield new MovingEarthStep(current.south().west(), 5);
                yield new MovingEarthStep(current.south(), 4);
            }
            case 5 -> {
                if (value == 0) yield new MovingEarthStep(current.south(), 4);
                if (value == 1) yield new MovingEarthStep(current.west(), 6);
                yield new MovingEarthStep(current.south().west(), 5);
            }
            case 6 -> {
                if (value == 0) yield new MovingEarthStep(current.south().west(), 5);
                if (value == 1) yield new MovingEarthStep(current.north().west(), 7);
                yield new MovingEarthStep(current.west(), 6);
            }
            default -> {
                if (value == 0) yield new MovingEarthStep(current.west(), 6);
                if (value == 1) yield new MovingEarthStep(current.north(), 0);
                yield new MovingEarthStep(current.north().west(), 7);
            }
        };
    }

    private static int headingFromLook(Vec3 look) {
        if (Math.abs(look.x) < 0.0001 && Math.abs(look.z) < 0.0001) {
            return 0;
        }
        double angle = Math.atan2(look.z, look.x);
        int octant = Mth.floor((angle / (Math.PI / 4.0)) + 8.5) & 7;
        return switch (octant) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            case 4 -> 6;
            case 5 -> 7;
            case 6 -> 0;
            default -> 1;
        };
    }

    private record MovingEarthStep(BlockPos pos, int heading) {}

    /**
     * Rite of Summoning the Lord of Torment.
     * Ingredient: Torment Scroll, Large circle.
     * Cannot be cast inside the Torment dimension itself.
     * Spawns LordOfTormentEntity at the circle center with a lightning-ring fanfare.
     */
    private static boolean riteOfSummoningLordOfTorment(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl))   return false;
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        // Block re-summoning from inside Torment
        if (sl.dimension().equals(WitcheryDimensions.TORMENT)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.lord_of_torment.already_in_torment"), false);
            return false;
        }

        // Lightning ring drama
        for (int i = 0; i < 12; i++) {
            double angle = 2 * Math.PI * i / 12;
            int bx = center.getX() + (int) Math.round(4 * Math.cos(angle));
            int bz = center.getZ() + (int) Math.round(4 * Math.sin(angle));
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
            bolt.setPos(bx + 0.5, (double)(center.getY() + 1), bz + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }

        // Portal particles column
        for (int y = 0; y < 8; y++) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, center.getX() + 0.5, center.getY() + y, center.getZ() + 0.5, 12, 0.8, 0.1, 0.8, 0.2);
        }

        // Spawn the boss 2 blocks above the circle center
        var lord = new com.emoniph.witchery.entity.LordOfTormentEntity(
                WitcheryEntities.LORD_OF_TORMENT.get(), sl);
        lord.setPos(center.getX() + 0.5, (double)(center.getY() + 2), center.getZ() + 0.5);
        lord.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(lord);

        success(player, level, center, "witchery.ritual.lord_of_torment.success",
                SoundEvents.WITHER_SPAWN, 0.5f, 4, 80);
        WitcheryMod.LOGGER.info("Rite of Summoning Lord of Torment: cast by {}", sp.getName().getString());
        return true;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static void success(Player player, Level level, BlockPos center,
                                String messageKey, net.minecraft.sounds.SoundEvent sound,
                                float pitch, int particleRadius, int particleCount) {
        level.playSound(null, center, sound, SoundSource.BLOCKS, 1f, pitch);
        if (messageKey != null) {
            player.displayClientMessage(Component.translatable(messageKey), false);
        }
        if (level instanceof ServerLevel sl) {
            spawnRingParticles(sl, center, particleRadius, particleCount);
        }
    }

    private static boolean hasSummonCircleObstruction(Level level, BlockPos center) {
        final int[][] pattern = new int[][]{
                {0, 0, 1, 1, 1, 0, 0},
                {0, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 2, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1},
                {0, 1, 1, 1, 1, 1, 0},
                {0, 0, 1, 1, 1, 0, 0}
        };

        int obstructions = 0;
        for (int y = center.getY() + 1; y <= center.getY() + 3; y++) {
            int offsetZ = (pattern.length - 1) / 2;
            for (int z = 0; z < pattern.length; z++) {
                int worldZ = center.getZ() - offsetZ + z;
                int offsetX = (pattern[z].length - 1) / 2;
                for (int x = 0; x < pattern[z].length; x++) {
                    int val = pattern[pattern.length - 1 - z][x];
                    if (val == 0) continue;

                    int worldX = center.getX() - offsetX + x;
                    if (!level.getBlockState(new BlockPos(worldX, y, worldZ)).blocksMotion()) {
                        continue;
                    }

                    obstructions += (val == 2) ? 100 : 1;
                    if (obstructions > 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Rite of Bind Circle to Talisman — reads the surrounding chalk circles and encodes them into
     * a blank Circle Talisman dropped at the center. Ingredient: blank CircleTalisman.
     * Trigger: any present circle stack; the old portable rite was not tied to one fixed ring size.
     */
    private static boolean riteOfBindCircleToTalisman(Player player, Level level, BlockPos center) {
        if (level.isClientSide()) return false;

        int[] variants = RitualCircleDetector.detectAllVariants(level, center);
        int data = (variants[2] << 6) | (variants[1] << 3) | variants[0];
        if (data == 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.unknownritual"), false);
            return false;
        }
        ItemStack talisman = new ItemStack(WitcheryItems.CIRCLE_TALISMAN.get());
        CircleTalismanItem.setCircleData(talisman, data);

        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5, center.getY() + 0.1, center.getZ() + 0.5, talisman);
        level.addFreshEntity(entity);

        success(player, level, center, "witchery.ritual.bind_circle.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 4, 48);
        WitcheryMod.LOGGER.info("Rite of Bind Circle: encoded data {} for {}",
                data, player.getName().getString());
        return true;
    }

    /** Rite of Glyphic Transformation — swaps one ring size to the dropped chalk family. */
    private static boolean riteOfGlyphicTransformation(Player player, Level level, BlockPos center, ItemStack ingredient) {
        if (!(level instanceof ServerLevel sl)) return false;
        Block targetBlock = glyphicTransformationTargetBlock(ingredient);
        if (targetBlock == null) {
            return false;
        }

        int size = glyphicTransformationSize(sl, center, ingredient);
        if (size <= 0) {
            return false;
        }

        int changed = transformGlyphicRing(sl, center, size, targetBlock);
        success(player, level, center, "witchery.ritual.glyphic_transform.success",
                SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 2, 32);
        WitcheryMod.LOGGER.info("Rite of Glyphic Transformation changed {} marks on size {} ring at {}", changed, size, center);
        return true;
    }

    /** Rite of Necromancy — infuses an empty Spectral Stone at night through a representative ritual-dust route. */
    private static boolean riteOfNecromancy(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireNight(player, level, center)) {
            return false;
        }
        Optional<SpectralStoneOfferings> offerings = findSpectralStoneOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.spectral_stone.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 6000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.spectral_stone.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().necroStone());
        consumeOfferedItem(offerings.get().congealedSpirit());
        consumeOfferedItem(offerings.get().condensedFear());
        consumeOfferedItem(offerings.get().boline());

        ItemStack output = new ItemStack(WitcheryItems.SPECTRAL_STONE.get());
        SpectralStoneItem.clearBinding(output);
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.15, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, "witchery.ritual.spectral_stone.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Necromancy created a Spectral Stone at {}", center);
        return true;
    }

    /** Rite of Binding — binds up to three nearby spectral creatures into a Spectral Stone on the medium ritual ring. */
    private static boolean riteOfBindSpectralStone(Player player, Level level, BlockPos center, ItemStack spectralStone) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (SpectralStoneItem.isBound(spectralStone)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.already"), false);
            return false;
        }
        Optional<BindSpectralOfferings> offerings = findBindSpectralOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.support"), false);
            return false;
        }

        SpectralStoneItem.StoredType storedType = findNearestSpectralType(sl, center, 5.0);
        if (storedType == SpectralStoneItem.StoredType.NONE) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.none"), false);
            return false;
        }
        int available = countNearbySpectralCreatures(sl, center, storedType, 5.0, 3);
        if (available <= 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.none"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 5000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.no_power"), false);
            return false;
        }
        int count = consumeNearbySpectralCreatures(sl, center, storedType, 5.0, 3);
        if (count <= 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.none"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().spectralDust());
        consumeOfferedItem(offerings.get().boline());

        ItemStack output = spectralStone.copy();
        output.setCount(1);
        SpectralStoneItem.bind(output, storedType, count);

        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.3, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, null, SoundEvents.EVOKER_PREPARE_SUMMON, 1.1f, 3, 40);
        player.displayClientMessage(Component.translatable(
                "witchery.ritual.bind_spectral.success",
                count,
                Component.translatable(storedType.translationKey())), false);
        WitcheryMod.LOGGER.info("Rite of Binding stored {} {} entities into a Spectral Stone at {}",
                count, storedType.name().toLowerCase(), center);
        return true;
    }

    /** Rite of Infusion — creates a Mystic Branch from Ent Twig and Mystic Unguent at night. */
    private static boolean riteOfInfuseMysticBranch(Player player, Level level, BlockPos center) {
        if (!requireNight(player, level, center)) {
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<MysticBranchOfferings> offerings = findMysticBranchOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_tree.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_tree.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().entTwig());
        consumeOfferedItem(offerings.get().mysticUnguent());

        ItemStack output = new ItemStack(WitcheryItems.MYSTIC_BRANCH.get());
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.25, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, "witchery.ritual.infusion_tree.success",
                SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
        WitcheryMod.LOGGER.info("Rite of Infusion created a Mystic Branch at {}", center);
        return true;
    }

    /** Rite of Infusion — creates a Crystal Ball from Quartz Sphere, gold, and Happenstance Oil at night. */
    private static boolean riteOfInfuseCrystalBall(Player player, Level level, BlockPos center) {
        if (!requireNight(player, level, center)) {
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<CrystalBallOfferings> offerings = findCrystalBallOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_future.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_future.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().quartzSphere());
        consumeOfferedItem(offerings.get().goldIngot());
        consumeOfferedItem(offerings.get().happenstanceOil());

        ItemStack output = new ItemStack(WitcheryItems.CRYSTAL_BALL.get());
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.25, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, "witchery.ritual.infusion_future.success",
                SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
        WitcheryMod.LOGGER.info("Rite of Infusion created a Crystal Ball at {}", center);
        return true;
    }

    /** Rite of Infusion — creates a Seer Stone from Quartz Sphere, mossy cobblestone, and Happenstance Oil at night. */
    private static boolean riteOfInfuseSeerStone(Player player, Level level, BlockPos center) {
        if (!requireNight(player, level, center)) {
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SeerStoneOfferings> offerings = findSeerStoneOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_seer.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.infusion_seer.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().quartzSphere());
        consumeOfferedItem(offerings.get().mossyCobblestone());
        consumeOfferedItem(offerings.get().happenstanceOil());

        ItemStack output = new ItemStack(WitcheryItems.SEER_STONE.get());
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.25, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, "witchery.ritual.infusion_seer.success",
                SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
        WitcheryMod.LOGGER.info("Rite of Infusion created a Seer Stone at {}", center);
        return true;
    }

    /** Shared infused-brew rite trigger — chooses the original Soaring or Grave branch by offerings and sacrifice. */
    private static boolean riteOfInfuseBrew(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;

        Optional<InfuseBrewSoaringOfferings> soaringOfferings = findInfuseBrewSoaringOfferings(level, center);
        if (soaringOfferings.isPresent()) {
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.ritual.infuse_brew.no_power"), false);
                return false;
            }

            consumeOfferedItem(soaringOfferings.get().infusedBrewBase());
            consumeOfferedItem(soaringOfferings.get().broom());
            consumeOfferedItem(soaringOfferings.get().feather());
            consumeOfferedItem(soaringOfferings.get().arthana());
            soaringOfferings.get().owl().discard();

            ItemEntity drop = new ItemEntity(sl,
                    center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                    new ItemStack(WitcheryItems.BREW_OF_SOARING.get()));
            drop.setDeltaMovement(0.0, 0.25, 0.0);
            sl.addFreshEntity(drop);

            success(player, level, center, "witchery.ritual.infuse_brew_soaring.success",
                    SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
            WitcheryMod.LOGGER.info("Rite of Infusion created Brew of Soaring at {}", center);
            return true;
        }

        Optional<InfuseBrewGraveOfferings> graveOfferings = findInfuseBrewGraveOfferings(level, center);
        if (graveOfferings.isPresent()) {
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.ritual.infuse_brew.no_power"), false);
                return false;
            }

            consumeOfferedItem(graveOfferings.get().infusedBrewBase());
            consumeOfferedItem(graveOfferings.get().bone());
            consumeOfferedItem(graveOfferings.get().denseWeb());
            consumeOfferedItem(graveOfferings.get().necroStone());
            graveOfferings.get().zombie().discard();

            ItemEntity drop = new ItemEntity(sl,
                    center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                    new ItemStack(WitcheryItems.BREW_OF_GRAVE.get()));
            drop.setDeltaMovement(0.0, 0.25, 0.0);
            sl.addFreshEntity(drop);

            success(player, level, center, "witchery.ritual.infuse_brew_grave.success",
                    SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
            WitcheryMod.LOGGER.info("Rite of Infusion created Brew of the Grave at {}", center);
            return true;
        }

        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        player.displayClientMessage(Component.translatable("witchery.ritual.infuse_brew.support"), false);
        return false;
    }

    private static Block glyphicTransformationTargetBlock(ItemStack ingredient) {
        if (ingredient.is(WitcheryItems.CHALK.get())) {
            return WitcheryBlocks.CIRCLE_GLYPH.get();
        }
        if (ingredient.is(WitcheryItems.CHALK_OTHERWHERE.get())) {
            return WitcheryBlocks.CIRCLE_OTHERWHERE.get();
        }
        if (ingredient.is(WitcheryItems.CHALK_INFERNAL.get())) {
            return WitcheryBlocks.CIRCLE_INFERNAL.get();
        }
        return null;
    }

    private static int glyphicTransformationSize(ServerLevel level, BlockPos center, ItemStack ingredient) {
        AABB area = new AABB(center.above()).inflate(4.0, 2.0, 4.0);
        int count = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = itemEntity.getItem();
            if (ItemStack.isSameItemSameComponents(stack, ingredient)) {
                count += stack.getCount();
            }
        }
        return Math.min(count, 3);
    }

    private static SpectralStoneItem.StoredType findNearestSpectralType(ServerLevel level, BlockPos center, double radius) {
        Vec3 centerVec = Vec3.atCenterOf(center);
        double radiusSquared = radius * radius;
        double nearestDistance = Double.MAX_VALUE;
        SpectralStoneItem.StoredType nearestType = SpectralStoneItem.StoredType.NONE;
        AABB area = new AABB(center).inflate(radius);

        for (com.emoniph.witchery.entity.SpectreEntity spectre : level.getEntitiesOfClass(com.emoniph.witchery.entity.SpectreEntity.class, area)) {
            double distance = spectre.distanceToSqr(centerVec);
            if (distance <= radiusSquared && distance < nearestDistance) {
                nearestDistance = distance;
                nearestType = SpectralStoneItem.StoredType.SPECTRE;
            }
        }
        for (com.emoniph.witchery.entity.BansheeEntity banshee : level.getEntitiesOfClass(com.emoniph.witchery.entity.BansheeEntity.class, area)) {
            double distance = banshee.distanceToSqr(centerVec);
            if (distance <= radiusSquared && distance < nearestDistance) {
                nearestDistance = distance;
                nearestType = SpectralStoneItem.StoredType.BANSHEE;
            }
        }
        for (com.emoniph.witchery.entity.PoltergeistEntity poltergeist : level.getEntitiesOfClass(com.emoniph.witchery.entity.PoltergeistEntity.class, area)) {
            double distance = poltergeist.distanceToSqr(centerVec);
            if (distance <= radiusSquared && distance < nearestDistance) {
                nearestDistance = distance;
                nearestType = SpectralStoneItem.StoredType.POLTERGEIST;
            }
        }

        return nearestType;
    }

    private static int consumeNearbySpectralCreatures(ServerLevel level, BlockPos center, SpectralStoneItem.StoredType type, double radius, int maxCount) {
        AABB area = new AABB(center).inflate(radius);
        Vec3 centerVec = Vec3.atCenterOf(center);
        double radiusSquared = radius * radius;

        List<? extends Entity> candidates = switch (type) {
            case SPECTRE -> level.getEntitiesOfClass(com.emoniph.witchery.entity.SpectreEntity.class, area);
            case BANSHEE -> level.getEntitiesOfClass(com.emoniph.witchery.entity.BansheeEntity.class, area);
            case POLTERGEIST -> level.getEntitiesOfClass(com.emoniph.witchery.entity.PoltergeistEntity.class, area);
            default -> List.of();
        };

        List<Entity> sorted = new ArrayList<>();
        for (Entity candidate : candidates) {
            if (candidate.distanceToSqr(centerVec) <= radiusSquared) {
                sorted.add(candidate);
            }
        }
        sorted.sort((left, right) -> Double.compare(left.distanceToSqr(centerVec), right.distanceToSqr(centerVec)));

        int count = 0;
        for (Entity entity : sorted) {
            entity.discard();
            level.playSound(null, entity.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 0.4f, 1.4f);
            level.sendParticles(ParticleTypes.PORTAL,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    12, 0.2, 0.2, 0.2, 0.05);
            count++;
            if (count >= maxCount) {
                break;
            }
        }
        return count;
    }

    private static int countNearbySpectralCreatures(ServerLevel level, BlockPos center, SpectralStoneItem.StoredType type, double radius, int maxCount) {
        AABB area = new AABB(center).inflate(radius);
        Vec3 centerVec = Vec3.atCenterOf(center);
        double radiusSquared = radius * radius;

        List<? extends Entity> candidates = switch (type) {
            case SPECTRE -> level.getEntitiesOfClass(com.emoniph.witchery.entity.SpectreEntity.class, area);
            case BANSHEE -> level.getEntitiesOfClass(com.emoniph.witchery.entity.BansheeEntity.class, area);
            case POLTERGEIST -> level.getEntitiesOfClass(com.emoniph.witchery.entity.PoltergeistEntity.class, area);
            default -> List.of();
        };

        int count = 0;
        for (Entity candidate : candidates) {
            if (candidate.distanceToSqr(centerVec) <= radiusSquared) {
                count++;
                if (count >= maxCount) {
                    break;
                }
            }
        }
        return count;
    }

    private static int transformGlyphicRing(ServerLevel level, BlockPos center, int size, Block targetBlock) {
        int radius = switch (size) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 0;
        };
        if (radius == 0) {
            return 0;
        }

        int changed = 0;
        BlockPos markLevel = center.above();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                    continue;
                }

                BlockPos targetPos = markLevel.offset(dx, 0, dz);
                BlockState state = level.getBlockState(targetPos);
                if ((state.is(WitcheryBlocks.CIRCLE_GLYPH.get())
                        || state.is(WitcheryBlocks.CIRCLE_OTHERWHERE.get())
                        || state.is(WitcheryBlocks.CIRCLE_INFERNAL.get()))
                        && !state.is(targetBlock)) {
                    level.setBlockAndUpdate(targetPos, targetBlock.defaultBlockState());
                    level.sendParticles(ParticleTypes.SMOKE,
                            targetPos.getX() + 0.5, targetPos.getY() + 0.2, targetPos.getZ() + 0.5,
                            4, 0.15, 0.1, 0.15, 0.0);
                    changed++;
                }
            }
        }
        return changed;
    }

    /** Rite of Blindness — blinds all non-player living entities within 10 blocks for 60 seconds. */
    private static boolean riteOfBlindness(Player player, Level level, BlockPos center) {
        Optional<BlindnessOfferings> offerings = findBlindnessOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.blindness.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        int covenSize = countNearbyCovenWitches(level, center);
        boolean enhanced = player instanceof ServerPlayer sp && hasActiveCurseMasteryFamiliar(sp);
        if (!queueActiveRitual(sl, ActiveRitualType.BLINDNESS, RitualCircleDetector.CircleSize.SMALL,
                center, center, 0, 80, 77, 5, covenSize, 0.0f, 0, List.of(), enhanced)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        List<ActiveRitual> rituals = ACTIVE_RITUALS.get(sl.dimension());
        if (rituals != null) {
            for (ActiveRitual ritual : rituals) {
                if (ritual.center.equals(center) && ritual.type == ActiveRitualType.BLINDNESS) {
                    ritual.nextGameTime = sl.getGameTime() + 20L;
                    break;
                }
            }
        }
        consumeOfferedItem(offerings.get().chargedStone());
        consumeOfferedItem(offerings.get().redstoneSoup());
        consumeOfferedItem(offerings.get().reekOfMisfortune());
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().brewOfInk());
        consumeOfferedItem(offerings.get().poisonousPotato());
        consumeOfferedItem(offerings.get().spiderEye());
        consumeOfferedItem(offerings.get().diamond());
        success(player, level, center, "witchery.ritual.blindness.success",
                SoundEvents.BAT_AMBIENT, 0.5f, 3, 32);
        return true;
    }

    /** Rite of Blight — withers players, zombifies villagers, slays animals nearby. */
    private static boolean riteOfBlight(Player player, Level level, BlockPos center) {
        Optional<BlightOfferings> offerings = findBlightOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.blight.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        int covenSize = countNearbyCovenWitches(level, center);
        boolean enhanced = player instanceof ServerPlayer sp && hasActiveCurseMasteryFamiliar(sp);
        if (!queueActiveRitual(sl, ActiveRitualType.BLIGHT, RitualCircleDetector.CircleSize.MEDIUM,
                center, center, 0, 80, 77, 5, covenSize, 0.0f, 0, List.of(), enhanced)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        List<ActiveRitual> rituals = ACTIVE_RITUALS.get(sl.dimension());
        if (rituals != null) {
            for (ActiveRitual ritual : rituals) {
                if (ritual.center.equals(center) && ritual.type == ActiveRitualType.BLIGHT) {
                    ritual.nextGameTime = sl.getGameTime() + 20L;
                    break;
                }
            }
        }
        consumeOfferedItem(offerings.get().chargedStone());
        consumeOfferedItem(offerings.get().redstoneSoup());
        consumeOfferedItem(offerings.get().reekOfMisfortune());
        consumeOfferedItem(offerings.get().poisonousPotato());
        consumeOfferedItem(offerings.get().rottenFlesh());
        consumeOfferedItem(offerings.get().diamond());
        success(player, level, center, "witchery.ritual.blight.success",
                SoundEvents.WITHER_SPAWN, 0.5f, 2, 40);
        return true;
    }

    /** Rite of Rain of Toads — sets thunderstorm, strikes lightning, spawns toads. */
    private static boolean riteOfRainOfToads(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 1)) return false;
        Optional<RainOfToadsOfferings> offerings = findRainOfToadsOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.rain_of_toads.support"), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        if (!queueActiveRitual(sl, ActiveRitualType.RAIN_OF_TOADS, RitualCircleDetector.CircleSize.MEDIUM, center, 5, 16, 200, 30, covenSize)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        consumeOfferedItem(offerings.get().chargedStone());
        consumeOfferedItem(offerings.get().redstoneSoup());
        consumeOfferedItem(offerings.get().reekOfMisfortune());
        consumeOfferedItem(offerings.get().toeOfFrog());
        consumeOfferedItem(offerings.get().waterBucket());
        consumeOfferedItem(offerings.get().belladonna());
        success(player, level, center, "witchery.ritual.rain_of_toads.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 4, 40);
        return true;
    }

    /** Rite of Sky's Wrath storm family — original inside/outside/portable small-circle branches. */
    private static boolean riteOfWeatherCallStorm(Player player, Level level, BlockPos center, StormRiteVariant variant) {
        if (!(level instanceof ServerLevel sl)) return false;
        int covenSize = countNearbyCovenWitches(level, center);
        Optional<StormOfferings> offerings = findStormOfferings(level, center, variant, covenSize);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            String supportKey = variant == StormRiteVariant.PORTABLE
                    ? "witchery.ritual.storm.portable_support"
                    : "witchery.ritual.storm.support";
            player.displayClientMessage(Component.translatable(supportKey), false);
            return false;
        }
        if (offerings.get().tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        int requiredPower = variant == StormRiteVariant.BASE ? 1000 : variant == StormRiteVariant.LARGE ? 2000 : 0;
        if (requiredPower > 0 && !consumeNearbyRitualAltarPower(sl, center, 16, requiredPower)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.storm.no_power"), false);
            return false;
        }
        int minRadius = variant == StormRiteVariant.BASE ? 0 : 3;
        int maxRadius = variant == StormRiteVariant.BASE ? 3 : 7;
        int targetStage = variant == StormRiteVariant.BASE ? 8 : 18;
        if (!queueActiveRitual(sl, ActiveRitualType.WEATHER_CALL_STORM, RitualCircleDetector.CircleSize.SMALL,
                center, offerings.get().effectCenter(), minRadius, maxRadius, targetStage,
                RITUAL_PULSE_INTERVAL, covenSize, 0.0f, 0, List.of())) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        consumeOfferedItem(offerings.get().weatherFocus());
        consumeOfferedItem(offerings.get().woodAsh());
        if (offerings.get().chargedStone() != null) {
            consumeOfferedItem(offerings.get().chargedStone());
        }
        if (offerings.get().boundWaystone() != null) {
            consumeOfferedItem(offerings.get().boundWaystone());
        }
        success(player, level, center, "witchery.ritual.storm.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 4, 48);
        return true;
    }

    /** Rite of Ice Shell — grows a hollow sphere of ice around the circle over several pulses. */
    private static boolean riteOfIceShell(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 2)) return false;
        int covenSize = countNearbyCovenWitches(level, center);
        if (!queueActiveRitual(sl, ActiveRitualType.ICE_SHELL, RitualCircleDetector.CircleSize.SMALL,
                center, 8, 8, 250, 5, covenSize)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        List<ActiveRitual> rituals = ACTIVE_RITUALS.get(sl.dimension());
        if (rituals != null) {
            for (ActiveRitual ritual : rituals) {
                if (ritual.center.equals(center) && ritual.type == ActiveRitualType.ICE_SHELL) {
                    ritual.nextGameTime = sl.getGameTime() + 20L;
                    break;
                }
            }
        }

        success(player, level, center, "witchery.ritual.ice_shell.success",
                SoundEvents.GLASS_PLACE, 0.9f, 3, 40);
        return true;
    }

    /** Rite of Raising the Column — gradually lifts real terrain instead of stamping stone into the air. */
    private static boolean riteOfRaiseColumn(Player player, Level level, BlockPos center, RitualCircleDetector.CircleSize circleSize) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<RaiseColumnOfferings> offerings = findRaiseColumnOfferings(level, center, circleSize);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            String supportKey = switch (circleSize) {
                case SMALL -> "witchery.ritual.raise_column.small_support";
                case MEDIUM -> "witchery.ritual.raise_column.medium_support";
                case LARGE -> "witchery.ritual.raise_column.large_support";
            };
            player.displayClientMessage(Component.translatable(supportKey), false);
            return false;
        }

        int baseRadius = switch (circleSize) {
            case SMALL -> 4;
            case MEDIUM -> 6;
            case LARGE -> 9;
        };
        int height = 8;
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = circleSize == RitualCircleDetector.CircleSize.SMALL
                ? findOptionalWaystoneRelocation(level, center, covenSize)
                : new OptionalWaystoneRelocation(null, center, false);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();

        if (!queueActiveRitual(sl, ActiveRitualType.RAISE_COLUMN, circleSize, center,
                relocation.effectCenter(), baseRadius, height, height - 1, 5, covenSize, 0.0f, 0,
                supplementalSacrifices)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().brewOfSprouting());
        consumeOfferedItem(offerings.get().cactus());
        consumeOfferedItem(offerings.get().focus());
        if (relocation.boundWaystone() != null) {
            consumeOfferedItem(relocation.boundWaystone());
        }
        success(player, level, center, "witchery.ritual.raise_column.success",
                SoundEvents.STONE_BREAK, 0.8f, 3, 32);
        return true;
    }

    /** Rite of Earth's Wrath — raises a volcano if a lava pool lies below the circle. */
    private static boolean riteOfVolcano(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        BlockPos effectCenter = relocation.effectCenter();
        if (!hasVolcanoLavaPool(sl, effectCenter)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.missinglava"), false);
            return false;
        }

        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, ActiveRitualType.VOLCANO, RitualCircleDetector.CircleSize.LARGE, center,
                effectCenter, 8, 8, 0, 15, covenSize, 0.0f, 0, supplementalSacrifices)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        if (relocation.boundWaystone() != null) {
            consumeOfferedItem(relocation.boundWaystone());
        }

        success(player, level, center, "witchery.ritual.volcano.success",
                SoundEvents.LAVA_POP, 0.8f, 4, 40);
        return true;
    }

    /** Rite of Transpose Ores — lifts hidden ore blocks into the circle over time. */
    private static boolean riteOfTransposeOres(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<TransposeOresOfferings> offerings = findTransposeOresOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.transpose_ores.support"), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        int pulses = 30 + covenSize * 5;
        if (!queueActiveRitual(sl, ActiveRitualType.TRANSPOSE_ORES, RitualCircleDetector.CircleSize.LARGE,
                center, 8, 0, pulses, 10, covenSize)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().enderPearl());
        consumeOfferedItem(offerings.get().ironIngot());
        consumeOfferedItem(offerings.get().blazePowder());
        consumeOfferedItem(offerings.get().chargedStone());
        success(player, level, center, "witchery.ritual.transpose_ores.success",
                SoundEvents.END_PORTAL_FRAME_FILL, 0.9f, 4, 40);
        return true;
    }

    /** Rite of Nature's Power — gradually spreads fertile life around the circle. */
    private static boolean riteOfNaturesPower(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<NaturesPowerOfferings> offerings = findNaturesPowerOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.natures_power.support"), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        if (!queueActiveRitual(sl, ActiveRitualType.NATURES_POWER, RitualCircleDetector.CircleSize.MEDIUM,
                center, 14, 8, 150, 20, covenSize)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().rowanSapling());
        consumeOfferedItem(offerings.get().alderSapling());
        consumeOfferedItem(offerings.get().hawthornSapling());
        consumeOfferedItem(offerings.get().oakSapling());
        consumeOfferedItem(offerings.get().spruceSapling());
        consumeOfferedItem(offerings.get().birchSapling());
        consumeOfferedItem(offerings.get().chargedStone());
        success(player, level, center, "witchery.ritual.natures_power.success",
                SoundEvents.BONE_MEAL_USE, 1.1f, 4, 40);
        return true;
    }

    private static void raiseColumnLayer(ServerLevel level, BlockPos center, int y, int radius, boolean topLayer) {
        int radiusSq = radius * radius;
        int innerRadius = Math.max(radius - 1, 0);
        int innerRadiusSq = innerRadius * innerRadius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > radiusSq) {
                    continue;
                }

                boolean rimBlock = distSq >= innerRadiusSq;
                if (!topLayer && rimBlock && level.random.nextInt(7) == 0) {
                    continue;
                }

                raiseColumnBlock(level, center.offset(dx, y - center.getY(), dz));
            }
        }
    }

    private static int transposeOreLayer(ServerLevel level, BlockPos center, int y, int radius, boolean includeGold) {
        int transposed = 0;
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                BlockPos orePos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(orePos);
                if (!isTransposeOre(state, includeGold)) {
                    continue;
                }

                ItemStack output = new ItemStack(state.getBlock().asItem());
                if (output.isEmpty()) {
                    continue;
                }

                double spawnX = center.getX() - radius + level.random.nextInt(radius * 2 + 1) + 0.5;
                double spawnZ = center.getZ() - radius + level.random.nextInt(radius * 2 + 1) + 0.5;
                ItemEntity entity = new ItemEntity(level, spawnX, center.getY() + 2.05, spawnZ, output);
                entity.setDeltaMovement(0.0, 0.0, 0.0);
                level.removeBlock(orePos, false);
                level.addFreshEntity(entity);
                transposed++;
            }
        }
        return transposed;
    }

    private static BlockPos findNaturesPowerAnchor(ServerLevel level, BlockPos center, int radius, int height) {
        int x = center.getX() - radius + level.random.nextInt(radius * 2 + 1);
        int z = center.getZ() - radius + level.random.nextInt(radius * 2 + 1);
        int baseY = center.getY() - 1;

        for (int offset = 0; offset <= height; offset++) {
            BlockPos up = new BlockPos(x, baseY + offset, z);
            if (isNaturesPowerAnchor(level, up)) {
                return up;
            }
            if (offset == 0) {
                continue;
            }
            BlockPos down = new BlockPos(x, baseY - offset, z);
            if (isNaturesPowerAnchor(level, down)) {
                return down;
            }
        }
        return null;
    }

    private static boolean isNaturesPowerAnchor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.blocksMotion()) {
            return false;
        }
        return level.getBlockState(pos.above()).isAir();
    }

    private static void growNaturePatch(ServerLevel level, BlockPos center, int radius) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                growNatureColumn(level, center.offset(dx, 0, dz));
            }
        }
    }

    private static void growNatureColumn(ServerLevel level, BlockPos groundPos) {
        BlockState state = level.getBlockState(groundPos);
        BlockPos abovePos = groundPos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        if (canNaturesPowerGreen(state)) {
            if (hasAdjacentWater(level, groundPos) && level.random.nextDouble() < 0.7) {
                level.setBlock(groundPos, Blocks.MYCELIUM.defaultBlockState(), 3);
            } else {
                level.setBlock(groundPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
            state = level.getBlockState(groundPos);
        }

        if (aboveState.isAir() && level.random.nextInt(4) == 0) {
            placeNaturesPowerPlant(level, abovePos);
        }

        BlockState plantState = level.getBlockState(abovePos);
        if (plantState.getBlock() instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, abovePos, plantState)
                && bonemealable.isBonemealSuccess(level, level.random, abovePos, plantState)) {
            bonemealable.performBonemeal(level, level.random, abovePos, plantState);
        }
    }

    private static boolean canNaturesPowerGreen(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MUD)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL);
    }

    private static boolean hasAdjacentWater(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(net.minecraft.tags.FluidTags.WATER)
                    || level.getFluidState(pos.above().relative(direction)).is(net.minecraft.tags.FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static void placeNaturesPowerPlant(ServerLevel level, BlockPos pos) {
        BlockState plant = switch (level.random.nextInt(10)) {
            case 0 -> Blocks.SHORT_GRASS.defaultBlockState();
            case 1 -> Blocks.FERN.defaultBlockState();
            case 2 -> Blocks.DANDELION.defaultBlockState();
            case 3 -> Blocks.POPPY.defaultBlockState();
            case 4 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
            case 5 -> Blocks.RED_MUSHROOM.defaultBlockState();
            case 6 -> WitcheryBlocks.EMBER_MOSS.get().defaultBlockState();
            case 7 -> WitcheryBlocks.GLINT_WEED.get().defaultBlockState();
            case 8 -> WitcheryBlocks.ROWAN_SAPLING.get().defaultBlockState();
            default -> Blocks.TALL_GRASS.defaultBlockState();
        };

        if (plant.canSurvive(level, pos)) {
            level.setBlock(pos, plant, 3);
        }
    }

    private static BlockPos findForestationAnchor(ServerLevel level, BlockPos center, int radius, int height) {
        int radiusSq = (radius + 1) * (radius + 1);
        int x = center.getX() - radius + level.random.nextInt(radius * 2 + 1);
        int z = center.getZ() - radius + level.random.nextInt(radius * 2 + 1);
        if (Mth.square(x - center.getX()) + Mth.square(z - center.getZ()) > radiusSq) {
            x = center.getX() - radius + level.random.nextInt(radius * 2 + 1);
            z = center.getZ() - radius + level.random.nextInt(radius * 2 + 1);
            if (Mth.square(x - center.getX()) + Mth.square(z - center.getZ()) > radiusSq) {
                return null;
            }
        }

        int baseY = center.getY() - 1;
        BlockPos direct = new BlockPos(x, baseY, z);
        if (isForestationAnchor(level, direct)) {
            return direct;
        }

        for (int offset = 1; offset < height; offset++) {
            BlockPos up = new BlockPos(x, baseY + offset, z);
            if (isForestationAnchor(level, up)) {
                return up;
            }
            BlockPos down = new BlockPos(x, baseY - offset, z);
            if (isForestationAnchor(level, down)) {
                return down;
            }
        }
        return null;
    }

    private static boolean isForestationAnchor(ServerLevel level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos);
        if (!ground.blocksMotion()) {
            return false;
        }
        return level.getBlockState(pos.above()).canBeReplaced();
    }

    private static boolean seedForestationTree(ServerLevel level, BlockPos plantPos, BlockState saplingState) {
        BlockState state = level.getBlockState(plantPos);
        if (!state.is(saplingState.getBlock())) {
            if (!state.canBeReplaced() || !saplingState.canSurvive(level, plantPos)) {
                return false;
            }
            level.setBlock(plantPos, saplingState, 3);
        }

        if (!(saplingState.getBlock() instanceof SaplingBlock sapling)) {
            return true;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            BlockState current = level.getBlockState(plantPos);
            if (!current.is(sapling)) {
                return true;
            }
            if (sapling.isBonemealSuccess(level, level.random, plantPos, current)) {
                sapling.performBonemeal(level, level.random, plantPos, current);
            }
        }
        return true;
    }

    private static boolean isTransposeOre(BlockState state, boolean includeGold) {
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
            return true;
        }
        return includeGold && (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE));
    }

    private static void raiseColumnBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canRaiseColumnMove(level, pos, state)) {
            return;
        }

        BlockPos above = pos.above();
        BlockPos below = pos.below();
        if (!canRaiseColumnPass(level, above) || !canRaiseColumnPass(level, below)) {
            return;
        }

        level.setBlock(above, state, 2);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static boolean canRaiseColumnMove(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.liquid() || state.hasBlockEntity()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private static boolean canRaiseColumnPass(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.liquid() || state.hasBlockEntity()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private static void liftRaiseColumnEntities(ServerLevel level, BlockPos center, int radius, int airSpace) {
        AABB bounds = new AABB(
                center.getX() - radius, center.getY(), center.getZ() - radius,
                center.getX() + radius + 1.0, center.getY() + airSpace + 1.0, center.getZ() + radius + 1.0
        );
        double radiusSq = radius * radius;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            double dx = entity.getX() - center.getX();
            double dz = entity.getZ() - center.getZ();
            if (dx * dx + dz * dz <= radiusSq) {
                entity.teleportTo(entity.getX(), entity.getY() + 1.0, entity.getZ());
            }
        }
    }

    private static boolean hasVolcanoLavaPool(ServerLevel level, BlockPos center) {
        for (int y = center.getY(); y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(center.getX(), y, center.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LAVA) && isVolcanoLavaCluster(level, pos, 2)) {
                return true;
            }
            if (state.is(Blocks.BEDROCK)) {
                break;
            }
        }
        return false;
    }

    private static boolean isVolcanoLavaCluster(ServerLevel level, BlockPos center, int requiredNeighbors) {
        int count = 0;
        count += level.getBlockState(center.below()).is(Blocks.LAVA) ? 1 : 0;
        count += level.getBlockState(center.north()).is(Blocks.LAVA) ? 1 : 0;
        count += level.getBlockState(center.south()).is(Blocks.LAVA) ? 1 : 0;
        count += level.getBlockState(center.east()).is(Blocks.LAVA) ? 1 : 0;
        count += level.getBlockState(center.west()).is(Blocks.LAVA) ? 1 : 0;
        count += level.getBlockState(center.above()).is(Blocks.LAVA) ? 1 : 0;
        return count >= requiredNeighbors;
    }

    private static void drawVolcanoFilledCircle(ServerLevel level, BlockPos center, int y, int radius, int heightStage, boolean replaceBlocks) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;
        while (x >= z) {
            drawVolcanoLine(level, center, -x, x, z, y, radius, heightStage, replaceBlocks);
            drawVolcanoLine(level, center, -z, z, x, y, radius, heightStage, replaceBlocks);
            drawVolcanoLine(level, center, -x, x, -z, y, radius, heightStage, replaceBlocks);
            drawVolcanoLine(level, center, -z, z, -x, y, radius, heightStage, replaceBlocks);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void drawVolcanoLine(ServerLevel level, BlockPos center, int startX, int endX, int zOffset, int y,
                                        int radius, int heightStage, boolean replaceBlocks) {
        int modStart = radius > 1 && level.random.nextInt(5) == 0 ? startX + 1 : startX;
        int modEnd = radius > 1 && level.random.nextInt(5) == 0 ? endX - 1 : endX;
        boolean edgeZ = radius == Math.abs(zOffset);
        for (int xOffset = modStart; xOffset <= modEnd; xOffset++) {
            if (xOffset == 0 && zOffset == 0) {
                continue;
            }
            boolean lower = (xOffset == modStart || xOffset == modEnd || edgeZ) && heightStage < 3;
            drawVolcanoPixel(level, center.offset(xOffset, y - center.getY(), zOffset), lower, replaceBlocks);
        }
    }

    private static void drawVolcanoPixel(ServerLevel level, BlockPos pos, boolean lower, boolean replaceBlocks) {
        BlockState state = level.getBlockState(pos);
        if (!canVolcanoReplace(level, pos, state, replaceBlocks)) {
            return;
        }
        BlockState fillState = lower && level.random.nextInt(5) != 0
                ? Blocks.SANDSTONE.defaultBlockState()
                : Blocks.SAND.defaultBlockState();
        level.setBlock(pos, fillState, 3);
    }

    private static boolean canVolcanoReplace(ServerLevel level, BlockPos pos, BlockState state, boolean replaceBlocks) {
        if (state.isAir()) {
            return true;
        }
        if (!replaceBlocks) {
            return false;
        }
        return !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0f
                && (state.canBeReplaced() || state.getFluidState().isEmpty() || state.is(Blocks.LAVA));
    }

    private static void liftVolcanoEntities(ServerLevel level, BlockPos center, int y, int radius) {
        AABB bounds = new AABB(center.getX() - radius, y, center.getZ() - radius,
                center.getX() + radius, y + 1.0, center.getZ() + radius);
        double radiusSq = radius * radius;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            double dx = entity.getX() - (center.getX() + 0.5);
            double dz = entity.getZ() - (center.getZ() + 0.5);
            if (dx * dx + dz * dz > radiusSq) {
                continue;
            }
            if (!level.getBlockState(entity.blockPosition()).blocksMotion()) {
                continue;
            }
            entity.teleportTo(entity.getX(), entity.getY() + 1.0, entity.getZ());
        }
    }

    private static void openVolcanoVent(ServerLevel level, BlockPos center) {
        for (int y = center.getY(); y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(center.getX(), y, center.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BEDROCK)) {
                break;
            }
            if (state.is(Blocks.LAVA)) {
                while (y > level.getMinY() && level.getBlockState(new BlockPos(center.getX(), y, center.getZ())).is(Blocks.LAVA)) {
                    clearVolcanoLava(level, new BlockPos(center.getX(), y, center.getZ()));
                    clearVolcanoLava(level, new BlockPos(center.getX() + 1, y, center.getZ()));
                    clearVolcanoLava(level, new BlockPos(center.getX() - 1, y, center.getZ()));
                    clearVolcanoLava(level, new BlockPos(center.getX(), y, center.getZ() + 1));
                    clearVolcanoLava(level, new BlockPos(center.getX(), y, center.getZ() - 1));
                    y--;
                }
                break;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void clearVolcanoLava(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.LAVA)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean pushProtectionMob(Mob mob, BlockPos center, double radius) {
        double dx = center.getX() + 0.5 - mob.getX();
        double dy = center.getY() + 0.5 - mob.getY();
        double dz = center.getZ() + 0.5 - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist >= radius || dist < 0.001) {
            return false;
        }

        double scale = 0.22 / dist;
        double motionX = Mth.clamp(-dx * scale, -0.22, 0.22);
        double motionY = Mth.clamp(-dy * scale, -0.10, 0.12);
        double motionZ = Mth.clamp(-dz * scale, -0.22, 0.22);
        mob.push(motionX, motionY, motionZ);
        mob.hurtMarked = true;
        return true;
    }

    private static boolean pullProtectionMob(Mob mob, BlockPos center, double radius) {
        double nextX = mob.getX() + mob.getDeltaMovement().x;
        double nextY = mob.getY() + mob.getDeltaMovement().y;
        double nextZ = mob.getZ() + mob.getDeltaMovement().z;
        double dx = center.getX() + 0.5 - nextX;
        double dy = center.getY() + 0.5 - nextY;
        double dz = center.getZ() + 0.5 - nextZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < radius - 1.0 || dist < 0.001) {
            return false;
        }

        double scale = 0.22 / dist;
        double motionX = Mth.clamp(dx * scale, -0.22, 0.22);
        double motionZ = Mth.clamp(dz * scale, -0.22, 0.22);
        mob.setDeltaMovement(motionX, 0.0, motionZ);
        mob.hurtMarked = true;
        return true;
    }

    /** Rite of Cursing Creatures — applies Wither, Slowness and Weakness to nearby mobs. */
    private static boolean riteOfCurseCreature(Player player, Level level, BlockPos center) {
        if (!requireThunderstorm(player, level, center)) return false;
        Optional<CurseCreatureOfferings> offerings = findCurseCreatureOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.curse_creature.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.curse_creature.no_power"), false);
            return false;
        }
        AABB aabb = new AABB(center).inflate(10);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (entity instanceof Player) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            count++;
        }
        consumeOfferedItem(offerings.get().taglockKit());
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().spiderEye());
        consumeOfferedItem(offerings.get().gunpowder());
        consumeOfferedItem(offerings.get().brewGrotesque());
        success(player, level, center, "witchery.ritual.curse_creature.success",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 32);
        WitcheryMod.LOGGER.info("Rite of Curse Creature afflicted {} entities", count);
        return true;
    }

    /** Curse of Corrupt Poppet — destroys bound voodoo-protection poppets on the taglock target. */
    private static boolean riteOfCorruptPoppet(Player caster, Level level, BlockPos center) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        if (!hasActiveCurseMasteryFamiliar(sp)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.requirescursemastery"), false);
            return false;
        }

        Optional<InventoryTaglock> taglock = findFilledTaglock(sp);
        if (taglock.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.no_taglock"), false);
            return false;
        }

        ServerPlayer target = ((ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglock.get().targetUuid());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.offline"), false);
            return false;
        }

        Optional<CorruptPoppetOfferings> offerings = findCorruptPoppetOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.support"), false);
            return false;
        }

        if (PoppetEvents.damageBoundPoppet(target, WitcheryItems.POPPET_PROTECTION_POPPET.get(), 1000)) {
            target.displayClientMessage(Component.translatable("witchery.poppet.shield.activated"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            strikeCurseBacklashLightning(sp);
            success(sp, level, center, null, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 2, 32);
            return true;
        }

        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 7000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.no_power"), false);
            return false;
        }

        consumeInventoryItem(sp, taglock.get().slot());
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().blazePowder());
        consumeOfferedItem(offerings.get().spectralDust());
        int destroyed = PoppetEvents.destroyBoundPoppets(target, WitcheryItems.VOODOO_PROTECTION_POPPET.get(), 1000, 10);
        success(sp, level, center, null, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 2, 32);
        sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.success", destroyed), false);
        WitcheryMod.LOGGER.info("Curse of Corrupt Poppet destroyed {} voodoo protection poppet(s) for {}", destroyed, target.getName().getString());
        return true;
    }

    /** Rite of Calling the Familiar — teleports the player's familiar to the center. */
    private static boolean riteOfCallFamiliar(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<CallFamiliarOfferings> offerings = findCallFamiliarOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.call_familiar.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 1000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.call_familiar.no_power"), false);
            return false;
        }

        WitcheryPlayerData familiarData = WitcheryAttachments.get(sp);
        double x = center.getX() + 0.5;
        double y = center.getY() + 1.0;
        double z = center.getZ() + 0.5;

        TamableAnimal familiar = FamiliarHelper.findBoundFamiliar(sp);
        if (familiar == null) {
            familiar = FamiliarHelper.restoreBoundFamiliar(sp, sl, x, y, z);
            if (familiar == null) {
                player.displayClientMessage(Component.translatable("witchery.ritual.call_familiar.none"), false);
                return false;
            }
        }

        java.util.UUID familiarUuid = familiar.getUUID();
        String familiarType = FamiliarHelper.describeFamiliar(familiar);
        String familiarName = familiar.hasCustomName() ? familiar.getCustomName().getString() : "";
        boolean familiarSitting = familiar.isOrderedToSit();
        boolean familiarNameVisible = familiar.isCustomNameVisible();
        boolean familiarChanged = familiarData.getFamiliarUUID().map(uuid -> !uuid.equals(familiarUuid)).orElse(true)
                || !familiarType.equals(familiarData.getFamiliarType())
                || !familiarName.equals(familiarData.getFamiliarName())
                || familiarSitting != familiarData.isFamiliarSitting()
                || familiarNameVisible != familiarData.isFamiliarNameVisible();
        if (familiarChanged) {
            familiarData.setFamiliarUUID(familiarUuid);
            familiarData.setFamiliarType(familiarType);
            familiarData.setFamiliarName(familiarName);
            familiarData.setFamiliarSitting(familiarSitting);
            familiarData.setFamiliarNameVisible(familiarNameVisible);
            PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, familiarData));
        }

        if (familiar.level() == sl) {
            familiar.teleportTo(x, y, z);
        } else {
            familiar.teleportTo(sl, x, y, z,
                    Set.of(), familiar.getYRot(), familiar.getXRot(), false);
        }
        familiar.setOrderedToSit(familiarData.isFamiliarSitting());

        consumeOfferedItem(offerings.get().breathOfTheGoddess());
        consumeOfferedItem(offerings.get().hintOfRebirth());
        consumeOfferedItem(offerings.get().whiffOfMagic());

        success(player, level, center, "witchery.ritual.call_familiar.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 2, 20);
        return true;
    }

    private static TamableAnimal findBindableFamiliarCandidate(ServerPlayer player, ServerLevel level, BlockPos center, double radius) {
        AABB search = new AABB(center).inflate(radius, 1.0, radius);
        List<TamableAnimal> tameables = level.getEntitiesOfClass(TamableAnimal.class, search);
        TamableAnimal best = null;
        double bestDistance = Double.MAX_VALUE;

        for (TamableAnimal tameable : tameables) {
            if (!canBecomeBoundFamiliar(player, tameable, center, radius)) {
                continue;
            }

            double distance = tameable.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = tameable;
            }
        }

        return best;
    }

    private static boolean hasBindableFamiliarCandidate(ServerLevel level, BlockPos center, double radius) {
        AABB playerBounds = new AABB(center).inflate(radius, 1.0, radius);
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, playerBounds);
        for (ServerPlayer nearbyPlayer : nearbyPlayers) {
            if (nearbyPlayer.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)
                    > radius * radius) {
                continue;
            }
            if (findBindableFamiliarCandidate(nearbyPlayer, level, center, radius) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean canBecomeBoundFamiliar(ServerPlayer player, TamableAnimal tameable, BlockPos center, double radius) {
        if (!tameable.isTame() || tameable.isRemoved() || !tameable.isAlive()) {
            return false;
        }
        if (!tameable.isOwnedBy(player)) {
            return false;
        }
        if (!(tameable instanceof FamiliarEntity) && tameable.getType() != EntityType.CAT) {
            return false;
        }
        return tameable.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) <= radius * radius;
    }

    private static TamableAnimal bindFamiliarToPlayer(ServerPlayer player, ServerLevel level, TamableAnimal tameable) {
        TamableAnimal bound = tameable;
        if (tameable.getType() == EntityType.CAT && !(tameable instanceof FamiliarEntity)) {
            CatFamiliarEntity converted = WitcheryEntities.FAMILIAR_CAT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
            if (converted == null) {
                return null;
            }
            converted.setPos(tameable.getX(), tameable.getY(), tameable.getZ());
            converted.setYRot(tameable.getYRot());
            converted.setXRot(tameable.getXRot());
            converted.tame(player);
            converted.setOrderedToSit(tameable.isOrderedToSit());
            if (tameable.hasCustomName()) {
                converted.setCustomName(tameable.getCustomName());
                converted.setCustomNameVisible(tameable.isCustomNameVisible());
            }
            if (tameable.isPersistenceRequired()) {
                converted.setPersistenceRequired();
            }
            level.addFreshEntity(converted);
            tameable.discard();
            bound = converted;
        } else if (tameable instanceof FamiliarEntity familiar) {
            familiar.tame(player);
            familiar.setPersistenceRequired();
            bound = familiar;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        data.setFamiliarUUID(bound.getUUID());
        data.setFamiliarType(FamiliarHelper.describeFamiliar(bound));
        data.setFamiliarName(bound.hasCustomName() ? bound.getCustomName().getString() : "");
        data.setFamiliarSitting(bound.isOrderedToSit());
        data.setFamiliarNameVisible(bound.isCustomNameVisible());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return bound;
    }

    /** Rite of Summon Witch — summons a hostile witch if the circle is not obstructed. */
    private static boolean riteOfSummonWitch(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SummonWitchOfferings> offerings = findSummonWitchOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_witch.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_witch.no_power"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        net.minecraft.world.entity.monster.Witch witch = EntityType.WITCH.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (witch == null) {
            return false;
        }

        witch.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        witch.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(witch);

        consumeOfferedItem(offerings.get().diamondVapour());
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().magmaCream());
        consumeOfferedItem(offerings.get().arthana());
        consumeOfferedItem(offerings.get().fermentedSpiderEye());
        success(player, level, center, "witchery.ritual.summon_witch.success",
                SoundEvents.WITCH_CELEBRATE, 0.8f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Summon Witch cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Imp — summons a hostile imp, but only with a coven present. */
    private static boolean riteOfSummonImp(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 1)) return false;
        Optional<SummonImpOfferings> offerings = findSummonImpOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_imp.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 5000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_imp.no_power"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var imp = WitcheryEntities.FLAME_IMP.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (imp == null) {
            return false;
        }

        imp.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        imp.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(imp);

        consumeOfferedItem(offerings.get().refinedEvil());
        consumeOfferedItem(offerings.get().infernalBlood());
        consumeOfferedItem(offerings.get().enderPearl());
        consumeOfferedItem(offerings.get().attunedStone());
        success(player, level, center, "witchery.ritual.summon_imp.success",
                SoundEvents.BLAZE_AMBIENT, 0.7f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Summon Imp cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Wither — summons a wither from the large infernal ring when the inner medium infernal ring is also present. */
    private static boolean riteOfSummonWither(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SummonWitherOfferings> offerings = findSummonWitherOfferings(level, center);
        Optional<ExpensiveSummonWitherOfferings> expensiveOfferings = findExpensiveSummonWitherOfferings(level, center);
        boolean useExpensive = expensiveOfferings.isPresent();
        if (offerings.isEmpty() && expensiveOfferings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_wither.support"), false);
            return false;
        }
        if (!useExpensive && !consumeNearbyRitualAltarPower(sl, center, 16, 4000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_wither.no_power"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var wither = EntityType.WITHER.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (wither == null) {
            return false;
        }

        wither.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        wither.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(wither);

        if (useExpensive) {
            consumeOfferedItem(expensiveOfferings.get().diamond());
            consumeOfferedItem(expensiveOfferings.get().enderPearl());
            consumeOfferedItem(expensiveOfferings.get().attunedStone());
            consumeOfferedItem(expensiveOfferings.get().chargedStone());
        } else {
            consumeOfferedItem(offerings.get().diamondVapour());
            consumeOfferedItem(offerings.get().enderPearl());
            offerings.get().villager().discard();
        }
        success(player, level, center, "witchery.ritual.summon_wither.success",
                SoundEvents.WITHER_SPAWN, 0.8f, 4, 64);
        WitcheryMod.LOGGER.info("Rite of Summon Wither cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Demon — summons a large infernal demon if the circle is clear. */
    private static boolean riteOfSummonDemon(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SummonDemonOfferings> offerings = findSummonDemonOfferings(level, center);
        Optional<ExpensiveSummonDemonOfferings> expensiveOfferings = findExpensiveSummonDemonOfferings(level, center);
        boolean useExpensive = expensiveOfferings.isPresent();
        if (offerings.isEmpty() && expensiveOfferings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_demon.support"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 3000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_demon.no_power"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var demon = WitcheryEntities.DEMON.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (demon == null) {
            return false;
        }

        demon.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        demon.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(demon);

        if (useExpensive) {
            consumeOfferedItem(expensiveOfferings.get().refinedEvil());
            consumeOfferedItem(expensiveOfferings.get().blazeRod());
            consumeOfferedItem(expensiveOfferings.get().enderPearl());
            consumeOfferedItem(expensiveOfferings.get().attunedStone());
            consumeOfferedItem(expensiveOfferings.get().chargedStone());
        } else {
            consumeOfferedItem(offerings.get().refinedEvil());
            consumeOfferedItem(offerings.get().blazePowder());
            consumeOfferedItem(offerings.get().enderPearl());
            offerings.get().villager().discard();
        }
        success(player, level, center, "witchery.ritual.summon_demon.success",
                SoundEvents.BLAZE_SHOOT, 0.7f, 4, 56);
        WitcheryMod.LOGGER.info("Rite of Summon Demon cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Cooking — cooks nearby food with the original support-gated and charged branches. */
    private static boolean riteOfCookItem(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;

        Optional<ChargedCookFoodOfferings> chargedOfferings = findChargedCookFoodOfferings(level, center);
        Optional<CookFoodOfferings> normalOfferings = chargedOfferings.isEmpty()
                ? findCookFoodOfferings(level, center)
                : Optional.empty();
        if (chargedOfferings.isEmpty() && normalOfferings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.cook.support"), false);
            return false;
        }

        boolean charged = chargedOfferings.isPresent();
        if (!charged && !consumeNearbyRitualAltarPower(sl, center, 16, 1000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.cook.no_power"), false);
            return false;
        }

        double burnChance = charged ? 0.16 : 0.08;
        AABB aabb = new AABB(center).inflate(5);
        int cooked = 0;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, aabb)) {
            ItemStack stack = ie.getItem();
            Optional<RecipeHolder<SmeltingRecipe>> recipe =
                    sl.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack), level.registryAccess());
            if (result.isEmpty() || !result.has(DataComponents.FOOD) || stack.getCount() <= 0) {
                continue;
            }

            int size = stack.getCount();
            int burnCount = 0;
            for (int i = 0; i < size; i++) {
                if (level.random.nextDouble() < burnChance) {
                    burnCount++;
                }
            }

            ie.discard();
            if (size - burnCount > 0) {
                ItemStack cookedStack = result.copyWithCount(size - burnCount);
                ItemEntity cookedEntity = new ItemEntity(level,
                        center.getX() + 0.5,
                        center.getY() + 0.05,
                        center.getZ() + 0.5,
                        cookedStack);
                cookedEntity.setDeltaMovement(0.0, 0.0, 0.0);
                level.addFreshEntity(cookedEntity);
            }
            if (burnCount > 0) {
                ItemEntity burntEntity = new ItemEntity(level,
                        center.getX() + 0.5,
                        center.getY() + 0.05,
                        center.getZ() + 0.5,
                        new ItemStack(Items.CHARCOAL, burnCount));
                burntEntity.setDeltaMovement(0.0, 0.0, 0.0);
                level.addFreshEntity(burntEntity);
            }
            cooked++;
        }

        if (cooked <= 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.cook.none"), false);
            return false;
        }

        if (charged) {
            consumeOfferedItem(chargedOfferings.get().chargedStone());
            consumeOfferedItem(chargedOfferings.get().woodAsh());
            consumeOfferedItem(chargedOfferings.get().blazePowder());
        } else {
            consumeOfferedItem(normalOfferings.get().woodAsh());
            consumeOfferedItem(normalOfferings.get().coal());
        }

        player.displayClientMessage(Component.translatable("witchery.ritual.cook.success", cooked), false);
        level.playSound(null, center, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
        spawnRingParticles(sl, center, 3, 24);
        return true;
    }

    /** Rite of the Crater — excavates downward in pulses, closer to the old staged ritual. */
    // ── Rite of Binding Spirits to Fetish ────────────────────────────────────

    /**
     * Binds nearby spirits/spectres/banshees/poltergeists to the fetish item, producing
     * a spirit-enchanted fetish that stores the effect type in CUSTOM_DATA.
     *
     * Effect ID mapping (mirrors original InfusedSpiritEffect):
     *   1 = Voodoo Protection  (3 spirits + 1 spectre + 1 banshee + 1 poltergeist)
     *   2 = Sentinel           (3 spirits + 3 spectres)
     *   3 = Shrieking          (3 spirits + 2 banshees)
     *   4 = Disorientation     (3 spirits + 2 poltergeists)
     *   5 = Ghost Walking      (3 spirits + 1 spectre + 1 banshee)
     */
    private static boolean riteOfBindSpiritsToFetish(Player player, Level level, BlockPos center, ItemStack fetishItem) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<BindFetishOfferings> offerings = findBindFetishOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_fetish.support"), false);
            return false;
        }

        double r = 16;
        AABB box = new AABB(center).inflate(r);

        List<com.emoniph.witchery.entity.SpiritEntity>      spirits      = sl.getEntitiesOfClass(com.emoniph.witchery.entity.SpiritEntity.class,      box);
        List<com.emoniph.witchery.entity.SpectreEntity>     spectres     = sl.getEntitiesOfClass(com.emoniph.witchery.entity.SpectreEntity.class,     box);
        List<com.emoniph.witchery.entity.BansheeEntity>     banshees     = sl.getEntitiesOfClass(com.emoniph.witchery.entity.BansheeEntity.class,     box);
        List<com.emoniph.witchery.entity.PoltergeistEntity> poltergeists = sl.getEntitiesOfClass(com.emoniph.witchery.entity.PoltergeistEntity.class, box);

        int effectId = matchFetishEffect(spirits.size(), spectres.size(), banshees.size(), poltergeists.size());
        if (effectId == 0) {
            player.displayClientMessage(Component.translatable("witchery.ritual.fetish.no_spirits"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 6000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_fetish.no_power"), false);
            return false;
        }

        consumeOfferedItem(offerings.get().attunedStone());
        consumeOfferedItem(offerings.get().necroStone());
        consumeOfferedItem(offerings.get().boline());

        // Consume entities
        consumeSpirits(spirits,      spiritCount(effectId));
        consumeSpirits(spectres,     spectreCount(effectId));
        consumeSpirits(banshees,     bansheeCount(effectId));
        consumeSpirits(poltergeists, poltergeistCount(effectId));

        // Output item with effect stored in CUSTOM_DATA
        ItemStack output = fetishItem.copy();
        output.setCount(1);
        net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, output,
                tag -> tag.putInt("FetishEffect", effectId));

        // Drop at center
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0, 0.15, 0);
        sl.addFreshEntity(drop);

        // Effects
        sl.playSound(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.6f, 1.2f);
        for (int i = 0; i < 20; i++) {
            double ox = (sl.random.nextDouble() - 0.5) * 2;
            double oy = sl.random.nextDouble() * 2;
            double oz = (sl.random.nextDouble() - 0.5) * 2;
            sl.sendParticles(ParticleTypes.PORTAL,
                    center.getX() + 0.5 + ox, center.getY() + 0.5 + oy, center.getZ() + 0.5 + oz,
                    1, 0, 0, 0, 0.1);
        }
        return true;
    }

    private static int matchFetishEffect(int spirits, int spectres, int banshees, int poltergeists) {
        // Check in same order as original effectList (id 1..5)
        if (spirits >= 3 && spectres >= 1 && banshees >= 1 && poltergeists >= 1) return 1; // Voodoo Protection
        if (spirits >= 3 && spectres >= 3)                                         return 2; // Sentinel
        if (spirits >= 3 && banshees >= 2)                                          return 3; // Shrieking
        if (spirits >= 3 && poltergeists >= 2)                                      return 4; // Disorientation
        if (spirits >= 3 && spectres >= 1 && banshees >= 1)                         return 5; // Ghost Walking
        return 0;
    }

    private static int spiritCount(int id)      { return switch (id) { case 1,2,3,4,5 -> 3; default -> 0; }; }
    private static int spectreCount(int id)     { return switch (id) { case 1,5 -> 1; case 2 -> 3; default -> 0; }; }
    private static int bansheeCount(int id)     { return switch (id) { case 1,5 -> 1; case 3 -> 2; default -> 0; }; }
    private static int poltergeistCount(int id) { return switch (id) { case 1 -> 1; case 4 -> 2; default -> 0; }; }

    private static <T extends net.minecraft.world.entity.Entity> void consumeSpirits(List<T> list, int count) {
        for (int i = 0; i < Math.min(count, list.size()); i++) list.get(i).discard();
    }

    // ── Taglock / Waystone helpers ────────────────────────────────────────────

    private record InventoryTaglock(int slot, UUID targetUuid) {}

    private record ChargeStoneOfferings(ItemEntity glowstone, ItemEntity redstone, ItemEntity woodAsh, ItemEntity quicklime) {}

    private record ForestationOfferings(ItemEntity wickerBundle, ItemEntity brewOfSprouting, ItemEntity entTwig) {}

    private record CookFoodOfferings(ItemEntity woodAsh, ItemEntity coal) {}

    private record ChargedCookFoodOfferings(ItemEntity chargedStone, ItemEntity woodAsh, ItemEntity blazePowder) {}

    private record SpectralStoneOfferings(ItemEntity necroStone, ItemEntity congealedSpirit, ItemEntity condensedFear, ItemEntity boline) {}

    private record BindSpectralOfferings(ItemEntity spectralDust, ItemEntity boline) {}

    private record BindFetishOfferings(ItemEntity attunedStone, ItemEntity necroStone, ItemEntity boline) {}

    private record CurseCreatureOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne,
                                          ItemEntity spiderEye, ItemEntity gunpowder,
                                          ItemEntity brewGrotesque) {}

    private record CurseSinkingOfferings(ItemEntity exhaleOfHornedOne, ItemEntity inkSac,
                                         ItemEntity netherWart, ItemEntity brewGrotesque) {}

    private record CurseOverheatingOfferings(ItemEntity exhaleOfHornedOne, ItemEntity demonicBlood,
                                             ItemEntity blazeRod, ItemEntity brewGrotesque) {}

    private record CurseInsanityOfferings(ItemEntity exhaleOfHornedOne, ItemEntity poisonousPotato,
                                          ItemEntity sugar, ItemEntity brewGrotesque) {}

    private record CurseWakingNightmareOfferings(ItemEntity exhaleOfHornedOne, ItemEntity mellifluousHunger,
                                                 ItemEntity tormentedTwine, ItemEntity emerald,
                                                 ItemEntity brewGrotesque) {}

    private record CureInsanityOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity potato,
                                         ItemEntity sugar, ItemEntity brewOfLove) {}

    private record CureSinkingOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity boneMeal,
                                        ItemEntity netherWart, ItemEntity brewOfTheDepths) {}

    private record CureOverheatingOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity icyNeedle,
                                            ItemEntity blazeRod, ItemEntity brewOfTheDepths) {}

    private record CureWakingNightmareOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity goldenCarrot,
                                                ItemEntity tormentedTwine, ItemEntity brewOfLove) {}

    private record CureMisfortuneOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity goldNugget,
                                           ItemEntity bread, ItemEntity brewOfLove) {}

    private record CureCreatureOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity spiderEye,
                                         ItemEntity gunpowder, ItemEntity brewOfLove) {}

    private record CorruptPoppetOfferings(ItemEntity exhaleOfHornedOne, ItemEntity blazePowder,
                                          ItemEntity spectralDust) {}

    private record SummonFamiliarOfferings(ItemEntity egg, ItemEntity goldIngot, ItemEntity arthana) {}

    private record BindFamiliarOfferings(ItemEntity tearOfTheGoddess, ItemEntity odourOfPurity,
                                         ItemEntity whiffOfMagic, ItemEntity diamond, ItemEntity demonicBlood) {}

    private record CallFamiliarOfferings(ItemEntity breathOfTheGoddess, ItemEntity hintOfRebirth,
                                         ItemEntity whiffOfMagic) {}

    private record SummonImpOfferings(ItemEntity refinedEvil, ItemEntity infernalBlood, ItemEntity enderPearl,
                                      ItemEntity attunedStone) {}

    private record SummonWitchOfferings(ItemEntity diamondVapour, ItemEntity exhaleOfHornedOne, ItemEntity magmaCream,
                                        ItemEntity arthana, ItemEntity fermentedSpiderEye) {}

    private record SummonWitherOfferings(ItemEntity diamondVapour, ItemEntity enderPearl, Villager villager) {}

    private record ExpensiveSummonWitherOfferings(ItemEntity diamond, ItemEntity enderPearl, ItemEntity attunedStone,
                                                  ItemEntity chargedStone) {}

    private record SummonDemonOfferings(ItemEntity refinedEvil, ItemEntity blazePowder, ItemEntity enderPearl,
                                        Villager villager) {}

    private record ExpensiveSummonDemonOfferings(ItemEntity refinedEvil, ItemEntity blazeRod, ItemEntity enderPearl,
                                                 ItemEntity attunedStone, ItemEntity chargedStone) {}

    private record HellOnEarthOfferings(ItemEntity redstoneSoup, ItemEntity demonHeart, ItemEntity waystone,
                                        ItemEntity netherStar, Villager villager, ItemEntity boundWaystone,
                                        BlockPos effectCenter, boolean tooFar) {}

    private record FertilityOfferings(ItemEntity hintOfRebirth, ItemEntity diamondVapour, ItemEntity quicklime,
                                      ItemEntity gypsum, ItemEntity mutandis) {}

    private record PortableFertilityOfferings(ItemEntity hintOfRebirth, ItemEntity diamondVapour, ItemEntity quicklime,
                                              ItemEntity gypsum, ItemEntity mutandisExtremis, ItemEntity chargedStone) {}

    private record BlindnessOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                                      ItemEntity exhaleOfHornedOne, ItemEntity brewOfInk, ItemEntity poisonousPotato,
                                      ItemEntity spiderEye, ItemEntity diamond) {}

    private record BlightOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                                   ItemEntity fermentedSpiderEye, ItemEntity poisonousPotato, ItemEntity rottenFlesh,
                                   ItemEntity diamond) {}

    private record RainOfToadsOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                                        ItemEntity toeOfFrog, ItemEntity waterBucket, ItemEntity belladonna) {}

    private record NaturesPowerOfferings(ItemEntity rowanSapling, ItemEntity alderSapling, ItemEntity hawthornSapling,
                                         ItemEntity oakSapling, ItemEntity spruceSapling, ItemEntity birchSapling,
                                         ItemEntity chargedStone) {}

    private record TransposeOresOfferings(ItemEntity enderPearl, ItemEntity ironIngot, ItemEntity blazePowder,
                                          ItemEntity diamondVapour, ItemEntity chargedStone) {}

    private record RaiseColumnOfferings(ItemEntity brewOfSprouting, ItemEntity cactus, ItemEntity focus) {}

    private record OptionalWaystoneRelocation(ItemEntity boundWaystone, BlockPos effectCenter, boolean tooFar) {}

    private enum StormRiteVariant {
        BASE,
        LARGE,
        PORTABLE
    }

    private record StormOfferings(ItemEntity weatherFocus, ItemEntity woodAsh, ItemEntity chargedStone,
                                  ItemEntity boundWaystone, BlockPos effectCenter, boolean tooFar) {}

    private record MysticBranchOfferings(ItemEntity entTwig, ItemEntity mysticUnguent) {}

    private record CrystalBallOfferings(ItemEntity quartzSphere, ItemEntity goldIngot, ItemEntity happenstanceOil) {}

    private record SeerStoneOfferings(ItemEntity quartzSphere, ItemEntity mossyCobblestone, ItemEntity happenstanceOil) {}

    private record PriorIncarnationOfferings(ItemEntity necroStone, ItemEntity dogTongue,
                                             ItemEntity bone, ItemEntity spectralDust,
                                             ItemEntity chargedStone) {}

    private record ManifestationOfferings(ItemEntity spectralDust, ItemEntity mellifluousHunger, ItemEntity necroStone,
                                           ItemEntity goldenPickaxe, ItemEntity arthana,
                                           ItemEntity gunpowder) {}

    private record TotalEclipseOfferings(ItemEntity quicklime, ItemEntity chargedStone) {}

    private record EntityTranspositionOfferings(ItemEntity waystone, ItemEntity enderDew, ItemEntity ironAxe) {}

    private record InfuseBrewSoaringOfferings(ItemEntity infusedBrewBase, ItemEntity broom, ItemEntity feather,
                                              ItemEntity arthana, OwlFamiliarEntity owl) {}

    private record InfuseBrewGraveOfferings(ItemEntity infusedBrewBase, ItemEntity bone, ItemEntity denseWeb,
                                            ItemEntity necroStone, Zombie zombie) {}

    /** Find a filled taglock in the caster's inventory without consuming it yet. */
    private static Optional<InventoryTaglock> findFilledTaglock(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(WitcheryItems.TAGLOCK_KIT.get())) continue;
            if (!TaglockKitItem.isFilled(s)) continue;
            Optional<UUID> uuid = TaglockKitItem.getUUID(s);
            if (uuid.isPresent()) {
                return Optional.of(new InventoryTaglock(i, uuid.get()));
            }
        }
        return Optional.empty();
    }

    /** Consume one item from the given inventory slot after the ritual target has been validated. */
    private static void consumeInventoryItem(Player player, int slot) {
        var inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return;
        ItemStack stack = inv.getItem(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            inv.setItem(slot, ItemStack.EMPTY);
        }
    }

    private static boolean hasActiveCurseMasteryFamiliar(ServerPlayer player) {
        return FamiliarHelper.hasActiveCatFamiliar(player);
    }

    private static void strikeCurseBacklashLightning(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(lightning);
    }

    // ── Sprint 4 — Waystone rites ─────────────────────────────────────────────

    /** Rite of Binding — summons a freshly bound Waystone item at the ritual center. */
    private static boolean riteOfBindWaystone(Player player, Level level, BlockPos center) {
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> glowstone = findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
        if (enderDew.isEmpty() || glowstone.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_waystone.support"), false);
            return false;
        }

        consumeOfferedItem(enderDew.get());
        consumeOfferedItem(glowstone.get());
        ItemStack boundWaystone = new ItemStack(WitcheryItems.WAYSTONE.get());
        WaystoneItem.bindTo(boundWaystone, center, level.dimension());
        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5,
                center.getY() + 1.5,
                center.getZ() + 0.5,
                boundWaystone);
        entity.setDeltaMovement(0.0, 0.3, 0.0);
        level.addFreshEntity(entity);
        success(player, level, center, "witchery.ritual.bind_waystone.success",
                SoundEvents.BEACON_ACTIVATE, 1f, 2, 32);
        WitcheryMod.LOGGER.info("Waystone summoned and bound to ritual center {} for {}", center, player.getName().getString());
        return true;
    }

    /**
     * Rite of Binding (copy) — restores the original small-circle bound-waystone copy route
     * and duplicates the dropped bound waystone's location onto one extra offered unbound waystone.
     */
    private static boolean riteOfBindWaystoneCopy(Player player, Level level, BlockPos center, ItemStack sourceWaystone) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!WaystoneItem.isBound(sourceWaystone)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }

        Optional<ItemEntity> unboundWaystone = findNearbyWaystoneOffering(level, center, false);
        if (unboundWaystone.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.bind_waystone_copy.none"), false);
            return false;
        }
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        if (enderDew.isEmpty() || diamond.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.bind_waystone_copy.support"), false);
            return false;
        }

        consumeOfferedItem(unboundWaystone.get());
        consumeOfferedItem(enderDew.get());
        consumeOfferedItem(diamond.get());
        ItemStack copiedWaystones = sourceWaystone.copyWithCount(2);
        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5,
                center.getY() + 1.5,
                center.getZ() + 0.5,
                copiedWaystones);
        entity.setDeltaMovement(0.0, 0.3, 0.0);
        level.addFreshEntity(entity);

        success(player, level, center, "witchery.ritual.bind_waystone_copy.success",
                SoundEvents.BEACON_ACTIVATE, 1f, 2, 32);
        WitcheryMod.LOGGER.info("Copied bound waystone at {} for {}", center, sp.getName().getString());
        return true;
    }

    private static Optional<ItemEntity> findNearbyOffering(Level level, BlockPos center, Predicate<ItemStack> matcher) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radius = 1.5;
        double radiusSq = radius * radius;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(radius);
        ItemEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (!matcher.test(stack)) {
                continue;
            }
            double distance = itemEntity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq || distance > closestDistance) {
                continue;
            }
            closest = itemEntity;
            closestDistance = distance;
        }
        return Optional.ofNullable(closest);
    }

    private static Optional<ItemEntity> findNearbyWaystoneOffering(Level level, BlockPos center, boolean bound) {
        return findNearbyOffering(level, center,
                stack -> stack.is(WitcheryItems.WAYSTONE.get()) && WaystoneItem.isBound(stack) == bound);
    }

    private static Optional<ItemEntity> findNearbyFilledTaglockOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center,
                stack -> stack.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isFilled(stack));
    }

    private static Optional<ItemEntity> findNearbyRedstoneOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, stack -> stack.is(Items.REDSTONE));
    }

    private static Optional<ItemEntity> findNearbyGlowstoneOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
    }

    private static Optional<ItemEntity> findNearbyObsidianOffering(Level level, BlockPos center) {
        return findNearbyOffering(level, center, stack -> stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN));
    }

    private static Optional<ForestationOfferings> findForestationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> wickerBundle = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WICKER_BUNDLE.get()));
        Optional<ItemEntity> brewOfSprouting = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_SPROUTING.get()));
        Optional<ItemEntity> entTwig = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENT_TWIG.get()));
        if (wickerBundle.isEmpty() || brewOfSprouting.isEmpty() || entTwig.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ForestationOfferings(
                wickerBundle.get(),
                brewOfSprouting.get(),
                entTwig.get()));
    }

    private static Optional<CookFoodOfferings> findCookFoodOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> coal = findNearbyOffering(level, center, stack -> stack.is(Items.COAL));
        if (woodAsh.isEmpty() || coal.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CookFoodOfferings(woodAsh.get(), coal.get()));
    }

    private static Optional<ChargedCookFoodOfferings> findChargedCookFoodOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        if (chargedStone.isEmpty() || woodAsh.isEmpty() || blazePowder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ChargedCookFoodOfferings(
                chargedStone.get(),
                woodAsh.get(),
                blazePowder.get()));
    }

    private static Optional<Villager> findNearbyVillagerSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, Villager.class);
    }

    private static Optional<OwlFamiliarEntity> findNearbyOwlSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, OwlFamiliarEntity.class);
    }

    private static Optional<Zombie> findNearbyZombieSacrifice(Level level, BlockPos center) {
        return findNearbyLivingSacrifice(level, center, Zombie.class);
    }

    private static <T extends LivingEntity> Optional<T> findNearbyLivingSacrifice(Level level, BlockPos center, Class<T> type) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radius = 1.5;
        double radiusSq = radius * radius;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(radius);
        T closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (T entity : level.getEntitiesOfClass(type, bounds)) {
            if (!entity.isAlive()) {
                continue;
            }
            double distance = entity.position().distanceToSqr(centerPoint);
            if (distance >= radiusSq || distance > closestDistance) {
                continue;
            }
            closest = entity;
            closestDistance = distance;
        }
        return Optional.ofNullable(closest);
    }

    private static Optional<CurseCreatureOfferings> findCurseCreatureOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TAGLOCK_KIT.get()));
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (taglockKit.isEmpty() || exhaleOfHornedOne.isEmpty() || spiderEye.isEmpty()
                || gunpowder.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseCreatureOfferings(
                taglockKit.get(),
                exhaleOfHornedOne.get(),
                spiderEye.get(),
                gunpowder.get(),
                brewGrotesque.get()));
    }

    private static Optional<CureCreatureOfferings> findCureCreatureOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_LOVE.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || spiderEye.isEmpty()
                || gunpowder.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureCreatureOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                spiderEye.get(),
                gunpowder.get(),
                brewOfLove.get()));
    }

    private static Optional<CureMisfortuneOfferings> findCureMisfortuneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> goldNugget = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_NUGGET));
        Optional<ItemEntity> bread = findNearbyOffering(level, center, stack -> stack.is(Items.BREAD));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_LOVE.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || goldNugget.isEmpty()
                || bread.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureMisfortuneOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                goldNugget.get(),
                bread.get(),
                brewOfLove.get()));
    }

    private static Optional<CurseSinkingOfferings> findCurseSinkingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> inkSac = findNearbyOffering(level, center, stack -> stack.is(Items.INK_SAC));
        Optional<ItemEntity> netherWart = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_WART));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (exhaleOfHornedOne.isEmpty() || inkSac.isEmpty() || netherWart.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseSinkingOfferings(
                exhaleOfHornedOne.get(),
                inkSac.get(),
                netherWart.get(),
                brewGrotesque.get()));
    }

    private static Optional<CurseOverheatingOfferings> findCurseOverheatingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (exhaleOfHornedOne.isEmpty() || demonicBlood.isEmpty() || blazeRod.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseOverheatingOfferings(
                exhaleOfHornedOne.get(),
                demonicBlood.get(),
                blazeRod.get(),
                brewGrotesque.get()));
    }

    private static Optional<CurseInsanityOfferings> findCurseInsanityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> sugar = findNearbyOffering(level, center, stack -> stack.is(Items.SUGAR));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (exhaleOfHornedOne.isEmpty() || poisonousPotato.isEmpty() || sugar.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseInsanityOfferings(
                exhaleOfHornedOne.get(),
                poisonousPotato.get(),
                sugar.get(),
                brewGrotesque.get()));
    }

    private static Optional<CurseWakingNightmareOfferings> findCurseWakingNightmareOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> mellifluousHunger = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()));
        Optional<ItemEntity> tormentedTwine = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()));
        Optional<ItemEntity> emerald = findNearbyOffering(level, center, stack -> stack.is(Items.EMERALD));
        Optional<ItemEntity> brewGrotesque = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_GROTESQUE.get()));
        if (exhaleOfHornedOne.isEmpty() || mellifluousHunger.isEmpty() || tormentedTwine.isEmpty()
                || emerald.isEmpty() || brewGrotesque.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CurseWakingNightmareOfferings(
                exhaleOfHornedOne.get(),
                mellifluousHunger.get(),
                tormentedTwine.get(),
                emerald.get(),
                brewGrotesque.get()));
    }

    private static Optional<CureInsanityOfferings> findCureInsanityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> potato = findNearbyOffering(level, center, stack -> stack.is(Items.POTATO));
        Optional<ItemEntity> sugar = findNearbyOffering(level, center, stack -> stack.is(Items.SUGAR));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_LOVE.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || potato.isEmpty()
                || sugar.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureInsanityOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                potato.get(),
                sugar.get(),
                brewOfLove.get()));
    }

    private static Optional<CureSinkingOfferings> findCureSinkingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> boneMeal = findNearbyOffering(level, center, stack -> stack.is(Items.BONE_MEAL));
        Optional<ItemEntity> netherWart = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_WART));
        Optional<ItemEntity> brewOfTheDepths = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_DEPTHS.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || boneMeal.isEmpty()
                || netherWart.isEmpty() || brewOfTheDepths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureSinkingOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                boneMeal.get(),
                netherWart.get(),
                brewOfTheDepths.get()));
    }

    private static Optional<CureOverheatingOfferings> findCureOverheatingOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> icyNeedle = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ICY_NEEDLE.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> brewOfTheDepths = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_THE_DEPTHS.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || icyNeedle.isEmpty()
                || blazeRod.isEmpty() || brewOfTheDepths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureOverheatingOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                icyNeedle.get(),
                blazeRod.get(),
                brewOfTheDepths.get()));
    }

    private static Optional<CureWakingNightmareOfferings> findCureWakingNightmareOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> taglockKit = findNearbyFilledTaglockOffering(level, center);
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> goldenCarrot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLDEN_CARROT));
        Optional<ItemEntity> tormentedTwine = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TORMENTED_TWINE.get()));
        Optional<ItemEntity> brewOfLove = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_LOVE.get()));
        if (taglockKit.isEmpty() || breathOfTheGoddess.isEmpty() || goldenCarrot.isEmpty()
                || tormentedTwine.isEmpty() || brewOfLove.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CureWakingNightmareOfferings(
                taglockKit.get(),
                breathOfTheGoddess.get(),
                goldenCarrot.get(),
                tormentedTwine.get(),
                brewOfLove.get()));
    }

    private static Optional<CorruptPoppetOfferings> findCorruptPoppetOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        if (exhaleOfHornedOne.isEmpty() || blazePowder.isEmpty() || spectralDust.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CorruptPoppetOfferings(
                exhaleOfHornedOne.get(),
                blazePowder.get(),
                spectralDust.get()));
    }

    private static Optional<SpectralStoneOfferings> findSpectralStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> congealedSpirit = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.CONGEALED_SPIRIT.get()));
        Optional<ItemEntity> condensedFear = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.CONDENSED_FEAR.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (necroStone.isEmpty() || congealedSpirit.isEmpty() || condensedFear.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SpectralStoneOfferings(necroStone.get(), congealedSpirit.get(), condensedFear.get(), boline.get()));
    }

    private static Optional<BindSpectralOfferings> findBindSpectralOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (spectralDust.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindSpectralOfferings(spectralDust.get(), boline.get()));
    }

    private static Optional<BindFetishOfferings> findBindFetishOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> boline = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        if (attunedStone.isEmpty() || necroStone.isEmpty() || boline.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindFetishOfferings(attunedStone.get(), necroStone.get(), boline.get()));
    }

    private static Optional<MysticBranchOfferings> findMysticBranchOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> entTwig = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENT_TWIG.get()));
        Optional<ItemEntity> mysticUnguent = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MYSTIC_UNGUENT.get()));
        if (entTwig.isEmpty() || mysticUnguent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MysticBranchOfferings(entTwig.get(), mysticUnguent.get()));
    }

    private static Optional<CrystalBallOfferings> findCrystalBallOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> quartzSphere = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()));
        Optional<ItemEntity> goldIngot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_INGOT));
        Optional<ItemEntity> happenstanceOil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()));
        if (quartzSphere.isEmpty() || goldIngot.isEmpty() || happenstanceOil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CrystalBallOfferings(quartzSphere.get(), goldIngot.get(), happenstanceOil.get()));
    }

    private static Optional<SeerStoneOfferings> findSeerStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> quartzSphere = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUARTZ_SPHERE.get()));
        Optional<ItemEntity> mossyCobblestone = findNearbyOffering(level, center, stack -> stack.is(Items.MOSSY_COBBLESTONE));
        Optional<ItemEntity> happenstanceOil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAPPENSTANCE_OIL.get()));
        if (quartzSphere.isEmpty() || mossyCobblestone.isEmpty() || happenstanceOil.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SeerStoneOfferings(quartzSphere.get(), mossyCobblestone.get(), happenstanceOil.get()));
    }

    private static Optional<PriorIncarnationOfferings> findPriorIncarnationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> dogTongue = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TONGUE_OF_DOG.get()));
        Optional<ItemEntity> bone = findNearbyOffering(level, center, stack -> stack.is(Items.BONE));
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (necroStone.isEmpty() || dogTongue.isEmpty() || bone.isEmpty()
                || spectralDust.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PriorIncarnationOfferings(necroStone.get(), dogTongue.get(), bone.get(),
                spectralDust.get(), chargedStone.get()));
    }

    private static Optional<ManifestationOfferings> findManifestationOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> spectralDust = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        Optional<ItemEntity> mellifluousHunger = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MELLIFLUOUS_HUNGER.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<ItemEntity> goldenPickaxe = findNearbyOffering(level, center, stack -> stack.is(Items.GOLDEN_PICKAXE));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<ItemEntity> gunpowder = findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        if (spectralDust.isEmpty() || mellifluousHunger.isEmpty() || necroStone.isEmpty() || goldenPickaxe.isEmpty()
                || arthana.isEmpty() || gunpowder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ManifestationOfferings(spectralDust.get(), mellifluousHunger.get(), necroStone.get(),
                goldenPickaxe.get(), arthana.get(), gunpowder.get()));
    }

    private static Optional<TotalEclipseOfferings> findTotalEclipseOfferings(Level level, BlockPos center, boolean portable) {
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> chargedStone = portable
                ? findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
                : Optional.empty();
        if (quicklime.isEmpty() || (portable && chargedStone.isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(new TotalEclipseOfferings(quicklime.get(), chargedStone.orElse(null)));
    }

    private static Optional<EntityTranspositionOfferings> findEntityTranspositionOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> waystone = findNearbyOffering(level, center,
                stack -> stack.is(WitcheryItems.WAYSTONE.get()) && !WaystoneItem.isBound(stack));
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> ironAxe = findNearbyOffering(level, center, stack -> stack.is(Items.IRON_AXE));
        if (waystone.isEmpty() || enderDew.isEmpty() || ironAxe.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EntityTranspositionOfferings(waystone.get(), enderDew.get(), ironAxe.get()));
    }

    private static Optional<InfuseBrewSoaringOfferings> findInfuseBrewSoaringOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> infusedBrewBase = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()));
        Optional<ItemEntity> broom = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BROOM.get()));
        Optional<ItemEntity> feather = findNearbyOffering(level, center, stack -> stack.is(Items.FEATHER));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<OwlFamiliarEntity> owl = findNearbyOwlSacrifice(level, center);
        if (infusedBrewBase.isEmpty() || broom.isEmpty() || feather.isEmpty() || arthana.isEmpty() || owl.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InfuseBrewSoaringOfferings(
                infusedBrewBase.get(),
                broom.get(),
                feather.get(),
                arthana.get(),
                owl.get()));
    }

    private static Optional<InfuseBrewGraveOfferings> findInfuseBrewGraveOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> infusedBrewBase = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.INFUSED_BREW_BASE.get()));
        Optional<ItemEntity> bone = findNearbyOffering(level, center, stack -> stack.is(Items.BONE));
        Optional<ItemEntity> denseWeb = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DENSE_WEB.get()));
        Optional<ItemEntity> necroStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.NECRO_STONE.get()));
        Optional<Zombie> zombie = findNearbyZombieSacrifice(level, center);
        if (infusedBrewBase.isEmpty() || bone.isEmpty() || denseWeb.isEmpty() || necroStone.isEmpty() || zombie.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InfuseBrewGraveOfferings(
                infusedBrewBase.get(),
                bone.get(),
                denseWeb.get(),
                necroStone.get(),
                zombie.get()));
    }

    private static Optional<SummonFamiliarOfferings> findSummonFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> egg = findNearbyOffering(level, center, stack -> stack.is(Items.EGG));
        Optional<ItemEntity> goldIngot = findNearbyOffering(level, center, stack -> stack.is(Items.GOLD_INGOT));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        if (egg.isEmpty() || goldIngot.isEmpty() || arthana.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonFamiliarOfferings(egg.get(), goldIngot.get(), arthana.get()));
    }

    private static Optional<BindFamiliarOfferings> findBindFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> tearOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TEAR_OF_GODDESS.get()));
        Optional<ItemEntity> odourOfPurity = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ODOUR_OF_PURITY.get()));
        Optional<ItemEntity> whiffOfMagic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        Optional<ItemEntity> demonicBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        if (tearOfTheGoddess.isEmpty() || odourOfPurity.isEmpty() || whiffOfMagic.isEmpty()
                || diamond.isEmpty() || demonicBlood.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BindFamiliarOfferings(
                tearOfTheGoddess.get(),
                odourOfPurity.get(),
                whiffOfMagic.get(),
                diamond.get(),
                demonicBlood.get()));
    }

    private static Optional<CallFamiliarOfferings> findCallFamiliarOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> breathOfTheGoddess = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREATH_OF_GODDESS.get()));
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> whiffOfMagic = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WHIFF_OF_MAGIC.get()));
        if (breathOfTheGoddess.isEmpty() || hintOfRebirth.isEmpty() || whiffOfMagic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CallFamiliarOfferings(
                breathOfTheGoddess.get(),
                hintOfRebirth.get(),
                whiffOfMagic.get()));
    }

    private static Optional<SummonImpOfferings> findSummonImpOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> infernalBlood = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMONIC_BLOOD.get()));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        if (refinedEvil.isEmpty() || infernalBlood.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonImpOfferings(
                refinedEvil.get(),
                infernalBlood.get(),
                enderPearl.get(),
                attunedStone.get()));
    }

    private static Optional<SummonWitchOfferings> findSummonWitchOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> magmaCream = findNearbyOffering(level, center, stack -> stack.is(Items.MAGMA_CREAM));
        Optional<ItemEntity> arthana = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ARTHANA.get()));
        Optional<ItemEntity> fermentedSpiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        if (diamondVapour.isEmpty() || exhaleOfHornedOne.isEmpty() || magmaCream.isEmpty()
                || arthana.isEmpty() || fermentedSpiderEye.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonWitchOfferings(
                diamondVapour.get(),
                exhaleOfHornedOne.get(),
                magmaCream.get(),
                arthana.get(),
                fermentedSpiderEye.get()));
    }

    private static Optional<SummonWitherOfferings> findSummonWitherOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (diamondVapour.isEmpty() || enderPearl.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonWitherOfferings(diamondVapour.get(), enderPearl.get(), villager.get()));
    }

    private static Optional<ExpensiveSummonWitherOfferings> findExpensiveSummonWitherOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (diamond.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExpensiveSummonWitherOfferings(
                diamond.get(),
                enderPearl.get(),
                attunedStone.get(),
                chargedStone.get()));
    }

    private static Optional<SummonDemonOfferings> findSummonDemonOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (refinedEvil.isEmpty() || blazePowder.isEmpty() || enderPearl.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SummonDemonOfferings(refinedEvil.get(), blazePowder.get(), enderPearl.get(), villager.get()));
    }

    private static Optional<ExpensiveSummonDemonOfferings> findExpensiveSummonDemonOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> refinedEvil = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REFINED_EVIL.get()));
        Optional<ItemEntity> blazeRod = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_ROD));
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> attunedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (refinedEvil.isEmpty() || blazeRod.isEmpty() || enderPearl.isEmpty() || attunedStone.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ExpensiveSummonDemonOfferings(
                refinedEvil.get(),
                blazeRod.get(),
                enderPearl.get(),
                attunedStone.get(),
                chargedStone.get()));
    }

    private static Optional<HellOnEarthOfferings> findHellOnEarthOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> demonHeart = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DEMON_HEART.get()));
        Optional<ItemEntity> waystone = findNearbyOffering(level, center,
                stack -> stack.is(WitcheryItems.WAYSTONE.get()) && !WaystoneItem.isBound(stack));
        Optional<ItemEntity> netherStar = findNearbyOffering(level, center, stack -> stack.is(Items.NETHER_STAR));
        Optional<Villager> villager = findNearbyVillagerSacrifice(level, center);
        if (redstoneSoup.isEmpty() || demonHeart.isEmpty() || waystone.isEmpty()
                || netherStar.isEmpty() || villager.isEmpty()) {
            return Optional.empty();
        }
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        return Optional.of(new HellOnEarthOfferings(
                redstoneSoup.get(),
                demonHeart.get(),
                waystone.get(),
                netherStar.get(),
                villager.get(),
                relocation.boundWaystone(),
                relocation.effectCenter(),
                relocation.tooFar()));
    }

    private static Optional<FertilityOfferings> findFertilityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> gypsum = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.GYPSUM.get()));
        Optional<ItemEntity> mutandis = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MUTANDIS.get()));
        if (hintOfRebirth.isEmpty() || diamondVapour.isEmpty() || quicklime.isEmpty() || gypsum.isEmpty() || mutandis.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FertilityOfferings(
                hintOfRebirth.get(),
                diamondVapour.get(),
                quicklime.get(),
                gypsum.get(),
                mutandis.get()));
    }

    private static Optional<PortableFertilityOfferings> findPortableFertilityOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> hintOfRebirth = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HINT_OF_REBIRTH.get()));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        Optional<ItemEntity> gypsum = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.GYPSUM.get()));
        Optional<ItemEntity> mutandisExtremis = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (hintOfRebirth.isEmpty() || diamondVapour.isEmpty() || quicklime.isEmpty() || gypsum.isEmpty()
                || mutandisExtremis.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PortableFertilityOfferings(
                hintOfRebirth.get(),
                diamondVapour.get(),
                quicklime.get(),
                gypsum.get(),
                mutandisExtremis.get(),
                chargedStone.get()));
    }

    private static Optional<BlindnessOfferings> findBlindnessOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> exhaleOfHornedOne = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()));
        Optional<ItemEntity> brewOfInk = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_INK.get()));
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> spiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.SPIDER_EYE));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty() || exhaleOfHornedOne.isEmpty()
                || brewOfInk.isEmpty() || poisonousPotato.isEmpty() || spiderEye.isEmpty() || diamond.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlindnessOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                exhaleOfHornedOne.get(),
                brewOfInk.get(),
                poisonousPotato.get(),
                spiderEye.get(),
                diamond.get()));
    }

    private static Optional<BlightOfferings> findBlightOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> fermentedSpiderEye = findNearbyOffering(level, center, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        Optional<ItemEntity> poisonousPotato = findNearbyOffering(level, center, stack -> stack.is(Items.POISONOUS_POTATO));
        Optional<ItemEntity> rottenFlesh = findNearbyOffering(level, center, stack -> stack.is(Items.ROTTEN_FLESH));
        Optional<ItemEntity> diamond = findNearbyOffering(level, center, stack -> stack.is(Items.DIAMOND));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty() || fermentedSpiderEye.isEmpty()
                || poisonousPotato.isEmpty() || rottenFlesh.isEmpty() || diamond.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlightOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                fermentedSpiderEye.get(),
                poisonousPotato.get(),
                rottenFlesh.get(),
                diamond.get()));
    }

    private static Optional<RainOfToadsOfferings> findRainOfToadsOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        Optional<ItemEntity> redstoneSoup = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        Optional<ItemEntity> reekOfMisfortune = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        Optional<ItemEntity> toeOfFrog = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.TOE_OF_FROG.get()));
        Optional<ItemEntity> waterBucket = findNearbyOffering(level, center, stack -> stack.is(Items.WATER_BUCKET));
        Optional<ItemEntity> belladonna = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BELLADONNA.get()));
        if (chargedStone.isEmpty() || redstoneSoup.isEmpty() || reekOfMisfortune.isEmpty()
                || toeOfFrog.isEmpty() || waterBucket.isEmpty() || belladonna.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RainOfToadsOfferings(
                chargedStone.get(),
                redstoneSoup.get(),
                reekOfMisfortune.get(),
                toeOfFrog.get(),
                waterBucket.get(),
                belladonna.get()));
    }

    private static Optional<NaturesPowerOfferings> findNaturesPowerOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> rowanSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ROWAN_SAPLING.get()));
        Optional<ItemEntity> alderSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ALDER_SAPLING.get()));
        Optional<ItemEntity> hawthornSapling = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.HAWTHORN_SAPLING.get()));
        Optional<ItemEntity> oakSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.OAK_SAPLING.asItem()));
        Optional<ItemEntity> spruceSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.SPRUCE_SAPLING.asItem()));
        Optional<ItemEntity> birchSapling = findNearbyOffering(level, center, stack -> stack.is(Blocks.BIRCH_SAPLING.asItem()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (rowanSapling.isEmpty() || alderSapling.isEmpty() || hawthornSapling.isEmpty()
                || oakSapling.isEmpty() || spruceSapling.isEmpty() || birchSapling.isEmpty()
                || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NaturesPowerOfferings(
                rowanSapling.get(),
                alderSapling.get(),
                hawthornSapling.get(),
                oakSapling.get(),
                spruceSapling.get(),
                birchSapling.get(),
                chargedStone.get()));
    }

    private static Optional<TransposeOresOfferings> findTransposeOresOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> enderPearl = findNearbyOffering(level, center, stack -> stack.is(Items.ENDER_PEARL));
        Optional<ItemEntity> ironIngot = findNearbyOffering(level, center, stack -> stack.is(Items.IRON_INGOT));
        Optional<ItemEntity> blazePowder = findNearbyOffering(level, center, stack -> stack.is(Items.BLAZE_POWDER));
        Optional<ItemEntity> diamondVapour = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.DIAMOND_VAPOUR.get()));
        Optional<ItemEntity> chargedStone = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        if (enderPearl.isEmpty() || ironIngot.isEmpty() || blazePowder.isEmpty()
                || diamondVapour.isEmpty() || chargedStone.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TransposeOresOfferings(
                enderPearl.get(),
                ironIngot.get(),
                blazePowder.get(),
                diamondVapour.get(),
                chargedStone.get()));
    }

    private static Optional<RaiseColumnOfferings> findRaiseColumnOfferings(Level level, BlockPos center,
                                                                           RitualCircleDetector.CircleSize circleSize) {
        Optional<ItemEntity> brewOfSprouting = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.BREW_OF_SPROUTING.get()));
        Optional<ItemEntity> cactus = findNearbyOffering(level, center, stack -> stack.is(Blocks.CACTUS.asItem()));
        Optional<ItemEntity> focus = switch (circleSize) {
            case SMALL -> findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
            case MEDIUM -> findNearbyOffering(level, center, stack -> stack.is(Items.REDSTONE));
            case LARGE -> findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
        };
        if (brewOfSprouting.isEmpty() || cactus.isEmpty() || focus.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RaiseColumnOfferings(brewOfSprouting.get(), cactus.get(), focus.get()));
    }

    private static OptionalWaystoneRelocation findOptionalWaystoneRelocation(Level level, BlockPos center, int covenSize) {
        Optional<ItemEntity> boundWaystone = findNearbyWaystoneOffering(level, center, true);
        if (boundWaystone.isEmpty()) {
            return new OptionalWaystoneRelocation(null, center, false);
        }

        ItemStack stack = boundWaystone.get().getItem();
        Optional<ResourceKey<Level>> boundDim = WaystoneItem.getBoundDim(stack);
        Optional<BlockPos> boundPos = WaystoneItem.getBoundPos(stack);
        if (boundDim.isEmpty() || boundPos.isEmpty() || !boundDim.get().equals(level.dimension())) {
            return new OptionalWaystoneRelocation(null, center, false);
        }

        int maxDistance = 50 + 50 * covenSize;
        boolean tooFar = boundPos.get().distSqr(center) > (double) (maxDistance * maxDistance);
        return new OptionalWaystoneRelocation(tooFar ? null : boundWaystone.get(), boundPos.get(), tooFar);
    }

    private static Optional<StormOfferings> findStormOfferings(Level level, BlockPos center, StormRiteVariant variant, int covenSize) {
        Optional<ItemEntity> weatherFocus = switch (variant) {
            case BASE -> findNearbyOffering(level, center, stack -> stack.is(Items.SNOWBALL));
            case LARGE -> findNearbyOffering(level, center, stack -> stack.is(Items.WATER_BUCKET));
            case PORTABLE -> findNearbyOffering(level, center, stack -> stack.is(Items.GUNPOWDER));
        };
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> chargedStone = variant == StormRiteVariant.PORTABLE
                ? findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()))
                : Optional.empty();
        if (weatherFocus.isEmpty() || woodAsh.isEmpty()
                || (variant == StormRiteVariant.PORTABLE && chargedStone.isEmpty())) {
            return Optional.empty();
        }
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        return Optional.of(new StormOfferings(weatherFocus.get(), woodAsh.get(), chargedStone.orElse(null),
                relocation.boundWaystone(), relocation.effectCenter(), relocation.tooFar()));
    }

    private static Optional<ChargeStoneOfferings> findChargeStoneOfferings(Level level, BlockPos center) {
        Optional<ItemEntity> glowstone = findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
        Optional<ItemEntity> redstone = findNearbyOffering(level, center, stack -> stack.is(Items.REDSTONE));
        Optional<ItemEntity> woodAsh = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.WOOD_ASH.get()));
        Optional<ItemEntity> quicklime = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.QUICKLIME.get()));
        if (glowstone.isEmpty() || redstone.isEmpty() || woodAsh.isEmpty() || quicklime.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ChargeStoneOfferings(glowstone.get(), redstone.get(), woodAsh.get(), quicklime.get()));
    }

    private static void consumeOfferedItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
    }

    /** Rite of Transposition (waystone) — teleports caster to bound waystone coordinates. */
    private static boolean riteOfTranspositionToWaystone(Player player, Level level, BlockPos center, ItemStack waystone) {
        if (!(player instanceof ServerPlayer sp)) return false;
        Optional<BlockPos> dest = WaystoneItem.getBoundPos(waystone);
        Optional<ResourceKey<Level>> dim = WaystoneItem.getBoundDim(waystone);
        if (dest.isEmpty() || dim.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }
        ServerLevel targetLevel = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getLevel(dim.get());
        if (targetLevel == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }
        BlockPos d = dest.get();
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radius = 3.0;
        double radiusSq = radius * radius;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(radius);
        int moved = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds)) {
            if (entity instanceof ItemEntity itemEntity
                    && itemEntity.distanceToSqr(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5) <= 3.0) {
                continue;
            }
            if (entity.position().distanceToSqr(centerPoint) >= radiusSq) {
                continue;
            }
            teleportEntityToWaystone(entity, targetLevel, d);
            moved++;
        }
        success(player, level, center, "witchery.ritual.transposition.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Transposition moved {} entity(s) from {} to {}", moved, center, d);
        return true;
    }

    /** Rite of Transposition (pull) — pulls the taglock target to the caster. Costs 3000 altar power. */
    private static void teleportEntityToWaystone(Entity entity, ServerLevel targetLevel, BlockPos dest) {
        double x = dest.getX() + 0.5;
        double y = dest.getY();
        double z = dest.getZ() + 0.5;
        if (entity instanceof ServerPlayer player) {
            if (player.level() == targetLevel) {
                player.teleportTo(x, y, z);
            } else {
                player.teleportTo(targetLevel, x, y, z,
                        java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
            }
            return;
        }

        if (entity.level() == targetLevel) {
            entity.teleportTo(x, y, z);
        } else {
            entity.teleportTo(targetLevel, x, y, z,
                    java.util.Set.<net.minecraft.world.entity.Relative>of(), entity.getYRot(), entity.getXRot(), false);
        }
    }

    /** Medium otherwhere transposition — teleports the bound taglock target to the circle center. */
    private static boolean riteOfTranspositionEntity(Player player, Level level, BlockPos center, ItemStack taglock) {
        if (!(player instanceof ServerPlayer sp)) return false;
        Optional<EntityTranspositionOfferings> offerings = findEntityTranspositionOfferings(level, center);
        if (offerings.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.support"), false);
            return false;
        }
        Optional<UUID> uuid = TaglockKitItem.getUUID(taglock);
        if (uuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(uuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.offline"), false);
            return false;
        }
        if (sp.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                || target.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.spirit_world"), false);
            return false;
        }
        if (hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower((ServerLevel) level, center, 16, 3000)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.no_power"), false);
            return false;
        }

        BlockPos destination = center.above();
        consumeOfferedItem(offerings.get().waystone());
        consumeOfferedItem(offerings.get().enderDew());
        consumeOfferedItem(offerings.get().ironAxe());
        if (target.level() == level) {
            target.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        } else {
            target.teleportTo((ServerLevel) level, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                    java.util.Set.<net.minecraft.world.entity.Relative>of(), target.getYRot(), target.getXRot(), false);
        }
        target.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.pulled"), false);
        success(player, level, center, "witchery.ritual.transposition_entity.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Transposition Entity moved {} to {}", target.getName().getString(), destination);
        return true;
    }

    private static boolean riteOfTranspositionPull(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        Optional<InventoryTaglock> taglock = findFilledTaglock(sp);
        if (taglock.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglock.get().targetUuid());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.offline"), false);
            return false;
        }
        if (hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }
        consumeInventoryItem(sp, taglock.get().slot());
        BlockPos dest = sp.blockPosition().above();
        target.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        target.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.pulled"), false);
        success(player, level, center, "witchery.ritual.transposition_pull.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 2, 60);
        return true;
    }

    // ── Sprint 4 — Curse rites ────────────────────────────────────────────────

    /**
     * Generic taglock-based curse dispatcher.
     * Finds a filled taglock, resolves the online target, consumes the taglock only after target validation,
     * then checks curse protection and runs the curse action on the target.
     */
    @FunctionalInterface
    private interface CurseAction { void apply(ServerPlayer target); }

    private static boolean applyCurse(Player caster, Level level, BlockPos center,
                                      CurseAction action, String successKey, String failKey,
                                      boolean checkHunterArmorProtection) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        Optional<InventoryTaglock> taglock = findFilledTaglock(sp);
        if (taglock.isEmpty()) {
            sp.displayClientMessage(Component.translatable(failKey + ".no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglock.get().targetUuid());
        if (target == null) {
            sp.displayClientMessage(Component.translatable(failKey + ".offline"), false);
            return false;
        }
        if (checkHunterArmorProtection && hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        // Voodoo Protection Poppet negates the curse
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }
        consumeInventoryItem(sp, taglock.get().slot());
        action.apply(target);
        target.displayClientMessage(Component.translatable(successKey + ".target"), false);
        success(caster, level, center, successKey + ".caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
        return true;
    }

    @FunctionalInterface
    private interface CureAction { boolean apply(ServerPlayer target); }

    private static boolean applyCure(Player caster, Level level, BlockPos center, ItemEntity offeredTaglock,
                                     CureAction action, String targetKey) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        Optional<UUID> taglockUuid = TaglockKitItem.getUUID(offeredTaglock.getItem());
        if (taglockUuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.cure.no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglockUuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.cure.offline"), false);
            return false;
        }
        if (!action.apply(target)) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.cure.none"), false);
            return false;
        }
        consumeOfferedItem(offeredTaglock);
        target.displayClientMessage(Component.translatable(targetKey), false);
        success(caster, level, center, "witchery.ritual.cure.success",
                SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.8f, 3, 48);
        return true;
    }

    private static boolean triggerVoodooProtection(ServerPlayer player) {
        return PoppetEvents.damageBoundPoppet(player, WitcheryItems.VOODOO_PROTECTION_POPPET.get(), 350);
    }

    private static boolean hasCurseProtection(ServerPlayer player) {
        return isWearingWitchHunterArmor(player) && player.level().random.nextFloat() < 0.9f;
    }

    private static boolean isWearingWitchHunterArmor(ServerPlayer player) {
        int pieces = 0;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.is(WitcheryItems.HUNTER_HELMET.get())
                    || s.is(WitcheryItems.HUNTER_CHESTPLATE.get())
                    || s.is(WitcheryItems.HUNTER_LEGGINGS.get())
                    || s.is(WitcheryItems.HUNTER_BOOTS.get())) pieces++;
        }
        return pieces >= 4;
    }

    private static boolean riteOfBreathOfTheGoddessCure(Player caster, Level level, BlockPos center) {
        Optional<CureMisfortuneOfferings> misfortune = findCureMisfortuneOfferings(level, center);
        if (misfortune.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, misfortune.get().taglockKit(), target -> {
                if (!target.hasEffect(MobEffects.UNLUCK)) return false;
                target.removeEffect(MobEffects.UNLUCK);
                return true;
            }, "curse.witchery.misfortune.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(misfortune.get().breathOfTheGoddess());
            consumeOfferedItem(misfortune.get().goldNugget());
            consumeOfferedItem(misfortune.get().bread());
            consumeOfferedItem(misfortune.get().brewOfLove());
            return true;
        }

        Optional<CureCreatureOfferings> curseCreature = findCureCreatureOfferings(level, center);
        if (curseCreature.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, curseCreature.get().taglockKit(), target -> {
                boolean hadCurse = target.hasEffect(MobEffects.WITHER)
                        || target.hasEffect(MobEffects.SLOWNESS)
                        || target.hasEffect(MobEffects.WEAKNESS);
                if (!hadCurse) return false;
                target.removeEffect(MobEffects.WITHER);
                target.removeEffect(MobEffects.SLOWNESS);
                target.removeEffect(MobEffects.WEAKNESS);
                return true;
            }, "curse.witchery.cursed.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(curseCreature.get().breathOfTheGoddess());
            consumeOfferedItem(curseCreature.get().spiderEye());
            consumeOfferedItem(curseCreature.get().gunpowder());
            consumeOfferedItem(curseCreature.get().brewOfLove());
            return true;
        }

        Optional<CureWakingNightmareOfferings> wakingNightmare = findCureWakingNightmareOfferings(level, center);
        if (wakingNightmare.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, wakingNightmare.get().taglockKit(), target -> {
                WitcheryPlayerData data = WitcheryAttachments.get(target);
                boolean hadNightmare = data.isDemonicNightmare() || (data.isInSpiritWorld() && !data.isSpiritWorldDream());
                if (!hadNightmare) return false;
                data.setSpiritWorldIsDream(true);
                data.setDemonicNightmare(false);
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
                return true;
            }, "curse.witchery.waking_nightmare.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(wakingNightmare.get().breathOfTheGoddess());
            consumeOfferedItem(wakingNightmare.get().goldenCarrot());
            consumeOfferedItem(wakingNightmare.get().tormentedTwine());
            consumeOfferedItem(wakingNightmare.get().brewOfLove());
            return true;
        }

        Optional<CureOverheatingOfferings> overheating = findCureOverheatingOfferings(level, center);
        if (overheating.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, overheating.get().taglockKit(), target -> {
                boolean hadOverheating = target.hasEffect(MobEffects.WEAKNESS) || target.isOnFire();
                if (!hadOverheating) return false;
                target.removeEffect(MobEffects.WEAKNESS);
                target.clearFire();
                return true;
            }, "curse.witchery.overheating.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(overheating.get().breathOfTheGoddess());
            consumeOfferedItem(overheating.get().icyNeedle());
            consumeOfferedItem(overheating.get().blazeRod());
            consumeOfferedItem(overheating.get().brewOfTheDepths());
            return true;
        }

        Optional<CureSinkingOfferings> sinking = findCureSinkingOfferings(level, center);
        if (sinking.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, sinking.get().taglockKit(), target -> {
                boolean hadSinking = target.hasEffect(MobEffects.SLOWNESS)
                        || target.hasEffect(MobEffects.MINING_FATIGUE)
                        || target.hasEffect(MobEffects.LEVITATION);
                if (!hadSinking) return false;
                target.removeEffect(MobEffects.SLOWNESS);
                target.removeEffect(MobEffects.MINING_FATIGUE);
                target.removeEffect(MobEffects.LEVITATION);
                return true;
            }, "curse.witchery.sinking.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(sinking.get().breathOfTheGoddess());
            consumeOfferedItem(sinking.get().boneMeal());
            consumeOfferedItem(sinking.get().netherWart());
            consumeOfferedItem(sinking.get().brewOfTheDepths());
            return true;
        }

        Optional<CureInsanityOfferings> insanity = findCureInsanityOfferings(level, center);
        if (insanity.isPresent()) {
            if (!(level instanceof ServerLevel sl)) return false;
            if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                caster.displayClientMessage(Component.translatable("witchery.ritual.cure.no_power"), false);
                return false;
            }
            boolean cured = applyCure(caster, level, center, insanity.get().taglockKit(), target -> {
                boolean hadInsanity = target.hasEffect(MobEffects.NAUSEA) || target.hasEffect(MobEffects.BLINDNESS);
                if (!hadInsanity) return false;
                target.removeEffect(MobEffects.NAUSEA);
                target.removeEffect(MobEffects.BLINDNESS);
                return true;
            }, "curse.witchery.insanity.goddess_removal");
            if (!cured) return false;
            consumeOfferedItem(insanity.get().breathOfTheGoddess());
            consumeOfferedItem(insanity.get().potato());
            consumeOfferedItem(insanity.get().sugar());
            consumeOfferedItem(insanity.get().brewOfLove());
            return true;
        }

        return false;
    }

    /** Curse of Blindness — blinds target + nearby players for 3 minutes. */
    private static boolean riteOfCurseBlindness(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 3600, 0));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 3600, 0));
        }, "witchery.curse.blindness", "witchery.curse.blindness", false);
    }

    /** Curse of Blight — withers the target. */
    private static boolean riteOfCurseBlight(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 2400, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 0));
        }, "witchery.curse.blight", "witchery.curse.blight", false);
    }

    /** Curse of Overheating — sets target on fire and applies fire vulnerability. */
    private static boolean riteOfCurseOverheating(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        Optional<CurseOverheatingOfferings> offerings = findCurseOverheatingOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_overheating.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_overheating.no_power"), false);
            return false;
        }
        boolean applied = applyCurse(caster, level, center, target -> {
            target.igniteForTicks(200);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
        }, "witchery.curse.overheating", "witchery.curse.overheating", false);
        if (!applied) {
            return false;
        }
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().demonicBlood());
        consumeOfferedItem(offerings.get().blazeRod());
        consumeOfferedItem(offerings.get().brewGrotesque());
        return true;
    }

    /** Curse of Sinking — extreme slowness and mining fatigue. */
    private static boolean riteOfCurseSinking(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        Optional<CurseSinkingOfferings> offerings = findCurseSinkingOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_sinking.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_sinking.no_power"), false);
            return false;
        }
        boolean applied = applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2400, 3));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 2400, 2));
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0)); // brief sink
        }, "witchery.curse.sinking", "witchery.curse.sinking", false);
        if (!applied) {
            return false;
        }
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().inkSac());
        consumeOfferedItem(offerings.get().netherWart());
        consumeOfferedItem(offerings.get().brewGrotesque());
        return true;
    }

    /** Curse of Insanity — nausea + confusion. */
    private static boolean riteOfCurseInsanity(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        Optional<CurseInsanityOfferings> offerings = findCurseInsanityOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_insanity.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_insanity.no_power"), false);
            return false;
        }
        boolean applied = applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 3600, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }, "witchery.curse.insanity", "witchery.curse.insanity", false);
        if (!applied) {
            return false;
        }
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().poisonousPotato());
        consumeOfferedItem(offerings.get().sugar());
        consumeOfferedItem(offerings.get().brewGrotesque());
        return true;
    }

    /** Curse of Raining Toads — showers toads on the target. */
    private static boolean riteOfCurseRainingToads(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            if (!(target.level() instanceof ServerLevel sl)) return;
            for (int i = 0; i < 8; i++) {
                var toad = WitcheryEntities.TOAD.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (toad != null) {
                    double dx = (sl.getRandom().nextDouble() - 0.5) * 6;
                    double dz = (sl.getRandom().nextDouble() - 0.5) * 6;
                    toad.setPos(target.getX() + dx, target.getY() + 4, target.getZ() + dz);
                    sl.addFreshEntity(toad);
                }
            }
        }, "witchery.curse.raining_toads", "witchery.curse.raining_toads", false);
    }

    /** Curse of Hell on Earth — summons lightning + hostile mobs around target (large circle). */
    private static boolean riteOfCurseHellOnEarth(Player caster, Level level, BlockPos center) {
        if (!requireNightOverworld(caster, level, center)) return false;
        Optional<HellOnEarthOfferings> offerings = findHellOnEarthOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.hell_on_earth.support"), false);
            return false;
        }
        if (offerings.get().tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        if (!(caster instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 5000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.hell_on_earth.no_power"), false);
            return false;
        }
        Optional<InventoryTaglock> taglock = findFilledTaglock(sp);
        if (taglock.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.curse.hell_on_earth.no_taglock"), false);
            return false;
        }
        ServerPlayer target = sl.getServer().getPlayerList().getPlayer(taglock.get().targetUuid());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.curse.hell_on_earth.offline"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }

        int covenSize = countNearbyCovenWitches(level, center);
        BlockPos effectCenter = offerings.get().boundWaystone() != null
                ? offerings.get().effectCenter()
                : target.blockPosition();
        List<ItemStack> supplementalSacrifices = offerings.get().boundWaystone() != null
                ? List.of(offerings.get().boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, ActiveRitualType.HELL_ON_EARTH, RitualCircleDetector.CircleSize.LARGE,
                center, effectCenter, 4, 20, 250, 5, covenSize, 200.0f, 0,
                supplementalSacrifices, hasActiveCurseMasteryFamiliar(sp))) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeInventoryItem(sp, taglock.get().slot());
        consumeHellOnEarthOfferings(offerings.get());
        target.displayClientMessage(Component.translatable("witchery.curse.hell_on_earth.target"), false);
        success(caster, level, center, "witchery.curse.hell_on_earth.caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
        return true;
    }

    private static boolean consumeHellOnEarthOfferings(HellOnEarthOfferings offerings) {
        consumeOfferedItem(offerings.redstoneSoup());
        consumeOfferedItem(offerings.demonHeart());
        consumeOfferedItem(offerings.waystone());
        consumeOfferedItem(offerings.netherStar());
        if (offerings.boundWaystone() != null) {
            consumeOfferedItem(offerings.boundWaystone());
        }
        offerings.villager().discard();
        return true;
    }

    /** Curse of Waking Nightmare — forces target into nightmare state if in Spirit World. */
    private static boolean riteOfCurseWakingNightmare(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        Optional<CurseWakingNightmareOfferings> offerings = findCurseWakingNightmareOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_waking_nightmare.support"), false);
            return false;
        }
        if (!(level instanceof ServerLevel sl)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 10000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.curse_waking_nightmare.no_power"), false);
            return false;
        }
        boolean applied = applyCurse(caster, level, center, target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (data.isInSpiritWorld()) {
                data.setSpiritWorldIsDream(false); // flip to nightmare
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
        }, "witchery.curse.waking_nightmare", "witchery.curse.waking_nightmare", true);
        if (!applied) {
            return false;
        }
        consumeOfferedItem(offerings.get().exhaleOfHornedOne());
        consumeOfferedItem(offerings.get().mellifluousHunger());
        consumeOfferedItem(offerings.get().tormentedTwine());
        consumeOfferedItem(offerings.get().emerald());
        consumeOfferedItem(offerings.get().brewGrotesque());
        return true;
    }

    private static void spawnRingParticles(ServerLevel level, BlockPos center, int radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + 0.5 + radius * Math.cos(angle);
            double z = center.getZ() + 0.5 + radius * Math.sin(angle);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    x, center.getY() + 0.5, z, 1, 0, 0.1, 0, 0.05);
        }
    }
}
