package com.emoniph.witchery.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.DistilleryBlock;
import com.emoniph.witchery.blocks.FumeFunnelBlock;
import com.emoniph.witchery.blocks.KettleBlock;
import com.emoniph.witchery.blocks.SpinningWheelBlock;
import com.emoniph.witchery.blocks.TormentPortalBlock;
import com.emoniph.witchery.blocks.WickerBundleBlock;
import com.emoniph.witchery.blocks.WitchesOvenBlock;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import com.emoniph.witchery.world.TormentHelper;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

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
 * /witchery familiar <type>         — set familiar type (owl/cat/toad/none)
 * /witchery fullmoon                — advance time to next full-moon midnight
 * /witchery clear                   — wipe all witchery state from self
 * /witchery status                  — show own witchery data
 * /witchery spawn <entity>          — spawn entity 2 blocks ahead
 * /witchery dream                   — enter the Spirit World dimension
 * /witchery mirror                  — enter the Mirror World dimension
 * /witchery torment                 — enter the Torment dimension
 * /witchery exit                    — return to previous dimension
 * /witchery give primer             — give the Witches' Primer guidebook
 * /witchery altar                   — show altar power at current location
 */
@EventBusSubscriber
public class WitcheryCommands {

    private static final List<String> INFUSION_TYPES =
            List.of("NONE", "LIGHT", "OVERWORLD", "OTHERWHERE", "INFERNAL");
    private static final List<String> FAMILIAR_TYPES =
            List.of("none", "owl", "cat", "toad");
    private static final int SETUP_HOUSE_SIZE = 11;
    private static final int SETUP_FOUNDATION_DEPTH = 3;
    private static final int SETUP_CLEAR_HEIGHT = 7;
    private static final int SETUP_FORWARD_OFFSET = 5;
    private static final int SETUP_CLEAR_PAD_SIZE = 20;
    private static final int SETUP_SHELL_DROP = 1;

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
                        .executes(ctx -> enterDimension(ctx.getSource(), WitcheryDimensions.DREAM_WORLD)))

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
                                .executes(ctx -> verifyTorment(ctx.getSource()))))

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
                                .executes(ctx -> setupStations(ctx.getSource()))))
                .then(Commands.literal("ritual")
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
                        .then(Commands.literal("coven_witch")   .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.COVEN_WITCH.get())))
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
                        .then(Commands.literal("death")          .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.DEATH.get()))))
        );
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
        if (type.equalsIgnoreCase("none")) {
            d.clearFamiliar();
            sync(p, d);
            src.sendSuccess(() -> Component.literal("Familiar cleared."), false);
        } else {
            src.sendSuccess(() -> Component.literal(
                    "Use the Rite of Familiar Binding in-game to bind a real familiar. "
                    + "This command only supports clearing (type=none)."), false);
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
        src.sendSuccess(() -> Component.literal(
                "[Witchery] " + p.getName().getString() + "\n"
                + "  vampire=" + d.isVampire() + " blood=" + d.getBloodLevel() + "/500\n"
                + "  werewolf=" + d.isWerewolf()
                + " transform=" + String.format("%.2f", d.getTransformationLevel())
                + " wolfmastery=" + d.getWolfMastery() + "/3\n"
                + "  infusion=" + d.getInfusionType()
                + " energy=" + d.getInfusionEnergy() + "/" + d.getInfusionMaxEnergy()
                + " soaring=" + d.getSoaringTicks() + "t\n"
                + "  familiar=" + d.getFamiliarUUID().map(u -> u.toString().substring(0, 8) + "...").orElse("none") + "\n"
                + "  spiritWorld=" + d.isInSpiritWorld()
                + (d.isInSpiritWorld() ? " (" + (d.isSpiritWorldDream() ? "dream" : "nightmare") + ")" : "") + "\n"
                + "  lastDeath=" + d.getLastDeathPos().map(Object::toString).orElse("none")), false);
        return 1;
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
            int x = referencePos.getX();
            int z = referencePos.getZ();
            for (int y = 200; y >= -60; y--) {
                var floor = new net.minecraft.core.BlockPos(x, y, z);
                if (!level.getBlockState(floor).isAir()
                        && level.getBlockState(floor.above()).isAir()
                        && level.getBlockState(floor.above().above()).isAir()) {
                    return new double[]{ x + 0.5, y + 1, z + 0.5 };
                }
            }
            return new double[]{ x + 0.5, 65, z + 0.5 };
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

    private static <T extends Mob> int spawnEntity(CommandSourceStack src, EntityType<T> type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) return 0;
        T entity = type.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (entity == null) return 0;
        var pos = player.blockPosition().relative(player.getDirection(), 2);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
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

    /** Toggle dream/nightmare mode. Works in Spirit World to switch modes; outside SW pre-sets for next entry. */
    private static int toggleNightmare(CommandSourceStack src, boolean nightmare)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        WitcheryPlayerData d = WitcheryAttachments.get(p);
        d.setSpiritWorldIsDream(!nightmare);
        sync(p, d);
        String mode = nightmare ? "nightmare" : "dream";
        src.sendSuccess(() -> Component.literal("Spirit World mode set to " + mode + "."), false);
        if (d.isInSpiritWorld()) {
            // Apply fog change immediately by syncing dimension state
            src.sendSuccess(() -> Component.literal("Fog updated — may need relog to see colour change."), false);
        }
        return 1;
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
        verifyStateAt(torment, getTormentCenterChestPos(level),
                state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                "Level " + level + " center chest room lost its Refilling Chest.", failures);
        verifyStateAt(torment, getTormentRightChestPos(seed, level),
                state -> state.is(WitcheryBlocks.REFILLING_CHEST.get()),
                "Level " + level + " right chest room lost its Refilling Chest.", failures);

        if (tormentStoneCount > 0) {
            failures.add("Level " + level + " still contains " + tormentStoneCount + " Torment Stone block(s) inside the legacy maze footprint.");
        }
        if (totalChestCount != 3) {
            failures.add("Level " + level + " should have exactly 3 floor-level Refilling Chests, but found " + totalChestCount + ".");
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
        var resourceId = Identifier.fromNamespaceAndPath("witchery", path);
        var resource = torment.getServer().getResourceManager().getResource(resourceId).orElse(null);
        if (resource == null) {
            failures.add("Missing Torment data resource " + resourceId + ".");
            return null;
        }

        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | IllegalStateException ex) {
            failures.add("Could not read Torment data resource " + resourceId + ": " + ex.getMessage());
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
                "Cleared a 20x20 pad in front and placed a sunken Rowan Witchery QA hut with player-height stations, stocked chest, and indoor traders."),
                false);
        return 1;
    }

    private static SetupHouseLayout createSetupHouseLayout(ServerPlayer player) {
        Direction depth = player.getDirection();
        Direction width = depth.getClockWise();
        BlockPos origin = player.blockPosition()
                .relative(depth, SETUP_FORWARD_OFFSET)
                .relative(width.getOpposite(), SETUP_HOUSE_SIZE / 2);
        return new SetupHouseLayout(origin, depth, width);
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
        BlockState slab = WitcheryBlocks.ROWAN_SLAB.get().defaultBlockState();
        int floorY = -SETUP_SHELL_DROP;
        int wallBaseY = floorY + 1;
        int wallTopY = wallBaseY + 3;
        int roofY = floorY + 5;
        int roofTrimY = floorY + 6;

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

        for (int x = 0; x < SETUP_HOUSE_SIZE; x++) {
            for (int z = 0; z < SETUP_HOUSE_SIZE; z++) {
                level.setBlock(layout.pos(x, roofY, z), planks, 3);
            }
        }

        for (int edge = 0; edge < SETUP_HOUSE_SIZE; edge++) {
            level.setBlock(layout.pos(edge, roofTrimY, 0), slab, 3);
            level.setBlock(layout.pos(edge, roofTrimY, SETUP_HOUSE_SIZE - 1), slab, 3);
            level.setBlock(layout.pos(0, roofTrimY, edge), slab, 3);
            level.setBlock(layout.pos(SETUP_HOUSE_SIZE - 1, roofTrimY, edge), slab, 3);
        }

        placeSetupDoor(level, layout);
        placeSetupWindows(level, layout);
        placeSetupLights(level, layout, roofY - 1);
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
        supplies.add(stack(Items.CHARCOAL, 64));
        supplies.add(stack(Items.CLAY_BALL, 64));
        supplies.add(stack(WitcheryItems.CLAY_JAR_SOFT.get(), 64));
        supplies.add(stack(WitcheryItems.CLAY_JAR.get(), 64));

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

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }
}
