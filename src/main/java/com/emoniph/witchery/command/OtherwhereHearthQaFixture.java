package com.emoniph.witchery.command;

import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.KettleBlock;
import com.emoniph.witchery.blocks.PlacedItemBlock;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.emoniph.witchery.blocks.entity.PlacedItemBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.ritual.OtherwhereHearthTransposition;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Builds a deliberately disposable, same-coordinate two-world Hearth recovery fixture. */
final class OtherwhereHearthQaFixture {
    private static final int FORWARD_OFFSET = 20;
    private static final int WALL_RADIUS = 11;

    private OtherwhereHearthQaFixture() {
    }

    static int setup(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sourceLevel)
                || !OtherwhereHearthTransposition.isSupportedDimension(sourceLevel)) {
            source.sendFailure(Component.literal(
                    "Otherwhere Hearth QA setup must begin in the Overworld or Spirit World."));
            return 0;
        }
        if (OtherwhereHearthTransposition.hasPendingTransaction(source.getServer())) {
            source.sendFailure(Component.literal(
                    "A persisted Hearth transaction is pending. Let recovery finish before replacing its worlds."));
            return 0;
        }

        ServerLevel spirit = source.getServer().getLevel(WitcheryDimensions.SPIRIT_WORLD);
        if (spirit == null) {
            source.sendFailure(Component.literal("The Spirit World is unavailable."));
            return 0;
        }
        ServerLevel destination = sourceLevel.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                ? source.getServer().overworld() : spirit;

        Direction facing = player.getDirection();
        BlockPos kettlePos = player.blockPosition().relative(facing, FORWARD_OFFSET);
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(kettlePos);
        if (!OtherwhereHearthTransposition.volumeFits(sourceLevel, volume)
                || !OtherwhereHearthTransposition.volumeFits(destination, volume)) {
            source.sendFailure(Component.literal(
                    "The paired 25x25x25 fixture would cross build height or a world border. Move and retry."));
            return 0;
        }

        List<ServerPlayer> sourcePlayers = playersIn(sourceLevel, volume);
        List<ServerPlayer> destinationPlayers = playersIn(destination, volume);
        if (!sourcePlayers.isEmpty() || !destinationPlayers.isEmpty()) {
            String occupied = !sourcePlayers.isEmpty()
                    ? sourceLevel.dimension().identifier() + ":" + sourcePlayers.getFirst().getScoreboardName()
                    : destination.dimension().identifier() + ":" + destinationPlayers.getFirst().getScoreboardName();
            source.sendFailure(Component.literal(
                    "Fixture volume contains player " + occupied + ". Move them outside and retry."));
            return 0;
        }

        buildAt(player, sourceLevel, destination, kettlePos, facing);

        OtherwhereHearthTransposition.Inspection inspection =
                OtherwhereHearthTransposition.inspect(player, sourceLevel, kettlePos);
        if (!inspection.ready()) {
            source.sendFailure(Component.literal(
                    "Fixture was built, but its dry-run found " + inspection.blockers().size()
                            + " blocker(s). Run /witchery verify otherwhere_hearth beside it."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Built a disposable paired-world Otherwhere Hearth fixture at "
                        + coordinates(kettlePos) + ".")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal(
                "It is READY with a bound Kettle, both rings, six sworn living coven witches, a charged altar, "
                        + "containers, sign, fluids, redstone, pets, a hostile, an item, and a ridden minecart. "
                        + "Stand beside the Kettle and run /witchery verify otherwhere_hearth before crossing."), false);
        return 1;
    }

    static void buildAt(ServerPlayer owner, ServerLevel sourceLevel, ServerLevel destination,
                        BlockPos kettlePos, Direction facing) {
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(kettlePos);
        OtherwhereHearthTransposition.loadVolumeForInspection(sourceLevel, kettlePos);
        OtherwhereHearthTransposition.loadVolumeForInspection(destination, kettlePos);
        removeNonPlayers(sourceLevel, volume);
        removeNonPlayers(destination, volume);
        clearVolume(sourceLevel, volume);
        clearVolume(destination, volume);

        buildShell(sourceLevel, kettlePos, false);
        buildShell(destination, kettlePos, true);
        buildSourceHearth(sourceLevel, kettlePos, facing, owner);
        buildSourceFixtures(sourceLevel, kettlePos, facing, owner);
        buildDestinationFixtures(destination, kettlePos, facing);
        fundSelectedAltar(sourceLevel, kettlePos);
    }

    /** Builds only the already-loaded source side for loaded-only Seer Stone QA. */
    static void buildLocalAt(ServerPlayer owner, ServerLevel sourceLevel,
                             BlockPos kettlePos, Direction facing) {
        OtherwhereHearthTransposition.Volume volume =
                OtherwhereHearthTransposition.volumeFor(kettlePos);
        OtherwhereHearthTransposition.loadVolumeForInspection(sourceLevel, kettlePos);
        removeNonPlayers(sourceLevel, volume);
        clearVolume(sourceLevel, volume);
        buildShell(sourceLevel, kettlePos, false);
        buildSourceHearth(sourceLevel, kettlePos, facing, owner);
        buildSourceFixtures(sourceLevel, kettlePos, facing, owner);
        fundSelectedAltar(sourceLevel, kettlePos);
    }

    private static void buildShell(ServerLevel level, BlockPos center, boolean spiritSide) {
        BlockState foundation = spiritSide
                ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floor = spiritSide
                ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                : WitcheryBlocks.ROWAN_PLANKS.get().defaultBlockState();
        BlockState wall = spiritSide
                ? Blocks.PURPLE_STAINED_GLASS.defaultBlockState()
                : WitcheryBlocks.ROWAN_LOG.get().defaultBlockState();

        for (int x = -WALL_RADIUS; x <= WALL_RADIUS; x++) {
            for (int z = -WALL_RADIUS; z <= WALL_RADIUS; z++) {
                level.setBlock(center.offset(x, -2, z), foundation, Block.UPDATE_ALL);
                level.setBlock(center.offset(x, -1, z), floor, Block.UPDATE_ALL);
            }
        }
        for (int y = 0; y <= 5; y++) {
            for (int i = -WALL_RADIUS; i <= WALL_RADIUS; i++) {
                if (Math.abs(i) > 1) {
                    level.setBlock(center.offset(i, y, -WALL_RADIUS), wall, Block.UPDATE_ALL);
                    level.setBlock(center.offset(i, y, WALL_RADIUS), wall, Block.UPDATE_ALL);
                    level.setBlock(center.offset(-WALL_RADIUS, y, i), wall, Block.UPDATE_ALL);
                    level.setBlock(center.offset(WALL_RADIUS, y, i), wall, Block.UPDATE_ALL);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlock(center.offset(x, 6, z),
                        spiritSide ? Blocks.TINTED_GLASS.defaultBlockState()
                                : Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void buildSourceHearth(ServerLevel level, BlockPos kettlePos,
                                          Direction facing, ServerPlayer owner) {
        BlockPos floorCenter = kettlePos.below();
        placeRing(level, floorCenter, RitualCircleDetector.CircleSize.MEDIUM,
                WitcheryBlocks.CIRCLE_GLYPH.get());
        placeRing(level, floorCenter, RitualCircleDetector.CircleSize.LARGE,
                WitcheryBlocks.CIRCLE_OTHERWHERE.get());
        level.setBlock(kettlePos.below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(kettlePos, WitcheryBlocks.KETTLE.get().defaultBlockState()
                .setValue(KettleBlock.FACING, facing.getOpposite()), Block.UPDATE_ALL);
        if (level.getBlockEntity(kettlePos) instanceof KettleBlockEntity kettle) {
            kettle.bindOtherwhereHearth(owner);
        }

        BlockPos altarBackLeft = kettlePos.offset(-10, 0, 4);
        placeAltar(level, altarBackLeft, Direction.Axis.X);
        level.setBlock(altarBackLeft.above(), WitcheryBlocks.INFINITY_EGG.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(altarBackLeft.east().above(), WitcheryBlocks.CHALICE.get().defaultBlockState()
                .setValue(ChaliceBlock.FILLED, true), Block.UPDATE_ALL);
        level.setBlock(altarBackLeft.east(2).above(), Blocks.PLAYER_HEAD.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(altarBackLeft.south().above(), WitcheryBlocks.CANDELABRA.get().defaultBlockState(), Block.UPDATE_ALL);
        placeAltarItem(level, altarBackLeft.south().east().above(), facing, WitcheryItems.ARTHANA.get());
        placeAltarItem(level, altarBackLeft.south().east(2).above(), facing, WitcheryItems.PENTACLE.get());

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockState leaves = WitcheryBlocks.ROWAN_LEAVES.get().defaultBlockState();
                if (leaves.hasProperty(LeavesBlock.PERSISTENT)) {
                    leaves = leaves.setValue(LeavesBlock.PERSISTENT, true);
                }
                level.setBlock(kettlePos.offset(x, -5, z), leaves, Block.UPDATE_ALL);
            }
        }

    }

    private static void fundSelectedAltar(ServerLevel level, BlockPos kettlePos) {
        BlockPos altarBackLeft = kettlePos.offset(-10, 0, 4);
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                if (level.getBlockEntity(altarBackLeft.offset(x, 0, z))
                        instanceof AltarBlockEntity altar) {
                    altar.invalidate(level);
                }
            }
        }
        AltarBlockEntity selected = AltarPowerSourceLookup.findClosest(level, kettlePos);
        if (selected != null) {
            selected.refundPower(100_000);
        }
    }

    private static void placeAltar(ServerLevel level, BlockPos backLeft, Direction.Axis axis) {
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 3; x++) {
                level.setBlock(backLeft.offset(x, 0, z), WitcheryBlocks.ALTAR.get().defaultBlockState()
                        .setValue(AltarBlock.AXIS, axis), Block.UPDATE_ALL);
            }
        }
    }

    private static void placeAltarItem(ServerLevel level, BlockPos pos, Direction facing, Item item) {
        level.setBlock(pos, WitcheryBlocks.PLACED_ITEM.get().defaultBlockState()
                .setValue(PlacedItemBlock.FACING, facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof PlacedItemBlockEntity placedItem) {
            placedItem.setStack(new ItemStack(item));
        }
    }

    private static void buildSourceFixtures(ServerLevel level, BlockPos center,
                                            Direction facing, ServerPlayer owner) {
        placeContainer(level, center.offset(9, 0, -7), Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, facing),
                List.of(new ItemStack(Items.DIAMOND, 3), new ItemStack(WitcheryItems.ATTUNED_STONE_CHARGED.get())));
        placeContainer(level, center.offset(9, 0, -5), Blocks.BARREL.defaultBlockState(),
                List.of(new ItemStack(Items.GOLDEN_APPLE), new ItemStack(WitcheryItems.BREW_OF_SLEEPING.get(), 2)));
        level.setBlock(center.offset(9, 0, -3), Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_ALL);

        buildFluidBasin(level, center.offset(8, 0, 7), Blocks.WATER.defaultBlockState());
        level.setBlock(center.offset(9, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(center.offset(10, 0, 3), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(center.offset(9, 0, 1), Blocks.PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, facing), Block.UPDATE_ALL);

        int[][] covenOffsets = {{-2, -2}, {0, -2}, {2, -2}, {-2, 2}, {0, 2}, {2, 2}};
        for (int index = 0; index < covenOffsets.length; index++) {
            CovenWitchEntity witch = spawnMob(level, WitcheryEntities.COVEN_WITCH.get(),
                    center.offset(covenOffsets[index][0], 0, covenOffsets[index][1]), index * 60.0F);
            if (witch != null) {
                witch.setCovenOwner(owner.getUUID());
                witch.setCustomName(Component.literal("Hearth Coven " + (index + 1)));
            }
        }

        Wolf wolf = spawnMob(level, EntityType.WOLF, center.offset(4, 0, -3), 0.0F);
        if (wolf != null) {
            wolf.tame(owner);
            wolf.setCustomName(Component.literal("Round Trip Dog"));
        }
        Cat cat = spawnMob(level, EntityType.CAT, center.offset(4, 0, -1), 0.0F);
        if (cat != null) {
            cat.tame(owner);
            cat.setCustomName(Component.literal("Round Trip Cat"));
        }
        Mob hostile = spawnMob(level, EntityType.ZOMBIE, center.offset(-4, 0, -3), 180.0F);
        if (hostile != null) {
            hostile.setCustomName(Component.literal("Round Trip Hostile"));
        }

        AbstractMinecart minecart = EntityType.MINECART.create(level, EntitySpawnReason.COMMAND);
        Mob villager = spawnMob(level, EntityType.VILLAGER, center.offset(-4, 0, 3), 0.0F);
        if (minecart != null && villager != null) {
            villager.setCustomName(Component.literal("Round Trip Passenger"));
            minecart.setPos(center.getX() - 3.5D, center.getY(), center.getZ() + 3.5D);
            minecart.setCustomName(Component.literal("Round Trip Vehicle"));
            level.addFreshEntity(minecart);
            villager.startRiding(minecart, true, true);
        }

        Entity stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        if (stand != null) {
            stand.setPos(center.getX() + 6.5D, center.getY(), center.getZ() + 5.5D);
            stand.setCustomName(Component.literal("Round Trip Display"));
            stand.setCustomNameVisible(true);
            level.addFreshEntity(stand);
        }
        ItemEntity item = new ItemEntity(level, center.getX() + 6.5D, center.getY() + 0.5D,
                center.getZ() - 5.5D, new ItemStack(Items.EMERALD, 7));
        item.setCustomName(Component.literal("Round Trip Item"));
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
    }

    private static void buildDestinationFixtures(ServerLevel level, BlockPos center, Direction facing) {
        placeContainer(level, center.offset(-9, 0, -7), Blocks.CHEST.defaultBlockState()
                        .setValue(ChestBlock.FACING, facing.getOpposite()),
                List.of(new ItemStack(Items.AMETHYST_SHARD, 11), new ItemStack(Items.CLOCK)));
        level.setBlock(center.offset(-9, 0, -5), Blocks.CRIMSON_SIGN.defaultBlockState(), Block.UPDATE_ALL);
        buildFluidBasin(level, center.offset(-8, 0, 7), Blocks.LAVA.defaultBlockState());
        level.setBlock(center.offset(-9, 0, 3), Blocks.SOUL_LANTERN.defaultBlockState(), Block.UPDATE_ALL);

        Mob pig = spawnMob(level, EntityType.PIG, center.offset(4, 0, 4), 90.0F);
        if (pig != null) {
            pig.setCustomName(Component.literal("Spirit-side Marker"));
            pig.setNoAi(true);
            pig.setInvulnerable(true);
        }
    }

    private static void buildFluidBasin(ServerLevel level, BlockPos fluidPos, BlockState fluid) {
        level.setBlock(fluidPos.below(), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(fluidPos.relative(direction), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(fluidPos, fluid, Block.UPDATE_ALL);
    }

    private static void placeContainer(ServerLevel level, BlockPos pos, BlockState state,
                                       List<ItemStack> contents) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container)) {
            return;
        }
        for (int slot = 0; slot < contents.size(); slot++) {
            container.setItem(slot, contents.get(slot).copy());
        }
        container.setChanged();
    }

    private static void placeRing(ServerLevel level, BlockPos floorCenter,
                                  RitualCircleDetector.CircleSize size, Block chalk) {
        for (BlockPos markPos : RitualCircleDetector.ringPositions(floorCenter, size)) {
            level.setBlock(markPos,
                    ChalkCircleBlock.withRandomGlyph(chalk.defaultBlockState(), level.getRandom()),
                    Block.UPDATE_ALL);
        }
    }

    private static <T extends Mob> T spawnMob(ServerLevel level, EntityType<T> type,
                                               BlockPos pos, float yaw) {
        T entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return null;
        }
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setYRot(yaw);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.COMMAND, null);
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        return entity;
    }

    private static List<ServerPlayer> playersIn(ServerLevel level,
                                                 OtherwhereHearthTransposition.Volume volume) {
        AABB bounds = volume.entityBounds();
        return level.players().stream()
                .filter(player -> bounds.intersects(player.getBoundingBox())
                        || bounds.contains(player.position()))
                .toList();
    }

    static void removeNonPlayers(ServerLevel level,
                                 OtherwhereHearthTransposition.Volume volume) {
        level.getEntitiesOfClass(Entity.class, volume.entityBounds(),
                        entity -> !(entity instanceof ServerPlayer))
                .forEach(Entity::discard);
    }

    static void clearVolume(ServerLevel level,
                            OtherwhereHearthTransposition.Volume volume) {
        for (BlockPos cursor : BlockPos.betweenClosed(volume.min(), volume.max())) {
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        }
    }

    private static String coordinates(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
