package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class QuestionList extends TheBorg {

	/**
	 * Retrieves the list of all the questions
	 * @param companyId - Company ID
	 * @return - A list of questions
	 */

	public List<Question> getQuestionList(int companyId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Question> questionList = new ArrayList<>();

		try {
			dch.getCompanyConnection(companyId);
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getQuestionList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Question q = new Question();
				q.setQuestionId(rs.getInt("que_id"));
				q.setQuestionText(rs.getString("question"));
				q.setQuestionType(QuestionType.values()[rs.getInt("que_type")]);
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
				q.setStartDate(rs.getDate("start_date"));
				q.setEndDate(rs.getDate("end_date"));
				q.setResponsePercentage(rs.getDouble("resp"));
				q.setRelationshipTypeId(rs.getInt("rel_id"));
				questionList.add(q);
			}
			rs.close();
			cstmt.close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(QuestionList.class).error("Exception while retrieving the list of questions", e);
		}
		return questionList;
	}

	/**
	 * Retrieves the list of questions in a specific batch
	 * @param companyId - Company ID
	 * @param batchId - the batch ID
	 * @return - A list of Question objects
	 */

	public List<Question> getQuestionListForBatch(int companyId, int batchId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Question> questionList = new ArrayList<Question>();
		try {
			dch.getCompanyConnection(companyId);
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getBatchQuestionList(?)}");
			cstmt.setInt(1, batchId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				Question q = new Question();
				q.setEndDate(rs.getDate("end_date"));
				q.setStartDate(rs.getDate("start_date"));
				q.setQuestionText(rs.getString("question"));
				q.setQuestionId(rs.getInt("que_id"));
				q.setResponsePercentage(rs.getDouble("resp"));
				q.setQuestionType(QuestionType.values()[rs.getInt("que_type")]);
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
				q.setRelationshipTypeId(rs.getInt("rel_id"));
				questionList.add(q);
			}
			rs.close();
			cstmt.close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(QuestionList.class).error("Exception while retrieving the list of questions for batch ID" + batchId, e);
		}

		return questionList;
	}

	/**
	 * Retrieves the list of questions of a particular batch and status (Upcoming/Completed)
	 * @param companyId - Company ID
	 * @param batchId - batch ID 
	 * @param filter - the status of the questions to be retrieved 
	 * @return - A list of Question objects based on the filter(status) and the batch ID 
	 */

	public List<Question> getQuestionListByStatus(int companyId, int batchId, String filter) {
		List<Question> questionList = getQuestionListForBatch(companyId, batchId);
		List<Question> questionListByStatus = new ArrayList<Question>();

		for (Question q1 : questionList) {
			if ((q1.getQuestionStatus(q1.getStartDate(), q1.getEndDate())).equalsIgnoreCase(filter)) {
				questionListByStatus.add(q1);
			}
		}
		org.apache.log4j.Logger.getLogger(QuestionList.class).debug(
				"Retrieved " + questionListByStatus.size() + " questions for " + filter + " status.");
		if (filter.equalsIgnoreCase("Completed")) {
			Collections.reverse(questionListByStatus);
		}
		return questionListByStatus;
	}

}
