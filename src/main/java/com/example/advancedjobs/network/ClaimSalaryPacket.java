package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ClaimSalaryPacket() {
    public static void encode(ClaimSalaryPacket packet, FriendlyByteBuf buf) {
    }

    public static ClaimSalaryPacket decode(FriendlyByteBuf buf) {
        return new ClaimSalaryPacket();
    }

    public static void handle(ClaimSalaryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AdvancedJobsMod.get().jobManager().claimSalary(context.getSender());
                AdvancedJobsMod.get().jobManager().syncToPlayer(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
}
