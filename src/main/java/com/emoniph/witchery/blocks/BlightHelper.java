package com.emoniph.witchery.blocks;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.BlightHeartSavedData;
import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BlightHelper {
    public static final int HEART_RADIUS = 16;

    public static final int HEART_RADIUS_SQ = HEART_RADIUS * HEART_RADIUS;
    private static final int LOCAL_SPREAD = 3;
    private static final GameProfile LEGACY_CLEANUP_PROFILE = new GameProfile(
            UUID.fromString("e74f7e78-65da-44e5-9424-f73c66246089"), "WitcheryBlight");

    private BlightHelper() {}

    public static boolean isHeartEnabled() {
        return WitcheryConfig.ENABLE_BLIGHT_HEART.get();
    }

    public static boolean isBlight(BlockState state) {
        return state.is(WitcheryBlocks.BLIGHT_HEART.get())
                || state.is(WitcheryBlocks.BLIGHT_FIBRE.get())
                || state.is(WitcheryBlocks.BLIGHTED_SOIL.get())
                || state.is(WitcheryBlocks.BLIGHTED_STONE.get())
                || state.is(WitcheryBlocks.BLIGHTED_LOG.get());
    }

    public static boolean isNearHeart(LevelReader level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel
                && BlightHeartSavedData.get(serverLevel.getServer()).hasSustainingHeartNear(serverLevel, pos);
    }

    public static boolean canBlightSurvive(LevelReader level, BlockPos pos) {
        return BlightFibreBlock.hasAttachableFace(level, pos) && !isProtected(level, pos);
    }

    public static boolean isProtected(LevelReader level, BlockPos pos) {
        if (!isLoaded(level, pos) || !isLoaded(level, pos.below())) {
            return true;
        }
        if (isBarrier(level.getBlockState(pos)) || isBarrier(level.getBlockState(pos.below()))) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!isLoaded(level, neighbor) || isBarrier(level.getBlockState(neighbor))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBarrier(BlockState state) {
        return state.is(WitcheryBlockTags.BLIGHT_BARRIERS);
    }

    public static void spreadFromHeart(ServerLevel level, BlockPos heartPos, RandomSource random) {
        if (!isHeartEnabled()) {
            return;
        }
        BlightHeartSavedData data = BlightHeartSavedData.get(level.getServer());
        BlightHeartSavedData.HeartRecord heart = data.heartAt(level, heartPos);
        ServerPlayer owner = onlineOwner(level, heart);
        if (owner == null || !data.claimSpreadWindow(heart, level.getGameTime())) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos origin = data.randomSpreadOrigin(heart, random).orElse(heartPos);
            BlockPos target = origin.offset(
                    random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD,
                    random.nextInt(3) - 1,
                    random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD);
            convertTarget(level, heart, owner, target, true);
        }
    }

    public static void trySpreadNear(ServerLevel level, BlockPos origin, RandomSource random, boolean ignoreChance) {
        if (!isHeartEnabled()) {
            return;
        }
        if (!ignoreChance && random.nextInt(4) != 0) {
            return;
        }
        BlightHeartSavedData data = BlightHeartSavedData.get(level.getServer());
        BlightHeartSavedData.HeartRecord heart = data.heartOwningMutation(level, origin);
        ServerPlayer owner = onlineOwner(level, heart);
        if (owner == null || !data.claimSpreadWindow(heart, level.getGameTime())) {
            return;
        }

        BlockPos target = origin.offset(
                random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD,
                random.nextInt(3) - 1,
                random.nextInt(LOCAL_SPREAD * 2 + 1) - LOCAL_SPREAD);
        if (!target.equals(origin)) {
            convertTarget(level, heart, owner, target, true);
        }
    }

    public static boolean canPlaceEmbeddedHeartAt(LevelReader level, BlockPos pos) {
        if (isProtected(level, pos) || isProtected(level, pos.above())) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        if (isBlight(state) || state.hasBlockEntity() || state.liquid() || state.canBeReplaced()
                || !above.canBeReplaced() || above.liquid()) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F
                && hardness <= 10.0F
                && state.isFaceSturdy(level, pos, Direction.UP);
    }

    public static boolean placeHeart(ServerLevel level, BlockPos heartPos, boolean seedAdjacent) {
        return false;
    }

    public static BlightHeartSavedData.PlacementResult canPlaceHeartFor(
            ServerLevel level, ServerPlayer owner, BlockPos heartPos) {
        if (!isHeartEnabled()) {
            return BlightHeartSavedData.PlacementResult.INVALID;
        }
        return BlightHeartSavedData.get(level.getServer()).canRegister(level, owner, heartPos);
    }

    public static String placementFailureKey(BlightHeartSavedData.PlacementResult result) {
        return switch (result) {
            case OWNER_LIMIT -> "witchery.blight_heart.owner_limit";
            case NEARBY_HEART -> "witchery.blight_heart.too_close";
            case INVALID, ALLOWED -> "witchery.ritual.blight_heart.no_ground";
        };
    }

    public static boolean placeHeart(ServerLevel level, BlockPos heartPos, boolean seedAdjacent,
                                     ServerPlayer owner) {
        if (!isHeartEnabled()) {
            return false;
        }
        if (!WitcheryProtectionService.canModifyBlock(owner, level, heartPos,
                WitcheryProtectionAction.BLOCK_TRANSFORM)
                || !WitcheryProtectionService.canModifyBlock(owner, level, heartPos.above(),
                WitcheryProtectionAction.BLOCK_TRANSFORM)) {
            return false;
        }
        BlockState original = level.getBlockState(heartPos);
        BlightHeartSavedData data = BlightHeartSavedData.get(level.getServer());
        if (data.register(level, owner, heartPos, original) != BlightHeartSavedData.PlacementResult.ALLOWED) {
            return false;
        }

        BlightHeartSavedData.HeartRecord heart = data.heartAt(level, heartPos);
        BlockPos above = heartPos.above();
        if (!level.setBlock(heartPos, WitcheryBlocks.BLIGHT_HEART.get().defaultBlockState(), Block.UPDATE_ALL)) {
            data.unregisterFailedPlacement(level, owner.getUUID());
            return false;
        }
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && aboveState.canBeReplaced()) {
            applyMutation(level, heart, owner, above, Blocks.AIR.defaultBlockState(), 0);
        }
        if (seedAdjacent) {
            seedHeartFibres(level, heart, owner);
        }
        level.playSound(null, heartPos, SoundEvents.WITCH_THROW, SoundSource.BLOCKS,
                0.8F, 0.55F + level.random.nextFloat() * 0.2F);
        level.playSound(null, heartPos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS,
                0.8F, 0.65F + level.random.nextFloat() * 0.25F);
        level.sendParticles(ParticleTypes.SMOKE,
                heartPos.getX() + 0.5D, heartPos.getY() + 0.65D, heartPos.getZ() + 0.5D,
                28, 0.55D, 0.35D, 0.55D, 0.04D);
        level.sendParticles(ParticleTypes.WITCH,
                heartPos.getX() + 0.5D, heartPos.getY() + 0.8D, heartPos.getZ() + 0.5D,
                22, 0.45D, 0.45D, 0.45D, 0.02D);
        return true;
    }

    static boolean convertTarget(ServerLevel level, BlightHeartSavedData.HeartRecord heart,
                                 ServerPlayer owner, BlockPos pos, boolean requireAdjacentBlight) {
        if (heart == null || owner == null || !level.isLoaded(pos)
                || heart.anchor().distSqr(pos) > HEART_RADIUS_SQ) {
            return false;
        }
        if (isProtected(level, pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (isBlight(state) || state.hasBlockEntity() || state.liquid()) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 10.0F) {
            return false;
        }

        if (canBecomeFibre(level, pos, state)) {
            BlockState fibre = BlightFibreBlock.withAttachments(WitcheryBlocks.BLIGHT_FIBRE.get().defaultBlockState(), level, pos);
            return applyMutation(level, heart, owner, pos, fibre, 4);
        }

        if (requireAdjacentBlight && !hasAdjacentBlight(level, pos)) {
            return false;
        }

        if (state.is(BlockTags.LOGS)) {
            BlockState blightedLog = WitcheryBlocks.BLIGHTED_LOG.get().defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                blightedLog = blightedLog.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            return applyMutation(level, heart, owner, pos, blightedLog, 6);
        }

        if (isSoil(state)) {
            return applyMutation(level, heart, owner, pos,
                    WitcheryBlocks.BLIGHTED_SOIL.get().defaultBlockState(), 6);
        }

        if (isStone(state)) {
            return applyMutation(level, heart, owner, pos,
                    WitcheryBlocks.BLIGHTED_STONE.get().defaultBlockState(), 6);
        }

        return false;
    }

    public static int cleanseArea(ServerLevel level, BlockPos center, int radius) {
        return cleanseArea(level, center, radius, null);
    }

    public static int cleanseArea(ServerLevel level, BlockPos center, int radius,
                                  @Nullable ServerPlayer actor) {
        int radiusSq = radius * radius;
        int cleansed = 0;
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (scan.distSqr(center) > radiusSq) {
                continue;
            }
            BlockPos pos = scan.immutable();
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isBlight(state) && cleanseBlock(level, pos, state, actor)) {
                cleansed++;
            }
        }
        if (cleansed > 0) {
            level.playSound(null, center, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.BLOCKS, 0.8F, 1.3F);
            level.sendParticles(ParticleTypes.POOF,
                    center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                    12 + cleansed, radius * 0.35D, radius * 0.2D, radius * 0.35D, 0.02D);
        }
        return cleansed;
    }

    public static void cleanseBlock(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            cleanseBlock(serverLevel, pos, state, null);
            return;
        }
        cleanseLegacyBlock(level, pos, state);
    }

    public static boolean cleanseBlock(ServerLevel level, BlockPos pos, BlockState state,
                                       @Nullable ServerPlayer actor) {
        BlightHeartSavedData data = BlightHeartSavedData.get(level.getServer());
        BlightHeartSavedData.HeartRecord tracked = data.heartOwningMutation(level, pos);
        if (tracked != null) {
            return data.restoreMutation(level, pos, actor, state.is(WitcheryBlocks.BLIGHT_HEART.get()));
        }
        ServerPlayer protectionActor = actor != null
                ? actor
                : FakePlayerFactory.get(level, LEGACY_CLEANUP_PROFILE);
        if (!level.isLoaded(pos) || !WitcheryProtectionService.canModifyBlock(
                protectionActor, level, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
            return false;
        }
        return cleanseLegacyBlock(level, pos, state);
    }

    private static boolean cleanseLegacyBlock(Level level, BlockPos pos, BlockState state) {
        if (state.is(WitcheryBlocks.BLIGHTED_SOIL.get())) {
            return level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(WitcheryBlocks.BLIGHTED_STONE.get())) {
            return level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(WitcheryBlocks.BLIGHTED_LOG.get())) {
            BlockState log = Blocks.OAK_LOG.defaultBlockState();
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                log = log.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            return level.setBlock(pos, log, Block.UPDATE_ALL);
        } else {
            return level.removeBlock(pos, false);
        }
    }

    public static void decayIfOrphaned(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!isHeartEnabled()) {
            if (random.nextInt(3) == 0) {
                cleanseBlock(level, pos, state, null);
            }
            return;
        }
        if (BlightHeartSavedData.get(level.getServer()).isSustained(level, pos) || random.nextInt(3) != 0) {
            return;
        }
        cleanseBlock(level, pos, state, null);
    }

    static void blightFeedback(ServerLevel level, BlockPos pos, int particles) {
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS,
                0.2F, 0.7F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                particles, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    public static void cleanseFeedback(ServerLevel level, BlockPos pos, int particles) {
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                particles, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private static void seedHeartFibres(ServerLevel level, BlightHeartSavedData.HeartRecord heart,
                                        ServerPlayer owner) {
        int placed = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.random.nextBoolean()
                    && seedFibreOnly(level, heart, owner, heart.anchor().relative(direction).above())) {
                placed++;
            }
        }

        for (int attempt = 0; attempt < 10 && placed < 6; attempt++) {
            int dx = level.random.nextInt(5) - 2;
            int dz = level.random.nextInt(5) - 2;
            if (dx == 0 && dz == 0) {
                continue;
            }
            if (seedFibreOnly(level, heart, owner, heart.anchor().offset(dx, 1, dz))) {
                placed++;
            }
        }
    }

    private static boolean seedFibreOnly(ServerLevel level, BlightHeartSavedData.HeartRecord heart,
                                         ServerPlayer owner, BlockPos pos) {
        if (isProtected(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (isBlight(state) || state.hasBlockEntity() || state.liquid() || !canBecomeFibre(level, pos, state)) {
            return false;
        }
        BlockState fibre = BlightFibreBlock.withAttachments(
                WitcheryBlocks.BLIGHT_FIBRE.get().defaultBlockState(), level, pos);
        return BlightFibreBlock.hasAnyAttachment(fibre)
                && applyMutation(level, heart, owner, pos, fibre, 3);
    }

    private static boolean applyMutation(ServerLevel level, BlightHeartSavedData.HeartRecord heart,
                                         ServerPlayer owner, BlockPos pos, BlockState next, int particles) {
        if (!level.isLoaded(pos) || !WitcheryProtectionService.canModifyBlock(owner, level, pos,
                WitcheryProtectionAction.BLOCK_TRANSFORM)) {
            return false;
        }
        BlockState original = level.getBlockState(pos);
        BlightHeartSavedData data = BlightHeartSavedData.get(level.getServer());
        if (!data.reserveMutation(heart, pos, original, next)) {
            return false;
        }
        if (!level.setBlock(pos, next, Block.UPDATE_ALL)) {
            data.rollbackFailedMutation(heart, pos);
            return false;
        }
        if (particles > 0) {
            blightFeedback(level, pos, particles);
        }
        return true;
    }

    @Nullable
    private static ServerPlayer onlineOwner(ServerLevel level,
                                            @Nullable BlightHeartSavedData.HeartRecord heart) {
        if (heart == null) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(heart.ownerId());
        return owner != null && owner.isAlive() && owner.level() == level ? owner : null;
    }

    private static boolean hasAdjacentBlight(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isBlight(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canBecomeFibre(LevelReader level, BlockPos pos, BlockState state) {
        return (state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.LEAVES))
                && canBlightSurvive(level, pos);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY);
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE);
    }

    private static boolean isLoaded(LevelReader level, BlockPos pos) {
        return !(level instanceof ServerLevel serverLevel) || serverLevel.isLoaded(pos);
    }
}
