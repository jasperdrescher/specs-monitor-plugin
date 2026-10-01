package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.regex.Pattern;

/** CPU model name and number of logical processors of a node. */
public class CpuInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern NOISE = Pattern.compile(
            "\\((?:R|TM|tm|r)\\)" // (R), (TM)
                    + "|@\\s*[\\d.]+\\s*[GM]Hz" // @ 2.40GHz
                    + "|\\b\\d+(?:st|nd|rd|th)\\s+Gen\\b" // 13th Gen
                    + "|\\b\\d+-Core\\b" // 8-Core
                    + "|\\b(?:Intel|AMD|Core|CPU|Processor)\\b", // vendor/filler words
            Pattern.CASE_INSENSITIVE);

    private final String name;
    private final int threads;

    public CpuInfo(String name, int cores) {
        this.name = name;
        this.threads = cores;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        String s = NOISE.matcher(name == null ? "" : name)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
        return s.isEmpty() ? name : s;
    }

    /** Number of logical processors (threads) available to the JVM on the node. */
    public int getThreads() {
        return threads;
    }

    @Override
    public String toString() {
        return getShortName() + " (" + threads + ")";
    }
}
