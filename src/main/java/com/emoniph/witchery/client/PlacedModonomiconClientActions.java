package com.emoniph.witchery.client;

import com.emoniph.witchery.network.PlacedModonomiconPageTurnPayload;
import com.emoniph.witchery.network.PlacedModonomiconPageJumpPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class PlacedModonomiconClientActions {
    private PlacedModonomiconClientActions() {}

    public static void sendPageTurn(BlockPos pos, int direction) {
        ClientPacketDistributor.sendToServer(new PlacedModonomiconPageTurnPayload(pos, direction));
    }

    public static void sendPageJump(BlockPos pos, int openPagesIndex) {
        ClientPacketDistributor.sendToServer(new PlacedModonomiconPageJumpPayload(pos, openPagesIndex));
    }
}
