package org.icube.owen.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;

public class TestHelper {

	static int companyId = 1;

	public static List<Filter> getAllForAllFilters() {
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

		return filterList;
	}

	public static List<Filter> getOneForEachFilter() {
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues(companyId);
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 1) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}
		return filterList;
	}

	public static List<Filter> getTwoForEachFilter() {
		FilterList fl = new FilterList();
		List<Filter> filterList = fl.getFilterValues(companyId);
		for (Filter f : filterList) {
			while (f.getFilterValues().size() > 2) {
				f.getFilterValues().remove(f.getFilterValues().keySet().iterator().next());
			}
		}
		return filterList;
	}

	public static List<Filter> getAllForOneFilter() {
		List<Filter> filterList = getOneForEachFilter();
		filterList.get(0).getFilterValues().clear();
		filterList.get(0).getFilterValues().put(0, "All");
		return filterList;
	}

	public static List<Filter> getAllForTwoFilters() {
		List<Filter> filterList = getAllForOneFilter();
		filterList.get(1).getFilterValues().clear();
		filterList.get(1).getFilterValues().put(0, "All");
		return filterList;
	}
}
