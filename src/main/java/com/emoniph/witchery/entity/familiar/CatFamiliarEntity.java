package com.emoniph.witchery.entity.familiar;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

/**
 * Cat familiar — follows the owner and avoids phantoms and creepers.
 */
public class CatFamiliarEntity extends FamiliarEntity {

    public CatFamiliarEntity(EntityType<? extends CatFamiliarEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new AvoidEntityGoal<Phantom>(this, Phantom.class, 16.0F, 1.0, 1.2));
        this.goalSelector.addGoal(4, new AvoidEntityGoal<Creeper>(this, Creeper.class, 10.0F, 1.0, 1.2));
    }
}
