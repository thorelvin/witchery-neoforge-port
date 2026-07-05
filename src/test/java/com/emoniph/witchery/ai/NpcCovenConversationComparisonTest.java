package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcCovenConversationComparisonTest {
    private static final String PLAYER = "Dev";
    private static final String FREE_WITCH = "Mara Fen";
    private static final String SWORN_WITCH = "Lilith Poe";
    private static final String[] QUESTIONS = {
            "hello",
            "who are you?",
            "what is the difference between witches and coven witches?",
            "will you follow me?",
            "will you protect me?",
            "what do you trade?",
            "what is witches salt for?",
            "thank you for standing with me"
    };

    @Test
    void sameQuestionsShowFreeAndSwornCovenTone() {
        List<Turn> turns = transcript();
        printTranscript(turns);

        assertEquals(QUESTIONS.length, turns.size());
        assertEquals("Hello, Dev.", turns.get(0).freeWitch());
        assertTrue(turns.get(0).swornWitch().contains("coven feels steadier"), turns.get(0).swornWitch());
        assertEquals("I am Mara Fen.", turns.get(1).freeWitch());
        assertTrue(turns.get(1).swornWitch().contains("sworn to your coven"), turns.get(1).swornWitch());
        assertTrue(turns.get(2).freeWitch().contains("bargain and trial"), turns.get(2).freeWitch());
        assertTrue(turns.get(2).swornWitch().contains("bloodless kin"), turns.get(2).swornWitch());
        assertTrue(turns.get(3).freeWitch().contains("do not follow folk home"), turns.get(3).freeWitch());
        assertTrue(turns.get(3).swornWitch().contains("keep pace"), turns.get(3).swornWitch());
        assertTrue(turns.get(4).freeWitch().contains("earn an oath"), turns.get(4).freeWitch());
        assertTrue(turns.get(4).swornWitch().contains("brew bottle beside yours"), turns.get(4).swornWitch());
        assertTrue(turns.get(5).freeWitch().startsWith("I trade "), turns.get(5).freeWitch());
        assertTrue(turns.get(5).swornWitch().contains("For you, coven-kin?"), turns.get(5).swornWitch());
        assertTrue(isSaltAnswer(turns.get(6).freeWitch()), turns.get(6).freeWitch());
        assertTrue(isSaltAnswer(turns.get(6).swornWitch()), turns.get(6).swornWitch());
        assertTrue(turns.get(7).freeWitch().contains("compliments have roots"), turns.get(7).freeWitch());
        assertTrue(turns.get(7).swornWitch().contains("I walk it with you now"), turns.get(7).swornWitch());
    }

    private static List<Turn> transcript() {
        List<Turn> turns = new ArrayList<>();
        for (String question : QUESTIONS) {
            turns.add(new Turn(question, answer(question, FREE_WITCH, false), answer(question, SWORN_WITCH, true)));
        }
        return turns;
    }

    private static String answer(String question, String witchName, boolean sworn) {
        return NpcDialogueFallback.covenChat(
                        question, witchName, PLAYER, "Plains", "night", "22:30", 42L, "", sworn)
                .orElse("<no answer>");
    }

    private static boolean isSaltAnswer(String speech) {
        return speech.contains("Witches' salt")
                && (speech.contains("ward") || speech.contains("protective"))
                && speech.contains("hostile magic");
    }

    private static void printTranscript(List<Turn> turns) {
        System.out.println("=== Coven Witch Conversation Comparison ===");
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            System.out.println((i + 1) + ". Player: " + turn.question());
            System.out.println("   Free witch (" + FREE_WITCH + "): " + turn.freeWitch());
            System.out.println("   Sworn coven witch (" + SWORN_WITCH + "): " + turn.swornWitch());
        }
    }

    private record Turn(String question, String freeWitch, String swornWitch) {
    }
}
