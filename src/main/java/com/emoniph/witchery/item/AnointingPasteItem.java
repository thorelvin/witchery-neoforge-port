package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.WitchCauldronBlock;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Anointing Paste - right-click a vanilla Cauldron (empty or water-filled) to
 * convert it into a Witch's Cauldron, preserving the water level.
 *
 * Source behaviour: ItemAnointingPaste.onItemUse (WitcheryResurrected 1.12.2).
 * Particles and sound match the Enchanted reference port.
 */
public class AnointingPasteItem extends Item {

    public AnointingPasteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos  = ctx.getClickedPos();
        BlockState clicked = level.getBlockState(pos);

        int waterLevel = getVanillaWaterLevel(clicked);
        if (waterLevel < 0) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            // Client: feedback only (particles + sound)
            spawnAnointingParticles(level, pos);
            level.playSound(
                    ctx.getPlayer(), pos,
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            return InteractionResult.SUCCESS;
        }

        // Server: convert block, consume item, award stat
        BlockState witchCauldron = WitcheryBlocks.WITCH_CAULDRON.get()
                .defaultBlockState()
                .setValue(WitchCauldronBlock.LEVEL, waterLevel);
        level.setBlockAndUpdate(pos, witchCauldron);
        level.gameEvent(ctx.getPlayer(), GameEvent.BLOCK_CHANGE, pos);

        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Returns the water level to preserve, or -1 if this block is not an
     * anointable vanilla cauldron.
     */
    private static int getVanillaWaterLevel(BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            return 0; // empty cauldron -> level 0
        }
        if (state.is(Blocks.WATER_CAULDRON)) {
            return state.getValue(LayeredCauldronBlock.LEVEL); // 1, 2, or 3
        }
        return -1; // lava/powder-snow cauldrons -> not anointable
    }

    /** Spawn a burst of witch particles around the cauldron (client-side only). */
    private static void spawnAnointingParticles(Level level, BlockPos pos) {
        for (int i = 0; i < 24; i++) {
            double x = pos.getX() - 0.5 + level.getRandom().nextDouble() * 2.0;
            double y = pos.getY()       + level.getRandom().nextDouble() * 1.5;
            double z = pos.getZ() - 0.5 + level.getRandom().nextDouble() * 2.0;
            level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, 0.06, 0.0);
        }
    }
}
