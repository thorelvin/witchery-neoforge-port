package com.emoniph.witchery.command;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HeadlessGameTestPlayer {
    private static final Field PLAYERS = field("players");
    private static final Field PLAYERS_BY_UUID = field("playersByUUID");

    private HeadlessGameTestPlayer() {
    }

    public static ServerPlayer create(ServerLevel level, UUID id, String name) {
        GameProfile profile = new GameProfile(id, name);
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, profile, ClientInformation.createDefault());
        player.connection = new HeadlessPacketListener(player, profile);

        PlayerList playerList = level.getServer().getPlayerList();
        if (playerList.getPlayer(id) != null) {
            throw new IllegalStateException("Duplicate headless GameTest player " + id);
        }

        players(playerList).add(player);
        playersByUuid(playerList).put(id, player);
        level.addNewPlayer(player);
        return player;
    }

    public static void remove(ServerPlayer player) {
        if (player == null) {
            return;
        }

        ServerLevel level = player.level();
        PlayerList playerList = level.getServer().getPlayerList();
        if (!player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        players(playerList).remove(player);
        playersByUuid(playerList).remove(player.getUUID(), player);
    }

    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> players(PlayerList playerList) {
        try {
            return (List<ServerPlayer>) PLAYERS.get(playerList);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access the GameTest player list", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> playersByUuid(PlayerList playerList) {
        try {
            return (Map<UUID, ServerPlayer>) PLAYERS_BY_UUID.get(playerList);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access the GameTest player index", exception);
        }
    }

    private static Field field(String name) {
        try {
            Field field = PlayerList.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static final class HeadlessPacketListener extends ServerGamePacketListenerImpl {
        private HeadlessPacketListener(ServerPlayer player, GameProfile profile) {
            super(player.level().getServer(), connection(), player,
                    CommonListenerCookie.createInitial(profile, false));
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, ChannelFutureListener listener) {
        }

        private static Connection connection() {
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            return connection;
        }
    }
}
