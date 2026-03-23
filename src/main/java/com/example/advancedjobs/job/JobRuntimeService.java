package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

class JobRuntimeService {
    private final JobClientSyncService clientSyncService;

    JobRuntimeService(JobClientSyncService clientSyncService) {
        this.clientSyncService = clientSyncService;
    }

    void syncToPlayer(ServerPlayer player, String playerPayload) {
        clientSyncService.syncToPlayer(player, playerPayload);
    }

    void syncCatalogToPlayer(ServerPlayer player, String catalogPayload) {
        clientSyncService.syncCatalogToPlayer(player, catalogPayload);
    }

    void syncLeaderboardToPlayer(ServerPlayer player, String jobId, String leaderboardPayload) {
        clientSyncService.syncLeaderboardToPlayer(player, jobId, leaderboardPayload);
    }

    void syncFullState(ServerPlayer player,
                       String catalogPayload,
                       String playerPayload,
                       String leaderboardJobId,
                       String leaderboardPayload) {
        clientSyncService.syncCatalogToPlayer(player, catalogPayload);
        clientSyncService.syncToPlayer(player, playerPayload);
        clientSyncService.syncLeaderboardToPlayer(player, leaderboardJobId, leaderboardPayload);
    }

    void syncAllOnlinePlayers(MinecraftServer server,
                              Consumer<ServerPlayer> syncFullState) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncFullState.accept(player);
        }
    }

    void syncCatalogToAllPlayers(MinecraftServer server, Consumer<ServerPlayer> syncCatalog) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncCatalog.accept(player);
        }
    }

    void openScreen(ServerPlayer player,
                    String tab,
                    String preferredJobId,
                    Consumer<ServerPlayer> syncFullState) {
        syncFullState.accept(player);
        clientSyncService.openScreen(player, tab, preferredJobId);
    }

    void expireRewardEventIfNeeded(MinecraftServer server, Consumer<ServerPlayer> syncToPlayer) {
        long endsAt = ConfigManager.economy().eventEndsAtEpochSecond();
        if (endsAt <= 0L || endsAt > TimeUtil.now()) {
            return;
        }
        if (Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) == 0) {
            ConfigManager.economy().setEventEndsAtEpochSecond(0L);
            ConfigManager.saveEconomyConfig();
            return;
        }
        ConfigManager.economy().setEventMultiplier(1.0D);
        ConfigManager.economy().setEventEndsAtEpochSecond(0L);
        ConfigManager.saveEconomyConfig();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(TextUtil.tr("message.advancedjobs.event_stopped"), false);
            server.getPlayerList().getPlayers().forEach(syncToPlayer);
        }
    }
}
