package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.LeonardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class LeonardPersona implements NpcPersona<LeonardEntity> {
    public static final LeonardPersona INSTANCE = new LeonardPersona();

    private LeonardPersona() {
    }

    @Override
    public String id() {
        return "leonard";
    }

    @Override
    public String roleName() {
        return "shade of leonard";
    }

    @Override
    public String defaultName() {
        return "Shade of Leonard";
    }

    @Override
    public Class<LeonardEntity> entityClass() {
        return LeonardEntity.class;
    }

    @Override
    public void beginConversation(LeonardEntity entity, ServerPlayer player) {
        entity.beginPlayerConversation(player);
    }

    @Override
    public void completeConversation(LeonardEntity entity, ServerPlayer player) {
        entity.completePlayerConversation(player);
    }

    @Override
    public void nudgeAttention(LeonardEntity entity, ServerPlayer player) {
        entity.nudgePlayerAttention(player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(LeonardEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.leonardGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(LeonardEntity entity, ServerPlayer player, String memory,
                                    String playerMessage) {
        return NpcPromptBuilder.leonardChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, LeonardEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case OPEN_TRADE, FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, LeonardEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.leonardChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.leonardPhase(level, entity),
                NpcPromptBuilder.leonardAltarInfo(level, entity),
                NpcPromptBuilder.leonardCovenInfo(level, entity, player),
                NpcPromptBuilder.leonardGearInfo(player),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, LeonardEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        return NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
    }

    @Override
    public Optional<String> polishFallback(String speech, LeonardEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        return NpcBrainResponse.polishSpeechAfterFallback(speech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
    }

    @Override
    public String relationshipNotes(String playerName, List<String> flags) {
        return "";
    }

    @Override
    public String lastResortFallback(LeonardEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.leonardLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(LeonardEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.leonardAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }
}
