package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record LeaveJobPacket(boolean secondary) {
    public static void encode(LeaveJobPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.secondary);
    }

    public static LeaveJobPacket decode(FriendlyByteBuf buf) {
        return new LeaveJobPacket(buf.readBoolean());
    }

    public static void handle(LeaveJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().leaveJob(context.getSender(), packet.secondary);
                AdvancedJobsMod.get().jobManager().syncToPlayer(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
}
