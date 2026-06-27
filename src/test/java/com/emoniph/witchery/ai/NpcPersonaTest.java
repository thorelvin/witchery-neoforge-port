package com.emoniph.witchery.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPersonaTest {
    @Test
    void socialActionsAreOptInForPersonas() {
        NpcPersona<LivingEntity> persona = new NpcPersona<>() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public String roleName() {
                return "test";
            }

            @Override
            public String defaultName() {
                return "Test";
            }

            @Override
            public Class<LivingEntity> entityClass() {
                return LivingEntity.class;
            }

            @Override
            public NpcBrainPrompt greetingPrompt(LivingEntity entity, ServerPlayer player, String memory) {
                return new NpcBrainPrompt("", "");
            }

            @Override
            public NpcBrainPrompt chatPrompt(LivingEntity entity, ServerPlayer player, String memory,
                                             String playerMessage) {
                return new NpcBrainPrompt("", "");
            }

            @Override
            public boolean allowsIntent(NpcIntent intent, LivingEntity entity, ServerPlayer player) {
                return true;
            }

            @Override
            public Optional<String> fallback(ServerLevel level, LivingEntity entity, ServerPlayer player,
                                             String playerMessage, String memoryContext) {
                return Optional.empty();
            }
        };

        assertTrue(persona.socialAction("thank you").isEmpty());
        assertFalse(persona.handleSocialAction(null, null, "thank you", NpcSocialAction.PRAISE, null));
    }
}
