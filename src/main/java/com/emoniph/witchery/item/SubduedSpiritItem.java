package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.SpiritEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class SubduedSpiritItem extends Item {
    private final boolean villageTarget;

    public SubduedSpiritItem(Properties properties) {
        this(properties, false);
    }

    public SubduedSpiritItem(Properties properties, boolean villageTarget) {
        super(properties);
        this.villageTarget = villageTarget;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos spawnPos = findSpawnPos(serverLevel, context.getClickedPos(), context.getClickedFace());
        if (spawnPos == null) {
            return InteractionResult.FAIL;
        }

        SpiritEntity spirit = WitcheryEntities.SPIRIT.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (spirit == null) {
            return InteractionResult.PASS;
        }

        spirit.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.05D, spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        if (!serverLevel.noCollision(spirit)) {
            return InteractionResult.FAIL;
        }

        spirit.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        spirit.setVillageTarget(villageTarget);
        spirit.setPersistenceRequired();
        serverLevel.addFreshEntity(spirit);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.6D, spawnPos.getZ() + 0.5D,
                12, 0.25D, 0.35D, 0.25D, 0.02D);
        serverLevel.playSound(null, spawnPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.5F);

        ItemStack stack = context.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static BlockPos findSpawnPos(Level level, BlockPos clicked, Direction face) {
        BlockPos start = level.getBlockState(clicked).canBeReplaced()
                ? clicked
                : clicked.relative(face);
        for (int offset = 0; offset <= 8; offset++) {
            BlockPos candidate = start.above(offset);
            if (canSpawnSpirit(level, candidate)) {
                return candidate;
            }
        }
        for (int offset = 1; offset <= 4; offset++) {
            BlockPos candidate = start.below(offset);
            if (canSpawnSpirit(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean canSpawnSpirit(Level level, BlockPos pos) {
        return isOpen(level, pos)
                && isOpen(level, pos.above())
                && !level.getBlockState(pos.below()).canBeReplaced()
                && level.getBlockState(pos.below()).getFluidState().isEmpty();
    }

    private static boolean isOpen(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }
}
