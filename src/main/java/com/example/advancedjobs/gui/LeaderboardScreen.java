package com.example.advancedjobs.gui;

public class LeaderboardScreen extends JobsDetailsScreen {
    public LeaderboardScreen() {
        super(JobsMainScreen.Tab.TOP);
    }

    public LeaderboardScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.TOP, preferredJobId);
    }
}
