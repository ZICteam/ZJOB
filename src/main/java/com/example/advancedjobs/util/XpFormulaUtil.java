package com.example.advancedjobs.util;

public final class XpFormulaUtil {
    private XpFormulaUtil() {
    }

    public static double requiredXpForLevel(int level, double baseXp, double growthFactor) {
        return baseXp * Math.pow(Math.max(1, level), growthFactor);
    }
}
