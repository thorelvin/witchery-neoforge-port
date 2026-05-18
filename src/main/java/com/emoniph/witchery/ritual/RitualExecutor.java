package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
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
    private static final int RITUAL_PULSE_INTERVAL = 30;
    private static final Map<ResourceKey<Level>, List<ActiveRitual>> ACTIVE_RITUALS = new HashMap<>();

    private enum ActiveRitualType {
        WEATHER_CALL_STORM,
        RAIN_OF_TOADS
    }

    private static final class ActiveRitual {
        private final ActiveRitualType type;
        private final BlockPos center;
        private final int minRadius;
        private final int maxRadius;
        private final int targetStage;
        private int stage;
        private long nextGameTime;

        private ActiveRitual(ActiveRitualType type, BlockPos center, int minRadius, int maxRadius, int targetStage, long nextGameTime) {
            this.type = type;
            this.center = center.immutable();
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.targetStage = targetStage;
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
        return switch (result.size()) {
            case SMALL -> dispatchSmall(player, level, floorCenter, ingredient);
            case MEDIUM -> dispatchMedium(player, level, floorCenter, ingredient);
            case LARGE -> dispatchLarge(player, level, floorCenter, ingredient);
        };
    }

    // ── Small ring dispatch ────────────────────────────────────────────────────

    private static boolean dispatchSmall(Player player, Level level, BlockPos center, ItemStack item) {
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
        } else if (item.is(Items.FEATHER)) {
            riteOfBindFamiliar(player, level, center, "owl");
            return true;
        } else if (item.is(Items.COD) || item.is(Items.SALMON)) {
            riteOfBindFamiliar(player, level, center, "cat");
            return true;
        } else if (item.is(Items.SLIME_BALL)) {
            riteOfBindFamiliar(player, level, center, "toad");
            return true;
        } else if (item.is(Items.COAL) || item.is(Items.COAL_BLOCK) || item.is(Items.BLACK_DYE)) {
            riteOfTotalEclipse(player, level, center);
            return true;
        } else if (item.is(Items.WHEAT) || item.is(Items.CARROT) || item.is(Items.APPLE) || item.is(Items.BONE)) {
            riteOfBeastialCall(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.WAYSTONE.get()) && !WaystoneItem.isBound(item)) {
            return riteOfBindWaystone(player, level, center);
        } else if (item.is(WitcheryItems.WAYSTONE.get()) && WaystoneItem.isBound(item)) {
            return riteOfTranspositionToWaystone(player, level, center);
        } else if (item.is(Items.ENDER_PEARL)) {
            return riteOfTransposition(player, level, center);
        } else if (item.is(Items.ROTTEN_FLESH)) {
            return riteOfPriorIncarnation(player, level, center);
        } else if (item.is(Items.INK_SAC)) {
            riteOfTheCloak(player, level, center);
            return true;
        } else if (item.is(Items.DIAMOND)) {
            riteOfCleansing(player, level, center);
            return true;
        } else if (item.is(Items.GLASS_BOTTLE)) {
            return riteOfCallFamiliar(player, level, center);
        } else if (item.is(Items.SNOWBALL) || item.is(Items.WATER_BUCKET)) {
            return riteOfWeatherCallStorm(player, level, center);
        } else if (item.is(Items.SUGAR)) {
            riteOfCookItem(player, level, center);
            return true;
        } else {
            // No ingredient or unrecognised — basic blessing effect
            riteOfBlessing(player, level, center);
            return false;
        }
    }

    // ── Medium ring dispatch ───────────────────────────────────────────────────

    private static boolean dispatchMedium(Player player, Level level, BlockPos center, ItemStack item) {
        if (item.is(Items.BONE_MEAL) || item.is(Items.BONE)) {
            riteOfFertility(player, level, center);
            return true;
        } else if (item.is(Items.REDSTONE) || item.is(Items.REDSTONE_BLOCK)) {
            riteOfCharging(player, level, center);
            return true;
        } else if (item.is(Items.GUNPOWDER)) {
            riteOfSkysWrath(player, level, center);
            return true;
        } else if (item.is(Items.GOLD_INGOT) || item.is(Items.GOLD_BLOCK)) {
            riteOfSanctity(player, level, center);
            return true;
        } else if (item.is(Items.OBSIDIAN) || item.is(Items.CRYING_OBSIDIAN)) {
            riteOfImprisonment(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get())) {
            riteOfInfernalPower(player, level, center);
            return true;
        } else if (item.is(Items.SPIDER_EYE) || item.is(Items.FERMENTED_SPIDER_EYE)) {
            riteOfTheSilentStep(player, level, center);
            return true;
        } else if (item.is(Items.BLAZE_POWDER)) {
            return riteOfCurseBlindness(player, level, center);
        } else if (item.is(Items.LILY_PAD) || item.is(Items.FROGSPAWN)) {
            return riteOfCurseRainingToads(player, level, center);
        } else if (item.is(Items.COBBLESTONE) || item.is(Items.STONE)) {
            riteOfRaiseColumn(player, level, center);
            return true;
        } else if (item.is(Items.MAGMA_CREAM)) {
            return riteOfCurseCreature(player, level, center);
        } else if (item.is(Items.WITHER_ROSE)) {
            return riteOfCurseBlight(player, level, center);
        } else if (item.is(Items.CHORUS_FRUIT)) {
            return riteOfCurseSinking(player, level, center);
        } else if (item.is(Items.PUFFERFISH)) {
            return riteOfCurseOverheating(player, level, center);
        } else if (item.is(Items.FERMENTED_SPIDER_EYE)) {
            return riteOfCurseInsanity(player, level, center);
        } else if (item.is(Items.GOLD_NUGGET)) {
            return riteOfCurseMisfortune(player, level, center);
        } else if (item.is(WitcheryItems.DEMON_HEART.get())) {
            return riteOfCreaturesOfNightTaglock(player, level, center);
        } else {
            riteOfSummoning(player, level, center);
            return !item.isEmpty();
        }
    }

    // ── Large ring dispatch ────────────────────────────────────────────────────

    private static boolean dispatchLarge(Player player, Level level, BlockPos center, ItemStack item) {
        if (item.is(ItemTags.SAPLINGS)) {
            riteOfTheForest(player, level, center);
            return true;
        } else if (isRawFood(item, level)) {
            riteOfBroiling(player, level, center);
            return true;
        } else if (item.is(Items.DIRT) || item.is(Items.GRAVEL) || item.is(Items.SAND)) {
            riteOfMovingEarth(player, level, center);
            return true;
        } else if (item.is(Items.FLINT) || item.is(Items.FLINT_AND_STEEL)) {
            riteOfBrokenEarth(player, level, center);
            return true;
        } else if (item.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isFilled(item)) {
            return riteOfTranspositionPull(player, level, center);
        } else if (item.is(Items.NETHER_STAR)) {
            return riteOfCurseHellOnEarth(player, level, center);
        } else if (item.is(Items.STRING)) {
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
        } else if (item.is(Items.WITHER_ROSE) || item.is(Items.NETHER_WART)) {
            riteOfBlight(player, level, center);
            return true;
        } else if (item.is(Items.TNT)) {
            riteOfCrater(player, level, center);
            return true;
        } else if (isFetishItem(item)) {
            return riteOfBindSpiritsToFetish(player, level, center, item);
        } else {
            // Default large-circle rite: protection barrier
            riteOfProtection(player, level, center);
            return !item.isEmpty();
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

    /** Rite of Summoning — Strength II + Night Vision, cosmetic lightning. */
    private static void riteOfSummoning(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,  1200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,  1200, 0));

        if (level instanceof ServerLevel sl) {
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, sl);
            bolt.setPos(center.getX() + 0.5, (double)(center.getY() + 1), center.getZ() + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        success(player, level, center, "witchery.ritual.summoning.success", SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 3, 72);
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
    private static void riteOfProtection(Player player, Level level, BlockPos center) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,  6000, 0)); // 5min
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0)); // 5min
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,        6000, 1)); // 5min, Absorption II

        success(player, level, center, "witchery.ritual.protection.success",
                SoundEvents.BEACON_ACTIVATE, 0.9f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Protection cast at {} by {}", center, player.getName().getString());
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

    /** Rite of Sanctity — repels all hostile mobs 25 blocks away from the center. */
    private static void riteOfSanctity(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        AABB area = new AABB(center).inflate(20);
        List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area);
        int pushed = 0;
        for (Monster mob : mobs) {
            double dx = mob.getX() - center.getX();
            double dz = mob.getZ() - center.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.5) { dx = 1; dz = 0; dist = 1; }
            double scale = 25.0 / dist;
            mob.teleportTo(center.getX() + dx * scale, mob.getY(), center.getZ() + dz * scale);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0));
            pushed++;
        }
        // Ward caster: Resistance II + Fire Resistance for 3 minutes
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 3600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,   3600, 0));
        player.displayClientMessage(Component.translatable("witchery.ritual.sanctity.success", pushed), false);
        success(player, level, center, null, SoundEvents.BEACON_ACTIVATE, 1.2f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Sanctity repelled {} mobs", pushed);
    }

    /** Rite of Imprisonment — traps all hostile mobs inside the circle with heavy debuffs. */
    private static void riteOfImprisonment(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        AABB area = new AABB(center).inflate(10);
        List<Monster> mobs = sl.getEntitiesOfClass(Monster.class, area);
        int trapped = 0;
        for (Monster mob : mobs) {
            // Teleport stray mobs back to center, then heavily debuff
            mob.teleportTo(center.getX() + 0.5 + (sl.random.nextDouble() - 0.5) * 2,
                           center.getY() + 1,
                           center.getZ() + 0.5 + (sl.random.nextDouble() - 0.5) * 2);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 4)); // Slowness V
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,           1200, 3));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,            1200, 0));
            trapped++;
        }
        player.displayClientMessage(Component.translatable("witchery.ritual.imprisonment.success", trapped), false);
        success(player, level, center, null, SoundEvents.CHAIN_PLACE, 1f, 2, 48);
        WitcheryMod.LOGGER.info("Rite of Imprisonment trapped {} mobs", trapped);
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

    /** Rite of Moving Earth — raises a 5×5 surface column by 5 blocks. */
    private static void riteOfMovingEarth(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return;
        final int LIFT = 5, RADIUS = 2;
        int lifted = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                // Collect contiguous solid blocks downward from center.y-1
                List<BlockState> column = new ArrayList<>();
                for (int dy = -1; dy >= -12; dy--) {
                    BlockState s = sl.getBlockState(center.offset(dx, dy, dz));
                    if (s.isAir() || s.liquid()) break;
                    column.add(s);
                }
                if (column.isEmpty()) continue;
                // Place shifted upward (bottom→top to avoid overwriting)
                for (int i = column.size() - 1; i >= 0; i--) {
                    sl.setBlock(center.offset(dx, -1 - i + LIFT, dz), column.get(i), 3);
                }
                // Clear original positions
                for (int i = 0; i < column.size(); i++) {
                    sl.setBlock(center.offset(dx, -1 - i, dz), Blocks.AIR.defaultBlockState(), 3);
                }
                lifted += column.size();
            }
        }
        success(player, level, center, "witchery.ritual.moving_earth.success",
                SoundEvents.ROOTED_DIRT_BREAK, 0.8f, 3, 72);
        WitcheryMod.LOGGER.info("Rite of Moving Earth raised {} blocks at {}", lifted, center);
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

    private static boolean requireCoven(Player player, Level level, BlockPos center, int requiredSize) {
        int covenSize = level.getEntitiesOfClass(
                com.emoniph.witchery.entity.CovenWitchEntity.class,
                new AABB(center).inflate(16.0)
        ).size();
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
        long gameTime = level.getGameTime();
        if (gameTime < ritual.nextGameTime) {
            return true;
        }

        ritual.nextGameTime = gameTime + RITUAL_PULSE_INTERVAL;
        return switch (ritual.type) {
            case WEATHER_CALL_STORM -> tickStormRitual(level, ritual);
            case RAIN_OF_TOADS -> tickRainOfToadsRitual(level, ritual);
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

    private static void queueActiveRitual(ServerLevel level, ActiveRitualType type, BlockPos center, int minRadius, int maxRadius, int targetStage) {
        ACTIVE_RITUALS.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                .add(new ActiveRitual(type, center, minRadius, maxRadius, targetStage, level.getGameTime() + RITUAL_PULSE_INTERVAL));
    }

    private static void spawnRitualBolt(ServerLevel level, int minRadius, int maxRadius, BlockPos center) {
        BlockPos strikePos = randomRitualPos(level, center, minRadius, maxRadius);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, strikePos);
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5);
        level.addFreshEntity(bolt);
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
        queueActiveRitual(sl, ActiveRitualType.RAIN_OF_TOADS, center, 5, 16, 200);
        success(player, level, center, "witchery.ritual.rain_of_toads.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 4, 40);
        return true;
    }

    /** Rite of Calling the Storm — triggers a thunderstorm and strikes 5–8 lightning bolts. */
    private static boolean riteOfWeatherCallStorm(Player player, Level level, BlockPos center) {
        if (!(level instanceof ServerLevel sl)) return false;
        queueActiveRitual(sl, ActiveRitualType.WEATHER_CALL_STORM, center, 0, 3, 8);
        success(player, level, center, "witchery.ritual.storm.success",
                SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 4, 48);
        return true;
    }

    /** Rite of Raising the Column — raises a 3-block-radius stone column 8 blocks high. */
    private static void riteOfRaiseColumn(Player player, Level level, BlockPos center) {
        int radius = 3;
        for (int y = center.getY() + 1; y <= center.getY() + 8; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius) continue;
                    BlockPos pos = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                    }
                }
            }
        }
        success(player, level, center, "witchery.ritual.raise_column.success",
                SoundEvents.STONE_BREAK, 0.8f, 3, 32);
    }

    /** Rite of Cursing Creatures — applies Wither, Slowness and Weakness to nearby mobs. */
    private static boolean riteOfCurseCreature(Player player, Level level, BlockPos center) {
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

    /** Rite of the Crater — excavates a sphere of radius 8 centered at the ritual. */
    private static void riteOfCrater(Player player, Level level, BlockPos center) {
        int r = 8;
        int removed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.BEDROCK)) continue;
            Block.dropResources(state, level, pos, null);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            removed++;
        }
        success(player, level, center, "witchery.ritual.crater.success",
                SoundEvents.GENERIC_EXPLODE.value(), 1f, 6, 64);
        WitcheryMod.LOGGER.info("Rite of the Crater removed {} blocks", removed);
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

    /** Find and consume a filled taglock from the caster's inventory. Returns the bound UUID. */
    private static Optional<UUID> consumeTaglock(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(WitcheryItems.TAGLOCK_KIT.get())) continue;
            if (!TaglockKitItem.isFilled(s)) continue;
            Optional<UUID> uuid = TaglockKitItem.getUUID(s);
            s.shrink(1);
            if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            return uuid;
        }
        return Optional.empty();
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
        Optional<UUID> targetUUID = consumeTaglock(sp);
        if (targetUUID.isEmpty()) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(targetUUID.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.offline"), false);
            return false;
        }
        // Witch Hunter armor provides 90% resist
        if (target.getRandom().nextFloat() < 0.9f
                && isWearingWitchHunterArmor(target)) {
            target.displayClientMessage(Component.translatable("witchery.ritual.transposition_pull.resisted"), false);
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
     * Finds and consumes a filled taglock, resolves the online target, checks Voodoo Protection Poppet,
     * then runs the curse action on the target.
     */
    @FunctionalInterface
    private interface CurseAction { void apply(ServerPlayer target); }

    private static boolean applyCurse(Player caster, Level level, BlockPos center,
                                      CurseAction action, String successKey, String failKey) {
        if (!(caster instanceof ServerPlayer sp)) return false;
        Optional<UUID> uuid = consumeTaglock(sp);
        if (uuid.isEmpty()) {
            sp.displayClientMessage(Component.translatable(failKey + ".no_taglock"), false);
            return false;
        }
        ServerPlayer target = ((net.minecraft.server.level.ServerLevel) sp.level()).getServer().getPlayerList().getPlayer(uuid.get());
        if (target == null) {
            sp.displayClientMessage(Component.translatable(failKey + ".offline"), false);
            return false;
        }
        // Voodoo Protection Poppet negates the curse
        if (hasVoodooProtection(target)) {
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

    private static boolean hasVoodooProtection(ServerPlayer player) {
        var uuid = player.getUUID();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var s = player.getInventory().getItem(i);
            if (s.is(WitcheryItems.VOODOO_PROTECTION_POPPET.get())
                    && com.emoniph.witchery.item.PoppetItem.getBoundUUID(s)
                        .map(uuid::equals).orElse(false)) return true;
        }
        return false;
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
        }, "witchery.curse.blindness", "witchery.curse.blindness");
    }

    /** Curse of Blight — withers the target. */
    private static boolean riteOfCurseBlight(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 2400, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 0));
        }, "witchery.curse.blight", "witchery.curse.blight");
    }

    /** Curse of Misfortune — gives the target bad luck. */
    private static boolean riteOfCurseMisfortune(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target ->
            target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 6000, 1)),
            "witchery.curse.misfortune", "witchery.curse.misfortune");
    }

    /** Curse of Overheating — sets target on fire and applies fire vulnerability. */
    private static boolean riteOfCurseOverheating(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.igniteForTicks(200);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0));
        }, "witchery.curse.overheating", "witchery.curse.overheating");
    }

    /** Curse of Sinking — extreme slowness and mining fatigue. */
    private static boolean riteOfCurseSinking(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2400, 3));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 2400, 2));
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0)); // brief sink
        }, "witchery.curse.sinking", "witchery.curse.sinking");
    }

    /** Curse of Insanity — nausea + confusion. */
    private static boolean riteOfCurseInsanity(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 3600, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }, "witchery.curse.insanity", "witchery.curse.insanity");
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
        }, "witchery.curse.raining_toads", "witchery.curse.raining_toads");
    }

    /** Curse of Hell on Earth — summons lightning + hostile mobs around target (large circle). */
    private static boolean riteOfCurseHellOnEarth(Player caster, Level level, BlockPos center) {
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
        }, "witchery.curse.hell_on_earth", "witchery.curse.hell_on_earth");
    }

    /** Curse of Waking Nightmare — forces target into nightmare state if in Spirit World. */
    private static boolean riteOfCurseWakingNightmare(Player caster, Level level, BlockPos center) {
        return applyCurse(caster, level, center, target -> {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            if (data.isInSpiritWorld()) {
                data.setSpiritWorldIsDream(false); // flip to nightmare
                PacketDistributor.sendToPlayer(target, SyncPlayerDataPayload.from(target, data));
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
        }, "witchery.curse.waking_nightmare", "witchery.curse.waking_nightmare");
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
        }, "witchery.ritual.curse_of_wolf", "witchery.ritual.curse_of_wolf");
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
        }, "witchery.ritual.creatures_night", "witchery.ritual.creatures_night");
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
