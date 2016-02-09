package org.icube.owen.survey;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class QuestionList extends TheBorg {
	public static void main(String arg[]) {
		getQuestionListByStatus(1, "Upcoming");
		getQuestionList();
	}

	public static List<Question> getQuestionList() {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Question> questionList = new ArrayList<Question>();
		Question q = new Question();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getQuestionList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				q.setEndDate(rs.getDate("enddate"));
				q.setStartDate(rs.getDate("startdate"));
				q.setQuestionText(rs.getString("question"));
				q.setQuestionId(rs.getInt("que_id"));
				q.setResponsePercentage(rs.getDouble("resp"));
				q.setQuestionType(QuestionType.values()[rs.getInt("que_type")]);
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
				questionList.add(q);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return questionList;
	}

	public static List<Question> getQuestionListByStatus(int batchId, String questionStatus) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Question> questionList = new ArrayList<Question>();
		List<Question> QuestionListByStatus = new ArrayList<Question>();
		Question q = new Question();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getQuestionListForBatch(?)}");
			cstmt.setInt(1, batchId);
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				q.setEndDate(rs.getDate("enddate"));
				q.setStartDate(rs.getDate("startdate"));
				q.setQuestionText(rs.getString("question"));
				q.setQuestionId(rs.getInt("que_id"));
				q.setResponsePercentage(rs.getDouble("resp"));
				q.setQuestionType(QuestionType.values()[rs.getInt("que_type")]);
				q.setSurveyBatchId(rs.getInt("survey_batch_id"));
				questionList.add(q);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		for (Question q1 : questionList) {

			switch (questionStatus) {
			case ("Upcoming"):
				if (q1.getStartDate().after(Date.from(Instant.now()))) {
					QuestionListByStatus.add(q1);
				}
				break;
			case ("Completed"):
				if (q.getEndDate().before(Date.from(Instant.now()))) {
					QuestionListByStatus.add(q1);
				}
				break;

			}
		}

		return QuestionListByStatus;
	}
}
