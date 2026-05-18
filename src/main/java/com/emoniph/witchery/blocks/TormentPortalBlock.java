package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.world.TormentHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.InsideBlockEffectApplier;

/**
 * Torment Portal — a 1×2 red portal block placed at the exit of each maze level.
 *
 * Collision behaviour:
 *   • Outside Torment → send player to a random Torment level.
 *   • Inside  Torment → 95% chance return to Overworld; 5% chance send to another level.
 * A 100-tick cooldown prevents portal loops.
 */
public class TormentPortalBlock extends Block {

    public TormentPortalBlock(Properties props) {
        super(props);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        var data = WitcheryAttachments.get(player);
        if (data.getTormentPortalCooldown() > 0) return;
        data.setTormentPortalCooldown(100);

        if (level.dimension().equals(WitcheryDimensions.TORMENT)) {
            // Inside Torment: 95 % exit, 5 % more Torment
            if (level.random.nextFloat() < 0.95f) {
                TormentHelper.returnFromTorment(player);
            } else {
                TormentHelper.sendToTorment(player, level.random.nextInt(TormentHelper.NUM_LEVELS));
            }
        } else {
            TormentHelper.sendToTorment(player, level.random.nextInt(TormentHelper.NUM_LEVELS));
        }
    }
}
