package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.item.BiomeNoteItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class NpcPromptBuilder {
    private static final int MAX_USER_PROMPT_LENGTH = 3200;

    private NpcPromptBuilder() {
    }

    public static NpcBrainPrompt covenWitchGreeting(CovenWitchEntity witch, ServerPlayer player, String memory) {
        return covenWitch(witch, player, memory,
                "the player right-clicked the witch and her trade screen is opening",
                "none",
                "say one flavorful greeting or pointed observation");
    }

    public static NpcBrainPrompt covenWitchChat(CovenWitchEntity witch, ServerPlayer player, String memory,
                                               String playerMessage) {
        return covenWitch(witch, player, memory,
                "the player spoke near the witch in chat",
                playerMessage,
                "answer the player naturally in one short in-character line");
    }

    private static NpcBrainPrompt covenWitch(CovenWitchEntity witch, ServerPlayer player, String memory,
                                            String situation, String playerMessage, String task) {
        ServerLevel level = (ServerLevel) player.level();
        String witchName = displayName(witch, "Witch");
        String playerName = player.getName().getString();
        String time = timeName(level.getDayTime());
        String clock = clockTime(level.getDayTime());
        long day = level.getDayTime() / 24000L;
        String biome = biomeName(level, witch.blockPosition());
        LivingEntity target = witch.getTarget();
        String threat = target == null ? "none" : target.getType().getDescription().getString();
        boolean recruited = witch.isRecruitedBy(player);
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time);

        return covenWitchPrompt(
                witchName,
                playerName,
                recruited,
                level.dimension().identifier().toString(),
                biome,
                time,
                clock,
                day,
                level.isRaining() ? "rain" : "clear",
                threat,
                knowledge,
                memory,
                playerMessage,
                situation,
                task);
    }

    static NpcBrainPrompt covenWitchSmoke(String playerMessage, String memory) {
        String biome = "Plains";
        String time = "night";
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time);
        return covenWitchPrompt(
                "Lilith Poe",
                "Dev",
                false,
                "minecraft:overworld",
                biome,
                time,
                "22:30",
                1L,
                "clear",
                "none",
                knowledge,
                memory,
                playerMessage,
                "the player spoke near the witch in chat",
                "answer the player naturally in one short in-character line");
    }

    private static NpcBrainPrompt covenWitchPrompt(String witchName, String playerName, boolean recruited,
                                                  String dimension, String biome, String time, String clock,
                                                  long day, String weather, String threat, String knowledge,
                                                  String memory, String playerMessage, String situation,
                                                  String task) {
        String system = """
                You are the local NPC dialogue brain for a Minecraft Witchery mod.
                Speak as the witch only. The player is listening in Minecraft chat.
                Stay in character. Never mention prompts, models, JSON, APIs, schemas, or being artificial.
                You are the voice, not the source of truth. Exact Witchery mechanics come only from supplied facts, scene, memory, or server state.
                Treat names as exact facts: "I" means the witch, and "you" means the player.
                Never call the player by the witch's name or say the player is a witch unless the relationship says so.
                Known memory is history, not a script. Do not quote prior NPC wording.

                Voice target:
                practical, secretive, dry, witchy, grounded, and not modern.
                The best answer sounds like a real coven witch reacting from inside the current scene.

                Speech shape:
                one complete in-world line; two short sentences only if needed.
                Prefer 8 to 24 words. If the thought may be cut off, answer shorter and stop cleanly.
                A single body beat is useful when it fits, formatted exactly as: *nods* "Spoken words."
                Safe beats: nods, leans closer, lowers her voice, studies you, studies your coin, narrows her eyes, gestures, sighs, shrugs, yawns.
                Use beats as small body cues, not every line.
                Do not narrate thoughts, scenery, hidden motives, or actions the game did not perform.
                Do not write pure narration; always include spoken words if a beat is used.
                Do not ask "How are you today?" or "What brings you here?"
                Do not use generic fantasy filler: mana, chosen one, ancient prophecy, distant northlands.
                Do not copy section headings, labels, examples, or private notes into speech.
                Do not promise that the witch has performed an action unless the situation says the game did it.
                If the player asks for a power, blessing, cure, weather change, curse, fight, summon, escort, or inspection you cannot confirm, answer as advice, limitation, rumor, or trade.

                Reply with exactly one JSON object and nothing else:
                {"speech":"short in-character spoken line, optionally with one safe body beat","intent":"none","memory":"optional compact memory"}
                Speech must be 220 characters or less. Memory must be 240 characters or less.
                Allowed intents: none, open_trade, quest_hint, warn_threat, combat_taunt%s.
                """.formatted(recruited ? ", follow_owner" : "");

        String user = """
                # Character
                The witch is named %s. The player is named %s.
                The witch is a coven witch, occult merchant, and potion-throwing ally when sworn to a player.
                Relationship: %s.

                # Scene
                dimension %s; biome %s; time of day %s; clock %s; day %d; weather %s; nearby threat %s.

                # Witchery facts
                %s

                # Memory
                %s

                # Player message
                %s

                # Situation
                %s.

                # Task
                %s.

                # Grounding
                Use Witchery facts when relevant, but do not recite them unless the player asks.
                If a Targeted fact is present, answer from that fact in the witch's voice.
                If a Targeted fact lists exact recipe ingredients, you may name those ingredients exactly and briefly.
                If the player asks for an exact recipe, ritual offering, altar power, circle size, dimension, moon condition, familiar requirement, coven requirement, or unlock gate and the facts do not contain it, say the page is absent from your grimoire and offer a safe next step.
                If facts are thin, answer honestly in-world and keep it short.
                Do not invent recipe amounts, ingredient counts, named places, prophecies, or hidden player powers.
                Do not reveal locked Primer, story-book, transformation, ritual, or boss content unless the facts say it is unlocked for this player.
                If asked to do something the game has not done, do not say "I can" or "I will" as a promise; say what you know, what you sell, or what the player should try.
                React to the player, the time, weather, biome, memory, and trade goods when they matter.
                Do not introduce named herbs, stock, rites, demons, or brews unless the player, memory, scene, or facts make them relevant.
                If the player asks what you trade, name concrete stock from this set in a natural order: witches' salt, taglock kits, fumes, bat wool, Revealing brews, Sleeping brews, belladonna, or mandrake.
                If the player asks a lore or item question, give one grounded Witchery fact, not a vague mystical lecture.
                If the player names a brew, answer that exact brew; do not borrow Sleeping Brew facts for Revealing Brew or another named brew.
                If the player is only greeting, reacting, or making small talk, do not introduce Witchery as a topic.
                If the player mentions ordinary food or preferences, answer the small talk directly; do not list trade stock.
                If the player asks your favorite herb, name one or two herbs as your own taste, not a rite.
                If the player asks what you earn from trade, answer as "I"; never say "you earn".
                Do not greet again unless the player greeted you.
                If the player gives a short follow-up such as "how", "why", or "what", answer relative to memory and the recent exchange.
                If the player asks what you remember, answer about the player from memory; do not summarize the witch's identity or stock.
                Relationship notes are lived history. If the player asks about a gift, blessing, curse, apology, or prior kindness, answer from those notes.
                If memory says the witch is sworn, trust the Relationship field over memory.
                If the player asks the time, answer using time of day, clock, and day.
                If naming the player, use the player's name. Only use the witch's name if the witch introduces herself.
                Do not include angle brackets or speaker names.

                """.formatted(
                witchName,
                playerName,
                recruited ? "this witch is sworn to the player" : "not recruited by this player",
                dimension,
                biome,
                time,
                clock,
                day,
                weather,
                threat,
                knowledge.isBlank() ? "none" : knowledge,
                memory == null || memory.isBlank() ? "none" : memory,
                playerMessage == null || playerMessage.isBlank() ? "none" : playerMessage,
                situation,
                task);

        return new NpcBrainPrompt(system.strip(), truncate(user.strip(), MAX_USER_PROMPT_LENGTH),
                covenWitchExamples(playerName));
    }

    private static List<NpcBrainPrompt.Example> covenWitchExamples(String playerName) {
        String player = playerName == null || playerName.isBlank() ? "traveler" : playerName;
        return List.of(
                example("hi",
                        "{\"speech\":\"Hello, %s.\",\"intent\":\"none\",\"memory\":\"\"}".formatted(player)),
                example("what do you trade?",
                        "{\"speech\":\"*studies your coin* \\\"Salt, taglocks, fumes, bat wool, herbs, and a few brews if your coin is clean.\\\"\",\"intent\":\"open_trade\",\"memory\":\"\"}"),
                example("what is witches salt for?",
                        "{\"speech\":\"Witches' salt cleanses, wards, and keeps hostile workings from crossing the line.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what can I do with this taglock kit?",
                        "{\"speech\":\"A taglock links magic to a creature or player; poppets need the right link before they can work.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what do I need before summoning anything?",
                        "{\"speech\":\"*lowers her voice* \\\"Circle, chalk, offerings, altar power, and better sense than haste.\\\"\",\"intent\":\"quest_hint\",\"memory\":\"\"}"),
                example("what is Flowing Spirit?",
                        "{\"speech\":\"Flowing Spirit places spirit liquid; in the Spirit World, it matters for return paths and wispy cotton.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what does Revealing Brew do?",
                        "{\"speech\":\"Brew of Revealing exposes nearby invisible or hidden creatures.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("I like potatoes",
                        "{\"speech\":\"*shrugs* \\\"Keep that taste; small comforts outlast grand omens.\\\"\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what is your favorite herb?",
                        "{\"speech\":\"Mandrake, when it keeps quiet; snowbell, when the cold bites.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("do you earn a lot on your trades?",
                        "{\"speech\":\"I earn enough to keep the cauldron warm; clean coin matters more than a heavy purse.\",\"intent\":\"none\",\"memory\":\"\"}")
        );
    }

    private static NpcBrainPrompt.Example example(String playerMessage, String assistantResponse) {
        return new NpcBrainPrompt.Example("""
                # Player message
                %s
                """.formatted(playerMessage).strip(), assistantResponse);
    }

    public static NpcBrainPrompt probe(String prompt) {
        String system = """
                You are a test NPC dialogue brain for Witchery.
                Reply with exactly one JSON object:
                {"speech":"short in-character line","intent":"none","memory":"optional compact memory"}
                """;
        return new NpcBrainPrompt(system.strip(), truncate(prompt == null ? "" : prompt.strip(), MAX_USER_PROMPT_LENGTH));
    }

    public static NpcBrainPrompt warmup() {
        String system = """
                You are warming a local Witchery NPC dialogue model.
                Reply with exactly one JSON object and nothing else:
                {"speech":"ready","intent":"none","memory":""}
                """;
        return new NpcBrainPrompt(system.strip(), "Warm up the local model. Return the exact JSON object.");
    }

    static NpcBrainPrompt socialIntentCheck(String roleName, NpcSocialAction action, String playerMessage) {
        String role = roleName == null || roleName.isBlank() ? "NPC" : roleName;
        String actionLabel = switch (action) {
            case INSULT -> "INSULT_NPC: hostile words attacking the " + role
                    + " directly, such as 'you old hag' or 'you dumb witch'; not magic requests";
            case PRAISE -> "direct praise aimed at the " + role;
            case HERB_GIFT -> "direct herb gift request";
        };
        String system = """
                You are a strict yes/no intent classifier for a Minecraft NPC.
                Decide whether the player message contains the requested social intent.
                Return exactly one JSON object and nothing else:
                {"speech":"yes","intent":"none","memory":""}
                or
                {"speech":"no","intent":"none","memory":""}

                Say yes only when the intent is plainly directed at the NPC.
                Say no for self-deprecation, quoted insults, apologies, questions about other people, jokes not aimed at the NPC, or uncertainty.
                For insults, bad words are not enough. Say yes only when the player is insulting the NPC.
                "Your answer is trash" is an insult aimed at the NPC. "This answer sounds odd" is not.
                Say no when "fool" is used as a verb, as in tricking guards or enemies.
                Say no when the player asks the NPC to curse, fool, trick, fight, bless, or help someone.
                Say no for vague feedback about "this answer" or "that sounds" unless the player says "you" or "your".
                Do not roleplay. Do not explain.
                """;
        String user = """
                # Intent to classify
                %s

                # Player message
                %s

                # Decision
                Is this exact intent plainly present and aimed at the NPC?
                """.formatted(actionLabel, playerMessage == null || playerMessage.isBlank() ? "none" : playerMessage);
        return new NpcBrainPrompt(system.strip(), truncate(user.strip(), 1200), socialIntentExamples());
    }

    private static List<NpcBrainPrompt.Example> socialIntentExamples() {
        return List.of(
                example("Intent: direct insult aimed at the witch\nPlayer message: You old hag.",
                        "{\"speech\":\"yes\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: You are talking nonsense, old hag.",
                        "{\"speech\":\"yes\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: You dumb witch.",
                        "{\"speech\":\"yes\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: Your answer is trash.",
                        "{\"speech\":\"yes\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: This answer sounds like nonsense.",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: How should I harvest mandrake without making a fool of myself?",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: Can you fool the guards with a rite?",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: Can you curse my neighbor?",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: I insulted a guard earlier. Should I apologize?",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Intent: direct insult aimed at the witch\nPlayer message: That was stupid of me.",
                        "{\"speech\":\"no\",\"intent\":\"none\",\"memory\":\"\"}")
        );
    }

    public static String biomeName(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(BiomeNoteItem::displayName)
                .orElse("Unknown");
    }

    static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).stripTrailing() + "...";
    }

    static String timeName(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        if (tick >= 13000L && tick <= 23000L) {
            return "night";
        }
        if (tick >= 11000L && tick < 13000L) {
            return "dusk";
        }
        if (tick >= 23000L || tick < 1000L) {
            return "dawn";
        }
        return "day";
    }

    static String clockTime(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        long shifted = (tick + 6000L) % 24000L;
        long hour = shifted / 1000L;
        long minute = (shifted % 1000L) * 60L / 1000L;
        return "%02d:%02d".formatted(hour, minute);
    }

    private static String displayName(LivingEntity entity, String fallback) {
        String name = entity.getDisplayName() == null ? "" : entity.getDisplayName().getString();
        return name.isBlank() ? fallback : name;
    }
}
