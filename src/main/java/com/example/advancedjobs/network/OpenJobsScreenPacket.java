package com.example.advancedjobs.network;

import com.example.advancedjobs.client.AdvancedJobsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record OpenJobsScreenPacket(String tab, String preferredJobId) {
    public static void encode(OpenJobsScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.tab());
        boolean hasPreferred = packet.preferredJobId() != null && !packet.preferredJobId().isBlank();
        buf.writeBoolean(hasPreferred);
        if (hasPreferred) {
            buf.writeUtf(packet.preferredJobId());
        }
    }

    public static OpenJobsScreenPacket decode(FriendlyByteBuf buf) {
        String tab = buf.readUtf();
        return new OpenJobsScreenPacket(tab, buf.readBoolean() ? buf.readUtf() : null);
    }

    public static void handle(OpenJobsScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> AdvancedJobsClient.openScreen(packet.tab(), packet.preferredJobId()));
        context.setPacketHandled(true);
    }
}
