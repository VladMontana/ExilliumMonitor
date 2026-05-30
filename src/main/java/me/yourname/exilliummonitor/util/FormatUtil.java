package me.yourname.exilliummonitor.util;

import java.util.Locale;

public final class FormatUtil {
    private static final long GIB = 1024L * 1024L * 1024L;

    private FormatUtil() {
    }

    public static String decimal(double value, int digits) {
        return String.format(Locale.US, "%." + digits + "f", value);
    }

    public static String bytesToGiB(long bytes) {
        return decimal(bytes / (double) GIB, 1) + "GB";
    }

    public static String memory(long usedBytes, long maxBytes) {
        return bytesToGiB(usedBytes) + " / " + bytesToGiB(maxBytes);
    }
}
