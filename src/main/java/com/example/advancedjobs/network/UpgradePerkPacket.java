package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record UpgradePerkPacket(String jobId, String nodeId) {
    public static void encode(UpgradePerkPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.jobId);
        buf.writeUtf(packet.nodeId);
    }

    public static UpgradePerkPacket decode(FriendlyByteBuf buf) {
        return new UpgradePerkPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(UpgradePerkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().unlockSkill(context.getSender(), packet.jobId, packet.nodeId);
                AdvancedJobsMod.get().jobManager().syncToPlayer(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
}
