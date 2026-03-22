package com.example.advancedjobs.client;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdvancedJobsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JOBS_MASTER.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CONTRACTS_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.DAILY_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.STATUS_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SALARY_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SKILLS_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.LEADERBOARD_BOARD.get(), PlayerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.HELP_BOARD.get(), PlayerNpcRenderer::new);
    }
}
