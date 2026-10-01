package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.regex.Pattern;

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
	private final int cores;

	public CpuInfo(String name, int cores) {
		this.name = name;
		this.cores = cores;
	}

	public String getName() {
		return name;
	}

	public String getShortName() {
        String s = NOISE.matcher(name == null ? "" : name).replaceAll(" ")
                .replaceAll("\\s+", " ").trim();
        return s.isEmpty() ? name : s;
    }

	public int getCores() {
		return cores;
	}

	@Override
	public String toString() {
		return getShortName() + " (" + cores + ")";
	}
}
