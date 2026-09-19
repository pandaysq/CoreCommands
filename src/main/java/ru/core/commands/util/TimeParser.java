package ru.core.commands.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {
    private static final Pattern PATTERN = Pattern.compile("(?i)(\\d+)([smhdw])");

    private TimeParser() {
    }

    public static Long parse(String input) {
        if (input == null || input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm")) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid time");
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2).toLowerCase()) {
            case "s" -> Duration.ofSeconds(amount).toMillis();
            case "m" -> Duration.ofMinutes(amount).toMillis();
            case "h" -> Duration.ofHours(amount).toMillis();
            case "d" -> Duration.ofDays(amount).toMillis();
            case "w" -> Duration.ofDays(amount * 7).toMillis();
            default -> throw new IllegalArgumentException("invalid time");
        };
    }

    public static String format(Long duration) {
        if (duration == null) {
            return "навсегда";
        }
        long seconds = Math.max(1, duration / 1000);
        if (seconds % 604800 == 0) return seconds / 604800 + "w";
        if (seconds % 86400 == 0) return seconds / 86400 + "d";
        if (seconds % 3600 == 0) return seconds / 3600 + "h";
        if (seconds % 60 == 0) return seconds / 60 + "m";
        return seconds + "s";
    }
}