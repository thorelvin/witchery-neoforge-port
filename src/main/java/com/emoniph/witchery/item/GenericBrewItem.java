package com.emoniph.witchery.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Placeholder brew with no effect — registers the item so recipes resolve. */
public class GenericBrewItem extends BrewItem {
    public GenericBrewItem(Properties props) { super(props); }
    @Override
    protected void applyEffect(Player player, Level level) { }
}
