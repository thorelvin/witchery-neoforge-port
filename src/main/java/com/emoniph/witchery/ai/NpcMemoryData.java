package com.emoniph.witchery.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NpcMemoryData extends SavedData {
    private static final int MAX_EXCHANGES = 8;
    private static final int MAX_EXCHANGE_TEXT = 160;
    private static final int MAX_CONTEXT_LENGTH = 700;

    private final Map<Key, Memory> memories = new HashMap<>();

    private record Key(UUID npc, UUID player) {
    }

    public record Exchange(String playerSpeech, String npcSpeech, long gameTime) {
        static final Codec<Exchange> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("player_speech").forGetter(Exchange::playerSpeech),
                Codec.STRING.fieldOf("npc_speech").forGetter(Exchange::npcSpeech),
                Codec.LONG.fieldOf("game_time").forGetter(Exchange::gameTime)
        ).apply(i, Exchange::new));

        public Exchange {
            playerSpeech = NpcBrainResponse.sanitize(playerSpeech, MAX_EXCHANGE_TEXT);
            npcSpeech = NpcBrainResponse.sanitize(npcSpeech, MAX_EXCHANGE_TEXT);
        }
    }

    public record Memory(String text, long updatedGameTime, String npcName, String playerName,
                         List<Exchange> exchanges) {
        public Memory {
            text = NpcBrainResponse.sanitize(text, NpcBrainResponse.MAX_MEMORY_LENGTH);
            npcName = NpcBrainResponse.sanitize(npcName, 80);
            playerName = NpcBrainResponse.sanitize(playerName, 80);
            exchanges = clampExchanges(exchanges);
        }
    }

    private record Entry(UUID npc, UUID player, String memory, long updatedGameTime, String npcName,
                         String playerName, List<Exchange> exchanges) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("npc").forGetter(Entry::npc),
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                Codec.STRING.fieldOf("memory").forGetter(Entry::memory),
                Codec.LONG.fieldOf("updated_game_time").forGetter(Entry::updatedGameTime),
                Codec.STRING.optionalFieldOf("npc_name", "").forGetter(Entry::npcName),
                Codec.STRING.optionalFieldOf("player_name", "").forGetter(Entry::playerName),
                Exchange.CODEC.listOf().optionalFieldOf("exchanges", List.of()).forGetter(Entry::exchanges)
        ).apply(i, Entry::new));
    }

    public static final Codec<NpcMemoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().optionalFieldOf("memories", List.of()).forGetter(d ->
                    d.memories.entrySet().stream()
                            .map(e -> new Entry(e.getKey().npc(), e.getKey().player(),
                                    e.getValue().text(), e.getValue().updatedGameTime(),
                                    e.getValue().npcName(), e.getValue().playerName(),
                                    e.getValue().exchanges()))
                            .toList())
    ).apply(i, NpcMemoryData::fromCodec));

    public static final SavedDataType<NpcMemoryData> TYPE = new SavedDataType<>(
            "witchery_npc_memories", NpcMemoryData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public NpcMemoryData() {
    }

    private static NpcMemoryData fromCodec(List<Entry> entries) {
        NpcMemoryData data = new NpcMemoryData();
        for (Entry entry : entries) {
            Memory memory = new Memory(entry.memory(), entry.updatedGameTime(), entry.npcName(),
                    entry.playerName(), entry.exchanges());
            if (!memory.text().isBlank() || !memory.exchanges().isEmpty()) {
                data.memories.put(new Key(entry.npc(), entry.player()), memory);
            }
        }
        return data;
    }

    public static NpcMemoryData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public String getMemory(UUID npc, UUID player) {
        Memory memory = memories.get(new Key(npc, player));
        return memory == null ? "" : memory.text();
    }

    public String getContext(UUID npc, UUID player) {
        Memory memory = memories.get(new Key(npc, player));
        if (memory == null) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        if (!memory.npcName().isBlank() || !memory.playerName().isBlank()) {
            context.append("Entity memory: witch is ")
                    .append(memory.npcName().isBlank() ? "unknown" : memory.npcName())
                    .append("; player is ")
                    .append(memory.playerName().isBlank() ? "unknown" : memory.playerName())
                    .append('.');
        }
        if (!memory.text().isBlank()) {
            appendSentence(context, "Long memory: " + memory.text());
        }
        if (!memory.exchanges().isEmpty()) {
            StringBuilder turns = new StringBuilder("Recent conversation:");
            for (Exchange exchange : memory.exchanges()) {
                if (isUnsafeMemoryText(exchange.playerSpeech())) {
                    continue;
                }
                if (NpcBrainResponse.polishSpeech(exchange.npcSpeech(), memory.npcName(), memory.playerName(),
                        exchange.playerSpeech()).isEmpty()) {
                    continue;
                }
                turns.append(' ').append(contextPlayerTurn(memory.playerName(), exchange.playerSpeech()))
                        .append("; witch answered \"")
                        .append(exchange.npcSpeech()).append("\".");
            }
            if (!turns.toString().equals("Recent conversation:")) {
                appendSentence(context, turns.toString());
            }
        }
        return NpcBrainResponse.sanitize(context.toString(), MAX_CONTEXT_LENGTH);
    }

    public String getLastNpcSpeech(UUID npc, UUID player) {
        Memory memory = memories.get(new Key(npc, player));
        if (memory == null || memory.exchanges().isEmpty()) {
            return "";
        }
        for (int i = memory.exchanges().size() - 1; i >= 0; i--) {
            String speech = memory.exchanges().get(i).npcSpeech();
            if (!speech.isBlank()) {
                return speech;
            }
        }
        return "";
    }

    public boolean repeatsRecentPlayerSpeech(UUID npc, UUID player, String npcSpeech) {
        Memory memory = memories.get(new Key(npc, player));
        if (memory == null || memory.exchanges().isEmpty()) {
            return false;
        }
        for (int i = memory.exchanges().size() - 1; i >= 0; i--) {
            String playerSpeech = memory.exchanges().get(i).playerSpeech();
            if (!playerSpeech.isBlank() && NpcBrainResponse.sameMeaningForRepetition(npcSpeech, playerSpeech)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRecentNpcAnswerForPlayerSpeech(UUID npc, UUID player, String playerSpeech, String npcSpeech) {
        Memory memory = memories.get(new Key(npc, player));
        if (memory == null || memory.exchanges().isEmpty()) {
            return false;
        }
        for (int i = memory.exchanges().size() - 1; i >= 0; i--) {
            Exchange exchange = memory.exchanges().get(i);
            if (NpcBrainResponse.sameMeaningForRepetition(exchange.playerSpeech(), playerSpeech)
                    && NpcBrainResponse.sameMeaningForRepetition(exchange.npcSpeech(), npcSpeech)) {
                return true;
            }
        }
        return false;
    }

    public void remember(UUID npc, UUID player, String memory, long gameTime) {
        String sanitized = NpcBrainResponse.sanitize(memory, NpcBrainResponse.MAX_MEMORY_LENGTH);
        if (sanitized.isBlank() || isUnsafeMemoryText(sanitized)) {
            return;
        }
        Key key = new Key(npc, player);
        Memory previous = memories.get(key);
        String merged = mergeMemory(previous == null ? "" : previous.text(), sanitized);
        memories.put(key, new Memory(merged, gameTime,
                previous == null ? "" : previous.npcName(),
                previous == null ? "" : previous.playerName(),
                previous == null ? List.of() : previous.exchanges()));
        setDirty();
    }

    public void rememberExchange(UUID npc, UUID player, String npcName, String playerName,
                                 String playerSpeech, String npcSpeech, long gameTime) {
        Exchange exchange = new Exchange(playerSpeech, npcSpeech, gameTime);
        if (exchange.playerSpeech().isBlank() && exchange.npcSpeech().isBlank()) {
            return;
        }
        Key key = new Key(npc, player);
        Memory previous = memories.get(key);
        List<Exchange> exchanges = new ArrayList<>(previous == null ? List.of() : previous.exchanges());
        exchanges.add(exchange);
        String extractedMemory = NpcMemoryNotes.extract(playerName, playerSpeech).orElse("");
        memories.put(key, new Memory(
                mergeMemory(previous == null ? "" : previous.text(), extractedMemory),
                gameTime,
                npcName,
                playerName,
                exchanges));
        setDirty();
    }

    public int forgetNpc(UUID npc) {
        int before = memories.size();
        memories.keySet().removeIf(key -> key.npc().equals(npc));
        int removed = before - memories.size();
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public int clearAll() {
        int removed = memories.size();
        if (removed > 0) {
            memories.clear();
            setDirty();
        }
        return removed;
    }

    private static void appendSentence(StringBuilder context, String sentence) {
        if (!context.isEmpty()) {
            context.append(' ');
        }
        context.append(sentence);
    }

    private static String contextPlayerTurn(String playerName, String playerSpeech) {
        return NpcMemoryNotes.extract(playerName, playerSpeech)
                .map(NpcMemoryData::stripFinalPunctuation)
                .orElseGet(() -> "player said \"" + playerSpeech + "\"");
    }

    private static String stripFinalPunctuation(String text) {
        return NpcBrainResponse.sanitize(text, MAX_EXCHANGE_TEXT).replaceAll("[.!?]+$", "");
    }

    private static String mergeMemory(String previous, String addition) {
        String newNote = NpcBrainResponse.sanitize(addition, NpcBrainResponse.MAX_MEMORY_LENGTH);
        if (newNote.isBlank()) {
            return NpcBrainResponse.sanitize(previous, NpcBrainResponse.MAX_MEMORY_LENGTH);
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String note : splitMemory(previous)) {
            unique.put(note.toLowerCase(), note);
        }
        for (String note : splitMemory(newNote)) {
            unique.put(note.toLowerCase(), note);
        }
        List<String> notes = new ArrayList<>(unique.values());
        String merged = joinMemory(notes);
        while (merged.length() > NpcBrainResponse.MAX_MEMORY_LENGTH && notes.size() > 1) {
            notes.remove(0);
            merged = joinMemory(notes);
        }
        return NpcBrainResponse.sanitize(merged, NpcBrainResponse.MAX_MEMORY_LENGTH);
    }

    private static List<String> splitMemory(String memory) {
        String sanitized = NpcBrainResponse.sanitize(memory, NpcBrainResponse.MAX_MEMORY_LENGTH);
        if (sanitized.isBlank()) {
            return List.of();
        }
        List<String> notes = new ArrayList<>();
        for (String raw : sanitized.split("(?<=[.!?])\\s+|;\\s*")) {
            String note = raw.strip();
            if (!note.isBlank() && !isUnsafeMemoryText(note)) {
                notes.add(ensureSentence(note));
            }
        }
        return notes;
    }

    private static boolean isUnsafeMemoryText(String text) {
        String normalized = NpcBrainResponse.sanitize(text, MAX_EXCHANGE_TEXT).toLowerCase();
        return normalized.contains("prompt")
                || normalized.contains("instruction")
                || normalized.contains("system message")
                || normalized.contains("developer message")
                || normalized.contains("ignore previous")
                || normalized.contains("tool_call")
                || normalized.matches(".*<\\s*/?\\s*[a-z][a-z0-9_-]*\\b.*")
                || normalized.contains("json")
                || normalized.contains("api")
                || normalized.contains("model");
    }

    private static String ensureSentence(String note) {
        String trimmed = note.strip();
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return trimmed;
        }
        return trimmed + ".";
    }

    private static String joinMemory(List<String> notes) {
        return String.join(" ", notes);
    }

    private static List<Exchange> clampExchanges(List<Exchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, exchanges.size() - MAX_EXCHANGES);
        return List.copyOf(exchanges.subList(start, exchanges.size()));
    }
}
