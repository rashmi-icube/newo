package org.icube.owen.configure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class QuestionList extends TheBorg {
	public static void main(String arg[]) {
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
}
