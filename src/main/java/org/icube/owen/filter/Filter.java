package org.icube.owen.filter;

import java.util.List;
import java.util.Map;

import org.icube.owen.TheBorg;

public class Filter extends TheBorg {
	private String filterName;
	private int filterId;
	private List<Map<String, String>> filterValues;
	
	public String getFilterName() {
		return filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public List<Map<String, String>> getFilterValues() {
		return filterValues;
	}

	public void setFilterValues(List<Map<String, String>> filterValuesMapList) {
		this.filterValues = filterValuesMapList;
	}
	
	
}
