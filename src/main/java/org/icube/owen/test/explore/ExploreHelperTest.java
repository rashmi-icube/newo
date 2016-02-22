package org.icube.owen.test.explore;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.explore.ExploreHelper;
import org.icube.owen.metrics.Metrics;
import org.junit.Test;

public class ExploreHelperTest {

	ExploreHelper eh = (ExploreHelper) ObjectFactory.getInstance("org.icube.owen.explore.ExploreHelper");
	
	@Test
	public void testGetIndividualMetricsData(){
		Employee empObj = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
		
		Map<Employee, List<Metrics>> result = eh.getIndividualMetricsData(Arrays.asList(empObj.get(1), empObj.get(2)));
		
		assertTrue(!result.isEmpty());
		
		for(Employee e : result.keySet()){
			List<Metrics> metricsList = result.get(e);
			for(Metrics m : metricsList){
				assertTrue(m.getId()>0);
				assertTrue(!m.getName().isEmpty());
				assertTrue(m.getScore()>=0);
				assertTrue(m.getDateOfCalculation()!= null);
			}
		}
		
	}
}
