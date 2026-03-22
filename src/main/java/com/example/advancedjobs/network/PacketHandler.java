package com.example.advancedjobs.network;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.util.ResourceLocationUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocationUtil.mod("main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int id;

    private PacketHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(id++, SyncJobCatalogPacket.class, SyncJobCatalogPacket::encode, SyncJobCatalogPacket::decode, SyncJobCatalogPacket::handle);
        CHANNEL.registerMessage(id++, SyncPlayerJobsPacket.class, SyncPlayerJobsPacket::encode, SyncPlayerJobsPacket::decode, SyncPlayerJobsPacket::handle);
        CHANNEL.registerMessage(id++, SyncLeaderboardPacket.class, SyncLeaderboardPacket::encode, SyncLeaderboardPacket::decode, SyncLeaderboardPacket::handle);
        CHANNEL.registerMessage(id++, OpenJobsScreenPacket.class, OpenJobsScreenPacket::encode, OpenJobsScreenPacket::decode, OpenJobsScreenPacket::handle);
        CHANNEL.registerMessage(id++, ClaimSalaryPacket.class, ClaimSalaryPacket::encode, ClaimSalaryPacket::decode, ClaimSalaryPacket::handle);
        CHANNEL.registerMessage(id++, RequestLeaderboardPacket.class, RequestLeaderboardPacket::encode, RequestLeaderboardPacket::decode, RequestLeaderboardPacket::handle);
        CHANNEL.registerMessage(id++, ChooseJobPacket.class, ChooseJobPacket::encode, ChooseJobPacket::decode, ChooseJobPacket::handle);
        CHANNEL.registerMessage(id++, LeaveJobPacket.class, LeaveJobPacket::encode, LeaveJobPacket::decode, LeaveJobPacket::handle);
        CHANNEL.registerMessage(id++, RerollContractsPacket.class, RerollContractsPacket::encode, RerollContractsPacket::decode, RerollContractsPacket::handle);
        CHANNEL.registerMessage(id++, UpgradePerkPacket.class, UpgradePerkPacket::encode, UpgradePerkPacket::decode, UpgradePerkPacket::handle);
    }
}
