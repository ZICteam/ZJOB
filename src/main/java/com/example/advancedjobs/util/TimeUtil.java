package com.example.advancedjobs.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static long now() {
        return Instant.now().getEpochSecond();
    }

    public static long nextResetEpochSecond(ZoneId zoneId, LocalTime resetTime) {
        ZonedDateTime reset = LocalDate.now(zoneId).atTime(resetTime).atZone(zoneId);
        if (!reset.isAfter(ZonedDateTime.now(zoneId))) {
            reset = reset.plusDays(1);
        }
        return reset.toEpochSecond();
    }

    public static String formatRemainingSeconds(long seconds) {
        long safe = Math.max(0L, seconds);
        long hours = safe / 3600L;
        long minutes = (safe % 3600L) / 60L;
        long secs = safe % 60L;
        if (hours > 0L) {
            return String.format("%02dh %02dm", hours, minutes);
        }
        return String.format("%02dm %02ds", minutes, secs);
    }
}
