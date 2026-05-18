package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Silver Vat — placed adjacent to a furnace smelting Gold Ore.
 * Each gold ore smelted has a 25% chance to deposit 1 silver ingot in the vat.
 * Right-click to collect. Up to 4 vats can be placed around one furnace for more yield.
 */
public class SilverVatBlock extends Block {

    private int storedSilver = 0; // per-instance (saved in block state or TE — simple approach: use level data)

    public SilverVatBlock(BlockBehaviour.Properties props) { super(props); }

    /**
     * Called by game events when a furnace completes a smelt.
     * WitcheryMobDropEvents hooks into LevelTickEvent to scan nearby vats.
     */
    public static void onFurnaceSmeltNearby(Level level, BlockPos furnacePos, ItemStack input) {
        // Only trigger for gold ore variants
        if (!input.is(net.minecraft.tags.ItemTags.create(
                net.minecraft.resources.Identifier.parse("minecraft:ores")))) return;
        if (!input.is(Blocks.GOLD_ORE.asItem()) && !input.is(Blocks.DEEPSLATE_GOLD_ORE.asItem())
                && !input.is(Blocks.NETHER_GOLD_ORE.asItem())) return;

    }

    /**
     * Simplified: when right-clicked, check if adjacent furnace is smelting gold ore,
     * accumulate silver and give it to player.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int collected = scanAndCollect(level, pos);
        if (collected > 0) {
            ItemStack silver = new ItemStack(WitcheryItems.SILVER_INGOT.get(), collected);
            if (!player.getInventory().add(silver)) {
                player.drop(silver, false);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(Component.translatable("witchery.silver_vat.collected", collected), false);
        } else {
            player.displayClientMessage(Component.translatable("witchery.silver_vat.empty"), false);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Scan adjacent furnaces. For each furnace actively smelting a gold ore,
     * roll 25% chance per completed smelt cycle to produce silver.
     * Simplified: if adjacent furnace has gold ore as fuel input, produce 1 silver.
     */
    private int scanAndCollect(Level level, BlockPos vatPos) {
        int total = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = vatPos.relative(dir);
            BlockState nbState = level.getBlockState(neighbor);
            if (!(nbState.getBlock() instanceof AbstractFurnaceBlock)) continue;
            if (!(level.getBlockEntity(neighbor) instanceof AbstractFurnaceBlockEntity furnace)) continue;

            // Check if the furnace input slot contains gold ore
            ItemStack inputSlot = furnace.getItem(0);
            boolean hasGoldOre = inputSlot.is(Blocks.GOLD_ORE.asItem())
                    || inputSlot.is(Blocks.DEEPSLATE_GOLD_ORE.asItem())
                    || inputSlot.is(Blocks.NETHER_GOLD_ORE.asItem());

            if (hasGoldOre && level.random.nextFloat() < 0.25f) {
                total++;
            }
        }
        return total;
    }
}
