package com.emoniph.witchery.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/**
 * Sleeping body that persists in the Overworld while a player is in the Spirit World.
 * If the corpse is killed, the owner is forcibly expelled from the Spirit World.
 */
public class CorpseEntity extends Mob {

    private String ownerUUID = "";

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
        this.setPose(Pose.SLEEPING);
    }

    @Override
    protected void registerGoals() {
        // No AI.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Immovable sleeping body.
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setOwner(ServerPlayer player) {
        this.ownerUUID = player.getStringUUID();
    }

    public Optional<UUID> getOwnerUUID() {
        if (ownerUUID.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(ownerUUID));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        ServerPlayer owner = getOwnerUUID()
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);

        if (!(source.getEntity() instanceof ServerPlayer attacker && attacker.isCreative()) && owner == null) {
            return false;
        }

        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && !this.isAlive() && owner != null) {
            owner.displayClientMessage(
                    Component.literal("§4Your sleeping body was destroyed! You are torn back..."), false);
            com.emoniph.witchery.spiritworld.SpiritWorldHelper.exit(owner);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // No drops or XP from a sleeping body.
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("OwnerUUID", ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        ownerUUID = tag.getStringOr("OwnerUUID", "");
    }
}
