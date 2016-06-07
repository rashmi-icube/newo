package org.icube.owen.test.employee;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.employee.EmployeeList;
import org.icube.owen.filter.Filter;
import org.icube.owen.test.TestHelper;
import org.junit.Ignore;
import org.junit.Test;

public class EmployeeListTest {
	EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");
	Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
	int companyId = 2;

	@Ignore
	public void testGetEmployeeMasterList() {
		List<Employee> empList = new ArrayList<Employee>();
		empList = el.getEmployeeMasterList(companyId);
		for (Employee e : empList) {
			assertNotNull(e.getFirstName());
			assertNotNull(e.getLastName());
			assertNotNull(e.getEmployeeId());
			assertNotNull(e.getCompanyId());
		}

	}

	@Ignore
	public void testGetEmployeeSmartListForTeam() {
		List<Employee> empList = new ArrayList<Employee>();
		List<Filter> filterList = new ArrayList<Filter>();
		Map<Integer, String> filterValuesMap = new HashMap<>();
		filterValuesMap.put(1, "HR");
		Filter f = new Filter();
		f.setFilterName("Function");
		f.setFilterId(1);
		f.setFilterValues(filterValuesMap);
		filterList.add(f);
		Map<Integer, String> filterValuesMap1 = new HashMap<>();
		filterValuesMap1.put(3, "Corporate");
		filterValuesMap1.put(4, "Region");
		Filter f1 = new Filter();
		f1.setFilterName("Position");
		f1.setFilterId(2);
		f1.setFilterValues(filterValuesMap1);
		filterList.add(f1);
		Map<Integer, String> filterValuesMap2 = new HashMap<>();
		filterValuesMap2.put(8, "INTG1");
		filterValuesMap2.put(9, "INTG2");
		Filter f2 = new Filter();
		f2.setFilterName("Zone");
		f2.setFilterId(3);
		f2.setFilterValues(filterValuesMap2);
		filterList.add(f2);
		empList = el.getEmployeeSmartListForTeam(companyId, filterList, 7);
		assertNotNull(empList);
		for (Employee emp : empList) {
			assertNotNull(emp.getEmployeeId());
			assertNotNull(emp.getFirstName());
			assertNotNull(emp.getLastName());
			assertNotNull(emp.getReportingManagerId());
			assertNotNull(emp.getCompanyEmployeeId());
			assertNotNull(emp.getScore());
		}

	}

	@Ignore
	public void testGetEmployeeSmartListForIndividual() {
		List<Employee> empList = new ArrayList<Employee>();
		List<Employee> partOfEmployeeList = new ArrayList<>();
		partOfEmployeeList.add(e.get(companyId, 16));
		empList = el.getEmployeeSmartListForIndividual(companyId, partOfEmployeeList, 1);
		assertNotNull(empList);
	}

	@Test
	public void testGetEmployeeListByFilters() {
		List<Filter> filterList = TestHelper.getOneForEachFilter(companyId);
		List<Employee> result = el.getEmployeeListByFilters(companyId, filterList);
		assertNotNull(result);
	}
	
	@Test
	public void testGet(){
		List<Integer> empIdList = new ArrayList<>();
		for(int i = 1; i<=287; i++){
			empIdList.add(i);
		}
		assertNotNull(el.get(1, empIdList));
	}
}
