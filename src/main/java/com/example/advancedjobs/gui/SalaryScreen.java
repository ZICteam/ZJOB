package com.example.advancedjobs.gui;

public class SalaryScreen extends JobsDetailsScreen {
    public SalaryScreen() {
        super(JobsMainScreen.Tab.SALARY);
    }

    public SalaryScreen(String preferredJobId) {
        super(JobsMainScreen.Tab.SALARY, preferredJobId);
    }
}
