package org.icube.owen.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ org.icube.owen.test.dashboard.AlertTest.class, org.icube.owen.test.dashboard.HrDashboardHelperTest.class,
		org.icube.owen.test.dashboard.IndividualDashboardHelperTest.class, org.icube.owen.test.employee.EmployeeHelperTest.class,
		org.icube.owen.test.employee.EmployeeListTest.class, org.icube.owen.test.employee.EmployeeTest.class,
		org.icube.owen.test.explore.ExploreHelperTest.class, org.icube.owen.test.individual.LoginTest.class,
		org.icube.owen.test.initiative.InitiativeHelperTest.class, org.icube.owen.test.initiative.InitiativeListTest.class,
		org.icube.owen.test.initiative.InitiativeTest.class, org.icube.owen.test.metrics.MetricsListTest.class,
		org.icube.owen.test.survey.BatchListTest.class, org.icube.owen.test.survey.QuestionListTest.class,
		org.icube.owen.test.survey.QuestionTest.class, org.icube.owen.test.survey.ResponseHelperTest.class })
public class AllTests {

}
