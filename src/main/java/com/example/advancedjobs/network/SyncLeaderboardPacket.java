package com.example.advancedjobs.network;

import com.example.advancedjobs.client.ClientJobState;
import com.example.advancedjobs.util.NetworkPayloadUtil;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncLeaderboardPacket(String jobId, String payload) {
    public static void encode(SyncLeaderboardPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.jobId);
        NetworkPayloadUtil.writeCompressedString(buf, packet.payload);
    }

    public static SyncLeaderboardPacket decode(FriendlyByteBuf buf) {
        return new SyncLeaderboardPacket(buf.readUtf(), NetworkPayloadUtil.readCompressedString(buf));
    }

    public static void handle(SyncLeaderboardPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientJobState.updateLeaderboard(packet.jobId, packet.payload));
        context.setPacketHandled(true);
    }
}
