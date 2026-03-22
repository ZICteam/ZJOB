package com.example.advancedjobs.util;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TextUtil {
    private TextUtil() {
    }

    public static MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String fmt2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
