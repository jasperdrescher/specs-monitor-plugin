package io.jenkins.plugins.specsmonitor;

import java.util.List;
import java.util.Locale;

/** Extracts a human-readable CPU model name from Linux tool output. */
final class CpuNameParser {

    /** Keys in /proc/cpuinfo that hold a readable name, in order of preference. */
    private static final List<String> PROC_CPUINFO_KEYS = List.of("model name", "cpu", "hardware");

    private CpuNameParser() {}

    /**
     * Parses the output of {@code lscpu} (run with {@code LC_ALL=C}) and returns
     * the first
     * {@code Model name:} value, or an empty string if there is none.
     */
    static String fromLscpu(String output) {
        if (output == null) {
            return "";
        }
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Model name:")) {
                String value = trimmed.substring("Model name:".length()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * Parses the lines of {@code /proc/cpuinfo}. Works for x86 ({@code model name})
     * and
     * POWER ({@code cpu}). Architectures that only print numeric IDs, such as
     * aarch64, yield
     * an empty string.
     */
    static String fromProcCpuinfo(List<String> lines) {
        for (String key : PROC_CPUINFO_KEYS) {
            for (String line : lines) {
                int colon = line.indexOf(':');
                if (colon > 0
                        && line.substring(0, colon)
                                .trim()
                                .toLowerCase(Locale.ROOT)
                                .equals(key)) {
                    String value = line.substring(colon + 1).trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "";
    }
}
