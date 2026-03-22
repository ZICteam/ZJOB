package com.example.advancedjobs.network;

import com.example.advancedjobs.client.ClientJobState;
import com.example.advancedjobs.util.NetworkPayloadUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncPlayerJobsPacket(String payload) {
    public static void encode(SyncPlayerJobsPacket packet, FriendlyByteBuf buf) {
        NetworkPayloadUtil.writeCompressedString(buf, packet.payload);
    }

    public static SyncPlayerJobsPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerJobsPacket(NetworkPayloadUtil.readCompressedString(buf));
    }

    public static void handle(SyncPlayerJobsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientJobState.update(packet.payload));
        context.setPacketHandled(true);
    }
}
