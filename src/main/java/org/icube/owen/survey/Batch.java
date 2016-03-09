package org.icube.owen.survey;

import java.util.Date;
import java.util.List;

import org.icube.owen.TheBorg;

public class Batch extends TheBorg {

	private int batchId;
	private Date startDate;
	private Date endDate;
	private List<Question> questionList;
	private Frequency batchFrequency;

	public int getBatchId() {
		return batchId;
	}

	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}

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

	public Frequency getBatchFrequency() {
		return batchFrequency;
	}

	public void setBatchFrequency(Frequency batchFrequency) {
		this.batchFrequency = batchFrequency;
	}

}
