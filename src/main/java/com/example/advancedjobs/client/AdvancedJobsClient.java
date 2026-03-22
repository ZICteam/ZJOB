package com.example.advancedjobs.client;

import com.example.advancedjobs.gui.ContractsScreen;
import com.example.advancedjobs.gui.DailyTasksScreen;
import com.example.advancedjobs.gui.HelpScreen;
import com.example.advancedjobs.gui.JobsMainScreen;
import com.example.advancedjobs.gui.LeaderboardScreen;
import com.example.advancedjobs.gui.MyJobScreen;
import com.example.advancedjobs.gui.SalaryScreen;
import com.example.advancedjobs.gui.SkillTreeScreen;
import net.minecraft.client.Minecraft;

public final class AdvancedJobsClient {
    private AdvancedJobsClient() {
    }

    public static void openMainScreen() {
        Minecraft.getInstance().setScreen(new JobsMainScreen());
    }

    public static void openScreen(String tab) {
        openScreen(tab, null);
    }

    public static void openScreen(String tab, String preferredJobId) {
        Minecraft minecraft = Minecraft.getInstance();
        String normalized = tab == null ? "" : tab.trim().toLowerCase(java.util.Locale.ROOT);
        minecraft.setScreen(switch (normalized) {
            case "my_job" -> new MyJobScreen(preferredJobId);
            case "skills" -> new SkillTreeScreen(preferredJobId);
            case "daily" -> new DailyTasksScreen(preferredJobId);
            case "contracts" -> new ContractsScreen(preferredJobId);
            case "salary" -> new SalaryScreen(preferredJobId);
            case "top", "leaderboard" -> new LeaderboardScreen(preferredJobId);
            case "help" -> new HelpScreen(preferredJobId);
            default -> new JobsMainScreen(preferredJobId);
        });
    }
}
