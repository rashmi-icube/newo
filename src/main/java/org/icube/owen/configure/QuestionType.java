package org.icube.owen.configure;

public enum QuestionType {
	ME(0), WE(1);

	private int value;

	private QuestionType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
