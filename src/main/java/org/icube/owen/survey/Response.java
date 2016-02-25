package org.icube.owen.survey;

import java.util.Map;

import org.icube.owen.employee.Employee;

public class Response {


	public boolean saveMeResponse(Question q, Employee e, int responseValue, String feedback) {
		// save response in sql and send acknowledgement

		return true;
	}

	public boolean saveWeResponse(Question q, Employee e, Map<Employee, Integer> employeeRating) {
		// save response in sql and send acknowledgement
		
		return true;
	}
}
