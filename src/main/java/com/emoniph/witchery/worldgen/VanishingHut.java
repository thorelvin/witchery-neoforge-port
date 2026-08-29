package com.emoniph.witchery.worldgen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.VanishingHutAnchorBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared placement, demolition, observation, and counterplay for the Vanishing Witch Hut.
 *
 * <p>The designer-owned structure stays in standard Minecraft NBT form at
 * {@code data/witchery/structure/vanishing_hut/vanishing_hut.nbt}. Code only adapts where and how
 * that template is placed, so the NBT remains editable with an in-game structure block.</p>
 */
public final class VanishingHut {

    public static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath(
            WitcheryMod.MODID, "vanishing_hut/vanishing_hut");

    public static final int SIZE_X = 7;
    public static final int SIZE_Y = 10;
    public static final int SIZE_Z = 9;
    public static final int ANCHOR_X = 3;
    public static final int ANCHOR_Y = 0;
    public static final int ANCHOR_Z = 4;

    public static final int MIN_DX = -ANCHOR_X;
    public static final int MAX_DX = SIZE_X - ANCHOR_X - 1;
    public static final int MIN_DZ = -ANCHOR_Z;
    public static final int MAX_DZ = SIZE_Z - ANCHOR_Z - 1;

    public static final int HALF = Math.max(-MIN_DX, MAX_DX);
    public static final int FLOOR_DY = 1;
    public static final int ROOF_DY = SIZE_Y - 1;
    public static final int WALL_HEIGHT = ROOF_DY - FLOOR_DY;

    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "chests/vanishing_hut"));
    private static final int SET_WITHOUT_DROPS = Block.UPDATE_CLIENTS
            | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private static final Set<String> OWNED_TEMPLATE_BLOCKS = Set.of(
            "minecraft:barrel",
            "minecraft:bookshelf",
            "minecraft:cauldron",
            "minecraft:campfire",
            "minecraft:cracked_stone_bricks",
            "minecraft:decorated_pot",
            "minecraft:infested_cobblestone",
            "minecraft:lantern",
            "minecraft:lever",
            "minecraft:mossy_stone_bricks",
            "minecraft:mossy_stone_brick_stairs",
            "minecraft:mossy_stone_brick_wall",
            "minecraft:oak_log",
            "minecraft:oxidized_copper_chest",
            "minecraft:potted_poppy",
            "minecraft:purple_stained_glass_pane",
            "minecraft:spruce_door",
            "minecraft:spruce_fence",
            "minecraft:spruce_planks",
            "minecraft:spruce_slab",
            "minecraft:spruce_stairs",
            "minecraft:spruce_trapdoor",
            "minecraft:stone_bricks",
            "minecraft:stone_brick_wall",
            "minecraft:stripped_oak_wood",
            "witchery:witch_cauldron");

    private VanishingHut() {}

    public static int groundY(BlockPos anchorPos) {
        return anchorPos.getY() + FLOOR_DY;
    }

    public static BlockPos templateOrigin(BlockPos anchorPos) {
        return anchorPos.offset(-ANCHOR_X, -ANCHOR_Y, -ANCHOR_Z);
    }

    public static boolean inFootprint(int dx, int dz) {
        return dx >= MIN_DX && dx <= MAX_DX && dz >= MIN_DZ && dz <= MAX_DZ;
    }

    /** Occupancy box: the walkable hut interior that primes the relocation trigger. */
    public static AABB interiorAabb(BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int floorY = groundY(anchorPos);
        return new AABB(ax - 2.0, floorY + 0.1, az - 2.0,
                ax + 3.0, floorY + 7.0, az + 4.0);
    }

    private static Vec3 center(BlockPos anchorPos) {
        return new Vec3(anchorPos.getX() + 0.5, groundY(anchorPos) + 3.5, anchorPos.getZ() + 0.5);
    }

    private static Vec3[] watchSamples(BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        double baseY = groundY(anchorPos);
        return new Vec3[]{
                center(anchorPos),
                new Vec3(ax + 0.5, anchorPos.getY() + ROOF_DY + 0.5, az + 0.5),
                new Vec3(ax + MIN_DX + 0.5, baseY + 1.5, az + MIN_DZ + 0.5),
                new Vec3(ax + MAX_DX + 0.5, baseY + 1.5, az + MIN_DZ + 0.5),
                new Vec3(ax + MIN_DX + 0.5, baseY + 1.5, az + MAX_DZ + 0.5),
                new Vec3(ax + MAX_DX + 0.5, baseY + 1.5, az + MAX_DZ + 0.5)
        };
    }

    public static boolean isWatched(ServerLevel level, BlockPos anchorPos) {
        Vec3 center = center(anchorPos);
        Vec3[] samples = watchSamples(anchorPos);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            if (player.distanceToSqr(center) > VanishingHutHelper.WATCH_RANGE_SQ) continue;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            for (Vec3 sample : samples) {
                if (VanishingHutHelper.isLookingAt(eye, look, sample)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Counterplay: a line of Witches' Salt across the north doorway holds the hut in place. The old
     * south-facing procedural hut row is accepted too, so existing test worlds do not lose the trick.
     */
    public static boolean isHeldBySalt(ServerLevel level, BlockPos anchorPos) {
        int surfaceY = groundY(anchorPos);
        int northThresholdZ = anchorPos.getZ() - 3;
        int legacySouthThresholdZ = anchorPos.getZ() + HALF + 1;
        return hasSaltLine(level, anchorPos.getX(), surfaceY + 1, northThresholdZ)
                || hasSaltLine(level, anchorPos.getX(), surfaceY, northThresholdZ)
                || hasSaltLine(level, anchorPos.getX(), surfaceY + 1, legacySouthThresholdZ)
                || hasSaltLine(level, anchorPos.getX(), surfaceY, legacySouthThresholdZ);
    }

    private static boolean hasSaltLine(ServerLevel level, int centerX, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            if (!level.getBlockState(new BlockPos(centerX + dx, y, z)).is(WitcheryBlocks.SALT_LINE.get())) {
                return false;
            }
        }
        return true;
    }

    public static void build(ServerLevelAccessor level, BlockPos anchorPos, UUID hutId) {
        ServerLevel serverLevel = level.getLevel();
        build(level, anchorPos, hutId, sampleSurfaceState(serverLevel, anchorPos));
    }

    /** Worldgen-safe build path that never requests a neighboring chunk during feature placement. */
    public static void buildForWorldgen(WorldGenLevel level, BlockPos anchorPos, UUID hutId) {
        BlockState surface = normalizeSurface(level.getBlockState(anchorPos.above()));
        if (surface == null) {
            surface = normalizeSurface(level.getBlockState(anchorPos));
        }
        build(level, anchorPos, hutId,
                surface == null ? Blocks.GRASS_BLOCK.defaultBlockState() : surface);
    }

    private static void build(ServerLevelAccessor level, BlockPos anchorPos, UUID hutId,
                              BlockState surface) {
        ServerLevel serverLevel = level.getLevel();
        StructureTemplate template = serverLevel.getStructureManager().getOrCreate(TEMPLATE);
        BlockPos origin = templateOrigin(anchorPos);
        boolean placed = template.placeInWorld(level, origin, origin, makeSettings(), serverLevel.random, 3);
        if (!placed) {
            WitcheryMod.LOGGER.warn("[VanishingHut] Could not place template {} at {}", TEMPLATE, origin);
        }

        backfillAnchorLayer(level, anchorPos, surface);
        restoreSurfaceLayerHoles(level, anchorPos, surface);
        installLootTables(level, anchorPos);

        level.setBlock(anchorPos, WitcheryBlocks.VANISHING_HUT_ANCHOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(anchorPos) instanceof VanishingHutAnchorBlockEntity be) {
            be.setHutId(hutId);
            be.markSettled(serverLevel.getGameTime());
        }
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    private static void backfillAnchorLayer(ServerLevelAccessor level, BlockPos anchorPos, BlockState support) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        for (int dx = MIN_DX; dx <= MAX_DX; dx++) {
            for (int dz = MIN_DZ; dz <= MAX_DZ; dz++) {
                BlockPos pos = new BlockPos(ax + dx, anchorPos.getY(), az + dz);
                if (!pos.equals(anchorPos) && level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, supportStateFor(support), 3);
                }
            }
        }
    }

    private static void restoreSurfaceLayerHoles(ServerLevelAccessor level, BlockPos anchorPos, BlockState surface) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int surfaceY = groundY(anchorPos);
        BlockState fill = normalizeSurface(surface);
        if (fill == null) {
            fill = Blocks.GRASS_BLOCK.defaultBlockState();
        }
        for (int dx = MIN_DX; dx <= MAX_DX; dx++) {
            for (int dz = MIN_DZ; dz <= MAX_DZ; dz++) {
                BlockPos pos = new BlockPos(ax + dx, surfaceY, az + dz);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, fill, 3);
                }
            }
        }
    }

    private static void installLootTables(ServerLevelAccessor level, BlockPos anchorPos) {
        ServerLevel serverLevel = level.getLevel();
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        for (int dx = MIN_DX; dx <= MAX_DX; dx++) {
            for (int dz = MIN_DZ; dz <= MAX_DZ; dz++) {
                for (int dy = FLOOR_DY; dy <= ROOF_DY; dy++) {
                    BlockPos pos = new BlockPos(ax + dx, anchorPos.getY() + dy, az + dz);
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
                    if (!blockId.getPath().contains("chest")) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof RandomizableContainer container) {
                        container.setLootTable(LOOT_TABLE, serverLevel.random.nextLong());
                        blockEntity.setChanged();
                    }
                }
            }
        }
    }

    public static CovenWitchEntity spawnResident(ServerLevelAccessor level, BlockPos anchorPos) {
        ServerLevel sl = level.getLevel();
        BlockPos home = residentHome(anchorPos);
        CovenWitchEntity witch = WitcheryEntities.COVEN_WITCH.get().create(sl, EntitySpawnReason.STRUCTURE);
        if (witch == null) {
            return null;
        }
        witch.setPersistenceRequired();
        witch.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        witch.finalizeSpawn(level, new DifficultyInstance(sl.getDifficulty(), 0L, 0L, 0.0F),
                EntitySpawnReason.STRUCTURE, null);
        witch.setHutHome(home);
        level.addFreshEntityWithPassengers(witch);
        return witch;
    }

    public static BlockPos residentHome(BlockPos anchorPos) {
        return new BlockPos(anchorPos.getX(), groundY(anchorPos) + 1, anchorPos.getZ() + 1);
    }

    public static BlockPos anchorFromResidentHome(BlockPos home) {
        return home.offset(0, -(FLOOR_DY + 1), -1);
    }

    /** Player standing position immediately outside the template's open north-facing door. */
    public static BlockPos arrivalPos(BlockPos anchorPos) {
        return new BlockPos(anchorPos.getX(), groundY(anchorPos) + 1, anchorPos.getZ() - 3);
    }

    public static void vanish(ServerLevel level, BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int surfaceY = groundY(anchorPos);
        int roofY = anchorPos.getY() + ROOF_DY;
        BlockState restoreSurface = sampleSurfaceState(level, anchorPos);
        BlockState restoreSupport = supportStateFor(restoreSurface);

        for (CovenWitchEntity witch : level.getEntitiesOfClass(CovenWitchEntity.class, interiorAabb(anchorPos))) {
            witch.discard();
        }

        for (int dx = MIN_DX; dx <= MAX_DX; dx++) {
            for (int dz = MIN_DZ; dz <= MAX_DZ; dz++) {
                for (int y = anchorPos.getY(); y <= roofY; y++) {
                    BlockPos pos = new BlockPos(ax + dx, y, az + dz);
                    BlockState state = level.getBlockState(pos);
                    if (pos.equals(anchorPos) || isOwnedTemplateBlock(state)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_WITHOUT_DROPS);
                    }
                }
                level.setBlock(new BlockPos(ax + dx, anchorPos.getY(), az + dz), restoreSupport, 3);
                level.setBlock(new BlockPos(ax + dx, surfaceY, az + dz), restoreSurface, 3);
            }
        }

        leaveTraces(level, ax, az, surfaceY + 1);
    }

    private static boolean isOwnedTemplateBlock(BlockState state) {
        return state.is(WitcheryBlocks.VANISHING_HUT_ANCHOR.get())
                || OWNED_TEMPLATE_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static BlockState sampleSurfaceState(ServerLevel level, BlockPos anchorPos) {
        int ax = anchorPos.getX();
        int az = anchorPos.getZ();
        int surfaceY = groundY(anchorPos);
        for (int ring = 1; ring <= 4; ring++) {
            for (int dx = MIN_DX - ring; dx <= MAX_DX + ring; dx++) {
                for (int dz = MIN_DZ - ring; dz <= MAX_DZ + ring; dz++) {
                    if (inFootprint(dx, dz)) {
                        continue;
                    }
                    boolean onRing = dx == MIN_DX - ring || dx == MAX_DX + ring
                            || dz == MIN_DZ - ring || dz == MAX_DZ + ring;
                    if (!onRing) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(ax + dx, surfaceY, az + dz);
                    BlockState state = normalizeSurface(level.getBlockState(pos));
                    if (state != null) {
                        return state;
                    }
                    state = normalizeSurface(level.getBlockState(pos.below()));
                    if (state != null) {
                        return state;
                    }
                }
            }
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static BlockState normalizeSurface(BlockState state) {
        if (state.getFluidState().isEmpty() && (state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT))) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (state.getFluidState().isEmpty() && (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY))) {
            return state;
        }
        return null;
    }

    private static BlockState supportStateFor(BlockState surface) {
        if (surface.is(Blocks.SAND)) {
            return Blocks.SAND.defaultBlockState();
        }
        if (surface.is(Blocks.RED_SAND)) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        if (surface.is(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (surface.is(Blocks.CLAY)) {
            return Blocks.CLAY.defaultBlockState();
        }
        if (surface.is(Blocks.MUD)) {
            return Blocks.MUD.defaultBlockState();
        }
        return Blocks.DIRT.defaultBlockState();
    }

    private static void leaveTraces(ServerLevel level, int ax, int az, int traceY) {
        placeIfAir(level, new BlockPos(ax - 1, traceY, az), Blocks.OAK_SLAB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax + 1, traceY, az + 1), Blocks.OAK_TRAPDOOR.defaultBlockState());
        placeIfAir(level, new BlockPos(ax, traceY, az - 1), Blocks.OAK_SLAB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax + MIN_DX, traceY, az + MIN_DZ), Blocks.COBWEB.defaultBlockState());
        placeIfAir(level, new BlockPos(ax + MAX_DX, traceY, az + MAX_DZ), Blocks.COBWEB.defaultBlockState());

        double cx = ax + 0.5;
        double cy = traceY + 0.2;
        double cz = az + 0.5;
        level.sendParticles(ParticleTypes.WHITE_ASH, cx, cy, cz, 60, 1.6, 1.2, 1.6, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 40, 1.4, 1.0, 1.4, 0.02);
        level.sendParticles(ParticleTypes.WITCH, cx, cy + 1.0, cz, 24, 1.2, 1.0, 1.2, 0.05);
        level.playSound(null, new BlockPos(ax, traceY, az), SoundEvents.WITCH_CELEBRATE,
                SoundSource.AMBIENT, 0.7F, 0.6F);

        Component farewell = Component.literal("We may not meet again.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        Vec3 here = new Vec3(cx, cy, cz);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(here, here).inflate(32.0));
        for (ServerPlayer player : nearby) {
            player.sendSystemMessage(farewell);
        }
    }

    private static void placeIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, state, 3);
        }
    }
}
