package com.emoniph.witchery.ai;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public interface NpcPersona<E extends LivingEntity> {
    String id();

    String roleName();

    String defaultName();

    Class<E> entityClass();

    default String displayName(E entity) {
        Component name = entity.getDisplayName();
        String text = name == null ? "" : name.getString();
        return text.isBlank() ? defaultName() : text;
    }

    default void beginConversation(E entity, ServerPlayer player) {
    }

    default void completeConversation(E entity, ServerPlayer player) {
    }

    default void nudgeAttention(E entity, ServerPlayer player) {
    }

    NpcBrainPrompt greetingPrompt(E entity, ServerPlayer player, String memory);

    NpcBrainPrompt chatPrompt(E entity, ServerPlayer player, String memory, String playerMessage);

    boolean allowsIntent(NpcIntent intent, E entity, ServerPlayer player);

    Optional<String> fallback(ServerLevel level, E entity, ServerPlayer player, String playerMessage,
                              String memoryContext);

    default Optional<NpcSocialAction> socialAction(String playerMessage) {
        return Optional.empty();
    }

    default Optional<String> polishRaw(String rawSpeech, E entity, ServerPlayer player, String playerMessage,
                                       String memoryContext, String expectedClock) {
        return NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
    }

    default Optional<String> polishFallback(String speech, E entity, ServerPlayer player, String playerMessage,
                                            String memoryContext, String expectedClock) {
        return NpcBrainResponse.polishSpeechAfterFallback(speech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
    }

    default Optional<String> supportFollowup(String playerMessage, String speech) {
        return Optional.empty();
    }

    default String relationshipNotes(String playerName, List<String> flags) {
        return "";
    }

    default String lastResortFallback(E entity, ServerPlayer player, String playerMessage, String avoidSpeech) {
        return "I heard you, but not clearly.";
    }

    default Optional<String> alternateForRepeatedFallback(E entity, ServerPlayer player, String playerMessage,
                                                          String avoidSpeech) {
        return Optional.empty();
    }
}
