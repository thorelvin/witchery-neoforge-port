package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Plain wicker bundles are the flammable rotated-pillar blocks used to build
 * Witchery's wickerman structure. Bloodied bundles keep the same pillar behavior
 * and additionally restore the Horned Huntsman ignition check from old Witchery.
 */
public class WickerBundleBlock extends RotatedPillarBlock {
    private static final PatternPoint[] REQUIRED_WICKERMAN_POINTS = new PatternPoint[]{
            new PatternPoint(0, 6), new PatternPoint(1, 6),
            new PatternPoint(0, 5), new PatternPoint(1, 5),
            new PatternPoint(-1, 4), new PatternPoint(0, 4), new PatternPoint(1, 4), new PatternPoint(2, 4),
            new PatternPoint(-1, 3), new PatternPoint(0, 3), new PatternPoint(1, 3), new PatternPoint(2, 3),
            new PatternPoint(-1, 2), new PatternPoint(0, 2), new PatternPoint(1, 2), new PatternPoint(2, 2),
            new PatternPoint(0, 1), new PatternPoint(1, 1),
            new PatternPoint(0, 0), new PatternPoint(1, 0)
    };
    private static final PatternPoint[] EMPTY_WICKERMAN_POINTS = new PatternPoint[]{
            new PatternPoint(0, 7), new PatternPoint(1, 7),
            new PatternPoint(-1, 6), new PatternPoint(2, 6),
            new PatternPoint(-1, 5), new PatternPoint(2, 5),
            new PatternPoint(-2, 4), new PatternPoint(3, 4),
            new PatternPoint(-2, 3), new PatternPoint(3, 3),
            new PatternPoint(-2, 2), new PatternPoint(3, 2),
            new PatternPoint(-1, 1), new PatternPoint(2, 1),
            new PatternPoint(-1, 0), new PatternPoint(2, 0)
    };
    private static final PatternPoint[] FIRE_WICKERMAN_POINTS = new PatternPoint[]{
            new PatternPoint(0, 6), new PatternPoint(1, 6),
            new PatternPoint(0, 3), new PatternPoint(1, 3),
            new PatternPoint(0, 2), new PatternPoint(1, 2),
            new PatternPoint(0, 1), new PatternPoint(1, 1),
            new PatternPoint(0, 0), new PatternPoint(1, 0),
            new PatternPoint(-1, 4), new PatternPoint(2, 4)
    };

    private final boolean bloodied;

    public WickerBundleBlock(BlockBehaviour.Properties properties) {
        this(properties, false);
    }

    public WickerBundleBlock(BlockBehaviour.Properties properties, boolean bloodied) {
        super(properties);
        this.bloodied = bloodied;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                       BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!tryIgniteMan(level, pos, player.getYRot())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            if (!player.isCreative()) {
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isBloodied() {
        return bloodied;
    }

    public static void buildBloodiedWickerman(Level level, BlockPos base, Direction facing) {
        Direction.Axis axis = facing.getAxis().isHorizontal() ? facing.getAxis() : Direction.Axis.Z;
        int fx = axis == Direction.Axis.X ? 1 : 0;
        int fz = axis == Direction.Axis.Z ? 1 : 0;
        BlockState vertical = WitcheryBlocks.BLOODIED_WICKER_BUNDLE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState horizontal = WitcheryBlocks.BLOODIED_WICKER_BUNDLE.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis);

        for (PatternPoint point : REQUIRED_WICKERMAN_POINTS) {
            BlockState state = usesHorizontalAxis(point) ? horizontal : vertical;
            level.setBlock(offset(base, point, fx, fz), state, Block.UPDATE_ALL);
        }
    }

    public static boolean tryIgniteMan(Level level, BlockPos pos, float rotationYaw) {
        if (!isBloodied(level, pos)) {
            return false;
        }
        WickermanMatch match = findBloodiedWickerman(level, pos);
        if (match == null) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }

        ignite(level, match.base(), match.fx(), match.fz());
        summonHuntsman(level, match.base(), match.fx(), match.fz(), rotationYaw);
        return true;
    }

    private static boolean matchesBloodiedWickerman(LevelReader level, BlockPos base, int fx, int fz) {
        for (PatternPoint point : REQUIRED_WICKERMAN_POINTS) {
            if (!isBloodied(level, offset(base, point, fx, fz))) {
                return false;
            }
        }
        for (PatternPoint point : EMPTY_WICKERMAN_POINTS) {
            if (isBloodied(level, offset(base, point, fx, fz))) {
                return false;
            }
        }
        return true;
    }

    private static void ignite(Level level, BlockPos base, int fx, int fz) {
        for (PatternPoint point : FIRE_WICKERMAN_POINTS) {
            setFire(level, offset(base, point, fx, fz));
        }
    }

    private static void summonHuntsman(Level level, BlockPos base, int fx, int fz, float rotationYaw) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HornedHuntsmanEntity huntsman = WitcheryEntities.HORNED_HUNTSMAN.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (huntsman == null) {
            return;
        }
        huntsman.setPos(
                base.getX() + 1.0 * fx + 0.5 * fz,
                base.getY() + 0.1,
                base.getZ() + 1.0 * fz + 0.5 * fx);
        huntsman.setYRot(180.0F + rotationYaw);
        huntsman.setXRot(0.0F);
        huntsman.yBodyRot = huntsman.getYRot();
        huntsman.yHeadRot = huntsman.getYRot();
        huntsman.setPersistenceRequired();
        huntsman.setPlayerCreated(true);
        huntsman.setInvulnerableTicks(150);
        huntsman.setHealth(huntsman.getMaxHealth() * 0.25F);
        serverLevel.addFreshEntity(huntsman);
        serverLevel.sendParticles(ParticleTypes.POOF,
                base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5,
                120, 0.6, 2.0, 0.6, 0.0);
    }

    private static void setFire(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static WickermanMatch findBloodiedWickerman(LevelReader level, BlockPos clickedPos) {
        for (int[] orientation : new int[][]{{1, 0}, {0, 1}}) {
            int fx = orientation[0];
            int fz = orientation[1];
            for (PatternPoint point : REQUIRED_WICKERMAN_POINTS) {
                BlockPos candidateBase = clickedPos.offset(-point.lateral() * fx, -point.y(), -point.lateral() * fz);
                if (matchesBloodiedWickerman(level, candidateBase, fx, fz)) {
                    return new WickermanMatch(candidateBase, fx, fz);
                }
            }
        }
        return null;
    }

    private static BlockPos offset(BlockPos base, PatternPoint point, int fx, int fz) {
        return base.offset(point.lateral() * fx, point.y(), point.lateral() * fz);
    }

    private static boolean usesHorizontalAxis(PatternPoint point) {
        return point.y() >= 2 && point.y() <= 4;
    }

    private static boolean isBloodied(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(WitcheryBlocks.BLOODIED_WICKER_BUNDLE.get());
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    private record PatternPoint(int lateral, int y) {}

    private record WickermanMatch(BlockPos base, int fx, int fz) {}
}
