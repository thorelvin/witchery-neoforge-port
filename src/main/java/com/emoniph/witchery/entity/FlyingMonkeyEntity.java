package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.WaystoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Source-shaped Flying Monkey with persistent taglock retrieval and waystone transport orders.
 */
public class FlyingMonkeyEntity extends TamableAnimal {
    private static final Ingredient SOURCE_FOOD = Ingredient.of(Items.BEEF, Items.PORKCHOP);
    private static final double COURIER_MAX_SPEED = 0.45D;
    private static final double PICKUP_RANGE_SQR = 2.25D;
    private static final double ARRIVAL_RANGE_SQR = 4.0D;
    private static final int TARGET_MISSING_TIMEOUT = 200;
    private static final int LANDING_SEARCH_RADIUS = 4;

    private static final String TAG_DYE_COLOR = "DyeColor";
    private static final String TAG_COURIER_STATE = "CourierState";
    private static final String TAG_COURIER_TARGET = "CourierTarget";
    private static final String TAG_COURIER_TARGET_NAME = "CourierTargetName";
    private static final String TAG_HOME_X = "CourierHomeX";
    private static final String TAG_HOME_Y = "CourierHomeY";
    private static final String TAG_HOME_Z = "CourierHomeZ";
    private static final String TAG_HOME_DIMENSION = "CourierHomeDimension";
    private static final String TAG_WAYSTONE_POS = "CourierWaystonePos";
    private static final String TAG_WAYSTONE_DIMENSION = "CourierWaystoneDimension";
    private static final String TAG_WAYSTONE_TRACKS_ENTITY = "CourierWaystoneTracksEntity";
    private static final String TAG_TIME_TO_LIVE = "SuicideIn";

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_DYE_COLOR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FlyingMonkeyEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    private int attackTimer;
    private CourierState courierState = CourierState.IDLE;
    @Nullable
    private UUID courierTargetUuid;
    private String courierTargetName = "";
    private double courierHomeX;
    private double courierHomeY;
    private double courierHomeZ;
    private String courierHomeDimension = "";
    private long waystonePos;
    private String waystoneDimension = "";
    private boolean waystoneTracksEntity;
    private int missingTargetTicks;
    private int timeToLive = -1;

    public FlyingMonkeyEntity(EntityType<? extends FlyingMonkeyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CourierGoal());
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(4, new UntamedTemptGoal());
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 14.0F, 5.0F));
        this.goalSelector.addGoal(8, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(9, new LandGoal());
        this.goalSelector.addGoal(10, new WaterAvoidingRandomFlyingGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.2F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
        if (spawnReason != EntitySpawnReason.LOAD && spawnReason != EntitySpawnReason.BREEDING) {
            setDyeColor(random.nextInt(100) == 0
                    ? DyeColor.WHITE
                    : DyeColor.byId(1 + random.nextInt(15)));
        }
        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (isTame() && isOwnedBy(player) && held.getItem() instanceof DyeItem dye) {
            if (!level().isClientSide()) {
                setDyeColor(dye.getDyeColor());
                consumeOne(player, held);
                courierFeedback((ServerLevel) level(), instantEffect(),
                        SoundEvents.DYE_USE, 0.75F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (isTame() && isOwnedBy(player) && isCourierActive()) {
            if (held.isEmpty() && player.isShiftKeyDown()) {
                if (!level().isClientSide()) {
                    abortCourier(true);
                    player.displayClientMessage(translatable("witchery.flying_monkey.cancelled"), false);
                }
                return InteractionResult.SUCCESS;
            }
            if (TaglockKitItem.isTaglockKit(held) || WaystoneItem.isWaystone(held)) {
                if (!level().isClientSide()) {
                    player.displayClientMessage(translatable("witchery.flying_monkey.busy"), false);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (isTame() && isOwnedBy(player) && WaystoneItem.isAnyBoundWaystone(held)) {
            if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                beginWaystoneJourney(serverPlayer, held);
            }
            return InteractionResult.SUCCESS;
        }

        if (isTame() && isOwnedBy(player)
                && TaglockKitItem.isTaglockKit(held) && TaglockKitItem.isFilled(held)) {
            if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                beginTaglockFetch(serverPlayer, held);
            }
            return InteractionResult.SUCCESS;
        }

        if (!isTame() && isSourceFood(held)) {
            if (!level().isClientSide()) {
                consumeOne(player, held);
                if (random.nextInt(3) == 0) {
                    tame(player);
                    navigation.stop();
                    setTarget(null);
                    setOrderedToSit(true);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (isTame() && isOwnedBy(player) && isSourceFood(held) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                consumeOne(player, held);
                heal(10.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (isTame() && isOwnedBy(player) && !isSourceFood(held)) {
            if (!level().isClientSide()) {
                setOrderedToSit(!isOrderedToSit());
                navigation.stop();
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void beginTaglockFetch(ServerPlayer owner, ItemStack taglock) {
        Optional<UUID> targetId = TaglockKitItem.getUUID(taglock);
        Optional<LivingEntity> target = targetId.flatMap(id -> PoppetItem.findLivingEntity(owner.level().getServer(), id));
        if (target.isEmpty() || !isFetchableTarget(target.get())) {
            owner.displayClientMessage(translatable("witchery.flying_monkey.target_unavailable",
                    TaglockKitItem.getBoundName(taglock)), false);
            return;
        }
        if (target.get().level() != level()) {
            owner.displayClientMessage(translatable("witchery.flying_monkey.target_other_dimension",
                    target.get().getDisplayName()), false);
            return;
        }

        rememberCourierHome();
        courierTargetUuid = target.get().getUUID();
        courierTargetName = target.get().getDisplayName().getString();
        courierState = CourierState.FETCHING;
        missingTargetTicks = 0;
        setOrderedToSit(false);
        navigation.stop();
        consumeOne(owner, taglock);
        owner.displayClientMessage(translatable("witchery.flying_monkey.fetching",
                target.get().getDisplayName()), false);
        courierFeedback((ServerLevel) level(), instantEffect(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.25F);
    }

    private void beginWaystoneJourney(ServerPlayer owner, ItemStack waystone) {
        rememberCourierHome();
        waystoneTracksEntity = WaystoneItem.isEntityBoundWaystone(waystone);
        courierTargetUuid = null;
        courierTargetName = "";

        if (waystoneTracksEntity) {
            Optional<LivingEntity> target = WaystoneItem.findBoundEntity(owner.level().getServer(), waystone);
            if (target.isEmpty() || !target.get().isAlive()) {
                owner.displayClientMessage(translatable("witchery.flying_monkey.target_unavailable",
                        TaglockKitItem.getBoundName(waystone)), false);
                clearCourierState(false);
                return;
            }
            courierTargetUuid = target.get().getUUID();
            courierTargetName = target.get().getDisplayName().getString();
            waystoneDimension = target.get().level().dimension().identifier().toString();
            waystonePos = target.get().blockPosition().asLong();
        } else {
            Optional<BlockPos> destination = WaystoneItem.getBoundPos(waystone);
            Optional<ResourceKey<Level>> dimension = WaystoneItem.getBoundDim(waystone);
            if (destination.isEmpty() || dimension.isEmpty()) {
                owner.displayClientMessage(translatable("witchery.flying_monkey.waystone_invalid"), false);
                clearCourierState(false);
                return;
            }
            ServerLevel destinationLevel = owner.level().getServer().getLevel(dimension.get());
            if (destinationLevel == null) {
                owner.displayClientMessage(translatable("witchery.flying_monkey.waystone_invalid"), false);
                clearCourierState(false);
                return;
            }
            destinationLevel.getChunkAt(destination.get());
            BlockPos safe = findSafeLanding(destinationLevel, destination.get(), owner);
            if (safe == null) {
                owner.displayClientMessage(translatable("witchery.flying_monkey.no_landing"), false);
                clearCourierState(false);
                return;
            }
            waystonePos = safe.asLong();
            waystoneDimension = dimension.get().identifier().toString();
        }

        courierState = CourierState.WAYSTONE;
        missingTargetTicks = 0;
        setOrderedToSit(false);
        navigation.stop();
        if (!owner.startRiding(this, true, true)) {
            owner.displayClientMessage(translatable("witchery.flying_monkey.target_unavailable",
                    owner.getDisplayName()), false);
            clearCourierState(false);
            return;
        }
        owner.displayClientMessage(translatable(waystoneTracksEntity
                ? "witchery.flying_monkey.waystone_entity_set"
                : "witchery.flying_monkey.waystone_set"), false);
        courierFeedback((ServerLevel) level(), instantEffect(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.25F);
    }

    private boolean isFetchableTarget(LivingEntity target) {
        return target != this && target.isAlive() && !target.isSpectator()
                && !target.isPassenger() && !target.isVehicle();
    }

    private void rememberCourierHome() {
        courierHomeX = getX();
        courierHomeY = getY();
        courierHomeZ = getZ();
        courierHomeDimension = level().dimension().identifier().toString();
    }

    private void tickFetch(ServerLevel level) {
        LivingEntity target = resolveCourierTarget(level).orElse(null);
        if (target == null || !target.isAlive()) {
            if (++missingTargetTicks >= TARGET_MISSING_TIMEOUT) {
                notifyOwner("witchery.flying_monkey.target_lost", courierTargetName);
                returnWithoutPassenger();
            }
            return;
        }
        missingTargetTicks = 0;
        if (target.level() != level || !isFetchableTarget(target)) {
            notifyOwner("witchery.flying_monkey.target_lost", courierTargetName);
            returnWithoutPassenger();
            return;
        }

        Vec3 destination = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        flyToward(destination);
        if (distanceToSqr(destination) > PICKUP_RANGE_SQR) {
            return;
        }
        if (!target.startRiding(this, true, true)) {
            notifyOwner("witchery.flying_monkey.target_lost", courierTargetName);
            returnWithoutPassenger();
            return;
        }

        courierState = CourierState.RETURNING;
        navigation.stop();
        if (target instanceof ServerPlayer player) {
            player.displayClientMessage(translatable("witchery.flying_monkey.picked_up"), false);
        }
        courierFeedback(level, instantEffect(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.75F, 1.4F);
    }

    private void tickReturn(ServerLevel level) {
        if (!dimensionMatches(level, courierHomeDimension)) {
            abortCourier(true);
            return;
        }

        Entity passenger = getFirstPassenger();
        if (courierTargetUuid != null
                && (passenger == null || !courierTargetUuid.equals(passenger.getUUID()))) {
            courierTargetUuid = null;
            notifyOwner("witchery.flying_monkey.passenger_dismounted", courierTargetName);
        }

        Vec3 home = new Vec3(courierHomeX, courierHomeY, courierHomeZ);
        flyToward(home);
        if (distanceToSqr(home) > ARRIVAL_RANGE_SQR) {
            return;
        }

        passenger = getFirstPassenger();
        if (passenger != null) {
            passenger.stopRiding();
            BlockPos landing = findSafeLanding(level, BlockPos.containing(home), passenger);
            if (landing != null) {
                passenger.teleportTo(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D);
            }
        }
        courierFeedback(level, instantEffect(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.75F, 1.0F);
        notifyOwner("witchery.flying_monkey.returned", courierTargetName);
        clearCourierState(true);
    }

    private void tickWaystone(ServerLevel level) {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !hasPassenger(owner)) {
            abortCourier(false);
            return;
        }

        ServerLevel destinationLevel;
        BlockPos destination;
        if (waystoneTracksEntity) {
            LivingEntity target = resolveCourierTarget(level).orElse(null);
            if (target == null || !target.isAlive()) {
                if (++missingTargetTicks >= TARGET_MISSING_TIMEOUT) {
                    notifyOwner("witchery.flying_monkey.target_lost", courierTargetName);
                    abortCourier(false);
                }
                return;
            }
            missingTargetTicks = 0;
            destinationLevel = (ServerLevel) target.level();
            destination = target.blockPosition();
            waystoneDimension = destinationLevel.dimension().identifier().toString();
            waystonePos = destination.asLong();
        } else {
            destinationLevel = resolveLevel(level, waystoneDimension);
            destination = BlockPos.of(waystonePos);
        }

        if (destinationLevel == null) {
            notifyOwner("witchery.flying_monkey.waystone_invalid");
            abortCourier(false);
            return;
        }

        destinationLevel.getChunkAt(destination);
        BlockPos safe = findSafeLanding(destinationLevel, destination, owner);
        if (safe == null) {
            notifyOwner("witchery.flying_monkey.no_landing");
            abortCourier(false);
            return;
        }

        if (destinationLevel != level) {
            crossDimensionWaystone(level, destinationLevel, safe, owner);
            return;
        }

        Vec3 target = Vec3.atBottomCenterOf(safe).add(0.0D, 0.5D, 0.0D);
        flyToward(target);
        if (distanceToSqr(target) <= ARRIVAL_RANGE_SQR) {
            finishWaystoneJourney(level, safe, owner);
        }
    }

    private void crossDimensionWaystone(ServerLevel source, ServerLevel destination,
                                        BlockPos landing, LivingEntity owner) {
        owner.stopRiding();
        clearCourierState(false);
        source.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.4D, getZ(),
                24, 0.4D, 0.4D, 0.4D, 0.08D);
        double x = landing.getX() + 0.5D;
        double y = landing.getY();
        double z = landing.getZ() + 0.5D;
        float yaw = getYRot();
        float pitch = getXRot();
        boolean monkeyMoved = teleportTo(destination, x, y + 1.0D, z,
                Set.<Relative>of(), yaw, pitch, false);
        boolean ownerMoved = owner.teleportTo(destination, x, y, z,
                Set.<Relative>of(), owner.getYRot(), owner.getXRot(), false);
        if (monkeyMoved && ownerMoved) {
            if (owner instanceof ServerPlayer player) {
                player.displayClientMessage(translatable("witchery.flying_monkey.waystone_arrived"), false);
            }
            destination.sendParticles(ParticleTypes.PORTAL, x, y + 0.5D, z,
                    24, 0.4D, 0.5D, 0.4D, 0.08D);
            destination.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.NEUTRAL, 0.8F, 1.15F);
        }
    }

    private void finishWaystoneJourney(ServerLevel level, BlockPos landing, LivingEntity owner) {
        owner.stopRiding();
        owner.teleportTo(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D);
        courierFeedback(level, ParticleTypes.PORTAL,
                SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.15F);
        notifyOwner("witchery.flying_monkey.waystone_arrived");
        clearCourierState(true);
    }

    private Optional<LivingEntity> resolveCourierTarget(ServerLevel level) {
        if (courierTargetUuid == null) {
            return Optional.empty();
        }
        return PoppetItem.findLivingEntity(level.getServer(), courierTargetUuid);
    }

    @Nullable
    private static ServerLevel resolveLevel(ServerLevel current, String dimension) {
        if (dimension.isBlank()) {
            return null;
        }
        try {
            ResourceKey<Level> key = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(dimension));
            return current.getServer().getLevel(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean dimensionMatches(ServerLevel level, String dimension) {
        return level.dimension().identifier().toString().equals(dimension);
    }

    private void flyToward(Vec3 target) {
        getLookControl().setLookAt(target.x, target.y, target.z, 30.0F, 30.0F);
        navigation.stop();
        Vec3 offset = target.subtract(position());
        if (horizontalCollision) {
            offset = offset.add(0.0D, 2.0D, 0.0D);
        }
        double distance = offset.length();
        if (distance < 1.0E-4D) {
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            return;
        }

        double speed = Math.min(COURIER_MAX_SPEED, Math.max(0.08D, distance * 0.12D));
        Vec3 desiredVelocity = offset.scale(speed / distance);
        setDeltaMovement(getDeltaMovement().scale(0.55D).add(desiredVelocity.scale(0.45D)));
        setYRot((float) (-(Math.atan2(getDeltaMovement().x, getDeltaMovement().z)
                * 180.0D / Math.PI)));
        yBodyRot = getYRot();
    }

    private void returnWithoutPassenger() {
        ejectPassengers();
        courierTargetUuid = null;
        courierState = CourierState.RETURNING;
        missingTargetTicks = 0;
        navigation.stop();
    }

    private void abortCourier(boolean sit) {
        ejectPassengers();
        clearCourierState(sit);
    }

    private void clearCourierState(boolean sit) {
        courierState = CourierState.IDLE;
        courierTargetUuid = null;
        courierTargetName = "";
        courierHomeX = 0.0D;
        courierHomeY = 0.0D;
        courierHomeZ = 0.0D;
        courierHomeDimension = "";
        waystonePos = 0L;
        waystoneDimension = "";
        waystoneTracksEntity = false;
        missingTargetTicks = 0;
        navigation.stop();
        setDeltaMovement(Vec3.ZERO);
        setOrderedToSit(sit);
    }

    private void notifyOwner(String key, Object... args) {
        if (!(level() instanceof ServerLevel serverLevel) || ownerUuid() == null) {
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid());
        if (owner != null) {
            owner.displayClientMessage(translatable(key, args), false);
        }
    }

    private static net.minecraft.network.chat.Component translatable(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args)
                .withStyle(ChatFormatting.DARK_PURPLE);
    }

    private static SpellParticleOption instantEffect() {
        return SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFF, 1.0F);
    }

    @Nullable
    private UUID ownerUuid() {
        EntityReference<LivingEntity> owner = getOwnerReference();
        return owner == null ? null : owner.getUUID();
    }

    private void courierFeedback(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle,
                                 SoundEvent sound, float volume, float pitch) {
        level.sendParticles(particle, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                12, 0.35D, 0.35D, 0.35D, 0.03D);
        level.playSound(null, blockPosition(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    @Nullable
    private static BlockPos findSafeLanding(ServerLevel level, BlockPos origin, Entity entity) {
        for (int distance = 0; distance <= LANDING_SEARCH_RADIUS; distance++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -distance; dx <= distance; dx++) {
                    for (int dz = -distance; dz <= distance; dz++) {
                        if (distance > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != distance) {
                            continue;
                        }
                        BlockPos feet = origin.offset(dx, dy, dz);
                        if (isSafeLanding(level, feet, entity)) {
                            return feet;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeLanding(ServerLevel level, BlockPos feet, Entity entity) {
        if (feet.getY() <= level.getMinY() || feet.getY() + entity.getBbHeight() >= level.getMaxY()
                || !level.hasChunkAt(feet)) {
            return false;
        }
        BlockPos floor = feet.below();
        if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                || !level.getFluidState(feet).isEmpty()) {
            return false;
        }
        double x = feet.getX() + 0.5D;
        double y = feet.getY();
        double z = feet.getZ() + 0.5D;
        AABB moved = entity.getBoundingBox().move(
                x - entity.getX(), y - entity.getY(), z - entity.getZ());
        return level.noCollision(entity, moved);
    }

    private static void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackTimer > 0) {
            attackTimer--;
        }
        if (!level().isClientSide() && isCourierActive() && level() instanceof ServerLevel serverLevel) {
            tickCourier(serverLevel);
        }
        if (!level().isClientSide() && timeToLive != -1
                && (--timeToLive <= 0 || getTarget() == null || !getTarget().isAlive())) {
            if (level() instanceof ServerLevel serverLevel) {
                courierFeedback(serverLevel, ParticleTypes.EXPLOSION,
                        SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.2F);
            }
            ejectPassengers();
            discard();
        }
    }

    private void tickCourier(ServerLevel level) {
        switch (courierState) {
            case FETCHING -> tickFetch(level);
            case RETURNING -> tickReturn(level);
            case WAYSTONE -> tickWaystone(level);
            case IDLE -> {
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            setOrderedToSit(false);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackTimer = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackTimer = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    public int getAttackTimer() {
        return attackTimer;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isSourceFood(stack);
    }

    static boolean isSourceFood(ItemStack stack) {
        return SOURCE_FOOD.test(stack);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (!getPassengers().isEmpty()) {
            return false;
        }
        if (courierState == CourierState.WAYSTONE) {
            return ownerUuid() != null && ownerUuid().equals(passenger.getUUID());
        }
        return courierTargetUuid != null && courierTargetUuid.equals(passenger.getUUID());
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (hasPassenger(passenger)) {
            double y = getY() - passenger.getBbHeight() * 0.6D;
            moveFunction.accept(passenger, getX(), y, getZ());
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FlyingMonkeyEntity child = WitcheryEntities.FLYING_MONKEY.get().create(level, EntitySpawnReason.BREEDING);
        if (child != null) {
            child.setDyeColor(getDyeColor());
            if (isTame()) {
                child.setTame(true, true);
                UUID owner = ownerUuid();
                if (owner != null) {
                    child.setOwnerReference(EntityReference.of(owner));
                }
            }
        }
        return child;
    }

    public void setTimeToLive(int ticks) {
        timeToLive = ticks < 0 ? -1 : ticks;
    }

    public boolean isTemporary() {
        return timeToLive != -1;
    }

    @Override
    protected void dropFromLootTable(ServerLevel level,
                                     net.minecraft.world.damagesource.DamageSource source,
                                     boolean causedByPlayer) {
        if (!isTemporary()) {
            super.dropFromLootTable(level, source, causedByPlayer);
        }
    }

    public boolean isCourierActive() {
        return courierState != CourierState.IDLE;
    }

    public CourierState getCourierState() {
        return courierState;
    }

    public Optional<UUID> getCourierTargetUuid() {
        return Optional.ofNullable(courierTargetUuid);
    }

    public Vec3 getCourierHome() {
        return new Vec3(courierHomeX, courierHomeY, courierHomeZ);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_DYE_COLOR, getDyeColor().getId());
        tag.putString(TAG_COURIER_STATE, courierState.name());
        if (courierTargetUuid != null) {
            tag.putString(TAG_COURIER_TARGET, courierTargetUuid.toString());
        }
        if (!courierTargetName.isEmpty()) {
            tag.putString(TAG_COURIER_TARGET_NAME, courierTargetName);
        }
        if (courierState != CourierState.IDLE) {
            tag.putDouble(TAG_HOME_X, courierHomeX);
            tag.putDouble(TAG_HOME_Y, courierHomeY);
            tag.putDouble(TAG_HOME_Z, courierHomeZ);
            tag.putString(TAG_HOME_DIMENSION, courierHomeDimension);
            tag.putLong(TAG_WAYSTONE_POS, waystonePos);
            tag.putString(TAG_WAYSTONE_DIMENSION, waystoneDimension);
            tag.putBoolean(TAG_WAYSTONE_TRACKS_ENTITY, waystoneTracksEntity);
        }
        tag.putInt(TAG_TIME_TO_LIVE, timeToLive);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setDyeColor(DyeColor.byId(tag.getIntOr(TAG_DYE_COLOR, 0)));
        courierState = CourierState.parse(tag.getStringOr(TAG_COURIER_STATE, CourierState.IDLE.name()));
        courierTargetUuid = parseUuid(tag.getStringOr(TAG_COURIER_TARGET, ""));
        courierTargetName = tag.getStringOr(TAG_COURIER_TARGET_NAME, "");
        courierHomeX = tag.getDoubleOr(TAG_HOME_X, 0.0D);
        courierHomeY = tag.getDoubleOr(TAG_HOME_Y, 0.0D);
        courierHomeZ = tag.getDoubleOr(TAG_HOME_Z, 0.0D);
        courierHomeDimension = tag.getStringOr(TAG_HOME_DIMENSION, "");
        waystonePos = tag.getLongOr(TAG_WAYSTONE_POS, 0L);
        waystoneDimension = tag.getStringOr(TAG_WAYSTONE_DIMENSION, "");
        waystoneTracksEntity = tag.getBooleanOr(TAG_WAYSTONE_TRACKS_ENTITY, false);
        timeToLive = tag.getIntOr(TAG_TIME_TO_LIVE, -1);
        missingTargetTicks = 0;
        if (courierState != CourierState.IDLE && courierHomeDimension.isBlank()) {
            clearCourierState(false);
        }
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DYE_COLOR, DyeColor.WHITE.getId());
    }

    public DyeColor getDyeColor() {
        return DyeColor.byId(entityData.get(DATA_DYE_COLOR));
    }

    public void setDyeColor(DyeColor color) {
        entityData.set(DATA_DYE_COLOR, color.getId());
    }

    @Override
    public int getAmbientSoundInterval() {
        return super.getAmbientSoundInterval() * 2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_MONKEY_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return WitcherySoundEvents.MOB_MONKEY_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_MONKEY_DEATH.get();
    }

    public enum CourierState {
        IDLE,
        FETCHING,
        RETURNING,
        WAYSTONE;

        private static CourierState parse(String value) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return IDLE;
            }
        }
    }

    private final class CourierGoal extends Goal {
        private CourierGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isCourierActive() && isTame();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            setOrderedToSit(false);
            navigation.stop();
        }

        @Override
        public void stop() {
            if (!isCourierActive()) {
                navigation.stop();
            }
        }
    }

    private final class UntamedTemptGoal extends TemptGoal {
        private UntamedTemptGoal() {
            super(FlyingMonkeyEntity.this, 0.6D, SOURCE_FOOD, true);
        }

        @Override
        public boolean canUse() {
            return !isTame() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !isTame() && super.canContinueToUse();
        }
    }

    private final class LandGoal extends Goal {
        @Nullable
        private BlockPos landing;
        private int remainingTicks;

        private LandGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isCourierActive() || isOrderedToSit() || onGround() || isInWater()
                    || random.nextInt(20) != 0 || !(level() instanceof ServerLevel)) {
                return false;
            }
            landing = findNaturalLanding();
            return landing != null;
        }

        @Override
        public boolean canContinueToUse() {
            return landing != null && !onGround() && remainingTicks > 0;
        }

        @Override
        public void start() {
            remainingTicks = 100;
            moveToLanding();
        }

        @Override
        public void tick() {
            remainingTicks--;
            if (tickCount % 10 == 0) {
                moveToLanding();
            }
        }

        @Override
        public void stop() {
            landing = null;
            navigation.stop();
        }

        private void moveToLanding() {
            if (landing != null) {
                navigation.moveTo(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D, 0.8D);
            }
        }

        @Nullable
        private BlockPos findNaturalLanding() {
            ServerLevel serverLevel = (ServerLevel) level();
            BlockPos center = blockPosition();
            for (int attempt = 0; attempt < 12; attempt++) {
                int dx = attempt == 0 ? 0 : random.nextInt(17) - 8;
                int dz = attempt == 0 ? 0 : random.nextInt(17) - 8;
                BlockPos column = center.offset(dx, 2, dz);
                for (int down = 0; down < 18; down++) {
                    BlockPos feet = column.below(down);
                    if (isSafeLanding(serverLevel, feet, FlyingMonkeyEntity.this)) {
                        return feet;
                    }
                }
            }
            return null;
        }
    }
}
