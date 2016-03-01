package org.icube.owen.survey;

import java.util.HashMap;
import java.util.Map;

public enum Frequency {
	WEEKLY(1), BIWEEKLY(2), MONTHLY(3), QUARTERLY(4);

	// Reverse-lookup map for getting a frequency from frequency ID
	private static final Map<Integer, Frequency> lookup = new HashMap<Integer, Frequency>();

	static {
		for (Frequency d : Frequency.values()) {
			lookup.put(d.getValue(), d);
		}
	}

	private int value;

	private Frequency(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	public static Frequency get(int value) {
		return lookup.get(value);
	}
}
