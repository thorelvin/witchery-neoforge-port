package com.emoniph.witchery.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

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

    public static final float SOURCE_WIDTH = 1.2F;
    public static final float SOURCE_HEIGHT = 0.5F;
    public static final float SOURCE_SHADOW_RADIUS = 0.0F;
    public static final float SOURCE_RENDER_X_OFFSET = 0.9F;
    public static final float SOURCE_RENDER_Y_OFFSET = 0.25F;
    public static final float SOURCE_PRONE_ROTATION_DEGREES = 90.0F;
    public static final double SOURCE_MOVEMENT_SPEED = 1.0D;
    /** Modern readability adaptation for the owner custom name assigned by the source entry path. */
    public static final boolean OWNER_NAMEPLATE_VISIBLE = true;
    private static final String SOURCE_OWNER_TAG = "Owner";

    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    // UUID is synced as its string form — this mapping has no UUID/Optional<UUID> data serializer.
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
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
    public void move(MoverType type, Vec3 movement) {
        // EntityCorpse overrides movement entirely: the body cannot fall, drift, or be knocked away.
    }

    @Override
    public void tick() {
        super.tick();
        // Self-clean: once the owner is back from the Spirit World, the body must not linger. The
        // explicit removal in SpiritWorldHelper.exit() can miss a body whose chunk had unloaded
        // during the trip (it is not in the entity lookup, and a force-loaded chunk's entities are
        // not queryable the same tick), so it would reappear when the player teleports home. This
        // catches that case — and any orphaned bodies from earlier trips — whenever the body is
        // loaded with its owner online and no longer dreaming.
        if (!this.level().isClientSide() && (this.tickCount & 7) == 0) {
            discardIfOwnerReturned();
        }
    }

    private void discardIfOwnerReturned() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = findOnlineOwner(level);
        // Owner offline: keep the body until they return and we can tell whether they are still dreaming.
        if (owner != null) {
            if (getOwnerUUID().isEmpty()) {
                this.entityData.set(DATA_OWNER_UUID, owner.getUUID().toString());
            }
            if (!com.emoniph.witchery.init.WitcheryAttachments.get(owner).isInSpiritWorld()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Immovable sleeping body.
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, SOURCE_MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setOwner(ServerPlayer player) {
        this.entityData.set(DATA_OWNER_UUID, player.getUUID().toString());
        this.entityData.set(DATA_OWNER_NAME, player.getGameProfile().name());
        this.setCustomName(Component.literal(player.getGameProfile().name()));
        this.setCustomNameVisible(OWNER_NAMEPLATE_VISIBLE);
        this.setPersistenceRequired();
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

    public static double sourceSpawnCenter(double coordinate) {
        return Mth.floor(coordinate) + 0.5D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        ServerPlayer owner = findOnlineOwner(level);

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

    private ServerPlayer findOnlineOwner(ServerLevel level) {
        ServerPlayer owner = getOwnerUUID()
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .orElse(null);
        if (owner == null && !getOwnerName().isBlank()) {
            owner = level.getServer().getPlayerList().getPlayerByName(getOwnerName());
        }
        return owner;
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
        tag.putString(SOURCE_OWNER_TAG, getOwnerName());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_OWNER_UUID, tag.getStringOr("OwnerUUID", ""));
        String ownerName = tag.getStringOr("OwnerName", tag.getStringOr(SOURCE_OWNER_TAG, ""));
        this.entityData.set(DATA_OWNER_NAME, ownerName);
        if (!ownerName.isBlank() && !this.hasCustomName()) {
            this.setCustomName(Component.literal(ownerName));
            this.setCustomNameVisible(OWNER_NAMEPLATE_VISIBLE);
        }
        if (!ownerName.isBlank()) {
            this.setPersistenceRequired();
        }
    }
}
