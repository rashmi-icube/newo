package org.icube.owen.test.jobScheduler;

import org.icube.owen.jobScheduler.CompanyDAO;
import org.junit.Test;

public class CompanyDAOTest{
	CompanyDAO cdao = new CompanyDAO();
	
	@Test
	public void testCompanyDAO(){
		//cdao.getCompanyDetails();
		cdao.run();
		
	}

}
