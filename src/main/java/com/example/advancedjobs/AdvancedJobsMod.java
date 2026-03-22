package com.example.advancedjobs;

import com.example.advancedjobs.command.JobsAdminCommand;
import com.example.advancedjobs.command.JobsCommand;
import com.example.advancedjobs.command.MoneyCommand;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.content.ModCreativeTabs;
import com.example.advancedjobs.content.ModItems;
import com.example.advancedjobs.event.JobEventHandler;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.network.PacketHandler;
import com.example.advancedjobs.util.DebugLog;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(AdvancedJobsMod.MOD_ID)
public final class AdvancedJobsMod {
    public static final String MOD_ID = "advancedjobs";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static AdvancedJobsMod instance;

    private final JobManager jobManager = new JobManager();
    private final JobEventHandler jobEventHandler = new JobEventHandler(jobManager);

    @SuppressWarnings("removal")
    public AdvancedJobsMod() {
        instance = this;
        var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigManager.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigManager.CLIENT_SPEC);
        ModCreativeTabs.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ConfigManager.init();
        DebugLog.initFromConfig();
        PacketHandler.init();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(jobEventHandler);
    }

    public static AdvancedJobsMod get() {
        return instance;
    }

    public JobManager jobManager() {
        return jobManager;
    }

    public JobEventHandler jobEventHandler() {
        return jobEventHandler;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        jobManager.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        jobManager.flush();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        JobsCommand.register(event.getDispatcher());
        JobsAdminCommand.register(event.getDispatcher());
        MoneyCommand.register(event.getDispatcher());
    }
}
