package com.example.advancedjobs.gui;

public class DailyTasksScreen extends JobsDetailsScreen {
    public DailyTasksScreen() {
        super(JobsMainScreen.Tab.DAILY);
    }

    public DailyTasksScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.DAILY, preferredJobId);
    }
}
