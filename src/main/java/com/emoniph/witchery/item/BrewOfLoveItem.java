package com.emoniph.witchery.item;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Brew of Love — causes nearby animals to enter love mode. */
public class BrewOfLoveItem extends BrewItem {

    public BrewOfLoveItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(8);
        level.getEntitiesOfClass(Animal.class, area).forEach(animal -> {
            if (!animal.isInLove()) {
                animal.setInLove(player);
            }
        });
    }
}
