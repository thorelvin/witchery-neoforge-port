package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CircleTalismanItem extends Item {

    // 0=empty, 1=small ring, 2=medium ring, 3=large ring, 4=center
    // Row 0 = south end, row 16 = north end; mirrored in Z during placement
    private static final int[][] PATTERN = {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,3,3,3,3,3,3,3,0,0,0,0,0},
        {0,0,0,0,3,0,0,0,0,0,0,0,3,0,0,0,0},
        {0,0,0,3,0,0,2,2,2,2,2,0,0,3,0,0,0},
        {0,0,3,0,0,2,0,0,0,0,0,2,0,0,3,0,0},
        {0,3,0,0,2,0,0,1,1,1,0,0,2,0,0,3,0},
        {0,3,0,2,0,0,1,0,0,0,1,0,0,2,0,3,0},
        {0,3,0,2,0,1,0,0,0,0,0,1,0,2,0,3,0},
        {0,3,0,2,0,1,0,0,4,0,0,1,0,2,0,3,0},
        {0,3,0,2,0,1,0,0,0,0,0,1,0,2,0,3,0},
        {0,3,0,2,0,0,1,0,0,0,1,0,0,2,0,3,0},
        {0,3,0,0,2,0,0,1,1,1,0,0,2,0,0,3,0},
        {0,0,3,0,0,2,0,0,0,0,0,2,0,0,3,0,0},
        {0,0,0,3,0,0,2,2,2,2,2,0,0,3,0,0,0},
        {0,0,0,0,3,0,0,0,0,0,0,0,3,0,0,0,0},
        {0,0,0,0,0,3,3,3,3,3,3,3,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
    };

    private static final int RADIUS = (PATTERN.length - 1) / 2; // 8

    public CircleTalismanItem(Properties props) { super(props.rarity(Rarity.UNCOMMON)); }

    // ── Data component helpers ────────────────────────────────────────────────

    public static void setCircleData(ItemStack stack, int data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("circle_data", data));
    }

    public static int getCircleData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("circle_data", 0);
    }

    @Override
    public Component getName(ItemStack stack) {
        int data = getCircleData(stack);
        if (data == 0) {
            return super.getName(stack);
        }
        return Component.literal("")
                .append(super.getName(stack))
                .append(Component.literal(" (" + describeCircleData(data) + ")"));
    }

    // ── Item use ──────────────────────────────────────────────────────────────

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int data = getCircleData(stack);
        if (data == 0) {
            BlockPos pos = existingCenter(level, clickedPos);
            if (pos == null) return InteractionResult.PASS;

            // Store mode: scan rings around the clicked center and pack into talisman
            int[] variants = RitualCircleDetector.detectAllVariants(level, pos);
            int packed = (variants[2] << 6) | (variants[1] << 3) | variants[0];
            if (packed == 0) {
                playFail(level, pos, player);
                return InteractionResult.FAIL;
            }
            setCircleData(stack, packed);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.5f, 1.2f);
            if (player != null)
                player.displayClientMessage(Component.translatable("witchery.talisman.stored"), false);
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = deploymentCenter(level, clickedPos);
        if (pos == null) {
            playFail(level, clickedPos, player);
            return InteractionResult.FAIL;
        }

        int small  = data & 7;
        int medium = (data >> 3) & 7;
        int large  = (data >> 6) & 7;

        // Pass 0: validate — check nothing solid occupies pattern positions
        if (!validate(level, pos, small, medium, large)) {
            playFail(level, pos, player);
            return InteractionResult.FAIL;
        }

        // Pass 1: place
        place(level, pos, small, medium, large);

        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.9f);
        if (player != null)
            player.displayClientMessage(Component.translatable("witchery.talisman.deployed"), false);
        resetAfterDeployment(stack, player);
        return InteractionResult.SUCCESS;
    }

    private BlockPos existingCenter(Level level, BlockPos clickedPos) {
        if (level.getBlockState(clickedPos).is(WitcheryBlocks.CIRCLE.get())) {
            return clickedPos;
        }
        BlockPos above = clickedPos.above();
        return level.getBlockState(above).is(WitcheryBlocks.CIRCLE.get()) ? above : null;
    }

    private BlockPos deploymentCenter(Level level, BlockPos clickedPos) {
        if (level.getBlockState(clickedPos).is(WitcheryBlocks.CIRCLE.get())) {
            return clickedPos;
        }
        BlockPos above = clickedPos.above();
        BlockState state = level.getBlockState(above);
        BlockState centerState = WitcheryBlocks.CIRCLE.get().defaultBlockState();
        return (state.isAir() || state.canBeReplaced()) && centerState.canSurvive(level, above) ? above : null;
    }

    private void resetAfterDeployment(ItemStack stack, Player player) {
        if (player == null || player.isCreative()) {
            return;
        }
        if (stack.getCount() > 1) {
            ItemStack blankTalisman = new ItemStack(this);
            stack.shrink(1);
            if (!player.getInventory().add(blankTalisman)) {
                player.drop(blankTalisman, false);
            }
        } else {
            setCircleData(stack, 0);
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validate(Level level, BlockPos center, int small, int medium, int large) {
        for (int z = 0; z < PATTERN.length - 1; z++) {
            for (int x = 0; x < PATTERN[z].length; x++) {
                int cell = PATTERN[PATTERN.length - 1 - z][x];
                if (cell == 0) continue;

                BlockPos worldPos = center.offset(x - RADIUS, 0, z - RADIUS);
                BlockState existing = level.getBlockState(worldPos);

                if (cell == 4) {
                    // Center must already be CIRCLE or we skip it
                    continue;
                }

                Block target = blockFor(cell, small, medium, large);
                if (target == null) return false;

                // Position must be air/replaceable OR already the correct block
                if (!existing.isAir() && !existing.canBeReplaced() && !existing.is(target)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ── Placement ────────────────────────────────────────────────────────────

    private void place(Level level, BlockPos center, int small, int medium, int large) {
        for (int z = 0; z < PATTERN.length - 1; z++) {
            for (int x = 0; x < PATTERN[z].length; x++) {
                int cell = PATTERN[PATTERN.length - 1 - z][x];
                if (cell == 0) continue;

                BlockPos worldPos = center.offset(x - RADIUS, 0, z - RADIUS);

                if (cell == 4) {
                    // Center — ensure it has a CIRCLE block
                    if (level.getBlockState(worldPos).isAir())
                        level.setBlock(worldPos, WitcheryBlocks.CIRCLE.get().defaultBlockState(), 3);
                    continue;
                }

                Block target = blockFor(cell, small, medium, large);
                if (target == null) continue;

                level.setBlock(worldPos, ChalkCircleBlock.withRandomGlyph(target.defaultBlockState(), level.getRandom()), 3);
                level.addParticle(ParticleTypes.POOF,
                        worldPos.getX() + 0.5, center.getY() + 1.1, worldPos.getZ() + 0.5,
                        0, 0.05, 0);
            }
        }
    }

    // ── Block lookup ──────────────────────────────────────────────────────────

    private static Block blockFor(int ringSize, int small, int medium, int large) {
        int variant = switch (ringSize) {
            case 1 -> small;
            case 2 -> medium;
            case 3 -> large;
            default -> 0;
        };
        return switch (variant) {
            case 1 -> WitcheryBlocks.CIRCLE_GLYPH.get();
            case 2 -> WitcheryBlocks.CIRCLE_OTHERWHERE.get();
            case 3 -> WitcheryBlocks.CIRCLE_INFERNAL.get();
            case 4 -> WitcheryBlocks.CIRCLE.get();
            default -> null;
        };
    }

    private static String describeCircleData(int data) {
        StringBuilder result = new StringBuilder();
        appendRing(result, "Small", data & 7);
        appendRing(result, "Medium", (data >> 3) & 7);
        appendRing(result, "Large", (data >> 6) & 7);
        return result.toString();
    }

    private static void appendRing(StringBuilder result, String size, int variant) {
        if (variant == 0) {
            return;
        }
        if (!result.isEmpty()) {
            result.append(", ");
        }
        result.append(size).append(' ').append(chalkName(variant));
    }

    private static String chalkName(int variant) {
        return switch (variant) {
            case 1 -> "Ritual";
            case 2 -> "Otherwhere";
            case 3 -> "Infernal";
            case 4 -> "Golden";
            default -> "Unknown";
        };
    }

    private void playFail(Level level, BlockPos pos, Player player) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS,
                0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
    }
}
