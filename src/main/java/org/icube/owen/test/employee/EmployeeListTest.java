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
import org.icube.owen.filter.FilterList;
import org.junit.Test;

public class EmployeeListTest {
	EmployeeList el = (EmployeeList) ObjectFactory.getInstance("org.icube.owen.employee.EmployeeList");

	@Test
	public void testGetEmployeeSmartListForTeam() {
		List<Employee> empList = new ArrayList<Employee>();
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
		empList = el.getEmployeeSmartListForTeam(filterList, 6);
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

	@Test
	public void testGetEmployeeListByFilters() {
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues();
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}

		Filter filter = filterList.get(0);
		if (filter.getFilterName().equalsIgnoreCase("Function")) {
			filter.getFilterValues().clear();
			filter.getFilterValues().put(1, "HR");
		}

		Map<Integer, Employee> result = el.getEmployeeListByFilters(1, filterList);
		assertNotNull(result);
	}

}
