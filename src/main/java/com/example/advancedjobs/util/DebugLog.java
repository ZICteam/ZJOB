package com.example.advancedjobs.util;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.config.ConfigManager;

public final class DebugLog {
    private static volatile boolean runtimeEnabled;

    private DebugLog() {
    }

    public static void initFromConfig() {
        try {
            runtimeEnabled = ConfigManager.COMMON.debugLogging.get();
        } catch (IllegalStateException ignored) {
            runtimeEnabled = false;
        }
    }

    public static boolean enabled() {
        return runtimeEnabled;
    }

    public static void setEnabled(boolean enabled) {
        runtimeEnabled = enabled;
    }

    public static void log(String message) {
        if (runtimeEnabled) {
            AdvancedJobsMod.LOGGER.info("[AdvancedJobs Debug] {}", message);
        }
    }
}
