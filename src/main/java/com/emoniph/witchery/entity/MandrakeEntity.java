package com.emoniph.witchery.entity;

import com.emoniph.witchery.item.EarmuffsItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Mandrake mob — tiny screaming creature that emerges from harvesting mandrake at night.
 * Keeps nearby players nauseated until it is killed, much closer to source.
 */
public class MandrakeEntity extends Monster {
    private static final int SCREAM_INTERVAL_TICKS = 10;
    private static final int SCREAM_DURATION_TICKS = 40;
    private static final int SCREAM_AMPLIFIER = 1;
    private static final double SCREAM_RADIUS = 8.0;

    public MandrakeEntity(EntityType<? extends MandrakeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.65)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 8f, 1.5, 1.5));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || tickCount % SCREAM_INTERVAL_TICKS != 0) {
            return;
        }

        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(SCREAM_RADIUS),
                LivingEntity::isAlive)) {
            if (EarmuffsItem.isWorn(player)) {
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SCREAM_DURATION_TICKS, SCREAM_AMPLIFIER, false, true));
        }
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
}
