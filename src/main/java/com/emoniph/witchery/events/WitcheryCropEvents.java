package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitcheryCropEvents {
    private static final int GRASS_SEED_CHANCE = 8;
    private static final int TOTAL_GRASS_SEED_WEIGHT = 14;

    @SubscribeEvent
    public static void onBreakGrass(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (event.getPlayer() == null || event.getPlayer().isCreative()) {
            return;
        }

        BlockState state = event.getState();
        if (!isGrassSeedSource(state)) {
            return;
        }

        ItemStack tool = event.getPlayer().getMainHandItem();
        if (tool.is(Items.SHEARS)) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextInt(GRASS_SEED_CHANCE) != 0) {
            return;
        }

        Block.popResource(level, event.getPos(), rollGrassSeed(random));
    }

    private static boolean isGrassSeedSource(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN);
    }

    private static ItemStack rollGrassSeed(RandomSource random) {
        int roll = random.nextInt(TOTAL_GRASS_SEED_WEIGHT);
        if (roll < 3) {
            return new ItemStack(WitcheryItems.ARTICHOKE_SEEDS.get());
        }
        if (roll < 7) {
            return new ItemStack(WitcheryItems.BELLADONNA_SEEDS.get());
        }
        if (roll < 12) {
            return new ItemStack(WitcheryItems.MANDRAKE_SEEDS.get());
        }
        return new ItemStack(WitcheryItems.SNOWBELL_SEEDS.get());
    }
}
