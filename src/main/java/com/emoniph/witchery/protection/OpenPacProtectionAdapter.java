package com.emoniph.witchery.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

final class OpenPacProtectionAdapter extends ReflectiveProtectionAdapter {
    private Method getApi;
    private Method getChunkProtection;
    private Method onBlockInteraction;
    private Method onEntityPlaceBlock;

    @Override
    public String id() {
        return "openpartiesandclaims";
    }

    @Override
    protected boolean initialize() throws ReflectiveOperationException {
        Class<?> apiClass = loadClass("xaero.pac.common.server.api.OpenPACServerAPI");
        Class<?> protectionClass = loadClass("xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI");
        getApi = method(apiClass, "get", MinecraftServer.class);
        getChunkProtection = method(apiClass, "getChunkProtection");
        onBlockInteraction = method(protectionClass, "onBlockInteraction",
                Entity.class, InteractionHand.class, ItemStack.class, ServerLevel.class, BlockPos.class,
                Direction.class, boolean.class, boolean.class, boolean.class);
        onEntityPlaceBlock = method(protectionClass, "onEntityPlaceBlock",
                Entity.class, ServerLevel.class, BlockPos.class);
        return true;
    }

    @Override
    public Decision canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                   WitcheryProtectionAction action) {
        if (actor == null || !isAvailable()) {
            return Decision.PASS;
        }
        try {
            Object protection = chunkProtection(level);
            return switch (action) {
                case BLOCK_BREAK, EXPLOSION -> checkBlockInteraction(protection, actor, level, pos, true);
                case BLOCK_PLACE -> checkBlockPlacement(protection, actor, level, pos);
                case BLOCK_TRANSFORM, RITUAL_AREA_EFFECT -> {
                    Decision breakDecision = checkBlockInteraction(protection, actor, level, pos, true);
                    if (breakDecision == Decision.DENY) {
                        yield Decision.DENY;
                    }
                    yield checkBlockPlacement(protection, actor, level, pos);
                }
                case ITEM_TRANSFER, ENTITY_INTERACT, NPC_COMMAND ->
                        checkBlockInteraction(protection, actor, level, pos, false);
            };
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("OpenPAC protection check failed", ex);
        }
    }

    private Object chunkProtection(ServerLevel level) throws ReflectiveOperationException {
        Object api = getApi.invoke(null, level.getServer());
        return getChunkProtection.invoke(api);
    }

    private Decision checkBlockInteraction(Object protection, ServerPlayer actor, ServerLevel level, BlockPos pos,
                                           boolean breaking) throws ReflectiveOperationException {
        boolean protectedAgainstAction = invokeBoolean(onBlockInteraction, protection, actor, InteractionHand.MAIN_HAND,
                ItemStack.EMPTY, level, pos, Direction.UP, breaking, false, true);
        return fromProtectedResult(protectedAgainstAction);
    }

    private Decision checkBlockPlacement(Object protection, ServerPlayer actor, ServerLevel level, BlockPos pos)
            throws ReflectiveOperationException {
        boolean protectedAgainstAction = invokeBoolean(onEntityPlaceBlock, protection, actor, level, pos);
        return fromProtectedResult(protectedAgainstAction);
    }
}
