package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class BatchList extends TheBorg {

	public static void main(String arg[]) {
		// getBatchList();

		changeFrequency(getBatchList().get(0), Frequency.WEEKLY);
	}

	/**
	 * Retrieves the list of Batches
	 * @return -  A list  of batches
	 */
	public static List<Batch> getBatchList() {

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Batch> batchList = new ArrayList<Batch>();
		List<Question> questionList = new ArrayList<Question>();

		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getBatchList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Batch b = new Batch();
				b.setBatchFrequency(Frequency.values()[rs.getInt("freq_id")]);
				b.setStartDate(rs.getDate("start_date"));
				b.setEndDate(rs.getDate("end_date"));
				b.setBatchId(rs.getInt("survey_batch_id"));
				CallableStatement cstmt1 = dch.mysqlCon.prepareCall("{call getQuestionListForBatch(?)}");
				cstmt1.setInt(1, rs.getInt("survey_batch_id"));
				ResultSet rs1 = cstmt1.executeQuery();
				while (rs1.next()) {
					Question q = new Question();
					q.setEndDate(rs1.getDate("enddate"));
					q.setStartDate(rs1.getDate("startdate"));
					q.setQuestionText(rs1.getString("question"));
					q.setQuestionId(rs1.getInt("que_id"));
					q.setResponsePercentage(rs1.getDouble("resp"));
					q.setQuestionType(QuestionType.values()[rs1.getInt("que_type")]);
					q.setSurveyBatchId(rs1.getInt("survey_batch_id"));
					questionList.add(q);
				}
				b.setQuestionList(questionList);
				batchList.add(b);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return batchList;

	}

	/**
	 * Changes the frequency of a batch
	 * @param batch - The batch object 
	 * @param changedFrequency - The updated frequency value
	 * @return - True/False depending on whether the frequency has been changed or not
	 */
	/**
	 * Changes the frequency of a batch
	 * @param batch - The batch object 
	 * @param changedFrequency - The updated frequency value
	 * @return - True/False depending on whether the frequency has been changed or not
	 */
	public static boolean changeFrequency(Batch batch, Frequency changedFrequency) {
		DatabaseConnectionHelper dch = new DatabaseConnectionHelper();

		boolean isChanged = false;

		// Should trigger the updation of all future dates for the questions part of the batch
		// TODO check if UI does this validation
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
				org.apache.log4j.Logger.getLogger(BatchList.class)
						.debug("Do nothing... Question has already been completed : " + q.getQuestionText());
				continue;
			} else {

				boolean isCurrent = q.getQuestionStatus(q.getStartDate(), q.getEndDate()).equalsIgnoreCase("current");
				previousEndDate = updateQuestion(q, changedFrequency, isCurrent, previousEndDate);
				try {
					CallableStatement cstmt = dch.mysqlCon.prepareCall("{call updateQuestionDate(?, ?, ?)}");
					cstmt.setInt(1, questionId);
					cstmt.setDate(2, (java.sql.Date) q.getStartDate());
					cstmt.setDate(3, (java.sql.Date) q.getEndDate());
					ResultSet rs = cstmt.executeQuery();
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while updating question ID : " + questionId, e);
				}

			}

		}
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call updateBatch(?, ?, ?, ?)}");
			cstmt.setInt(1, batch.getBatchId());
			cstmt.setInt(2, changedFrequency.getValue());
			cstmt.setDate(3, (java.sql.Date) batch.getStartDate());
			cstmt.setDate(4, (java.sql.Date) previousEndDate);
			ResultSet rs = cstmt.executeQuery();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(BatchList.class).error("Exception while updating batch ID : " + batch.getBatchId(), e);
		}
		// if start date + frequency < current_date then end date = current date
		// add days to the next question based on end date of the previous question
		// questions will be ordered based on the question ID

		return isChanged;

	}

	
	
	private static Date updateQuestion(Question q, Frequency frequency, boolean isCurrent, Date previousEndDate) {

		switch (frequency) {

		case WEEKLY: // 7
			if (isCurrent) {
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(q.getStartDate(), 6));
				q.setEndDate(endDate.before(Date.from(Instant.now())) ? convertJavaDateToSqlDate(Date.from(Instant.now())) : endDate);
				previousEndDate = endDate;
			} else {
				Date startDate = convertJavaDateToSqlDate(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(startDate, 6));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}

			break;
		case BIWEEKLY: // 14
			if (isCurrent) {
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(q.getStartDate(), 13));
				q.setEndDate(endDate.before(Date.from(Instant.now())) ? convertJavaDateToSqlDate(Date.from(Instant.now())) : endDate);
				previousEndDate = endDate;

			} else {
				Date startDate = convertJavaDateToSqlDate(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(startDate, 13));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;
		case MONTHLY: // 1 month
			if (isCurrent) {
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 1), -1));
				q.setEndDate(endDate.before(Date.from(Instant.now())) ? convertJavaDateToSqlDate(Date.from(Instant.now())) : endDate);
				previousEndDate = endDate;
			} else {
				Date startDate = convertJavaDateToSqlDate(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 1), -1));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;
		case QUARTERLY: // 3 months
			if (isCurrent) {
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 3), -1));
				q.setEndDate(endDate.before(Date.from(Instant.now())) ? convertJavaDateToSqlDate(Date.from(Instant.now())) : endDate);
				previousEndDate = endDate;
			} else {
				Date startDate = convertJavaDateToSqlDate(DateUtils.addDays(previousEndDate, 1));
				q.setStartDate(startDate);
				Date endDate = convertJavaDateToSqlDate(DateUtils.addDays(DateUtils.addMonths(q.getStartDate(), 3), -1));
				q.setEndDate(endDate);
				previousEndDate = endDate;
			}
			break;

		}

		return previousEndDate;
	}

	private static java.sql.Date convertJavaDateToSqlDate(java.util.Date date) {
		return new java.sql.Date(date.getTime());
	}

}