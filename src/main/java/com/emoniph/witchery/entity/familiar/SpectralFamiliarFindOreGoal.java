package com.emoniph.witchery.entity.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Moves a spectral familiar onto the surface above a discovered ore column,
 * then lets it "mark" the spot for the owner.
 */
public class SpectralFamiliarFindOreGoal extends Goal {

    private final SpectralFamiliarEntity familiar;
    private final double speed;
    private int pathTicks;

    public SpectralFamiliarFindOreGoal(SpectralFamiliarEntity familiar, double speed) {
        this.familiar = familiar;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = this.familiar.getOwner();
        return owner != null
                && this.familiar.hasPendingSearch()
                && !this.familiar.isOrderedToSit()
                && this.familiar.distanceToSqr(owner) < 100.0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = this.familiar.getOwner();
        return owner != null
                && this.familiar.hasPendingSearch()
                && this.pathTicks <= 200
                && this.familiar.distanceToSqr(owner) < 100.0;
    }

    @Override
    public void start() {
        this.pathTicks = 0;
        moveTowardPerch();
    }

    @Override
    public void stop() {
        this.familiar.getNavigation().stop();
        if (this.familiar.hasPendingSearch()) {
            this.familiar.clearSearch();
        }
    }

    @Override
    public void tick() {
        this.pathTicks++;
        BlockPos perch = this.familiar.getSearchPerch();
        if (perch == null) {
            return;
        }

        if (this.familiar.distanceToSqr(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5) > 2.0) {
            moveTowardPerch();
            return;
        }

        this.familiar.finishSearch();
    }

    private void moveTowardPerch() {
        BlockPos perch = this.familiar.getSearchPerch();
        if (perch == null) {
            return;
        }
        this.familiar.getNavigation().moveTo(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5, this.speed);
    }
}
