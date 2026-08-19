package com.emoniph.witchery.entity;

import com.mojang.datafixers.util.Pair;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Spirit - tameable flying Spirit World mob that drops its matching subdued item.
 */
public class SpiritEntity extends TamableAnimal {
    private static final String TAG_SPIRIT_TYPE = "SpiritType";
    private static final String TAG_SUICIDE_IN = "SuicideIn";
    private static final String TAG_VILLAGE_WAYPOINT = "VillageWaypoint";
    private static final int SPIRIT_TYPE_NORMAL = 0;
    private static final int SPIRIT_TYPE_VILLAGE = 1;
    static final int SOURCE_PARTICLE_COLOR = 0xFFCC00;
    private static final int VILLAGE_SPIRIT_TTL_TICKS = 20 * 10;
    private static final int VILLAGE_SEARCH_RADIUS_CHUNKS = 8;
    private static final List<String> VILLAGE_STRUCTURE_IDS = List.of(
            "village_plains", "village_savanna", "village_taiga",
            "village_snowy", "village_desert", "village_meadow");

    private int spiritType = SPIRIT_TYPE_NORMAL;
    private int timeToLive = -1;
    private BlockPos villageWaypoint;

    public SpiritEntity(EntityType<? extends SpiritEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER, -1f);
        this.setNoGravity(true);
        this.setTame(false, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.6,
                stack -> !isTame() && stack.is(WitcheryItems.FOCUSED_WILL.get()), true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 14.0F, 5.0F));
        this.goalSelector.addGoal(8, new FlyToVillageWaypointGoal(this));
        this.goalSelector.addGoal(9, new LandGoal(this));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.2F));
    }

    public static boolean checkSpiritSpawnRules(EntityType<SpiritEntity> type, ServerLevelAccessor level,
                                                 EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        BlockState floor = level.getBlockState(pos.below());
        return level.getLevel().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)
                && pos.getY() >= 60
                && random.nextInt(10) == 0
                && isSourceSpawnFloor(floor)
                && level.getRawBrightness(pos, 0) > 8
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    static boolean isSourceSpawnFloor(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isSourceFood(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    static boolean isSourceFood(ItemStack stack) {
        return stack.is(Items.BONE);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        dropSubduedItem(level);
    }

    public void setVillageTarget(boolean villageTarget) {
        this.spiritType = villageTarget ? SPIRIT_TYPE_VILLAGE : SPIRIT_TYPE_NORMAL;
        if (villageTarget && level() instanceof ServerLevel serverLevel) {
            startVillageFlight(serverLevel);
        }
    }

    public boolean isVillageTarget() {
        return spiritType == SPIRIT_TYPE_VILLAGE;
    }

    private void startVillageFlight(ServerLevel level) {
        this.timeToLive = VILLAGE_SPIRIT_TTL_TICKS;
        this.villageWaypoint = findNearestVillage(level, blockPosition());
    }

    private static BlockPos findNearestVillage(ServerLevel level, BlockPos origin) {
        List<Holder<Structure>> holders = new ArrayList<>();
        level.registryAccess().lookup(Registries.STRUCTURE).ifPresent(registry -> {
            for (String id : VILLAGE_STRUCTURE_IDS) {
                ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE,
                        Identifier.withDefaultNamespace(id));
                registry.get(key).ifPresent(holders::add);
            }
        });
        if (holders.isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holders), origin,
                        VILLAGE_SEARCH_RADIUS_CHUNKS, false);
        return result == null ? null : safeSurfaceWaypoint(level, result.getFirst());
    }

    private static BlockPos safeSurfaceWaypoint(ServerLevel level, BlockPos pos) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnSpiritParticles();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || timeToLive < 0) {
            return;
        }
        if (--timeToLive <= 0) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    getX(), getY() + 0.1D, getZ(), 8, 0.25D, 0.25D, 0.25D, 0.0D);
            dropSubduedItem(serverLevel);
            discard();
        }
    }

    private void spawnSpiritParticles() {
        if (random.nextInt(4) != 0) {
            return;
        }
        level().addParticle(new DustParticleOptions(SOURCE_PARTICLE_COLOR, 0.55F),
                getX() + (random.nextDouble() - 0.5D) * 0.45D,
                getY() + 0.2D + random.nextDouble() * 0.25D,
                getZ() + (random.nextDouble() - 0.5D) * 0.45D,
                0.0D, 0.01D, 0.0D);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SPIRIT_TYPE, spiritType);
        tag.putInt(TAG_SUICIDE_IN, timeToLive);
        if (villageWaypoint != null) {
            tag.putLong(TAG_VILLAGE_WAYPOINT, villageWaypoint.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        spiritType = tag.getIntOr(TAG_SPIRIT_TYPE, SPIRIT_TYPE_NORMAL);
        timeToLive = tag.getIntOr(TAG_SUICIDE_IN, -1);
        long waypoint = tag.getLongOr(TAG_VILLAGE_WAYPOINT, Long.MIN_VALUE);
        villageWaypoint = waypoint == Long.MIN_VALUE ? null : BlockPos.of(waypoint);
    }

    private net.minecraft.world.item.Item getSubduedDropItem() {
        return isVillageTarget()
                ? WitcheryItems.SUBDUED_VILLAGE_SPIRIT.get()
                : WitcheryItems.SUBDUED_SPIRIT.get();
    }

    private void dropSubduedItem(ServerLevel level) {
        this.spawnAtLocation(level, new ItemStack(getSubduedDropItem(), 1));
    }

    private static class FlyToVillageWaypointGoal extends Goal {
        private final SpiritEntity spirit;

        private FlyToVillageWaypointGoal(SpiritEntity spirit) {
            this.spirit = spirit;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return spirit.villageWaypoint != null && spirit.timeToLive >= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos target = spirit.villageWaypoint;
            double x = target.getX() + 0.5D;
            double y = target.getY() + 2.0D;
            double z = target.getZ() + 0.5D;
            spirit.getMoveControl().setWantedPosition(x, y, z, 0.8D);
        }
    }

    private static final class LandGoal extends Goal {
        private final SpiritEntity spirit;
        private BlockPos target;

        private LandGoal(SpiritEntity spirit) {
            this.spirit = spirit;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (spirit.onGround() || spirit.isOrderedToSit() || spirit.random.nextInt(20) != 0
                    || !spirit.level().getFluidState(spirit.blockPosition()).isEmpty()) {
                return false;
            }
            int surfaceY = spirit.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    spirit.getBlockX(), spirit.getBlockZ());
            if (surfaceY >= spirit.getBlockY()) {
                return false;
            }
            target = new BlockPos(spirit.getBlockX(), surfaceY, spirit.getBlockZ());
            return spirit.level().getFluidState(target.below()).isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !spirit.onGround()
                    && spirit.distanceToSqr(target.getCenter()) > 0.5D;
        }

        @Override
        public void start() {
            spirit.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.8D);
        }

        @Override
        public void stop() {
            target = null;
        }
    }
}
