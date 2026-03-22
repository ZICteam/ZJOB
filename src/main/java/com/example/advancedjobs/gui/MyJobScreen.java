package com.example.advancedjobs.gui;

public class MyJobScreen extends JobsDetailsScreen {
    public MyJobScreen() {
        super(JobsMainScreen.Tab.MY_JOB);
    }

    public MyJobScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.MY_JOB, preferredJobId);
    }
}
