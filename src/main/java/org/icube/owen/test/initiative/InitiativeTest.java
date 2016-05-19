package org.icube.owen.test.initiative;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.initiative.Initiative;
import org.icube.owen.test.TestHelper;
import org.junit.Test;

public class InitiativeTest {
	Initiative initiative = (Initiative) ObjectFactory.getInstance("org.icube.owen.initiative.Initiative");
	int companyId = 2;

	@Test
	public void testSetInitiativeProperties() {
		List<Employee> ownerOfList = new ArrayList<>();
		List<Employee> partOfEmployeeList = new ArrayList<>();
		Employee e = new Employee();
		ownerOfList.add(e.get(companyId, 2));
		ownerOfList.add(e.get(companyId, 4));
		ownerOfList.add(e.get(companyId, 3));
		partOfEmployeeList.add(e.get(companyId, 7));

		initiative.setInitiativeProperties("1Individual", 1, "Individual", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant
				.now()),1, "You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create(companyId);
		initiative.setInitiativeProperties("1Team", 6, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				2,"You are owners of the initiative", TestHelper.getAllForAllFilters(companyId), ownerOfList, null);
		initiative.create(companyId);
		initiative.setInitiativeProperties("3Individual", 3, "Individual", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant
				.now()), 3,"You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create(companyId);

		initiative.setInitiativeProperties("4Individual", 4, "Individual", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant
				.now()),4, "You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create(companyId);

		initiative.setInitiativeProperties("5Individual", 5, "Individual", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant
				.now()),5, "You are owners of the initiative", null, ownerOfList, partOfEmployeeList);
		initiative.create(companyId);

		initiative.setInitiativeProperties("1Team", 6, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				6,"You are owners of the initiative", TestHelper.getAllForOneFilter(companyId), ownerOfList, null);
		initiative.create(companyId);

		initiative.setInitiativeProperties("2Team", 7, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				7,"You are owners of the initiative", TestHelper.getAllForTwoFilters(companyId), ownerOfList, null);
		initiative.create(companyId);

		initiative.setInitiativeProperties("3Team", 8, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				8,"You are owners of the initiative", TestHelper.getOneForEachFilter(companyId), ownerOfList, null);
		initiative.create(companyId);

		initiative.setInitiativeProperties("4Team", 9, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				1,"You are owners of the initiative", TestHelper.getTwoForEachFilter(companyId), ownerOfList, null);
		initiative.create(companyId);

		initiative.setInitiativeProperties("5Team", 10, "Team", Date.from(Instant.now()), Date.from(Instant.now()), Date.from(Instant.now()),
				2,"You are owners of the initiative", TestHelper.getAllForAllFilters(companyId), ownerOfList, null);
		initiative.create(companyId);

	}

	@Test
	public void testGet() {
		Initiative i = new Initiative();
		i = initiative.get(companyId, 18);
		assertNotNull(i.getInitiativeName());
		assertNotNull(i.getInitiativeStartDate());
		assertNotNull(i.getInitiativeMetrics());
		assertNotNull(i.getCreatedByEmpId());
	}

	@Test
	public void testGetInitiativeTypeMap() {
		assertNotNull(initiative.getInitiativeTypeMap(companyId, "Individual"));
	}

	@Test
	public void testDelete() {
		boolean status = initiative.delete(companyId, 17);
		assertTrue(status);
	}

	@Test
	public void testUpdateInitiative() {
		Initiative updatedinitiative = initiative.get(companyId, 18);
		List<Employee> ownerOfList = new ArrayList<>();
		Employee e = new Employee();
		ownerOfList.add(e.get(companyId, 7));
		ownerOfList.add(e.get(companyId, 20));
		ownerOfList.add(e.get(companyId, 22));
		updatedinitiative.setInitiativeEndDate(Date.from(Instant.now()));
		updatedinitiative.setInitiativeComment("the comment has been updated now");
		updatedinitiative.setOwnerOfList(ownerOfList);
		boolean status = initiative.updateInitiative(companyId, updatedinitiative);
		assertTrue(status);
	}

	@Test
	public void testComplete() {
		boolean status = initiative.complete(companyId, 16);
		assertTrue(status);
	}

}
