package com.emoniph.witchery.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Active creature powers gained by sacrificing mobs near an altar. */
public enum CreaturePowerType {

    SPIDER {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            EntityHitResult target = pickEntity(player, 16.0);
            if (target != null && target.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 4));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                level.sendParticles(ParticleTypes.ITEM_COBWEB, living.getX(), living.getY(0.5), living.getZ(),
                        16, 0.35, 0.4, 0.35, 0.02);
            } else {
                placeWeb(player, level);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        @Override public String displayName() { return "Spider"; }
    },

    CREEPER {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            level.explode(player, player.getX(), player.getY(), player.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
            player.fallDistance = 0.0F;
        }
        @Override public String displayName() { return "Creeper"; }
    },

    SPEED {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 3));
        }
        @Override public String displayName() { return "Speed"; }
    },

    JUMP {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 3));
            player.fallDistance = 0f;
        }
        @Override public String displayName() { return "Jump"; }
    },

    HEAL {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        @Override public String displayName() { return "Heal"; }
    },

    SQUID {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            EntityHitResult target = pickEntity(player, 16.0);
            if (target != null && target.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                level.sendParticles(ParticleTypes.SQUID_INK, living.getX(), living.getY(0.8), living.getZ(),
                        32, 0.6, 0.4, 0.6, 0.05);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        @Override public String displayName() { return "Squid"; }
    },

    GHAST {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            Vec3 look = player.getLookAngle();
            LargeFireball fireball = new LargeFireball(level, player, look, 1);
            fireball.setPos(player.getX() + look.x, player.getEyeY() - 0.1D + look.y, player.getZ() + look.z);
            level.addFreshEntity(fireball);
            level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        @Override public String displayName() { return "Ghast"; }
    },

    BLAZE {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            Vec3 look = player.getLookAngle();
            for (double yawOffset : new double[] {-10.0D, 0.0D, 10.0D}) {
                Vec3 shot = look.yRot((float) Math.toRadians(yawOffset));
                SmallFireball fireball = new SmallFireball(level,
                        player.getX() + shot.x,
                        player.getEyeY() - 0.1D + shot.y,
                        player.getZ() + shot.z,
                        shot);
                fireball.setOwner(player);
                level.addFreshEntity(fireball);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        @Override public String displayName() { return "Blaze"; }
    },

    PIGMAN {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 2));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 2));
        }
        @Override public String displayName() { return "Pigman"; }
    },

    ZOMBIE {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 0));
        }
        @Override public String displayName() { return "Zombie"; }
    },

    SKELETON {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            var arrow = ProjectileUtil.getMobArrow(player, new ItemStack(Items.ARROW), 1.0F, player.getMainHandItem());
            Vec3 look = player.getLookAngle();
            arrow.setBaseDamage(4.0D);
            arrow.setCritArrow(true);
            arrow.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            arrow.shoot(look.x, look.y, look.z, 2.4F, 1.0F);
            level.addFreshEntity(arrow);
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        @Override public String displayName() { return "Skeleton"; }
    },

    ENDERMAN {
        @Override public void activate(Player player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            HitResult hit = player.pick(16.0, 1.0f, false);
            Vec3 dest = hit.getLocation();
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos standing = blockHit.getBlockPos().relative(blockHit.getDirection());
                if (level.getBlockState(standing.below()).isAir()) {
                    standing = blockHit.getBlockPos().above();
                }
                dest = Vec3.atBottomCenterOf(standing);
            }
            player.teleportTo(dest.x, dest.y, dest.z);
            player.fallDistance = 0.0F;
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        }
        @Override public String displayName() { return "Enderman"; }
    },

    BAT {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 400, 0));
            player.fallDistance = 0f;
        }
        @Override public String displayName() { return "Bat"; }
    };

    public abstract void activate(Player player);
    public abstract String displayName();

    private static EntityHitResult pickEntity(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> entity instanceof LivingEntity && entity.isPickable() && entity != player,
                range * range);
    }

    private static void placeWeb(Player player, ServerLevel level) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getLookAngle().scale(16.0D));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!level.getBlockState(pos).canBeReplaced()) return;
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
    }

    public static @Nullable CreaturePowerType fromString(String name) {
        for (CreaturePowerType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
