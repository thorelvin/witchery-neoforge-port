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

    public static boolean tryIgniteMan(Level level, BlockPos pos, float rotationYaw) {
        boolean xleft = isAnyWicker(level, pos.west());
        boolean xright = isAnyWicker(level, pos.east());
        boolean zleft = isAnyWicker(level, pos.north());
        boolean zright = isAnyWicker(level, pos.south());
        BlockPos base = pos;
        int fx = 0;
        int fz = 0;

        if (((!xleft && !xright) || (!zleft && !zright)) && (xleft || xright || zleft || zright)) {
            if (!xleft && !xright) {
                if (zleft && !zright) {
                    base = pos.north();
                } else if (!zleft) {
                    base = pos.south();
                }
                while (isAnyWicker(level, base.below())) {
                    base = base.below();
                }
                while (isAnyWicker(level, base.north())) {
                    base = base.north();
                }
                fz = 1;
            } else {
                if (xleft && !xright) {
                    base = pos.west();
                } else if (!xleft) {
                    base = pos.east();
                }
                while (isAnyWicker(level, base.below())) {
                    base = base.below();
                }
                while (isAnyWicker(level, base.west())) {
                    base = base.west();
                }
                fx = 1;
            }

            if (!matchesBloodiedWickerman(level, base, fx, fz)) {
                return false;
            }
            if (level.isClientSide()) {
                return true;
            }

            ignite(level, base, fx, fz);
            summonHuntsman(level, base, fx, fz, rotationYaw);
            return true;
        }
        return false;
    }

    private static boolean matchesBloodiedWickerman(LevelReader level, BlockPos base, int fx, int fz) {
        int dx = base.getX();
        int dy = base.getY();
        int dz = base.getZ();
        return !bloodied(level, dx, dy + 7, dz)
                && !bloodied(level, dx + fx, dy + 7, dz + fz)
                && !bloodied(level, dx - fx, dy + 6, dz - fz)
                && bloodied(level, dx, dy + 6, dz)
                && bloodied(level, dx + fx, dy + 6, dz + fz)
                && !bloodied(level, dx + 2 * fx, dy + 6, dz + 2 * fz)
                && !bloodied(level, dx - fx, dy + 5, dz - fz)
                && bloodied(level, dx, dy + 5, dz)
                && bloodied(level, dx + fx, dy + 5, dz + fz)
                && !bloodied(level, dx + 2 * fx, dy + 5, dz + 2 * fz)
                && !bloodied(level, dx - 2 * fx, dy + 4, dz - 2 * fz)
                && bloodied(level, dx - fx, dy + 4, dz - fz)
                && bloodied(level, dx, dy + 4, dz)
                && bloodied(level, dx + fx, dy + 4, dz + fz)
                && bloodied(level, dx + 2 * fx, dy + 4, dz + 2 * fz)
                && !bloodied(level, dx + 3 * fx, dy + 4, dz + 3 * fz)
                && !bloodied(level, dx - 2 * fx, dy + 3, dz - 2 * fz)
                && bloodied(level, dx - fx, dy + 3, dz - fz)
                && bloodied(level, dx, dy + 3, dz)
                && bloodied(level, dx + fx, dy + 3, dz + fz)
                && bloodied(level, dx + 2 * fx, dy + 3, dz + 2 * fz)
                && !bloodied(level, dx + 3 * fx, dy + 3, dz + 3 * fz)
                && !bloodied(level, dx - 2 * fx, dy + 2, dz - 2 * fz)
                && bloodied(level, dx - fx, dy + 2, dz - fz)
                && bloodied(level, dx, dy + 2, dz)
                && bloodied(level, dx + fx, dy + 2, dz + fz)
                && bloodied(level, dx + 2 * fx, dy + 2, dz + 2 * fz)
                && !bloodied(level, dx + 3 * fx, dy + 2, dz + 3 * fz)
                && !bloodied(level, dx - fx, dy + 1, dz - fz)
                && bloodied(level, dx, dy + 1, dz)
                && bloodied(level, dx + fx, dy, dz + fz)
                && !bloodied(level, dx + 2 * fx, dy + 1, dz + 2 * fz)
                && !bloodied(level, dx - fx, dy, dz - fz)
                && bloodied(level, dx, dy, dz)
                && bloodied(level, dx + fx, dy, dz + fz)
                && !bloodied(level, dx + 2 * fx, dy, dz + 2 * fz);
    }

    private static void ignite(Level level, BlockPos base, int fx, int fz) {
        setFire(level, base.offset(0, 6, 0));
        setFire(level, base.offset(fx, 6, fz));
        setFire(level, base.offset(0, 3, 0));
        setFire(level, base.offset(fx, 3, fz));
        setFire(level, base.offset(0, 2, 0));
        setFire(level, base.offset(fx, 2, fz));
        setFire(level, base.above());
        setFire(level, base.offset(fx, 1, fz));
        setFire(level, base);
        setFire(level, base.offset(fx, 0, fz));
        setFire(level, base.offset(-fx, 4, -fz));
        setFire(level, base.offset(2 * fx, 4, 2 * fz));
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

    private static boolean isAnyWicker(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof WickerBundleBlock;
    }

    private static boolean bloodied(LevelReader level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).is(WitcheryBlocks.BLOODIED_WICKER_BUNDLE.get());
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }
}
