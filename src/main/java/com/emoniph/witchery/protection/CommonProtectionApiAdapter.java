package com.emoniph.witchery.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class CommonProtectionApiAdapter extends ReflectiveProtectionAdapter {
    private Object unknownProfile;
    private Class<?> unknownProfileParameterType;
    private Method canBreakBlock;
    private Method canPlaceBlock;
    private Method canInteractBlock;

    @Override
    public String id() {
        return "common-protection-api";
    }

    @Override
    protected boolean initialize() throws ReflectiveOperationException {
        Class<?> commonProtection = loadClass("eu.pb4.common.protection.api.CommonProtection");
        unknownProfile = loadUnknownProfile(commonProtection);
        canBreakBlock = protectionMethod(commonProtection, "canBreakBlock");
        canPlaceBlock = protectionMethod(commonProtection, "canPlaceBlock");
        canInteractBlock = protectionMethod(commonProtection, "canInteractBlock");
        return true;
    }

    @Override
    public Decision canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                   WitcheryProtectionAction action) {
        if (actor == null || !isAvailable()) {
            return Decision.PASS;
        }
        try {
            return switch (action) {
                case BLOCK_BREAK, EXPLOSION -> checkAllowed(canBreakBlock, level, pos, actor);
                case BLOCK_PLACE -> checkAllowed(canPlaceBlock, level, pos, actor);
                case BLOCK_TRANSFORM, RITUAL_AREA_EFFECT -> {
                    Decision breakDecision = checkAllowed(canBreakBlock, level, pos, actor);
                    if (breakDecision == Decision.DENY) {
                        yield Decision.DENY;
                    }
                    yield checkAllowed(canPlaceBlock, level, pos, actor);
                }
                case ITEM_TRANSFER, ENTITY_INTERACT, NPC_COMMAND -> checkAllowed(canInteractBlock, level, pos, actor);
            };
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Common Protection API check failed", ex);
        }
    }

    private Decision checkAllowed(Method method, ServerLevel level, BlockPos pos, ServerPlayer actor)
            throws ReflectiveOperationException {
        return fromAllowedResult(invokeBoolean(method, null, level, pos, unknownProfile, actor));
    }

    private Object loadUnknownProfile(Class<?> commonProtection) throws ReflectiveOperationException {
        try {
            Field field = commonProtection.getField("UNKNOWN");
            field.setAccessible(true);
            unknownProfileParameterType = field.getType();
            return field.get(null);
        } catch (NoSuchFieldException ignored) {
            Class<?> provider = loadClass("eu.pb4.common.protection.api.ProtectionProvider");
            Field field = provider.getField("UNKNOWN");
            field.setAccessible(true);
            unknownProfileParameterType = field.getType();
            return field.get(null);
        }
    }

    private Method protectionMethod(Class<?> commonProtection, String name) throws NoSuchMethodException {
        NoSuchMethodException firstMiss = null;
        if (unknownProfileParameterType != null) {
            try {
                return method(commonProtection, name, Level.class, BlockPos.class,
                        unknownProfileParameterType, Player.class);
            } catch (NoSuchMethodException ex) {
                firstMiss = ex;
            }
        }
        Class<?> runtimeType = unknownProfile.getClass();
        if (runtimeType != unknownProfileParameterType) {
            try {
                return method(commonProtection, name, Level.class, BlockPos.class, runtimeType, Player.class);
            } catch (NoSuchMethodException ex) {
                if (firstMiss == null) {
                    firstMiss = ex;
                }
            }
        }
        for (Method candidate : commonProtection.getMethods()) {
            Class<?>[] parameterTypes = candidate.getParameterTypes();
            if (!candidate.getName().equals(name) || parameterTypes.length != 4) {
                continue;
            }
            if (parameterTypes[0].isAssignableFrom(ServerLevel.class)
                    && parameterTypes[1].isAssignableFrom(BlockPos.class)
                    && parameterTypes[2].isInstance(unknownProfile)
                    && parameterTypes[3].isAssignableFrom(ServerPlayer.class)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        if (firstMiss != null) {
            throw firstMiss;
        }
        throw new NoSuchMethodException(name);
    }
}
