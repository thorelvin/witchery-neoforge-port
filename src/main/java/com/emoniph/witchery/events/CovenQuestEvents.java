package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Keeps a coven fight-quest pet (a Spider/Zombie stamped with {@link CovenWitchEntity#TAG_QUEST_WITCH_UUID})
 * from turning on the witch who conjured it — the witch is an {@code AbstractVillager}, which a vanilla
 * Zombie hunts by default, so without this the conjured pet would kill its own quest-giver (very visible
 * in creative, where the player is not a valid melee target). The pet is meant for the player only.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class CovenQuestEvents {

    private CovenQuestEvents() {}

    @SubscribeEvent
    public static void onQuestPetChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.getPersistentData().getStringOr(CovenWitchEntity.TAG_QUEST_WITCH_UUID, "").isEmpty()) {
            return;
        }
        if (event.getNewAboutToBeSetTarget() instanceof CovenWitchEntity) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
