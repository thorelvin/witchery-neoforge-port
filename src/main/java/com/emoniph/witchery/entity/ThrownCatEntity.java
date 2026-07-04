package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class ThrownCatEntity extends PathfinderMob {
    private static final int MAX_LIFE_TICKS = 20 * 6;
    private static final int BLINDNESS_TICKS = 20 * 3;
    private static final int NAUSEA_TICKS = 20 * 4;
    private static final int SCRATCHED_TICKS = 20 * 60 * 5;
    private static final float SCRATCHED_HEALTH_PENALTY = 4.0F;

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

    public void launch(Vec3 motion) {
        setDeltaMovement(motion);
        hurtMarked = true;
    }

    @Override
    public void tick() {
        super.tick();
        setBaby(true);
        setNoAi(true);
        fallDistance = 0.0F;
        moveAsThrownCat();
        if (level().isClientSide()) {
            return;
        }
        if (tickCount > MAX_LIFE_TICKS || onGround() || horizontalCollision || verticalCollision) {
            discard();
            return;
        }
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.25D),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            scratch(player);
            break;
        }
    }

    private void moveAsThrownCat() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-7D) {
            return;
        }
        move(MoverType.SELF, motion);
        Vec3 next = getDeltaMovement();
        if (!isNoGravity()) {
            next = next.add(0.0D, -0.05D, 0.0D);
        }
        setDeltaMovement(next.scale(onGround() ? 0.45D : 0.98D));
        hurtMarked = true;
    }

    private void scratch(Player player) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner(serverLevel);
        DamageSource source = owner != null
                ? serverLevel.damageSources().mobAttack(owner)
                : serverLevel.damageSources().generic();
        boolean alreadyScratched = player.hasEffect(WitcheryMobEffects.SCRATCHED);
        float scratchedMaxHealth = Math.max(1.0F,
                player.getMaxHealth() - (alreadyScratched ? 0.0F : SCRATCHED_HEALTH_PENALTY));
        player.hurtServer(serverLevel, source, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS, 0, false, true));
        player.addEffect(new MobEffectInstance(WitcheryMobEffects.SCRATCHED, SCRATCHED_TICKS, 0, false, true));
        clampScratchedHealth(player, scratchedMaxHealth);
        discard();
    }

    private void clampScratchedHealth(Player player, float maxHealth) {
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetHealthPacket(serverPlayer.getHealth(),
                    serverPlayer.getFoodData().getFoodLevel(), serverPlayer.getFoodData().getSaturationLevel()));
        }
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
        try {
            ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            ownerUuid = null;
        }
    }
}
