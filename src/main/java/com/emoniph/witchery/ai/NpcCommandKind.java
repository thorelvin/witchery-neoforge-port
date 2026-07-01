package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;

public enum NpcCommandKind {
    MINE("ORDER_MINE: the player is directly telling this NPC to start mining, digging, working, or finding koboldite now"),
    WORSHIP("ORDER_WORSHIP: the player is directly telling this NPC to worship, pray, or go to a nearby goblin patron statue now"),
    STAY("ORDER_STAY: the player is directly telling this NPC to stay, wait, hold still, or stop wandering now"),
    STOP("ORDER_STOP: the player is directly telling this NPC to stop its current movement or work now"),
    COME_HERE("ORDER_COME_HERE: the player is directly telling this NPC to come closer or return to the player now"),
    GO_AWAY("ORDER_GO_AWAY: the player is directly telling this NPC to leave, back away, or go away now"),
    FOLLOW("ORDER_FOLLOW: the player is directly telling this NPC to follow the player now"),
    GUARD("ORDER_GUARD: the player is directly telling this NPC to guard, defend, or protect now");

    private final String intentLabel;

    NpcCommandKind(String intentLabel) {
        this.intentLabel = intentLabel;
    }

    public String intentLabel() {
        return intentLabel;
    }

    public static Optional<NpcCommandKind> candidate(String message) {
        String normalized = normalize(message);
        if (matchesMine(normalized)) {
            return Optional.of(MINE);
        }
        if (matchesWorship(normalized)) {
            return Optional.of(WORSHIP);
        }
        if (matchesComeHere(normalized)) {
            return Optional.of(COME_HERE);
        }
        if (matchesGoAway(normalized)) {
            return Optional.of(GO_AWAY);
        }
        if (matchesFollow(normalized)) {
            return Optional.of(FOLLOW);
        }
        if (matchesGuard(normalized)) {
            return Optional.of(GUARD);
        }
        if (matchesStop(normalized)) {
            return Optional.of(STOP);
        }
        if (matchesStay(normalized)) {
            return Optional.of(STAY);
        }
        return Optional.empty();
    }

    public boolean matchesCandidate(String message) {
        String normalized = normalize(message);
        return switch (this) {
            case MINE -> matchesMine(normalized);
            case WORSHIP -> matchesWorship(normalized);
            case STAY -> matchesStay(normalized);
            case STOP -> matchesStop(normalized);
            case COME_HERE -> matchesComeHere(normalized);
            case GO_AWAY -> matchesGoAway(normalized);
            case FOLLOW -> matchesFollow(normalized);
            case GUARD -> matchesGuard(normalized);
        };
    }

    public boolean isPlainDirect(String message) {
        String normalized = normalize(message);
        if (isQuestionLike(normalized)) {
            return false;
        }
        return switch (this) {
            case MINE -> normalized.matches("^(please\\s+)?(go\\s+)?(mine|dig|work)\\b.*")
                    || normalized.contains("start mining")
                    || normalized.contains("start digging")
                    || normalized.contains("mine for me")
                    || normalized.contains("dig for me")
                    || normalized.contains("go find koboldite")
                    || normalized.contains("find koboldite for me")
                    || normalized.contains("get koboldite for me");
            case WORSHIP -> normalized.matches("^(please\\s+)?(go\\s+)?(worship|pray)\\b.*")
                    || normalized.contains("go worship")
                    || normalized.contains("go pray")
                    || normalized.contains("pray at")
                    || normalized.contains("worship statue")
                    || normalized.contains("worship the statue")
                    || normalized.contains("worship patron");
            case STAY -> normalized.equals("stay")
                    || normalized.equals("wait")
                    || normalized.contains("stay here")
                    || normalized.contains("stay still")
                    || normalized.contains("stand still")
                    || normalized.contains("hold still")
                    || normalized.contains("wait here")
                    || normalized.contains("dont run")
                    || normalized.contains("do not run")
                    || normalized.contains("dont wander")
                    || normalized.contains("do not wander");
            case STOP -> normalized.equals("stop")
                    || normalized.equals("stop that")
                    || normalized.contains("stop running")
                    || normalized.contains("stop moving")
                    || normalized.contains("stop wandering")
                    || normalized.contains("stop working");
            case COME_HERE -> normalized.equals("come")
                    || normalized.equals("come here")
                    || normalized.equals("come back")
                    || normalized.contains("come to me")
                    || normalized.contains("come over here")
                    || normalized.contains("get over here")
                    || normalized.contains("return to me");
            case GO_AWAY -> normalized.equals("go away")
                    || normalized.equals("leave")
                    || normalized.equals("back off")
                    || normalized.contains("leave me alone")
                    || normalized.contains("get away")
                    || normalized.contains("move away")
                    || normalized.contains("back away");
            case FOLLOW -> normalized.equals("follow")
                    || normalized.equals("follow me")
                    || normalized.contains("walk with me")
                    || normalized.contains("come with me");
            case GUARD -> normalized.equals("guard")
                    || normalized.equals("guard me")
                    || normalized.contains("protect me")
                    || normalized.contains("defend me")
                    || normalized.contains("stand guard")
                    || normalized.contains("keep watch");
        };
    }

    public static boolean isClearlyQuestion(String message) {
        String normalized = normalize(message);
        return normalized.matches("^(what|where|when|why|who|which|how)\\b.*")
                || normalized.matches("^(do|does|did)\\s+\\w+\\s+.*")
                || normalized.matches("^(tell me|explain|teach me|show me)\\b.*");
    }

    public static String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replace("\u2019", "")
                .replaceAll("[^a-z0-9_ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isQuestionLike(String normalized) {
        return isClearlyQuestion(normalized)
                || normalized.matches("^(can|could|would|will|should)\\s+\\w+\\s+.*");
    }

    private static boolean matchesMine(String normalized) {
        return normalized.matches("^(mine|dig|work)\\b.*")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+(mine|dig|work)\\b.*")
                || normalized.contains("go mine")
                || normalized.contains("start mining")
                || normalized.contains("start digging")
                || normalized.contains("mine for me")
                || normalized.contains("dig for me")
                || normalized.contains("find koboldite")
                || normalized.contains("get koboldite");
    }

    private static boolean matchesWorship(String normalized) {
        return normalized.matches("^(worship|pray)\\b.*")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+(worship|pray)\\b.*")
                || normalized.contains("go worship")
                || normalized.contains("go pray")
                || normalized.contains("pray at")
                || normalized.contains("worship statue")
                || normalized.contains("worship the statue")
                || normalized.contains("worship patron")
                || normalized.contains("goblin altar")
                || normalized.contains("patron altar")
                || normalized.contains("pray to mog")
                || normalized.contains("pray to gulg");
    }

    private static boolean matchesStay(String normalized) {
        return normalized.equals("stay")
                || normalized.equals("wait")
                || normalized.contains("dont run")
                || normalized.contains("do not run")
                || normalized.contains("dont leave")
                || normalized.contains("do not leave")
                || normalized.contains("dont wander")
                || normalized.contains("do not wander")
                || normalized.contains("stay here")
                || normalized.contains("stay still")
                || normalized.contains("stand still")
                || normalized.contains("hold still")
                || normalized.contains("wait here");
    }

    private static boolean matchesStop(String normalized) {
        return normalized.equals("stop")
                || normalized.equals("stop that")
                || normalized.contains("stop running")
                || normalized.contains("stop moving")
                || normalized.contains("stop wandering")
                || normalized.contains("stop working");
    }

    private static boolean matchesComeHere(String normalized) {
        return normalized.equals("come")
                || normalized.equals("come here")
                || normalized.equals("come back")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+(come|return)\\b.*")
                || normalized.contains("come to me")
                || normalized.contains("come over here")
                || normalized.contains("get over here")
                || normalized.contains("return to me");
    }

    private static boolean matchesGoAway(String normalized) {
        return normalized.equals("go away")
                || normalized.equals("leave")
                || normalized.equals("back off")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+(leave|go away|back off)\\b.*")
                || normalized.contains("leave me alone")
                || normalized.contains("get away")
                || normalized.contains("move away")
                || normalized.contains("back away");
    }

    private static boolean matchesFollow(String normalized) {
        return normalized.equals("follow")
                || normalized.equals("follow me")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+follow\\b.*")
                || normalized.contains("walk with me")
                || normalized.contains("come with me");
    }

    private static boolean matchesGuard(String normalized) {
        return normalized.equals("guard")
                || normalized.equals("guard me")
                || normalized.matches(".*\\b(can|could|would|will)\\s+you\\s+(guard|protect|defend)\\b.*")
                || normalized.contains("protect me")
                || normalized.contains("defend me")
                || normalized.contains("stand guard")
                || normalized.contains("keep watch");
    }
}
