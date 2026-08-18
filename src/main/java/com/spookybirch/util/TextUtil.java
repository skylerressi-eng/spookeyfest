package com.spookybirch.util;

/** Small string helpers for parsing Hypixel's chat/scoreboard text. */
public final class TextUtil {

    /** Strip Minecraft color/format codes (the section-sign kind). */
    public static String stripColor(String in) {
        if (in == null) return "";
        return in.replaceAll("(?i)\\u00A7[0-9A-FK-OR]", "");
    }

    /** Remove commas and spaces from a number-ish string: "1,234" -> "1234". */
    public static String cleanNumber(String in) {
        if (in == null) return "";
        return in.replace(",", "").replace(" ", "").trim();
    }

    public static int parseIntSafe(String in, int fallback) {
        try {
            return Integer.parseInt(cleanNumber(in));
        } catch (Exception e) {
            return fallback;
        }
    }

    private TextUtil() {}
}
