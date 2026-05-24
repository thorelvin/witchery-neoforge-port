package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BroomEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import net.minecraft.world.InteractionResult;

/**
 * Broom item — right-click to mount. Spawns a new broom entity in the world
 * or re-mounts a nearby one if the player has dismounted.
 */
public class BroomItem extends Item {

    public BroomItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.is(WitcheryItems.ENCHANTED_BROOM.get()) || super.isFoil(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel sl = (ServerLevel) level;

        // Try to re-mount any nearby riderless broom within 4 blocks
        AABB searchBox = player.getBoundingBox().inflate(4.0);
        List<BroomEntity> nearby = sl.getEntitiesOfClass(BroomEntity.class, searchBox,
                b -> b.getPassengers().isEmpty());
        if (!nearby.isEmpty()) {
            BroomEntity broom = nearby.get(0);
            player.startRiding(broom, true, true);
            return InteractionResult.SUCCESS_SERVER;
        }

        // No nearby broom — spawn a new one just above and in front of the player
        var look = player.getLookAngle();
        BroomEntity broom = BroomEntity.create(sl,
                player.getX() + look.x * 1.5,
                player.getY() + 0.5,
                player.getZ() + look.z * 1.5);
        ItemStack stack = player.getItemInHand(hand);
        broom.setEnchanted(stack.is(WitcheryItems.ENCHANTED_BROOM.get()));
        broom.setYRot(player.getYRot());
        sl.addFreshEntity(broom);
        player.startRiding(broom, true, true);

        // Consume the item (one broom per use in survival)
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
