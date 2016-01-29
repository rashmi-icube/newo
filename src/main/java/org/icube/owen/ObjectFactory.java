package org.icube.owen;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.initiative.InitiativeHelper;
import org.icube.owen.initiative.InitiativeList;
import org.icube.owen.metrics.MetricsList;

public class ObjectFactory {

	/**
	 * Get the instance of class given in the parameter
	 * 
	 * @param className
	 * className for which the instance is to be created
	 * @return instance object for the className given
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
			cons = c.getConstructors()[0];
			TheBorg object = (TheBorg) cons.newInstance();
			return object;
		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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

		//testRScript();

		Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");

		System.out.println("Initiative type map : " + initiative.getInitiativeTypeMap("team"));

		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		List<Filter> filterMasterList = fl.getFilterValues();
		System.out.println("All filter values : " + filterMasterList);
		Filter functionFilter = fl.getFilterValues("Position");
		System.out.println("Function filter values : " + functionFilter);

		EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list : " + el.getEmployeeSmartList(filterMasterList));
		
		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e1 = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		partOfEmployeeList.add(e1.get(16));
		partOfEmployeeList.add(e1.get(12));
		partOfEmployeeList.add(e1.get(11));

		List<Employee> ownerOfList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		ownerOfList.add(e.get(19));
		ownerOfList.add(e.get(18));
		ownerOfList.add(e.get(3));

		initiative.setInitiativeProperties("1Individual", "1", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("2Individual", "2", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("3Individual", "3", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("4Individual", "4", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("5Individual", "5", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("1Team", "6", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("2Team", "7", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("3Team", "8", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("4Team", "9", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("5Team", "10", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		InitiativeList il = (InitiativeList) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeList");
		System.out.println(il.getInitiativeList());

		System.out.println(initiative.get(1)); //individual
		// System.out.println(initiative.get(8)); //team

		MetricsList ml = (MetricsList) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsList");
		System.out.println(ml.getInitiativeMetrics("team", filterMasterList));

		Initiative updatedinitiative = initiative.get(3);
		ownerOfList = new ArrayList<>();
		e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");

		ownerOfList.add(e.get(7));
		ownerOfList.add(e.get(20));
		ownerOfList.add(e.get(22));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated");
		updatedinitiative.setOwnerOfList(ownerOfList);
		initiative.updateInitiative(updatedinitiative);

		initiative.delete(2);
		System.out.println(il.getInitiativeList("Deleted"));

		initiative = initiative.get(3);
		initiative.complete(3);

		InitiativeHelper ih = (InitiativeHelper) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeHelper");
		System.out.println(ih.getInitiativeCount());
		
		il.getInitiativeList("Deleted");
		il.getInitiativeList("Completed");
		il.getInitiativeList("Active");
		il.getInitiativeList("Pending");
		il.getInitiativeList();

	}

	/*public static void testRScript() {
		try {
			RConnection c = new RConnection();
			// source the Palindrom function
			c.eval("source(\"/Users/apple/Documents/workspace/owen/resources/rscript.r\")");

			// call the function. Return true
			REXP is_aba_palindrome = c.eval("palindrome('aba')");
			System.out.println(is_aba_palindrome.asInteger()); // prints 1 => true

			// call the function. return false
			REXP is_abc_palindrome = c.eval("palindrome('abc')");
			System.out.println(is_abc_palindrome.asInteger()); // prints 0 => false

		} catch (REngineException e) {
			org.apache.log4j.Logger.getLogger(ObjectFactory.class).error("Exception while trying to run RScript", e);
		} catch (REXPMismatchException e1) {
			org.apache.log4j.Logger.getLogger(ObjectFactory.class).error("Exception while trying to run RScript", e1);
		}

	}*/
}
