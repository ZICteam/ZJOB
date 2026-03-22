package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record RequestLeaderboardPacket(String jobId) {
    public static void encode(RequestLeaderboardPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.jobId);
    }

    public static RequestLeaderboardPacket decode(FriendlyByteBuf buf) {
        return new RequestLeaderboardPacket(buf.readUtf());
    }

    public static void handle(RequestLeaderboardPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().syncLeaderboardToPlayer(context.getSender(), packet.jobId);
            }
        });
        context.setPacketHandled(true);
    }
}
