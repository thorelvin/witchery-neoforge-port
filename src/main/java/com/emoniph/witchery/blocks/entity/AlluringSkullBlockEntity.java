package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.AlluringSkullBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AlluringSkullBlockEntity extends BlockEntity {

    private static final int TICK_INTERVAL = 100;
    private static final double RANGE = 64.0;
    private static final double HEIGHT = 10.0;

    public AlluringSkullBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.ALLURING_SKULL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlluringSkullBlockEntity blockEntity) {
        if (!state.getValue(AlluringSkullBlock.ON)) {
            return;
        }
        if (level.getGameTime() % TICK_INTERVAL != 0) {
            return;
        }

        AABB bounds = new AABB(pos).inflate(RANGE, HEIGHT, RANGE);
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, bounds)) {
            if (!mob.getType().is(EntityTypeTags.UNDEAD) || !mob.isAlive()) {
                continue;
            }
            mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Math.max(1.0, mob.getSpeed()));
        }
    }
}
