package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.entity.MirrorFaceEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.DuplicationGrenadeItem;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.world.MirrorHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import org.jetbrains.annotations.Nullable;

/**
 * Mirror — a 2-tall facing block that serves as a portal into the Mirror World.
 *
 * Properties:
 *   FACING     — horizontal direction the reflective face looks toward
 *   PART       — HEAD (top) or FOOT (bottom)
 *   UNBREAKABLE — true for the inner Mirror World copy (cannot be broken by players)
 *
 * Walking through the mirror face from outside teleports to the Mirror World room.
 * Walking through the linked inner mirror returns to the source dimension.
 */
public class MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<MirrorBlock> CODEC = simpleCodec(MirrorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    public static final EnumProperty<BedPart>  PART        = BlockStateProperties.BED_PART;
    public static final BooleanProperty        UNBREAKABLE = BooleanProperty.create("unbreakable");

    // Source metadata names the direction that the reflective face looks toward.
    private static final double COLLISION_DEPTH = 2.4D;
    private static final double SELECTION_DEPTH = 5.12D;
    private static final VoxelShape NORTH_COLLISION = Block.box(0, 0, 0, 16, 16, COLLISION_DEPTH);
    private static final VoxelShape SOUTH_COLLISION = Block.box(0, 0, 16 - COLLISION_DEPTH, 16, 16, 16);
    private static final VoxelShape WEST_COLLISION = Block.box(0, 0, 0, COLLISION_DEPTH, 16, 16);
    private static final VoxelShape EAST_COLLISION = Block.box(16 - COLLISION_DEPTH, 0, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SELECTION = Block.box(0, 0, 0, 16, 16, SELECTION_DEPTH);
    private static final VoxelShape SOUTH_SELECTION = Block.box(0, 0, 16 - SELECTION_DEPTH, 16, 16, 16);
    private static final VoxelShape WEST_SELECTION = Block.box(0, 0, 0, SELECTION_DEPTH, 16, 16);
    private static final VoxelShape EAST_SELECTION = Block.box(16 - SELECTION_DEPTH, 0, 0, 16, 16, 16);

    public MirrorBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING,      Direction.NORTH)
                .setValue(PART,        BedPart.FOOT)
                .setValue(UNBREAKABLE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, UNBREAKABLE);
    }

    // ── Placement ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clickedFace = ctx.getClickedFace();
        Direction facing = clickedFace.getAxis().isHorizontal()
                ? clickedFace : ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxY() - 1
                && level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(PART, BedPart.FOOT)
                    .setValue(UNBREAKABLE, false);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockPos top = pos.above();
        level.setBlockAndUpdate(top, state.setValue(PART, BedPart.HEAD));
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof MirrorBlockEntity mirror && mirror.hasRoom()) {
            MirrorHelper.reconnectSourceMirror(serverLevel, pos, mirror);
        }
    }

    // ── Shapes ───────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return sourceSelectionShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return sourceCollisionShape(state.getValue(FACING));
    }

    static VoxelShape sourceCollisionShape(Direction facing) {
        return switch (facing) {
            case NORTH -> SOUTH_COLLISION;
            case SOUTH -> NORTH_COLLISION;
            case WEST  -> EAST_COLLISION;
            default    -> WEST_COLLISION;
        };
    }

    static VoxelShape sourceSelectionShape(Direction facing) {
        return switch (facing) {
            case NORTH -> SOUTH_SELECTION;
            case SOUTH -> NORTH_SELECTION;
            case WEST  -> EAST_SELECTION;
            default    -> WEST_SELECTION;
        };
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                       BlockPos pos, Player player, InteractionHand hand,
                                       net.minecraft.world.phys.BlockHitResult hit) {
        if (!stack.is(WitcheryItems.QUARTZ_SPHERE.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(UNBREAKABLE) || WitcheryDimensions.isMirrorWorld(level)) {
            failCharge(serverLevel, pos, player, "witchery.mirror.duplication.wrong_mirror");
            return InteractionResult.SUCCESS;
        }

        BlockPos footPos = state.getValue(PART) == BedPart.HEAD ? pos.below() : pos;
        if (!(level.getBlockEntity(footPos) instanceof MirrorBlockEntity mirror)
                || mirror.isDemonKilled()) {
            failCharge(serverLevel, footPos, player, "witchery.mirror.duplication.no_reflection");
            return InteractionResult.SUCCESS;
        }

        AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(level, footPos);
        if (altar == null || !altar.consumePower(2000)) {
            failCharge(serverLevel, footPos, player, "witchery.mirror.duplication.no_power");
            return InteractionResult.SUCCESS;
        }

        ItemStack grenade = new ItemStack(WitcheryItems.DUPLICATION_GRENADE.get());
        DuplicationGrenadeItem.setOwner(grenade, serverPlayer);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!serverPlayer.addItem(grenade)) {
            Block.popResource(serverLevel, footPos, grenade);
        }
        serverPlayer.displayClientMessage(Component.translatable("witchery.mirror.duplication.success"), true);
        serverLevel.playSound(null, footPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 0.75F);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                16, 0.35D, 0.45D, 0.35D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                10, 0.3D, 0.35D, 0.3D, 0.03D);
        return InteractionResult.SUCCESS;
    }

    private static void failCharge(ServerLevel level, BlockPos pos, Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.55F, 0.7F);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    // ── Portal logic ─────────────────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos footPos = state.getValue(PART) == BedPart.HEAD ? pos.below() : pos;
        BlockState footState = level.getBlockState(footPos);
        boolean mirrorWorld = WitcheryDimensions.isMirrorWorld(level);
        boolean unbreakable = footState.hasProperty(UNBREAKABLE) && footState.getValue(UNBREAKABLE);
        if (mirrorWorld || unbreakable
                || level.getBlockEntity(footPos) instanceof MirrorBlockEntity mirror
                && mirror.isDemonKilled()) {
            return InteractionResult.CONSUME;
        }
        String speech = mirrorRightClickSpeech(serverLevel, footPos, player);
        player.displayClientMessage(NpcBrainService.speechComponent("Mirror", 0x55FFFF, speech), false);
        showMirrorFace(serverLevel, footPos);
        serverLevel.playSound(null, footPos, WitcherySoundEvents.MOB_REFLECTION_SPEECH.get(),
                SoundSource.BLOCKS, 0.5F, 0.5F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                8, 0.25D, 0.35D, 0.25D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                6, 0.3D, 0.35D, 0.3D, 0.01D);
        return InteractionResult.CONSUME;
    }

    public static boolean showMirrorFace(ServerLevel level, BlockPos mirrorPos) {
        BlockState state = level.getBlockState(mirrorPos);
        if (!state.is(WitcheryBlocks.MIRROR.get()) || WitcheryDimensions.isMirrorWorld(level)) {
            return false;
        }
        BlockPos footPos = state.hasProperty(PART) && state.getValue(PART) == BedPart.HEAD
                ? mirrorPos.below()
                : mirrorPos;
        BlockState footState = level.getBlockState(footPos);
        if (!footState.is(WitcheryBlocks.MIRROR.get())
                || footState.getValue(UNBREAKABLE)
                || level.getBlockEntity(footPos) instanceof MirrorBlockEntity mirror && mirror.isDemonKilled()) {
            return false;
        }
        return MirrorFaceEntity.show(level, footPos, footState.getValue(FACING));
    }

    private static String mirrorRightClickSpeech(ServerLevel level, BlockPos footPos, Player player) {
        if (level.getBlockEntity(footPos) instanceof MirrorBlockEntity mirror && mirror.hasRoom()) {
            if (mirror.isDemonKilled()) {
                return "Only your own face answers. The dangerous one is gone.";
            }
            return "Something with your eyes waits behind this mirror.";
        }
        return "The glass is quiet, but not empty.";
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                              InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)
                || entity instanceof MirrorFaceEntity
                || !(entity instanceof LivingEntity || entity instanceof ItemEntity)
                || entity.tickCount % 5 != 1) return;

        // Only trigger from the FOOT half to avoid double-fire
        if (state.getValue(PART) != BedPart.FOOT) return;

        // Commit the cooldown only after a successful crossing.
        var data = entity.getPersistentData();
        long lastTeleport = data.getLong("MirrorTeleportCooldown").orElse(Long.MIN_VALUE);
        if (lastTeleport != Long.MIN_VALUE && level.getGameTime() - lastTeleport < 60) return;

        // Check entity is in the entry zone
        Direction facing = state.getValue(FACING);
        VoxelShape entryZone = sourceSelectionShape(facing);
        Vec3 epos = entity.position();
        double lx = epos.x - pos.getX();
        double ly = epos.y - pos.getY();
        double lz = epos.z - pos.getZ();
        if (!entryZone.bounds().inflate(0.1).contains(lx, ly, lz)) return;

        if (entity instanceof LivingEntity living
                && Direction.fromYRot(living.getYRot()) != facing.getOpposite()) return;

        boolean inMirrorWorld = WitcheryDimensions.isMirrorWorld(level);
        boolean unbreakable = state.getValue(UNBREAKABLE);
        MirrorBlockEntity mirror = MirrorHelper.getMirrorBlockEntity(serverLevel, pos);
        boolean crossed = false;

        if (!unbreakable && mirror != null) {
            if (inMirrorWorld || mirror.isDemonKilled()) {
                crossed = tryHorizontalBridge(serverLevel, pos, facing, entity);
            }
            if (!crossed && inMirrorWorld) {
                crossed = tryPoweredLocalTravel(serverLevel, pos, facing, entity);
            } else if (!crossed && mirror.isDemonKilled()) {
                crossed = tryVerticalBridge(serverLevel, pos, facing, entity);
            }
        }

        if (!crossed && inMirrorWorld && entity instanceof ServerPlayer player) {
            if (unbreakable) {
                // Unbreakable entry mirror → exit back to the linked dimension/position
                crossed = MirrorHelper.exitMirrorWorld(player, serverLevel, pos);
            } else {
                // Ordinary mirrors in this dimension only provide local bridges.
                crossed = false;
            }
        } else if (!crossed && !inMirrorWorld && entity instanceof ServerPlayer player) {
            crossed = MirrorHelper.enterMirrorWorld(player, serverLevel, pos);
        }

        if (crossed) {
            data.putLong("MirrorTeleportCooldown", level.getGameTime());
            level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    private static boolean tryHorizontalBridge(ServerLevel level, BlockPos sourceFoot,
                                               Direction facing, Entity entity) {
        Direction travel = facing.getOpposite();
        for (int distance = 1; distance < 32; distance++) {
            BlockPos check = sourceFoot.relative(travel, distance);
            BlockState target = level.getBlockState(check);
            if (!target.is(WitcheryBlocks.MIRROR.get())) continue;
            BlockPos targetFoot = target.getValue(PART) == BedPart.HEAD ? check.below() : check;
            BlockState targetFootState = level.getBlockState(targetFoot);
            if (targetFootState.getValue(FACING) == facing.getOpposite()) {
                return teleportWithinLevel(entity, targetFoot, targetFootState.getValue(FACING),
                        targetFoot.getY(), false);
            }
        }
        return false;
    }

    private static boolean tryPoweredLocalTravel(ServerLevel level, BlockPos sourceFoot,
                                                  Direction facing, Entity entity) {
        Direction travel = facing.getOpposite();
        int topY = sourceFoot.getY() + 1;
        for (int distance = 1; distance < 10; distance++) {
            BlockPos destination = sourceFoot.relative(travel, distance).atY(topY);
            if (!level.getBlockState(destination).isAir()
                    || !level.getBlockState(destination.below()).isAir()) continue;

            int chunkX = Math.floorDiv(destination.getX(), 16);
            int sectionY = com.emoniph.witchery.world.MirrorWorldLayout.sectionY(destination.getY());
            int chunkZ = Math.floorDiv(destination.getZ(), 16);
            BlockPos roomEntry = com.emoniph.witchery.world.MirrorWorldLayout.innerMirrorTop(
                    chunkX, sectionY, chunkZ);
            boolean occupiedEntry = level.getBlockState(roomEntry).is(WitcheryBlocks.MIRROR.get())
                    && level.getBlockState(roomEntry).getValue(UNBREAKABLE);
            boolean ownEntry = entity instanceof ServerPlayer player
                    && WitcheryAttachments.get(player).getCurrentMirrorRoute()
                    .map(route -> com.emoniph.witchery.world.MirrorWorldLayout.roomForIndex(
                            route.destinationRoomIndex()))
                    .map(room -> com.emoniph.witchery.world.MirrorWorldLayout.innerMirrorTop(
                            room.chunkX(), room.sectionY(), room.chunkZ()))
                    .filter(destination::equals).isPresent();
            if (occupiedEntry && !ownEntry) return false;

            AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(level, sourceFoot);
            if (altar == null || !altar.consumePower(3000)) return false;
            return teleportWithinLevel(entity, destination, travel, destination.getY(), false);
        }
        return false;
    }

    private static boolean tryVerticalBridge(ServerLevel level, BlockPos sourceFoot,
                                              Direction facing, Entity entity) {
        for (int verticalDirection : new int[]{1, -1}) {
            for (int distance = 2; distance < 16; distance++) {
                BlockPos check = sourceFoot.offset(0, verticalDirection * distance, 0);
                BlockState target = level.getBlockState(check);
                if (!target.is(WitcheryBlocks.MIRROR.get())) continue;
                BlockPos targetFoot = target.getValue(PART) == BedPart.HEAD ? check.below() : check;
                BlockState targetFootState = level.getBlockState(targetFoot);
                if (targetFootState.getValue(FACING) == facing) {
                    return teleportWithinLevel(entity, targetFoot, facing, targetFoot.getY(), true);
                }
            }
        }
        return false;
    }

    private static boolean teleportWithinLevel(Entity entity, BlockPos destination,
                                                Direction emergeFacing, double y,
                                                boolean reversePlayer) {
        double x = destination.getX() + 0.5D + emergeFacing.getStepX() * 0.7D;
        double z = destination.getZ() + 0.5D + emergeFacing.getStepZ() * 0.7D;
        entity.teleportTo(x, y, z);
        if (reversePlayer && entity instanceof ServerPlayer player) {
            player.setYRot(player.getYRot() + 180.0F);
        }
        return true;
    }

    // ── Breaking ─────────────────────────────────────────────────────────────

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (state.getValue(UNBREAKABLE)) return -1.0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, net.minecraft.util.RandomSource random) {
        BedPart part = state.getValue(PART);
        if (part == BedPart.FOOT && direction == Direction.UP && !neighborState.is(this)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (part == BedPart.HEAD && direction == Direction.DOWN && !neighborState.is(this)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BedPart part = state.getValue(PART);
        BlockPos other = part == BedPart.FOOT ? pos.above() : pos.below();
        BlockPos foot = part == BedPart.HEAD ? pos.below() : pos;
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                && !state.getValue(UNBREAKABLE)
                && level.getBlockEntity(foot) instanceof MirrorBlockEntity mirror) {
            MirrorHelper.disconnectSourceMirror(serverLevel, mirror);
            if (!player.getAbilities().instabuild) {
                ItemStack drop = new ItemStack(WitcheryItems.MIRROR.get());
                drop.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                        net.minecraft.world.item.component.TypedEntityData.of(
                                WitcheryBlockEntities.MIRROR.get(),
                                mirror.saveCustomOnly(level.registryAccess())));
                Block.popResource(level, foot, drop);
            }
        }
        BlockState otherState = level.getBlockState(other);
        if (otherState.is(this) && otherState.getValue(PART) != part) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, other, Block.getId(otherState));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ── Block entity ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == BedPart.FOOT) {
            return new MirrorBlockEntity(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || state.getValue(PART) != BedPart.FOOT) {
            return null;
        }
        if (type != WitcheryBlockEntities.MIRROR.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                MirrorBlockEntity.clientTick(
                        tickLevel, tickPos, tickState, (MirrorBlockEntity) blockEntity);
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) && adjacentState.getValue(FACING) == state.getValue(FACING)
                || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) { return true; }
}
