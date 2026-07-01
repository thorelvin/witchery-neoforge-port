package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * Walking through from inside the Mirror World returns to the Overworld.
 */
public class MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<MirrorBlock> CODEC = simpleCodec(MirrorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    public static final EnumProperty<BedPart>  PART        = BlockStateProperties.BED_PART;
    public static final BooleanProperty        UNBREAKABLE = BooleanProperty.create("unbreakable");

    // Thin-slab shapes: slab is pressed against the named wall.
    // NORTH wall = low-Z side (z=0-4), SOUTH wall = high-Z side (z=12-16),
    // WEST wall  = low-X side (x=0-4), EAST wall  = high-X side (x=12-16).
    private static final VoxelShape NORTH = Block.box(0,  0, 0, 16, 16,  4);
    private static final VoxelShape SOUTH = Block.box(0,  0, 12, 16, 16, 16);
    private static final VoxelShape WEST  = Block.box(0,  0, 0,  4, 16, 16);
    private static final VoxelShape EAST  = Block.box(12, 0, 0, 16, 16, 16);

    // Entry zone shapes (slightly thinner) for walk-through detection
    private static final VoxelShape NORTH_ENTRY = Block.box(0,  0, 0, 16, 16,  3);
    private static final VoxelShape SOUTH_ENTRY = Block.box(0,  0, 13, 16, 16, 16);
    private static final VoxelShape WEST_ENTRY  = Block.box(0,  0, 0,  3, 16, 16);
    private static final VoxelShape EAST_ENTRY  = Block.box(13, 0, 0, 16, 16, 16);

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
        Direction facing = ctx.getHorizontalDirection();
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
        BlockPos top = pos.above();
        level.setBlockAndUpdate(top, state.setValue(PART, BedPart.HEAD));
    }

    // ── Shapes ───────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            default    -> EAST;
        };
    }

    private static VoxelShape entryShapeFor(Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH_ENTRY;
            case SOUTH -> SOUTH_ENTRY;
            case WEST  -> WEST_ENTRY;
            default    -> EAST_ENTRY;
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
                || !mirror.hasRoom() || mirror.isDemonKilled()) {
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
        String speech = mirrorRightClickSpeech(serverLevel, footPos, player, mirrorWorld, unbreakable);
        player.displayClientMessage(NpcBrainService.speechComponent("Mirror", 0x55FFFF, speech), false);
        serverLevel.playSound(null, footPos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 0.45F, 0.72F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                8, 0.25D, 0.35D, 0.25D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                footPos.getX() + 0.5D, footPos.getY() + 1.0D, footPos.getZ() + 0.5D,
                6, 0.3D, 0.35D, 0.3D, 0.01D);
        return InteractionResult.CONSUME;
    }

    private static String mirrorRightClickSpeech(ServerLevel level, BlockPos footPos, Player player,
                                                 boolean mirrorWorld, boolean unbreakable) {
        if (mirrorWorld && unbreakable) {
            return "The way home waits behind the silver, " + player.getName().getString() + ".";
        }
        if (mirrorWorld) {
            return "A deeper room clouds the glass. It is listening.";
        }
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
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        // Only trigger from the FOOT half to avoid double-fire
        if (state.getValue(PART) != BedPart.FOOT) return;

        // Cooldown via player's persistent data
        var cd = player.getPersistentData();
        long lastTeleport = cd.getLong("MirrorTeleportCooldown").orElse(0L);
        if (level.getGameTime() - lastTeleport < 40) return;

        // Check entity is in the entry zone
        Direction facing = state.getValue(FACING);
        VoxelShape entryZone = entryShapeFor(facing);
        Vec3 epos = entity.position();
        double lx = epos.x - pos.getX();
        double ly = epos.y - pos.getY();
        double lz = epos.z - pos.getZ();
        if (!entryZone.bounds().inflate(0.1).contains(lx, ly, lz)) return;

        cd.putLong("MirrorTeleportCooldown", level.getGameTime());

        ServerLevel serverLevel = (ServerLevel) level;

        if (WitcheryDimensions.isMirrorWorld(level)) {
            if (state.getValue(UNBREAKABLE)) {
                // Unbreakable entry mirror → exit back to the linked dimension/position
                MirrorHelper.exitMirrorWorld(player, serverLevel, pos);
            } else {
                // Regular mirror placed inside mirror world → descend into a new room
                MirrorHelper.enterDeepMirrorRoom(player, serverLevel, pos);
            }
        } else {
            MirrorHelper.enterMirrorWorld(player, serverLevel, pos);
        }

        level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5f, 1.0f);
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

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return adjacentState.is(this) && adjacentState.getValue(FACING) == state.getValue(FACING)
                || super.skipRendering(state, adjacentState, side);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) { return true; }
}
