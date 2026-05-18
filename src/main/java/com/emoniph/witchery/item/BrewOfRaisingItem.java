package com.emoniph.witchery.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Raising — raises up to 2 zombies that serve the drinker briefly. */
public class BrewOfRaisingItem extends BrewItem {

    public BrewOfRaisingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel sl)) return;
        // Spawn 2 zombies near the player; they are initially peaceful (no target set)
        for (int i = 0; i < 2; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (zombie == null) continue;
            double angle = Math.PI * i;
            zombie.setPos(player.getX() + Math.sin(angle) * 1.5, player.getY(), player.getZ() + Math.cos(angle) * 1.5);
            zombie.setBaby(false);
            sl.addFreshEntity(zombie);
            // Zombies despawn after 30s via the normal despawn mechanic;
            // targeting the player is intentionally not set so they attack other mobs first
        }
    }
}
