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

public class ObjectFactory {

	// TODO Ravi : What is the point of creating class objects from ObjectFactory

	// TODO Ravi : What is the point of creating class objects from ObjectFactory
	
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
		Filter functionFilter = fl.getFilterValues("Function");
		System.out.println("Function filter values : " + functionFilter);

		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		partOfEmployeeList.add(e.get(16));

		EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list :" + el.getEmployeeSmartListForIndividual(partOfEmployeeList, 1));
		el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list : " + el.getEmployeeSmartListForTeam(filterMasterList, 7));

		List<Employee> ownerOfList = new ArrayList<>();
		ownerOfList.add(e.get(19));
		ownerOfList.add(e.get(18));
		ownerOfList.add(e.get(3));

		initiative.setInitiativeProperties("1Individual", 1, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("2Individual", 2, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("3Individual", 3, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("4Individual", 4, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("5Individual", 5, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("1Team", 6, "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("2Team", 7, "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("3Team", 8, "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("4Team", 9, "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("5Team", 10, "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList, null);
		initiative.create();

		InitiativeList il = (InitiativeList) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeList");
		System.out.println(il.getInitiativeList());

		System.out.println(initiative.get(1)); // individual
		Initiative initObj = initiative.get(6);
		initObj.getFilterList();

		System.out.println(initiative.get(8)); // team

		Initiative updatedinitiative = initiative.get(16);
		ownerOfList.clear();
		ownerOfList.add(e.get(7));
		ownerOfList.add(e.get(20));
		ownerOfList.add(e.get(22));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated");
		updatedinitiative.setOwnerOfList(ownerOfList);
		initiative.updateInitiative(updatedinitiative);
		System.out.println(il.getInitiativeList());

		initiative.delete(17);
		System.out.println(il.getInitiativeListByStatus("Individual", "Active"));
		System.out.println(il.getInitiativeListByType("Individual", 1));

		initiative = initiative.get(17);
		initiative.complete(17);

		InitiativeHelper ih = (InitiativeHelper) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeHelper");
		System.out.println(ih.getInitiativeCount());

		il.getInitiativeListByStatus("Team", "Deleted");
		il.getInitiativeListByStatus("Team", "Completed");
		il.getInitiativeListByStatus("Team", "Active");
		il.getInitiativeListByStatus("Team", "Pending");
		il.getInitiativeList();

	}
}
