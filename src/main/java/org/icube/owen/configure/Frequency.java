package org.icube.owen.configure;

public enum Frequency {
	WEEKLY(1), BIWEEKLY(2), MONTHLY(3), QUARTERLY(4);

	private int value;

	private Frequency(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
