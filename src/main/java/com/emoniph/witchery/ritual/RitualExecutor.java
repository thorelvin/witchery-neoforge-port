package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.BlightHelper;
import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.WitchHunterEntity;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.events.PoppetEvents;
import com.emoniph.witchery.events.SilverEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.CircleTalismanItem;
import com.emoniph.witchery.item.BiomeNoteItem;
import com.emoniph.witchery.item.SpectralStoneItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
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
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static com.emoniph.witchery.ritual.RitualOfferings.*;

public class RitualExecutor {
    private static final int VARIANT_RITUAL = 1;
    private static final int VARIANT_OTHERWHERE = 2;
    private static final int VARIANT_INFERNAL = 3;

    private static final int RITUAL_PULSE_INTERVAL = 30;
    private static final int VOID_BRAMBLE_NULL_RANGE = 32;
    private static final int VOID_BRAMBLE_NULL_RANGE_SQ = VOID_BRAMBLE_NULL_RANGE * VOID_BRAMBLE_NULL_RANGE;
    private static final int BLIGHT_HEART_RANDOM_RADIUS = 150;
    private static final int BLIGHT_RITE_POWER = 3000;
    private static final double FAMILIAR_BIND_RADIUS = 7.0;
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

    private record ClimateChangeScan(ClimateChangePlan plan, List<ItemEntity> itemsToConsume) {}

    /** Test hook: a sample SavedData with one staged ritual (for codec round-trip verification). */
    static RitualSavedData buildTestSavedData() {
        RitualSavedData data = new RitualSavedData();
        data.activeRituals.add(new ActiveRitual(ActiveRitualType.SANCTITY_CIRCLE, BlockPos.ZERO,
                RitualCircleDetector.CircleSize.SMALL, BlockPos.ZERO, 1, 0, 4, 0, 20, 2, 0.8f, false, 0,
                100L, new ItemStack(Items.DIAMOND), List.of(new ItemStack(Items.REDSTONE, 3)), "Tester"));
        return data;
    }

    private static List<ActiveRitual> activeRituals(ServerLevel level) {
        return RitualSavedData.get(level).activeRituals;
    }

    private static Map<BlockPos, ClimateChangePlan> climatePlans(ServerLevel level) {
        return RitualSavedData.get(level).climatePlans;
    }

    private static void markRitualsDirty(ServerLevel level) {
        RitualSavedData.get(level).setDirty();
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

            // Circle Magic does not work in the Spirit World; only Rite of Manifestation (spectral_dust) is permitted
            if (level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                    && !ingredient.is(WitcheryItems.SPECTRAL_DUST.get())) {
                level.playSound(null, floorCenter, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.rite.wrongdimension"), false);
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
                ActivationResult activation = tryRitualDefinitions(
                        result.size(), player, level, floorCenter, ingredient, circleVariant);
                if (activation != null && activation.matched()) {
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

    private static Optional<RitualOffering> protectionSupportOffering(ActiveRitualType type, ItemStack trigger,
                                                                      Level level, BlockPos center) {
        if (type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS) {
            return findNearbyRitualOffering(level, center, stack -> stack.is(Items.REDSTONE));
        }
        if (type != ActiveRitualType.PROTECTION_BARRIER_PORTABLE) {
            return Optional.empty();
        }
        return trigger.is(WitcheryItems.ATTUNED_STONE_CHARGED.get())
                ? findNearbyRitualOffering(level, center, RitualOfferings::isObsidianStack)
                : findNearbyRitualOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
    }

    private static String protectionSupportKey(ActiveRitualType type, ItemStack trigger) {
        if (type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS) {
            return "witchery.ritual.protection.redstone_support";
        }
        if (type != ActiveRitualType.PROTECTION_BARRIER_PORTABLE) {
            return null;
        }
        return trigger.is(WitcheryItems.ATTUNED_STONE_CHARGED.get())
                ? "witchery.ritual.protection.obsidian_support"
                : "witchery.ritual.protection.charged_support";
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

    // ── Data-driven rite definitions ───────────────────────────────────────────
    // The simple "trigger item (+ ring variant) -> riteOf..." rites are dispatched from this table
    // instead of the if-chains below. Each migrated trigger is unique within its ring's dispatch, so
    // consulting definitions first yields exactly the rite the old ordered chain would have. Offerings
    // and altar power are still validated inside each effect via RitualTransaction.
    private static final List<RitualDefinition> RITUAL_DEFINITIONS = List.of(
            // ── Small ring ──
            // Infusion brew bases — each needs a companion Medium ring in the matching chalk family.
            RitualDefinition.of("infuse_light", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.GHOST_OF_THE_LIGHT.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfusion(player, level, center, InfusionType.LIGHT),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL)),
            RitualDefinition.of("infuse_overworld", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.SOUL_OF_THE_WORLD.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfusion(player, level, center, InfusionType.OVERWORLD),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL)),
            RitualDefinition.of("infuse_otherwhere", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.SPIRIT_OF_OTHERWHERE.get()), VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfInfusion(player, level, center, InfusionType.OTHERWHERE),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_OTHERWHERE)),
            RitualDefinition.of("infuse_infernal", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.INFERNAL_ANIMUS.get()), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfInfusion(player, level, center, InfusionType.INFERNAL),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_INFERNAL)),
            RitualDefinition.of("summon_witch", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.DIAMOND_VAPOUR.get()), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfSummonWitch(player, level, center)),
            RitualDefinition.of("summon_familiar", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.DROP_OF_LUCK.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfSummonFamiliar(player, level, center)),
            RitualDefinition.of("prior_incarnation", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.ROTTEN_FLESH), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfPriorIncarnation(player, level, center)),
            RitualDefinition.of("ice_shell", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.FROZEN_HEART.get()), RitualDefinition.ANY_VARIANT,
                    (player, level, center, trigger) -> riteOfIceShell(player, level, center)),
            RitualDefinition.of("storm_base", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.SNOWBALL), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfWeatherCallStorm(player, level, center, StormRiteVariant.BASE)),
            RitualDefinition.of("storm_large", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.WATER_BUCKET), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfWeatherCallStorm(player, level, center, StormRiteVariant.LARGE)),
            RitualDefinition.of("storm_portable", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.GUNPOWDER), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfWeatherCallStorm(player, level, center, StormRiteVariant.PORTABLE)),
            RitualDefinition.of("sanctity", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.FEATHER), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfSanctity(player, level, center)),
            RitualDefinition.of("imprisonment", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.SLIME_BALL), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfImprisonment(player, level, center)),
            RitualDefinition.of("cook_item", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.SUGAR), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCookItem(player, level, center)),
            RitualDefinition.of("infuse_seer_stone", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.QUARTZ_SPHERE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfuseSeerStone(player, level, center)),
            RitualDefinition.of("total_eclipse", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.STONE_AXE) || item.is(Items.IRON_AXE), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfTotalEclipse(player, level, center, trigger)),
            RitualDefinition.of("blindness", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.BLAZE_POWDER), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfBlindness(player, level, center)),
            RitualDefinition.of("banish_demon", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.BLAZE_POWDER), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBanishDemon(player, level, center, false)),
            RitualDefinition.of("spectral_stone", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.SPECTRAL_DUST.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfSpectralStone(player, level, center)),
            RitualDefinition.of("manifestation_dream", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.SPECTRAL_DUST.get()), VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfManifestationDream(player, level, center)),
            RitualDefinition.of("raise_column_small", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.COBBLESTONE) || item.is(Items.STONE), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.SMALL)),
            // Filled taglock on a Ritual ring with any cure offering set -> Breath of the Goddess cure.
            RitualDefinition.of("breath_of_goddess_cure", RitualCircleDetector.CircleSize.SMALL,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBreathOfTheGoddessCure(player, level, center))
                    .requiringOffering((level, center) ->
                            findCureMisfortuneOfferings(level, center).isPresent()
                            || findCureCreatureOfferings(level, center).isPresent()
                            || findCureInsanityOfferings(level, center).isPresent()
                            || findCureSinkingOfferings(level, center).isPresent()
                            || findCureOverheatingOfferings(level, center).isPresent()
                            || findCureWakingNightmareOfferings(level, center).isPresent()),
            // Bone Meal: Portable Fertility (with offerings) else plain Fertility.
            RitualDefinition.of("portable_fertility", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.BONE_MEAL), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfPortableFertility(player, level, center))
                    .requiringOffering((level, center) -> findPortableFertilityOfferings(level, center).isPresent()),
            RitualDefinition.of("fertility", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.BONE_MEAL), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfFertility(player, level, center)),
            // Attuned Stone: Charge Stone (Medium companion + offerings) else Necromantic Stone (offerings).
            RitualDefinition.of("charge_stone_small", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.ATTUNED_STONE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfChargeStone(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL))
                    .requiringOffering((level, center) -> findChargeStoneOfferings(level, center).isPresent()),
            RitualDefinition.of("necromantic_stone", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.ATTUNED_STONE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfNecromanticStone(player, level, center))
                    .requiringOffering((level, center) -> findNecromanticStoneOfferings(level, center).isPresent()),
            // Charged Attuned Stone: portable Protection Barrier (with Obsidian offered) else Banish Demon.
            RitualDefinition.of("protection_barrier_charged", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfProtectionBarrier(player, level, center,
                            ActiveRitualType.PROTECTION_BARRIER_PORTABLE, RitualCircleDetector.CircleSize.SMALL, 6, 4))
                    .requiringOffering((level, center) ->
                            findNearbyRitualOffering(level, center, RitualOfferings::isObsidianStack).isPresent()),
            RitualDefinition.of("banish_demon_charged", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBanishDemon(player, level, center, true)),
            // Obsidian: portable Protection Barrier (Charged Stone offered, no Redstone) else Monsters Barrier.
            RitualDefinition.of("obsidian_barrier_portable", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.OBSIDIAN), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfProtectionBarrier(player, level, center,
                            ActiveRitualType.PROTECTION_BARRIER_PORTABLE, RitualCircleDetector.CircleSize.SMALL, 6, 4))
                    .requiringOffering((level, center) ->
                            findNearbyRitualOffering(level, center, stack -> stack.is(Items.REDSTONE)).isEmpty()
                            && findNearbyRitualOffering(level, center, stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get())).isPresent()),
            RitualDefinition.of("obsidian_barrier_monsters", RitualCircleDetector.CircleSize.SMALL,
                    item -> item.is(Items.OBSIDIAN), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfProtectionBarrier(player, level, center,
                            ActiveRitualType.PROTECTION_BARRIER_MONSTERS, RitualCircleDetector.CircleSize.SMALL, 4, 5)),
            // Waystones. Bound (location) -> copy on a Ritual ring; blank -> Blooded (with offerings)
            // else plain bind; any bound (location or filled Blooded) -> transposition on an Otherwhere ring.
            // isBoundWaystone/isBlankWaystone are disjoint and the variants separate copy from transposition,
            // so order only matters for blooded-before-bind.
            RitualDefinition.of("bind_waystone_copy", RitualCircleDetector.CircleSize.SMALL,
                    WaystoneItem::isBoundWaystone, VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindWaystoneCopy(player, level, center, trigger)),
            RitualDefinition.of("bind_blooded_waystone", RitualCircleDetector.CircleSize.SMALL,
                    WaystoneItem::isBlankWaystone, VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindBloodedWaystone(player, level, center))
                    .requiringOffering((level, center) -> findBindBloodedWaystoneOfferings(level, center).isPresent()),
            RitualDefinition.of("bind_waystone", RitualCircleDetector.CircleSize.SMALL,
                    WaystoneItem::isBlankWaystone, VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindWaystone(player, level, center)),
            RitualDefinition.of("transposition_to_waystone", RitualCircleDetector.CircleSize.SMALL,
                    WaystoneItem::isAnyBoundWaystone, VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfTranspositionToWaystone(player, level, center, trigger)),

            // ── Medium ring ──
            // Companion-Small-ring rites (Ritual chalk inner ring).
            RitualDefinition.of("infusion_recharge", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> isPotion(item, Potions.REGENERATION), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfusionRecharge(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)),
            RitualDefinition.of("enchant_broom", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.FLYING_OINTMENT.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfEnchantBroom(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)),
            RitualDefinition.of("mystic_branch", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.ENT_TWIG.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfuseMysticBranch(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)),
            RitualDefinition.of("infuse_brew", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.INFUSED_BREW_BASE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfuseBrew(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)),
            RitualDefinition.of("whiff_of_magic_familiar", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.WHIFF_OF_MAGIC.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfWhiffOfMagicFamiliar(player, level, center)),
            RitualDefinition.of("bind_familiar", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.TEAR_OF_GODDESS.get()) || item.is(WitcheryItems.ODOUR_OF_PURITY.get()),
                    VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindFamiliar(player, level, center)),
            RitualDefinition.of("call_familiar", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.BREATH_OF_GODDESS.get()) || item.is(WitcheryItems.HINT_OF_REBIRTH.get()),
                    VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfCallFamiliar(player, level, center)),
            RitualDefinition.of("the_forest", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(ItemTags.SAPLINGS), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfTheForest(player, level, center, trigger)),
            RitualDefinition.of("rain_of_toads", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(Items.LILY_PAD) || item.is(Items.FROGSPAWN), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfRainOfToads(player, level, center)),
            RitualDefinition.of("raise_column_medium", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(Items.COBBLESTONE) || item.is(Items.STONE), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.MEDIUM)),
            RitualDefinition.of("natures_power", RitualCircleDetector.CircleSize.MEDIUM,
                    WitcheryItems::isBrewOfSprouting, VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfNaturesPower(player, level, center)),
            RitualDefinition.of("blight", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(Items.WITHER_ROSE), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfBlight(player, level, center)),
            RitualDefinition.of("corrupt_poppet", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.VOODOO_PROTECTION_POPPET.get()), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCorruptPoppet(player, level, center)),
            RitualDefinition.of("transposition_entity", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfTranspositionEntity(player, level, center, trigger)),
            RitualDefinition.of("bind_spectral_stone", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.SPECTRAL_STONE.get()) && !SpectralStoneItem.isBound(item), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindSpectralStone(player, level, center, trigger)),
            RitualDefinition.of("bind_fetish", RitualCircleDetector.CircleSize.MEDIUM,
                    RitualExecutor::isFetishItem, VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfBindSpiritsToFetish(player, level, center, trigger)),
            // Attuned Stone with a Small Ritual companion ring and Charge Stone offerings -> Charge Stone.
            RitualDefinition.of("charge_stone_medium", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.ATTUNED_STONE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfChargeStone(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL))
                    .requiringOffering((level, center) -> findChargeStoneOfferings(level, center).isPresent()),
            // Obsidian with a Glowstone Dust offering -> Protection.
            RitualDefinition.of("protection", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(Items.OBSIDIAN) || item.is(Items.CRYING_OBSIDIAN), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfProtection(player, level, center))
                    .requiringOffering((level, center) ->
                            findNearbyRitualOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST)).isPresent()),
            // Filled taglock on an Infernal ring: the curse family, chosen by which curse offerings are present.
            RitualDefinition.of("curse_creature", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseCreature(player, level, center))
                    .requiringOffering((level, center) -> findCurseCreatureOfferings(level, center).isPresent()),
            RitualDefinition.of("curse_sinking", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseSinking(player, level, center))
                    .requiringOffering((level, center) -> findCurseSinkingOfferings(level, center).isPresent()),
            RitualDefinition.of("curse_overheating", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseOverheating(player, level, center))
                    .requiringOffering((level, center) -> findCurseOverheatingOfferings(level, center).isPresent()),
            RitualDefinition.of("curse_insanity", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseInsanity(player, level, center))
                    .requiringOffering((level, center) -> findCurseInsanityOfferings(level, center).isPresent()),
            // Crystal Ball — but defer to the small Seer Stone rite when only Seer Stone offerings are
            // present beneath a Small Ritual ring (the old inner NO_MATCH guard, now a veto).
            RitualDefinition.of("crystal_ball", RitualCircleDetector.CircleSize.MEDIUM,
                    item -> item.is(WitcheryItems.QUARTZ_SPHERE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfInfuseCrystalBall(player, level, center))
                    .vetoedWhen((level, center) ->
                            findCrystalBallOfferings(level, center).isEmpty()
                            && findSeerStoneOfferings(level, center).isPresent()
                            && RitualCircleDetector.hasRing(level, center, RitualCircleDetector.CircleSize.SMALL)
                            && RitualCircleDetector.detectVariant(level, center, RitualCircleDetector.CircleSize.SMALL) == VARIANT_RITUAL),

            // ── Large ring ──
            RitualDefinition.of("beastial_call", RitualCircleDetector.CircleSize.LARGE,
                    RitualExecutor::isBeastialCallTrigger, VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfBeastialCall(player, level, center)),
            RitualDefinition.of("transpose_ores", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(WitcheryItems.DIAMOND_VAPOUR.get()), VARIANT_OTHERWHERE,
                    (player, level, center, trigger) -> riteOfTransposeOres(player, level, center)),
            RitualDefinition.of("moving_earth", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.DIRT) || item.is(Items.GRAVEL) || item.is(Items.SAND), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfMovingEarth(player, level, center)),
            RitualDefinition.of("volcano", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.LAVA_BUCKET), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfVolcano(player, level, center)),
            RitualDefinition.of("broken_earth", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.FLINT) || item.is(Items.FLINT_AND_STEEL), VARIANT_RITUAL,
                    (player, level, center, trigger) -> { riteOfBrokenEarth(player, level, center); return true; }),
            RitualDefinition.of("raise_column_large", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.COBBLESTONE) || item.is(Items.STONE), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.LARGE)),
            RitualDefinition.of("curse_waking_nightmare", RitualCircleDetector.CircleSize.LARGE,
                    item -> TaglockKitItem.isTaglockKit(item) && TaglockKitItem.isFilled(item), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseWakingNightmare(player, level, center)),
            RitualDefinition.of("summon_imp", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(WitcheryItems.REFINED_EVIL.get()), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfSummonImp(player, level, center)),
            RitualDefinition.of("summon_demon", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(WitcheryItems.TORMENTED_TWINE.get()), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfSummonDemon(player, level, center)),
            RitualDefinition.of("summoning_lord_of_torment", RitualCircleDetector.CircleSize.LARGE,
                    WitcheryItems::isTormentContract, RitualDefinition.ANY_VARIANT,
                    (player, level, center, trigger) -> riteOfSummoningLordOfTorment(player, level, center)),
            // Companion-ring-gated large rites (no offering, no disambiguation).
            RitualDefinition.of("hell_on_earth", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.NETHER_STAR), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfCurseHellOnEarth(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_OTHERWHERE),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.SMALL, VARIANT_INFERNAL)),
            RitualDefinition.of("summon_wither", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(Items.WITHER_SKELETON_SKULL), VARIANT_INFERNAL,
                    (player, level, center, trigger) -> riteOfSummonWither(player, level, center),
                    RitualDefinition.CompanionRing.of(RitualCircleDetector.CircleSize.MEDIUM, VARIANT_INFERNAL)),
            // Climate Change — but defer to the medium Charge Stone rite when that setup is present
            // (the old shouldLetMediumChargeStoneHandle guard, now a veto).
            RitualDefinition.of("climate_change", RitualCircleDetector.CircleSize.LARGE,
                    item -> item.is(WitcheryItems.ATTUNED_STONE.get()), VARIANT_RITUAL,
                    (player, level, center, trigger) -> riteOfClimateChange(player, level, center))
                    .vetoedWhen(RitualExecutor::shouldLetMediumChargeStoneHandle));

    /**
     * Try the data-driven {@link RitualDefinition} table for the given ring. Returns the activation
     * result if a definition matched (success or matched-but-failed), or {@code null} if no definition
     * applies and the caller should fall through to the remaining {@code if}-chain.
     */
    private static ActivationResult tryRitualDefinitions(RitualCircleDetector.CircleSize size,
            Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        for (RitualDefinition definition : RITUAL_DEFINITIONS) {
            if (definition.matches(size, item, circleVariant)
                    && companionRingsPresent(definition, level, center)
                    && offeringsPresent(definition, level, center)
                    && notVetoed(definition, level, center)) {
                return matchedResult(definition.run(player, level, center, item));
            }
        }
        return null;
    }

    /** Whether every companion ring a definition requires is currently drawn around the centre. */
    private static boolean companionRingsPresent(RitualDefinition definition, Level level, BlockPos center) {
        for (RitualDefinition.CompanionRing ring : definition.companionRings()) {
            if (!hasCompanionRing(level, center, ring.size(), ring.variant())) {
                return false;
            }
        }
        return true;
    }

    /** Whether every offering a definition requires is present among the offerings around the centre. */
    private static boolean offeringsPresent(RitualDefinition definition, Level level, BlockPos center) {
        for (RitualDefinition.OfferingRequirement requirement : definition.offeringRequirements()) {
            if (!requirement.present(level, center)) {
                return false;
            }
        }
        return true;
    }

    /** Whether no veto condition on the definition currently holds (a held veto skips the rite). */
    private static boolean notVetoed(RitualDefinition definition, Level level, BlockPos center) {
        for (RitualDefinition.Veto veto : definition.vetoes()) {
            if (veto.vetoes(level, center)) {
                return false;
            }
        }
        return true;
    }

    /** Test hook: every rite definition name in table order. */
    static java.util.List<String> allRitualDefinitionNames() {
        return RITUAL_DEFINITIONS.stream().map(RitualDefinition::name).toList();
    }

    /** Test hook: the name of the rite definition the dropped item would trigger, or {@code null}. */
    static String matchingRitualDefinitionName(RitualCircleDetector.CircleSize size, ItemStack item, int circleVariant) {
        for (RitualDefinition definition : RITUAL_DEFINITIONS) {
            if (definition.matches(size, item, circleVariant)) {
                return definition.name();
            }
        }
        return null;
    }

    /** Test hook: the {@code "SIZE:variant"} companion rings the matched rite requires, or {@code null}. */
    static java.util.List<String> matchingRitualDefinitionCompanionRings(
            RitualCircleDetector.CircleSize size, ItemStack item, int circleVariant) {
        for (RitualDefinition definition : RITUAL_DEFINITIONS) {
            if (definition.matches(size, item, circleVariant)) {
                return definition.companionRings().stream()
                        .map(ring -> ring.size() + ":" + ring.variant())
                        .toList();
            }
        }
        return null;
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

    private static boolean shouldLetMediumChargeStoneHandle(Level level, BlockPos center) {
        if (!hasCompanionRing(level, center, RitualCircleDetector.CircleSize.MEDIUM, VARIANT_RITUAL)
                || !hasCompanionRing(level, center, RitualCircleDetector.CircleSize.SMALL, VARIANT_RITUAL)
                || findChargeStoneOfferings(level, center).isEmpty()) {
            return false;
        }
        return !(level instanceof ServerLevel sl) || scanClimateChangePlan(sl, center) == null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SMALL rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Infusion — applies a typed infusion to the player from a brew base. */
    private static boolean riteOfInfusion(Player player, Level level, BlockPos center, InfusionType type) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;

        int startupCost = type == InfusionType.LIGHT ? 2000
                : (type == InfusionType.OVERWORLD || type == InfusionType.OTHERWHERE || type == InfusionType.INFERNAL)
                ? 4000
                : 0;
        if (startupCost > 0 && !consumeNearbyRitualAltarPower(sl, center, 16, startupCost)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.infusion.no_power"), false);
            return false;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setInfusionType(type);
        data.setInfusionEnergy(200, 200);
        com.emoniph.witchery.events.InfusionEvents.onInfusionGranted(sp, type);

        String msgKey = infusionSuccessKey(type);

        PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, data));
        success(player, level, center, msgKey, SoundEvents.BEACON_ACTIVATE, 1.2f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Infusion ({}): {} is now infused.", type, sp.getName().getString());
        return true;
    }

    private static String infusionSuccessKey(InfusionType type) {
        if (type == InfusionType.LIGHT) {
            return "witchery.ritual.infusion.light.success";
        }
        if (type == InfusionType.OVERWORLD) {
            return "witchery.ritual.infusion.overworld.success";
        }
        if (type == InfusionType.OTHERWHERE) {
            return "witchery.ritual.infusion.otherwhere.success";
        }
        return type == InfusionType.INFERNAL
                ? "witchery.ritual.infusion.infernal.success"
                : "witchery.ritual.infusion.success";
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

        AABB playerBounds = new AABB(center).inflate(FAMILIAR_BIND_RADIUS, 1.0, FAMILIAR_BIND_RADIUS);
        List<ServerPlayer> nearbyPlayers = sl.getEntitiesOfClass(ServerPlayer.class, playerBounds);
        List<FamiliarBindTarget> bindTargets = new ArrayList<>();

        for (ServerPlayer nearbyPlayer : nearbyPlayers) {
            if (nearbyPlayer.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5)
                    > FAMILIAR_BIND_RADIUS * FAMILIAR_BIND_RADIUS) {
                continue;
            }

            TamableAnimal candidate = findBindableFamiliarCandidate(nearbyPlayer, sl, center, FAMILIAR_BIND_RADIUS);
            if (candidate == null) {
                continue;
            }

            bindTargets.add(new FamiliarBindTarget(nearbyPlayer, candidate));
        }

        if (bindTargets.isEmpty()) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 8000,
                "witchery.ritual.bind_familiar.no_power")
                .offer(offerings.get().tearOfTheGoddess())
                .offer(offerings.get().odourOfPurity())
                .offer(offerings.get().whiffOfMagic())
                .offer(offerings.get().diamond())
                .offer(offerings.get().demonicBlood());
        // Rollback-safe: bind the familiars as the committed effect; if none could actually be
        // bound, the transaction consumes nothing. Previously this consumed 8000 altar power and
        // five sacrifices BEFORE binding, then returned false on a finalize failure — losing them.
        boolean[] boundAny = {false};
        if (!transaction.commit(player, () -> {
            for (FamiliarBindTarget bindTarget : bindTargets) {
                TamableAnimal familiar = bindFamiliarToPlayer(bindTarget.player(), sl, bindTarget.candidate());
                if (familiar == null) {
                    continue;
                }
                familiar.setOrderedToSit(false);
                sl.sendParticles(ParticleTypes.HEART,
                        familiar.getX(), familiar.getY() + 0.8, familiar.getZ(),
                        6, 0.25, 0.25, 0.25, 0.02);
                boundAny[0] = true;
            }
            if (!boundAny[0]) {
                WitcheryMod.LOGGER.warn("Rite of Bind Familiar at {} found candidates but bound none; nothing consumed.", center);
            }
            return boundAny[0];
        })) {
            return false;
        }

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

    /** Rite of Summon Familiar — restores the original small-circle Drop of Luck path as a real spectral familiar summon. */
    private static boolean riteOfSummonFamiliar(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<SummonFamiliarOfferings> offerings = findSummonFamiliarOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.summon_familiar.support"), false);
            return false;
        }
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var familiar = WitcheryEntities.SPECTRAL_FAMILIAR.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (familiar == null) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000, "witchery.ritual.summon_familiar.no_power")
                .offer(offerings.get().egg())
                .offer(offerings.get().goldIngot())
                .offer(offerings.get().arthana());
        if (!transaction.commit(player)) {
            return false;
        }

        familiar.snapTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, player.getYRot(), 0.0F);
        familiar.setOrderedToSit(false);
        familiar.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(familiar);

        bindFamiliarToPlayer(sp, sl, familiar);

        success(player, level, center, "witchery.ritual.summon_familiar.success",
                SoundEvents.PIG_AMBIENT, 1.0f, 2, 24);
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
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
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

        BlockState saplingState = saplingBlock.defaultBlockState();
        int covenSize = countNearbyCovenWitches(level, center);
        OptionalWaystoneRelocation relocation = findOptionalWaystoneRelocation(level, center, covenSize);
        if (relocation.tooFar()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.toofaraway"), false);
            return false;
        }
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 4000,
                "witchery.ritual.forest.no_power")
                .offer(offerings.get().wickerBundle())
                .offer(offerings.get().brewOfSprouting())
                .offer(offerings.get().entTwig())
                .offer(relocation.boundWaystone());
        if (!transaction.commit(player)) {
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

        player.displayClientMessage(Component.translatable("witchery.ritual.forest.success", grown), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of the Forest seeded {} {} grove attempts at {} for {}",
                grown, triggerSapling.getHoverName().getString(), effectCenter, player.getName().getString());
        return true;
    }

    /** Rite of Charge Stone — infuses an Attuned Stone into its charged variant on paired small and medium ritual circles. */
    private static boolean riteOfChargeStone(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ChargeStoneOfferings> offerings = findChargeStoneOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.charge_stone.support"), false);
            return false;
        }
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000,
                "witchery.ritual.charge_stone.no_power")
                .offer(offerings.get().glowstone())
                .offer(offerings.get().redstone())
                .offer(offerings.get().woodAsh())
                .offer(offerings.get().quicklime());
        if (!transaction.commit(player)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            return false;
        }

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
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
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

    /** Rite of Protection Barrier (large) — sustains the broader monster barrier from the medium ritual ring. */
    private static boolean riteOfProtection(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<RitualOffering> glowstone = findNearbyRitualOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
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
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 1000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, ActiveRitualType.PROTECTION_BARRIER, RitualCircleDetector.CircleSize.MEDIUM,
                center, relocation.effectCenter(), 6, 6, 0, 20, 0, 1.4f, 0,
                supplementalSacrifices, player.getScoreboardName())) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        glowstone.get().consumeOne();
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
        ItemStack trigger = PENDING_RITUAL_INGREDIENT.get();
        Optional<RitualOffering> supportOffering = protectionSupportOffering(type, trigger, level, center);
        String supportKey = protectionSupportKey(type, trigger);
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
        int startupPower = type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS ? 500 : 0;
        float upkeepCost = type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS ? 1.2f : 0.0f;
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
        if (startupPower > 0 && !consumeNearbyRitualAltarPower(sl, center, 16, startupPower)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        List<ItemStack> supplementalSacrifices = relocation.boundWaystone() != null
                ? List.of(relocation.boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, type, circleSize, center, relocation.effectCenter(),
                radius, height, targetStage, 20, 0, upkeepCost, 0,
                supplementalSacrifices, player.getScoreboardName())) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        supportOffering.ifPresent(RitualOffering::consumeOne);
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
        Optional<BanishDemonOfferings> offerings = findBanishDemonOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.banish_demon.support"), false);
            return false;
        }
        if (!portable && !consumeNearbyRitualAltarPower(sl, center, 16, 2000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.banish_demon.no_power"), false);
            return false;
        }

        offerings.get().blazePowder().consumeOne();
        offerings.get().waystone().consumeOne();
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
        Optional<RitualOffering> redstone = findNearbyRitualOffering(level, center, stack -> stack.is(Items.REDSTONE));
        if (redstone.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.redstone_support"), false);
            return false;
        }
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 500)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.SANCTITY_CIRCLE, RitualCircleDetector.CircleSize.SMALL, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        redstone.get().consumeOne();
        success(player, level, center, "witchery.ritual.sanctity.activated",
                SoundEvents.BEACON_ACTIVATE, 1.2f, 2, 48);
        return true;
    }

    /** Rite of Imprisonment — sustains an attractive circle that keeps creatures penned in. */
    private static boolean riteOfImprisonment(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<RitualOffering> redstone = findNearbyRitualOffering(level, center, stack -> stack.is(Items.REDSTONE));
        if (redstone.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.redstone_support"), false);
            return false;
        }
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 500)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.protection.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.IMPRISONMENT_CIRCLE, RitualCircleDetector.CircleSize.SMALL, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        redstone.get().consumeOne();
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
        storeClimateChangePlan(sl, center, scan.plan());
        List<ItemStack> supplementalSacrifices = copyConsumedItemStacks(scan.itemsToConsume());
        if (!queueActiveRitual(sl, ActiveRitualType.CLIMATE_CHANGE, RitualCircleDetector.CircleSize.LARGE,
                center, radius, 0, 5, 20, covenSize, 0.0f, 0, supplementalSacrifices)) {
            clearClimateChangePlan(sl, center);
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
        ClimateChangePlan selectedPlan = null;
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

            ClimateChangePlan notePlan = climatePlanFromBiomeNote(level, stack);
            ClimateFocus focus = notePlan == null ? climateFocusFromStack(stack) : null;
            if (notePlan == null && focus == null) {
                continue;
            }

            double distance = itemEntity.distanceToSqr(centerX, centerY, centerZ);
            if (selectedFocusEntity == null
                    || distance < bestDistance
                    || (distance == bestDistance && itemEntity.getId() < selectedFocusEntity.getId())) {
                selectedFocusEntity = itemEntity;
                selectedFocus = focus;
                selectedPlan = notePlan;
                bestDistance = distance;
            }
        }

        if (selectedFocusEntity == null || (selectedFocus == null && selectedPlan == null)) {
            return null;
        }

        List<ItemEntity> itemsToConsume = new ArrayList<>(glowstoneEntities.size() + 1);
        itemsToConsume.add(selectedFocusEntity);
        itemsToConsume.addAll(glowstoneEntities);
        ClimateChangePlan plan = selectedPlan != null
                ? selectedPlan
                : new ClimateChangePlan(resolveClimateBiome(level, selectedFocus, glowstone), weatherForFocus(selectedFocus));
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

    private static ClimateChangePlan climatePlanFromBiomeNote(ServerLevel level, ItemStack stack) {
        if (!stack.is(WitcheryItems.BIOME_NOTE.get())) {
            return null;
        }
        Optional<ResourceKey<Biome>> biomeKey = BiomeNoteItem.getBiomeKey(stack);
        if (biomeKey.isEmpty()) {
            return null;
        }
        var lookup = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Optional<Holder.Reference<Biome>> biome = lookup.get(biomeKey.get());
        return biome.map(holder -> new ClimateChangePlan(holder, weatherForBiomeKey(biomeKey.get()))).orElse(null);
    }

    private static Holder<Biome> resolveClimateBiome(ServerLevel level, ClimateFocus focus, int glowstoneCount) {
        if (focus == ClimateFocus.FOREST) {
            return pickClimateBiome(level, glowstoneCount, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST);
        }
        if (focus == ClimateFocus.PLAINS) {
            return pickClimateBiome(level, glowstoneCount, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
        }
        if (focus == ClimateFocus.MOUNTAIN) {
            return pickClimateBiome(level, glowstoneCount, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS, Biomes.FROZEN_PEAKS);
        }
        if (focus == ClimateFocus.HILLS) {
            return pickClimateBiome(level, glowstoneCount, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
        }
        if (focus == ClimateFocus.SWAMP) {
            return pickClimateBiome(level, glowstoneCount, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
        }
        if (focus == ClimateFocus.WATER) {
            return pickClimateBiome(level, glowstoneCount, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_OCEAN);
        }
        if (focus == ClimateFocus.DESERT) {
            return pickClimateBiome(level, glowstoneCount, Biomes.DESERT);
        }
        if (focus == ClimateFocus.FROZEN) {
            return pickClimateBiome(level, glowstoneCount, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.FROZEN_PEAKS);
        }
        if (focus == ClimateFocus.JUNGLE) {
            return pickClimateBiome(level, glowstoneCount, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE);
        }
        if (focus == ClimateFocus.WASTELAND) {
            return pickClimateBiome(level, glowstoneCount, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
        }
        if (focus == ClimateFocus.BEACH) {
            return pickClimateBiome(level, glowstoneCount, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE);
        }
        if (focus == ClimateFocus.MUSHROOM) {
            return pickClimateBiome(level, glowstoneCount, Biomes.MUSHROOM_FIELDS);
        }
        return pickClimateBiome(level, glowstoneCount, Biomes.CHERRY_GROVE, Biomes.FLOWER_FOREST, Biomes.DARK_FOREST);
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
        if (focus == ClimateFocus.DESERT) {
            return ClimateWeather.SUN;
        }
        return focus == ClimateFocus.FROZEN ? ClimateWeather.RAIN : ClimateWeather.NONE;
    }

    private static ClimateWeather weatherForBiomeKey(ResourceKey<Biome> key) {
        if (key.equals(Biomes.DESERT) || key.equals(Biomes.BADLANDS)
                || key.equals(Biomes.ERODED_BADLANDS) || key.equals(Biomes.WOODED_BADLANDS)) {
            return ClimateWeather.SUN;
        }
        if (key.equals(Biomes.SNOWY_PLAINS) || key.equals(Biomes.ICE_SPIKES)
                || key.equals(Biomes.SNOWY_TAIGA) || key.equals(Biomes.SNOWY_BEACH)
                || key.equals(Biomes.FROZEN_PEAKS) || key.equals(Biomes.FROZEN_OCEAN)
                || key.equals(Biomes.DEEP_FROZEN_OCEAN)) {
            return ClimateWeather.RAIN;
        }
        return ClimateWeather.NONE;
    }

    private static void storeClimateChangePlan(ServerLevel level, BlockPos center, ClimateChangePlan plan) {
        climatePlans(level).put(center.immutable(), plan);
        markRitualsDirty(level);
    }

    private static ClimateChangePlan getClimateChangePlan(ServerLevel level, BlockPos center) {
        return climatePlans(level).get(center);
    }

    private static void clearClimateChangePlan(ServerLevel level, BlockPos center) {
        if (climatePlans(level).remove(center) != null) {
            markRitualsDirty(level);
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
        if (weather == ClimateWeather.SUN) {
            if (level.isRaining() || level.isThundering()) {
                level.setWeatherParameters(0, 0, false, false);
            }
            clearClimateSnow(level, center, radius);
        } else if (weather == ClimateWeather.RAIN && !level.isRaining() && !level.isThundering()) {
            int rainTicks = (300 + level.random.nextInt(600)) * 20;
            level.setWeatherParameters(0, rainTicks, true, false);
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
                || level.dimension().equals(WitcheryDimensions.TORMENT)
                || level.dimension().equals(WitcheryDimensions.MIRROR_WORLD)) {
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

    static int countNearbyCovenWitches(Level level, BlockPos center) {
        return level.getEntitiesOfClass(
                com.emoniph.witchery.entity.CovenWitchEntity.class,
                new AABB(center).inflate(16.0)
        ).size();
    }

    private static boolean hasNearbyRitualAltar(ServerLevel level, BlockPos center, int radius) {
        AltarBlockEntity altar = findNearbyRitualAltar(level, center, radius);
        return altar != null && altar.hasValidStructure() && altar.getPower() > 0;
    }

    private record FamiliarBindTarget(ServerPlayer player, TamableAnimal candidate) {}

    static boolean consumeNearbyRitualAltarPower(ServerLevel level, BlockPos center, int radius, int powerCost) {
        return consumeNearbyRawRitualAltarPower(level, center, radius, scaleRitualPowerCost(powerCost));
    }

    private static boolean consumeNearbyRawRitualAltarPower(ServerLevel level, BlockPos center, int radius, int powerCost) {
        AltarBlockEntity altar = findNearbyRitualAltar(level, center, radius);
        return altar != null && altar.hasValidStructure() && altar.consumePower(powerCost);
    }

    static int scaleRitualPowerCost(int powerCost) {
        if (powerCost <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(powerCost * WitcheryConfig.RITUAL_POWER_MULTIPLIER.get()));
    }

    private static float scaleRitualUpkeepCost(float upkeepCost) {
        if (upkeepCost <= 0.0f) {
            return 0.0f;
        }
        return (float)(upkeepCost * WitcheryConfig.RITUAL_POWER_MULTIPLIER.get());
    }

    private static boolean consumeNearbyRitualAltarPower(ServerLevel level, ActiveRitual ritual, int radius) {
        float upkeepCost = scaleRitualUpkeepCost(ritual.upkeepCost);
        if (upkeepCost <= 0.0f) {
            return true;
        }

        ritual.upkeepDebt += upkeepCost;
        int wholePower = Mth.floor(ritual.upkeepDebt);
        if (wholePower <= 0) {
            return hasNearbyRitualAltar(level, ritual.center, radius);
        }

        if (!consumeNearbyRawRitualAltarPower(level, ritual.center, radius, wholePower)) {
            return false;
        }

        ritual.upkeepDebt -= wholePower;
        return true;
    }

    static AltarBlockEntity findNearbyRitualAltar(Level level, BlockPos center, int radius) {
        return AltarPowerSourceLookup.findClosest(level, center);
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
        List<ActiveRitual> rituals = activeRituals(level);
        if (rituals.isEmpty()) {
            return;
        }

        for (int i = rituals.size() - 1; i >= 0; i--) {
            ActiveRitual ritual = rituals.get(i);
            if (tickActiveRitual(level, ritual)) {
                spawnActiveCenterParticle(level, ritual.center);
            } else {
                if (ritual.failedOnStop) {
                    level.playSound(null, ritual.center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                }
                if (ritual.refundOnStop) {
                    refundRitualIngredient(level, ritual);
                }
                rituals.remove(i);
            }
        }
        // Rituals advanced their stage/timers this pass — persist the change.
        markRitualsDirty(level);
    }

    private static void spawnActiveCenterParticle(ServerLevel level, BlockPos center) {
        double x = center.getX() + 0.4D + level.random.nextDouble() * 0.2D;
        double y = center.getY() + 1.1D + level.random.nextDouble() * 0.3D;
        double z = center.getZ() + 0.4D + level.random.nextDouble() * 0.2D;
        level.sendParticles(DustParticleOptions.REDSTONE, x, y, z, 2, 0.02D, 0.02D, 0.02D, 0.0D);
    }

    private static boolean tickActiveRitual(ServerLevel level, ActiveRitual ritual) {
        if (!hasSustainingCircle(level, ritual)) {
            clearClimateChangePlan(level, ritual.center);
            ritual.failedOnStop = true;
            ritual.refundOnStop = true;
            return false;
        }

        long gameTime = level.getGameTime();
        if (gameTime < ritual.nextGameTime) {
            return true;
        }

        ritual.nextGameTime = gameTime + ritual.pulseInterval;
        return tickActiveRitualByType(level, ritual);
    }

    private static boolean tickActiveRitualByType(ServerLevel level, ActiveRitual ritual) {
        ActiveRitualType type = ritual.type;
        if (type == ActiveRitualType.WEATHER_CALL_STORM) return tickStormRitual(level, ritual);
        if (type == ActiveRitualType.ICE_SHELL) return tickIceShellRitual(level, ritual);
        if (type == ActiveRitualType.RAIN_OF_TOADS) return tickRainOfToadsRitual(level, ritual);
        if (type == ActiveRitualType.BLINDNESS) return tickBlindnessRitual(level, ritual);
        if (type == ActiveRitualType.BLIGHT) return tickBlightRitual(level, ritual);
        if (type == ActiveRitualType.HELL_ON_EARTH) return tickHellOnEarthRitual(level, ritual);
        if (type == ActiveRitualType.CALL_CREATURES) return tickCallCreaturesRitual(level, ritual);
        if (type == ActiveRitualType.CLIMATE_CHANGE) return tickClimateChangeRitual(level, ritual);
        if (type == ActiveRitualType.NATURES_POWER) return tickNaturesPowerRitual(level, ritual);
        if (type == ActiveRitualType.TRANSPOSE_ORES) return tickTransposeOresRitual(level, ritual);
        if (type == ActiveRitualType.MOVING_EARTH) return tickMovingEarthRitual(level, ritual);
        if (type == ActiveRitualType.RAISE_COLUMN) return tickRaiseColumnRitual(level, ritual);
        if (type == ActiveRitualType.VOLCANO) return tickVolcanoRitual(level, ritual);
        if (type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS) return tickProtectionBarrierMonstersRitual(level, ritual);
        if (type == ActiveRitualType.PROTECTION_BARRIER_PORTABLE) return tickProtectionBarrierPortableRitual(level, ritual);
        if (type == ActiveRitualType.PROTECTION_BARRIER) return tickProtectionBarrierRitual(level, ritual);
        if (type == ActiveRitualType.INFUSION_RECHARGE) return tickInfusionRechargeRitual(level, ritual);
        if (type == ActiveRitualType.SANCTITY_CIRCLE) return tickSanctityCircleRitual(level, ritual);
        return tickImprisonmentCircleRitual(level, ritual);
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
        ClimateChangePlan plan = getClimateChangePlan(level, ritual.center);
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
        clearClimateChangePlan(level, ritual.center);
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
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, true, ritual.ownerName);
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
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, false, ritual.ownerName);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.effectCenter, ritual.minRadius, 12);
        }
        return true;
    }

    private static boolean tickProtectionBarrierPortableRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        drawProtectionBarrier(level, ritual.effectCenter, ritual.minRadius, ritual.maxRadius, true, ritual.ownerName);
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

    private static void drawProtectionBarrier(ServerLevel level, BlockPos center, int radius, int height, boolean blocksPlayers, String ownerName) {
        BlockPos circlePlaneCenter = center.above();
        drawBarrierFilledCircle(level, circlePlaneCenter.below(), radius, blocksPlayers, ownerName);
        drawBarrierCircleCylinder(level, circlePlaneCenter, radius, height, blocksPlayers, ownerName);
        drawBarrierFilledCircle(level, circlePlaneCenter.above(height), radius, blocksPlayers, ownerName);
    }

    private static void drawBarrierCircleCylinder(ServerLevel level, BlockPos center, int radius, int height, boolean blocksPlayers, String ownerName) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;

        while (x >= z) {
            drawBarrierPixelColumn(level, center, x, z, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, z, x, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, -x, z, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, -z, x, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, -x, -z, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, -z, -x, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, x, -z, height, blocksPlayers, ownerName);
            drawBarrierPixelColumn(level, center, z, -x, height, blocksPlayers, ownerName);

            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void drawBarrierPixelColumn(ServerLevel level, BlockPos center, int dx, int dz, int height, boolean blocksPlayers, String ownerName) {
        for (int dy = 0; dy < height; dy++) {
            drawBarrierPixel(level, center.offset(dx, dy, dz), blocksPlayers, ownerName);
        }
    }

    private static void drawBarrierFilledCircle(ServerLevel level, BlockPos center, int radius, boolean blocksPlayers, String ownerName) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;

        while (x >= z) {
            drawBarrierLine(level, center, -x, x, z, blocksPlayers, ownerName);
            drawBarrierLine(level, center, -z, z, x, blocksPlayers, ownerName);
            drawBarrierLine(level, center, -x, x, -z, blocksPlayers, ownerName);
            drawBarrierLine(level, center, -z, z, -x, blocksPlayers, ownerName);

            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void drawBarrierLine(ServerLevel level, BlockPos center, int x1, int x2, int z, boolean blocksPlayers, String ownerName) {
        for (int x = x1; x <= x2; x++) {
            drawBarrierPixel(level, center.offset(x, 0, z), blocksPlayers, ownerName);
        }
    }

    private static void drawBarrierPixel(ServerLevel level, BlockPos pos, boolean blocksPlayers, String ownerName) {
        com.emoniph.witchery.blocks.BarrierBlock.setBarrier(level, pos, 30, blocksPlayers, ownerName);
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
        return queueActiveRitual(level, type, circleSize, center, effectCenter, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, heading, supplementalSacrifices, enhanced, "");
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, BlockPos effectCenter, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading,
                                             List<ItemStack> supplementalSacrifices, boolean enhanced, String ownerName) {
        List<ActiveRitual> rituals = activeRituals(level);
        if (hasActiveRitualAt(level, center)) {
            return false;
        }

        int circleVariant = RitualCircleDetector.detectVariant(level, center, circleSize);
        ItemStack sacrificedIngredient = PENDING_RITUAL_INGREDIENT.get();
        rituals.add(new ActiveRitual(type, center, circleSize, effectCenter, circleVariant, minRadius, maxRadius, targetStage, pulseInterval,
                covenSize, upkeepCost, enhanced, heading,
                level.getGameTime() + pulseInterval,
                sacrificedIngredient.isEmpty() ? ItemStack.EMPTY : sacrificedIngredient.copy(),
                List.copyOf(supplementalSacrifices), ownerName));
        markRitualsDirty(level);
        return true;
    }

    private static boolean queueActiveRitual(ServerLevel level, ActiveRitualType type, RitualCircleDetector.CircleSize circleSize,
                                             BlockPos center, BlockPos effectCenter, int minRadius, int maxRadius, int targetStage,
                                             int pulseInterval, int covenSize, float upkeepCost, int heading,
                                             List<ItemStack> supplementalSacrifices, String ownerName) {
        return queueActiveRitual(level, type, circleSize, center, effectCenter, minRadius, maxRadius, targetStage,
                pulseInterval, covenSize, upkeepCost, heading, supplementalSacrifices, false, ownerName);
    }

    private static boolean hasActiveRitualAt(ServerLevel level, BlockPos center) {
        return activeRituals(level).stream().anyMatch(active -> active.center.equals(center));
    }

    public static String describeActiveRitual(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return "active=none";
        }
        List<ActiveRitual> rituals = activeRituals(serverLevel);
        for (ActiveRitual ritual : rituals) {
            if (!ritual.center.equals(center)) {
                continue;
            }
            long pulseIn = Math.max(0L, ritual.nextGameTime - level.getGameTime());
            String effect = ritual.effectCenter.equals(ritual.center)
                    ? ""
                    : " effect@" + ritual.effectCenter.toShortString();
            return "active=" + activeRitualName(ritual.type)
                    + " sustaining"
                    + " stage=" + ritual.stage
                    + " pulseIn=" + pulseIn + "t"
                    + " upkeep=" + ritual.upkeepCost
                    + " radius=" + ritual.minRadius
                    + effect;
        }
        return "active=none";
    }

    private static String activeRitualName(ActiveRitualType type) {
        if (type == ActiveRitualType.SANCTITY_CIRCLE) return "Sanctity";
        if (type == ActiveRitualType.IMPRISONMENT_CIRCLE) return "Imprisonment";
        if (type == ActiveRitualType.PROTECTION_BARRIER
                || type == ActiveRitualType.PROTECTION_BARRIER_MONSTERS
                || type == ActiveRitualType.PROTECTION_BARRIER_PORTABLE) {
            return "Barrier";
        }
        if (type == ActiveRitualType.INFUSION_RECHARGE) return "Infusion Recharge";
        if (type == ActiveRitualType.WEATHER_CALL_STORM) return "Storm";
        if (type == ActiveRitualType.ICE_SHELL) return "Ice Shell";
        if (type == ActiveRitualType.RAIN_OF_TOADS) return "Rain of Toads";
        if (type == ActiveRitualType.BLINDNESS) return "Blindness";
        if (type == ActiveRitualType.BLIGHT) return "Blight";
        if (type == ActiveRitualType.HELL_ON_EARTH) return "Hell on Earth";
        if (type == ActiveRitualType.CALL_CREATURES) return "Beastial Call";
        if (type == ActiveRitualType.CLIMATE_CHANGE) return "Climate Change";
        if (type == ActiveRitualType.NATURES_POWER) return "Nature's Power";
        if (type == ActiveRitualType.TRANSPOSE_ORES) return "Transpose Ores";
        if (type == ActiveRitualType.MOVING_EARTH) return "Moving Earth";
        if (type == ActiveRitualType.RAISE_COLUMN) return "Raise Column";
        return "Volcano";
    }

    private static boolean ensureActiveRitualCenterAvailable(Player player, ServerLevel level, BlockPos center) {
        if (hasActiveRitualAt(level, center)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
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

    private static Optional<BlockPos> findBlightHeartPos(ServerLevel level, BlockPos center) {
        return randomEmbeddedBlightHeartPos(level, center)
                .or(() -> fallbackEmbeddedBlightHeartPos(level, center));
    }

    private static Optional<BlockPos> randomEmbeddedBlightHeartPos(ServerLevel level, BlockPos center) {
        int radiusSq = BLIGHT_HEART_RANDOM_RADIUS * BLIGHT_HEART_RANDOM_RADIUS;
        for (int attempt = 0; attempt < 96; attempt++) {
            int dx = level.random.nextInt(BLIGHT_HEART_RANDOM_RADIUS * 2 + 1) - BLIGHT_HEART_RANDOM_RADIUS;
            int dz = level.random.nextInt(BLIGHT_HEART_RANDOM_RADIUS * 2 + 1) - BLIGHT_HEART_RANDOM_RADIUS;
            if (dx * dx + dz * dz > radiusSq) {
                continue;
            }

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    new BlockPos(center.getX() + dx, center.getY(), center.getZ() + dz));
            BlockPos heartPos = embeddedHeartPosFromSurface(level, surface);
            if (heartPos != null) {
                return Optional.of(heartPos);
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> fallbackEmbeddedBlightHeartPos(ServerLevel level, BlockPos center) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, center);
        return Optional.ofNullable(embeddedHeartPosFromSurface(level, surface));
    }

    private static BlockPos embeddedHeartPosFromSurface(ServerLevel level, BlockPos surface) {
        BlockPos heartPos = surface.below();
        while (heartPos.getY() > level.getMinY() && level.getBlockState(heartPos).canBeReplaced()) {
            heartPos = heartPos.below();
        }
        return BlightHelper.canPlaceEmbeddedHeartAt(level, heartPos) ? heartPos : null;
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
            level.setBlockAndUpdate(abovePos, Blocks.AIR.defaultBlockState());
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

    private static void spawnBlightRiteBurst(ServerLevel level, BlockPos center) {
        double x = center.getX() + 0.5D;
        double y = center.getY() + 1.0D;
        double z = center.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.PORTAL, x, y, z,
                60, 1.8D, 0.25D, 1.8D, 0.06D);
        level.sendParticles(ParticleTypes.WITCH, x, y + 0.15D, z,
                80, 2.2D, 0.35D, 2.2D, 0.08D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z,
                36, 1.1D, 0.2D, 1.1D, 0.04D);

        for (int radius = 1; radius <= 5; radius++) {
            int count = radius * 10;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                level.sendParticles(ParticleTypes.WITCH,
                        x + radius * Math.cos(angle),
                        y + 0.05D,
                        z + radius * Math.sin(angle),
                        1, 0.0D, 0.03D, 0.0D, 0.02D);
            }
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
    /** Rite of Necromancy — infuses an Attuned Stone into a Necromantic Stone at night. */
    private static boolean riteOfNecromanticStone(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireNight(player, level, center)) {
            return false;
        }
        Optional<NecromanticStoneOfferings> offerings = findNecromanticStoneOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.necro_stone.support"), false);
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 1000,
                "witchery.ritual.necro_stone.no_power")
                .offer(offerings.get().attunedStone())
                .offer(offerings.get().bone())
                .offer(offerings.get().rottenFlesh())
                .offer(offerings.get().woodAsh())
                .offer(offerings.get().gunpowder())
                .offer(offerings.get().spectralDust());
        if (!transaction.commit(player)) {
            return false;
        }

        ItemStack output = new ItemStack(WitcheryItems.NECRO_STONE.get());
        ItemEntity drop = new ItemEntity(sl, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, output);
        drop.setDeltaMovement(0.0, 0.15, 0.0);
        sl.addFreshEntity(drop);

        success(player, level, center, "witchery.ritual.necro_stone.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Necromancy created a Necromantic Stone at {}", center);
        return true;
    }

    /** Spectral Stone rite â€” condenses a Spectral Stone from an existing Necromantic Stone at night. */
    private static boolean riteOfSpectralStone(Player player, Level level, BlockPos center) {
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
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 6000,
                "witchery.ritual.spectral_stone.no_power")
                .offer(offerings.get().necroStone())
                .offer(offerings.get().congealedSpirit())
                .offer(offerings.get().condensedFear())
                .offer(offerings.get().spectralDust())
                .offer(offerings.get().boline());
        if (!transaction.commit(player)) {
            return false;
        }

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
        List<Entity> capturedSpectralEntities = findNearbySpectralCreatures(sl, center, storedType, 5.0, 3);
        if (capturedSpectralEntities.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.none"), false);
            return false;
        }
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 5000, "witchery.ritual.bind_spectral.no_power")
                .offer(offerings.get().spectralDust())
                .offer(offerings.get().boline());
        if (!transaction.commit(player)) {
            return false;
        }

        int count = consumeCapturedSpectralCreatures(sl, capturedSpectralEntities);
        if (count <= 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_spectral.none"), false);
            return false;
        }

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
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 3000,
                "witchery.ritual.infusion_tree.no_power")
                .offer(offerings.get().entTwig())
                .offer(offerings.get().mysticUnguent());
        if (!transaction.commit(player)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            return false;
        }

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
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000,
                "witchery.ritual.infusion_future.no_power")
                .offer(offerings.get().quartzSphere())
                .offer(offerings.get().goldIngot())
                .offer(offerings.get().happenstanceOil());
        if (!transaction.commit(player)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            return false;
        }

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
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000,
                "witchery.ritual.infusion_seer.no_power")
                .offer(offerings.get().quartzSphere())
                .offer(offerings.get().mossyCobblestone())
                .offer(offerings.get().happenstanceOil());
        if (!transaction.commit(player)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            return false;
        }

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
            RitualTransaction transaction = new RitualTransaction(sl, center, 16, 3000,
                    "witchery.ritual.infuse_brew.no_power")
                    .offer(soaringOfferings.get().infusedBrewBase())
                    .offer(soaringOfferings.get().broom())
                    .offer(soaringOfferings.get().feather())
                    .offer(soaringOfferings.get().arthana());
            if (!transaction.commit(player)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                return false;
            }

            soaringOfferings.get().owl().discard();

            ItemEntity drop = new ItemEntity(sl,
                    center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                    new ItemStack(WitcheryItems.INFUSED_SOARING_BREW.get()));
            drop.setDeltaMovement(0.0, 0.25, 0.0);
            sl.addFreshEntity(drop);

            success(player, level, center, "witchery.ritual.infuse_brew_soaring.success",
                    SoundEvents.BEACON_POWER_SELECT, 1.0f, 3, 40);
            WitcheryMod.LOGGER.info("Rite of Infusion created Brew of Soaring at {}", center);
            return true;
        }

        Optional<InfuseBrewGraveOfferings> graveOfferings = findInfuseBrewGraveOfferings(level, center);
        if (graveOfferings.isPresent()) {
            RitualTransaction transaction = new RitualTransaction(sl, center, 16, 3000,
                    "witchery.ritual.infuse_brew.no_power")
                    .offer(graveOfferings.get().infusedBrewBase())
                    .offer(graveOfferings.get().bone())
                    .offer(graveOfferings.get().denseWeb())
                    .offer(graveOfferings.get().necroStone());
            if (!transaction.commit(player)) {
                level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                return false;
            }

            graveOfferings.get().zombie().discard();

            ItemEntity drop = new ItemEntity(sl,
                    center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                    new ItemStack(WitcheryItems.INFUSED_GRAVE_BREW.get()));
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

    private static <T extends Entity> List<T> findNearestEntities(ServerLevel level, Class<T> entityClass,
                                                                  BlockPos center, double radius, int maxCount) {
        AABB area = new AABB(center).inflate(radius);
        Vec3 centerVec = Vec3.atCenterOf(center);
        double radiusSquared = radius * radius;

        List<T> candidates = level.getEntitiesOfClass(entityClass, area);
        List<T> sorted = new ArrayList<>();
        for (T candidate : candidates) {
            if (candidate.distanceToSqr(centerVec) <= radiusSquared) {
                sorted.add(candidate);
            }
        }
        sorted.sort((left, right) -> Double.compare(left.distanceToSqr(centerVec), right.distanceToSqr(centerVec)));
        if (sorted.size() > maxCount) {
            return new ArrayList<>(sorted.subList(0, maxCount));
        }
        return sorted;
    }

    private static List<Entity> findNearbySpectralCreatures(ServerLevel level, BlockPos center, SpectralStoneItem.StoredType type,
                                                            double radius, int maxCount) {
        if (type == SpectralStoneItem.StoredType.SPECTRE) {
            return new ArrayList<>(findNearestEntities(level, com.emoniph.witchery.entity.SpectreEntity.class, center, radius, maxCount));
        }
        if (type == SpectralStoneItem.StoredType.BANSHEE) {
            return new ArrayList<>(findNearestEntities(level, com.emoniph.witchery.entity.BansheeEntity.class, center, radius, maxCount));
        }
        if (type == SpectralStoneItem.StoredType.POLTERGEIST) {
            return new ArrayList<>(findNearestEntities(level, com.emoniph.witchery.entity.PoltergeistEntity.class, center, radius, maxCount));
        }
        return List.of();
    }

    private static int consumeCapturedEntities(ServerLevel level, List<? extends Entity> capturedEntities) {
        int count = 0;
        for (Entity entity : capturedEntities) {
            if (!entity.isAlive() || entity.isRemoved()) {
                continue;
            }
            entity.discard();
            level.playSound(null, entity.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 0.4f, 1.4f);
            level.sendParticles(ParticleTypes.PORTAL,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    12, 0.2, 0.2, 0.2, 0.05);
            count++;
        }
        return count;
    }

    private static int consumeCapturedSpectralCreatures(ServerLevel level, List<Entity> capturedEntities) {
        return consumeCapturedEntities(level, capturedEntities);
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
                    BlockState next = targetBlock.defaultBlockState();
                    if (state.hasProperty(ChalkCircleBlock.GLYPH)) {
                        next = ChalkCircleBlock.withGlyph(next, state.getValue(ChalkCircleBlock.GLYPH));
                    }
                    level.setBlockAndUpdate(targetPos, next);
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
        List<ActiveRitual> rituals = activeRituals(sl);
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
        Optional<BlockPos> heartPos = findBlightHeartPos(sl, center);
        if (heartPos.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.blight.no_ground"), false);
            return false;
        }
        if (hasActiveRitualAt(sl, center)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        if (!consumeNearbyRitualAltarPower(sl, center, 16, BLIGHT_RITE_POWER)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.blight.no_power"), false);
            return false;
        }
        consumeOfferedItem(offerings.get().chargedStone());
        consumeOfferedItem(offerings.get().redstoneSoup());
        consumeOfferedItem(offerings.get().reekOfMisfortune());
        consumeOfferedItem(offerings.get().fermentedSpiderEye());
        consumeOfferedItem(offerings.get().poisonousPotato());
        consumeOfferedItem(offerings.get().rottenFlesh());
        consumeOfferedItem(offerings.get().diamond());
        BlightHelper.placeHeart(sl, heartPos.get(), true);
        level.playSound(null, center, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0f, 0.5f);
        spawnBlightRiteBurst(sl, center);
        player.displayClientMessage(Component.translatable("witchery.ritual.blight.success"), false);
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
        if (!ensureActiveRitualCenterAvailable(player, sl, center)) return false;
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

        List<ActiveRitual> rituals = activeRituals(sl);
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
            String supportKey = raiseColumnSupportKey(circleSize);
            player.displayClientMessage(Component.translatable(supportKey), false);
            return false;
        }

        int baseRadius = raiseColumnBaseRadius(circleSize);
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

    private static String raiseColumnSupportKey(RitualCircleDetector.CircleSize circleSize) {
        if (circleSize == RitualCircleDetector.CircleSize.SMALL) {
            return "witchery.ritual.raise_column.small_support";
        }
        return circleSize == RitualCircleDetector.CircleSize.MEDIUM
                ? "witchery.ritual.raise_column.medium_support"
                : "witchery.ritual.raise_column.large_support";
    }

    private static int raiseColumnBaseRadius(RitualCircleDetector.CircleSize circleSize) {
        if (circleSize == RitualCircleDetector.CircleSize.SMALL) {
            return 4;
        }
        return circleSize == RitualCircleDetector.CircleSize.MEDIUM ? 6 : 9;
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

    /** Curse Creature — current port approximation targets the bound victim directly. */
    private static boolean riteOfCurseCreature(Player player, Level level, BlockPos center) {
        if (!requireThunderstorm(player, level, center)) return false;
        Optional<CurseCreatureOfferings> offerings = findCurseCreatureOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.curse_creature.support"), false);
            return false;
        }
        int potencyBonus = cursePotencyBonus(player, level, center);
        boolean applied = performOfferedCurse(player, level, center,
                offerings.get().taglockKit(),
                2000,
                "witchery.ritual.curse_creature.no_power",
                target -> {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200 + (potencyBonus * 60), Math.min(2, potencyBonus)));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200 + (potencyBonus * 60), 1 + Math.min(2, potencyBonus)));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200 + (potencyBonus * 60), Math.min(2, potencyBonus)));
                },
                "witchery.curse.cursed",
                "witchery.curse.cursed",
                true,
                offerings.get().exhaleOfHornedOne(),
                offerings.get().spiderEye(),
                offerings.get().gunpowder(),
                offerings.get().brewGrotesque());
        if (!applied) {
            return false;
        }
        WitcheryMod.LOGGER.info("Curse Creature afflicted the filled-taglock target");
        return true;
    }

    /** Curse of Corrupt Poppet — destroys bound voodoo-protection poppets on the taglock target. */
    private static boolean riteOfCorruptPoppet(Player caster, Level level, BlockPos center) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        if (!hasActiveCurseMasteryFamiliar(sp)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.requirescursemastery"), false);
            return false;
        }

        Optional<ItemEntity> taglock = findNearbyFilledTaglockOffering(level, center);
        if (taglock.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.no_taglock"), false);
            return false;
        }

        Optional<ServerPlayer> target = resolveOfferedTaglockTarget(
                caster, taglock.get(),
                "witchery.ritual.corrupt_poppet.no_taglock",
                "witchery.ritual.corrupt_poppet.offline");
        if (target.isEmpty()) {
            return false;
        }

        Optional<CorruptPoppetOfferings> offerings = findCorruptPoppetOfferings(level, center);
        if (offerings.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.support"), false);
            return false;
        }

        if (PoppetEvents.damageBoundPoppet(target.get(), WitcheryItems.POPPET_PROTECTION_POPPET.get(), 1000)) {
            target.get().displayClientMessage(Component.translatable("witchery.poppet.shield.activated")
                    .withStyle(ChatFormatting.GOLD), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            strikeCurseBacklashLightning(sp);
            success(sp, level, center, null, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 2, 32);
            return true;
        }

        if (!PoppetEvents.hasBoundPoppet(target.get(), WitcheryItems.VOODOO_PROTECTION_POPPET.get())) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.none"), false);
            return false;
        }

        if (!(level instanceof ServerLevel sl)) return false;
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 7000, "witchery.ritual.corrupt_poppet.no_power")
                .offer(taglock.get())
                .offer(offerings.get().exhaleOfHornedOne())
                .offer(offerings.get().blazePowder())
                .offer(offerings.get().spectralDust());
        if (!transaction.commit(sp)) {
            return false;
        }
        int destroyed = PoppetEvents.destroyBoundPoppets(target.get(), WitcheryItems.VOODOO_PROTECTION_POPPET.get(), 1000, 10);
        success(sp, level, center, null, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 2, 32);
        sp.displayClientMessage(Component.translatable("witchery.ritual.corrupt_poppet.success", destroyed), false);
        WitchHunterEntity.blackMagicPerformed(sp);
        WitcheryMod.LOGGER.info("Curse of Corrupt Poppet destroyed {} voodoo protection poppet(s) for {}", destroyed, target.get().getName().getString());
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

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 1000, "witchery.ritual.call_familiar.no_power")
                .offer(offerings.get().breathOfTheGoddess())
                .offer(offerings.get().hintOfRebirth())
                .offer(offerings.get().whiffOfMagic());
        if (!transaction.commit(player)) {
            return false;
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
            bound = familiar;
        }

        FamiliarHelper.promoteBoundFamiliar(bound);
        FamiliarHelper.ensureDefaultFamiliarName(bound, level.getRandom());

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
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        net.minecraft.world.entity.monster.Witch witch = EntityType.WITCH.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (witch == null) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000, "witchery.ritual.summon_witch.no_power")
                .offer(offerings.get().diamondVapour())
                .offer(offerings.get().exhaleOfHornedOne())
                .offer(offerings.get().magmaCream())
                .offer(offerings.get().arthana())
                .offer(offerings.get().fermentedSpiderEye());
        if (!transaction.commit(player)) {
            return false;
        }

        witch.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        witch.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(witch);

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
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var imp = WitcheryEntities.FLAME_IMP.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (imp == null) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 5000, "witchery.ritual.summon_imp.no_power")
                .offer(offerings.get().refinedEvil())
                .offer(offerings.get().infernalBlood())
                .offer(offerings.get().enderPearl())
                .offer(offerings.get().attunedStone());
        if (!transaction.commit(player)) {
            return false;
        }

        imp.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        imp.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(imp);

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
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var wither = EntityType.WITHER.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (wither == null) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, useExpensive ? 0 : 4000,
                "witchery.ritual.summon_wither.no_power");
        if (useExpensive) {
            transaction.offer(expensiveOfferings.get().diamond())
                    .offer(expensiveOfferings.get().enderPearl())
                    .offer(expensiveOfferings.get().attunedStone())
                    .offer(expensiveOfferings.get().chargedStone());
        } else {
            transaction.offer(offerings.get().diamondVapour())
                    .offer(offerings.get().enderPearl());
        }
        if (!transaction.commit(player)) {
            return false;
        }

        wither.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        wither.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(wither);

        if (!useExpensive) {
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
        if (hasSummonCircleObstruction(sl, center)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.obstructedcircle"), false);
            return false;
        }

        var demon = WitcheryEntities.DEMON.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (demon == null) {
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 3000, "witchery.ritual.summon_demon.no_power");
        if (useExpensive) {
            transaction.offer(expensiveOfferings.get().refinedEvil())
                    .offer(expensiveOfferings.get().blazeRod())
                    .offer(expensiveOfferings.get().enderPearl())
                    .offer(expensiveOfferings.get().attunedStone())
                    .offer(expensiveOfferings.get().chargedStone());
        } else {
            transaction.offer(offerings.get().refinedEvil())
                    .offer(offerings.get().blazePowder())
                    .offer(offerings.get().enderPearl());
        }
        if (!transaction.commit(player)) {
            return false;
        }

        demon.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        demon.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(demon);

        if (!useExpensive) {
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
        double burnChance = charged ? 0.16 : 0.08;
        List<ItemEntity> cookableItems = findCookableFoodItems(sl, new AABB(center).inflate(5));
        if (cookableItems.isEmpty()) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.cook.none"), false);
            return false;
        }

        if (charged) {
            consumeOfferedItem(chargedOfferings.get().chargedStone());
            consumeOfferedItem(chargedOfferings.get().woodAsh());
            consumeOfferedItem(chargedOfferings.get().blazePowder());
        } else {
            RitualTransaction transaction = new RitualTransaction(sl, center, 16, 1000,
                    "witchery.ritual.cook.no_power")
                    .offer(normalOfferings.get().woodAsh())
                    .offer(normalOfferings.get().coal());
            if (!transaction.commit(player)) {
                return false;
            }
        }

        int cooked = 0;
        for (ItemEntity ie : cookableItems) {
            ItemStack stack = ie.getItem();
            if (!ie.isAlive() || stack.getCount() <= 0) {
                continue;
            }
            ItemStack result = smeltedFoodResult(sl, stack);
            if (result.isEmpty()) {
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

        player.displayClientMessage(Component.translatable("witchery.ritual.cook.success", cooked), false);
        level.playSound(null, center, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
        spawnRingParticles(sl, center, 3, 24);
        return true;
    }

    private static List<ItemEntity> findCookableFoodItems(ServerLevel level, AABB search) {
        List<ItemEntity> cookable = new ArrayList<>();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, search)) {
            if (smeltedFoodResult(level, itemEntity.getItem()).isEmpty()) {
                continue;
            }
            cookable.add(itemEntity);
        }
        return cookable;
    }

    private static ItemStack smeltedFoodResult(ServerLevel level, ItemStack stack) {
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack), level.registryAccess());
        if (result.isEmpty() || !result.has(DataComponents.FOOD) || stack.getCount() <= 0) {
            return ItemStack.EMPTY;
        }
        return result;
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

        List<com.emoniph.witchery.entity.SpiritEntity> spirits = findNearestEntities(
                sl, com.emoniph.witchery.entity.SpiritEntity.class, center, 16.0, 3);
        List<com.emoniph.witchery.entity.SpectreEntity> spectres = findNearestEntities(
                sl, com.emoniph.witchery.entity.SpectreEntity.class, center, 16.0, 3);
        List<com.emoniph.witchery.entity.BansheeEntity> banshees = findNearestEntities(
                sl, com.emoniph.witchery.entity.BansheeEntity.class, center, 16.0, 2);
        List<com.emoniph.witchery.entity.PoltergeistEntity> poltergeists = findNearestEntities(
                sl, com.emoniph.witchery.entity.PoltergeistEntity.class, center, 16.0, 2);

        int effectId = matchFetishEffect(spirits.size(), spectres.size(), banshees.size(), poltergeists.size());
        if (effectId == 0) {
            player.displayClientMessage(Component.translatable("witchery.ritual.fetish.no_spirits"), false);
            return false;
        }
        List<Entity> capturedSpirits = new ArrayList<>();
        capturedSpirits.addAll(new ArrayList<>(spirits.subList(0, spiritCount(effectId))));
        capturedSpirits.addAll(new ArrayList<>(spectres.subList(0, spectreCount(effectId))));
        capturedSpirits.addAll(new ArrayList<>(banshees.subList(0, bansheeCount(effectId))));
        capturedSpirits.addAll(new ArrayList<>(poltergeists.subList(0, poltergeistCount(effectId))));

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 6000, "witchery.ritual.bind_fetish.no_power")
                .offer(offerings.get().attunedStone())
                .offer(offerings.get().necroStone())
                .offer(offerings.get().boline());
        if (!transaction.commit(player)) {
            return false;
        }

        if (consumeCapturedEntities(sl, capturedSpirits) <= 0) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.ritual.fetish.no_spirits"), false);
            return false;
        }

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

    // ── Taglock / Waystone helpers ────────────────────────────────────────────

    /** Consume one item from the given inventory slot after the ritual target has been validated. */
    static void consumeInventoryItem(Player player, int slot) {
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

    private static int cursePotencyBonus(Player caster, Level level, BlockPos center) {
        int bonus = countNearbyCovenWitches(level, center) / 2;
        if (caster instanceof ServerPlayer sp && hasActiveCurseMasteryFamiliar(sp)) {
            bonus++;
        }
        return bonus;
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
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ItemEntity> enderDew = findNearbyOffering(level, center, stack -> stack.is(WitcheryItems.ENDER_DEW.get()));
        Optional<ItemEntity> glowstone = findNearbyOffering(level, center, stack -> stack.is(Items.GLOWSTONE_DUST));
        if (enderDew.isEmpty() || glowstone.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_waystone.support"), false);
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 0, null)
                .offer(enderDew.get())
                .offer(glowstone.get());
        if (!transaction.commit(player)) {
            return false;
        }

        ItemStack boundWaystone = new ItemStack(WitcheryItems.BOUND_WAYSTONE.get());
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

    /** Rite of Binding (entity) - summons a source-style blooded Waystone bound to a nearby living target. */
    private static boolean riteOfBindBloodedWaystone(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<BindBloodedWaystoneOfferings> offerings = findBindBloodedWaystoneOfferings(level, center);
        if (offerings.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_blooded_waystone.support"), false);
            return false;
        }
        Optional<LivingEntity> target = findBloodedWaystoneTarget(level, center);
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.bind_blooded_waystone.no_target"), false);
            return false;
        }

        BindBloodedWaystoneOfferings support = offerings.get();
        RitualTransaction transaction = new RitualTransaction(sl, center, 16, support.needsPower() ? 500 : 0,
                support.needsPower() ? "witchery.ritual.bind_blooded_waystone.no_power" : null)
                .offer(support.enderDew())
                .offer(support.slimeBall())
                .offer(support.snowball())
                .offer(support.icyNeedle())
                .offer(support.chargedStone());
        if (!transaction.commit(player)) {
            return false;
        }

        ItemStack bloodedWaystone = new ItemStack(WitcheryItems.BLOODED_WAYSTONE.get());
        WaystoneItem.bindToEntity(bloodedWaystone, target.get());
        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5,
                center.getY() + 1.5,
                center.getZ() + 0.5,
                bloodedWaystone);
        entity.setDeltaMovement(0.0, 0.3, 0.0);
        level.addFreshEntity(entity);
        success(player, level, center, "witchery.ritual.bind_blooded_waystone.success",
                SoundEvents.BEACON_ACTIVATE, 1f, 2, 32);
        WitcheryMod.LOGGER.info("Blooded waystone summoned at {} and bound to {} for {}",
                center, target.get().getName().getString(), player.getName().getString());
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

        RitualTransaction transaction = new RitualTransaction((ServerLevel) level, center, 16, 0, null)
                .offer(unboundWaystone.get())
                .offer(enderDew.get())
                .offer(diamond.get());
        if (!transaction.commit(player)) {
            return false;
        }

        ItemStack copiedWaystones = sourceWaystone.copyWithCount(2)
                .transmuteCopy(WitcheryItems.BOUND_WAYSTONE.get());
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

    static void consumeOfferedItem(ItemEntity itemEntity) {
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
        if (WaystoneItem.isEntityBoundWaystone(waystone)) {
            return riteOfTranspositionToBloodedWaystone(sp, level, center, waystone);
        }
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

    /** Blooded Waystone transposition targets the live entity stored in the waystone's taglock data. */
    private static boolean riteOfTranspositionToBloodedWaystone(ServerPlayer player, Level level, BlockPos center, ItemStack waystone) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<LivingEntity> target = WaystoneItem.findBoundEntity(sl.getServer(), waystone);
        if (target.isEmpty() || !target.get().isAlive() || !(target.get().level() instanceof ServerLevel targetLevel)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }

        LivingEntity living = target.get();
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
            teleportEntityTo(entity, targetLevel, living.getX(), living.getY(), living.getZ());
            moved++;
        }
        success(player, level, center, "witchery.ritual.transposition.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Transposition moved {} entity(s) from {} to blooded waystone target {}",
                moved, center, living.getName().getString());
        return true;
    }

    private static void teleportEntityToWaystone(Entity entity, ServerLevel targetLevel, BlockPos dest) {
        double x = dest.getX() + 0.5;
        double y = dest.getY();
        double z = dest.getZ() + 0.5;
        teleportEntityTo(entity, targetLevel, x, y, z);
    }

    private static void teleportEntityTo(Entity entity, ServerLevel targetLevel, double x, double y, double z) {
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
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return false;
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

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 3000,
                "witchery.ritual.transposition_entity.no_power")
                .offer(offerings.get().waystone())
                .offer(offerings.get().enderDew())
                .offer(offerings.get().ironAxe());
        if (!transaction.commit(player)) {
            return false;
        }

        BlockPos destination = center.above();
        if (target.level() == level) {
            target.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        } else {
            target.teleportTo(sl, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                    java.util.Set.<net.minecraft.world.entity.Relative>of(), target.getYRot(), target.getXRot(), false);
        }
        target.displayClientMessage(Component.translatable("witchery.ritual.transposition_entity.pulled"), false);
        success(player, level, center, "witchery.ritual.transposition_entity.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Transposition Entity moved {} to {}", target.getName().getString(), destination);
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

    private static boolean applyCurse(Player caster, Level level, BlockPos center, ItemEntity offeredTaglock,
                                      CurseAction action, String successKey, String failKey,
                                      boolean checkHunterArmorProtection) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        Optional<UUID> taglockUuid = TaglockKitItem.getUUID(offeredTaglock.getItem());
        if (taglockUuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable(failKey + ".no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglockUuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable(failKey + ".offline"), false);
            return false;
        }
        if (checkHunterArmorProtection && hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }
        consumeOfferedItem(offeredTaglock);
        action.apply(target);
        target.displayClientMessage(Component.translatable(successKey + ".target"), false);
        success(caster, level, center, successKey + ".caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
        WitchHunterEntity.blackMagicPerformed(sp);
        return true;
    }

    private static boolean performOfferedCurse(Player caster, Level level, BlockPos center, ItemEntity offeredTaglock,
                                               int altarPowerCost, String noPowerKey, CurseAction action,
                                               String successKey, String failKey, boolean checkHunterArmorProtection,
                                               ItemEntity... offerings) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<UUID> taglockUuid = TaglockKitItem.getUUID(offeredTaglock.getItem());
        if (taglockUuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable(failKey + ".no_taglock"), false);
            return false;
        }
        ServerPlayer target = sl.getServer().getPlayerList().getPlayer(taglockUuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable(failKey + ".offline"), false);
            return false;
        }
        if (checkHunterArmorProtection && hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, altarPowerCost, noPowerKey)
                .offer(offeredTaglock);
        for (ItemEntity offering : offerings) {
            transaction.offer(offering);
        }
        if (!transaction.commit(caster)) {
            return false;
        }

        action.apply(target);
        target.displayClientMessage(Component.translatable(successKey + ".target"), false);
        success(caster, level, center, successKey + ".caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
        WitchHunterEntity.blackMagicPerformed(sp);
        return true;
    }

    @FunctionalInterface
    private interface CurePrecheck { boolean test(ServerPlayer target); }

    @FunctionalInterface
    private interface CureEffect { void apply(ServerPlayer target); }

    private static Optional<ServerPlayer> resolveOfferedTaglockTarget(Player caster, ItemEntity offeredTaglock,
                                                                      String noTaglockKey, String offlineKey) {
        if (!(caster instanceof ServerPlayer sp)) return Optional.empty();
        Optional<UUID> taglockUuid = TaglockKitItem.getUUID(offeredTaglock.getItem());
        if (taglockUuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable(noTaglockKey), false);
            return Optional.empty();
        }
        ServerPlayer target = ((ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(taglockUuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable(offlineKey), false);
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private static boolean performOfferedCure(Player caster, Level level, BlockPos center, ItemEntity offeredTaglock,
                                              CurePrecheck precheck, CureEffect effect, String targetKey,
                                              ItemEntity... offerings) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<ServerPlayer> target = resolveOfferedTaglockTarget(
                caster, offeredTaglock, "witchery.ritual.cure.no_taglock", "witchery.ritual.cure.offline");
        if (target.isEmpty()) {
            return false;
        }
        if (!precheck.test(target.get())) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.cure.none"), false);
            return false;
        }

        RitualTransaction transaction = new RitualTransaction(sl, center, 16, 2000, "witchery.ritual.cure.no_power")
                .offer(offeredTaglock);
        for (ItemEntity offering : offerings) {
            transaction.offer(offering);
        }
        if (!transaction.commit(caster)) {
            return false;
        }

        effect.apply(target.get());
        target.get().displayClientMessage(Component.translatable(targetKey), false);
        success(caster, level, center, "witchery.ritual.cure.success",
                SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.8f, 3, 48);
        return true;
    }

    private static boolean triggerVoodooProtection(ServerPlayer player) {
        return PoppetEvents.damageBoundPoppet(player, WitcheryItems.VOODOO_PROTECTION_POPPET.get(), 350);
    }

    private static boolean hasCurseProtection(ServerPlayer player) {
        return SilverEvents.hasHunterCurseProtection(player);
    }

    private static boolean riteOfBreathOfTheGoddessCure(Player caster, Level level, BlockPos center) {
        Optional<CureMisfortuneOfferings> misfortune = findCureMisfortuneOfferings(level, center);
        if (misfortune.isPresent()) {
            return performOfferedCure(caster, level, center,
                    misfortune.get().taglockKit(),
                    target -> target.hasEffect(MobEffects.UNLUCK),
                    target -> target.removeEffect(MobEffects.UNLUCK),
                    "curse.witchery.misfortune.goddess_removal",
                    misfortune.get().breathOfTheGoddess(),
                    misfortune.get().goldNugget(),
                    misfortune.get().bread(),
                    misfortune.get().brewOfLove());
        }

        Optional<CureCreatureOfferings> curseCreature = findCureCreatureOfferings(level, center);
        if (curseCreature.isPresent()) {
            return performOfferedCure(caster, level, center,
                    curseCreature.get().taglockKit(),
                    target -> target.hasEffect(MobEffects.WITHER)
                            || target.hasEffect(MobEffects.SLOWNESS)
                            || target.hasEffect(MobEffects.WEAKNESS),
                    target -> {
                        target.removeEffect(MobEffects.WITHER);
                        target.removeEffect(MobEffects.SLOWNESS);
                        target.removeEffect(MobEffects.WEAKNESS);
                    },
                    "curse.witchery.cursed.goddess_removal",
                    curseCreature.get().breathOfTheGoddess(),
                    curseCreature.get().spiderEye(),
                    curseCreature.get().gunpowder(),
                    curseCreature.get().brewOfLove());
        }

        Optional<CureWakingNightmareOfferings> wakingNightmare = findCureWakingNightmareOfferings(level, center);
        if (wakingNightmare.isPresent()) {
            return performOfferedCure(caster, level, center,
                    wakingNightmare.get().taglockKit(),
                    target -> {
                        WitcheryPlayerData data = WitcheryAttachments.get(target);
                        return data.isDemonicNightmare() || (data.isInSpiritWorld() && !data.isSpiritWorldDream());
                    },
                    target -> {
                        WitcheryPlayerData data = WitcheryAttachments.get(target);
                        data.setSpiritWorldIsDream(true);
                        data.setDemonicNightmare(false);
                        PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
                    },
                    "curse.witchery.waking_nightmare.goddess_removal",
                    wakingNightmare.get().breathOfTheGoddess(),
                    wakingNightmare.get().goldenCarrot(),
                    wakingNightmare.get().tormentedTwine(),
                    wakingNightmare.get().brewOfLove());
        }

        Optional<CureOverheatingOfferings> overheating = findCureOverheatingOfferings(level, center);
        if (overheating.isPresent()) {
            return performOfferedCure(caster, level, center,
                    overheating.get().taglockKit(),
                    target -> target.hasEffect(MobEffects.WEAKNESS) || target.isOnFire(),
                    target -> {
                        target.removeEffect(MobEffects.WEAKNESS);
                        target.clearFire();
                    },
                    "curse.witchery.overheating.goddess_removal",
                    overheating.get().breathOfTheGoddess(),
                    overheating.get().icyNeedle(),
                    overheating.get().blazeRod(),
                    overheating.get().brewOfTheDepths());
        }

        Optional<CureSinkingOfferings> sinking = findCureSinkingOfferings(level, center);
        if (sinking.isPresent()) {
            return performOfferedCure(caster, level, center,
                    sinking.get().taglockKit(),
                    target -> target.hasEffect(MobEffects.SLOWNESS)
                            || target.hasEffect(MobEffects.MINING_FATIGUE)
                            || target.hasEffect(MobEffects.LEVITATION),
                    target -> {
                        target.removeEffect(MobEffects.SLOWNESS);
                        target.removeEffect(MobEffects.MINING_FATIGUE);
                        target.removeEffect(MobEffects.LEVITATION);
                    },
                    "curse.witchery.sinking.goddess_removal",
                    sinking.get().breathOfTheGoddess(),
                    sinking.get().boneMeal(),
                    sinking.get().netherWart(),
                    sinking.get().brewOfTheDepths());
        }

        Optional<CureInsanityOfferings> insanity = findCureInsanityOfferings(level, center);
        if (insanity.isPresent()) {
            return performOfferedCure(caster, level, center,
                    insanity.get().taglockKit(),
                    target -> target.hasEffect(MobEffects.NAUSEA) || target.hasEffect(MobEffects.BLINDNESS),
                    target -> {
                        target.removeEffect(MobEffects.NAUSEA);
                        target.removeEffect(MobEffects.BLINDNESS);
                    },
                    "curse.witchery.insanity.goddess_removal",
                    insanity.get().breathOfTheGoddess(),
                    insanity.get().potato(),
                    insanity.get().sugar(),
                    insanity.get().brewOfLove());
        }

        return false;
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
        int potencyBonus = cursePotencyBonus(caster, level, center);
        return performOfferedCurse(caster, level, center,
                offerings.get().taglockKit(),
                2000,
                "witchery.ritual.curse_overheating.no_power",
                target -> {
            target.igniteForTicks(200 + (potencyBonus * 80));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200 + (potencyBonus * 200), Math.min(2, potencyBonus)));
                },
                "witchery.curse.overheating",
                "witchery.curse.overheating",
                false,
                offerings.get().exhaleOfHornedOne(),
                offerings.get().demonicBlood(),
                offerings.get().blazeRod(),
                offerings.get().brewGrotesque());
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
        int potencyBonus = cursePotencyBonus(caster, level, center);
        return performOfferedCurse(caster, level, center,
                offerings.get().taglockKit(),
                2000,
                "witchery.ritual.curse_sinking.no_power",
                target -> {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2400 + (potencyBonus * 300), 3 + Math.min(1, potencyBonus)));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 2400 + (potencyBonus * 300), 2 + Math.min(1, potencyBonus)));
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20 + (potencyBonus * 10), 0)); // brief sink
                },
                "witchery.curse.sinking",
                "witchery.curse.sinking",
                false,
                offerings.get().exhaleOfHornedOne(),
                offerings.get().inkSac(),
                offerings.get().netherWart(),
                offerings.get().brewGrotesque());
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
        int potencyBonus = cursePotencyBonus(caster, level, center);
        return performOfferedCurse(caster, level, center,
                offerings.get().taglockKit(),
                2000,
                "witchery.ritual.curse_insanity.no_power",
                target -> {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 3600 + (potencyBonus * 400), 1 + Math.min(1, potencyBonus)));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100 + (potencyBonus * 40), 0));
                },
                "witchery.curse.insanity",
                "witchery.curse.insanity",
                false,
                offerings.get().exhaleOfHornedOne(),
                offerings.get().poisonousPotato(),
                offerings.get().sugar(),
                offerings.get().brewGrotesque());
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
        if (!ensureActiveRitualCenterAvailable(caster, sl, center)) return false;
        if (!consumeNearbyRitualAltarPower(sl, center, 16, 5000)) {
            level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            caster.displayClientMessage(Component.translatable("witchery.ritual.hell_on_earth.no_power"), false);
            return false;
        }
        int covenSize = countNearbyCovenWitches(level, center);
        BlockPos effectCenter = offerings.get().effectCenter();
        List<ItemStack> supplementalSacrifices = offerings.get().boundWaystone() != null
                ? List.of(offerings.get().boundWaystone().getItem().copyWithCount(1))
                : List.of();
        if (!queueActiveRitual(sl, ActiveRitualType.HELL_ON_EARTH, RitualCircleDetector.CircleSize.LARGE,
                center, effectCenter, 4, 20, 250, 5, covenSize, 200.0f, 0,
                supplementalSacrifices, hasActiveCurseMasteryFamiliar(sp))) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        consumeHellOnEarthOfferings(offerings.get());
        success(caster, level, center, "witchery.curse.hell_on_earth.caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
        WitchHunterEntity.blackMagicPerformed(sp);
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
        return performOfferedCurse(caster, level, center,
                offerings.get().taglockKit(),
                10000,
                "witchery.ritual.curse_waking_nightmare.no_power",
                target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (data.isInSpiritWorld()) {
                data.setSpiritWorldIsDream(false); // flip to nightmare
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
                },
                "witchery.curse.waking_nightmare",
                "witchery.curse.waking_nightmare",
                true,
                offerings.get().exhaleOfHornedOne(),
                offerings.get().mellifluousHunger(),
                offerings.get().tormentedTwine(),
                offerings.get().emerald(),
                offerings.get().brewGrotesque());
    }

    private static void spawnRingParticles(ServerLevel level, BlockPos center, int radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = center.getX() + 0.5 + radius * Math.cos(angle);
            double z = center.getZ() + 0.5 + radius * Math.sin(angle);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    x, center.getY() + 1.1, z, 1, 0, 0.1, 0, 0.05);
        }
    }

    /**
     * Called on server stop. Cancels all active rituals for this dimension and
     * drops their sacrificed ingredients back into the world so they survive
     * the final world save. Without this, long-running staged rituals would
     * silently lose consumed resources across server restarts.
     */
    public static void cancelAllWithRefund(ServerLevel level) {
        RitualSavedData data = RitualSavedData.get(level);
        for (ActiveRitual ritual : data.activeRituals) {
            refundRitualIngredient(level, ritual);
        }
        data.activeRituals.clear();
        data.climatePlans.clear();
        data.setDirty();
    }
}
