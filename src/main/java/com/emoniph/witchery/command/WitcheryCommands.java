package com.emoniph.witchery.command;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
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
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

                // ── Spawn ─────────────────────────────────────────────────────
                .then(Commands.literal("spawn")
                        .then(Commands.literal("owl")           .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.OWL.get())))
                        .then(Commands.literal("familiar_cat")  .executes(ctx -> spawnEntity(ctx.getSource(), WitcheryEntities.FAMILIAR_CAT.get())))
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
        d.setInfusionType(type);
        sync(p, d);
        src.sendSuccess(() -> Component.literal("Infusion set to " + type + "."), false);
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
    private static double[] findSafeEntry(ServerLevel level, ResourceKey<Level> dim) {
        // Force full chunk generation at spawn point (synchronous on server thread)
        level.getChunk(0, 0, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);

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

        double[] safePos = findSafeEntry(target, dim);
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

    // ── Shared ────────────────────────────────────────────────────────────────

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }
}
