package com.emoniph.witchery.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class GoblinBossTeleportHelper {
    private static final double SOURCE_RANGE = 16.0D;
    private static final int SOURCE_PARTICLE_COUNT = 128;

    private GoblinBossTeleportHelper() {
    }

    static boolean teleportToward(Mob boss, Entity target) {
        Vec3 direction = new Vec3(
                boss.getX() - target.getX(),
                boss.getBoundingBox().minY + boss.getBbHeight() * 0.5D
                        - target.getY() + target.getEyeHeight(),
                boss.getZ() - target.getZ()).normalize();
        double x = boss.getX() + (boss.getRandom().nextDouble() - 0.5D) * 8.0D
                - direction.x * SOURCE_RANGE;
        double y = boss.getY() + boss.getRandom().nextInt(16) - 8
                - direction.y * SOURCE_RANGE;
        double z = boss.getZ() + (boss.getRandom().nextDouble() - 0.5D) * 8.0D
                - direction.z * SOURCE_RANGE;
        return tryTeleportTo(boss, x, y, z);
    }

    static boolean tryTeleportTo(Mob boss, double x, double y, double z) {
        if (!(boss.level() instanceof ServerLevel level)) return false;
        Vec3 origin = boss.position();
        BlockPos candidate = BlockPos.containing(x, y, z);
        if (level.isOutsideBuildHeight(candidate) || !level.getWorldBorder().isWithinBounds(candidate)
                || !level.hasChunkAt(candidate)) return false;

        double landingY = y;
        BlockPos feet = candidate;
        boolean foundGround = false;
        while (feet.getY() > level.getMinY()) {
            BlockPos supportPos = feet.below();
            if (!level.hasChunkAt(supportPos)) return false;
            BlockState support = level.getBlockState(supportPos);
            if (support.blocksMotion() && support.getFluidState().isEmpty()) {
                foundGround = true;
                break;
            }
            landingY -= 1.0D;
            feet = BlockPos.containing(x, landingY, z);
        }
        if (!foundGround || level.isOutsideBuildHeight(feet)
                || !level.getWorldBorder().isWithinBounds(feet)) return false;

        boss.setPos(x, landingY, z);
        boolean safe = level.noCollision(boss) && !level.containsAnyLiquid(boss.getBoundingBox());
        if (!safe) {
            boss.setPos(origin.x, origin.y, origin.z);
            return false;
        }

        emitSuccess(level, boss, origin);
        return true;
    }

    private static void emitSuccess(ServerLevel level, Mob boss, Vec3 origin) {
        for (int index = 0; index < SOURCE_PARTICLE_COUNT; index++) {
            double progress = index / (double) (SOURCE_PARTICLE_COUNT - 1);
            double x = origin.x + (boss.getX() - origin.x) * progress
                    + (boss.getRandom().nextDouble() - 0.5D) * boss.getBbWidth() * 2.0D;
            double y = origin.y + (boss.getY() - origin.y) * progress
                    + boss.getRandom().nextDouble() * boss.getBbHeight();
            double z = origin.z + (boss.getZ() - origin.z) * progress
                    + (boss.getRandom().nextDouble() - 0.5D) * boss.getBbWidth() * 2.0D;
            level.sendParticles(ParticleTypes.PORTAL, x, y, z, 1,
                    (boss.getRandom().nextFloat() - 0.5F) * 0.2F,
                    (boss.getRandom().nextFloat() - 0.5F) * 0.2F,
                    (boss.getRandom().nextFloat() - 0.5F) * 0.2F, 0.0D);
        }
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, boss.getSoundSource(), 1.0F, 1.0F);
        boss.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    }
}
