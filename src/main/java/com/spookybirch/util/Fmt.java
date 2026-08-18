package com.spookybirch.util;

/** Number/time formatting shared by the HUD and the stats command. */
public final class Fmt {

    /** 1234 -> "1,234"; 12345 -> "12.3k"; 1234567 -> "1.23M". */
    public static String num(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) return trim(v / 1_000_000.0) + "M";
        if (a >= 10_000)    return trim(v / 1_000.0) + "k";
        return commas(Math.round(v));
    }

    /** Rate like "1.2k/hr". */
    public static String rate(double perHour) {
        return num(perHour) + "/hr";
    }

    /** Seconds -> "1h 02m", "12m 30s", "45s". Negative -> "--". */
    public static String duration(long seconds) {
        if (seconds < 0) return "--";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + pad2(m) + "m";
        if (m > 0) return m + "m " + pad2(s) + "s";
        return s + "s";
    }

    private static String trim(double v) {
        // one decimal, but drop a trailing ".0"
        String out = String.format("%.1f", v);
        if (out.endsWith(".0")) out = out.substring(0, out.length() - 2);
        return out;
    }

    private static String commas(long v) {
        return String.format("%,d", v);
    }

    private static String pad2(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }

    private Fmt() {}
}
