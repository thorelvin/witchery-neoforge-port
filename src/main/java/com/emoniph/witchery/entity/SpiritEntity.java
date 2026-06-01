package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Spirit - passive flying Spirit World mob. Flees from players and drops its matching subdued item.
 */
public class SpiritEntity extends Monster {
    private static final String TAG_SPIRIT_TYPE = "SpiritType";
    private static final int SPIRIT_TYPE_NORMAL = 0;
    private static final int SPIRIT_TYPE_VILLAGE = 1;

    private int spiritType = SPIRIT_TYPE_NORMAL;

    public SpiritEntity(EntityType<? extends SpiritEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER, -1f);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<Player>(this, Player.class, 8f, 1.0, 1.2));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        this.spawnAtLocation(level, new ItemStack(getSubduedDropItem(), 1));
    }

    public void setVillageTarget(boolean villageTarget) {
        this.spiritType = villageTarget ? SPIRIT_TYPE_VILLAGE : SPIRIT_TYPE_NORMAL;
    }

    public boolean isVillageTarget() {
        return spiritType == SPIRIT_TYPE_VILLAGE;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SPIRIT_TYPE, spiritType);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        spiritType = tag.getIntOr(TAG_SPIRIT_TYPE, SPIRIT_TYPE_NORMAL);
    }

    private net.minecraft.world.item.Item getSubduedDropItem() {
        return isVillageTarget()
                ? WitcheryItems.SUBDUED_VILLAGE_SPIRIT.get()
                : WitcheryItems.SUBDUED_SPIRIT.get();
    }

}
