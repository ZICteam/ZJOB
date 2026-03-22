package com.example.advancedjobs.network;

import java.util.function.Supplier;
import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ChooseJobPacket(String jobId, boolean secondary) {
    public static void encode(ChooseJobPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.jobId);
        buf.writeBoolean(packet.secondary);
    }

    public static ChooseJobPacket decode(FriendlyByteBuf buf) {
        return new ChooseJobPacket(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(ChooseJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().chooseJob(context.getSender(), packet.jobId, packet.secondary);
                AdvancedJobsMod.get().jobManager().syncToPlayer(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
}
