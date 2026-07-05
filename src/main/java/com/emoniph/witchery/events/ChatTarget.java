package com.emoniph.witchery.events;

import net.minecraft.world.entity.LivingEntity;

enum ChatPersona {
    COVEN_WITCH,
    LEONARD,
    BABA_YAGA,
    VILLAGER,
    HOBGOBLIN,
    REFLECTION
}

record ChatTarget(ChatPersona kind, LivingEntity entity, double distanceSqr, boolean cancelPlayerChat) {
}
