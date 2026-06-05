package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.UUID;

public class ThrownCatEntity extends PathfinderMob {
    private static final int MAX_LIFE_TICKS = 20 * 6;
    private static final int BLINDNESS_TICKS = 20 * 3;
    private static final int NAUSEA_TICKS = 20 * 4;
    private static final int SCRATCHED_TICKS = 20 * 60 * 5;

    @Nullable
    private UUID ownerUuid;

    public ThrownCatEntity(EntityType<? extends ThrownCatEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        setBaby(true);
        setNoAi(true);
        fallDistance = 0.0F;
        if (level().isClientSide()) {
            return;
        }
        if (tickCount > MAX_LIFE_TICKS || onGround()) {
            discard();
            return;
        }
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.25D),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            scratch(player);
            break;
        }
    }

    private void scratch(Player player) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner(serverLevel);
        DamageSource source = owner != null
                ? serverLevel.damageSources().mobAttack(owner)
                : serverLevel.damageSources().generic();
        player.hurtServer(serverLevel, source, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(WitcheryMobEffects.SCRATCHED, SCRATCHED_TICKS, 0, false, true));
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        discard();
    }

    @Nullable
    private LivingEntity getOwner(ServerLevel level) {
        if (ownerUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putString("Owner", ownerUuid.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String owner = tag.getStringOr("Owner", "");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
    }
}
