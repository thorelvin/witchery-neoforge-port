package com.emoniph.witchery.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Persists phase-two releases until every online or reconnecting participant has returned. */
public final class TormentReleaseData extends SavedData {
    private final List<ReleasedSession> sessions = new ArrayList<>();

    public static final Codec<TormentReleaseData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ReleasedSession.CODEC.listOf().optionalFieldOf("sessions", List.of())
                            .forGetter(data -> data.sessions)
            ).apply(instance, TormentReleaseData::fromCodec));

    public static final SavedDataType<TormentReleaseData> TYPE = new SavedDataType<>(
            "witchery_torment_releases",
            TormentReleaseData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public TormentReleaseData() {}

    private static TormentReleaseData fromCodec(List<ReleasedSession> sessions) {
        TormentReleaseData data = new TormentReleaseData();
        data.sessions.addAll(sessions);
        return data;
    }

    public static TormentReleaseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void release(UUID sessionId, Collection<UUID> participants) {
        if (sessionId == null || participants.isEmpty()) return;
        List<UUID> merged = new ArrayList<>(participants);
        for (ReleasedSession session : sessions) {
            if (session.sessionId().equals(sessionId)) {
                for (UUID participant : session.pendingPlayers()) {
                    if (!merged.contains(participant)) merged.add(participant);
                }
            }
        }
        sessions.removeIf(session -> session.sessionId().equals(sessionId));
        sessions.add(new ReleasedSession(sessionId, merged));
        setDirty();
    }

    /** Removes all pending entries for a player and reports whether their active session was released. */
    public boolean consume(UUID playerId, UUID activeSession) {
        boolean released = false;
        boolean changed = false;
        for (int index = sessions.size() - 1; index >= 0; index--) {
            ReleasedSession session = sessions.get(index);
            if (!session.pendingPlayers().contains(playerId)) continue;
            released |= activeSession != null && session.sessionId().equals(activeSession);
            List<UUID> remaining = new ArrayList<>(session.pendingPlayers());
            remaining.removeIf(playerId::equals);
            if (remaining.isEmpty()) {
                sessions.remove(index);
            } else {
                sessions.set(index, new ReleasedSession(session.sessionId(), remaining));
            }
            changed = true;
        }
        if (changed) setDirty();
        return released;
    }

    public boolean isReleased(UUID sessionId, UUID playerId) {
        return sessionId != null && sessions.stream().anyMatch(session ->
                session.sessionId().equals(sessionId) && session.pendingPlayers().contains(playerId));
    }

    public int pendingPlayerCount() {
        return sessions.stream().mapToInt(session -> session.pendingPlayers().size()).sum();
    }

    record ReleasedSession(UUID sessionId, List<UUID> pendingPlayers) {
        static final Codec<ReleasedSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("session_id").forGetter(ReleasedSession::sessionId),
                UUIDUtil.CODEC.listOf().fieldOf("pending_players").forGetter(ReleasedSession::pendingPlayers)
        ).apply(instance, ReleasedSession::new));

        ReleasedSession {
            pendingPlayers = List.copyOf(pendingPlayers);
        }
    }
}
