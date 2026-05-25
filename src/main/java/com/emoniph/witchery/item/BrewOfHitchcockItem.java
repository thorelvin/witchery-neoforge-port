package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.RavenEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Brew of Hitchcock - a reference to "The Birds". Direct hits call a brief
 * attacking flock onto the target; drinking keeps the port's wider flock burst.
 */
public class BrewOfHitchcockItem extends BrewItem {
    public static final int DEFAULT_RAVEN_COUNT = 12;
    public static final double DEFAULT_RAVEN_SPREAD = 8.0;
    private static final int TARGETED_MIN_RAVENS = 3;
    private static final int TARGETED_RANDOM_RAVENS = 2;
    private static final int TARGETED_RAVEN_TTL = 400;

    public BrewOfHitchcockItem(Properties p) { super(p); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        int spawned = spawnTargetedFlock(serverLevel, target);
        if (spawned <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        spawnRavenFlock(level, player.getX(), player.getY() + 1.0, player.getZ(), DEFAULT_RAVEN_COUNT, DEFAULT_RAVEN_SPREAD);
    }

    private static int spawnTargetedFlock(ServerLevel level, LivingEntity target) {
        RandomSource random = level.getRandom();
        int birds = random.nextInt(TARGETED_RANDOM_RAVENS) + TARGETED_MIN_RAVENS;
        int spawned = 0;
        for (int i = 0; i < birds; i++) {
            RavenEntity bird = WitcheryEntities.RAVEN.get().create(level, EntitySpawnReason.TRIGGERED);
            if (bird == null) {
                continue;
            }

            double x = target.getX() - 2.0D + random.nextInt(5);
            double y = target.getY() + target.getBbHeight() + 1.0D + random.nextInt(2);
            double z = target.getZ() - 2.0D + random.nextInt(5);
            bird.setPos(x, y, z);
            bird.setYRot(0.0F);
            bird.setTarget(target);
            bird.setTimeToLive(TARGETED_RAVEN_TTL);

            if (level.addFreshEntity(bird)) {
                level.sendParticles(ParticleTypes.PORTAL,
                        bird.getX(), bird.getY() + bird.getBbHeight() * 0.5D, bird.getZ(),
                        16, 0.45D, 0.55D, 0.45D, 0.04D);
                level.playSound(null, bird.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
                spawned++;
            }
        }
        return spawned;
    }

    public static int spawnRavenFlock(Level level, double centerX, double centerY, double centerZ, int count, double spread) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            RavenEntity bird = WitcheryEntities.RAVEN.get().create(level, EntitySpawnReason.TRIGGERED);
            if (bird == null) {
                continue;
            }
            double ox = (level.getRandom().nextDouble() - 0.5) * spread;
            double oz = (level.getRandom().nextDouble() - 0.5) * spread;
            bird.setPos(centerX + ox, centerY, centerZ + oz);
            bird.setYRot(level.getRandom().nextFloat() * 360.0F);
            // Fling birds in random directions.
            bird.setDeltaMovement(
                (level.getRandom().nextDouble() - 0.5) * 0.8,
                level.getRandom().nextDouble() * 0.6,
                (level.getRandom().nextDouble() - 0.5) * 0.8
            );
            level.addFreshEntity(bird);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        centerX + ox, centerY, centerZ + oz,
                        1, 0, 0.05D, 0, 0);
            } else {
                level.addParticle(ParticleTypes.CLOUD,
                        centerX + ox, centerY, centerZ + oz,
                        0, 0.05, 0);
            }
            spawned++;
        }
        return spawned;
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
