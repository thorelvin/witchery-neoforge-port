package com.emoniph.witchery.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.DistilleryBlock;
import com.emoniph.witchery.blocks.FumeFunnelBlock;
import com.emoniph.witchery.blocks.KettleBlock;
import com.emoniph.witchery.blocks.PlacedItemBlock;
import com.emoniph.witchery.blocks.SpinningWheelBlock;
import com.emoniph.witchery.blocks.TormentPortalBlock;
import com.emoniph.witchery.blocks.WickerBundleBlock;
import com.emoniph.witchery.blocks.WitchesOvenBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.blocks.entity.RefillingChestBlockEntity;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import com.emoniph.witchery.world.TormentHelper;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
import com.emoniph.witchery.predictions.Predictions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Debug/admin commands for Witchery. All require permission level 2.
 *
 * /witchery werewolf [targets] [true|false]
 * /witchery vampire  [targets] [true|false]
 * /witchery blood    <0-500>
 * /witchery infusion <type>
 * /witchery wolfmastery <0-3>       — set wolf mastery level
 * /witchery soaring [ticks]         — grant soaring brew buff
 * /witchery familiar <type>         — set familiar type (owl/cat/toad/spectral/none)
 * /witchery fullmoon                — advance time to next full-moon midnight
 * /witchery clear                   — wipe all witchery state from self
 * /witchery status                  — show own witchery data
 * /witchery spawn <entity>          — spawn entity 2 blocks ahead
 * /witchery dream                   — enter the Spirit World dimension
 * /witchery mirror                  — enter the Mirror World dimension
 * /witchery reflection              — summon a Reflection linked to you as owner
 * /witchery torment                 — enter the Torment dimension
 * /witchery exit                    — return to previous dimension
 * /witchery give primer             — give the Witches' Primer guidebook
 * /witchery altar                   — show altar power at current location
 * /witchery setup rite              - place a QA pad with ritual circle rings
 * /witchery goto <type>             — teleport to nearest witchery structure (hobgoblin_hut / coven_circle / straw_man)
 *                                   or a Witchery village building such as apothecary
 */
@EventBusSubscriber
public class WitcheryCommands {

    private static final List<String> INFUSION_TYPES =
            List.of("NONE", "LIGHT", "OVERWORLD", "OTHERWHERE", "INFERNAL");
    private static final List<String> FAMILIAR_TYPES =
            List.of("none", "owl", "cat", "toad", "spectral");
    private static final int SETUP_HOUSE_SIZE = 11;
    private static final int SETUP_FOUNDATION_DEPTH = 3;
    private static final int SETUP_CLEAR_HEIGHT = 7;
    private static final int SETUP_FORWARD_OFFSET = 5;
    private static final int SETUP_CLEAR_PAD_SIZE = 20;
    private static final int SETUP_SHELL_DROP = 1;
    private static final int SETUP_RITE_PAD_WIDTH = 71;
    private static final int SETUP_RITE_PAD_DEPTH = 17;
    private static final int SETUP_RITE_FORWARD_OFFSET = 5;
    private static final int SETUP_RITE_FOUNDATION_DEPTH = 3;
    private static final int SETUP_RITE_CLEAR_HEIGHT = 5;
    private static final int SETUP_RITE_FLOOR_Y = -1;

    private record SetupHouseLayout(BlockPos origin, Direction depth, Direction width) {
        BlockPos pos(int x, int z) {
            return origin.relative(width, x).relative(depth, z);
        }

        BlockPos pos(int x, int y, int z) {
            return pos(x, z).above(y);
        }

        Direction left() {
            return width.getOpposite();
        }

        Direction frontFacing() {
            return depth.getOpposite();
        }
    }

    private record SetupRiteLayout(BlockPos origin, Direction depth, Direction width) {
        BlockPos pos(int x, int z) {
            return origin.relative(width, x).relative(depth, z);
        }

        BlockPos pos(int x, int y, int z) {
            return pos(x, z).above(y);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("witchery")
                .requires(src -> src.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS)))

                // ── Player state ──────────────────────────────────────────────
                .then(Commands.literal("werewolf")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> setWerewolf(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"), true))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setWerewolf(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                BoolArgumentType.getBool(ctx, "value")))))
                        .executes(ctx -> setWerewolfSelf(ctx.getSource(), true)))

                .then(Commands.literal("vampire")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> setVampire(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"), true))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setVampire(ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                BoolArgumentType.getBool(ctx, "value")))))
                        .executes(ctx -> setVampireSelf(ctx.getSource(), true)))

                .then(Commands.literal("blood")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 500))
                                .executes(ctx -> setBlood(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))

                .then(Commands.literal("infusion")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(INFUSION_TYPES, b))
                                .executes(ctx -> setInfusion(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                .then(Commands.literal("wolfmastery")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 3))
                                .executes(ctx -> setWolfMastery(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))

                .then(Commands.literal("soaring")
                        .executes(ctx -> setSoaring(ctx.getSource(), 144000))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 144000))
                                .executes(ctx -> setSoaring(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "ticks")))))

                .then(Commands.literal("familiar")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(FAMILIAR_TYPES, b))
                                .executes(ctx -> setFamiliar(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")))))

                // ── World / time ──────────────────────────────────────────────
                .then(Commands.literal("fullmoon")
                        .executes(ctx -> forceFullMoon(ctx.getSource())))

                // ── Give / info ──────────────────────────────────────────────
                .then(Commands.literal("altar")
                        .executes(ctx -> showAltarPower(ctx.getSource())))

                // ── Utility ───────────────────────────────────────────────────
                .then(Commands.literal("clear")
                        .executes(ctx -> clearState(ctx.getSource())))

                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource())))

                // ── Dimensions ───────────────────────────────────────────────
                .then(Commands.literal("dream")
                        .executes(ctx -> enterDimension(ctx.getSource(), WitcheryDimensions.SPIRIT_WORLD)))

                .then(Commands.literal("mirror")
                        .executes(ctx -> enterDimension(ctx.getSource(), WitcheryDimensions.MIRROR_WORLD)))

                .then(Commands.literal("torment")
                        .executes(ctx -> enterDimension(ctx.getSource(), WitcheryDimensions.TORMENT)))

                .then(Commands.literal("exit")
                        .executes(ctx -> exitDimension(ctx.getSource())))

                // ── New debug commands ────────────────────────────────────────

                .then(Commands.literal("nightmare")
                        .executes(ctx -> toggleNightmare(ctx.getSource(), true))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> toggleNightmare(ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, "value")))))

                .then(Commands.literal("transformation")
                        .then(Commands.argument("level", FloatArgumentType.floatArg(0f, 1f))
                                .executes(ctx -> setTransformation(ctx.getSource(),
                                        FloatArgumentType.getFloat(ctx, "level")))))

                .then(Commands.literal("tormentlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                .executes(ctx -> tormentLevel(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))

                .then(Commands.literal("verify")
                        .then(Commands.literal("torment")
                                .executes(ctx -> verifyTorment(ctx.getSource())))
                        .then(Commands.literal("spiritworld")
                                .executes(ctx -> verifySpiritWorld(ctx.getSource()))))

                .then(Commands.literal("debug")
                        .then(Commands.literal("brew")
                                .executes(ctx -> showBrewDebug(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> showBrewDebug(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"))))))

                // Debug: force a specific crystal-ball prophecy to come true immediately.
                // /witchery crystal <prophecy> [target]   (use "all" to fire every prophecy)
                .then(Commands.literal("crystal")
                        .then(Commands.argument("prophecy", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    java.util.List<String> names = new java.util.ArrayList<>(Predictions.DEBUG_NAMES.keySet());
                                    names.add("all");
                                    return SharedSuggestionProvider.suggest(names, b);
                                })
                                .executes(ctx -> triggerProphecy(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "prophecy"),
                                        ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> triggerProphecy(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "prophecy"),
                                                EntityArgument.getPlayer(ctx, "target"))))))

                // Debug: grant Mystic Branch demon-soul spell knowledge.
                // /witchery learn <ignianima|carnosadiem|morsmordre|tormentum|all|clear> [target]
                .then(Commands.literal("learn")
                        .then(Commands.argument("knowledge", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        java.util.List.of("ignianima", "carnosadiem", "morsmordre", "tormentum", "all", "clear"), b))
                                .executes(ctx -> learnSymbol(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "knowledge"),
                                        ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> learnSymbol(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "knowledge"),
                                                EntityArgument.getPlayer(ctx, "target"))))))

                // Debug: set Mystic Branch infusion so spells can be cast in survival.
                // /witchery infuse <none|light|overworld|otherwhere|infernal> [energy] [target]
                .then(Commands.literal("infuse")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        java.util.List.of("none", "light", "overworld", "otherwhere", "infernal"), b))
                                .executes(ctx -> infuse(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"), 5000,
                                        ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("energy", IntegerArgumentType.integer(0))
                                        .executes(ctx -> infuse(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                IntegerArgumentType.getInteger(ctx, "energy"),
                                                ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> infuse(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        IntegerArgumentType.getInteger(ctx, "energy"),
                                                        EntityArgument.getPlayer(ctx, "target")))))))

                .then(Commands.literal("give")
                        .then(Commands.literal("primer")
                                .executes(ctx -> givePrimer(ctx.getSource())))
                        .then(Commands.literal("all")
                                .executes(ctx -> giveAll(ctx.getSource())))
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    java.util.List<String> names = new java.util.ArrayList<>();
                                    BuiltInRegistries.ITEM.entrySet().forEach(e -> {
                                        if (e.getKey().identifier().getNamespace().equals("witchery"))
                                            names.add(e.getKey().identifier().getPath());
                                    });
                                    return SharedSuggestionProvider.suggest(names, b);
                                })
                                .executes(ctx -> giveItem(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "item")))))

                .then(Commands.literal("kettle")
                        .executes(ctx -> kettleInfo(ctx.getSource()))
                        .then(Commands.literal("clear")
                                .executes(ctx -> kettleClear(ctx.getSource()))))

                .then(Commands.literal("setup")
                        .then(Commands.literal("stations")
                                .executes(ctx -> setupStations(ctx.getSource())))
                        .then(Commands.literal("rite")
                                .executes(ctx -> setupRite(ctx.getSource()))))
                .then(Commands.literal("ritual")
                        .then(Commands.literal("circle")
                                .executes(ctx -> inspectRitualCircle(ctx.getSource())))
                        .then(Commands.literal("hitchcock")
                                .executes(ctx -> startHitchcockRitual(ctx.getSource()))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> startHitchcockRitual(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))

                // ── Spawn ─────────────────────────────────────────────────────
                .then(Commands.literal("spawn")
                        .then(Commands.literal("wickerman")     .executes(ctx -> spawnWickerman(ctx.getSource())))
                        .then(Commands.literal("raven")         .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.RAVEN.get())))
                        .then(Commands.literal("ravens")
                                .executes(ctx -> spawnRavens(ctx.getSource(), com.emoniph.witchery.item.BrewOfHitchcockItem.DEFAULT_RAVEN_COUNT))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> spawnRavens(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("owl")           .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.OWL.get())))
                        .then(Commands.literal("familiar_cat")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.FAMILIAR_CAT.get())))
                        .then(Commands.literal("spectral_familiar").executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.SPECTRAL_FAMILIAR.get())))
                        .then(Commands.literal("toad")          .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.TOAD.get())))
                        .then(Commands.literal("parasytic_louse").executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.PARASYTIC_LOUSE.get())))
                        .then(Commands.literal("coven_witch")   .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.COVEN_WITCH.get())))
                        .then(Commands.literal("crazy_cat_lady").executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.CRAZY_CAT_LADY.get())))
                        .then(Commands.literal("witch_hunter")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.WITCH_HUNTER.get())))
                        .then(Commands.literal("banshee")       .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.BANSHEE.get())))
                        .then(Commands.literal("spectre")       .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.SPECTRE.get())))
                        .then(Commands.literal("poltergeist")   .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.POLTERGEIST.get())))
                        .then(Commands.literal("treefyd")       .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.TREEFYD.get())))
                        .then(Commands.literal("ent")           .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.ENT.get())))
                        .then(Commands.literal("mandrake_mob")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.MANDRAKE_MOB.get())))
                        .then(Commands.literal("demon")         .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.DEMON.get())))
                        .then(Commands.literal("broom")         .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.BROOM.get())))
                        .then(Commands.literal("nightmare")      .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.NIGHTMARE.get())))
                        .then(Commands.literal("spirit")         .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.SPIRIT.get())))
                        .then(Commands.literal("hobgoblin")      .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.HOBGOBLIN.get())))
                        .then(Commands.literal("flying_monkey")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.FLYING_MONKEY.get())))
                        .then(Commands.literal("binky")          .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.BINKY.get())))
                        .then(Commands.literal("lord_of_torment").executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.LORD_OF_TORMENT.get())))
                        .then(Commands.literal("flame_imp")      .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.FLAME_IMP.get())))
                        .then(Commands.literal("death")          .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.DEATH.get())))
                        .then(Commands.literal("reflection")     .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.REFLECTION.get())))
                        .then(Commands.literal("egassy")         .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.EGASSY.get())))
                        .then(Commands.literal("goblin_gulg")    .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.GOBLIN_GULG.get())))
                        .then(Commands.literal("goblin_mog")     .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.GOBLIN_MOG.get())))
                        .then(Commands.literal("vampire")        .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.VAMPIRE.get())))
                        .then(Commands.literal("baba_yaga")      .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.BABA_YAGA.get())))
                        .then(Commands.literal("wolfman")        .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.WOLFMAN.get())))
                        .then(Commands.literal("were_villager")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.WERE_VILLAGER.get())))
                        .then(Commands.literal("village_guard")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.VILLAGE_GUARD.get())))
                        .then(Commands.literal("leonard")        .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.LEONARD.get())))
                        .then(Commands.literal("lost_soul")      .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.LOST_SOUL.get()))))

                .then(Commands.literal("reflection")
                        .executes(ctx -> summonReflection(ctx.getSource())))
                .then(Commands.literal("goto")
                        .then(Commands.literal("hobgoblin_hut")   .executes(ctx -> gotoStructure(ctx.getSource(), "witchery", "hobgoblin_hut")))
                        .then(Commands.literal("coven_circle")    .executes(ctx -> gotoStructure(ctx.getSource(), "witchery", "coven_circle")))
                        .then(Commands.literal("stone_circle")    .executes(ctx -> gotoStructure(ctx.getSource(), "witchery", "coven_circle")))
                        .then(Commands.literal("straw_man")       .executes(ctx -> gotoStructure(ctx.getSource(), "witchery", "straw_man")))
                        .then(Commands.literal("apothecary")      .executes(ctx -> gotoVillageBuilding(ctx.getSource(), "apothecary")))
                        .then(Commands.literal("village_plains")  .executes(ctx -> gotoStructure(ctx.getSource(), "minecraft", "village_plains")))
                        .then(Commands.literal("village_meadow")  .executes(ctx -> gotoMeadowVillage(ctx.getSource())))
                        .then(Commands.literal("village_savanna") .executes(ctx -> gotoStructure(ctx.getSource(), "minecraft", "village_savanna")))
                        .then(Commands.literal("village_taiga")   .executes(ctx -> gotoStructure(ctx.getSource(), "minecraft", "village_taiga")))
                        .then(Commands.literal("village_snowy")   .executes(ctx -> gotoStructure(ctx.getSource(), "minecraft", "village_snowy")))
                        .then(Commands.literal("village_desert")  .executes(ctx -> gotoStructure(ctx.getSource(), "minecraft", "village_desert"))))
                .then(Commands.literal("walls")
                        .executes(ctx -> buildWalls(ctx.getSource()))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetWalls(ctx.getSource()))))

                // Debug: unlock/relock Witches' Primer guide sections (the guide/* advancements).
                // /witchery guide unlock [all|<book>]   /witchery guide reset
                .then(Commands.literal("guide")
                        .then(Commands.literal("unlock")
                                .executes(ctx -> guideUnlock(ctx.getSource(), "all"))
                                .then(Commands.argument("book", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(GUIDE_UNLOCK_SUGGEST, b))
                                        .executes(ctx -> guideUnlock(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "book")))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> guideReset(ctx.getSource()))))
                .then(Commands.literal("rite")
                        .then(Commands.literal("list")
                                .executes(ctx -> riteList(ctx.getSource())))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        RitualExecutor.allRitualDefinitionNames(), b))
                                .executes(ctx -> riteForce(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
        );
    }

    // ── Guide progression (debug) ───────────────────────────────────────────────

    /** Guide-section keys, matching the {@code witchery:guide/<key>} chain advancements. */
    private static final List<String> GUIDE_BOOKS = List.of(
            "primer", "herbology", "infusions", "oven", "distilling", "circle_magic",
            "burning", "wands", "poppets", "spirit_world", "creatures", "transformations");
    private static final List<String> GUIDE_UNLOCK_SUGGEST;
    static {
        java.util.List<String> s = new java.util.ArrayList<>();
        s.add("all");
        s.addAll(GUIDE_BOOKS);
        GUIDE_UNLOCK_SUGGEST = List.copyOf(s);
    }

    private static int guideUnlock(CommandSourceStack src, String book)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String key = book.toLowerCase(java.util.Locale.ROOT);
        List<String> targets = key.equals("all") ? GUIDE_BOOKS : List.of(key);
        if (!key.equals("all") && !GUIDE_BOOKS.contains(key)) {
            src.sendFailure(Component.literal("Unknown guide '" + book + "'. Options: all, "
                    + String.join(", ", GUIDE_BOOKS)));
            return 0;
        }
        int n = 0;
        for (String name : targets) if (setGuideAdvancement(src, p, name, true)) n++;
        final int count = n;
        src.sendSuccess(() -> Component.literal("[Guide] Unlocked " + count + " section(s) for "
                + p.getName().getString() + "."), false);
        return n;
    }

    private static int guideReset(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int n = 0;
        for (String name : GUIDE_BOOKS) if (setGuideAdvancement(src, p, name, false)) n++;
        final int count = n;
        src.sendSuccess(() -> Component.literal("[Guide] Re-locked " + count + " section(s) for "
                + p.getName().getString() + "."), false);
        return n;
    }

    /** Grants or revokes the {@code witchery:guide/<name>} advancement (criterion {@code has_book}). */
    private static boolean setGuideAdvancement(CommandSourceStack src, ServerPlayer p, String name, boolean grant) {
        net.minecraft.advancements.AdvancementHolder holder = src.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath("witchery", "guide/" + name));
        if (holder == null) return false;
        return grant ? p.getAdvancements().award(holder, "has_book")
                     : p.getAdvancements().revoke(holder, "has_book");
    }

    // ── Rite force-fire (debug) ──────────────────────────

    private static int riteList(CommandSourceStack src) {
        var names = RitualExecutor.allRitualDefinitionNames();
        src.sendSuccess(() -> Component.literal("[Rite] " + names.size() + " definitions: "
                + String.join(", ", names)), false);
        return names.size();
    }

    /** Force-runs a rite definition's effect by name at the player (bypasses dispatch gating). */
    private static int riteForce(CommandSourceStack src, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!RitualExecutor.allRitualDefinitionNames().contains(name)) {
            src.sendFailure(Component.literal("Unknown rite '" + name + "'. Try /witchery rite list."));
            return 0;
        }
        var center = p.blockPosition();
        boolean ok = RitualExecutor.forceRunRitualDefinition(name, p, p.level(), center, p.getMainHandItem());
        src.sendSuccess(() -> Component.literal("[Rite] Force-ran '" + name + "' at " + center.toShortString()
                + " -> " + (ok ? "SUCCESS"
                : "no effect (the rite's own power/offering checks may have failed)")), false);
        return ok ? 1 : 0;
    }

    // ── Player state ──────────────────────────────────────────────────────────

    private static int setWerewolfSelf(CommandSourceStack src, boolean v) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return applyWerewolf(src.getPlayerOrException(), v, src);
    }

    private static int setWerewolf(CommandSourceStack src, Collection<ServerPlayer> targets, boolean v) {
        int n = 0;
        for (ServerPlayer p : targets) n += applyWerewolf(p, v, src);
        return n;
    }

    private static int applyWerewolf(ServerPlayer p, boolean v, CommandSourceStack src) {
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setWerewolf(v);
        if (!v) d.setTransformationLevel(0f);
        sync(p, d);
        src.sendSuccess(() -> Component.literal(p.getName().getString() +
                (v ? " is now a werewolf." : " is no longer a werewolf.")), true);
        return 1;
    }

    private static int setVampireSelf(CommandSourceStack src, boolean v) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return applyVampire(src.getPlayerOrException(), v, src);
    }

    private static int setVampire(CommandSourceStack src, Collection<ServerPlayer> targets, boolean v) {
        int n = 0;
        for (ServerPlayer p : targets) n += applyVampire(p, v, src);
        return n;
    }

    private static int applyVampire(ServerPlayer p, boolean v, CommandSourceStack src) {
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setVampire(v);
        if (v && d.getBloodLevel() == 0) d.setBloodLevel(250);
        if (!v) d.setBloodLevel(0);
        sync(p, d);
        src.sendSuccess(() -> Component.literal(p.getName().getString() +
                (v ? " is now a vampire." : " is no longer a vampire.")), true);
        return 1;
    }

    private static int setBlood(CommandSourceStack src, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setBloodLevel(amount);
        sync(p, d);
        src.sendSuccess(() -> Component.literal("Blood level set to " + amount + "/500."), false);
        return 1;
    }

    private static int setInfusion(CommandSourceStack src, String typeName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        InfusionType type = InfusionType.fromString(typeName.toUpperCase());
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        if (type == InfusionType.NONE) {
            d.clearInfusion();
        } else {
            d.setInfusionType(type);
            d.setInfusionEnergy(200, 200);
            com.emoniph.witchery.events.InfusionEvents.onInfusionGranted(p, type);
        }
        sync(p, d);
        src.sendSuccess(() -> Component.literal(
                "Infusion set to " + type + (type == InfusionType.NONE ? "." : " (200/200 charges).")), false);
        return 1;
    }

    private static int setWolfMastery(CommandSourceStack src, int level)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setWolfMastery(level);
        sync(p, d);
        src.sendSuccess(() -> Component.literal("Wolf mastery set to " + level + "/3."), false);
        return 1;
    }

    private static int setSoaring(CommandSourceStack src, int ticks)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setSoaringTicks(ticks);
        sync(p, d);
        src.sendSuccess(() -> Component.literal("Soaring buff set to " + ticks + " ticks ("
                + (ticks / 20) + "s)."), false);
        return 1;
    }

    private static int setFamiliar(CommandSourceStack src, String type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        String lower = type.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("none")) {
            d.clearFamiliar();
            sync(p, d);
            src.sendSuccess(() -> Component.literal("Familiar cleared."), false);
        } else if (lower.equals("owl") || lower.equals("cat") || lower.equals("toad") || lower.equals("spectral")) {
            d.clearFamiliar();
            d.setFamiliarType(lower);
            sync(p, d);
            if (p.level() instanceof ServerLevel sl) {
                var familiar = FamiliarHelper.restoreBoundFamiliar(p, sl, p.getX(), p.getY() + 1, p.getZ());
                if (familiar != null) {
                    src.sendSuccess(() -> Component.literal("Familiar set to " + lower + " and spawned."), false);
                } else {
                    src.sendSuccess(() -> Component.literal("Familiar type set to " + lower + " (spawn failed — try /witchery familiar " + lower + " again or use the rite)."), false);
                }
            }
        } else {
            src.sendSuccess(() -> Component.literal(
                    "Unknown familiar type '" + type + "'. Valid types: owl, cat, toad, spectral, none."), false);
        }
        return 1;
    }

    private static int givePrimer(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        p.addItem(new net.minecraft.world.item.ItemStack(WitcheryItems.WITCHES_PRIMER.get()));
        src.sendSuccess(() -> Component.literal("Gave The Witches' Primer."), false);
        return 1;
    }

    private static int showAltarPower(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!(p.level() instanceof ServerLevel sl)) return 0;
        int radius = 20;
        var center = p.blockPosition();
        var bb = new net.minecraft.world.phys.AABB(center).inflate(radius);
        // Scan for altar BEs within radius
        var altars = new java.util.ArrayList<com.emoniph.witchery.blocks.entity.AltarBlockEntity>();
        for (var bp : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset( radius,  radius,  radius))) {
            if (sl.getBlockEntity(bp) instanceof com.emoniph.witchery.blocks.entity.AltarBlockEntity altar) {
                altars.add(altar);
            }
        }
        if (altars.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No altar within " + radius + " blocks."), false);
        } else {
            for (var altar : altars) {
                var bp = altar.getBlockPos();
                src.sendSuccess(() -> Component.literal(
                        "Altar at " + bp.getX() + "," + bp.getY() + "," + bp.getZ()
                        + " — power=" + altar.getPower() + "/" + altar.getMaxPower()), false);
            }
        }
        return altars.size();
    }

    // ── World / time ──────────────────────────────────────────────────────────

    private static int forceFullMoon(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!(p.level() instanceof ServerLevel sl)) return 0;
        // Moon phase = (dayNumber) % 8; phase 0 = full moon.
        // Advance to the next day whose number is a multiple of 8, at midnight (18000 ticks).
        long currentDay = sl.getDayTime() / 24000L;
        long nextFullMoonDay = ((currentDay / 8) + 1) * 8;
        sl.setDayTime(nextFullMoonDay * 24000L + 18000L);
        src.sendSuccess(() -> Component.literal("Time set to full moon midnight. Howl!"), false);
        return 1;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static int clearState(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setVampire(false);
        d.setWerewolf(false);
        d.setBloodLevel(0);
        d.setTransformationLevel(0f);
        d.setInfusionType(InfusionType.NONE);
        sync(p, d);
        // Strip witchery-applied effects
        p.removeEffect(MobEffects.WEAKNESS);
        p.removeEffect(MobEffects.HUNGER);
        p.removeEffect(MobEffects.NIGHT_VISION);
        p.removeEffect(MobEffects.SPEED);
        p.removeEffect(MobEffects.STRENGTH);
        p.removeEffect(MobEffects.JUMP_BOOST);
        p.removeEffect(MobEffects.FIRE_RESISTANCE);
        p.removeEffect(MobEffects.SLOW_FALLING);
        p.removeEffect(MobEffects.DARKNESS);
        p.removeEffect(MobEffects.SATURATION);
        p.removeEffect(MobEffects.HASTE);
        p.removeEffect(MobEffects.ABSORPTION);
        p.removeEffect(MobEffects.RESISTANCE);
        src.sendSuccess(() -> Component.literal("All witchery state cleared."), false);
        return 1;
    }

    private static int showStatus(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);

        // ── transformations ──────────────────────────────────────────────
        String vampire  = "  vampire=" + d.isVampire() + "  blood=" + d.getBloodLevel() + "/500";
        String werewolf = "  werewolf=" + d.isWerewolf()
                + "  transform=" + String.format("%.2f", d.getTransformationLevel())
                + "  wolfmastery=" + d.getWolfMastery() + "/3"
                + "  howlCooldown=" + tickStr(d.getHowlCooldown());

        // ── brew buffs ───────────────────────────────────────────────────
        String brews = "  soaring=" + tickStr(d.getSoaringTicks())
                + "  depths=" + tickStr(d.getDepthsTicks())
                + "  grotesque=" + tickStr(d.getGrotesqueTicks());
        String misc  = "  ghostWalker=" + d.isGhostWalkerActive()
                + "  tormentCooldown=" + tickStr(d.getTormentPortalCooldown())
                + "  demonicNightmare=" + d.isDemonicNightmare();

        // ── infusion / creature power ────────────────────────────────────
        String infusion = "  infusion=" + d.getInfusionType()
                + "  energy=" + d.getInfusionEnergy() + "/" + d.getInfusionMaxEnergy();
        String creature = "  creaturePower=" + (d.getCreaturePowerType().isEmpty() ? "none" : d.getCreaturePowerType())
                + "  charges=" + d.getCreaturePowerCharges() + "/100";

        // ── familiar ─────────────────────────────────────────────────────
        String famUUID = d.getFamiliarUUID().map(u -> u.toString().substring(0, 8) + "...").orElse("none");
        String famExtra = d.getFamiliarType().isEmpty() ? "" :
                " (" + d.getFamiliarType() + " \"" + d.getFamiliarName() + "\""
                        + " sitting=" + d.isFamiliarSitting()
                        + " nameVisible=" + d.isFamiliarNameVisible() + ")";
        String familiar = "  familiar=" + famUUID + famExtra;

        // ── Spirit World ─────────────────────────────────────────────────
        String sw = "  inSpiritWorld=" + d.isInSpiritWorld()
                + (d.isInSpiritWorld() ? "  (" + (d.isSpiritWorldDream() ? "dream" : "nightmare") + ")" : "")
                + "  manifestedGhost=" + d.isManifestedGhost()
                + "  manifestTime=" + tickStr(d.getManifestationTime())
                + "  corpse=" + d.getCorpseUUID().map(u -> u.toString().substring(0, 8) + "...").orElse("none");
        String returnP = "  returnPos=" + d.getReturnPos().map(Object::toString).orElse("none")
                + "  returnDim=" + (d.getReturnDimension().isEmpty() ? "none" : d.getReturnDimension());

        // ── death / hunter timing ────────────────────────────────────────
        String death = "  lastDeath=" + d.getLastDeathPos().map(Object::toString).orElse("none")
                + "  dim=" + (d.getLastDeathDimension().isEmpty() ? "none" : d.getLastDeathDimension());
        long nhKill   = d.getLastNightmareKillGameTime();
        long whTrigger = d.getWitchHunterTriggerGameTime();
        String timing = "  lastNightmareKill=" + (nhKill == Long.MIN_VALUE ? "never" : nhKill + "t")
                + "  witchHunterTrigger=" + (whTrigger == Long.MIN_VALUE ? "none" : whTrigger + "t");

        String out = "[Witchery] " + p.getName().getString() + "\n"
                + vampire + "\n" + werewolf + "\n"
                + brews + "\n" + misc + "\n"
                + infusion + "\n" + creature + "\n"
                + familiar + "\n"
                + sw + "\n" + returnP + "\n"
                + death + "\n" + timing;

        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }

    /** Debug: set {@code target}'s Mystic Branch infusion type + energy so survival casting works. */
    private static int infuse(CommandSourceStack src, String type, int energy, ServerPlayer target) {
        InfusionType infusion = InfusionType.fromString(type);
        WitcheryPlayerData data = WitcheryAttachments.get(target);
        if (infusion == InfusionType.NONE) {
            data.clearInfusion();
            src.sendSuccess(() -> Component.literal("[Branch] Cleared infusion on "
                    + target.getName().getString() + "."), false);
        } else {
            data.setInfusionType(infusion);
            data.setInfusionEnergy(energy, energy);
            com.emoniph.witchery.events.InfusionEvents.onInfusionGranted(target, infusion);
            src.sendSuccess(() -> Component.literal("[Branch] " + target.getName().getString()
                    + " infused with " + infusion.name() + " (" + energy + "/" + energy + ")."), false);
        }
        sync(target, data);
        return 1;
    }

    /** Grant (or clear) Mystic Branch demon-soul spell knowledge on {@code target}. */
    private static int learnSymbol(CommandSourceStack src, String knowledge, ServerPlayer target) {
        WitcheryAttachments.LearnedSymbols learned = WitcheryAttachments.learned(target);
        String name = knowledge.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> all = java.util.List.of("ignianima", "carnosadiem", "morsmordre", "tormentum");

        if (name.equals("clear")) {
            int n = learned.keys.size();
            learned.keys.clear();
            src.sendSuccess(() -> Component.literal("[Branch] Cleared " + n + " learned glyph(s) from "
                    + target.getName().getString() + "."), false);
            return n;
        }
        if (name.equals("all")) {
            learned.keys.addAll(all);
            src.sendSuccess(() -> Component.literal("[Branch] " + target.getName().getString()
                    + " has learned all demonic glyphs."), false);
            return all.size();
        }
        if (!all.contains(name)) {
            src.sendFailure(Component.literal("Unknown knowledge '" + knowledge + "'. Options: "
                    + String.join(", ", all) + ", all, clear"));
            return 0;
        }
        boolean added = learned.keys.add(name);
        src.sendSuccess(() -> Component.literal("[Branch] " + target.getName().getString()
                + (added ? " has learned " : " already knew ") + name + "."), false);
        return 1;
    }

    /** Force a named crystal-ball prophecy (or "all") to come true on {@code target} right now. */
    private static int triggerProphecy(CommandSourceStack src, String name, ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel level)) {
            src.sendFailure(Component.literal("Target must be in a server level."));
            return 0;
        }

        if (name.equalsIgnoreCase("all")) {
            int fired = 0;
            for (int id : Predictions.DEBUG_NAMES.values()) {
                if (Predictions.debugForce(level, target, id)) {
                    fired++;
                }
            }
            final int total = fired;
            src.sendSuccess(() -> Component.literal("[Crystal] Forced all prophecies on "
                    + target.getName().getString() + " (" + total + "/" + Predictions.DEBUG_NAMES.size()
                    + " fired)."), false);
            return fired;
        }

        Integer id = Predictions.DEBUG_NAMES.get(name.toLowerCase(java.util.Locale.ROOT));
        if (id == null) {
            src.sendFailure(Component.literal("Unknown prophecy '" + name + "'. Options: "
                    + String.join(", ", Predictions.DEBUG_NAMES.keySet()) + ", all"));
            return 0;
        }

        boolean fired = Predictions.debugForce(level, target, id);
        src.sendSuccess(() -> Component.literal("[Crystal] '" + name + "' on "
                + target.getName().getString() + (fired ? "" : " (no open ground for the effect — try elsewhere)")), false);
        return fired ? 1 : 0;
    }

    private static int showBrewDebug(CommandSourceStack src, ServerPlayer target)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        WitcheryPlayerData d = WitcheryAttachments.get(target);
        String out = "[Witchery Brews] " + target.getName().getString() + "\n"
                + "  Soaring:          " + tickStr(d.getSoaringTicks()) + "\n"
                + "  Depths:           " + tickStr(d.getDepthsTicks()) + "\n"
                + "  Grotesque Aura:   " + tickStr(d.getGrotesqueTicks()) + "\n"
                + "  Ghost Walker:     " + (d.isGhostWalkerActive() ? "active" : "inactive") + "\n"
                + "  Howl Cooldown:    " + tickStr(d.getHowlCooldown()) + "\n"
                + "  TormentCooldown:  " + tickStr(d.getTormentPortalCooldown()) + "\n"
                + "  Infusion:         " + d.getInfusionType()
                + "  " + d.getInfusionEnergy() + "/" + d.getInfusionMaxEnergy() + "\n"
                + "  CreaturePower:    " + (d.getCreaturePowerType().isEmpty() ? "none" : d.getCreaturePowerType())
                + "  charges=" + d.getCreaturePowerCharges() + "/100";
        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }

    /** Formats tick count as "0t" or "Xt (Ys)". */
    private static String tickStr(int t) {
        return t == 0 ? "0t" : t + "t (" + (t / 20) + "s)";
    }

    // ── Dimension helpers ─────────────────────────────────────────────────────

    /**
     * Forces chunk (0,0) to FULL generation status, then scans for a safe standing position.
     * Must be called on the server thread — getChunk with ChunkStatus.FULL is synchronous.
     *
     * Dream World: End-style generator, central island peaks around Y=55–70.
     *   Scan downward from 75 for the first solid block with 2 air above it.
     *
     * Torment uses its dedicated maze-entry helper instead of this generic scan.
     */
    private static double[] findSafeEntry(ServerLevel level, ResourceKey<Level> dim, BlockPos referencePos) {
        // Force full chunk generation at spawn point (synchronous on server thread)
        BlockPos chunkAnchor = dim.equals(WitcheryDimensions.MIRROR_WORLD) ? referencePos : BlockPos.ZERO;
        level.getChunk(chunkAnchor.getX() >> 4, chunkAnchor.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);

        if (dim.equals(WitcheryDimensions.SPIRIT_WORLD)) {
            // Overworld-style terrain: scan from 200 down to find solid surface
            for (int y = 200; y >= -60; y--) {
                var floor = new net.minecraft.core.BlockPos(0, y, 0);
                if (!level.getBlockState(floor).isAir()
                        && level.getBlockState(floor.above()).isAir()
                        && level.getBlockState(floor.above().above()).isAir()) {
                    return new double[]{ 0.5, y + 1, 0.5 };
                }
            }
            return new double[]{ 0.5, 65, 0.5 };
        }

        if (dim.equals(WitcheryDimensions.MIRROR_WORLD)) {
            // Mirror World is only 16 blocks tall (y=0-15). Scanning from y=200 finds the
            // CEILING (y=15) first because y=16+ are out-of-range air, placing the player
            // on top of all mirror blocks (y=16). Instead, snap to the chunk's guaranteed-open
            // interior centre (local 8,8 is always air) and use the known floor height.
            int cx = referencePos.getX() >> 4;
            int cz = referencePos.getZ() >> 4;
            return new double[]{ cx * 16 + 8.5, com.emoniph.witchery.world.MirrorHelper.ROOM_FLOOR_Y, cz * 16 + 8.5 };
        }

        return new double[]{ 0.5, 64, 0.5 };
    }

    private static int enterDimension(CommandSourceStack src, ResourceKey<Level> dim)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel target = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(dim);
        if (target == null) {
            src.sendFailure(Component.literal("Dimension " + dim.identifier() + " is not loaded."));
            return 0;
        }
        if (dim.equals(WitcheryDimensions.TORMENT)) {
            TormentHelper.sendToTorment(player, 0);
            src.sendSuccess(() -> Component.literal("Entered torment at maze level 0."), false);
            return 1;
        }
        // Save return position before teleporting
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        data.setReturnPos(player.blockPosition(), player.level().dimension());
        sync(player, data);

        double[] safePos = findSafeEntry(target, dim, player.blockPosition());
        player.teleportTo(target, safePos[0], (double)(safePos[1]), safePos[2], java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Entered " + dim.identifier().getPath() + "."), false);
        return 1;
    }

    private static int exitDimension(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        WitcheryPlayerData data = WitcheryAttachments.get(player);

        if (data.getReturnPos().isEmpty()) {
            ServerLevel overworld = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                var spawn = new net.minecraft.core.BlockPos(0, 64, 0);
                player.teleportTo(overworld, spawn.getX() + 0.5, (double)(spawn.getY()), spawn.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
                src.sendSuccess(() -> Component.literal(
                        "No return point stored — sent to overworld spawn at "
                        + spawn.getX() + ", " + spawn.getY() + ", " + spawn.getZ() + "."), false);
            }
            return 1;
        }

        var pos = data.getReturnPos().get();
        String dimStr = data.getReturnDimension();
        ResourceKey<Level> returnDim = ResourceKey.create(Registries.DIMENSION,
                Identifier.parse(dimStr.isEmpty() ? "minecraft:overworld" : dimStr));
        ServerLevel returnLevel = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(returnDim);
        if (returnLevel == null) returnLevel = ((net.minecraft.server.level.ServerLevel) player.level()).getServer().getLevel(Level.OVERWORLD);

        // Snapshot for the lambda before clearing
        final var returnLevelFinal = returnLevel;
        final var posFinal = pos;
        final var dimFinal = dimStr.isEmpty() ? "minecraft:overworld" : dimStr;

        data.clearReturnPos();
        sync(player, data);

        player.teleportTo(returnLevelFinal, posFinal.getX() + 0.5, (double)(posFinal.getY()), posFinal.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal(
                "Returning to " + dimFinal + " at "
                + posFinal.getX() + ", " + posFinal.getY() + ", " + posFinal.getZ() + "."), false);
        return 1;
    }

    // ── Spawn helper ──────────────────────────────────────────────────────────

    /** Summon a Reflection linked to the command executor as its owner. */
    private static int summonReflection(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;
        com.emoniph.witchery.entity.ReflectionEntity reflection =
                new com.emoniph.witchery.entity.ReflectionEntity(WitcheryEntities.REFLECTION.get(), sl);
        var pos = player.blockPosition().relative(player.getDirection(), 2);
        reflection.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        reflection.setOwner(player);
        sl.addFreshEntity(reflection);
        src.sendSuccess(() -> Component.literal("Summoned your Reflection."), false);
        return 1;
    }

    private static <T extends Mob> int spawnEntity(CommandSourceStack src, EntityType<T> type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;
        T entity = type.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (entity == null) return 0;
        var pos = player.blockPosition().relative(player.getDirection(), 2);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.finalizeSpawn(sl, new net.minecraft.world.DifficultyInstance(sl.getDifficulty(), 0L, 0L, 0.0F),
                net.minecraft.world.entity.EntitySpawnReason.TRIGGERED, null);
        sl.addFreshEntity(entity);
        src.sendSuccess(() -> Component.literal("Spawned " + type.getDescription().getString() + "."), false);
        return 1;
    }

    private static int spawnRavens(CommandSourceStack src, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;

        BlockPos center = player.blockPosition().relative(player.getDirection(), 5);
        int spawned = com.emoniph.witchery.item.BrewOfHitchcockItem.spawnRavenFlock(
                sl,
                center.getX() + 0.5,
                center.getY() + 1.0,
                center.getZ() + 0.5,
                count,
                com.emoniph.witchery.item.BrewOfHitchcockItem.DEFAULT_RAVEN_SPREAD);
        if (spawned <= 0) {
            src.sendFailure(Component.literal("Failed to spawn ravens."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(
                "Spawned " + spawned + " ravens at "
                        + center.getX() + ", " + center.getY() + ", " + center.getZ() + "."), false);
        return spawned;
    }

    private static int startHitchcockRitual(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return startHitchcockRitual(src, com.emoniph.witchery.item.BrewOfHitchcockItem.DEFAULT_RAVEN_COUNT);
    }

    private static int startHitchcockRitual(CommandSourceStack src, int count)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;

        int spawned = com.emoniph.witchery.item.BrewOfHitchcockItem.spawnRavenFlock(
                sl,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                count,
                com.emoniph.witchery.item.BrewOfHitchcockItem.DEFAULT_RAVEN_SPREAD);
        if (spawned <= 0) {
            src.sendFailure(Component.literal("Failed to start Brew of Hitchcock."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(
                "Started Brew of Hitchcock and released " + spawned + " ravens."), false);
        return spawned;
    }

    // ── New debug handlers ────────────────────────────────────────────────────

    /** Toggle dream/nightmare mode, using the normal Spirit World entry path outside the dimension. */
    private static int toggleNightmare(CommandSourceStack src, boolean nightmare)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        if (!d.isInSpiritWorld()) {
            if (!p.level().dimension().equals(Level.OVERWORLD)) {
                src.sendFailure(Component.literal("Nightmare debug entry can only start from the Overworld or while already in the Spirit World."));
                return 0;
            }
            SpiritWorldHelper.enter(p, nightmare ? 1.0 : 0.0);
            src.sendSuccess(() -> Component.literal("Entered a forced Spirit World " + (nightmare ? "nightmare" : "dream") + "."), false);
            return 1;
        }

        d.setSpiritWorldIsDream(!nightmare);
        d.setDemonicNightmare(false);
        sync(p, d);
        String mode = nightmare ? "nightmare" : "dream";
        src.sendSuccess(() -> Component.literal("Spirit World mode set to " + mode + "."), false);
        if (p.level() instanceof ServerLevel spiritWorld
                && spiritWorld.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            if (nightmare) {
                d.setLastNightmareKillGameTime(Long.MIN_VALUE);
                SpiritWorldHelper.tickNightmareRespawn(p, spiritWorld, d);
                sync(p, d);
                src.sendSuccess(() -> Component.literal("Nightmare pressure forced; a victim-bound Nightmare should now be present if one was not already nearby."), false);
            } else {
                int removed = removeVictimNightmares(p, spiritWorld);
                src.sendSuccess(() -> Component.literal("Peaceful dream restored; removed " + removed + " bound Nightmare(s)."), false);
            }
        }
        return 1;
    }

    private static int removeVictimNightmares(ServerPlayer player, ServerLevel level) {
        var nightmares = level.getEntitiesOfClass(NightmareEntity.class,
                player.getBoundingBox().inflate(64.0),
                nightmare -> nightmare.matchesVictim(player));
        nightmares.forEach(NightmareEntity::discard);
        return nightmares.size();
    }

    /** Set werewolf transformation level (0.0 = none, 1.0 = full moon). */
    private static int setTransformation(CommandSourceStack src, float level)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setTransformationLevel(level);
        sync(p, d);
        src.sendSuccess(() -> Component.literal(
                "Transformation level set to " + String.format("%.2f", level) + "."), false);
        return 1;
    }

    /** Teleport to Torment maze level N (0-5). Positions player at the entry X/Z of that level. */
    private static int tormentLevel(CommandSourceStack src, int level)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel torment = ((ServerLevel) p.level()).getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) {
            src.sendFailure(Component.literal("Torment dimension not loaded."));
            return 0;
        }
        TormentHelper.sendToTorment(p, level);
        final int lvl = level;
        src.sendSuccess(() -> Component.literal(
                "Teleported to Torment maze level " + lvl + " (Y=" + TormentHelper.getStandingY(level) + ")."), false);
        return 1;
    }

    /** Server-side Torment audit for fresh/deleted dimension chunks. */
    private static int verifyTorment(CommandSourceStack src) {
        ServerLevel torment = src.getServer().getLevel(WitcheryDimensions.TORMENT);
        if (torment == null) {
            src.sendFailure(Component.literal("Torment dimension is not loaded."));
            return 0;
        }

        loadTormentFootprintChunks(torment);

        java.util.List<String> failures = new java.util.ArrayList<>();
        verifyTormentDimensionSettings(torment, failures);
        for (int level = 0; level < TormentHelper.NUM_LEVELS; level++) {
            verifyTormentLevel(torment, level, failures);
        }

        if (failures.isEmpty()) {
            src.sendSuccess(() -> Component.literal(
                    "Torment verification passed for all 6 levels: legacy Force/Bedrock maze, portal chamber, 3 chest rooms, and red navigable atmosphere look correct."),
                    false);
            return 1;
        }

        src.sendFailure(Component.literal("Torment verification failed with " + failures.size() + " mismatch(es)."));
        for (String failure : failures) {
            src.sendFailure(Component.literal(" - " + failure));
        }
        return 0;
    }

    /** Server-side Spirit World data audit for fresh/deleted dimension chunks. */
    private static int verifySpiritWorld(CommandSourceStack src) {
        ServerLevel spiritWorld = src.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spiritWorld == null) {
            src.sendFailure(Component.literal("Spirit World dimension is not loaded."));
            return 0;
        }

        java.util.List<String> failures = new java.util.ArrayList<>();
        verifySpiritWorldDimensionSettings(spiritWorld, failures);
        verifySpiritWorldBiomeData(spiritWorld, failures);

        if (failures.isEmpty()) {
            src.sendSuccess(() -> Component.literal(
                    "Spirit World verification passed: fixed biome, source baseline animals, native Spirit spawns, and vegetation/cotton feature wiring are loaded."),
                    false);
            return 1;
        }

        src.sendFailure(Component.literal("Spirit World verification failed with " + failures.size() + " mismatch(es)."));
        for (String failure : failures) {
            src.sendFailure(Component.literal(" - " + failure));
        }
        return 0;
    }

    private static void loadTormentFootprintChunks(ServerLevel torment) {
        int minChunkX = Math.floorDiv(TormentHelper.STRUCTURE_MIN_X, 16);
        int maxChunkX = Math.floorDiv(TormentHelper.STRUCTURE_MAX_X, 16);
        int minChunkZ = Math.floorDiv(TormentHelper.STRUCTURE_MIN_Z, 16);
        int maxChunkZ = Math.floorDiv(TormentHelper.STRUCTURE_MAX_Z, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                torment.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static void verifyTormentDimensionSettings(ServerLevel torment, java.util.List<String> failures) {
        var type = torment.dimensionType();
        if (!type.hasSkyLight()) {
            failures.add("Torment dimension type lost skylight.");
        }
        if (type.hasCeiling()) {
            failures.add("Torment dimension type unexpectedly has a ceiling.");
        }
        if (Math.abs(type.ambientLight() - 0.15F) > 0.0001F) {
            failures.add("Torment ambient light should be 0.15 but was " + type.ambientLight() + ".");
        }

        JsonObject dimensionJson = readTormentDataJson(torment, "dimension_type/torment.json", failures);
        if (dimensionJson != null) {
            if (!dimensionJson.has("fixed_time") || dimensionJson.get("fixed_time").getAsLong() != 6000L) {
                failures.add("Torment dimension JSON fixed_time should stay 6000.");
            }
            if (!dimensionJson.has("effects") || !"minecraft:overworld".equals(dimensionJson.get("effects").getAsString())) {
                failures.add("Torment dimension JSON effects should stay minecraft:overworld.");
            }
        }

        JsonObject biomeJson = readTormentDataJson(torment, "worldgen/biome/torment.json", failures);
        if (biomeJson != null) {
            JsonObject effectsJson = biomeJson.getAsJsonObject("effects");
            if (effectsJson == null) {
                failures.add("Torment biome JSON is missing its effects object.");
            } else {
                if (!effectsJson.has("fog_color") || effectsJson.get("fog_color").getAsInt() != 10498610) {
                    failures.add("Torment biome JSON fog_color should stay 10498610.");
                }
                if (!effectsJson.has("sky_color") || effectsJson.get("sky_color").getAsInt() != 13127760) {
                    failures.add("Torment biome JSON sky_color should stay 13127760.");
                }
            }
        }
    }

    private static void verifySpiritWorldDimensionSettings(ServerLevel spiritWorld, java.util.List<String> failures) {
        var type = spiritWorld.dimensionType();
        if (!type.hasSkyLight()) {
            failures.add("Spirit World dimension type lost skylight.");
        }
        if (type.hasCeiling()) {
            failures.add("Spirit World dimension type unexpectedly has a ceiling.");
        }
        if (Math.abs(type.ambientLight()) > 0.0001F) {
            failures.add("Spirit World ambient light should be 0.0 but was " + type.ambientLight() + ".");
        }

        JsonObject dimensionTypeJson = readWitcheryDataJson(spiritWorld, "dimension_type/spirit_world.json", "Spirit World", failures);
        if (dimensionTypeJson != null) {
            if (!dimensionTypeJson.has("effects") || !"minecraft:overworld".equals(dimensionTypeJson.get("effects").getAsString())) {
                failures.add("Spirit World dimension type JSON effects should stay minecraft:overworld.");
            }
            if (!dimensionTypeJson.has("ambient_light") || Math.abs(dimensionTypeJson.get("ambient_light").getAsFloat()) > 0.0001F) {
                failures.add("Spirit World dimension type JSON ambient_light should stay 0.0.");
            }
            if (!dimensionTypeJson.has("has_skylight") || !dimensionTypeJson.get("has_skylight").getAsBoolean()) {
                failures.add("Spirit World dimension type JSON should keep has_skylight true.");
            }
            if (!dimensionTypeJson.has("has_ceiling") || dimensionTypeJson.get("has_ceiling").getAsBoolean()) {
                failures.add("Spirit World dimension type JSON should keep has_ceiling false.");
            }
        }

        JsonObject dimensionJson = readWitcheryDataJson(spiritWorld, "dimension/spirit_world.json", "Spirit World", failures);
        if (dimensionJson != null) {
            JsonObject generatorJson = dimensionJson.getAsJsonObject("generator");
            if (generatorJson == null) {
                failures.add("Spirit World dimension JSON is missing its generator object.");
            } else {
                if (!generatorJson.has("settings") || !"minecraft:overworld".equals(generatorJson.get("settings").getAsString())) {
                    failures.add("Spirit World generator should keep minecraft:overworld noise settings.");
                }
                JsonObject biomeSourceJson = generatorJson.getAsJsonObject("biome_source");
                if (biomeSourceJson == null) {
                    failures.add("Spirit World generator is missing its fixed biome source.");
                } else if (!biomeSourceJson.has("biome") || !"witchery:spirit_world".equals(biomeSourceJson.get("biome").getAsString())) {
                    failures.add("Spirit World generator should use the fixed witchery:spirit_world biome.");
                }
            }
        }
    }

    private static void verifySpiritWorldBiomeData(ServerLevel spiritWorld, java.util.List<String> failures) {
        JsonObject biomeJson = readWitcheryDataJson(spiritWorld, "worldgen/biome/spirit_world.json", "Spirit World", failures);
        if (biomeJson == null) {
            return;
        }

        JsonObject effectsJson = biomeJson.getAsJsonObject("effects");
        if (effectsJson == null) {
            failures.add("Spirit World biome JSON is missing its effects object.");
        } else {
            verifyJsonInt(effectsJson, "fog_color", 13382553, "Spirit World biome fog_color", failures);
            verifyJsonInt(effectsJson, "sky_color", 13382553, "Spirit World biome sky_color", failures);
            verifyJsonInt(effectsJson, "grass_color", 9027656, "Spirit World biome grass_color", failures);
            verifyJsonInt(effectsJson, "foliage_color", 8371292, "Spirit World biome foliage_color", failures);
        }

        verifyBiomeSpawner(biomeJson, "creature", "minecraft:sheep", failures);
        verifyBiomeSpawner(biomeJson, "creature", "minecraft:pig", failures);
        verifyBiomeSpawner(biomeJson, "creature", "minecraft:chicken", failures);
        verifyBiomeSpawner(biomeJson, "creature", "minecraft:cow", failures);
        verifyBiomeSpawner(biomeJson, "ambient", "witchery:spirit", failures);

        verifyBiomeFeature(biomeJson, "witchery:spirit_grass", failures);
        verifyBiomeFeature(biomeJson, "witchery:spirit_tall_grass", failures);
        verifyBiomeFeature(biomeJson, "witchery:spirit_mushrooms", failures);
        verifyBiomeFeature(biomeJson, "witchery:spirit_dead_bush", failures);
        verifyBiomeFeature(biomeJson, "witchery:spirit_tree", failures);
        verifyBiomeFeature(biomeJson, "witchery:wispy_cotton", failures);
        verifyBiomeFeature(biomeJson, "witchery:disturbed_cotton", failures);
    }

    private static void verifyJsonInt(JsonObject json, String key, int expected, String label, java.util.List<String> failures) {
        if (!json.has(key) || json.get(key).getAsInt() != expected) {
            failures.add(label + " should stay " + expected + ".");
        }
    }

    private static void verifyBiomeSpawner(JsonObject biomeJson, String category, String type, java.util.List<String> failures) {
        JsonObject spawnersJson = biomeJson.getAsJsonObject("spawners");
        if (spawnersJson == null || !spawnersJson.has(category)) {
            failures.add("Spirit World biome is missing " + category + " spawners.");
            return;
        }
        for (var element : spawnersJson.getAsJsonArray(category)) {
            if (element.isJsonObject()) {
                JsonObject entry = element.getAsJsonObject();
                if (entry.has("type") && type.equals(entry.get("type").getAsString())) {
                    return;
                }
            }
        }
        failures.add("Spirit World biome is missing " + type + " in the " + category + " spawn bucket.");
    }

    private static void verifyBiomeFeature(JsonObject biomeJson, String feature, java.util.List<String> failures) {
        if (!biomeJson.has("features")) {
            failures.add("Spirit World biome is missing its feature step list.");
            return;
        }
        for (var stepElement : biomeJson.getAsJsonArray("features")) {
            if (!stepElement.isJsonArray()) {
                continue;
            }
            for (var featureElement : stepElement.getAsJsonArray()) {
                if (featureElement.isJsonPrimitive() && feature.equals(featureElement.getAsString())) {
                    return;
                }
            }
        }
        failures.add("Spirit World biome is missing placed feature " + feature + ".");
    }

    private static void verifyTormentLevel(ServerLevel torment, int level, java.util.List<String> failures) {
        int mazeY = TormentHelper.getMazeY(level);
        int standingY = TormentHelper.getStandingY(level);
        BlockPos entryPos = TormentHelper.getEntryPos(level);
        BlockPos portalLower = TormentHelper.getPortalLowerPos(level);
        long seed = torment.getSeed();

        verifyStateAt(torment, new BlockPos(TormentHelper.ORIGIN_X, mazeY, TormentHelper.ORIGIN_Z),
                state -> state.is(WitcheryBlocks.FORCE.get()),
                "Level " + level + " must keep Force walls on the outer maze border.", failures);
        verifyStateAt(torment, new BlockPos(TormentHelper.ORIGIN_X, mazeY + 5, TormentHelper.ORIGIN_Z),
                state -> state.is(WitcheryBlocks.FORCE.get()),
                "Level " + level + " must keep Force ceilings on the outer maze border.", failures);

        var sampleForce = torment.getBlockState(new BlockPos(TormentHelper.ORIGIN_X, mazeY + 5, TormentHelper.ORIGIN_Z));
        if (sampleForce.is(WitcheryBlocks.FORCE.get())) {
            if (sampleForce.getRenderShape() != RenderShape.INVISIBLE) {
                failures.add("Level " + level + " Force ceiling is no longer invisible.");
            }
            if (sampleForce.getLightBlock() != 0) {
                failures.add("Level " + level + " Force ceiling no longer has light block 0.");
            }
            if (!sampleForce.propagatesSkylightDown()) {
                failures.add("Level " + level + " Force ceiling no longer propagates skylight.");
            }
        }

        verifyStateAt(torment, entryPos.below(),
                state -> state.is(Blocks.BEDROCK),
                "Level " + level + " entry foundation must be Bedrock.", failures);
        verifyStateAt(torment, new BlockPos(entryPos.getX(), mazeY, entryPos.getZ()),
                state -> state.is(Blocks.BEDROCK) || state.is(Blocks.MYCELIUM),
                "Level " + level + " entry floor must be Bedrock or rare Mycelium.", failures);
        verifyStateAt(torment, entryPos,
                net.minecraft.world.level.block.state.BlockState::isAir,
                "Level " + level + " entry standing space must stay open.", failures);
        verifyStateAt(torment, entryPos.above(),
                net.minecraft.world.level.block.state.BlockState::isAir,
                "Level " + level + " entry headroom must stay open.", failures);
        verifyStateAt(torment, new BlockPos(entryPos.getX(), standingY + 4, entryPos.getZ()),
                state -> state.is(WitcheryBlocks.FORCE.get()),
                "Level " + level + " entry lane must keep a Force ceiling.", failures);

        verifyStateAt(torment, portalLower,
                state -> state.is(WitcheryBlocks.TORMENT_PORTAL.get())
                        && state.hasProperty(TormentPortalBlock.AXIS)
                        && state.getValue(TormentPortalBlock.AXIS) == Direction.Axis.X,
                "Level " + level + " portal lower block is missing from the old chamber.", failures);
        verifyStateAt(torment, portalLower.above(),
                state -> state.is(WitcheryBlocks.TORMENT_PORTAL.get())
                        && state.hasProperty(TormentPortalBlock.AXIS)
                        && state.getValue(TormentPortalBlock.AXIS) == Direction.Axis.X,
                "Level " + level + " portal upper block is missing from the old chamber.", failures);
        verifyStateAt(torment, portalLower.above(2),
                state -> state.is(Blocks.BEDROCK),
                "Level " + level + " portal top frame must be Bedrock.", failures);
        verifyStateAt(torment, portalLower.west(),
                state -> state.is(Blocks.BEDROCK),
                "Level " + level + " portal west frame must be Bedrock.", failures);
        verifyStateAt(torment, portalLower.east(),
                state -> state.is(Blocks.BEDROCK),
                "Level " + level + " portal east frame must be Bedrock.", failures);
        verifyStateAt(torment, new BlockPos(TormentHelper.PORTAL_X, standingY, TormentHelper.PORTAL_Z - 3),
                net.minecraft.world.level.block.state.BlockState::isAir,
                "Level " + level + " portal chamber should stay open three blocks before the portal.", failures);
        verifyStateAt(torment, new BlockPos(TormentHelper.PORTAL_X - 3, standingY, TormentHelper.PORTAL_Z - 5),
                net.minecraft.world.level.block.state.BlockState::isAir,
                "Level " + level + " portal chamber west side should stay open.", failures);
        verifyStateAt(torment, new BlockPos(TormentHelper.PORTAL_X + 3, standingY, TormentHelper.PORTAL_Z - 5),
                net.minecraft.world.level.block.state.BlockState::isAir,
                "Level " + level + " portal chamber east side should stay open.", failures);

        int totalChestCount = 0;
        int tormentStoneCount = 0;
        for (int x = TormentHelper.STRUCTURE_MIN_X; x <= TormentHelper.STRUCTURE_MAX_X; x++) {
            for (int z = TormentHelper.STRUCTURE_MIN_Z; z <= TormentHelper.STRUCTURE_MAX_Z; z++) {
                for (int y = mazeY - 1; y <= mazeY + 5; y++) {
                    if (torment.getBlockState(new BlockPos(x, y, z)).is(WitcheryBlocks.TORMENT_STONE.get())) {
                        tormentStoneCount++;
                    }
                }

                if (torment.getBlockState(new BlockPos(x, mazeY, z)).is(WitcheryBlocks.REFILLING_CHEST.get())) {
                    totalChestCount++;
                }
            }
        }

        verifyStateAt(torment, getTormentLeftChestPos(seed, level),
                state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                "Level " + level + " left chest room lost its Refilling Chest.", failures);
        verifyRefillingChestBlockEntity(torment, getTormentLeftChestPos(seed, level), level, "left", failures);
        verifyStateAt(torment, getTormentCenterChestPos(level),
                state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                "Level " + level + " center chest room lost its Refilling Chest.", failures);
        verifyRefillingChestBlockEntity(torment, getTormentCenterChestPos(level), level, "center", failures);
        verifyStateAt(torment, getTormentRightChestPos(seed, level),
                state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                "Level " + level + " right chest room lost its Refilling Chest.", failures);
        verifyRefillingChestBlockEntity(torment, getTormentRightChestPos(seed, level), level, "right", failures);

        if (tormentStoneCount > 0) {
            failures.add("Level " + level + " still contains " + tormentStoneCount + " Torment Stone block(s) inside the legacy maze footprint.");
        }
        if (totalChestCount != 3) {
            failures.add("Level " + level + " should have exactly 3 floor-level Refilling Chests, but found " + totalChestCount + ".");
        }
    }

    private static void verifyRefillingChestBlockEntity(ServerLevel level, BlockPos pos, int tormentLevel,
                                                        String room, java.util.List<String> failures) {
        if (level.getBlockState(pos).is(WitcheryBlocks.REFILLING_CHEST.get())
                && !(level.getBlockEntity(pos) instanceof RefillingChestBlockEntity)) {
            failures.add("Level " + tormentLevel + " " + room + " Refilling Chest is missing its block entity renderer/storage.");
        }
    }

    private static void verifyStateAt(ServerLevel level, net.minecraft.core.BlockPos pos,
                                      java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> predicate,
                                      String message, java.util.List<String> failures) {
        if (!predicate.test(level.getBlockState(pos))) {
            failures.add(message + " Found " + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()) + " at " + pos + ".");
        }
    }

    private static JsonObject readTormentDataJson(ServerLevel torment, String path, java.util.List<String> failures) {
        return readWitcheryDataJson(torment, path, "Torment", failures);
    }

    private static JsonObject readWitcheryDataJson(ServerLevel level, String path, String label, java.util.List<String> failures) {
        var resourceId = Identifier.fromNamespaceAndPath("witchery", path);
        var resource = level.getServer().getResourceManager().getResource(resourceId).orElse(null);
        if (resource == null) {
            failures.add("Missing " + label + " data resource " + resourceId + ".");
            return null;
        }

        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | IllegalStateException ex) {
            failures.add("Could not read " + label + " data resource " + resourceId + ": " + ex.getMessage());
            return null;
        }
    }

    private static BlockPos getTormentLeftChestPos(long seed, int level) {
        return new BlockPos(TormentHelper.ORIGIN_X + 3, TormentHelper.getMazeY(level),
                TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + chestShift(seed, level, 0));
    }

    private static BlockPos getTormentCenterChestPos(int level) {
        return new BlockPos(TormentHelper.ENTRY_X, TormentHelper.getMazeY(level), TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE);
    }

    private static BlockPos getTormentRightChestPos(long seed, int level) {
        return new BlockPos(TormentHelper.ORIGIN_X + TormentHelper.MAZE_SIZE * 2 - 3, TormentHelper.getMazeY(level),
                TormentHelper.ORIGIN_Z + TormentHelper.MAZE_SIZE + chestShift(seed, level, 1));
    }

    private static int chestShift(long seed, int level, int index) {
        long h = seed ^ ((long) level * 0x632be59bd9b4e019L) ^ ((long) index * 0x85157af5L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (int) Math.floorMod(h, 11L) - 5;
    }

    /** Give a witchery item by registry path name. */
    private static int giveItem(CommandSourceStack src, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        var loc = Identifier.fromNamespaceAndPath("witchery", name);
        Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            src.sendFailure(Component.literal("Unknown witchery item: " + name));
            return 0;
        }
        p.addItem(new ItemStack(item));
        src.sendSuccess(() -> Component.literal("Gave witchery:" + name + "."), false);
        return 1;
    }

    /** Give one of every registered witchery item. */
    private static int giveAll(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int count = 0;
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (!entry.getKey().identifier().getNamespace().equals("witchery")) continue;
            Item item = entry.getValue();
            if (item == net.minecraft.world.item.Items.AIR) continue;
            p.addItem(new ItemStack(item));
            count++;
        }
        final int total = count;
        src.sendSuccess(() -> Component.literal("Gave " + total + " witchery items."), false);
        return total;
    }

    /** Show info about the nearest kettle within 8 blocks. */
    private static int kettleInfo(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!(p.level() instanceof ServerLevel sl)) return 0;
        var kettle = findNearbyKettle(p, sl);
        if (kettle == null) { src.sendFailure(Component.literal("No kettle within 8 blocks.")); return 0; }
        int ingredients = 0;
        for (int i = 0; i < com.emoniph.witchery.blocks.entity.KettleBlockEntity.RESULT_SLOT; i++)
            if (!kettle.getItem(i).isEmpty()) ingredients++;
        final int ingCount = ingredients;
        src.sendSuccess(() -> Component.literal(
                "Kettle @ " + kettle.getBlockPos()
                + "\n  water=" + kettle.getWaterLevel()
                + " ingredients=" + ingCount + "/6"
                + " filled=" + kettle.isFilled()
                + " ruined=" + kettle.isRuined()
                + "\n  result=" + kettle.getItem(com.emoniph.witchery.blocks.entity.KettleBlockEntity.RESULT_SLOT).getCount()
                + " bottles=" + kettle.getItem(com.emoniph.witchery.blocks.entity.KettleBlockEntity.BOTTLE_SLOT).getCount()), false);
        return 1;
    }

    /** Clear a nearby kettle — removes all items and resets state. */
    private static int kettleClear(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!(p.level() instanceof ServerLevel sl)) return 0;
        var kettle = findNearbyKettle(p, sl);
        if (kettle == null) { src.sendFailure(Component.literal("No kettle within 8 blocks.")); return 0; }
        kettle.reset(true);
        kettle.setChanged();
        src.sendSuccess(() -> Component.literal("Kettle cleared."), false);
        return 1;
    }

    private static com.emoniph.witchery.blocks.entity.KettleBlockEntity findNearbyKettle(
            ServerPlayer p, ServerLevel sl) {
        var center = p.blockPosition();
        for (var bp : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            if (sl.getBlockEntity(bp) instanceof com.emoniph.witchery.blocks.entity.KettleBlockEntity k)
                return k;
        }
        return null;
    }

    private static int setupStations(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) return 0;

        SetupHouseLayout layout = createSetupHouseLayout(player);
        clearSetupHouse(level, layout);
        buildSetupHouse(level, layout);
        placeSetupContents(level, layout);
        fillSetupStations(level, layout);
        placeSetupTraders(level, layout);

        src.sendSuccess(() -> Component.literal(
                "Cleared a 20x20 pad in front and placed a sunken pitched-roof Rowan Witchery QA hut with a boosted altar, stocked chest, power garden, and indoor traders."),
                false);
        return 1;
    }

    private static int setupRite(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) return 0;

        SetupRiteLayout layout = createSetupRiteLayout(player);
        clearSetupRitePad(level, layout);
        buildSetupRitePad(level, layout);
        placeSetupRiteCircles(level, layout);

        src.sendSuccess(() -> Component.literal(
                "Cleared a 71x17 pad in front and placed white, golden, otherwhere, and infernal small/medium/large rite rings."),
                false);
        return 1;
    }

    private static int inspectRitualCircle(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) return 0;

        BlockPos center = player.blockPosition().below();
        List<RitualCircleDetector.DetectionResult> rings = RitualCircleDetector.detectPresent(level, center);
        if (rings.isEmpty()) {
            BlockPos feet = player.blockPosition();
            rings = RitualCircleDetector.detectPresent(level, feet);
            if (!rings.isEmpty()) {
                center = feet;
            }
        }

        if (rings.isEmpty()) {
            src.sendSuccess(() -> Component.literal(
                    "No complete ritual rings found under your feet. Stand on the center block and run /witchery ritual circle."),
                    false);
            return 0;
        }

        BlockPos foundCenter = center;
        int[] variants = RitualCircleDetector.detectAllVariants(level, foundCenter);
        src.sendSuccess(() -> Component.literal("Ritual circle at "
                + foundCenter.getX() + ", " + foundCenter.getY() + ", " + foundCenter.getZ()
                + ": " + describeDetectedRings(variants)
                + ". Activate by right-clicking the center floor with an Arthana."), false);
        return rings.size();
    }

    private static String describeDetectedRings(int[] variants) {
        List<String> parts = new ArrayList<>();
        appendDetectedRing(parts, "small", variants[0]);
        appendDetectedRing(parts, "medium", variants[1]);
        appendDetectedRing(parts, "large", variants[2]);
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    private static void appendDetectedRing(List<String> parts, String size, int variant) {
        if (variant == 0) {
            return;
        }
        parts.add(size + " " + chalkVariantName(variant));
    }

    private static String chalkVariantName(int variant) {
        return switch (variant) {
            case 1 -> "ritual";
            case 2 -> "otherwhere";
            case 3 -> "infernal";
            case 4 -> "golden";
            default -> "mixed";
        };
    }

    private static SetupHouseLayout createSetupHouseLayout(ServerPlayer player) {
        Direction depth = player.getDirection();
        Direction width = depth.getClockWise();
        BlockPos origin = player.blockPosition()
                .relative(depth, SETUP_FORWARD_OFFSET)
                .relative(width.getOpposite(), SETUP_HOUSE_SIZE / 2);
        return new SetupHouseLayout(origin, depth, width);
    }

    private static SetupRiteLayout createSetupRiteLayout(ServerPlayer player) {
        Direction depth = player.getDirection();
        Direction width = depth.getClockWise();
        BlockPos origin = player.blockPosition()
                .relative(depth, SETUP_RITE_FORWARD_OFFSET)
                .relative(width.getOpposite(), SETUP_RITE_PAD_WIDTH / 2);
        return new SetupRiteLayout(origin, depth, width);
    }

    private static void clearSetupRitePad(ServerLevel level, SetupRiteLayout layout) {
        clearBox(level,
                layout.pos(0, 0, 0),
                layout.pos(SETUP_RITE_PAD_WIDTH - 1, SETUP_RITE_CLEAR_HEIGHT, SETUP_RITE_PAD_DEPTH - 1));
    }

    private static void buildSetupRitePad(ServerLevel level, SetupRiteLayout layout) {
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState border = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState foundation = Blocks.STONE_BRICKS.defaultBlockState();

        for (int x = 0; x < SETUP_RITE_PAD_WIDTH; x++) {
            for (int z = 0; z < SETUP_RITE_PAD_DEPTH; z++) {
                for (int depth = 1; depth <= SETUP_RITE_FOUNDATION_DEPTH; depth++) {
                    level.setBlock(layout.pos(x, SETUP_RITE_FLOOR_Y - depth, z), foundation, 3);
                }
                boolean edge = x == 0 || z == 0 || x == SETUP_RITE_PAD_WIDTH - 1 || z == SETUP_RITE_PAD_DEPTH - 1;
                level.setBlock(layout.pos(x, SETUP_RITE_FLOOR_Y, z), edge ? border : floor, 3);
            }
        }
    }

    private static void placeSetupRiteCircles(ServerLevel level, SetupRiteLayout layout) {
        int centerZ = SETUP_RITE_PAD_DEPTH / 2;
        placeSetupRiteCircle(level, layout, 8, centerZ,
                WitcheryBlocks.CIRCLE_GLYPH.get(), Blocks.CALCITE.defaultBlockState());
        placeSetupRiteCircle(level, layout, 26, centerZ,
                WitcheryBlocks.CIRCLE.get(), Blocks.GOLD_BLOCK.defaultBlockState());
        placeSetupRiteCircle(level, layout, 44, centerZ,
                WitcheryBlocks.CIRCLE_OTHERWHERE.get(), Blocks.PURPLE_CONCRETE.defaultBlockState());
        placeSetupRiteCircle(level, layout, 62, centerZ,
                WitcheryBlocks.CIRCLE_INFERNAL.get(), Blocks.RED_NETHER_BRICKS.defaultBlockState());
    }

    private static void placeSetupRiteCircle(ServerLevel level, SetupRiteLayout layout, int centerX, int centerZ,
                                             Block chalkBlock, BlockState centerFloor) {
        BlockPos floorCenter = layout.pos(centerX, SETUP_RITE_FLOOR_Y, centerZ);
        level.setBlock(floorCenter, centerFloor, 3);
        for (RitualCircleDetector.CircleSize size : RitualCircleDetector.CircleSize.values()) {
            for (BlockPos markPos : RitualCircleDetector.ringPositions(floorCenter, size)) {
                BlockState mark = ChalkCircleBlock.withRandomGlyph(chalkBlock.defaultBlockState(), level.getRandom());
                level.setBlock(markPos, mark, 3);
            }
        }
    }

    private static void clearSetupHouse(ServerLevel level, SetupHouseLayout layout) {
        clearBox(level,
                layout.pos(-5, 0, -4),
                layout.pos(SETUP_CLEAR_PAD_SIZE - 6, SETUP_CLEAR_HEIGHT, SETUP_CLEAR_PAD_SIZE - 5));
        clearBox(level,
                layout.origin().below(SETUP_FOUNDATION_DEPTH + SETUP_SHELL_DROP),
                layout.pos(SETUP_HOUSE_SIZE - 1, SETUP_CLEAR_HEIGHT, SETUP_HOUSE_SIZE - 1));
    }

    private static void buildSetupHouse(ServerLevel level, SetupHouseLayout layout) {
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState planks = WitcheryBlocks.ROWAN_PLANKS.get().defaultBlockState();
        BlockState log = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState();
        int floorY = -SETUP_SHELL_DROP;
        int wallBaseY = floorY + 1;
        int wallTopY = wallBaseY + 3;
        int roofY = floorY + 5;

        for (int x = 0; x < SETUP_HOUSE_SIZE; x++) {
            for (int z = 0; z < SETUP_HOUSE_SIZE; z++) {
                for (int depth = 1; depth <= SETUP_FOUNDATION_DEPTH; depth++) {
                    level.setBlock(layout.pos(x, floorY - depth, z), stone, 3);
                }
                level.setBlock(layout.pos(x, floorY, z), planks, 3);
            }
        }

        for (int y = wallBaseY; y <= wallTopY; y++) {
            for (int x = 0; x < SETUP_HOUSE_SIZE; x++) {
                level.setBlock(layout.pos(x, y, 0), planks, 3);
                level.setBlock(layout.pos(x, y, SETUP_HOUSE_SIZE - 1), planks, 3);
            }
            for (int z = 1; z < SETUP_HOUSE_SIZE - 1; z++) {
                level.setBlock(layout.pos(0, y, z), planks, 3);
                level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, y, z), planks, 3);
            }
        }

        for (int y = wallBaseY; y <= wallTopY; y++) {
            level.setBlock(layout.pos(0, y, 0), log, 3);
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, y, 0), log, 3);
            level.setBlock(layout.pos(0, y, SETUP_HOUSE_SIZE - 1), log, 3);
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, y, SETUP_HOUSE_SIZE - 1), log, 3);
            level.setBlock(layout.pos(4, y, 0), log, 3);
            level.setBlock(layout.pos(6, y, 0), log, 3);
        }

        for (int y = wallBaseY; y <= wallTopY; y++) {
            level.setBlock(layout.pos(0, y, 5), log, 3);
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, y, 5), log, 3);
            level.setBlock(layout.pos(5, y, SETUP_HOUSE_SIZE - 1), log, 3);
        }

        BlockState widthBeam = log.setValue(RotatedPillarBlock.AXIS, layout.width().getAxis());
        BlockState depthBeam = log.setValue(RotatedPillarBlock.AXIS, layout.depth().getAxis());
        for (int x = 1; x < SETUP_HOUSE_SIZE - 1; x++) {
            level.setBlock(layout.pos(x, wallTopY, 0), widthBeam, 3);
            level.setBlock(layout.pos(x, wallTopY, SETUP_HOUSE_SIZE - 1), widthBeam, 3);
        }
        for (int z = 1; z < SETUP_HOUSE_SIZE - 1; z++) {
            level.setBlock(layout.pos(0, wallTopY, z), depthBeam, 3);
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, wallTopY, z), depthBeam, 3);
        }

        placeSetupRoof(level, layout, roofY);

        placeSetupDoor(level, layout);
        placeSetupWindows(level, layout);
        placeSetupLights(level, layout, roofY - 1);
    }

    private static void placeSetupRoof(ServerLevel level, SetupHouseLayout layout, int baseY) {
        BlockState leftSlope = setupRoofStair(layout.width());
        BlockState rightSlope = setupRoofStair(layout.left());
        BlockState gable = WitcheryBlocks.ROWAN_PLANKS.get().defaultBlockState();
        BlockState ridge = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, layout.depth().getAxis());

        for (int z = -1; z <= SETUP_HOUSE_SIZE; z++) {
            for (int step = 0; step < 3; step++) {
                int y = baseY + step;
                int leftX = -1 + step * 2;
                int rightX = SETUP_HOUSE_SIZE - step * 2;
                level.setBlock(layout.pos(leftX, y, z), leftSlope, 3);
                level.setBlock(layout.pos(leftX + 1, y, z), leftSlope, 3);
                level.setBlock(layout.pos(rightX, y, z), rightSlope, 3);
                level.setBlock(layout.pos(rightX - 1, y, z), rightSlope, 3);
            }
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE / 2, baseY + 3, z), ridge, 3);
        }

        for (int z : List.of(0, SETUP_HOUSE_SIZE - 1)) {
            fillSetupGableRow(level, layout, z, baseY, 1, SETUP_HOUSE_SIZE - 2, gable);
            fillSetupGableRow(level, layout, z, baseY + 1, 3, SETUP_HOUSE_SIZE - 4, gable);
            fillSetupGableRow(level, layout, z, baseY + 2, 5, SETUP_HOUSE_SIZE - 6, gable);
        }
    }

    private static void fillSetupGableRow(ServerLevel level, SetupHouseLayout layout, int z, int y,
                                          int fromX, int toX, BlockState state) {
        for (int x = fromX; x <= toX; x++) {
            level.setBlock(layout.pos(x, y, z), state, 3);
        }
    }

    private static BlockState setupRoofStair(Direction facing) {
        return WitcheryBlocks.ROWAN_STAIRS.get().defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static void placeSetupDoor(ServerLevel level, SetupHouseLayout layout) {
        BlockPos lowerPos = layout.pos(5, 1 - SETUP_SHELL_DROP, 0);
        BlockState lower = WitcheryBlocks.ROWAN_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.FACING, layout.frontFacing())
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.POWERED, false)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockState header = WitcheryBlocks.ROWAN_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, layout.width().getAxis());
        level.setBlock(lowerPos, lower, 3);
        level.setBlock(lowerPos.above(), upper, 3);
        level.setBlock(lowerPos.above(2), header, 3);
    }

    private static void placeSetupWindows(ServerLevel level, SetupHouseLayout layout) {
        List<BlockPos> panes = new ArrayList<>();
        for (int y = 2 - SETUP_SHELL_DROP; y <= 3 - SETUP_SHELL_DROP; y++) {
            panes.add(layout.pos(1, y, 0));
            panes.add(layout.pos(2, y, 0));
            panes.add(layout.pos(8, y, 0));
            panes.add(layout.pos(9, y, 0));

            panes.add(layout.pos(1, y, 10));
            panes.add(layout.pos(2, y, 10));
            panes.add(layout.pos(8, y, 10));
            panes.add(layout.pos(9, y, 10));

            panes.add(layout.pos(0, y, 2));
            panes.add(layout.pos(0, y, 3));
            panes.add(layout.pos(0, y, 7));
            panes.add(layout.pos(0, y, 8));

            panes.add(layout.pos(10, y, 2));
            panes.add(layout.pos(10, y, 3));
            panes.add(layout.pos(10, y, 7));
            panes.add(layout.pos(10, y, 8));
        }

        for (BlockPos pos : panes) {
            level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 3);
        }
        for (BlockPos pos : panes) {
            BlockState connectedPane = net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                    Blocks.GLASS_PANE.defaultBlockState(), level, pos);
            level.setBlock(pos, connectedPane, 3);
        }
    }

    private static void placeSetupLights(ServerLevel level, SetupHouseLayout layout, int lightY) {
        placeMachine(level, layout.pos(3, lightY, 3), WitcheryBlocks.GLOWGLOBE.get().defaultBlockState());
        placeMachine(level, layout.pos(7, lightY, 3), WitcheryBlocks.GLOWGLOBE.get().defaultBlockState());
        placeMachine(level, layout.pos(3, lightY, 7), WitcheryBlocks.GLOWGLOBE.get().defaultBlockState());
        placeMachine(level, layout.pos(7, lightY, 7), WitcheryBlocks.GLOWGLOBE.get().defaultBlockState());
    }

    private static void placeSetupContents(ServerLevel level, SetupHouseLayout layout) {
        placeAltarRig(level, layout.pos(1, 0, 7), layout.depth(), layout.width());
        placeKettleRig(level, layout.pos(2, 0, 5), layout.frontFacing());
        placeMachine(level, layout.pos(1, 0, 4), Blocks.WATER_CAULDRON.defaultBlockState());
        placeMachine(level, layout.pos(4, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        placeMachine(level, layout.pos(8, 0, 5),
                WitcheryBlocks.DISTILLERY.get().defaultBlockState().setValue(DistilleryBlock.FACING, layout.left()));
        placeMachine(level, layout.pos(8, 0, 7),
                WitcheryBlocks.WITCHES_OVEN.get().defaultBlockState().setValue(WitchesOvenBlock.FACING, layout.left()));
        placeMachine(level, layout.pos(8, 1, 7),
                WitcheryBlocks.FUME_FUNNEL.get().defaultBlockState().setValue(FumeFunnelBlock.FACING, layout.left()));
        placeMachine(level, layout.pos(8, 0, 2),
                WitcheryBlocks.SPINNING_WHEEL.get().defaultBlockState().setValue(SpinningWheelBlock.FACING, layout.left()));
        placeSupplyChest(level, layout.pos(6, 0, 1), layout.depth(), layout.width());
        placeSetupAltarGarden(level, layout);
    }

    private static void placeAltarRig(ServerLevel level, BlockPos backLeft, Direction depthDirection, Direction widthDirection) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                BlockPos pos = backLeft.relative(depthDirection, row).relative(widthDirection, col);
                placeMachine(level, pos,
                        WitcheryBlocks.ALTAR.get().defaultBlockState().setValue(AltarBlock.AXIS, widthDirection.getAxis()));
            }
        }

        placeMachine(level, backLeft.above(), WitcheryBlocks.CANDELABRA.get().defaultBlockState());
        placeMachine(level, backLeft.relative(widthDirection).above(),
                WitcheryBlocks.CHALICE.get().defaultBlockState().setValue(ChaliceBlock.FILLED, true));
        placeMachine(level, backLeft.relative(widthDirection, 2).above(), Blocks.SKELETON_SKULL.defaultBlockState());
        placeAltarItem(level, backLeft.relative(depthDirection), depthDirection.getOpposite(), WitcheryItems.ARTHANA.get());
        placeAltarItem(level, backLeft.relative(depthDirection).relative(widthDirection),
                depthDirection.getOpposite(), WitcheryItems.PENTACLE.get());
        placeAltarItem(level, backLeft.relative(depthDirection).relative(widthDirection, 2),
                depthDirection.getOpposite(), WitcheryItems.MYSTIC_BRANCH.get());
    }

    private static void placeAltarItem(ServerLevel level, BlockPos altarPos, Direction facing, Item item) {
        BlockPos itemPos = altarPos.above();
        level.setBlock(itemPos, WitcheryBlocks.PLACED_ITEM.get().defaultBlockState()
                .setValue(PlacedItemBlock.FACING, facing), 3);
        if (level.getBlockEntity(itemPos) instanceof PlacedItemBlockEntity placedItem) {
            placedItem.setStack(new ItemStack(item));
            level.sendBlockUpdated(itemPos, level.getBlockState(itemPos), level.getBlockState(itemPos), 3);
        }
        if (level.getBlockEntity(altarPos) instanceof AltarBlockEntity altar) {
            altar.invalidate(level);
        }
    }

    private static void placeSetupAltarGarden(ServerLevel level, SetupHouseLayout layout) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        placeGardenPost(level, layout, -2, 2,
                WitcheryBlocks.ROWAN_LOG.get().defaultBlockState(),
                WitcheryBlocks.ROWAN_LEAVES.get().defaultBlockState());
        placeGardenPost(level, layout, -2, 8,
                WitcheryBlocks.ALDER_LOG.get().defaultBlockState(),
                WitcheryBlocks.ALDER_LEAVES.get().defaultBlockState());
        placeGardenPost(level, layout, SETUP_HOUSE_SIZE + 1, 2,
                WitcheryBlocks.HAWTHORN_LOG.get().defaultBlockState(),
                WitcheryBlocks.HAWTHORN_LEAVES.get().defaultBlockState());
        placeGardenPost(level, layout, SETUP_HOUSE_SIZE + 1, 8,
                WitcheryBlocks.ROWAN_LOG.get().defaultBlockState(),
                WitcheryBlocks.ROWAN_LEAVES.get().defaultBlockState());

        placeGardenPlant(level, layout, 2, SETUP_HOUSE_SIZE + 2, grass,
                aged(WitcheryBlocks.BELLADONNA.get().defaultBlockState(), BlockStateProperties.AGE_4, 4));
        placeGardenPlant(level, layout, 3, SETUP_HOUSE_SIZE + 2, grass,
                aged(WitcheryBlocks.MANDRAKE.get().defaultBlockState(), BlockStateProperties.AGE_4, 4));
        placeGardenPlant(level, layout, 4, SETUP_HOUSE_SIZE + 2, grass,
                aged(WitcheryBlocks.ARTICHOKE.get().defaultBlockState(), BlockStateProperties.AGE_4, 4));
        placeGardenPlant(level, layout, 6, SETUP_HOUSE_SIZE + 2, Blocks.FARMLAND.defaultBlockState(),
                aged(WitcheryBlocks.SNOWBELL.get().defaultBlockState(), BlockStateProperties.AGE_4, 4));
        placeGardenPlant(level, layout, 7, SETUP_HOUSE_SIZE + 2, Blocks.FARMLAND.defaultBlockState(),
                aged(WitcheryBlocks.WORMWOOD_PLANT.get().defaultBlockState(), BlockStateProperties.AGE_4, 4));
        placeGardenPlant(level, layout, 8, SETUP_HOUSE_SIZE + 2, grass,
                aged(WitcheryBlocks.GARLIC_CROP.get().defaultBlockState(), BlockStateProperties.AGE_5, 5));

        placeGardenPlant(level, layout, 1, -2, grass, WitcheryBlocks.ROWAN_SAPLING.get().defaultBlockState());
        placeGardenPlant(level, layout, 3, -2, grass, WitcheryBlocks.ALDER_SAPLING.get().defaultBlockState());
        placeGardenPlant(level, layout, 7, -2, grass, WitcheryBlocks.HAWTHORN_SAPLING.get().defaultBlockState());
        placeGardenPlant(level, layout, 9, -2, grass, WitcheryBlocks.BLOOD_ROSE.get().defaultBlockState());
    }

    private static void placeGardenPost(ServerLevel level, SetupHouseLayout layout, int x, int z,
                                        BlockState log, BlockState leaves) {
        level.setBlock(layout.pos(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(layout.pos(x, 0, z), log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 3);
        level.setBlock(layout.pos(x, 1, z), persistentLeaves(leaves), 3);
    }

    private static void placeGardenPlant(ServerLevel level, SetupHouseLayout layout, int x, int z,
                                         BlockState support, BlockState plant) {
        level.setBlock(layout.pos(x, -1, z), support, 3);
        level.setBlock(layout.pos(x, 0, z), plant, 3);
    }

    private static BlockState persistentLeaves(BlockState leaves) {
        return leaves.hasProperty(LeavesBlock.PERSISTENT)
                ? leaves.setValue(LeavesBlock.PERSISTENT, true)
                : leaves;
    }

    private static BlockState aged(BlockState state, IntegerProperty property, int age) {
        return state.hasProperty(property) ? state.setValue(property, age) : state;
    }

    private static void placeKettleRig(ServerLevel level, BlockPos kettlePos, Direction machineFacing) {
        Direction left = machineFacing.getClockWise();
        Direction right = machineFacing.getCounterClockWise();
        placeMachine(level, kettlePos.below(), Blocks.CAMPFIRE.defaultBlockState());
        placeMachine(level, kettlePos.relative(left), Blocks.COBBLESTONE.defaultBlockState());
        placeMachine(level, kettlePos.relative(right), Blocks.COBBLESTONE.defaultBlockState());
        placeMachine(level, kettlePos,
                WitcheryBlocks.KETTLE.get().defaultBlockState().setValue(KettleBlock.FACING, machineFacing));
    }

    private static void placeSupplyChest(ServerLevel level, BlockPos firstPos, Direction chestFacing, Direction stripDirection) {
        BlockPos secondPos = firstPos.relative(stripDirection);
        ChestType firstType = stripDirection == chestFacing.getCounterClockWise() ? ChestType.RIGHT : ChestType.LEFT;
        ChestType secondType = firstType == ChestType.RIGHT ? ChestType.LEFT : ChestType.RIGHT;

        level.setBlock(firstPos, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, chestFacing)
                .setValue(ChestBlock.TYPE, firstType), 3);
        level.setBlock(secondPos, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, chestFacing)
                .setValue(ChestBlock.TYPE, secondType), 3);

        List<ItemStack> supplies = createSetupChestSupplies();
        fillChestHalf(level, firstPos, supplies, 0, 27);
        fillChestHalf(level, secondPos, supplies, 27, 27);
    }

    private static void fillChestHalf(ServerLevel level, BlockPos pos, List<ItemStack> supplies, int startIndex, int slotCount) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            return;
        }

        for (int slot = 0; slot < slotCount; slot++) {
            int supplyIndex = startIndex + slot;
            chest.setItem(slot, supplyIndex < supplies.size() ? supplies.get(supplyIndex).copy() : ItemStack.EMPTY);
        }
        chest.setChanged();
    }

    private static List<ItemStack> createSetupChestSupplies() {
        List<ItemStack> supplies = new ArrayList<>();

        supplies.add(stack(Items.GLASS_BOTTLE, 64));
        supplies.add(stack(Items.WATER_BUCKET, 1));
        supplies.add(stack(Items.BUCKET, 1));
        supplies.add(stack(Items.EMERALD, 32));
        supplies.add(stack(Items.GOLD_NUGGET, 64));
        supplies.add(stack(Items.GOLD_INGOT, 32));
        supplies.add(stack(Items.COAL, 64));
        supplies.add(stack(Items.CLAY_BALL, 64));
        supplies.add(stack(WitcheryItems.CLAY_JAR_SOFT.get(), 64));
        supplies.add(stack(WitcheryItems.CLAY_JAR.get(), 64));
        supplies.add(stack(WitcheryItems.GLASS_GOBLET.get(), 16));
        supplies.add(stack(WitcheryItems.ARTHANA.get(), 1));
        supplies.add(stack(WitcheryItems.BOLINE.get(), 1));
        supplies.add(stack(WitcheryItems.PENTACLE.get(), 1));
        supplies.add(stack(WitcheryItems.MYSTIC_BRANCH.get(), 1));
        supplies.add(stack(WitcheryItems.CHALK.get(), 4));
        supplies.add(stack(WitcheryItems.CHALK_GOLDEN.get(), 4));
        supplies.add(stack(WitcheryItems.CHALK_OTHERWHERE.get(), 4));
        supplies.add(stack(WitcheryItems.CHALK_INFERNAL.get(), 4));
        supplies.add(stack(WitcheryItems.TAGLOCK_KIT.get(), 16));

        supplies.add(stack(WitcheryItems.HAWTHORN_SAPLING.get(), 64));
        supplies.add(stack(WitcheryItems.ALDER_SAPLING.get(), 64));
        supplies.add(stack(WitcheryItems.ROWAN_SAPLING.get(), 64));
        supplies.add(stack(WitcheryItems.WOOD_ASH.get(), 64));
        supplies.add(stack(WitcheryItems.FOUL_FUME.get(), 64));
        supplies.add(stack(WitcheryItems.ODOUR_OF_PURITY.get(), 64));
        supplies.add(stack(WitcheryItems.QUICKLIME.get(), 64));
        supplies.add(stack(WitcheryItems.GYPSUM.get(), 64));
        supplies.add(stack(WitcheryItems.OIL_OF_VITRIOL.get(), 64));
        supplies.add(stack(WitcheryItems.REEK_OF_MISFORTUNE.get(), 64));

        supplies.add(stack(WitcheryItems.WISPY_COTTON.get(), 64));
        supplies.add(stack(Items.STRING, 64));
        supplies.add(stack(WitcheryItems.FANCIFUL_THREAD.get(), 64));
        supplies.add(stack(WitcheryItems.BAT_WOOL.get(), 64));
        supplies.add(stack(WitcheryItems.SPANISH_MOSS.get(), 64));
        supplies.add(stack(WitcheryItems.GOLDEN_THREAD.get(), 64));
        supplies.add(stack(WitcheryItems.DIAMOND_VAPOUR.get(), 32));

        supplies.add(stack(WitcheryItems.BELLADONNA.get(), 64));
        supplies.add(stack(WitcheryItems.MANDRAKE_ROOT.get(), 64));
        supplies.add(stack(WitcheryItems.ARTICHOKE.get(), 64));
        supplies.add(stack(WitcheryItems.SNOWBELL.get(), 64));
        supplies.add(stack(WitcheryItems.WORMWOOD.get(), 64));
        supplies.add(stack(WitcheryItems.ROWAN_BERRIES.get(), 64));
        supplies.add(stack(Items.LILY_PAD, 64));
        supplies.add(stack(Items.INK_SAC, 64));
        supplies.add(stack(Items.REDSTONE, 64));
        supplies.add(stack(Items.GLOWSTONE_DUST, 64));

        supplies.add(stack(WitcheryItems.BREATH_OF_GODDESS.get(), 64));
        supplies.add(stack(WitcheryItems.HINT_OF_REBIRTH.get(), 64));
        supplies.add(stack(WitcheryItems.WHIFF_OF_MAGIC.get(), 64));
        supplies.add(stack(WitcheryItems.MUTANDIS.get(), 32));
        supplies.add(stack(WitcheryItems.MUTANDIS_EXTREMIS.get(), 16));
        supplies.add(stack(Items.FLINT_AND_STEEL, 1));

        return supplies;
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static void fillSetupStations(ServerLevel level, SetupHouseLayout layout) {
        BlockPos ovenPos = layout.pos(8, 0, 7);
        BlockPos distilleryPos = layout.pos(8, 0, 5);
        BlockPos wheelPos = layout.pos(8, 0, 2);

        if (level.getBlockEntity(ovenPos) instanceof com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity oven) {
            oven.setItem(com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity.SLOT_INPUT,
                    stack(WitcheryItems.HAWTHORN_SAPLING.get(), 1));
            oven.setItem(com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity.SLOT_FUEL,
                    stack(Items.COAL, 1));
            oven.setItem(com.emoniph.witchery.blocks.entity.WitchesOvenBlockEntity.SLOT_JARS,
                    stack(WitcheryItems.CLAY_JAR.get(), 1));
            oven.setChanged();
            level.sendBlockUpdated(ovenPos, oven.getBlockState(), oven.getBlockState(), 3);
        }

        if (level.getBlockEntity(distilleryPos) instanceof com.emoniph.witchery.blocks.entity.DistilleryBlockEntity distillery) {
            distillery.setItem(com.emoniph.witchery.blocks.entity.DistilleryBlockEntity.SLOT_INPUT_1,
                    stack(WitcheryItems.FOUL_FUME.get(), 1));
            distillery.setItem(com.emoniph.witchery.blocks.entity.DistilleryBlockEntity.SLOT_INPUT_2,
                    stack(WitcheryItems.QUICKLIME.get(), 1));
            distillery.setItem(com.emoniph.witchery.blocks.entity.DistilleryBlockEntity.SLOT_JARS,
                    stack(WitcheryItems.CLAY_JAR.get(), 1));
            distillery.setChanged();
            level.sendBlockUpdated(distilleryPos, distillery.getBlockState(), distillery.getBlockState(), 3);
        }

        if (level.getBlockEntity(wheelPos) instanceof com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity wheel) {
            wheel.setItem(com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity.SLOT_FIBRE,
                    stack(WitcheryItems.WISPY_COTTON.get(), 4));
            wheel.setItem(com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity.SLOT_MODIFIER_1,
                    stack(Items.STRING, 1));
            wheel.setItem(com.emoniph.witchery.blocks.entity.SpinningWheelBlockEntity.SLOT_MODIFIER_2,
                    stack(WitcheryItems.ODOUR_OF_PURITY.get(), 1));
            wheel.setChanged();
            level.sendBlockUpdated(wheelPos, wheel.getBlockState(), wheel.getBlockState(), 3);
        }
    }

    private static void placeSetupTraders(ServerLevel level, SetupHouseLayout layout) {
        BlockPos max = layout.pos(SETUP_HOUSE_SIZE - 1, 3, SETUP_HOUSE_SIZE - 1);
        net.minecraft.world.phys.AABB interior = new net.minecraft.world.phys.AABB(
                layout.origin().getX(), layout.origin().getY() - SETUP_SHELL_DROP, layout.origin().getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);

        level.getEntitiesOfClass(com.emoniph.witchery.entity.CovenWitchEntity.class, interior)
                .forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(com.emoniph.witchery.entity.HobgoblinEntity.class, interior)
                .forEach(net.minecraft.world.entity.Entity::discard);

        spawnSetupMob(level, WitcheryEntities.COVEN_WITCH.get(), layout.pos(4, 0, 4), 45.0F);
        spawnSetupMob(level, WitcheryEntities.HOBGOBLIN.get(), layout.pos(6, 0, 4), -45.0F);
    }

    private static <T extends Mob> void spawnSetupMob(ServerLevel level, EntityType<T> type, BlockPos pos, float yaw) {
        T entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return;
        }
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setYRot(yaw);
        entity.setXRot(0.0F);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.COMMAND, null);
        entity.setPersistenceRequired();
        level.addFreshEntityWithPassengers(entity);
    }

    private static void placeMachine(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 3);
    }

    private static void clearBox(ServerLevel level, BlockPos min, BlockPos max) {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private static int spawnWickerman(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;

        Direction facing = player.getDirection();
        BlockPos base = player.blockPosition().relative(facing, 5);
        WickerBundleBlock.buildBloodiedWickerman(sl, base, facing);

        src.sendSuccess(() -> Component.literal(
                "Spawned a bloodied wickerman at " + base.getX() + ", " + base.getY() + ", " + base.getZ()
                        + ". Light any bloodied bundle with flint and steel to summon the Horned Huntsman."),
                false);
        return 1;
    }

    // ── walls command ────────────────────────────────────────────────────────

    private static int buildWalls(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();

        // Manual walls should operate on an already generated nearby village.
        // Vanilla locate can trigger the same synchronous scan that made chained
        // village goto commands freeze.
        net.minecraft.world.level.levelgen.structure.StructureStart start =
                com.emoniph.witchery.events.VillageWallEvents.findVillageStartNear(level, player.blockPosition());

        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            src.sendFailure(Component.literal(
                    "[VillageWalls] No generated village start found nearby. Stand in or near an explored village."));
            return 0;
        }

        net.minecraft.world.level.levelgen.structure.BoundingBox bb = start.getBoundingBox();
        net.minecraft.core.BlockPos found = new net.minecraft.core.BlockPos(
                (bb.minX() + bb.maxX()) / 2, 64, (bb.minZ() + bb.maxZ()) / 2);
        int pieces = start.getPieces().size();
        boolean alreadyProcessed = com.emoniph.witchery.world.VillageWallData.get(level)
                .isProcessed(start.getBoundingBox());
        boolean queued = com.emoniph.witchery.events.VillageWallEvents
                .queueGeneration(level, start, false, "command");
        if (!queued) {
            src.sendFailure(Component.literal("[VillageWalls] Could not queue wall generation for that village."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(
                alreadyProcessed
                ? "[VillageWalls] That village is already marked walled. Run /witchery walls reset if you need to re-run."
                : "[VillageWalls] Wall generation queued for village at " + found.getX() + " ~ " + found.getZ()
                + " (" + pieces + " pieces). Run /witchery walls reset if you need to re-run."), false);
        return 1;
    }

    private static int resetWalls(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel level = src.getLevel();
        // Clear the processed set so villages can be re-walled.
        com.emoniph.witchery.world.VillageWallData.get(level).reset();
        src.sendSuccess(() -> Component.literal(
                "[VillageWalls] Processed-village set cleared. All villages will be re-walled on next scan."),
                false);
        return 1;
    }

    // ── goto structure ────────────────────────────────────────────────────────

    private static int gotoStructure(CommandSourceStack src, String structureId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return gotoStructure(src, "witchery", structureId);
    }

    // Per-player cooldown so /witchery goto can't be spammed (each teleport triggers heavy
    // village wall generation + chunk loading; rapid repeats can overload/freeze the game).
    private static final java.util.Map<java.util.UUID, Long> GOTO_COOLDOWN = new java.util.HashMap<>();
    private static final long GOTO_COOLDOWN_TICKS = 600L; // 30 seconds
    private static final int GOTO_SEARCH_RADIUS_RINGS = 32;
    private static final int GOTO_VILLAGE_SEARCH_RADIUS_RINGS = 8;
    private static final boolean GOTO_SKIP_REFERENCED_STRUCTURES = false;
    private static final int GOTO_PRELOAD_RADIUS_CHUNKS = 0; // arrival chunk only

    private static int gotoVillageBuilding(CommandSourceStack src, String building)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();

        long now = level.getGameTime();
        Long last = GOTO_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < GOTO_COOLDOWN_TICKS) {
            long remaining = (GOTO_COOLDOWN_TICKS - (now - last) + 19) / 20;
            src.sendFailure(Component.literal(
                    "The leylines are still tangled from your last crossing. Rest " + remaining
                    + "s before you walk the hidden paths again.").withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        BlockPos from = player.blockPosition();
        WitcheryMod.LOGGER.info("[Goto] Queueing safe village-building search for {} from {} (radius={}, one candidate chunk per tick).",
                building, from, GOTO_VILLAGE_SEARCH_RADIUS_RINGS);
        boolean queued = com.emoniph.witchery.events.VillageWallEvents.queueVillageBuildingGotoLocate(
                level, player, from, building, GOTO_VILLAGE_SEARCH_RADIUS_RINGS);
        if (!queued) {
            src.sendFailure(Component.literal(
                    "The hidden paths could not prepare a safe search for a village " + building + ".")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        GOTO_COOLDOWN.put(player.getUUID(), now);
        src.sendSuccess(() -> Component.literal(
                "The hidden paths begin searching village omens for a " + building + ".")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int gotoMeadowVillage(CommandSourceStack src)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();

        long now = level.getGameTime();
        Long last = GOTO_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < GOTO_COOLDOWN_TICKS) {
            long remaining = (GOTO_COOLDOWN_TICKS - (now - last) + 19) / 20;
            src.sendFailure(Component.literal(
                    "The leylines are still tangled from your last crossing. Rest " + remaining
                    + "s before you walk the hidden paths again.").withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key =
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                        Identifier.withDefaultNamespace("village_plains"));

        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.level.levelgen.structure.Structure>> holderOpt =
                level.registryAccess()
                        .lookup(Registries.STRUCTURE)
                        .flatMap(reg -> reg.get(key));

        if (holderOpt.isEmpty()) {
            src.sendFailure(Component.literal("The spirits know of no such place: minecraft:village_plains")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        final BlockPos from = player.blockPosition();
        WitcheryMod.LOGGER.info("[Goto] Queueing safe meadow village search via minecraft:village_plains from {} (radius={}, one candidate chunk per tick).",
                from, GOTO_VILLAGE_SEARCH_RADIUS_RINGS);
        boolean queued = com.emoniph.witchery.events.VillageWallEvents.queueVillageGotoLocate(
                level, player, holderOpt.get(), from, "minecraft", "village_meadow",
                GOTO_VILLAGE_SEARCH_RADIUS_RINGS, net.minecraft.world.level.biome.Biomes.MEADOW);
        if (!queued) {
            src.sendFailure(Component.literal(
                    "The hidden paths could not prepare a safe Meadow village search.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }
        GOTO_COOLDOWN.put(player.getUUID(), now);
        src.sendSuccess(() -> Component.literal(
                "The hidden paths begin a slower search for a Meadow village.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int gotoStructure(CommandSourceStack src, String namespace, String structureId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();

        long now = level.getGameTime();
        Long last = GOTO_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < GOTO_COOLDOWN_TICKS) {
            long remaining = (GOTO_COOLDOWN_TICKS - (now - last) + 19) / 20; // round up to seconds
            src.sendFailure(Component.literal(
                    "The leylines are still tangled from your last crossing. Rest " + remaining
                    + "s before you walk the hidden paths again.").withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key =
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                        Identifier.fromNamespaceAndPath(namespace, structureId));

        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.level.levelgen.structure.Structure>> holderOpt =
                level.registryAccess()
                        .lookup(Registries.STRUCTURE)
                        .flatMap(reg -> reg.get(key));

        if (holderOpt.isEmpty()) {
            src.sendFailure(Component.literal("The spirits know of no such place: " + namespace + ":" + structureId)
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        final BlockPos from = player.blockPosition();
        if (com.emoniph.witchery.events.VillageWallEvents.isVillageId(namespace, structureId)) {
            WitcheryMod.LOGGER.info("[Goto] Queueing safe village search for {}:{} from {} (radius={}, one candidate chunk per tick).",
                    namespace, structureId, from, GOTO_VILLAGE_SEARCH_RADIUS_RINGS);
            boolean queued = com.emoniph.witchery.events.VillageWallEvents.queueVillageGotoLocate(
                    level, player, holderOpt.get(), from, namespace, structureId, GOTO_VILLAGE_SEARCH_RADIUS_RINGS);
            if (!queued) {
                src.sendFailure(Component.literal(
                        "The hidden paths could not prepare a safe village search for that structure.")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                return 0;
            }
            GOTO_COOLDOWN.put(player.getUUID(), now);
            src.sendSuccess(() -> Component.literal(
                    "The hidden paths begin a slower search, one village omen at a time.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return 1;
        }

        // Non-village Witchery structures still use vanilla locate semantics.
        // Village gotos use the ticked safe-locate path above because biome-specific
        // vanilla village locate can monopolize the server thread when chained.
        final net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet =
                net.minecraft.core.HolderSet.direct(holderOpt.get());
        int searchRadius = GOTO_SEARCH_RADIUS_RINGS;

        WitcheryMod.LOGGER.info("[Goto] Searching for {}:{} from {} (radius={}, skipReferenced={}).",
                namespace, structureId, from, searchRadius, GOTO_SKIP_REFERENCED_STRUCTURES);
        com.mojang.datafixers.util.Pair<BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> result =
                level.getChunkSource().getGenerator()
                        .findNearestMapStructure(level, holderSet, from, searchRadius,
                                GOTO_SKIP_REFERENCED_STRUCTURES);

        String fullId = namespace + ":" + structureId;
        if (result == null) {
            src.sendFailure(Component.literal(
                    "The scrying mists reveal nothing nearby within the safe range. Wander further and gaze again.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return 0;
        }

        BlockPos found = result.getFirst();
        // Force only the arrival chunk into memory so the heightmap is accurate.
        // Loading the old 5x5 area synchronously could full-generate 25 chunks
        // before teleport and make the integrated server appear frozen.
        int preloaded = preloadGotoDestination(level, found);
        WitcheryMod.LOGGER.info("[Goto] Prepared {} chunk(s) around {}:{} at {} before teleport.",
                preloaded, namespace, structureId, found);

        int surfaceY = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                found.getX(), found.getZ());
        // Final safety net: if heightmap is still suspicious, use structure Y + 3.
        // found.getY() is the village center Y (ground level at generation time).
        if (surfaceY < 20) {
            surfaceY = Math.max(found.getY() + 3, 20);
        }

        player.teleportTo(level, found.getX() + 0.5, surfaceY, found.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;
        GOTO_COOLDOWN.put(player.getUUID(), now);

        int dist = (int) Math.sqrt(found.distSqr(from));
        src.sendSuccess(() -> Component.literal(
                "The hidden paths carry you away, across " + dist + " blocks, to "
                + found.getX() + " ~ " + found.getZ() + ".").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int preloadGotoDestination(ServerLevel level, BlockPos found) {
        ChunkPos center = new ChunkPos(found);
        int count = 0;
        for (int dx = -GOTO_PRELOAD_RADIUS_CHUNKS; dx <= GOTO_PRELOAD_RADIUS_CHUNKS; dx++) {
            for (int dz = -GOTO_PRELOAD_RADIUS_CHUNKS; dz <= GOTO_PRELOAD_RADIUS_CHUNKS; dz++) {
                level.getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, true);
                count++;
            }
        }
        return count;
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }
}
