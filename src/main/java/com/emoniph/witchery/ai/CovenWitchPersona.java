package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    public String relationshipNotes(String playerName, List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        String name = playerName == null || playerName.isBlank() ? "The player" : playerName;
        List<String> notes = new ArrayList<>();
        for (String flag : flags) {
            switch (normalizeFlag(flag)) {
                case "coven_witch_herb_gift" ->
                        notes.add(name + " received one herb gift from this witch.");
                case "coven_witch_blessed" ->
                        notes.add(name + " was blessed with luck after praising this witch.");
                case "coven_witch_cursed" ->
                        notes.add(name + " was cursed with bad luck after insulting this witch.");
                default -> {
                }
            }
        }
        return NpcBrainResponse.sanitize(String.join(" ", notes), 220);
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

    private static String normalizeFlag(String flag) {
        return NpcBrainResponse.sanitize(flag, 48)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
