package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitcheryMobDropEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        var rng    = entity.getRandom();
        var pos    = entity.blockPosition();
        var level  = entity.level();

        if (entity instanceof Bat && rng.nextFloat() < 0.5f) {
            int count = 1 + rng.nextInt(2);
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.BAT_WOOL.get(), count)));
        }

        if (entity instanceof Wolf && rng.nextFloat() < 0.5f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.TONGUE_OF_DOG.get(), 1)));
        }

        if (entity instanceof Witch && rng.nextFloat() < 0.4f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get(), 1)));
        }
    }

    // ── Flying Monkey creation: right-click wolf with Mutandis Extremis near an Owl ──

    @SubscribeEvent
    public static void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Wolf wolf)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;

        ItemStack held = event.getItemStack();
        if (!held.is(WitcheryItems.MUTANDIS_EXTREMIS.get())) return;

        // Look for a nearby Owl familiar within 5 blocks
        AABB range = new AABB(wolf.blockPosition()).inflate(5);
        List<com.emoniph.witchery.entity.familiar.OwlFamiliarEntity> owls =
                sl.getEntitiesOfClass(com.emoniph.witchery.entity.familiar.OwlFamiliarEntity.class, range);
        if (owls.isEmpty()) return;

        com.emoniph.witchery.entity.familiar.OwlFamiliarEntity owl = owls.get(0);

        // Consume one Mutandis Extremis
        if (!event.getEntity().getAbilities().instabuild) held.shrink(1);

        // Remove wolf and owl, spawn Flying Monkey
        wolf.discard();
        owl.discard();

        FlyingMonkeyEntity monkey = WitcheryEntities.FLYING_MONKEY.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (monkey != null) {
            monkey.setPos(wolf.getX(), wolf.getY(), wolf.getZ());
            monkey.finalizeSpawn(sl, sl.getCurrentDifficultyAt(wolf.blockPosition()),
                    EntitySpawnReason.MOB_SUMMONED, null);
            sl.addFreshEntity(monkey);
        }

        event.getEntity().displayClientMessage(
                Component.translatable("witchery.flying_monkey.created"), false);
        event.setCanceled(true);
    }
}
