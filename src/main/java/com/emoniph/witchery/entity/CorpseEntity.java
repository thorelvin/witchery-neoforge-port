package com.emoniph.witchery.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 *
 * <p>The owner's UUID and name are synced to the client so the corpse can be rendered with the
 * owner's own player skin (source {@code EntityCorpse} resolves the skin from the owner username).
 */
public class CorpseEntity extends Mob {

    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    // UUID is synced as its string form — this mapping has no UUID/Optional<UUID> data serializer.
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
        this.setPose(Pose.SLEEPING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_OWNER_UUID, "");
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
        this.entityData.set(DATA_OWNER_UUID, player.getUUID().toString());
        this.entityData.set(DATA_OWNER_NAME, player.getGameProfile().name());
    }

    public Optional<UUID> getOwnerUUID() {
        String uuidStr = this.entityData.get(DATA_OWNER_UUID);
        if (uuidStr.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String getOwnerName() {
        return this.entityData.get(DATA_OWNER_NAME);
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
            owner.displayClientMessage(Component.translatable("witchery.spirit_world.body_destroyed")
                    .withStyle(ChatFormatting.DARK_RED), false);
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
        getOwnerUUID().ifPresent(uuid -> tag.putString("OwnerUUID", uuid.toString()));
        tag.putString("OwnerName", getOwnerName());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_OWNER_UUID, tag.getStringOr("OwnerUUID", ""));
        this.entityData.set(DATA_OWNER_NAME, tag.getStringOr("OwnerName", ""));
    }
}
