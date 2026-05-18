package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.HobgoblinEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Statue of Hobgoblin Patron — nearby hobgoblins worship it instead of their natural shrine.
 * Right-click to collect worship power (stealthy prayer theft).
 * Each use has a chance to anger Gulg or Mog, who send avatars to attack the witch.
 */
public class StatueHobgoblinPatronBlock extends Block {

    private static final int SEARCH_RADIUS = 16;
    private static final float ANGER_CHANCE = 0.25f; // 25% chance each use angers a god

    public StatueHobgoblinPatronBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        // Count worshipping hobgoblins nearby
        AABB range = new AABB(pos).inflate(SEARCH_RADIUS);
        List<HobgoblinEntity> hobgoblins = sl.getEntitiesOfClass(HobgoblinEntity.class, range);

        if (hobgoblins.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.statue_patron.no_worshippers"), false);
            return InteractionResult.FAIL;
        }

        // Grant a brief power boost based on hobgoblin count
        int power = hobgoblins.size();
        player.displayClientMessage(Component.translatable("witchery.statue_patron.power_gained", power), false);

        // Chance to anger Gulg or Mog
        if (sl.getRandom().nextFloat() < ANGER_CHANCE) {
            boolean gulg = sl.getRandom().nextBoolean();
            player.displayClientMessage(Component.translatable(
                    gulg ? "witchery.statue_patron.gulg_angered" : "witchery.statue_patron.mog_angered"), false);
            // All nearby hobgoblins turn hostile to the player temporarily
            for (HobgoblinEntity hob : hobgoblins) {
                hob.setTarget(player);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
