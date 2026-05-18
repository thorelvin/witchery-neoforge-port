package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click on a Dream Catcher block to insert this weave. */
public class DreamWeaveItem extends Item {
    private final DreamWeaveType weaveType;

    public DreamWeaveItem(DreamWeaveType weaveType, Properties props) {
        super(props);
        this.weaveType = weaveType;
    }

    public DreamWeaveType getWeaveType() { return weaveType; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof DreamCatcherBlockEntity be))
            return InteractionResult.PASS;
        be.setWeave(weaveType);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative())
            ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
