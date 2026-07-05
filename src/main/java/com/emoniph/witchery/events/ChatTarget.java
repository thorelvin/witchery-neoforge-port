package com.emoniph.witchery.events;

import net.minecraft.world.entity.LivingEntity;

enum ChatPersona {
    BOSS,
    COVEN_WITCH,
    LEONARD,
    BABA_YAGA,
    DEMON,
    FLAME_IMP,
    VILLAGER,
    HOBGOBLIN,
    REFLECTION
}

record ChatTarget(ChatPersona kind, LivingEntity entity, double distanceSqr, boolean cancelPlayerChat) {
}
