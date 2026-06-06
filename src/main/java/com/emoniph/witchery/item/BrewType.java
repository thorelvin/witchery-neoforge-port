package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.Brews;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Shared effect logic for the three custom brew types across all dispersal tiers. */
public enum BrewType {

    FOULNESS {
        @Override
        public void applySplash(Level level, Vec3 pos, int tier) {
            double radius = 3 + tier * 2.0;   // tier1=5, tier3=9, tier5=13 — use AABB inflate
            int nausea  = scale(300, tier);
            int poison  = scale(200, tier);
            AABB area = new AABB(pos.x - radius, pos.y - radius / 2, pos.z - radius,
                                 pos.x + radius, pos.y + radius / 2, pos.z + radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                e.addEffect(new MobEffectInstance(MobEffects.NAUSEA,  nausea, 0));
                e.addEffect(new MobEffectInstance(MobEffects.POISON,  poison, 1));
            }
        }

        @Override
        public void applyGas(Level level, Vec3 pos, int tier) {
            float radius   = tier * 4.0f / 3;           // tier1≈1.3, tier3=4, tier5≈6.7
            int duration   = scale(600, tier);
            int nausea     = scale(120, tier);
            int poison     = scale(80, tier);
            AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setRadius(radius);
            cloud.setDuration(duration);
            cloud.setRadiusPerTick(-0.005f);
            cloud.setWaitTime(10);
            Brews.tagSourceBrewGasCloud(cloud);
            cloud.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nausea, 0));
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, poison, 1));
            level.addFreshEntity(cloud);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 20 + tier * 5, 1.0, 0.5, 1.0, 0.1);
            }
        }
    },

    FOAMING {
        @Override
        public void applySplash(Level level, Vec3 pos, int tier) {
            int radius = 1 + tier;                       // tier1=2, tier3=4, tier5=6
            int radiusSq = radius * (radius + 1);        // tier1=6, tier3=20, tier5=42
            placeWebs(level, BlockPos.containing(pos), radius, radiusSq);
        }

        @Override
        public void applyGas(Level level, Vec3 pos, int tier) {
            int radius = 3 + tier;                       // tier1=4, tier3=6, tier5=8
            int radiusSq = radius * (radius + 1);        // tier1=20, tier3=42, tier5=72
            placeWebs(level, BlockPos.containing(pos), radius, radiusSq);
        }

        private void placeWebs(Level level, BlockPos center, int radius, int radiusSq) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radiusSq) continue;
                    BlockPos floor = center.offset(dx, 0, dz);
                    if (level.getBlockState(floor).isAir()) {
                        level.setBlockAndUpdate(floor, Blocks.COBWEB.defaultBlockState());
                    }
                }
            }
        }
    },

    POISON {
        @Override
        public void applySplash(Level level, Vec3 pos, int tier) {
            double radius = 1 + tier;                    // tier1=2, tier3=4, tier5=6
            int poison   = scale(300, tier);
            int weakness = scale(200, tier);
            AABB area = new AABB(pos.x - radius, pos.y - radius / 2, pos.z - radius,
                                 pos.x + radius, pos.y + radius / 2, pos.z + radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                e.addEffect(new MobEffectInstance(MobEffects.POISON,   poison,   2));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 1));
            }
        }

        @Override
        public void applyGas(Level level, Vec3 pos, int tier) {
            float radius   = tier;                       // tier1=1, tier3=3, tier5=5
            int duration   = scale(400, tier);
            int poison     = scale(120, tier);
            int weakness   = scale(80, tier);
            AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setRadius(radius);
            cloud.setDuration(duration);
            cloud.setRadiusPerTick(-0.005f);
            cloud.setWaitTime(10);
            Brews.tagSourceBrewGasCloud(cloud);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON,   poison,   2));
            cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 1));
            level.addFreshEntity(cloud);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 15 + tier * 4, 0.5, 0.5, 0.5, 0.05);
            }
        }
    };

    /** Apply splash-tier dispersal at pos, scaled by brew tier (1–5). */
    public abstract void applySplash(Level level, Vec3 pos, int tier);

    /** Apply gas-tier dispersal at pos, scaled by brew tier (1–5). */
    public abstract void applyGas(Level level, Vec3 pos, int tier);

    /** Scale a base tick/duration value by tier. Tier 3 returns the base unchanged. */
    static int scale(int base, int tier) {
        return Math.max(1, base * tier / 3);
    }
}
