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
        unknownProfile = unknownProfile(commonProtection);
        Class<?> profileClass = unknownProfile.getClass();
        canBreakBlock = method(commonProtection, "canBreakBlock",
                Level.class, BlockPos.class, profileClass, Player.class);
        canPlaceBlock = method(commonProtection, "canPlaceBlock",
                Level.class, BlockPos.class, profileClass, Player.class);
        canInteractBlock = method(commonProtection, "canInteractBlock",
                Level.class, BlockPos.class, profileClass, Player.class);
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

    private static Object unknownProfile(Class<?> commonProtection) throws ReflectiveOperationException {
        try {
            Field field = commonProtection.getField("UNKNOWN");
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException ignored) {
            Class<?> provider = loadClass("eu.pb4.common.protection.api.ProtectionProvider");
            Field field = provider.getField("UNKNOWN");
            field.setAccessible(true);
            return field.get(null);
        }
    }
}
