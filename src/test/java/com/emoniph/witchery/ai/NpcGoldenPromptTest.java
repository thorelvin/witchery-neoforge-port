package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcGoldenPromptTest {
    private static final int MAX_USER_PROMPT_LENGTH = 3200;

    @Test
    void representativePersonaPromptsKeepGoldenGuardrails() {
        for (GoldenPrompt prompt : prompts()) {
            assertGoldenPrompt(prompt);
        }
    }

    private static void assertGoldenPrompt(GoldenPrompt golden) {
        String system = golden.prompt().systemPrompt().toLowerCase(Locale.ROOT);
        String user = golden.prompt().userPrompt().toLowerCase(Locale.ROOT);

        assertTrue(golden.prompt().userPrompt().length() <= MAX_USER_PROMPT_LENGTH,
                golden.name() + " user prompt exceeded max length");
        assertFalse(golden.prompt().systemPrompt().isBlank(), golden.name() + " system prompt was blank");
        assertFalse(golden.prompt().examples().isEmpty(), golden.name() + " examples were missing");

        for (String expected : golden.systemContains()) {
            assertTrue(system.contains(expected.toLowerCase(Locale.ROOT)),
                    golden.name() + " system missing: " + expected + "\n" + golden.prompt().systemPrompt());
        }
        for (String expected : golden.userContains()) {
            assertTrue(user.contains(expected.toLowerCase(Locale.ROOT)),
                    golden.name() + " user missing: " + expected + "\n" + golden.prompt().userPrompt());
        }
        for (String forbidden : golden.userExcludes()) {
            assertFalse(user.contains(forbidden.toLowerCase(Locale.ROOT)),
                    golden.name() + " user included unrelated prompt fragment: " + forbidden
                            + "\n" + golden.prompt().userPrompt());
        }
    }

    private static List<GoldenPrompt> prompts() {
        return List.of(
                new GoldenPrompt(
                        "coven_witch_chat_recipe",
                        NpcPromptBuilder.covenWitchSmoke("tell me the exact recipe for Brew of Sleeping", ""),
                        List.of("Speak as the witch only", "practical, secretive"),
                        List.of("The witch is named Lilith Poe", "biome Plains", "Brew of Sleeping",
                                "Purified Milk", "Whiff of Magic"),
                        List.of("small forest hobgoblin", "apothecary card")),
                new GoldenPrompt(
                        "coven_witch_greeting",
                        NpcPromptBuilder.covenWitchSmoke("hello", ""),
                        List.of("Speak as the witch only", "practical, secretive"),
                        List.of("The witch is named Lilith Poe", "biome Plains", "time of day night",
                                "clock 22:30", "the player spoke near the witch in chat"),
                        List.of("small forest hobgoblin", "apothecary card", "Boss state")),
                new GoldenPrompt(
                        "hobgoblin_chat_trade",
                        NpcPromptBuilder.hobgoblinSmoke("what do you trade?", ""),
                        List.of("Speak as the hobgoblin only", "small forest hobgoblin"),
                        List.of("The hobgoblin is named Hobgoblin", "Trade state", "Koboldite",
                                "nearby goblin altar"),
                        List.of("practical coven witch", "apothecary card")),
                new GoldenPrompt(
                        "hobgoblin_repair",
                        NpcPromptBuilder.cardRepair("hobgoblin", "", "Dev",
                                "how do I get koboldite dust?",
                                "Message routing: route=lore; topic=koboldite; reply strategy=answer exact known route.",
                                "I know fumes.", "Forest", "day").orElseThrow(),
                        List.of("repair one Minecraft Witchery NPC line", "supplied in-world facts"),
                        List.of("Hobgoblin is speaking to Dev", "small greedy hobgoblin", "Scene: Forest, day",
                                "Koboldite", "Bad draft to avoid"),
                        List.of("practical coven witch", "apothecary card")),
                new GoldenPrompt(
                        "leonard_chat_lore",
                        NpcPromptBuilder.leonardSmoke("how were you summoned?", ""),
                        List.of("Speak as the Shade of Leonard only", "crossroads creditor"),
                        List.of("The Shade is named Shade of Leonard", "Nearby altar", "power 12000",
                                "cauldron"),
                        List.of("apothecary card", "nearby goblin altar")),
                new GoldenPrompt(
                        "baba_yaga_chat",
                        NpcPromptBuilder.babaYagaSmoke("how do I fight you?", ""),
                        List.of("Speak as Baba Yaga only", "ancient crone boss witch"),
                        List.of("Baba Yaga is named Baba Yaga", "Swamp", "Nullifying Bolt",
                                "wooden weapons"),
                        List.of("apothecary card", "nearby goblin altar")),
                new GoldenPrompt(
                        "villager_chat_apothecary",
                        NpcPromptBuilder.villagerSmoke("what do you sell?", ""),
                        List.of("Speak as the villager only", "superstitious"),
                        List.of("profession: apothecary", "Village state nearby", "apothecary card",
                                "fumes"),
                        List.of("nearby goblin altar", "Boss state: health")),
                new GoldenPrompt(
                        "reflection_chat_mirror_world",
                        NpcPromptBuilder.reflectionSmoke("why do you look like me and how do I leave?", ""),
                        List.of("Speak as the player's Reflection only", "familiar-but-wrong"),
                        List.of("Mirror World", "hostile Mirror World guardian", "owner Dev",
                                "borrowed the owner's shape"),
                        List.of("apothecary card", "nearby goblin altar"))
        );
    }

    private record GoldenPrompt(String name, NpcBrainPrompt prompt, List<String> systemContains,
                                List<String> userContains, List<String> userExcludes) {
    }
}
