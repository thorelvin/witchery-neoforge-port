package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Overworld infusion's shockwave (source {@code InfusionOverworld.ShockwaveTask} run through
 * {@code ServerTickEvents.TASKS}). Simplified vs source: an expanding ring of damage + radial knockback
 * with a dust ring each stage, skipping the literal block-moving ripple (cosmetic + heavy). Tasks live
 * in a transient per-server list ticked from {@link LevelTickEvent.Post}.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class EarthShockwaveEvents {

    private EarthShockwaveEvents() {}

    private static final List<Shockwave> ACTIVE = new ArrayList<>();

    /** Begin a shockwave centred under the caster, expanding to {@code maxRadius}. */
    public static void start(ServerLevel level, ServerPlayer owner, int maxRadius) {
        ACTIVE.add(new Shockwave(level.dimension(), owner.getUUID(), owner.blockPosition().below(),
                Math.max(2, maxRadius) + 2));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (ACTIVE.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ACTIVE.removeIf(wave -> wave.dimension.equals(level.dimension()) && wave.tick(level));
    }

    static void clearSession() {
        ACTIVE.clear();
    }

    private static final class Shockwave {
        private final ResourceKey<net.minecraft.world.level.Level> dimension;
        private final UUID owner;
        private final BlockPos center;
        private final int maxRadius;
        private int stage = 0;

        Shockwave(ResourceKey<net.minecraft.world.level.Level> dimension, UUID owner,
                  BlockPos center, int maxRadius) {
            this.dimension = dimension;
            this.owner = owner;
            this.center = center;
            this.maxRadius = maxRadius;
        }

        boolean tick(ServerLevel level) {
            stage++;
            int radius = stage + 1;
            double cx = center.getX() + 0.5;
            double cy = center.getY() + 1.0;
            double cz = center.getZ() + 0.5;

            for (int deg = 0; deg < 360; deg += 18) {
                double a = Math.toRadians(deg);
                level.sendParticles(ParticleTypes.POOF, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius,
                        1, 0.0, 0.05, 0.0, 0.01);
            }

            Entity ownerEntity = level.getEntity(owner);
            DamageSource source = ownerEntity instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().magic();
            AABB band = new AABB(center).inflate(radius + 1, 2.0, radius + 1);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, band)) {
                if (entity.getUUID().equals(owner)) {
                    continue;
                }
                double dx = entity.getX() - cx;
                double dz = entity.getZ() - cz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= radius && dist <= radius + 1.0) {
                    entity.hurtServer(level, source, 8.0F);
                    Vec3 push = new Vec3(dx, 0.0, dz).normalize();
                    entity.push(push.x, 0.4, push.z);
                    entity.hurtMarked = true;
                }
            }
            return stage >= maxRadius;
        }
    }
}
