package org.icube.owen.test.jobScheduler;

import java.sql.SQLException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.jobScheduler.CompanyDAO;
import org.junit.Test;

public class CompanyDAOTest {
	CompanyDAO cdao = new CompanyDAO();

	@Test
	public void testCompanyDAO() {
		// cdao.getCompanyDetails();
		cdao.run();
		cdao.run();
		
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			dch.companySqlConnectionPool.get(1).close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cdao.run();
		try {
			dch.companyNeoConnectionPool.get(1).close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cdao.run();
		try {
			dch.companySqlConnectionPool.get(1).close();
			dch.companyNeoConnectionPool.get(1).close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

}
