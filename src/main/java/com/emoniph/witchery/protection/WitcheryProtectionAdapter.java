package com.emoniph.witchery.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface WitcheryProtectionAdapter {
    String id();

    boolean isAvailable();

    Decision canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                            WitcheryProtectionAction action);

    enum Decision {
        ALLOW,
        DENY,
        PASS
    }
}
