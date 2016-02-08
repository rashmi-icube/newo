package org.icube.owen.configure;

import java.util.Date;
import java.util.List;

import org.icube.owen.TheBorg;

public class Batch extends TheBorg {

	private Date startDate;
	private Date endDate;
	private List<Question> questionList;
	private Frequency batchFrequency;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public List<Question> getQuestionList() {
		return questionList;
	}

	public void setQuestionList(List<Question> questionList) {
		this.questionList = questionList;
	}

	public Frequency getFrequency() {
		return batchFrequency;
	}

	public void setFrequency(Frequency input) {
		batchFrequency = input;
	}

}
