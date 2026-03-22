package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record RerollContractsPacket(String jobId) {
    public static void encode(RerollContractsPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.jobId);
    }

    public static RerollContractsPacket decode(FriendlyByteBuf buf) {
        return new RerollContractsPacket(buf.readUtf());
    }

    public static void handle(RerollContractsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().rerollContracts(context.getSender(), packet.jobId());
            }
        });
        context.setPacketHandled(true);
    }
}
