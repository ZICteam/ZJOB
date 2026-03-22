package com.example.advancedjobs.gui;

public class HelpScreen extends JobsDetailsScreen {
    public HelpScreen() {
        super(JobsMainScreen.Tab.HELP);
    }

    public HelpScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.HELP, preferredJobId);
    }
}
