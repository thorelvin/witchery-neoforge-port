package com.emoniph.witchery.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Brew of Love - encourages nearby animals and villagers to breed. */
public class BrewOfLoveItem extends BrewItem {
    private static final double RADIUS = 4.0D;
    private static final int MAX_VILLAGER_PAIRS = 10;
    private static final String SOURCE_ENSLAVER_TAG = "WITCEnslaverName";
    private static final String PORT_ENSLAVER_UUID_TAG = "witchery_enslaver_uuid";

    public BrewOfLoveItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = new AABB(player.blockPosition()).inflate(RADIUS, 2.0D, RADIUS);
        List<Villager> adultVillagers = new ArrayList<>();
        List<Zombie> enslavedZombies = new ArrayList<>();

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.distanceToSqr(player) < RADIUS * RADIUS)) {
            if (entity instanceof Animal animal) {
                stirAnimalLove(animal, player);
            } else if (entity instanceof Villager villager && villager.getAge() >= 0) {
                adultVillagers.add(villager);
            } else if (entity instanceof Zombie zombie && !zombie.isBaby() && isEnslavedBy(zombie, player)) {
                enslavedZombies.add(zombie);
            }
        }

        pairVillagers(serverLevel, adultVillagers);
        pairEnslavedZombies(serverLevel, player, enslavedZombies);
    }

    private static void stirAnimalLove(Animal animal, Player player) {
        if (animal.getAge() >= 0 && animal.canFallInLove()) {
            animal.setAge(0);
            animal.setInLove(player);
        }
    }

    private static void pairVillagers(ServerLevel level, List<Villager> villagers) {
        int pairs = Math.min(MAX_VILLAGER_PAIRS, villagers.size() / 2);
        for (int i = 0; i < pairs; i++) {
            Villager villager = villagers.get(i * 2);
            Villager mate = villagers.get(i * 2 + 1);
            giveBirth(level, villager, mate);
        }
    }

    private static void giveBirth(ServerLevel level, Villager villager, Villager mate) {
        Villager child = villager.getBreedOffspring(level, mate);
        if (child == null) {
            return;
        }

        villager.setPos(mate.getX(), mate.getY(), mate.getZ());
        level.sendParticles(ParticleTypes.HEART,
                mate.getX(), mate.getY() + mate.getBbHeight() * 0.75D, mate.getZ(),
                8, 1.0D, 1.0D, 1.0D, 0.0D);

        mate.setAge(6000);
        villager.setAge(6000);
        child.setAge(-24000);
        child.setPos(villager.getX(), villager.getY(), villager.getZ());
        level.addFreshEntity(child);
        level.sendParticles(ParticleTypes.HEART,
                child.getX(), child.getY() + child.getBbHeight() * 0.75D, child.getZ(),
                8, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private static void pairEnslavedZombies(ServerLevel level, Player player, List<Zombie> zombies) {
        int pairs = Math.min(MAX_VILLAGER_PAIRS, zombies.size() / 2);
        for (int i = 0; i < pairs; i++) {
            Zombie zombie = zombies.get(i * 2);
            Zombie mate = zombies.get(i * 2 + 1);
            giveZombieBirth(level, player, zombie, mate);
        }
    }

    private static void giveZombieBirth(ServerLevel level, Player player, Zombie zombie, Zombie mate) {
        zombie.setPos(mate.getX(), mate.getY(), mate.getZ());
        level.sendParticles(ParticleTypes.HEART,
                mate.getX(), mate.getY() + mate.getBbHeight() * 0.75D, mate.getZ(),
                8, 1.0D, 1.0D, 1.0D, 0.0D);

        Zombie baby = EntityType.ZOMBIE.create(level, EntitySpawnReason.TRIGGERED);
        if (baby == null) {
            return;
        }
        baby.setPos(mate.getX(), mate.getY(), mate.getZ());
        baby.setYRot(level.random.nextFloat() * 360.0F);
        baby.setXRot(0.0F);
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()), EntitySpawnReason.TRIGGERED, null);
        baby.setBaby(true);
        stampEnslaver(baby, player);
        level.addFreshEntity(baby);
        level.sendParticles(ParticleTypes.HEART,
                baby.getX(), baby.getY() + baby.getBbHeight() * 0.75D, baby.getZ(),
                8, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    private static boolean isEnslavedBy(Zombie zombie, Player player) {
        String playerName = player.getName().getString();
        String sourceName = zombie.getPersistentData().getStringOr(SOURCE_ENSLAVER_TAG, "");
        if (sourceName.equalsIgnoreCase(playerName)) {
            return true;
        }
        return zombie.getPersistentData().getStringOr(PORT_ENSLAVER_UUID_TAG, "").equals(player.getUUID().toString());
    }

    private static void stampEnslaver(Zombie zombie, Player player) {
        zombie.getPersistentData().putString(SOURCE_ENSLAVER_TAG, player.getName().getString());
        zombie.getPersistentData().putString(PORT_ENSLAVER_UUID_TAG, player.getUUID().toString());
        zombie.setPersistenceRequired();
        zombie.setTarget(null);
    }
}
