package org.icube.owen.test.initiative;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.filter.Filter;
import org.icube.owen.initiative.Initiative;
import org.junit.Test;

public class InitiativeTest {
	Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");
	
	@Test
	public void testSetInitiativeProperties(){
		List<Employee> ownerOfList = new ArrayList<>();
		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = new Employee();
		ownerOfList.add(e.get(19));
		ownerOfList.add(e.get(18));
		ownerOfList.add(e.get(3));
		partOfEmployeeList.add(e.get(16));
		List<Filter> filterList = new ArrayList<Filter>();
		Map<Integer, String> filterValuesMap = new HashMap<>();
		filterValuesMap.put(0, "All");
        Filter f = new Filter();
        f.setFilterName("Function");
        f.setFilterId(1);
        f.setFilterValues(filterValuesMap);
        filterList.add(f);
        Map<Integer, String> filterValuesMap1 = new HashMap<>();
		filterValuesMap1.put(0, "All");
        Filter f1 = new Filter();
        f1.setFilterName("Position");
        f1.setFilterId(2);
        f1.setFilterValues(filterValuesMap1);
        filterList.add(f1);
        Map<Integer, String> filterValuesMap2 = new HashMap<>();
		filterValuesMap2.put(0, "All");
        Filter f2 = new Filter();
        f2.setFilterName("Zone");
        f2.setFilterId(3);
        f2.setFilterValues(filterValuesMap2);
        filterList.add(f2);

		initiative.setInitiativeProperties("1Individual", 1, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();
		initiative.setInitiativeProperties("1Team", 6, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();
		initiative.setInitiativeProperties("3Individual", 3, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("4Individual", 4, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("5Individual", 5, "Individual", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create();

		initiative.setInitiativeProperties("1Team", 6, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("2Team", 7, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("3Team", 8, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("4Team", 9, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();

		initiative.setInitiativeProperties("5Team", 10, "Team", Date.from(Instant.now()), Date.from(Instant.now()),Date.from(Instant.now()),
				"You are owners of the initiative", filterList, ownerOfList, null);
		initiative.create();
		
	}
	
	@Test
	public void testGet(){
		Initiative i = new Initiative();
		i = initiative.get(1);
		assertNotNull(i.getInitiativeName());
		assertNotNull(i.getInitiativeStartDate());
		assertNotNull(i.getInitiativeMetrics());
	}
	
	@Test
	public void testGetInitiativeTypeMap(){
		 Map<Integer, String> initiativeTypeMap = initiative.getInitiativeTypeMap("Individual");
	}
	
	@Test
	public void testDelete(){
		boolean status = initiative.delete(17);
		assertTrue(status);
	}
	
	@Test
	public void testUpdateInitiative(){
		Initiative updatedinitiative = initiative.get(19);
		List<Employee> ownerOfList = new ArrayList<>();
		Employee e = new Employee();
		ownerOfList.add(e.get(7));
		ownerOfList.add(e.get(20));
		ownerOfList.add(e.get(22));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated now");
		updatedinitiative.setOwnerOfList(ownerOfList);
		boolean status = initiative.updateInitiative(updatedinitiative);
		assertTrue(status);
	}
	
	@Test
	public void testComplete(){
		boolean status = initiative.complete(16);
		assertTrue(status);
	}

}
