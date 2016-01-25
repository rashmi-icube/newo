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
	 * @param className className for which the instance is to be created
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

		System.out.println("Initiative type map : " + initiative.getInitiativeTypeMap("team"));

		FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
		List<Filter> filterMasterList = fl.getFilterValues();
		System.out.println("All filter values : " + filterMasterList);
		Filter functionFilter = fl.getFilterValues("Position");
		System.out.println("Function filter values : " + functionFilter);

		EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
		System.out.println("Employee smart list : " + el.getEmployeeSmartList(filterMasterList));

		List<Employee> ownerOfList = new ArrayList<>();
		Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		ownerOfList.add(e.get("5031840"));
		ownerOfList.add(e.get("549192"));
		ownerOfList.add(e.get("507212"));

		initiative.setInitiativeProperties("1Individual", "Performance", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("2Individual", "Social Cohesion", "Individual", Date.from(Instant.now()), Date.from(Instant
				.now()), "You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("3Individual", "Retention", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("4Individual", "Innovation", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("5Individual", "Sentiment", "Individual", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("1Team", "Performance", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("2Team", "Social Cohesion", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("3Team", "Retention", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("4Team", "Innovation", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		initiative.setInitiativeProperties("5Team", "Sentiment", "Team", Date.from(Instant.now()), Date.from(Instant.now()),
				"You are owners of the initiative", filterMasterList, ownerOfList);
		initiative.create();

		InitiativeList il = (InitiativeList) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeList");
		System.out.println(il.getInitiativeList());

		System.out.println(initiative.get(1));

		MetricsList ml = (MetricsList) ObjectFactory.getInstance("org.icube.owen.metrics.MetricsList");
		System.out.println(ml.getInitiativeMetrics("team", filterMasterList));

		Initiative updatedinitiative = initiative.get(3);
		ownerOfList = new ArrayList<>();
		e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");

		ownerOfList.add(e.get("52312"));
		ownerOfList.add(e.get("549192"));
		ownerOfList.add(e.get("507212"));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated");
		updatedinitiative.setOwnerOf(ownerOfList);
		initiative.updateInitiative(updatedinitiative);

		initiative.delete(2);
		
		initiative = initiative.get(3);
		initiative.complete(3);

		InitiativeHelper ih = (InitiativeHelper) ObjectFactory.getInstance("org.icube.owen.initiative.InitiativeHelper");
		System.out.println(ih.getInitiativeCount());

	}
}
