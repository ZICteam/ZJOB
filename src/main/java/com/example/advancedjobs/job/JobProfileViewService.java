package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TextUtil;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

class JobProfileViewService {
    List<String> assignedJobIds(PlayerJobProfile profile) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (profile.activeJobId() != null) {
            ids.add(profile.activeJobId());
        }
        if (ConfigManager.COMMON.allowSecondaryJob.get() && profile.secondaryJobId() != null) {
            ids.add(profile.secondaryJobId());
        }
        return List.copyOf(ids);
    }

    String activeJobId(PlayerJobProfile profile) {
        return profile.activeJobId();
    }

    boolean hasAssignedJob(PlayerJobProfile profile, String jobId) {
        return jobId != null && (jobId.equals(profile.activeJobId()) || jobId.equals(profile.secondaryJobId()));
    }

    String firstAssignedJob(PlayerJobProfile profile, String... jobIds) {
        for (String assigned : assignedJobIds(profile)) {
            for (String candidate : jobIds) {
                if (assigned.equals(candidate)) {
                    return assigned;
                }
            }
        }
        return null;
    }

    String leaderboardJobId(PlayerJobProfile profile) {
        return profile.activeJobId() != null ? profile.activeJobId() : "all";
    }

    void grantTitle(ServerPlayer player, PlayerJobProfile profile, String titleId, JobProgressionService progressionService) {
        if (profile.unlockTitle(titleId)) {
            player.sendSystemMessage(TextUtil.tr("message.advancedjobs.title_unlocked", progressionService.titleComponent(titleId)));
        }
    }
}
