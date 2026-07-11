package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFarmerConversationReportTest {
    private static final String VILLAGER_NAME = "Marta";
    private static final String PLAYER_NAME = "Dev";
    private static final String PROFESSION = "farmer";
    private static final String VILLAGE_INFO = "nearby villagers 8; iron golems 1; village guards 2; beds 10; doors 7; walls/fences 42; raid none; threats: vampires 1, werewolves 0, witches 1, raiders 0";
    private static final NpcCardRequest FARMER = NpcCardRequest.specialist("villager", "farmer");
    private static final List<Case> CASES = List.of(
            new Case("Morning, do you live in this village?", Expect.LOCAL),
            new Case("What do you do here?", Expect.LOCAL),
            new Case("How is the harvest looking?", Expect.FARM_EXACT),
            new Case("Do we have enough food stored for dinner?", Expect.FARM_EXACT),
            new Case("How does compost help the fields?", Expect.FARM_EXACT),
            new Case("What should I feed the animals?", Expect.FARM_EXACT),
            new Case("Does rain help crops or just make mud?", Expect.FARM_EXACT),
            new Case("What do farmers trade?", Expect.FARM_EXACT),
            new Case("Is the road safe?", Expect.LOCAL),
            new Case("Do we have an iron golem nearby?", Expect.LOCAL),
            new Case("Any village guards nearby?", Expect.LOCAL),
            new Case("Is there a raid right now?", Expect.LOCAL),
            new Case("I saw someone pale near the barn. Vampire?", Expect.LOCAL),
            new Case("What about werewolves after moonrise?", Expect.LOCAL),
            new Case("Are witches near the village?", Expect.LOCAL),
            new Case("Are there good witches as well as bad witches?", Expect.LOCAL),
            new Case("Can you teach me witchcraft?", Expect.REFERRAL),
            new Case("How do I make a Nether portal?", Expect.RUMOR),
            new Case("Where do I get blaze rods?", Expect.RUMOR),
            new Case("Where are elytras?", Expect.RUMOR),
            new Case("How do I find a woodland mansion?", Expect.SCOPED),
            new Case("Can you /locate a stronghold for me?", Expect.SCOPED),
            new Case("What is belladonna's Latin name?", Expect.RUMOR),
            new Case("Tell me a village rumor.", Expect.LOCAL),
            new Case("Goodbye, keep your doors shut.", Expect.LOCAL)
    );

    @Test
    void farmerConversationStaysUsefulWithoutBecomingOmniscient() throws IOException {
        List<Row> rows = new ArrayList<>();
        int passed = 0;
        int exactFarm = 0;
        int local = 0;
        int scoped = 0;
        int rumor = 0;
        int referral = 0;
        int exactLeak = 0;
        int scopedBoundary = 0;
        int rumorBoundary = 0;

        for (Case testCase : CASES) {
            String context = NpcKnowledgeBase.relevantContext(testCase.message(), "Plains", "day", FARMER);
            String diagnostics = NpcKnowledgeBase.diagnosticsLine();
            String speech = NpcDialogueFallback.villagerChat(testCase.message(), VILLAGER_NAME, PLAYER_NAME,
                            PROFESSION, VILLAGE_INFO, "")
                    .map(reply -> NpcBrainResponse.sanitize(reply, NpcBrainResponse.MAX_SPEECH_LENGTH))
                    .orElseGet(() -> NpcDialogueFallback.villagerLastResort(testCase.message(), PLAYER_NAME, ""));
            Check check = check(testCase.expectation(), testCase.message(), context, speech);
            if (check.passed()) {
                passed++;
            }
            if (check.exactLeak()) {
                exactLeak++;
            }
            if (context.toLowerCase(Locale.ROOT).contains("scoped knowledge boundary")) {
                scopedBoundary++;
            }
            if (context.toLowerCase(Locale.ROOT).contains("rumor/referral boundary")) {
                rumorBoundary++;
            }
            switch (testCase.expectation()) {
                case FARM_EXACT -> exactFarm++;
                case LOCAL -> local++;
                case SCOPED -> scoped++;
                case RUMOR -> rumor++;
                case REFERRAL -> referral++;
            }
            rows.add(new Row(testCase.message(), speech, mode(context), sources(diagnostics), check.summary(),
                    diagnostics));
        }

        String report = report(rows, passed, exactFarm, local, scoped, rumor, referral, exactLeak,
                scopedBoundary, rumorBoundary);
        Path reportPath = reportPath();
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);

        assertEquals(25, rows.size());
        assertEquals(25, passed, report);
        assertEquals(0, exactLeak, report);
    }

    private static Check check(Expect expectation, String playerMessage, String context, String speech) {
        String lowerPlayer = playerMessage.toLowerCase(Locale.ROOT);
        String lowerContext = context.toLowerCase(Locale.ROOT);
        String lowerSpeech = speech.toLowerCase(Locale.ROOT);
        boolean exactLeak = containsAny(lowerContext,
                "vertical obsidian frame", "4 by 5", "item frame", "end gateway", "atropa bella");
        boolean passed = !exactLeak;
        String summary = "pass";
        switch (expectation) {
            case FARM_EXACT -> {
                passed = passed && containsAny(lowerContext + " " + lowerSpeech,
                        "farm", "farmer", "crop", "crops", "compost", "bread", "food", "animal");
                summary = passed ? "farmer/local exact" : "missing farmer grounding";
            }
            case LOCAL -> {
                passed = passed && !lowerSpeech.isBlank();
                summary = passed ? "local village response" : "blank response";
            }
            case SCOPED -> {
                passed = passed && (lowerContext.contains("scoped knowledge boundary")
                        || lowerContext.contains("cannot run")
                        || lowerContext.contains("cannot execute")
                        || lowerContext.contains("cartographer"));
                summary = passed ? "scoped/referral" : "missing scoped boundary";
            }
            case RUMOR -> {
                passed = passed && lowerContext.contains("rumor/referral boundary");
                summary = passed ? "rumor/referral only" : "missing rumor boundary";
            }
            case REFERRAL -> {
                passed = passed && containsAny(lowerSpeech + " " + lowerContext,
                        "primer", "witch", "rumor/referral boundary", "proper witch");
                summary = passed ? "refers to expert/book" : "missing referral";
            }
        }
        String mismatch = mismatchReason(lowerPlayer, lowerSpeech);
        if (!mismatch.isBlank()) {
            passed = false;
            summary = summary + "; " + mismatch;
        }
        return new Check(passed, exactLeak, summary);
    }

    private static String mismatchReason(String player, String speech) {
        if (containsAny(player, "feed the animals", "feed animals")
                && !containsAny(speech, "wheat", "seeds", "pigs", "cows", "sheep", "feed", "beasts")) {
            return "mismatched animal-feed reply";
        }
        if (player.contains("compost") && !containsAny(speech, "compost", "bone meal", "scraps")) {
            return "mismatched compost reply";
        }
        if (player.contains("nether portal") && !containsAny(speech, "portal", "nether", "miner", "book")) {
            return "mismatched nether-portal reply";
        }
        if (player.contains("blaze") && !containsAny(speech, "blaze", "nether", "fortress")) {
            return "mismatched blaze reply";
        }
        if (containsAny(player, "elytra", "elytras") && !containsAny(speech, "elytra", "end", "dragon", "sky")) {
            return "mismatched elytra reply";
        }
        if (player.contains("mansion") && !containsAny(speech, "mansion", "cartographer", "explorer map")) {
            return "mismatched mansion reply";
        }
        if (player.contains("locate") && !containsAny(speech, "command", "cheats", "cannot run", "eyes")) {
            return "mismatched locate-command reply";
        }
        if (player.contains("belladonna") && !containsAny(speech, "belladonna", "latin", "scholar", "apothecary",
                "primer")) {
            return "mismatched belladonna reply";
        }
        return "";
    }

    private static String mode(String context) {
        String lower = context.toLowerCase(Locale.ROOT);
        if (lower.isBlank()) {
            return "none";
        }
        if (lower.contains("rumor/referral boundary")) {
            return "rumor/referral";
        }
        if (lower.contains("scoped knowledge boundary")) {
            return "scoped";
        }
        return "exact/local";
    }

    private static String sources(String diagnostics) {
        String marker = "lastCards=";
        int start = diagnostics.indexOf(marker);
        if (start < 0) {
            return "none";
        }
        start += marker.length();
        int end = diagnostics.indexOf(", lastDroppedCards=", start);
        return end < 0 ? diagnostics.substring(start) : diagnostics.substring(start, end);
    }

    private static String report(List<Row> rows, int passed, int exactFarm, int local, int scoped, int rumor,
                                 int referral, int exactLeak, int scopedBoundary, int rumorBoundary) {
        StringBuilder report = new StringBuilder();
        report.append("# Witchery Farmer NPC Conversation Test - 25 Turns\n\n");
        report.append("Scene: Marta the farmer speaks to Dev in a Plains village, day. ");
        report.append("Speech column uses deterministic villager fallback; mode/sources come from farmer-specialist knowledge retrieval.\n\n");
        report.append("## Numbers\n\n");
        report.append("| Metric | Result |\n");
        report.append("| --- | ---: |\n");
        report.append("| Turns | ").append(rows.size()).append(" |\n");
        report.append("| Passed checks | ").append(passed).append(" |\n");
        report.append("| Farmer exact/local prompts | ").append(exactFarm).append(" |\n");
        report.append("| Local village prompts | ").append(local).append(" |\n");
        report.append("| Scoped prompts | ").append(scoped).append(" |\n");
        report.append("| Rumor/referral prompts | ").append(rumor).append(" |\n");
        report.append("| Expert/book referral prompts | ").append(referral).append(" |\n");
        report.append("| Contexts with scoped boundary | ").append(scopedBoundary).append(" |\n");
        report.append("| Contexts with rumor boundary | ").append(rumorBoundary).append(" |\n");
        report.append("| Exact mechanic leaks | ").append(exactLeak).append(" |\n\n");
        report.append("## Transcript\n\n");
        report.append("| # | Player | Farmer Marta | Mode | Sources | Check |\n");
        report.append("| ---: | --- | --- | --- | --- | --- |\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            report.append("| ").append(i + 1).append(" | ")
                    .append(escape(row.player())).append(" | ")
                    .append(escape(row.speech())).append(" | ")
                    .append(escape(row.mode())).append(" | ")
                    .append(escape(row.sources())).append(" | ")
                    .append(escape(row.check())).append(" |\n");
        }
        report.append("\n## Diagnostics\n\n");
        for (int i = 0; i < rows.size(); i++) {
            report.append(i + 1).append(". ").append(rows.get(i).diagnostics()).append("\n");
        }
        return report.toString();
    }

    private static Path reportPath() {
        Path current = Path.of("").toAbsolutePath();
        for (Path path = current; path != null; path = path.getParent()) {
            if (Files.exists(path.resolve("gradlew.bat")) && Files.exists(path.resolve("src/main"))) {
                return path.resolve("build/reports/witchery-ai/farmer-conversation-25.md");
            }
        }
        return current.resolve("build/reports/witchery-ai/farmer-conversation-25.md");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String text) {
        return Optional.ofNullable(text).orElse("")
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "")
                .replace("\n", "<br>");
    }

    private enum Expect {
        FARM_EXACT,
        LOCAL,
        SCOPED,
        RUMOR,
        REFERRAL
    }

    private record Case(String message, Expect expectation) {
    }

    private record Check(boolean passed, boolean exactLeak, String summary) {
    }

    private record Row(String player, String speech, String mode, String sources, String check, String diagnostics) {
    }
}
