package com.example.advancedjobs.gui;

public class ContractsScreen extends JobsDetailsScreen {
    public ContractsScreen() {
        super(JobsMainScreen.Tab.CONTRACTS);
    }

    public ContractsScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.CONTRACTS, preferredJobId);
    }
}
