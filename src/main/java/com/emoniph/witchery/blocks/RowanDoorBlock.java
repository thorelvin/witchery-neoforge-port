package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.RowanDoorBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.RowanDoorKeyItem;
import com.emoniph.witchery.item.RowanKeyringItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A lockable door made from Rowan Wood.
 * Placing it gives the player a Rowan Door Key bound to this specific door.
 * Only the holder of the matching key (or keyring) can open/close it.
 */
public class RowanDoorBlock extends DoorBlock implements EntityBlock {

    public RowanDoorBlock(BlockBehaviour.Properties props) {
        super(BlockSetType.OAK, props);
    }

    // ── Block entity — only LOWER half gets one ───────────────────────────────

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new RowanDoorBlockEntity(pos, state) : null;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    // ── On placement: give the player their key ───────────────────────────────

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (!(placer instanceof Player player)) return;

        // Generate UUID and store in LOWER half's BE
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (level.getBlockEntity(lowerPos) instanceof RowanDoorBlockEntity be) {
            UUID doorUUID = UUID.randomUUID();
            be.setDoorUUID(doorUUID);

            ItemStack key = new ItemStack(WitcheryItems.ROWAN_DOOR_KEY.get());
            RowanDoorKeyItem.bindKey(key, doorUUID);
            player.addItem(key);
        }
    }

    // ── Locked interaction ────────────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.CONSUME;

        // Normalize to LOWER half for UUID lookup
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        UUID doorUUID = getDoorUUID(level, lowerPos);

        if (doorUUID != null && !playerHasKey(player, doorUUID)) {
            player.displayClientMessage(Component.translatable("block.witchery.rowan_door.locked"), true);
            return InteractionResult.SUCCESS;
        }

        // Key found (or no UUID): toggle open via parent's setOpen (handles sound + game event)
        this.setOpen(player, level, state, pos, !this.isOpen(state));
        return (level.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS_SERVER : net.minecraft.world.InteractionResult.SUCCESS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static UUID getDoorUUID(Level level, BlockPos lowerPos) {
        if (level.getBlockEntity(lowerPos) instanceof RowanDoorBlockEntity be) {
            return be.getDoorUUID().orElse(null);
        }
        return null;
    }

    private static boolean playerHasKey(Player player, UUID doorUUID) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(WitcheryItems.ROWAN_DOOR_KEY.get())   && RowanDoorKeyItem.matchesDoor(s, doorUUID)) return true;
            if (s.is(WitcheryItems.ROWAN_KEYRING.get())    && RowanKeyringItem.hasKey(s, doorUUID))       return true;
        }
        return false;
    }
}
