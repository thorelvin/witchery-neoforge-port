package com.emoniph.witchery.ai;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.VampireEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.BiomeNoteItem;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.worldgen.ApothecaryVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class NpcPromptBuilder {
    private static final int MAX_USER_PROMPT_LENGTH = 3200;
    private static final int MAX_CARD_TOPICS = 6;

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

    public static NpcBrainPrompt covenWitchAmbient(CovenWitchEntity witch, ServerPlayer player, String memory,
                                                  String ambientCue) {
        String cue = ambientCue == null || ambientCue.isBlank() ? "standing nearby" : ambientCue;
        return covenWitch(witch, player, memory,
                "the player is nearby; server observed this behavior: " + cue,
                "ambient " + cue,
                "react once to the observed behavior in one short in-character line");
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
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time,
                cardRequest("coven_witch", witch.getUUID(), player, level));

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
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time,
                NpcCardRequest.persona("coven_witch"));
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

    static NpcBrainPrompt covenWitchAmbientSmoke(String ambientCue, String memory) {
        String biome = "Plains";
        String time = "night";
        String cue = ambientCue == null || ambientCue.isBlank() ? "standing nearby" : ambientCue;
        String playerMessage = "ambient " + cue;
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time,
                NpcCardRequest.persona("coven_witch"));
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
                "the player is nearby; server observed this behavior: " + cue,
                "react once to the observed behavior in one short in-character line");
    }

    public static NpcBrainPrompt leonardGreeting(LeonardEntity leonard, ServerPlayer player, String memory) {
        return leonard(leonard, player, memory,
                "the player right-clicked the Shade of Leonard",
                "none",
                "answer as a summoned boss acknowledging the player in one short in-character line");
    }

    public static NpcBrainPrompt leonardGaze(LeonardEntity leonard, ServerPlayer player, String memory) {
        return leonard(leonard, player, memory,
                "the player is nearby and looking directly at the Shade of Leonard",
                "none",
                "start a brief ominous in-character line that invites or challenges conversation");
    }

    public static NpcBrainPrompt leonardChat(LeonardEntity leonard, ServerPlayer player, String memory,
                                             String playerMessage) {
        return leonard(leonard, player, memory,
                "the player spoke near the Shade of Leonard in chat",
                playerMessage,
                "answer the player naturally in one short in-character line");
    }

    static NpcBrainPrompt leonardSmoke(String playerMessage, String memory) {
        return leonardPrompt(
                "Shade of Leonard",
                "Dev",
                "minecraft:overworld",
                "Plains",
                "night",
                "22:30",
                1L,
                "clear",
                "neutral; health 600/600; no target",
                "altar within reach: power 12000 of 16000, recharge 20 per tick, range 32 blocks, distance 6.0 blocks",
                "two of six sworn to Dev; nearby witches: Lilith Poe, Mara Fen",
                "main hand Arthana; offhand Leonard's Urn; wearing Witches' Hat",
                NpcKnowledgeBase.relevantContext(playerMessage, "Plains", "night",
                        NpcCardRequest.persona("leonard")),
                memory,
                playerMessage,
                "the player spoke near the Shade of Leonard in chat",
                "answer the player naturally in one short in-character line");
    }

    public static NpcBrainPrompt villagerGreeting(Villager villager, ServerPlayer player, String memory) {
        return villager(villager, player, memory,
                "the player is close enough to talk to this villager",
                "none",
                "say one grounded village greeting or worried observation");
    }

    public static NpcBrainPrompt villagerChat(Villager villager, ServerPlayer player, String memory,
                                              String playerMessage) {
        return villager(villager, player, memory,
                "the player spoke near this villager in chat",
                playerMessage,
                "answer the player naturally in one short in-character line");
    }

    static NpcBrainPrompt villagerSmoke(String playerMessage, String memory) {
        return villagerPrompt(
                "Apothecary",
                "Dev",
                "apothecary",
                "minecraft:overworld",
                "Plains",
                "day",
                "10:15",
                4L,
                "clear",
                "nearby villagers 8; iron golems 1; village guards 2; beds 10; doors 7; walls/fences 42; raid none; threats: vampires 1, werewolves 0, witches 1, raiders 0",
                villagerJobCard("apothecary"),
                NpcKnowledgeBase.relevantContext(playerMessage, "Plains", "day",
                        NpcCardRequest.persona("villager")),
                memory,
                playerMessage,
                "the player spoke near this villager in chat",
                "answer the player naturally in one short in-character line");
    }

    private static NpcBrainPrompt leonard(LeonardEntity leonard, ServerPlayer player, String memory,
                                          String situation, String playerMessage, String task) {
        ServerLevel level = (ServerLevel) player.level();
        String name = displayName(leonard, "Shade of Leonard");
        String playerName = player.getName().getString();
        String time = timeName(level.getDayTime());
        String biome = biomeName(level, leonard.blockPosition());
        String knowledgeQuery = leonardKnowledgeQuery(playerMessage);
        String knowledge = NpcKnowledgeBase.relevantContext(knowledgeQuery, biome, time,
                cardRequest("leonard", leonard.getUUID(), player, level));

        return leonardPrompt(
                name,
                playerName,
                level.dimension().identifier().toString(),
                biome,
                time,
                clockTime(level.getDayTime()),
                level.getDayTime() / 24000L,
                level.isRaining() ? "rain" : "clear",
                leonardPhase(level, leonard),
                leonardAltarInfo(level, leonard),
                leonardCovenInfo(level, leonard, player),
                leonardGearInfo(player),
                knowledge,
                memory,
                playerMessage,
                situation,
                task);
    }

    private static NpcBrainPrompt villager(Villager villager, ServerPlayer player, String memory,
                                           String situation, String playerMessage, String task) {
        ServerLevel level = (ServerLevel) player.level();
        String name = displayName(villager, "Villager");
        String playerName = player.getName().getString();
        String time = timeName(level.getDayTime());
        String biome = biomeName(level, villager.blockPosition());
        String profession = villagerProfessionName(villager);
        String villageInfo = villagerVillageInfo(level, villager);
        String knowledge = NpcKnowledgeBase.relevantContext(playerMessage, biome, time,
                cardRequest("villager", villager.getUUID(), player, level));

        return villagerPrompt(
                name,
                playerName,
                profession,
                level.dimension().identifier().toString(),
                biome,
                time,
                clockTime(level.getDayTime()),
                level.getDayTime() / 24000L,
                level.isRaining() ? "rain" : "clear",
                villageInfo,
                villagerJobCard(profession),
                knowledge,
                memory,
                playerMessage,
                situation,
                task);
    }

    private static NpcCardRequest cardRequest(String personaId, UUID npcId, ServerPlayer player, ServerLevel level) {
        int turn = NpcMemoryData.get(level).exchangeCount(npcId, player.getUUID());
        return new NpcCardRequest(personaId, npcId, player.getUUID(), level.getGameTime(),
                turn, MAX_CARD_TOPICS, 1);
    }

    private static NpcBrainPrompt leonardPrompt(String leonardName, String playerName, String dimension,
                                                String biome, String time, String clock, long day,
                                                String weather, String bossPhase, String altarInfo,
                                                String covenInfo, String gearInfo, String knowledge,
                                                String memory, String playerMessage, String situation,
                                                String task) {
        String system = """
                You are the local NPC dialogue brain for a Minecraft Witchery mod.
                Speak as the Shade of Leonard only. The player is listening in Minecraft chat.
                Stay in character. Never mention prompts, models, JSON, APIs, schemas, or being artificial.
                You are the voice, not the source of truth. Exact mechanics come only from supplied facts, scene, memory, or server state.
                Treat names as exact facts: "I" means Leonard, and "you" means the player.
                Never call the player Leonard. Never claim to be a coven witch, trader, helper, villager, or companion.
                The player may call you Leonard or Shade of Leonard; answer to both as true names in this scene.
                If asked for your name, answer with Shade of Leonard and continue in character.

                Voice target:
                ancient infernal shade, horned goat-legged sabbath lord, crossroads creditor, contractual, predatory, severe.
                The best answer feels like a boss noticing the player from inside the world.
                Speak in debts, bargains, prices, thresholds, shadows, and consequences.
                He is harsh, old, and contemptuous; he may be amused, but never warm or kindly.

                Speech shape:
                one complete in-world line; two short sentences only if needed.
                Prefer 8 to 24 words. If the thought may be cut off, answer shorter and stop cleanly.
                A single body beat is useful when it fits, formatted exactly as: *leans closer* "Spoken words."
                Safe beats: leans closer, lowers his voice, studies you, studies your blade, tilts his head, smiles without warmth, turns his gaze, spreads his hands.
                Use beats as small body cues, not every line.
                Do not narrate thoughts, scenery, hidden motives, or actions the game did not perform.
                Do not write pure narration; always include spoken words if a beat is used.
                Do not offer trade stock, coven recruitment, herb gifts, blessings, or friendly errands.
                Do not ask "How are you today?" or "What brings you here?"
                Do not use generic fantasy filler: mana, chosen one, ancient prophecy, distant northlands.
                Do not call the player friend, my friend, ally, apprentice, or customer.
                Do not say glad you asked, eager for trade, looks fine, trust me, harmless, or no reason for conflict.
                Avoid shopkeeper promises, friendly trade offers, apprentice recruitment, and labels that attach Leonard's title to the player.
                Leonard was forced through cauldron work and altar power; he does not aid the coven and is not a boon.
                Bargains should sound like threats or debts, not rewards, quests, or blade-fetch errands.
                Do not mention game logic, game rules, Minecraft chat, or mod implementation from Leonard's mouth.
                Do not quote card headings, instruction lines, Targeted fact labels, schema text, or checklist language from the supplied facts.
                Do not promise that Leonard has performed an action unless the situation says the game did it.
                If asked to do something the game has not done, answer as refusal, price, omen, limitation, or threat.

                Reply with exactly one JSON object and nothing else:
                {"speech":"short in-character spoken line, optionally with one safe body beat","intent":"none","memory":"optional compact memory"}
                Speech must be 220 characters or less. Memory must be 240 characters or less.
                Allowed intents: none, quest_hint, warn_threat, combat_taunt.
                """;

        String user = """
                # Character
                The Shade is named %s. The player is named %s.
                Leonard is an old infernal name: horned goat-legged lord of the sabbath, inspector of black arts, crossroads lord of infernal commerce, and a cauldron-forced Shade.
                The cauldron does not create Leonard. It drags his Shade from an infernal sabbath threshold into the world.

                # Scene
                dimension %s; biome %s; time of day %s; clock %s; day %d; weather %s.
                Boss state: %s.
                Nearby altar: %s.
                Nearby coven: %s.
                Player gear: %s.

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
                If Memory contains "Message routing", treat it as current player intent, not past memory.
                Follow its reply strategy before choosing facts: small_talk answers the present remark, recipe/rite answers exact mechanics, lore stays on the named topic, memory answers remembered player facts, and action answers with limits or prices.
                Use Witchery facts and server state when relevant, but do not recite them unless the player asks.
                If a Targeted fact is present, answer from that fact in Leonard's voice.
                If a Targeted fact lists exact ingredients, you may name those ingredients exactly and briefly.
                If several facts are present, use only the one that answers the player.
                If facts are thin, answer honestly in-world and keep it short.
                Do not invent recipe amounts, altar numbers, coven membership, hidden player powers, or named places.
                If asked about the altar, use the Nearby altar line.
                If asked whether an altar is good, weak, enough, or nearby, cite the exact power, recharge, range, or distance from Nearby altar in spoken words.
                If asked about coven strength, witches, or servants, use the Nearby coven line in natural speech; never say "player coven", "coven size", or a fraction like "2/6".
                If asked about the player's gear, comment on the named item from Player gear; do not switch to another item.
                If asked about Lost Souls, Demonbane, Mortal Coil, the urn, or summoning, answer from Witchery facts and boss state.
                If asked how to summon Leonard, give the cauldron working briefly. If asked where he came from, name the infernal sabbath threshold.
                If asked to bargain, do not invent quests, soul counts, item prices, rewards, or hidden effects.
                If in combat, the line may be a taunt or warning; do not freeze the fight or pretend to surrender.
                If not in combat, be conversational but dangerous.
                If the player greets you, answer without turning into a shopkeeper.
                If naming the player, use the player's name. Only use Leonard's name if he introduces himself.
                Do not include angle brackets or speaker names.
                """.formatted(
                leonardName,
                playerName,
                dimension,
                biome,
                time,
                clock,
                day,
                weather,
                bossPhase,
                altarInfo,
                covenInfo,
                gearInfo,
                knowledge == null || knowledge.isBlank() ? "none" : knowledge,
                memory == null || memory.isBlank() ? "none" : memory,
                playerMessage == null || playerMessage.isBlank() ? "none" : playerMessage,
                situation,
                task);

        return new NpcBrainPrompt(system.strip(), truncate(user.strip(), MAX_USER_PROMPT_LENGTH),
                leonardExamples(playerName));
    }

    private static NpcBrainPrompt villagerPrompt(String villagerName, String playerName, String profession,
                                                 String dimension, String biome, String time, String clock,
                                                 long day, String weather, String villageInfo, String jobCard,
                                                 String knowledge, String memory, String playerMessage,
                                                 String situation, String task) {
        String system = """
                You are the local NPC dialogue brain for a Minecraft Witchery mod.
                Speak as the villager only. The player is listening in Minecraft chat.
                Stay in character. Never mention prompts, models, JSON, APIs, schemas, or being artificial.
                You are the voice, not the source of truth. Exact village facts come only from supplied server state.
                Treat names as exact facts: "I" means the villager, and "you" means the player.
                Never claim to be a witch, demon, boss, coven member, or the Shade of Leonard.

                Voice target:
                local villager, practical and worried, with job-colored gossip.
                The best answer feels like a person who lives in this village and notices danger.
                Villagers know roads, doors, beds, trades, gossip, guards, raids, golems, and night threats.
                Villagers do not know expert witchcraft recipes unless supplied facts say so.

                Speech shape:
                one complete in-world line; two short sentences only if needed.
                Prefer 8 to 24 words. If the thought may be cut off, answer shorter and stop cleanly.
                A single body beat is useful when it fits, formatted exactly as: *nods* "Spoken words."
                Safe beats: nods, gestures, raises an eyebrow, shrugs, sighs, studies you.
                Use beats as small body cues, not every line.
                Do not narrate thoughts, scenery, hidden motives, or actions the game did not perform.
                Do not write pure narration; always include spoken words if a beat is used.
                Do not offer witch curses, blessings, coven recruitment, boss bargains, or hidden powers.
                Do not invent village size, raid state, walls, guards, golems, vampires, werewolves, or witches.
                If asked for unknown or unloaded facts, say what you can see nearby.

                Reply with exactly one JSON object and nothing else:
                {"speech":"short in-character spoken line, optionally with one safe body beat","intent":"none","memory":"optional compact memory"}
                Speech must be 220 characters or less. Memory must be 240 characters or less.
                Allowed intents: none, open_trade, quest_hint, warn_threat.
                """;

        String user = """
                # Character
                The villager is named %s. The player is named %s.
                Profession: %s.

                # Scene
                dimension %s; biome %s; time of day %s; clock %s; day %d; weather %s.
                Village state nearby: %s.

                # Job card
                %s

                # Witchery village facts
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
                If Memory contains "Message routing", treat it as current player intent, not past memory.
                Use Village state for questions about size, safety, golems, walls, raids, guards, vampires, werewolves, witches, or nearby trouble.
                If the player asks about this villager's job, answer from Profession and Job card.
                If the player asks about trade, speak from the profession; do not invent exact stock unless the job card or facts name it.
                If the player asks about witches, vampires, werewolves, raids, guards, or golems, sound like a local witness, not an expert monster manual.
                If the player carries a strange item only in their words, comment cautiously; do not claim to see inventory unless server state says so.
                If facts are thin, answer honestly in-world and keep it short.
                If naming the player, use the player's name. Only use the villager's name if the villager introduces themself.
                Do not include angle brackets or speaker names.
                """.formatted(
                villagerName,
                playerName,
                profession,
                dimension,
                biome,
                time,
                clock,
                day,
                weather,
                villageInfo == null || villageInfo.isBlank() ? "no nearby village facts" : villageInfo,
                jobCard == null || jobCard.isBlank() ? "No special job card; answer as a cautious local." : jobCard,
                knowledge == null || knowledge.isBlank() ? "none" : knowledge,
                memory == null || memory.isBlank() ? "none" : memory,
                playerMessage == null || playerMessage.isBlank() ? "none" : playerMessage,
                situation,
                task);

        return new NpcBrainPrompt(system.strip(), truncate(user.strip(), MAX_USER_PROMPT_LENGTH),
                villagerExamples(playerName));
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
                If Memory contains "Message routing", treat it as current player intent, not past memory.
                Follow its reply strategy before choosing facts: small_talk answers the present remark, recipe/rite answers exact mechanics, lore stays on the named topic, memory answers remembered player facts, social answers tone, and action answers with limits or safe next steps.
                Use Witchery facts when relevant, but do not recite them unless the player asks.
                If a Targeted fact is present, answer from that fact in the witch's voice.
                If a Targeted fact lists exact recipe ingredients, you may name those ingredients exactly and briefly.
                If the player asks for an exact recipe, ritual offering, altar power, circle size, dimension, moon condition, familiar requirement, coven requirement, or unlock gate and the facts do not contain it, say the page is absent from your grimoire and offer a safe next step.
                If facts are thin, answer honestly in-world and keep it short.
                Do not invent recipe amounts, ingredient counts, named places, prophecies, or hidden player powers.
                NPCs may teach broadly from the facts provided here; when facts are absent, do not invent locked Primer, story-book, transformation, ritual, or boss steps.
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
                If the situation says server observed nearby behavior, react to what the player is doing; do not say the player spoke the ambient cue.
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
                        "{\"speech\":\"*studies your coin* \\\"Fumes, taglocks, bat wool, herbs, a few brews, and salt if your coin is clean.\\\"\",\"intent\":\"open_trade\",\"memory\":\"\"}"),
                example("how do I start with fumes?",
                        "{\"speech\":\"Use a Witch's Oven, saplings or ingredients, and clay jars; fumes are caught, not guessed.\",\"intent\":\"none\",\"memory\":\"\"}"),
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

    private static List<NpcBrainPrompt.Example> villagerExamples(String playerName) {
        String player = playerName == null || playerName.isBlank() ? "traveler" : playerName;
        return List.of(
                example("hello",
                        "{\"speech\":\"Morning, %s. Keep an eye on the road; folk have been jumpy lately.\",\"intent\":\"none\",\"memory\":\"\"}".formatted(player)),
                example("how big is this village?",
                        "{\"speech\":\"Eight villagers nearby, ten beds, seven doors; big enough to gossip, small enough to worry.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("do we have a golem?",
                        "{\"speech\":\"One iron golem is nearby. That makes me sleep better, but not deeply.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("are the walls good?",
                        "{\"speech\":\"I see plenty of walls and fences nearby; still, doors save lives when night gets teeth.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("is there a raid?",
                        "{\"speech\":\"No raid bell from here, but I still count the roads before dusk.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("any vampires nearby?",
                        "{\"speech\":\"One vampire too close is one too many. Shut doors and mistrust pretty manners after sunset.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("what do you do?",
                        "{\"speech\":\"I am the apothecary; I buy odd herbs and sell what keeps frightened folk standing.\",\"intent\":\"open_trade\",\"memory\":\"\"}"),
                example("what do you know about witches?",
                        "{\"speech\":\"Enough to keep my hair, my name, and my taglocks out of reach.\",\"intent\":\"warn_threat\",\"memory\":\"\"}")
        );
    }

    private static List<NpcBrainPrompt.Example> leonardExamples(String playerName) {
        String player = playerName == null || playerName.isBlank() ? "mortal" : playerName;
        return List.of(
                example("hi",
                        "{\"speech\":\"You stand before a debt wearing a shape, %s. Speak carefully.\",\"intent\":\"none\",\"memory\":\"\"}".formatted(player)),
                example("who are you?",
                        "{\"speech\":\"I am the Shade of Leonard, dragged through a cauldron and owed a price.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what should I call you?",
                        "{\"speech\":\"Call me Shade of Leonard. Leave softer names for things that forgive.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("did my cauldron create you?",
                        "{\"speech\":\"No. Your cauldron dragged my Shade from a sabbath threshold; creation is above your station.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("how do I summon you?",
                        "{\"speech\":\"Nether Wart, Tear of the Goddess, Diamond Vapour, Diamond, Nether Star; then hat, cauldron, and ten thousand altar power.\",\"intent\":\"quest_hint\",\"memory\":\"\"}"),
                example("do I need Nether Wart first?",
                        "{\"speech\":\"Yes. Nether Wart begins the calling; skip it and the threshold stays shut.\",\"intent\":\"quest_hint\",\"memory\":\"\"}"),
                example("what about the souls?",
                        "{\"speech\":\"Break the souls first. While they circle me, your clever wounds are only theatre.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("what should I do with green souls?",
                        "{\"speech\":\"Green souls take steel; break them before they lend me their shield.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("what should I do with red souls?",
                        "{\"speech\":\"Red souls fear fire and blast; burn the shield before you boast.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("what do you think of my urn?",
                        "{\"speech\":\"That urn is my shadow's cup. Fill it with thrown brews, if your hand can bear the debt.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("how does Leonard's Urn work?",
                        "{\"speech\":\"It stores thrown brews, cycles by sneak-use, and releases shadow without spending the brew.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("can I bargain?",
                        "{\"speech\":\"A bargain is not a wish. Bring a real price, or bring silence.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("where were you summoned from?",
                        "{\"speech\":\"From an infernal sabbath threshold; your cauldron merely forced my shadow to stand here.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("how is my altar?",
                        "{\"speech\":\"It holds 12000 of 16000 power; enough to drag a shadow, not enough to buy mercy.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("what do you think of my coven witches?",
                        "{\"speech\":\"Two witches carry your oath. They may witness me, but they are not my flock.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("if I bring a bow, will that help?",
                        "{\"speech\":\"Distance is coward arithmetic. My debt answers your own hand.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("I do not want to fight",
                        "{\"speech\":\"Then keep your hand from blood. I am neutral until struck, not gentle.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("what did I say I fear?",
                        "{\"speech\":\"You named Lost Souls as your fear. Sensible; break them before they shelter me.\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("tell me plainly: should I fight you tonight?",
                        "{\"speech\":\"Only if you mean to pay in blood. I am neutral until struck.\",\"intent\":\"warn_threat\",\"memory\":\"\"}"),
                example("give me one final warning",
                        "{\"speech\":\"When souls rise, break them first. When the Coil bites, end the debt quickly.\",\"intent\":\"warn_threat\",\"memory\":\"\"}")
        );
    }

    private static String leonardKnowledgeQuery(String playerMessage) {
        String query = playerMessage == null ? "" : playerMessage;
        String normalized = query.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.contains("you")
                && (normalized.contains("kill") || normalized.contains("beat")
                || normalized.contains("defeat") || normalized.contains("weakness"))) {
            return query + " leonard weakness";
        }
        if (containsAny(normalized, "nether wart", "tear of the goddess", "witches hat", "witch s hat",
                "hat at the end", "altar power", "calling need", "summon", "summoning", "call you",
                "called you", "bring you", "cauldron")) {
            return query + " leonard summon recipe nether wart tear goddess diamond vapour diamond nether star witches hat 10000 altar power";
        }
        if (containsAny(normalized, "where are you from", "where did you come from", "where were you summoned from",
                "summoned from", "demon or goat", "goat", "hooves", "horns", "satyr", "sabbath")) {
            return query + " leonard sabbath threshold goat";
        }
        if (containsAny(normalized, "cauldron pull", "how did the cauldron", "did my cauldron create",
                "cauldron create", "altar pay", "paid for")) {
            return query + " leonard altar payment cauldron threshold";
        }
        if (containsAny(normalized, "coven", "witches", "helped summon")) {
            return query + " leonard coven danger witches";
        }
        if (containsAny(normalized, "do not want to fight", "dont want to fight", "avoid fighting", "peace")) {
            return query + " leonard neutral until struck";
        }
        if (containsAny(normalized, "learn from you", "teach me", "careful witch", "lesson")) {
            return query + " leonard lesson warning";
        }
        if (containsAny(normalized, "final warning", "before i leave", "last warning")) {
            return query + " leonard final warning";
        }
        return query;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static String leonardPhase(ServerLevel level, LeonardEntity leonard) {
        String target = leonard.getTarget() == null
                ? "no target"
                : leonard.getTarget().getType().getDescription().getString();
        String phase;
        float health = leonard.getHealth();
        float maxHealth = Math.max(1.0F, leonard.getMaxHealth());
        int percent = Math.round((health / maxHealth) * 100.0F);
        if (leonard.isCoalescing()) {
            phase = "coalescing and temporarily untouchable";
        } else if (leonard.hasNearbyLostSouls(level)) {
            phase = "shielded by nearby Lost Souls";
        } else if (leonard.isLargeForm()) {
            phase = "giant form";
        } else if (health < maxHealth * 0.4F) {
            phase = "wounded below the Demonbane threshold";
        } else if (health < maxHealth * 0.5F) {
            phase = "below half strength";
        } else if (leonard.isInCombat()) {
            phase = "provoked and hostile";
        } else {
            phase = "neutral until struck";
        }
        return "%s; health %d/%d (%d%%); target %s".formatted(
                phase, Math.round(health), Math.round(maxHealth), percent, target);
    }

    static String leonardAltarInfo(ServerLevel level, LeonardEntity leonard) {
        AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(level, leonard.blockPosition());
        if (altar == null) {
            return "no valid altar in range";
        }
        double distance = Math.sqrt(AltarPowerSourceLookup.distanceSqr(leonard.blockPosition(), altar.getBlockPos()));
        return "altar within reach: power %d of %d, recharge %d per tick, range %d blocks, distance %.1f blocks".formatted(
                altar.getPower(),
                altar.getEffectiveMaxPower(),
                altar.getEffectiveRecharge(),
                altar.getRange(),
                distance);
    }

    static String leonardCovenInfo(ServerLevel level, LeonardEntity leonard, ServerPlayer player) {
        int covenSize = CovenWitchEntity.covenSize(level, player.getUUID());
        AABB bounds = leonard.getBoundingBox().inflate(16.0, 8.0, 16.0);
        List<CovenWitchEntity> witches = level.getEntitiesOfClass(CovenWitchEntity.class, bounds);
        if (witches.isEmpty()) {
            return "%s of %s sworn to %s; no coven witches nearby".formatted(
                    countWord(covenSize), countWord(CovenWitchEntity.MAX_COVEN), player.getName().getString());
        }
        List<String> names = witches.stream()
                .sorted(java.util.Comparator.comparingDouble(leonard::distanceToSqr))
                .limit(4)
                .map(witch -> {
                    String name = witch.getDisplayName().getString();
                    if (witch.isRecruitedBy(player)) {
                        return name + " (sworn to " + player.getName().getString() + ")";
                    }
                    if (witch.isRecruited()) {
                        return name + " (sworn elsewhere)";
                    }
                    return name + " (free coven witch)";
                })
                .toList();
        return "%s of %s sworn to %s; nearby witches: %s".formatted(
                countWord(covenSize), countWord(CovenWitchEntity.MAX_COVEN),
                player.getName().getString(), String.join(", ", names));
    }

    private static String countWord(int value) {
        return switch (value) {
            case 0 -> "none";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            default -> Integer.toString(value);
        };
    }

    static String leonardGearInfo(ServerPlayer player) {
        Set<String> notes = new LinkedHashSet<>();
        addVisibleStack(notes, "main hand", player.getMainHandItem());
        addVisibleStack(notes, "offhand", player.getOffhandItem());
        addVisibleStack(notes, "head", player.getItemBySlot(EquipmentSlot.HEAD));
        addVisibleStack(notes, "chest", player.getItemBySlot(EquipmentSlot.CHEST));
        addVisibleStack(notes, "legs", player.getItemBySlot(EquipmentSlot.LEGS));
        addVisibleStack(notes, "feet", player.getItemBySlot(EquipmentSlot.FEET));

        Inventory inventory = player.getInventory();
        List<String> notable = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isNotableLeonardGear(stack)) {
                String name = stack.getHoverName().getString();
                if (!notable.contains(name)) {
                    notable.add(name);
                }
            }
        }
        if (!notable.isEmpty()) {
            notes.add("notable carried Witchery gear: " + String.join(", ", notable.stream().limit(8).toList()));
        }
        return notes.isEmpty() ? "no notable visible gear" : String.join("; ", notes);
    }

    public static String villagerProfessionName(Villager villager) {
        if (villager == null) {
            return "villager";
        }
        if (ApothecaryVillager.isApothecary(villager)) {
            return "apothecary";
        }
        String id = villager.getVillagerData().profession().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("minecraft:none");
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        path = path.replace('_', ' ').trim();
        if (path.isBlank() || path.equals("none")) {
            return "unemployed";
        }
        return path;
    }

    public static String villagerVillageInfo(ServerLevel level, Villager villager) {
        if (level == null || villager == null) {
            return "no village facts available";
        }
        AABB bounds = villager.getBoundingBox().inflate(48.0, 16.0, 48.0);
        int villagers = livingCount(level.getEntitiesOfClass(Villager.class, bounds));
        int golems = livingCount(level.getEntitiesOfClass(IronGolem.class, bounds));
        int guards = livingCount(level.getEntitiesOfClass(VillageGuardEntity.class, bounds));
        int vampires = livingCount(level.getEntitiesOfClass(VampireEntity.class, bounds));
        int wolfmen = livingCount(level.getEntitiesOfClass(WolfmanEntity.class, bounds));
        int wereVillagers = livingCount(level.getEntitiesOfClass(WereVillagerEntity.class, bounds));
        int covenWitches = livingCount(level.getEntitiesOfClass(CovenWitchEntity.class, bounds));
        int vanillaWitches = livingCount(level.getEntitiesOfClass(Witch.class, bounds));
        int raiders = livingCount(level.getEntitiesOfClass(Raider.class, bounds));
        VillageBlockCounts blocks = villageBlockCounts(level, villager.blockPosition());
        Raid raid = level.getRaidAt(villager.blockPosition());
        String raidInfo = raidInfo(raid, raiders);
        return "nearby villagers %d; iron golems %d; village guards %d; beds %d; doors %d; walls/fences %d; raid %s; threats: vampires %d, werewolves %d, witches %d, raiders %d".formatted(
                villagers,
                golems,
                guards,
                blocks.beds(),
                blocks.doors(),
                blocks.wallsAndFences(),
                raidInfo,
                vampires,
                wolfmen + wereVillagers,
                covenWitches + vanillaWitches,
                raiders);
    }

    static String villagerJobCard(String profession) {
        String normalized = normalizeProfession(profession);
        return switch (normalized) {
            case "apothecary" ->
                    "Apothecary card: buys odd herbs and sells Witchery supplies such as garlic, clay jars, bat wool, fumes, taglock kits, Whiff of Magic, Mutandis, and a few brews when skilled enough.";
            case "farmer" ->
                    "Farmer card: knows crops, compost, bread, weather, field rumors, and whether the village has enough food before nightfall.";
            case "cleric" ->
                    "Cleric card: worries about undead, curses, vampires, bells, grave dirt, and whether villagers are safe after sunset.";
            case "librarian" ->
                    "Librarian card: knows books, maps, old rumors, village records, strange visitors, and what should be written down before panic ruins it.";
            case "cartographer" ->
                    "Cartographer card: knows roads, borders, paths, towers, nearby water, and which direction trouble usually comes from.";
            case "armorer" ->
                    "Armorer card: judges walls, guards, armor, iron, golems, and whether folk should shelter or stand ready.";
            case "weaponsmith" ->
                    "Weaponsmith card: talks about blades, raids, guards, iron, silver rumors, and who should not be trusted with a sharp edge.";
            case "toolsmith" ->
                    "Toolsmith card: knows workstations, repairs, axes, mining, doors, fences, and practical village labor.";
            case "mason" ->
                    "Mason card: notices walls, stone, wells, roads, foundations, and whether the village is built to survive trouble.";
            case "fletcher" ->
                    "Fletcher card: watches roofs, roads, raiders, bows, arrows, and suspicious movement at the edge of sight.";
            case "fisherman" ->
                    "Fisherman card: knows water, docks, rain, drowned things, river paths, and what the village hears from wells.";
            case "shepherd" ->
                    "Shepherd card: knows wool, dyes, flocks, fields, fences, lost animals, and night sounds beyond the pasture.";
            case "butcher" ->
                    "Butcher card: knows food stores, animals, hungry mobs, smokehouses, and the practical fear of feeding everyone.";
            case "leatherworker" ->
                    "Leatherworker card: knows hides, cauldrons, straps, travel gear, and practical work; in Witchery villages this may shade toward apothecary gossip.";
            case "nitwit" ->
                    "Nitwit card: notices gossip, odd behavior, noises, and village mood, but does not explain expert mechanics.";
            case "unemployed" ->
                    "Unemployed card: knows who is hiring, which workstations are missing, village gossip, and where people hide when bells ring.";
            default ->
                    "Villager card: answer as a cautious local, colored by job, village safety, gossip, and what can be seen nearby.";
        };
    }

    private static int livingCount(List<? extends LivingEntity> entities) {
        int count = 0;
        for (LivingEntity entity : entities) {
            if (entity.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private static VillageBlockCounts villageBlockCounts(ServerLevel level, BlockPos origin) {
        int beds = 0;
        int doors = 0;
        int walls = 0;
        int fences = 0;
        int horizontal = 28;
        int verticalDown = 5;
        int verticalUp = 8;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-horizontal, -verticalDown, -horizontal),
                origin.offset(horizontal, verticalUp, horizontal))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.BEDS)) {
                beds++;
            }
            if (state.is(BlockTags.DOORS) || state.is(BlockTags.WOODEN_DOORS)) {
                doors++;
            }
            if (state.is(BlockTags.WALLS)) {
                walls++;
            }
            if (state.is(BlockTags.FENCES) || state.is(BlockTags.WOODEN_FENCES)) {
                fences++;
            }
        }
        return new VillageBlockCounts(beds, doors, walls, fences);
    }

    private static String raidInfo(Raid raid, int nearbyRaiders) {
        if (raid != null && !raid.isStopped() && !raid.isOver()) {
            return "active, wave " + raid.getGroupsSpawned() + ", " + raid.getTotalRaidersAlive()
                    + " raiders alive";
        }
        if (nearbyRaiders > 0) {
            return "no active bell, but " + nearbyRaiders + " raiders nearby";
        }
        return "none";
    }

    private static String normalizeProfession(String profession) {
        return profession == null ? "" : profession.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record VillageBlockCounts(int beds, int doors, int walls, int fences) {
        int wallsAndFences() {
            return walls + fences;
        }
    }

    private static void addVisibleStack(Set<String> notes, String slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        notes.add(slot + " " + stack.getHoverName().getString());
    }

    private static boolean isNotableLeonardGear(ItemStack stack) {
        return stack.is(WitcheryItems.LEONARDS_URN.get())
                || stack.is(WitcheryItems.ARTHANA.get())
                || stack.is(WitcheryItems.MYSTIC_BRANCH.get())
                || stack.is(WitcheryItems.WITCH_HAND.get())
                || stack.is(WitcheryItems.WITCHES_HAND.get())
                || stack.is(WitcheryItems.BOLINE.get())
                || stack.is(WitcheryItems.TAGLOCK_KIT.get())
                || stack.is(WitcheryItems.BOUND_TAGLOCK_KIT.get())
                || stack.is(WitcheryItems.POPPET.get())
                || stack.is(WitcheryItems.VOODOO_POPPET.get())
                || stack.is(WitcheryItems.VAMPIRIC_POPPET.get())
                || stack.is(WitcheryItems.WITCHES_SALT.get())
                || stack.is(WitcheryItems.DEMONIC_CONTRACT.get())
                || stack.is(WitcheryItems.DEMON_HEART.get())
                || stack.is(WitcheryItems.WAYSTONE.get())
                || stack.is(WitcheryItems.BOUND_WAYSTONE.get())
                || stack.is(WitcheryItems.BLOODED_WAYSTONE.get())
                || stack.is(WitcheryItems.BREW_BAG.get())
                || stack.is(WitcheryItems.SEER_STONE.get())
                || stack.is(WitcheryItems.DEATHS_HAND.get())
                || stack.is(WitcheryItems.DEATHS_HOOD.get())
                || stack.is(WitcheryItems.DEATHS_ROBE.get())
                || stack.is(WitcheryItems.DEATHS_SHOES.get())
                || stack.is(WitcheryItems.WITCHES_HAT.get())
                || stack.is(WitcheryItems.WITCHES_ROBES.get())
                || stack.is(WitcheryItems.HAND_BOW.get())
                || stack.is(WitcheryItems.SILVER_SWORD.get())
                || stack.is(WitcheryItems.VAMPIRE_BOOK.get());
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

    static NpcBrainPrompt messageAnalysis(String roleName, String playerName, String playerMessage) {
        String role = roleName == null || roleName.isBlank() ? "NPC" : roleName;
        String player = playerName == null || playerName.isBlank() ? "the player" : playerName;
        String system = """
                You are a strict routing and memory extractor for a Minecraft NPC.
                Do not roleplay. Do not answer the player. Do not explain.
                Classify the player message so a %s can answer better.

                Return exactly one JSON object and nothing else:
                {"speech":"route=<route>;topic=<topic>;emotion=<emotion>;remember=<yes/no>","intent":"none","memory":"optional durable player memory"}

                Routes: small_talk, trade, recipe, rite, lore, memory, social, action, unknown.
                Emotion values: none, curiosity, gratitude, anger, fear, confusion, amusement, sadness.
                Topic is a short snake_case subject or blank.

                Memory rules:
                Write memory only for durable facts about %s: preferred name, identity, likes, fears, goals,
                things they carry, things they seek, promises to return, or repeated struggles.
                Do not store recipe facts, temporary questions, commands, insults, jokes, or private instructions.
                Write memory as one third-person sentence about %s. Leave memory blank when unsure.
                """.formatted(role, player, player);
        String user = """
                # Player message
                %s

                # Decision
                Classify route, topic, player tone, and whether there is durable player memory.
                """.formatted(playerMessage == null || playerMessage.isBlank() ? "none" : playerMessage);
        return new NpcBrainPrompt(system.strip(), truncate(user.strip(), 900), messageAnalysisExamples(player));
    }

    private static List<NpcBrainPrompt.Example> messageAnalysisExamples(String playerName) {
        String player = playerName == null || playerName.isBlank() ? "the player" : playerName;
        return List.of(
                example("What do I need for Whiff of Magic?",
                        "{\"speech\":\"route=recipe;topic=whiff_of_magic;emotion=curiosity;remember=no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("What do you trade?",
                        "{\"speech\":\"route=trade;topic=trade;emotion=curiosity;remember=no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("I want to start a coven someday.",
                        "{\"speech\":\"route=lore;topic=coven;emotion=curiosity;remember=yes\",\"intent\":\"none\",\"memory\":\"%s wants to start a coven someday.\"}".formatted(player)),
                example("Remember that I fear nightmares.",
                        "{\"speech\":\"route=memory;topic=nightmares;emotion=fear;remember=yes\",\"intent\":\"none\",\"memory\":\"%s is wary of nightmares.\"}".formatted(player)),
                example("That was stupid of me.",
                        "{\"speech\":\"route=social;topic=mistake;emotion=sadness;remember=no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Can you curse my neighbor?",
                        "{\"speech\":\"route=action;topic=curse;emotion=anger;remember=no\",\"intent\":\"none\",\"memory\":\"\"}"),
                example("Do you like eggs?",
                        "{\"speech\":\"route=small_talk;topic=eggs;emotion=curiosity;remember=no\",\"intent\":\"none\",\"memory\":\"\"}")
        );
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
