package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public final class HuntsmanSpearEvents {
    private static final double SOURCE_WOLF_CHANCE = 0.25;
    private static final int SOURCE_SPEED_DURATION = 12000;

    private HuntsmanSpearEvents() {
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || event.getNewDamage() <= 0.0F
                || !player.isUsingItem()
                || !isHuntsmanSpear(player.getUseItem())) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !attacker.isAlive()) {
            return;
        }
        if (player.getRandom().nextDouble() >= SOURCE_WOLF_CHANCE) {
            return;
        }

        Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.TRIGGERED);
        if (wolf == null) {
            return;
        }
        wolf.setPos(player.getX(), player.getY(), player.getZ());
        wolf.setYRot(player.getYRot());
        wolf.setXRot(player.getXRot());
        wolf.setTarget(attacker);
        wolf.startPersistentAngerTimer();
        wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, SOURCE_SPEED_DURATION, 1));
        level.addFreshEntity(wolf);
    }

    private static boolean isHuntsmanSpear(ItemStack stack) {
        return stack.is(WitcheryItems.HUNTSMAN_SPEAR.get());
    }
}
