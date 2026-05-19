package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.events.PoppetEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.CircleTalismanItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
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

public class RitualExecutor {
    private static final int VARIANT_OTHERWHERE = 2;
    private static final int VARIANT_INFERNAL = 3;
    private static final int VARIANT_RITUAL = 4;

    private static final int RITUAL_PULSE_INTERVAL = 30;
    private static final Map<ResourceKey<Level>, List<ActiveRitual>> ACTIVE_RITUALS = new HashMap<>();

    private enum ActiveRitualType {
        WEATHER_CALL_STORM,
        ICE_SHELL,
        RAIN_OF_TOADS,
        CALL_CREATURES,
        MOVING_EARTH,
        RAISE_COLUMN,
        CRATER,
        PROTECTION_BARRIER_MONSTERS,
        PROTECTION_BARRIER_PORTABLE,
        PROTECTION_BARRIER,
        SANCTITY_CIRCLE,
        IMPRISONMENT_CIRCLE
    }

    private static final class ActiveRitual {
        private final ActiveRitualType type;
        private final BlockPos center;
        private final RitualCircleDetector.CircleSize circleSize;
        private final int minRadius;
        private final int maxRadius;
        private final int targetStage;
        private final int pulseInterval;
        private final int covenSize;
        private final float upkeepCost;
        private final List<BlockPos> path = new ArrayList<>();
        private final int heading;
        private int stage;
        private long nextGameTime;
        private float upkeepDebt;

        private ActiveRitual(ActiveRitualType type, BlockPos center, RitualCircleDetector.CircleSize circleSize,
                             int minRadius, int maxRadius, int targetStage, int pulseInterval,
                             int covenSize, float upkeepCost, int heading, long nextGameTime) {
            this.type = type;
            this.center = center.immutable();
            this.circleSize = circleSize;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.targetStage = targetStage;
            this.pulseInterval = pulseInterval;
            this.covenSize = covenSize;
            this.upkeepCost = upkeepCost;
            this.heading = heading;
            this.nextGameTime = nextGameTime;
        }
    }

    /**
     * Dispatch the correct rite based on circle size + ingredient item dropped at the center.
     * Ingredient matching is intentionally lenient: several items may trigger the same rite.
     * Must be called server-side only.
     */
    public static boolean execute(Player player, Level level, BlockPos floorCenter,
                                  RitualCircleDetector.DetectionResult result, ItemStack ingredient) {
        if (isGlyphicTransformationChalk(ingredient)) {
            return riteOfGlyphicTransformation(player, level, floorCenter, ingredient);
        }
        int circleVariant = RitualCircleDetector.detectVariant(level, floorCenter, result.size());
        return switch (result.size()) {
            case SMALL -> dispatchSmall(player, level, floorCenter, ingredient, circleVariant);
            case MEDIUM -> dispatchMedium(player, level, floorCenter, ingredient, circleVariant);
            case LARGE -> dispatchLarge(player, level, floorCenter, ingredient, circleVariant);
        };
    }

    private static boolean hasVariant(int currentVariant, int requiredVariant) {
        return currentVariant == requiredVariant;
    }

    private static boolean isGlyphicTransformationChalk(ItemStack stack) {
        return stack.is(WitcheryItems.CHALK.get())
                || stack.is(WitcheryItems.CHALK_OTHERWHERE.get())
                || stack.is(WitcheryItems.CHALK_INFERNAL.get());
    }

    // ── Small ring dispatch ────────────────────────────────────────────────────

    private static boolean dispatchSmall(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        // Infusion brew bases take priority — each grants a specific infusion type
        if (item.is(WitcheryItems.GHOST_OF_THE_LIGHT.get())) {
            riteOfInfusion(player, level, center, InfusionType.LIGHT);
            return true;
        } else if (item.is(WitcheryItems.SOUL_OF_THE_WORLD.get())) {
            riteOfInfusion(player, level, center, InfusionType.OVERWORLD);
            return true;
        } else if (item.is(WitcheryItems.SPIRIT_OF_OTHERWHERE.get())) {
            riteOfInfusion(player, level, center, InfusionType.OTHERWHERE);
            return true;
        } else if (item.is(WitcheryItems.INFERNAL_ANIMUS.get())) {
            riteOfInfusion(player, level, center, InfusionType.INFERNAL);
            return true;
        } else if ((item.is(Items.COAL) || item.is(Items.COAL_BLOCK) || item.is(Items.BLACK_DYE))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfTotalEclipse(player, level, center);
            return true;
        } else if (item.is(Items.BLAZE_POWDER) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            riteOfBlindness(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.DIAMOND_VAPOUR.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfSummonWitch(player, level, center);
        } else if (item.is(WitcheryItems.WAYSTONE.get())
                && !WaystoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfBindWaystone(player, level, center);
        } else if (item.is(WitcheryItems.WAYSTONE.get())
                && WaystoneItem.isBound(item)
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            return riteOfTranspositionToWaystone(player, level, center);
        } else if (item.is(WitcheryItems.SPECTRAL_DUST.get()) && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            riteOfManifestationDream(player, level, center);
            return true;
        } else if (item.is(Items.ENDER_PEARL)) {
            return riteOfTransposition(player, level, center);
        } else if (item.is(Items.ROTTEN_FLESH) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfPriorIncarnation(player, level, center);
        } else if (item.is(Items.INK_SAC)) {
            riteOfTheCloak(player, level, center);
            return true;
        } else if (item.is(Items.DIAMOND)) {
            riteOfCleansing(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.FROZEN_HEART.get())) {
            return riteOfIceShell(player, level, center);
        } else if ((item.is(Items.SNOWBALL) || item.is(Items.WATER_BUCKET))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfWeatherCallStorm(player, level, center);
        } else if (item.is(Items.SUGAR) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            riteOfCookItem(player, level, center);
            return true;
        } else if (item.is(Items.COBBLESTONE) || item.is(Items.STONE)) {
            return riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.SMALL);
        } else {
            if (item.isEmpty()) {
                riteOfBlessing(player, level, center);
                return true;
            }
            return false;
        }
    }

    // ── Medium ring dispatch ───────────────────────────────────────────────────

    private static boolean dispatchMedium(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        if (item.is(Items.BONE_MEAL) || item.is(Items.BONE)) {
            riteOfFertility(player, level, center);
            return true;
        } else if (item.is(Items.FEATHER) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfBindFamiliar(player, level, center, "owl");
            return true;
        } else if ((item.is(Items.COD) || item.is(Items.SALMON)) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfBindFamiliar(player, level, center, "cat");
            return true;
        } else if (item.is(Items.SLIME_BALL) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfBindFamiliar(player, level, center, "toad");
            return true;
        } else if (item.is(Items.GLASS_BOTTLE) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfCallFamiliar(player, level, center);
        } else if (item.is(ItemTags.SAPLINGS) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfTheForest(player, level, center);
            return true;
        } else if (item.is(Items.REDSTONE) || item.is(Items.REDSTONE_BLOCK)) {
            riteOfCharging(player, level, center);
            return true;
        } else if (item.is(Items.GUNPOWDER)) {
            riteOfSkysWrath(player, level, center);
            return true;
        } else if ((item.is(Blocks.COBWEB.asItem()) || item.is(WitcheryItems.DENSE_WEB.get()))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfProtectionBarrier(player, level, center, ActiveRitualType.PROTECTION_BARRIER_MONSTERS, 4, 5);
        } else if (item.is(WitcheryItems.ATTUNED_STONE.get()) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfProtectionBarrier(player, level, center, ActiveRitualType.PROTECTION_BARRIER_PORTABLE, 6, 4);
        } else if ((item.is(Items.GOLD_INGOT) || item.is(Items.GOLD_BLOCK))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfSanctity(player, level, center);
            return true;
        } else if ((item.is(Items.OBSIDIAN) || item.is(Items.CRYING_OBSIDIAN))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            riteOfImprisonment(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get())) {
            riteOfInfernalPower(player, level, center);
            return true;
        } else if (item.is(Items.SPIDER_EYE)) {
            riteOfTheSilentStep(player, level, center);
            return true;
        } else if ((item.is(Items.LILY_PAD) || item.is(Items.FROGSPAWN))
                && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfRainOfToads(player, level, center);
        } else if (item.is(Items.COBBLESTONE) || item.is(Items.STONE)) {
            return riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.MEDIUM);
        } else if (item.is(Items.MAGMA_CREAM) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfCurseCreature(player, level, center);
        } else if (item.is(Items.WITHER_SKELETON_SKULL) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfSummonWither(player, level, center);
        } else if (item.is(Items.WITHER_ROSE) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            riteOfBlight(player, level, center);
            return true;
        } else if (item.is(Items.CHORUS_FRUIT) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfCurseSinking(player, level, center);
        } else if (item.is(Items.PUFFERFISH) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfCurseOverheating(player, level, center);
        } else if (item.is(Items.FERMENTED_SPIDER_EYE) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfCurseInsanity(player, level, center);
        } else if (item.is(Items.GOLD_NUGGET)) {
            return riteOfCurseMisfortune(player, level, center);
        } else if (item.is(WitcheryItems.DEMON_HEART.get())) {
            return riteOfCreaturesOfNightTaglock(player, level, center);
        } else if (item.is(Items.MILK_BUCKET)) {
            return riteOfSummoning(player, level, center);
        } else {
            return false;
        }
    }

    // ── Large ring dispatch ────────────────────────────────────────────────────

    private static boolean dispatchLarge(Player player, Level level, BlockPos center, ItemStack item, int circleVariant) {
        if (isRawFood(item, level)) {
            riteOfBroiling(player, level, center);
            return true;
        } else if ((item.is(Items.WHEAT) || item.is(Items.CARROT) || item.is(Items.APPLE) || item.is(Items.BONE))
                && hasVariant(circleVariant, VARIANT_OTHERWHERE)) {
            riteOfBeastialCall(player, level, center);
            return true;
        } else if (item.is(Items.DIRT) || item.is(Items.GRAVEL) || item.is(Items.SAND)) {
            return riteOfMovingEarth(player, level, center);
        } else if (item.is(Items.FLINT) || item.is(Items.FLINT_AND_STEEL)) {
            riteOfBrokenEarth(player, level, center);
            return true;
        } else if (item.is(Items.COBBLESTONE) || item.is(Items.STONE)) {
            return riteOfRaiseColumn(player, level, center, RitualCircleDetector.CircleSize.LARGE);
        } else if (item.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isFilled(item)) {
            return riteOfTranspositionPull(player, level, center);
        } else if (item.is(Items.NETHER_STAR)) {
            return riteOfCurseHellOnEarth(player, level, center);
        } else if (item.is(Items.STRING) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfCurseWakingNightmare(player, level, center);
        } else if (item.is(Items.BONE) || item.is(Items.BONE_BLOCK)) {
            return riteOfCurseOfTheWolfTaglock(player, level, center);
        } else if (item.is(Items.DRAGON_BREATH)) {
            riteOfDragonsFury(player, level, center);
            return true;
        } else if (item.is(Items.WET_SPONGE) || item.is(Items.SPONGE)) {
            return riteOfRain(player, level, center);
        } else if (item.is(WitcheryItems.FLYING_OINTMENT.get())) {
            return riteOfEnchantBroom(player, level, center);
        } else if (item.is(WitcheryItems.REFINED_EVIL.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfSummonImp(player, level, center);
        } else if (item.is(WitcheryItems.TORMENTED_TWINE.get()) && hasVariant(circleVariant, VARIANT_INFERNAL)) {
            return riteOfSummonDemon(player, level, center);
        } else if (item.is(WitcheryItems.INFERNAL_ANIMUS.get())) {
            return riteOfHellgate(player, level, center);
        } else if (item.is(Items.SKELETON_SKULL) || item.is(Items.WITHER_SKELETON_SKULL)) {
            riteOfBinding(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.TORMENT_SCROLL.get())) {
            return riteOfSummoningLordOfTorment(player, level, center);
        } else if (item.is(WitcheryItems.CIRCLE_TALISMAN.get()) && CircleTalismanItem.getCircleData(item) == 0) {
            riteOfBindCircleToTalisman(player, level, center);
            return true;
        } else if (item.is(Items.TNT)) {
            riteOfCrater(player, level, center);
            return true;
        } else if (isFetishItem(item) && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfBindSpiritsToFetish(player, level, center, item);
        } else if ((item.is(Blocks.OBSIDIAN.asItem()) || item.is(Items.GLOWSTONE_DUST))
                && hasVariant(circleVariant, VARIANT_RITUAL)) {
            return riteOfProtection(player, level, center);
        } else {
            return false;
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

        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setInfusionType(type);

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

    /** Rite of Bind Familiar — spawns and binds a familiar of the given type to the player. */
    private static void riteOfBindFamiliar(Player player, Level level, BlockPos center, String type) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(level instanceof ServerLevel sl)) return;

        FamiliarEntity familiar = switch (type) {
            case "owl"  -> new OwlFamiliarEntity(WitcheryEntities.OWL.get(), level);
            case "cat"  -> new CatFamiliarEntity(WitcheryEntities.FAMILIAR_CAT.get(), level);
            case "toad" -> new ToadFamiliarEntity(WitcheryEntities.TOAD.get(), level);
            default -> null;
        };
        if (familiar == null) return;

        familiar.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        familiar.tame(sp);
        sl.addFreshEntity(familiar);

        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setFamiliarUUID(familiar.getUUID());
        PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, data));

        success(player, level, center, "witchery.ritual.bind_familiar.success",
                SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1, 30);
        WitcheryMod.LOGGER.info("Bound {} familiar to {}", type, sp.getName().getString());
    }

    /** Rite of Blessing — fallback small-circle rite when no ingredient is present. */
    private static void riteOfBlessing(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,   1200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
        success(player, level, center, "witchery.ritual.infusion.success",
                SoundEvents.BEACON_ACTIVATE, 1.2f, 1, 30);
    }

    /** Rite of Total Eclipse — forces midnight on the server. */
    private static void riteOfTotalEclipse(Player player, Level level, BlockPos center) {
        if (!requireDay(player, level, center)) {
            return;
        }
        if (level instanceof ServerLevel sl) {
            sl.setDayTime(18000L); // midnight
        }
        success(player, level, center, "witchery.ritual.eclipse.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1, 24);
        WitcheryMod.LOGGER.info("Rite of Total Eclipse cast at {} by {}", center, player.getName().getString());
    }

    /** Rite of Beastial Call — teleports all animals within 30 blocks into the circle. */
    private static void riteOfBeastialCall(Player player, Level level, BlockPos center) {
        AABB area = new AABB(center).inflate(30);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area);
        int count = 0;
        for (Animal a : animals) {
            a.teleportTo(center.getX() + 0.5 + (level.random.nextDouble() - 0.5),
                         center.getY() + 1.0,
                         center.getZ() + 0.5 + (level.random.nextDouble() - 0.5));
            count++;
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.beastial_call.success", count), false);
        success(player, level, center, null, SoundEvents.AMBIENT_CAVE.value(), 1f, 1, 24);
        WitcheryMod.LOGGER.info("Rite of Beastial Call attracted {} animals", count);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Summoning — a long-running coven rite that allures nearby creatures in pulses. */
    private static boolean riteOfSummoning(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 3)) return false;
        if (!queueActiveRitual(sl, ActiveRitualType.CALL_CREATURES, RitualCircleDetector.CircleSize.MEDIUM,
                center, 0, 0, 250, 60)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.summoning.success",
                SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 3, 72);
        return true;
    }

    /** Rite of Fertility — bone-meals all crops in 10-block radius; cures zombie villagers. */
    private static void riteOfFertility(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
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

        player.displayClientMessage(Component.translatable("witchery.ritual.fertility.success", grown, cured), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Fertility: grew {}, cured {} zombie villagers", grown, cured);
    }

    /** Rite of Charging — Haste II + Absorption III + Resistance for 3 minutes. */
    private static void riteOfCharging(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.HASTE,         3600, 1)); // Haste II
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,         3600, 2)); // Absorption III
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,  1200, 0)); // Resistance I

        success(player, level, center, "witchery.ritual.charging.success",
                SoundEvents.BEACON_ACTIVATE, 1.5f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Charging activated for {}", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LARGE rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of the Forest — instantly grows all saplings within 15 blocks. */
    private static void riteOfTheForest(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        int grown = 0;
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-15, -2, -15), center.offset(15, 10, 15))) {
            BlockState s = sl.getBlockState(bp);
            if (s.getBlock() instanceof SaplingBlock sapling) {
                // Apply bone meal up to 10 times to force growth
                for (int attempt = 0; attempt < 10; attempt++) {
                    if (!sl.getBlockState(bp).is(s.getBlock())) break; // already grew
                    if (sapling.isBonemealSuccess(sl, sl.random, bp, sl.getBlockState(bp))) {
                        sapling.performBonemeal(sl, sl.random, bp, sl.getBlockState(bp));
                        grown++;
                        break;
                    }
                }
            }
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.forest.success", grown), false);
        success(player, level, center, null, SoundEvents.GRASS_PLACE, 1f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of the Forest grew {} trees", grown);
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

    /** Rite of Protection — Fire Resistance + Resistance I + Absorption II for 5 minutes. */
    private static boolean riteOfProtection(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!hasNearbyRitualAltar(sl, center, 16)) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.PROTECTION_BARRIER, RitualCircleDetector.CircleSize.LARGE,
                center, 6, 6, 0, 20, 0, 1.4f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        success(player, level, center, "witchery.ritual.protection.activated",
                SoundEvents.BEACON_ACTIVATE, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Protection barrier raised at {} by {}", center, player.getName().getString());
        return true;
    }

    private static boolean riteOfProtectionBarrier(Player player, Level level, BlockPos center,
                                                   ActiveRitualType type, int radius, int height) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (type != ActiveRitualType.PROTECTION_BARRIER_PORTABLE && !hasNearbyRitualAltar(sl, center, 16)) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.no_power"), false);
            return false;
        }

        int targetStage = type == ActiveRitualType.PROTECTION_BARRIER_PORTABLE ? 60 : 0;
        float upkeepCost = switch (type) {
            case PROTECTION_BARRIER_MONSTERS -> 1.2f;
            case PROTECTION_BARRIER_PORTABLE -> 0.0f;
            default -> 0.0f;
        };
        if (!queueActiveRitual(sl, type, RitualCircleDetector.CircleSize.MEDIUM, center, radius, height, targetStage, 20, 0, upkeepCost, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        success(player, level, center, "witchery.ritual.protection.activated",
                SoundEvents.BEACON_ACTIVATE, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Protection variant {} raised at {} by {}", type, center, player.getName().getString());
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // M21 — SMALL rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Transposition — teleports the caster to their bound respawn point (bed/anchor). */
    private static boolean riteOfTransposition(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        var respawnCfg = sp.getRespawnConfig();
        if (respawnCfg == null || respawnCfg.respawnData() == net.minecraft.world.level.storage.LevelData.RespawnData.DEFAULT) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }
        BlockPos respawnPos = respawnCfg.respawnData().pos();
        ResourceKey<Level> respawnDim = respawnCfg.respawnData().dimension();
        ServerLevel targetLevel = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getLevel(respawnDim);
        if (targetLevel == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }
        double tx = respawnPos.getX() + 0.5, ty = respawnPos.getY() + 0.5, tz = respawnPos.getZ() + 0.5;
        if (level == targetLevel) {
            sp.teleportTo(tx, ty, tz);
        } else {
            sp.teleportTo(targetLevel, tx, (double)(ty), tz, java.util.Set.<net.minecraft.world.entity.Relative>of(), sp.getYRot(), sp.getXRot(), false);
        }
        success(player, level, center, "witchery.ritual.transposition.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Transposition: {} teleported to respawn.", sp.getName().getString());
        return true;
    }

    /** Rite of Prior Incarnation — summons dropped items near the caster's last death spot. */
    private static boolean riteOfPriorIncarnation(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
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
        sp.displayClientMessage(Component.translatable("witchery.ritual.prior_incarnation.success", recalled), false);
        success(player, level, center, null, SoundEvents.ENDERMAN_TELEPORT, 1f, 1, 30);
        WitcheryMod.LOGGER.info("Rite of Prior Incarnation: recalled {} stacks for {}", recalled, sp.getName().getString());
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // M21 — MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Creatures of the Night — curses the caster with vampirism. Persists through death. */
    private static boolean riteOfCreaturesOfNight(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        if (data.isVampire()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.creatures_of_night.already"), false);
            return false;
        }
        data.setVampire(true);
        data.setBloodLevel(250); // start at half blood
        PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, data));
        success(player, level, center, "witchery.ritual.creatures_of_night.success",
                SoundEvents.WITHER_SPAWN, 0.6f, 2, 48);
        WitcheryMod.LOGGER.info("Creatures of the Night: {} is now a vampire.", sp.getName().getString());
        return true;
    }

    /** Rite of Sky's Wrath — strikes lightning at 12 points around the circle. */
    private static void riteOfSkysWrath(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        int strikes = 0;
        for (int i = 0; i < 12; i++) {
            double angle = 2 * Math.PI * i / 12;
            int bx = center.getX() + (int) Math.round(4 * Math.cos(angle));
            int bz = center.getZ() + (int) Math.round(4 * Math.sin(angle));
            int by = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos(bx, 0, bz)).getY();
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
            bolt.setPos(bx + 0.5, (double)by, bz + 0.5);
            sl.addFreshEntity(bolt);
            strikes++;
        }
        success(player, level, center, null, SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 5, 120);
        WitcheryMod.LOGGER.info("Rite of Sky's Wrath: {} strikes around {}", strikes, center);
    }

    /** Rite of Sanctity — sustains a repulsive protection circle around the rite. */
    private static boolean riteOfSanctity(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!hasNearbyRitualAltar(sl, center, 16)) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.SANCTITY_CIRCLE, RitualCircleDetector.CircleSize.MEDIUM, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.sanctity.activated",
                SoundEvents.BEACON_ACTIVATE, 1.2f, 2, 48);
        return true;
    }

    /** Rite of Imprisonment — sustains an attractive circle that keeps creatures penned in. */
    private static boolean riteOfImprisonment(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!hasNearbyRitualAltar(sl, center, 16)) {
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.no_power"), false);
            return false;
        }
        if (!queueActiveRitual(sl, ActiveRitualType.IMPRISONMENT_CIRCLE, RitualCircleDetector.CircleSize.MEDIUM, center, 4, 0, 0, 20, 0, 0.8f, 0)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.imprisonment.activated",
                SoundEvents.CHAIN_PLACE, 1f, 2, 48);
        return true;
    }

    /** Curse of the Wolf — curses the caster with lycanthropy. Transforms at full moon. */
    private static boolean riteOfCurseOfTheWolf(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        if (data.isWerewolf()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.curse_of_wolf.already"), false);
            return false;
        }
        data.setWerewolf(true);
        PacketDistributor.sendToPlayer(sp, SyncPlayerDataPayload.from(sp, data));
        success(player, level, center, "witchery.ritual.curse_of_wolf.success",
                SoundEvents.WOLF_SHAKE, 0.8f, 3, 72);
        WitcheryMod.LOGGER.info("Curse of the Wolf: {} is now a werewolf.", sp.getName().getString());
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

    /** Rite of the Cloak — Invisibility V + Night Vision for 5 minutes. */
    private static void riteOfTheCloak(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 6000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0));
        success(player, level, center, "witchery.ritual.cloak.success",
                SoundEvents.ENDERMAN_TELEPORT, 1.4f, 1, 24);
        WitcheryMod.LOGGER.info("Rite of the Cloak cast by {}", player.getName().getString());
    }

    /** Rite of Cleansing — removes all negative effects; grants Regeneration II for 10 s. */
    private static void riteOfCleansing(Player player, Level level, BlockPos center) {
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        success(player, level, center, "witchery.ritual.cleansing.success",
                SoundEvents.BEACON_ACTIVATE, 1.6f, 1, 24);
        WitcheryMod.LOGGER.info("Rite of Cleansing cast by {}", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — MEDIUM rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Infernal Power — Strength II + Fire Resistance + Speed II for 5 min. */
    private static void riteOfInfernalPower(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,    6000, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED,  6000, 1));
        success(player, level, center, "witchery.ritual.infernal_power.success",
                SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 0.7f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Infernal Power cast by {}", player.getName().getString());
    }

    /** Rite of the Silent Step — Speed III + Jump Boost II + Slow Falling for 3 min; removes hunger. */
    private static void riteOfTheSilentStep(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 3600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,           3600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,   3600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION,      200, 0));
        success(player, level, center, "witchery.ritual.silent_step.success",
                SoundEvents.CAT_AMBIENT, 1.2f, 2, 40);
        WitcheryMod.LOGGER.info("Rite of the Silent Step cast by {}", player.getName().getString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — LARGE rites
    // ══════════════════════════════════════════════════════════════════════════

    /** Rite of Dragon's Fury — deals 8 magic damage to every hostile mob within 20 blocks. */
    private static void riteOfDragonsFury(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        List<net.minecraft.world.entity.monster.Monster> mobs =
                sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                        new AABB(center).inflate(20));
        int hit = 0;
        for (var mob : mobs) {
            mob.hurt(player.damageSources().magic(), 8.0f);
            hit++;
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.dragons_fury.success", hit), false);
        success(player, level, center, null, SoundEvents.ENDER_DRAGON_AMBIENT, 0.6f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Dragon's Fury struck {} mobs", hit);
    }

    /** Rite of Manifestation — sends the caster into the Dream World. */
    /** Rite of Infusion (Flying Ointment) — enchants the nearest broom within 5 blocks. */
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
        broom.setEnchanted(true);
        success(player, level, center, "witchery.ritual.enchant_broom.success",
                SoundEvents.BEACON_POWER_SELECT, 1f, 3, 48);
        WitcheryMod.LOGGER.info("Broom at {} enchanted by {}", broom.blockPosition(), player.getName().getString());
        return true;
    }

    // ── Deferred: Rite of Manifestation (spectral overworld form) ──────────────
    // Kept for future implementation. Currently unused — see PORTING_BACKLOG M8.
    private static void riteOfManifestationDream(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return;
        ServerLevel dream = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getLevel(WitcheryDimensions.DREAM_WORLD);
        if (dream == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.manifestation.unavailable"), false);
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(sp);
        data.setReturnPos(sp.blockPosition(), sp.level().dimension());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp,
                com.emoniph.witchery.network.SyncPlayerDataPayload.from(sp, data));
        success(player, level, center, "witchery.ritual.manifestation.success",
                SoundEvents.ENDERMAN_TELEPORT, 1.8f, 3, 60);
        // Force chunk 0,0 to generate, then find solid ground
        dream.getChunk(0, 0, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        int dreamY = 65;
        for (int y = 75; y >= 45; y--) {
            var bp = new net.minecraft.core.BlockPos(0, y, 0);
            if (!dream.getBlockState(bp).isAir()
                    && dream.getBlockState(bp.above()).isAir()
                    && dream.getBlockState(bp.above().above()).isAir()) {
                dreamY = y + 1; break;
            }
        }
        sp.teleportTo(dream, 0.5, (double)(dreamY), 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), sp.getYRot(), sp.getXRot(), false);
        WitcheryMod.LOGGER.info("Rite of Manifestation: {} entered Dream World.", sp.getName().getString());
    }

    /** Rite of Hellgate — tears open a rift to the Torment dimension. */
    private static boolean riteOfHellgate(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getLevel(WitcheryDimensions.TORMENT) == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.hellgate.unavailable"), false);
            return false;
        }
        // Strike lightning ring for drama
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8;
                int bx = center.getX() + (int) Math.round(3 * Math.cos(angle));
                int bz = center.getZ() + (int) Math.round(3 * Math.sin(angle));
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
                bolt.setPos(bx + 0.5, (double)(center.getY() + 1), bz + 0.5);
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
        }
        success(player, level, center, null, SoundEvents.PORTAL_TRIGGER, 1f, 3, 60);
        com.emoniph.witchery.world.TormentHelper.sendToTorment(sp, level.random.nextInt(
                com.emoniph.witchery.world.TormentHelper.NUM_LEVELS));
        WitcheryMod.LOGGER.info("Rite of Hellgate: {} entered Torment.", sp.getName().getString());
        return true;
    }

    /** Rite of Rain — starts rain for 10 minutes. */
    private static boolean riteOfRain(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!canPerformClimateRite(player, level, center)) return false;
        sl.setWeatherParameters(0, 12000, true, false); // 10 min rain, no thunder
        success(player, level, center, "witchery.ritual.rain.success",
                SoundEvents.WEATHER_RAIN, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Rain started by {}", player.getName().getString());
        return true;
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

        rituals.removeIf(ritual -> !tickActiveRitual(level, ritual));
        if (rituals.isEmpty()) {
            ACTIVE_RITUALS.remove(level.dimension());
        }
    }

    private static boolean tickActiveRitual(ServerLevel level, ActiveRitual ritual) {
        if (!RitualCircleDetector.hasRing(level, ritual.center, ritual.circleSize)) {
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
            case CALL_CREATURES -> tickCallCreaturesRitual(level, ritual);
            case MOVING_EARTH -> tickMovingEarthRitual(level, ritual);
            case RAISE_COLUMN -> tickRaiseColumnRitual(level, ritual);
            case CRATER -> tickCraterRitual(level, ritual);
            case PROTECTION_BARRIER_MONSTERS -> tickProtectionBarrierMonstersRitual(level, ritual);
            case PROTECTION_BARRIER_PORTABLE -> tickProtectionBarrierPortableRitual(level, ritual);
            case PROTECTION_BARRIER -> tickProtectionBarrierRitual(level, ritual);
            case SANCTITY_CIRCLE -> tickSanctityCircleRitual(level, ritual);
            case IMPRISONMENT_CIRCLE -> tickImprisonmentCircleRitual(level, ritual);
        };
    }

    private static boolean tickStormRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        switch (ritual.stage) {
            case 1, 2 -> spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
            case 3 -> {
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
            }
            case 4 -> {
                if (!level.isThundering()) {
                    int stormTicks = (300 + level.random.nextInt(600)) * 20;
                    level.setWeatherParameters(0, stormTicks, true, true);
                }
                spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
            }
            default -> {
                int extraBolts = level.random.nextInt(4);
                for (int i = 0; i < extraBolts; i++) {
                    spawnRitualBolt(level, ritual.minRadius, ritual.maxRadius, ritual.center);
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

    private static boolean tickCallCreaturesRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        allureCreatures(level, ritual.center, ritual.stage % 8);
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
        if (ritual.stage == 1) {
            level.playSound(null, ritual.center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.8f);
            spawnRingParticles(level, ritual.center, Math.max(ritual.minRadius, 3), 32);
        }

        int baseRadius = ritual.minRadius;
        int height = ritual.maxRadius;
        int effectiveRadius = baseRadius + ritual.covenSize * 2;
        int airSpace = baseRadius * 2;

        for (int y = ritual.center.getY() + airSpace; y >= ritual.center.getY() - height; y--) {
            raiseColumnLayer(level, ritual.center, y, effectiveRadius, y == ritual.center.getY() - 1);
        }

        liftRaiseColumnEntities(level, ritual.center, effectiveRadius, airSpace);
        return ritual.stage < ritual.targetStage;
    }

    private static boolean tickCraterRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        if (ritual.stage == 1) {
            level.playSound(null, ritual.center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.8f);
            spawnRingParticles(level, ritual.center, Math.max(ritual.minRadius, 4), 24);
        }

        int radius = ritual.minRadius;
        int height = ritual.maxRadius;
        if (ritual.stage > height) {
            return false;
        }

        for (int depth = 1; depth <= ritual.stage; depth++) {
            float layerRadius = radius - (height - ritual.stage - 1 + depth) * radius / (float) height;
            carveCraterLayer(level, ritual.center, ritual.center.getY() - depth, Math.max((int) Math.ceil(layerRadius), 1), radius);
        }
        return true;
    }

    private static boolean tickProtectionBarrierRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            return false;
        }

        ritual.stage++;
        drawProtectionBarrier(level, ritual.center, ritual.minRadius, ritual.maxRadius, true);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.center, ritual.minRadius, 16);
        }
        return ritual.targetStage <= 0 || ritual.stage < ritual.targetStage;
    }

    private static boolean tickProtectionBarrierMonstersRitual(ServerLevel level, ActiveRitual ritual) {
        if (!consumeNearbyRitualAltarPower(level, ritual, 16)) {
            return false;
        }

        ritual.stage++;
        drawProtectionBarrier(level, ritual.center, ritual.minRadius, ritual.maxRadius, false);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.center, ritual.minRadius, 12);
        }
        return true;
    }

    private static boolean tickProtectionBarrierPortableRitual(ServerLevel level, ActiveRitual ritual) {
        ritual.stage++;
        drawProtectionBarrier(level, ritual.center, ritual.minRadius, ritual.maxRadius, true);
        if (ritual.stage == 1 || ritual.stage % 4 == 0) {
            spawnRingParticles(level, ritual.center, ritual.minRadius, 12);
        }
        return ritual.stage < ritual.targetStage;
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
        List<ActiveRitual> rituals = ACTIVE_RITUALS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        boolean occupied = rituals.stream().anyMatch(active -> active.center.equals(center));
        if (occupied) {
            return false;
        }

        rituals.add(new ActiveRitual(type, center, circleSize, minRadius, maxRadius, targetStage, pulseInterval,
                covenSize, upkeepCost, heading,
                level.getGameTime() + pulseInterval));
        return true;
    }

    private static void spawnRitualBolt(ServerLevel level, int minRadius, int maxRadius, BlockPos center) {
        BlockPos strikePos = randomRitualPos(level, center, minRadius, maxRadius);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, strikePos);
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        level.addFreshEntity(bolt);
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
     * Rite of Binding — summons Death at a Large circle with a Skeleton/Wither Skull.
     * Death is neutral until looked at; attacks then teleports to the looker.
     */
    private static void riteOfBinding(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(level instanceof ServerLevel sl)) return;

        // Lightning at midnight atmosphere
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
            bolt.setPos(center.getX() + 3 * Math.cos(angle) + 0.5, (double)(center.getY() + 1), center.getZ() + 3 * Math.sin(angle) + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }

        var death = WitcheryEntities.DEATH.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (death != null) death.setPos(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        death.finalizeSpawn(sl, sl.getCurrentDifficultyAt(center),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(death);

        success(player, level, center, "witchery.ritual.binding.success",
                SoundEvents.WITHER_SPAWN, 0.4f, 4, 80);
        WitcheryMod.LOGGER.info("Rite of Binding: Death summoned by {}", sp.getName().getString());
    }

    /**
     * Rite of Summoning the Lord of Torment.
     * Ingredient: Torment Scroll, Large circle.
     * Cannot be cast inside the Torment dimension itself.
     * Spawns LordOfTormentEntity at the circle center with a lightning-ring fanfare.
     */
    private static boolean riteOfSummoningLordOfTorment(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!(level instanceof ServerLevel sl))   return false;

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
     * Rite of Bind Circle to Talisman — reads the surrounding chalk circle and encodes it into
     * a blank Circle Talisman dropped at the center. Ingredient: blank CircleTalisman.
     * Trigger: large circle, blank talisman ingredient.
     */
    private static void riteOfBindCircleToTalisman(Player player, Level level, BlockPos center) {
        if (level.isClientSide()) return;

        // Sample representative positions for each ring (from center row of the 17x17 pattern)
        // Small ring: x-offset -3; Medium: -5; Large: -7
        int small  = detectVariant(level, center.offset(-3, 0, 0));
        int medium = detectVariant(level, center.offset(-5, 0, 0));
        int large  = detectVariant(level, center.offset(-7, 0, 0));

        int data = (large << 6) | (medium << 3) | small;
        ItemStack talisman = new ItemStack(WitcheryItems.CIRCLE_TALISMAN.get());
        CircleTalismanItem.setCircleData(talisman, data);

        ItemEntity entity = new ItemEntity(level,
                center.getX() + 0.5, center.getY() + 0.1, center.getZ() + 0.5, talisman);
        level.addFreshEntity(entity);

        success(player, level, center, "witchery.ritual.bind_circle.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 4, 48);
        WitcheryMod.LOGGER.info("Rite of Bind Circle: encoded data {} for {}",
                data, player.getName().getString());
    }

    /** Returns 1=GLYPH, 2=OTHERWHERE, 3=INFERNAL, 0=unknown for the block at pos. */
    private static int detectVariant(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(WitcheryBlocks.CIRCLE_GLYPH.get()))     return 1;
        if (state.is(WitcheryBlocks.CIRCLE_OTHERWHERE.get())) return 2;
        if (state.is(WitcheryBlocks.CIRCLE_INFERNAL.get()))  return 3;
        return 1; // default to ritual glyph if not detected
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
    private static void riteOfBlindness(Player player, Level level, BlockPos center) {
        AABB aabb = new AABB(center).inflate(10);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (entity instanceof Player) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
            count++;
        }
        success(player, level, center, "witchery.ritual.blindness.success",
                SoundEvents.BAT_AMBIENT, 0.5f, 3, 32);
        WitcheryMod.LOGGER.info("Rite of Blindness afflicted {} entities", count);
    }

    /** Rite of Blight — withers players, zombifies villagers, slays animals nearby. */
    private static void riteOfBlight(Player player, Level level, BlockPos center) {
        AABB aabb = new AABB(center).inflate(15);
        for (Player p : level.getEntitiesOfClass(Player.class, aabb)) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 2400, 1));
        }
        for (Villager villager : level.getEntitiesOfClass(Villager.class, aabb)) {
            if (level.random.nextFloat() < 0.1f) {
                ZombieVillager zv = EntityType.ZOMBIE_VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (zv != null) { zv.setPos(villager.getX(), villager.getY(), villager.getZ()); level.addFreshEntity(zv); }
                villager.discard();
            }
        }
        for (net.minecraft.world.entity.animal.cow.Cow cow : level.getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class, new net.minecraft.world.phys.AABB(center).inflate(8))) {
            double r2 = level.random.nextFloat();
            var moo = WitcheryEntities.FAMILIAR_CAT.get().create((net.minecraft.server.level.ServerLevel) level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (r2 < 0.15f) {
                if (moo != null) { moo.setPos(cow.getX(), cow.getY(), cow.getZ()); level.addFreshEntity(moo); }
                cow.discard();
            } else if (r2 < 0.38f) {
                cow.discard();
            }
        }
        // Animal loop removed temporarily
    }

    /** Rite of Rain of Toads — sets thunderstorm, strikes lightning, spawns toads. */
    private static boolean riteOfRainOfToads(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 1)) return false;
        if (!queueActiveRitual(sl, ActiveRitualType.RAIN_OF_TOADS, RitualCircleDetector.CircleSize.MEDIUM, center, 5, 16, 200)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.rain_of_toads.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 4, 40);
        return true;
    }

    /** Rite of Calling the Storm — triggers a thunderstorm and strikes 5–8 lightning bolts. */
    private static boolean riteOfWeatherCallStorm(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!queueActiveRitual(sl, ActiveRitualType.WEATHER_CALL_STORM, RitualCircleDetector.CircleSize.SMALL, center, 0, 3, 8)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
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

        int baseRadius = switch (circleSize) {
            case SMALL -> 4;
            case MEDIUM -> 6;
            case LARGE -> 9;
        };
        int height = 8;

        if (!queueActiveRitual(sl, ActiveRitualType.RAISE_COLUMN, circleSize, center,
                baseRadius, height, height - 1, 5, countNearbyCovenWitches(level, center))) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }

        success(player, level, center, "witchery.ritual.raise_column.success",
                SoundEvents.STONE_BREAK, 0.8f, 3, 32);
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
        AABB aabb = new AABB(center).inflate(10);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (entity instanceof Player) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            count++;
        }
        success(player, level, center, "witchery.ritual.curse_creature.success",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 32);
        WitcheryMod.LOGGER.info("Rite of Curse Creature afflicted {} entities", count);
        return true;
    }

    /** Rite of Calling the Familiar — teleports the player's familiar to the center. */
    private static boolean riteOfCallFamiliar(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        Optional<UUID> uuid = WitcheryAttachments.get(player).getFamiliarUUID();
        if (uuid.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.ritual.call_familiar.none"), false);
            return false;
        }
        net.minecraft.world.entity.Entity familiar = sl.getEntity(uuid.get());
        if (familiar == null) {
            player.displayClientMessage(Component.translatable("witchery.ritual.call_familiar.none"), false);
            return false;
        }
        familiar.teleportTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        success(player, level, center, "witchery.ritual.call_familiar.success",
                SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 2, 20);
        return true;
    }

    /** Rite of Summon Witch — summons a hostile witch if the circle is not obstructed. */
    private static boolean riteOfSummonWitch(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
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

        success(player, level, center, "witchery.ritual.summon_witch.success",
                SoundEvents.WITCH_CELEBRATE, 0.8f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Summon Witch cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Imp — summons a hostile imp, but only with a coven present. */
    private static boolean riteOfSummonImp(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!requireCoven(player, level, center, 1)) return false;
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

        success(player, level, center, "witchery.ritual.summon_imp.success",
                SoundEvents.BLAZE_AMBIENT, 0.7f, 3, 48);
        WitcheryMod.LOGGER.info("Rite of Summon Imp cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Wither — summons a wither if the infernal circle is unobstructed. */
    private static boolean riteOfSummonWither(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
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

        success(player, level, center, "witchery.ritual.summon_wither.success",
                SoundEvents.WITHER_SPAWN, 0.8f, 4, 64);
        WitcheryMod.LOGGER.info("Rite of Summon Wither cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Summon Demon — summons a large infernal demon if the circle is clear. */
    private static boolean riteOfSummonDemon(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
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

        success(player, level, center, "witchery.ritual.summon_demon.success",
                SoundEvents.BLAZE_SHOOT, 0.7f, 4, 56);
        WitcheryMod.LOGGER.info("Rite of Summon Demon cast by {}", player.getName().getString());
        return true;
    }

    /** Rite of Cooking — smelt all raw food items dropped within 5 blocks. */
    private static void riteOfCookItem(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        AABB aabb = new AABB(center).inflate(5);
        int cooked = 0;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, aabb)) {
            ItemStack stack = ie.getItem();
            Optional<RecipeHolder<SmeltingRecipe>> recipe =
                    ((net.minecraft.server.level.ServerLevel) level).getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack), level.registryAccess());
                if (!result.isEmpty()) {
                    ie.setItem(result);
                    cooked++;
                }
            }
        }
        if (cooked > 0) {
            player.displayClientMessage(Component.translatable("witchery.ritual.cook.success", cooked), false);
        }
        level.playSound(null, center, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
        if (level instanceof ServerLevel slevel) spawnRingParticles(slevel, center, 3, 24);
    }

    /** Rite of the Crater — excavates downward in pulses, closer to the old staged ritual. */
    private static boolean riteOfCrater(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        if (!queueActiveRitual(sl, ActiveRitualType.CRATER, RitualCircleDetector.CircleSize.LARGE, center, 8, 8, 8, 10)) {
            player.displayClientMessage(Component.translatable("witchery.ritual.already_active"), false);
            return false;
        }
        success(player, level, center, "witchery.ritual.crater.success",
                SoundEvents.GENERIC_EXPLODE.value(), 1f, 6, 64);
        return true;
    }

    private static void carveCraterLayer(ServerLevel level, BlockPos center, int y, int radius, int maxRadius) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                carveCraterPixel(level, center, new BlockPos(center.getX() + dx, y, center.getZ() + dz), maxRadius);
            }
        }
    }

    private static void carveCraterPixel(ServerLevel level, BlockPos center, BlockPos pos, int maxRadius) {
        if ((pos.getX() == center.getX() && pos.getZ() == center.getZ())
                || !shouldCarveCraterPixel(center, pos, maxRadius)) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!canCarveCrater(level, pos, state)) {
            return;
        }

        if (shouldDropCraterBlock(state)) {
            Block.dropResources(state, level, pos, null);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean shouldCarveCraterPixel(BlockPos center, BlockPos pos, int maxRadius) {
        int depth = center.getY() - pos.getY();
        if (depth > 3) {
            return true;
        }

        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double minRadius = Math.max(maxRadius - 3 - depth, 0);
        return dx * dx + dz * dz > minRadius * minRadius;
    }

    private static boolean canCarveCrater(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.hasBlockEntity() || state.liquid()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private static boolean shouldDropCraterBlock(BlockState state) {
        return !state.is(Blocks.STONE)
                && !state.is(Blocks.DIRT)
                && !state.is(Blocks.GRASS_BLOCK)
                && !state.is(Blocks.SAND)
                && !state.is(Blocks.GRAVEL)
                && !state.is(Blocks.CLAY);
    }

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

    /** Find a bound waystone in the caster's inventory without consuming it. */
    private static Optional<ItemStack> findBoundWaystone(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(WitcheryItems.WAYSTONE.get()) && WaystoneItem.isBound(s)) return Optional.of(s);
        }
        return Optional.empty();
    }

    // ── Sprint 4 — Waystone rites ─────────────────────────────────────────────

    /** Rite of Binding — binds an unbound Waystone in inventory to the caster's current position. */
    private static boolean riteOfBindWaystone(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(WitcheryItems.WAYSTONE.get()) || WaystoneItem.isBound(s)) continue;
            WaystoneItem.bindTo(s, sp.blockPosition(), sp.level().dimension());
            success(player, level, center, "witchery.ritual.bind_waystone.success",
                    SoundEvents.BEACON_ACTIVATE, 1f, 2, 32);
            WitcheryMod.LOGGER.info("Waystone bound to {} for {}", sp.blockPosition(), sp.getName().getString());
            return true;
        }
        sp.displayClientMessage(Component.translatable("witchery.ritual.bind_waystone.none"), false);
        return false;
    }

    /** Rite of Transposition (waystone) — teleports caster to bound waystone coordinates. */
    private static boolean riteOfTranspositionToWaystone(Player player, Level level, BlockPos center) {
        if (!(player instanceof ServerPlayer sp)) return false;
        Optional<ItemStack> ws = findBoundWaystone(sp);
        if (ws.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition.no_waystone"), false);
            return false;
        }
        Optional<BlockPos> dest = WaystoneItem.getBoundPos(ws.get());
        Optional<ResourceKey<Level>> dim = WaystoneItem.getBoundDim(ws.get());
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
        if (sp.level() == targetLevel) sp.teleportTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);
        else sp.teleportTo(targetLevel, d.getX() + 0.5, (double)(d.getY()), d.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), sp.getYRot(), sp.getXRot(), false);
        success(player, level, center, "witchery.ritual.transposition.success",
                SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1, 30);
        return true;
    }

    /** Rite of Transposition (pull) — pulls the taglock target to the caster. Costs 3000 altar power. */
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
        consumeInventoryItem(sp, taglock.get().slot());
        if (hasCurseProtection(target)) {
            sp.displayClientMessage(Component.translatable("witchery.rite.blackmagicdampening"), false);
            return false;
        }
        if (triggerVoodooProtection(target)) {
            target.displayClientMessage(Component.translatable("witchery.curse.blocked_by_poppet"), false);
            sp.displayClientMessage(Component.translatable("witchery.curse.target_protected"), false);
            return false;
        }
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
        consumeInventoryItem(sp, taglock.get().slot());
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
        action.apply(target);
        target.displayClientMessage(Component.translatable(successKey + ".target"), false);
        success(caster, level, center, successKey + ".caster",
                SoundEvents.WITCH_CELEBRATE, 1f, 3, 48);
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

    /** Curse of Misfortune — gives the target bad luck. */
    private static boolean riteOfCurseMisfortune(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target ->
            target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 6000, 1)),
            "witchery.curse.misfortune", "witchery.curse.misfortune", false);
    }

    /** Curse of Overheating — sets target on fire and applies fire vulnerability. */
    private static boolean riteOfCurseOverheating(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        return applyCurse(caster, level, center, target -> {
            target.igniteForTicks(200);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
        }, "witchery.curse.overheating", "witchery.curse.overheating", false);
    }

    /** Curse of Sinking — extreme slowness and mining fatigue. */
    private static boolean riteOfCurseSinking(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2400, 3));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 2400, 2));
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0)); // brief sink
        }, "witchery.curse.sinking", "witchery.curse.sinking", false);
    }

    /** Curse of Insanity — nausea + confusion. */
    private static boolean riteOfCurseInsanity(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 3600, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }, "witchery.curse.insanity", "witchery.curse.insanity", false);
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
        return applyCurse(caster, level, center, target -> {
            if (!(target.level() instanceof ServerLevel sl)) return;
            // Lightning strikes
            for (int i = 0; i < 5; i++) {
                double dx = (sl.getRandom().nextDouble() - 0.5) * 10;
                double dz = (sl.getRandom().nextDouble() - 0.5) * 10;
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
                bolt.setPos(target.getX() + dx, target.getY(), target.getZ() + dz);
                sl.addFreshEntity(bolt);
            }
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 1));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 2400, 1));
        }, "witchery.curse.hell_on_earth", "witchery.curse.hell_on_earth", false);
    }

    /** Curse of Waking Nightmare — forces target into nightmare state if in Spirit World. */
    private static boolean riteOfCurseWakingNightmare(Player caster, Level level, BlockPos center) {
        if (!requireThunderstorm(caster, level, center)) return false;
        return applyCurse(caster, level, center, target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (data.isInSpiritWorld()) {
                data.setSpiritWorldIsDream(false); // flip to nightmare
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
        }, "witchery.curse.waking_nightmare", "witchery.curse.waking_nightmare", true);
    }

    /** Updated Curse of the Wolf — targets a specific player via taglock. */
    private static boolean riteOfCurseOfTheWolfTaglock(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (!data.isWerewolf()) {
                data.setWerewolf(true);
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
                target.displayClientMessage(Component.translatable("witchery.ritual.curse_of_wolf.cursed"), false);
            }
        }, "witchery.ritual.curse_of_wolf", "witchery.ritual.curse_of_wolf", false);
    }

    /** Updated Creatures of the Night — vampirism curse via taglock. */
    private static boolean riteOfCreaturesOfNightTaglock(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (!data.isVampire()) {
                data.setVampire(true);
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
                target.displayClientMessage(Component.translatable("witchery.ritual.creatures_night.cursed"), false);
            }
        }, "witchery.ritual.creatures_night", "witchery.ritual.creatures_night", false);
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
