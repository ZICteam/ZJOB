package com.example.advancedjobs.network;

import com.example.advancedjobs.client.ClientJobState;
import com.example.advancedjobs.util.NetworkPayloadUtil;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncJobCatalogPacket(String payload) {
    public static void encode(SyncJobCatalogPacket packet, FriendlyByteBuf buf) {
        NetworkPayloadUtil.writeCompressedString(buf, packet.payload);
    }

    public static SyncJobCatalogPacket decode(FriendlyByteBuf buf) {
        return new SyncJobCatalogPacket(NetworkPayloadUtil.readCompressedString(buf));
    }

    public static void handle(SyncJobCatalogPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientJobState.updateCatalog(packet.payload));
        context.setPacketHandled(true);
    }
}
