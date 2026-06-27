package com.emoniph.witchery.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface NpcSocialContext<E extends LivingEntity> {
    ServerLevel level();

    NpcMemoryData memoryData();

    boolean markCooldown(String action, long ticks);

    void playFeedback(boolean pleasant);

    void say(String speech);
}
