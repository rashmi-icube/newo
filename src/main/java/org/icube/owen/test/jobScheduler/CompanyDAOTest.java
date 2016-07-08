package org.icube.owen.test.jobScheduler;

import org.icube.owen.jobScheduler.CompanyDAO;
import org.junit.Test;

public class CompanyDAOTest {
	CompanyDAO cdao = new CompanyDAO();

	@Test
	public void testCompanyDAO() {
		// cdao.getCompanyDetails();
		cdao.run();
		cdao.run();

		/*DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		try {
			dch.companySqlConnectionPool.get(1).close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAOTest.class).error("Error in closing the Sql connection", e);
		}
		cdao.run();
		try {
			dch.companyNeoConnectionPool.get(1).close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAOTest.class).error("Error in closing the Neo connection", e);
		}
		cdao.run();
		try {
			dch.companySqlConnectionPool.get(1).close();
			dch.companyNeoConnectionPool.get(1).close();
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAOTest.class).error("Error in closing the database connections (Sql/Neo)", e);
		}*/
	}

}
