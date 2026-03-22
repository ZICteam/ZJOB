package com.example.advancedjobs.integration.journeymap;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.entity.ContractsBoardEntity;
import com.example.advancedjobs.entity.DailyBoardEntity;
import com.example.advancedjobs.entity.HelpBoardEntity;
import com.example.advancedjobs.entity.JobsMasterEntity;
import com.example.advancedjobs.entity.LeaderboardBoardEntity;
import com.example.advancedjobs.entity.SalaryBoardEntity;
import com.example.advancedjobs.entity.SkillsBoardEntity;
import com.example.advancedjobs.entity.StatusBoardEntity;
import com.example.advancedjobs.event.JobEventHandler;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.event.ClientEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

@ClientPlugin
public final class AdvancedJobsJourneyMapPlugin implements IClientPlugin {
    private static final String PREFIX = "advancedjobs_npc_";
    private IClientAPI api;

    @Override
    public void initialize(IClientAPI api) {
        this.api = api;
        this.api.subscribe(AdvancedJobsMod.MOD_ID, EnumSet.of(
            ClientEvent.Type.MAPPING_STARTED,
            ClientEvent.Type.MAPPING_STOPPED,
            ClientEvent.Type.DISPLAY_UPDATE
        ));
        refreshWaypoints();
    }

    @Override
    public String getModId() {
        return AdvancedJobsMod.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (event.type == ClientEvent.Type.MAPPING_STOPPED) {
            api.removeAll(AdvancedJobsMod.MOD_ID, DisplayType.Waypoint);
            return;
        }
        refreshWaypoints();
    }

    private void refreshWaypoints() {
        if (api == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            api.removeAll(AdvancedJobsMod.MOD_ID, DisplayType.Waypoint);
            return;
        }
        api.removeAll(AdvancedJobsMod.MOD_ID, DisplayType.Waypoint);
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
                continue;
            }
            NpcMarker marker = markerFor(living);
            if (marker == null) {
                continue;
            }
            try {
                Waypoint waypoint = new Waypoint(PREFIX + entity.getUUID(), marker.nameKey,
                    entity.level().dimension(), entity.blockPosition())
                    .setPersistent(false)
                    .setEditable(false)
                    .setColor(marker.color);
                api.show(waypoint);
            } catch (Exception ignored) {
            }
        }
    }

    private NpcMarker markerFor(LivingEntity living) {
        String visibleName = living.getCustomName() != null ? living.getCustomName().getString() : null;
        if (living instanceof JobsMasterEntity || living.getPersistentData().getBoolean(JobEventHandler.JOB_NPC_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.jobs_master").getString(), 0xC59A63);
        }
        if (living instanceof DailyBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.DAILY_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.daily_board").getString(), 0x7EB26D);
        }
        if (living instanceof StatusBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.STATUS_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.status_board").getString(), 0x8A9BC7);
        }
        if (living instanceof ContractsBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.CONTRACTS_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.contracts_board").getString(), 0x6A8EA0);
        }
        if (living instanceof SalaryBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.SALARY_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.salary_board").getString(), 0xBE9A58);
        }
        if (living instanceof SkillsBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.SKILLS_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.skills_board").getString(), 0x68B67B);
        }
        if (living instanceof LeaderboardBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.LEADERBOARD_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.leaderboard_board").getString(), 0xD3A35F);
        }
        if (living instanceof HelpBoardEntity || living.getPersistentData().getBoolean(JobEventHandler.HELP_BOARD_TAG)) {
            return new NpcMarker(visibleName != null ? visibleName : Component.translatable("entity.advancedjobs.help_board").getString(), 0x7FAFC6);
        }
        return null;
    }

    private record NpcMarker(String nameKey, int color) {
    }
}
