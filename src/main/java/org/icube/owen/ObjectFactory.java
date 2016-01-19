package org.icube.owen;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.initiative.InitiativeList;

public class ObjectFactory {

	/**
	 * Get the instance of class given in the parameter
	 * @param className
	 * @return class instance
	 */
	static public TheBorg getInstance(String className) {
		Class<?> c;
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			org.apache.log4j.Logger.getLogger(ObjectFactory.class).error("Exception while creating an instance for class : " + className, e1);
			return null;
		}
		Constructor<?> cons;
		try {
			cons =c.getConstructor(c.getClass());
			TheBorg object = (TheBorg) cons.newInstance();
			return object;
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			org.apache.log4j.Logger.getLogger(ObjectFactory.class).error("Exception while calling the constructor for class : " + className, e);
			return null;
		} 
	}

	// TODO make this function private
	static DatabaseConnectionHelper dch;

	static public DatabaseConnectionHelper getDBHelper() {
		if (dch == null) {
			dch = new DatabaseConnectionHelper();
		}
		return dch;
	}

	public static void main(String[] args) {
		
		Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");
		System.out.println("Initiative type map : " + initiative.getInitiativeTypeMap());
		
		/*String filter = "Function";
		FilterList fl = new FilterList();
		fl.getFilterValues(filter);
	

		

		List<Filter> filterList = new ArrayList<>();

		// function dummy values
		Filter f = new Filter();
		Map<String, String> filterValueMap = new HashMap<>();
		f.setFilterName("Function");
		filterValueMap.put("1", "F1");
		// filterValueMap.put("2", "F2");
		f.setFilterValues(filterValueMap);
		filterList.add(f);

		// zone dummy values
		Filter f1 = new Filter();
		filterValueMap = new HashMap<>();
		f1.setFilterName("Zone");
		filterValueMap.put("1", "All");
		f1.setFilterValues(filterValueMap);
		filterList.add(f1);

		// position dummy values
		Filter f2 = new Filter();
		filterValueMap = new HashMap<>();
		f2.setFilterName("Position");
		filterValueMap.put("1", "All");
		f2.setFilterValues(filterValueMap);
		filterList.add(f2);

		EmployeeList el = new EmployeeList();
		List<Employee> employeeList = el.getEmployeeSmartList(filterList);

		initiative.setInitiativeProperties("OurInitiative", "Change Process", Date.from(Instant.now()), Date.from(Instant.now()), "xyz", filterList,
				employeeList);

		int initiativeId = initiative.create();
		System.out.println("Created initiative with ID : " + initiativeId);
		if (initiativeId > 0) {
			if (initiative.setPartOf(initiativeId, filterList)) {
				org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting part of initiative");
			}
			if (initiative.setOwner(initiativeId, employeeList)) {
				org.apache.log4j.Logger.getLogger(Initiative.class).debug("Success in setting owner for initiative");
			}
		}

		get(5);


		InitiativeList ri = new InitiativeList();
		List<Initiative> initiativeList = ri.getInitiativeList();
		System.out.println(initiativeList.toString());*/
	

	}
}
