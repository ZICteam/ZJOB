package com.example.advancedjobs.job;

import com.example.advancedjobs.network.OpenJobsScreenPacket;
import com.example.advancedjobs.network.PacketHandler;
import com.example.advancedjobs.network.SyncJobCatalogPacket;
import com.example.advancedjobs.network.SyncLeaderboardPacket;
import com.example.advancedjobs.network.SyncPlayerJobsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

class JobClientSyncService {
    void syncToPlayer(ServerPlayer player, String playerPayload) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerJobsPacket(playerPayload));
    }

    void syncCatalogToPlayer(ServerPlayer player, String catalogPayload) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncJobCatalogPacket(catalogPayload));
    }

    void syncLeaderboardToPlayer(ServerPlayer player, String jobId, String leaderboardPayload) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLeaderboardPacket(jobId, leaderboardPayload));
    }

    void openScreen(ServerPlayer player, String tab, String preferredJobId) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenJobsScreenPacket(tab, preferredJobId));
    }
}
