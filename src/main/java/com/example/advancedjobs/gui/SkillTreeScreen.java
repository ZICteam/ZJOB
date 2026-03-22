package com.example.advancedjobs.gui;

public class SkillTreeScreen extends JobsDetailsScreen {
    public SkillTreeScreen() {
        super(JobsMainScreen.Tab.SKILLS);
    }

    public SkillTreeScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.SKILLS, preferredJobId);
    }
}
