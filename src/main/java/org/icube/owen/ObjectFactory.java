package org.icube.owen;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.initiative.InitiativeList;

public class ObjectFactory {
	static Logger myLogger = getLogger("org.icube.owen.ObjectFactory");

	/**
	 * Get the instance of class given in the parameter
	 * 
	 * @param className
	 * @return class instance
	 */
	static public TheBorg getInstance(String className) {
		Class<?> c;
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			myLogger.error("Exception while creating an instance for class : " + className, e1);
			return null;
		}
		Constructor<?> cons;
		try {
			cons = c.getConstructors()[0];
			TheBorg object = (TheBorg) cons.newInstance();
			return object;
		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			myLogger.error("Exception while calling the constructor for class : " + className, e);
			return null;
		}
	}

	// TODO make this function private
	static DatabaseConnectionHelper dch;
	static Logger logger;

	static public DatabaseConnectionHelper getDBHelper() {
		if (dch == null) {
			dch = new DatabaseConnectionHelper();
		}
		return dch;
	}

	static public Logger getLogger(String className) {
		if (logger == null) {
			try {
				logger = Logger.getLogger(Class.forName(className));
			} catch (ClassNotFoundException e) {
				myLogger.error("Class not found for logger object for class " + className, e);

			}

		}
		return logger;
	}

	public static void main(String[] args) {

		Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");
		System.out.println("Initiative type map : " + initiative.getInitiativeTypeMap());

		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		List<Filter> filterMasterList = fl.getFilterValues();
		System.out.println("All filter values : " + filterMasterList);
		Filter functionFilter = fl.getFilterValues("Function");
		System.out.println("Function filter values : " + functionFilter);

		EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list : " + el.getEmployeeSmartList(filterMasterList));

		List<Employee> ownerOfList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		ownerOfList.add(e.get("5031840"));
		ownerOfList.add(e.get("549192"));
		ownerOfList.add(e.get("507212"));

		initiative.setInitiativeProperties("YourInitiative", "Change Process", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		InitiativeList il = (InitiativeList) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeList");
		System.out.println(il.getInitiativeList());

		System.out.println(initiative.get(1));
	}
}
