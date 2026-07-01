package com.emoniph.witchery.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class FtbChunksProtectionAdapter extends ReflectiveProtectionAdapter {
    private Method api;
    private Method isManagerLoaded;
    private Method getManager;
    private Method shouldPreventInteraction;
    private Object editBlockProtection;
    private Object interactBlockProtection;

    @Override
    public String id() {
        return "ftbchunks";
    }

    @Override
    protected boolean initialize() throws ReflectiveOperationException {
        Class<?> apiClass = loadClass("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
        Class<?> topLevelApiClass = loadClass("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
        Class<?> managerClass = loadClass("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
        Class<?> protectionClass = loadClass("dev.ftb.mods.ftbchunks.api.Protection");

        api = method(apiClass, "api");
        isManagerLoaded = method(topLevelApiClass, "isManagerLoaded");
        getManager = method(topLevelApiClass, "getManager");
        shouldPreventInteraction = method(managerClass, "shouldPreventInteraction",
                Entity.class, InteractionHand.class, BlockPos.class, protectionClass, Entity.class);
        editBlockProtection = staticField(protectionClass, "EDIT_BLOCK");
        interactBlockProtection = staticField(protectionClass, "INTERACT_BLOCK");
        return true;
    }

    @Override
    public Decision canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                   WitcheryProtectionAction action) {
        if (actor == null || !isAvailable()) {
            return Decision.PASS;
        }
        try {
            Object manager = manager();
            if (manager == null) {
                return Decision.PASS;
            }
            Object protection = switch (action) {
                case ITEM_TRANSFER, ENTITY_INTERACT, NPC_COMMAND -> interactBlockProtection;
                case BLOCK_BREAK, BLOCK_PLACE, BLOCK_TRANSFORM, EXPLOSION, RITUAL_AREA_EFFECT -> editBlockProtection;
            };
            boolean protectedAgainstAction = invokeBoolean(shouldPreventInteraction, manager, actor,
                    InteractionHand.MAIN_HAND, pos, protection, null);
            return fromProtectedResult(protectedAgainstAction);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("FTB Chunks protection check failed", ex);
        }
    }

    private Object manager() throws ReflectiveOperationException {
        Object topLevelApi = api.invoke(null);
        if (!invokeBoolean(isManagerLoaded, topLevelApi)) {
            return null;
        }
        return getManager.invoke(topLevelApi);
    }

    private static Object staticField(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
