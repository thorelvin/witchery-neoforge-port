package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Thorns - source-style cactus columns in practical item form. */
public class BrewOfThornsItem extends BrewItem {
    private static final BlockPos[] ENTITY_COLUMN_OFFSETS = {
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    };

    private static final BlockPos[] DRINK_FALLBACK_OFFSETS = {
            new BlockPos(3, 0, 0),
            new BlockPos(-3, 0, 0),
            new BlockPos(0, 0, 3),
            new BlockPos(0, 0, -3),
            new BlockPos(2, 0, 2),
            new BlockPos(-2, 0, 2),
            new BlockPos(2, 0, -2),
            new BlockPos(-2, 0, -2)
    };

    public BrewOfThornsItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP && !level.getBlockState(clickedPos).is(Blocks.CACTUS)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int height = hasBrewMastery(player) ? 4 : 3;
        if (!plantCactus(serverLevel, clickedPos, height, player)) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        int height = hasBrewMastery(player) ? 2 : 1;
        boolean planted = false;
        BlockPos center = target.blockPosition();
        for (BlockPos offset : ENTITY_COLUMN_OFFSETS) {
            planted |= plantCactus(serverLevel, center.offset(offset), height, player);
        }
        if (!planted) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int height = hasBrewMastery(player) ? 4 : 3;
        BlockPos center = player.blockPosition();
        for (BlockPos offset : DRINK_FALLBACK_OFFSETS) {
            plantCactus(serverLevel, center.offset(offset), height, player);
        }
    }

    private static boolean hasBrewMastery(Player player) {
        return player != null && FamiliarHelper.hasActiveToadFamiliar(player);
    }

    public static boolean plantCactus(ServerLevel level, BlockPos pos, int height) {
        return plantCactus(level, pos, height, true);
    }

    public static boolean plantCactus(ServerLevel level, BlockPos pos, int height, boolean feedback) {
        return plantCactus(level, pos, height, null, feedback, false);
    }

    private static boolean plantCactus(ServerLevel level, BlockPos pos, int height, Player player) {
        return plantCactus(level, pos, height, player, true, true);
    }

    private static boolean plantCactus(ServerLevel level, BlockPos pos, int height, Player player, boolean feedback,
                                      boolean protect) {
        BlockPos ground = pos;
        if (!level.getBlockState(ground).isSolid()) {
            ground = ground.below();
        }

        BlockState groundState = level.getBlockState(ground);
        if (!canBecomeCactusBase(level, ground, groundState)) {
            return false;
        }

        if (groundState.is(Blocks.CACTUS)) {
            while (level.getBlockState(ground.above()).is(Blocks.CACTUS)) {
                ground = ground.above();
            }
        } else {
            if (protect && !WitcheryTerrainProtection.canTransform(player, level, ground)) {
                return false;
            }
            level.setBlock(ground, Blocks.SAND.defaultBlockState(), 3);
            if (feedback) {
                level.sendParticles(ParticleTypes.POOF,
                        ground.getX() + 0.5D, ground.getY() + 0.5D, ground.getZ() + 0.5D,
                        6, 0.2D, 0.2D, 0.2D, 0.02D);
            }
        }

        int placed = 0;
        for (int i = 1; i <= height && ground.getY() + i < level.getMaxY(); i++) {
            BlockPos cactusPos = ground.above(i);
            if (!setCactusIfNotSolid(level, cactusPos, player, feedback, protect)) {
                break;
            }
            placed++;
        }
        if (placed > 0 && feedback) {
            level.playSound(null, ground, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.7F, 0.8F);
        }
        return placed > 0;
    }

    private static boolean canBecomeCactusBase(ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(Blocks.CACTUS)
                || (!state.isAir()
                && state.isSolid()
                && !state.liquid()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0.0F);
    }

    private static boolean setCactusIfNotSolid(ServerLevel level, BlockPos pos, Player player, boolean feedback,
                                               boolean protect) {
        BlockState current = level.getBlockState(pos);
        BlockState cactus = Blocks.CACTUS.defaultBlockState();
        if (current.isSolid()
                || current.liquid()
                || current.hasBlockEntity()
                || !cactus.canSurvive(level, pos)) {
            return false;
        }
        if (protect && !(current.isAir()
                ? WitcheryTerrainProtection.canPlace(player, level, pos)
                : WitcheryTerrainProtection.canTransform(player, level, pos))) {
            return false;
        }

        level.setBlock(pos, cactus, 3);
        if (feedback) {
            level.sendParticles(ParticleTypes.POOF,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    8, 0.2D, 0.35D, 0.2D, 0.02D);
        }
        return true;
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
