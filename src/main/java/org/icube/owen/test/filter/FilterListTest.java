package org.icube.owen.test.filter;

import java.util.List;

import org.icube.owen.ObjectFactory;
import org.icube.owen.filter.Filter;
import org.icube.owen.filter.FilterList;
import org.junit.Test;

public class FilterListTest {
	FilterList fl = (FilterList) ObjectFactory.getInstance("org.icube.owen.filter.FilterList");
	int companyId = 2;
	
	@Test
	public void testGetFilterValues(){
		List<Filter> fList = fl.getFilterValues(companyId);
		for(Filter f : fList){
			System.out.println(f.getFilterId());
			System.out.println(f.getFilterName());
		}

		
	}
	
	@Test
	public void testGetFilterValues2(){
		Filter f = fl.getFilterValues(companyId,"Function");
		System.out.println(f.getFilterId());
		System.out.println(f.getFilterName());
		
	}

}
