package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Brew of Substitution - source-style dropped-block substitution in practical item form. */
public class BrewOfSubstitutionItem extends BrewItem {
    private static final int NORMAL_RADIUS = 4;
    private static final int ENHANCED_RADIUS = 6;

    public BrewOfSubstitutionItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int replaced = substituteBlocks(serverLevel, context.getClickedPos(), hasBrewMastery(player));
        if (replaced <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!level.isClientSide()) {
            shuffleFallback(player, level);
        }
    }

    private static boolean hasBrewMastery(Player player) {
        return player != null && FamiliarHelper.hasActiveToadFamiliar(player);
    }

    private static int substituteBlocks(ServerLevel level, BlockPos origin, boolean enhanced) {
        BlockState referenceState = level.getBlockState(origin);
        Block referenceBlock = referenceState.getBlock();
        ItemStack displacedStack = new ItemStack(referenceBlock.asItem());
        if (referenceState.isAir()
                || referenceState.hasBlockEntity()
                || displacedStack.isEmpty()
                || referenceState.getDestroySpeed(level, origin) < 0.0F) {
            return 0;
        }

        int radius = enhanced ? ENHANCED_RADIUS : NORMAL_RADIUS;
        List<ItemEntity> blockItems = collectBlockItems(level, origin, radius);
        if (blockItems.isEmpty()) {
            return 0;
        }

        List<BlockPos> scan = scanPositions(origin, radius);
        int replaced = 0;
        int itemIndex = 0;
        for (BlockPos scanPos : scan) {
            if (itemIndex >= blockItems.size()) {
                break;
            }

            BlockPos target = findReplaceableReference(level, scanPos, referenceBlock);
            if (target == null) {
                continue;
            }

            ItemEntity itemEntity = blockItems.get(itemIndex);
            ItemStack itemStack = itemEntity.getItem();
            if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
                itemIndex++;
                continue;
            }

            BlockState replacement = blockItem.getBlock().defaultBlockState();
            level.setBlock(target, replacement, 3);
            level.sendParticles(ParticleTypes.WITCH,
                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                    8, 0.25D, 0.25D, 0.25D, 0.02D);
            replaced++;

            itemStack.shrink(1);
            if (itemStack.isEmpty()) {
                itemEntity.discard();
                itemIndex++;
            } else {
                itemEntity.setItem(itemStack);
            }
        }

        if (replaced > 0) {
            dropDisplacedBlocks(level, origin, displacedStack, replaced);
            level.playSound(null, origin, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        return replaced;
    }

    private static List<ItemEntity> collectBlockItems(ServerLevel level, BlockPos origin, int radius) {
        double radiusSq = radius * radius;
        AABB area = new AABB(origin).inflate(radius);
        List<ItemEntity> result = new ArrayList<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (item.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D) <= radiusSq
                    && item.getItem().getItem() instanceof BlockItem) {
                result.add(item);
            }
        }
        result.sort(Comparator.comparingDouble(item ->
                item.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D)));
        return result;
    }

    private static List<BlockPos> scanPositions(BlockPos origin, int radius) {
        int radiusSq = radius * radius;
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < radiusSq) {
                        positions.add(origin.offset(dx, dy, dz));
                    }
                }
            }
        }
        positions.sort(Comparator.comparingInt(pos -> pos.distManhattan(origin)));
        return positions;
    }

    private static BlockPos findReplaceableReference(ServerLevel level, BlockPos scanPos, Block referenceBlock) {
        if (isReplaceableReference(level, scanPos, referenceBlock, scanPos.above())) {
            return scanPos;
        }
        if (isReplaceableReference(level, scanPos.above(), referenceBlock, scanPos.above(2))) {
            return scanPos.above();
        }
        if (isReplaceableReference(level, scanPos.below(), referenceBlock, scanPos)) {
            return scanPos.below();
        }
        if (isReplaceableReference(level, scanPos.above(2), referenceBlock, scanPos.above(3))) {
            return scanPos.above(2);
        }
        if (isReplaceableReference(level, scanPos.below(2), referenceBlock, scanPos.below())) {
            return scanPos.below(2);
        }
        return null;
    }

    private static boolean isReplaceableReference(ServerLevel level, BlockPos target, Block referenceBlock, BlockPos aboveTarget) {
        BlockState targetState = level.getBlockState(target);
        return targetState.is(referenceBlock)
                && !targetState.hasBlockEntity()
                && isReplaceable(level.getBlockState(aboveTarget));
    }

    private static boolean isReplaceable(BlockState state) {
        return state.isAir()
                || state.liquid()
                || (!state.isSolid() && !state.hasBlockEntity());
    }

    private static void dropDisplacedBlocks(ServerLevel level, BlockPos origin, ItemStack prototype, int count) {
        int remaining = count;
        while (remaining > 0) {
            int amount = Math.min(remaining, prototype.getMaxStackSize());
            remaining -= amount;
            ItemStack drop = prototype.copyWithCount(amount);
            level.addFreshEntity(new ItemEntity(level,
                    origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D,
                    drop));
        }
    }

    private static void shuffleFallback(Player player, Level level) {
        BlockPos center = player.blockPosition();
        int radius = 4;

        List<BlockPos> solid = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isSolid() && !state.hasBlockEntity()) {
                        solid.add(pos);
                    }
                }
            }
        }

        if (solid.size() < 2) {
            return;
        }

        List<BlockState> states = new ArrayList<>();
        for (BlockPos pos : solid) {
            states.add(level.getBlockState(pos));
        }

        Random rng = new Random(center.asLong());
        for (int i = states.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            BlockState tmp = states.get(i);
            states.set(i, states.get(j));
            states.set(j, tmp);
        }

        for (int i = 0; i < solid.size(); i++) {
            level.setBlockAndUpdate(solid.get(i), states.get(i));
        }
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
