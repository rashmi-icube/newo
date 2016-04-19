package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;

public class BatchList extends TheBorg {

	/**
	 * Retrieves the Frequency labels to populate the Frequency drop down
	 * @return - A frequency label map containing the frequency values
	 */
	public Map<Integer, String> getFrequencyLabelMap() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		Map<Integer, String> getFrequencyLabelMap = new HashMap<>();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getFrequencyList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				getFrequencyLabelMap.put(rs.getInt(1), rs.getString(2));
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while retrieving frequency label map", e);
		}
		return getFrequencyLabelMap;
	}

	/**
	 * Retrieves the list of Batches
	 * @return -  A list  of batches
	 */
	public List<Batch> getBatchList() {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Batch> batchList = new ArrayList<Batch>();

		try {

			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getBatch(?)}");
			cstmt.setInt(1, 1);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Batch b = new Batch();
				b.setBatchFrequency(Frequency.get(rs.getInt("freq_id")));
				b.setStartDate(rs.getDate("start_date"));
				b.setEndDate(rs.getDate("end_date"));
				b.setBatchId(rs.getInt("survey_batch_id"));
				CallableStatement cstmt1 = dch.mysqlCon.prepareCall("{call getBatchQuestionList(?)}");
				cstmt1.setInt(1, rs.getInt("survey_batch_id"));
				ResultSet rs1 = cstmt1.executeQuery();
				List<Question> questionList = new ArrayList<Question>();
				while (rs1.next()) {
					Question q = new Question();
					q.setEndDate(UtilHelper.getEndOfDay(rs1.getDate("end_date")));
					q.setStartDate(UtilHelper.getStartOfDay(rs1.getDate("start_date")));
					q.setQuestionText(rs1.getString("question"));
					q.setQuestionId(rs1.getInt("que_id"));
					q.setResponsePercentage(rs1.getDouble("resp"));
					q.setQuestionType(QuestionType.values()[rs1.getInt("que_type")]);
					q.setSurveyBatchId(rs1.getInt("survey_batch_id"));
					q.setRelationshipTypeId(rs1.getInt("rel_id"));
					questionList.add(q);
				}
				b.setQuestionList(questionList);
				batchList.add(b);
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while retrieving batch list", e);
		}
		return batchList;

	}

	/**
	 * Changes the frequency of a batch
	 * @param batch - The batch object 
	 * @param changedFrequency - The updated frequency value
	 * @return - True/False depending on whether the frequency has been changed or not
	 */
	public boolean changeFrequency(Batch batch, Frequency changedFrequency) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

		boolean isChanged = false;

		// Should trigger the updation of all future dates for the questions part of the batch
		if (batch.getBatchFrequency().equals(changedFrequency)) {
			org.apache.log4j.Logger.getLogger(BatchList.class).debug("Do nothing... Old frequency same as changed frequency : " + changedFrequency);
			return isChanged;
		}
		List<Question> questionList = batch.getQuestionList();
		List<Integer> questionIdList = new ArrayList<>();
		for (Question q : questionList) {
			questionIdList.add(q.getQuestionId());
		}
		Collections.sort(questionIdList);
		Date previousEndDate = null;
		for (int questionId : questionIdList) {
			Question q = new Question();
			q = q.getQuestion(questionId);

			if (q.getQuestionStatus(q.getStartDate(), q.getEndDate()).equalsIgnoreCase("completed")) {
				org.apache.log4j.Logger.getLogger(BatchList.class).debug(
						"Do nothing... Question has already been completed : " + q.getQuestionId() + " : " + q.getQuestionText());
				continue;
			} else {

				boolean isCurrent = q.getQuestionStatus(q.getStartDate(), q.getEndDate()).equalsIgnoreCase("current");
				previousEndDate = updateQuestion(q, changedFrequency, isCurrent, previousEndDate);
				try {
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call updateQuestionDate(?, ?, ?)}");
					cstmt.setInt(1, questionId);
					cstmt.setDate(2, UtilHelper.convertJavaDateToSqlDate(q.getStartDate()));
					cstmt.setDate(3, UtilHelper.convertJavaDateToSqlDate(q.getEndDate()));
					cstmt.executeQuery();
					org.apache.log4j.Logger.getLogger(BatchList.class).debug(
							"Successfully changed frequency for question " + questionId + " in batch " + batch.getBatchId());
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while updating question ID : " + questionId, e);
				}

			}

		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call updateBatch(?, ?, ?, ?)}");
			cstmt.setInt(1, batch.getBatchId());
			cstmt.setInt(2, changedFrequency.getValue());
			cstmt.setDate(3, UtilHelper.convertJavaDateToSqlDate(UtilHelper.getStartOfDay(batch.getStartDate())));
			cstmt.setDate(4, UtilHelper.convertJavaDateToSqlDate(UtilHelper.getEndOfDay(previousEndDate)));
			cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(BatchList.class).debug("Successfully changed frequency for batch " + batch.getBatchId());
			isChanged = true;
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while updating batch ID : " + batch.getBatchId(), e);
		}

		return isChanged;

	}

	private static Date updateQuestion(Question q, Frequency frequency, boolean isCurrent, Date previousEndDate) {

		switch (frequency) {

		case WEEKLY: // 7
			if (isCurrent) {
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(q.getStartDate(), 6));
				Date actualEndDate = UtilHelper.getEndOfDay(endDate.before(Date.from(Instant.now())) ? (Date.from(Instant.now())) : endDate);
				q.setEndDate(actualEndDate);
				previousEndDate = actualEndDate;
			} else {
				Date startDate = UtilHelper.getStartOfDay(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(startDate, 6));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}

			break;
		case BIWEEKLY: // 14
			if (isCurrent) {
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(q.getStartDate(), 13));
				Date actualEndDate = UtilHelper.getEndOfDay(endDate.before(Date.from(Instant.now())) ? (Date.from(Instant.now())) : endDate);
				q.setEndDate(actualEndDate);
				previousEndDate = actualEndDate;

			} else {
				Date startDate = UtilHelper.getStartOfDay(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(startDate, 13));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;
		case MONTHLY: // 1 month
			if (isCurrent) {
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 1), -1));
				Date actualEndDate = UtilHelper.getEndOfDay(endDate.before(Date.from(Instant.now())) ? (Date.from(Instant.now())) : endDate);
				q.setEndDate(actualEndDate);
				previousEndDate = actualEndDate;
			} else {
				Date startDate = UtilHelper.getStartOfDay(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 1), -1));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;
		case QUARTERLY: // 3 months
			if (isCurrent) {
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 3), -1));
				Date actualEndDate = UtilHelper.getEndOfDay(endDate.before(Date.from(Instant.now())) ? (Date.from(Instant.now())) : endDate);
				q.setEndDate(actualEndDate);
				previousEndDate = actualEndDate;
			} else {
				Date startDate = UtilHelper.getStartOfDay(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = UtilHelper.getEndOfDay(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 3), -1));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;

		}

		return previousEndDate;
	}

	/**
	 * Gets the current batch based on the comparison of the dates
	 * @return current batch
	 */
	public Batch getCurrentBatch() {
		// TODO This is temp... Until we use only 1 batch through out the application
		List<Batch> batchList = getBatchList();
		Batch currentBatch = batchList.get(0);

		/*for(Batch b : batchList){
			if((b.getStartDate().compareTo(Date.from(Instant.now())) <= 0) && (b.getEndDate().after(Date.from(Instant.now())))){
				currentBatch = b;
			}
		}*/

		return currentBatch;

	}

}