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
import org.rosuda.REngine.REXP;

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

		Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");

		System.out.println("Initiative type map : " + initiative.getInitiativeTypeMap("Team"));

		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		List<Filter> filterMasterList = fl.getFilterValues();
		System.out.println("All filter values : " + filterMasterList);
		Filter functionFilter = fl.getFilterValues("Position");
		System.out.println("Function filter values : " + functionFilter);

		EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list :" + el.getEmployeeSmartListForIndividual());
		el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list : " + el.getEmployeeSmartListForTeam(filterMasterList, "Retention"));

		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		partOfEmployeeList.add(e.get(16));
		partOfEmployeeList.add(e.get(12));
		partOfEmployeeList.add(e.get(11));

		List<Employee> ownerOfList = new ArrayList<>();
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

		System.out.println(initiative.get(1)); // individual
		Initiative initObj = initiative.get(6);
		initObj.getFilterList();

		// System.out.println(initiative.get(8)); //team

		MetricsList ml = (MetricsList) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsList");
		System.out.println(ml.getInitiativeMetricsForIndividual(partOfEmployeeList, 1));
		System.out.println(ml.getInitiativeMetricsForTeam(filterMasterList, 6));

		Initiative updatedinitiative = initiative.get(3);
		ownerOfList.clear();
		ownerOfList.add(e.get(7));
		ownerOfList.add(e.get(20));
		ownerOfList.add(e.get(22));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated");
		updatedinitiative.setOwnerOfList(ownerOfList);
		initiative.updateInitiative(updatedinitiative);
		System.out.println(il.getInitiativeList());

		initiative.delete(2);
		System.out.println(il.getInitiativeListByStatus("Active"));
		System.out.println(il.getInitiativeListByType(1));

		initiative = initiative.get(3);
		initiative.complete(3);

		InitiativeHelper ih = (InitiativeHelper) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeHelper");
		System.out.println(ih.getInitiativeCount());

		il.getInitiativeListByStatus("Deleted");
		il.getInitiativeListByStatus("Completed");
		il.getInitiativeListByStatus("Active");
		il.getInitiativeListByStatus("Pending");
		il.getInitiativeList();
		
		testRScript();

	}

	/**
	 * library(Rserve)
	 * Rserve(args = "--no-save")
	 */
	public static void testRScript() {
		try {

			DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();

			// source the Palindrom function
			dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/rscript.r\")");

			// call the function. Return true
			REXP is_aba_palindrome = dch.rCon.eval("palindrome('aba')");
			System.out.println(is_aba_palindrome.asInteger()); // prints 1 => true

			// call the function. return false
			REXP is_abc_palindrome = dch.rCon.eval("palindrome('abc')");
			System.out.println(is_abc_palindrome.asInteger()); // prints 0 => false

			dch.rCon.eval("source(\"/Users/apple/Documents/workspace/owen/scripts/performanceTeam.r\")");
			int[] funcParam = { 1 };
			int[] zoneParam = { 8 };
			int[] posParam = { 4 };

			dch.rCon.assign("funcParam", funcParam);
			dch.rCon.assign("zoneParam", zoneParam);
			dch.rCon.assign("posParam", posParam);

			REXP performance = dch.rCon.parseAndEval("try(eval(Performance(funcParam ,posParam ,zoneParam)))");
			if (performance.inherits("try-error")) {
				System.err.println("Error: " + performance.asDouble());
			} else {
				System.out.println(performance.asDouble()); // prints 0 => false
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
