package org.icube.owen.configure;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class BatchList extends TheBorg {
	
	public static void main(String arg[]) {
		getBatchList();
	}

	public static List<Batch> getBatchList(){

		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		List<Batch> batchList = new ArrayList<Batch>();
		List<Question> questionList = new ArrayList<Question>();
		Batch b = new Batch();
		Question q = new Question();
		try {
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getBatchList()}");
			ResultSet rs = cstmt.executeQuery();
			while (rs.next()) {
				b.setFrequency(Frequency.values()[rs.getInt("freq_id")]);
				b.setStartDate(rs.getDate("start_date"));
				b.setEndDate(rs.getDate("end_date"));
				b.setQuestionList(questionList);
				b.setBatchId(rs.getInt("survey_batch_id"));
				batchList.add(b);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return batchList ;
		
	}

}