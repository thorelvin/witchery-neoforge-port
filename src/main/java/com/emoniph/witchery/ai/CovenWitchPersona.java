package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class CovenWitchPersona implements NpcPersona<CovenWitchEntity> {
    public static final CovenWitchPersona INSTANCE = new CovenWitchPersona();

    private CovenWitchPersona() {
    }

    @Override
    public String id() {
        return "coven_witch";
    }

    @Override
    public String roleName() {
        return "witch";
    }

    @Override
    public String defaultName() {
        return "Witch";
    }

    @Override
    public Class<CovenWitchEntity> entityClass() {
        return CovenWitchEntity.class;
    }

    @Override
    public void beginConversation(CovenWitchEntity entity, ServerPlayer player) {
        entity.beginPlayerConversation(player);
    }

    @Override
    public void completeConversation(CovenWitchEntity entity, ServerPlayer player) {
        entity.completePlayerConversation(player);
    }

    @Override
    public void nudgeAttention(CovenWitchEntity entity, ServerPlayer player) {
        entity.nudgePlayerAttention(player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(CovenWitchEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.covenWitchGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(CovenWitchEntity entity, ServerPlayer player, String memory,
                                    String playerMessage) {
        return NpcPromptBuilder.covenWitchChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, CovenWitchEntity entity, ServerPlayer player) {
        return intent.allowedForCovenWitch(entity.isRecruitedBy(player));
    }

    @Override
    public Optional<String> fallback(ServerLevel level, CovenWitchEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.covenChat(playerMessage, displayName(entity), player.getName().getString(),
                NpcPromptBuilder.biomeName(level, entity.blockPosition()),
                NpcPromptBuilder.timeName(level.getDayTime()),
                NpcPromptBuilder.clockTime(level.getDayTime()),
                level.getDayTime() / 24000L,
                memoryContext);
    }

    @Override
    public Optional<String> supportFollowup(String playerMessage, String speech) {
        return NpcBrainResponse.supportFollowup(playerMessage, speech);
    }

    @Override
    public String lastResortFallback(CovenWitchEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.lastResortAside(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(CovenWitchEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.alternateForRepeatedFallback(playerMessage, player.getName().getString(),
                avoidSpeech);
    }
}
