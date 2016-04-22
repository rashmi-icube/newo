package org.icube.owen.jobScheduler;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class CompanyDAO extends TimerTask {

	private RConnection rCon;
	private DatabaseConnectionHelper dch;

	@Override
	public void run() {

		getCompanyDetails();

	}

	/**
	 * Retrieves the details of the company (ID,name,username,password,sql server,sql user id)
	 */
	public void getCompanyDetails() {

		try {
			dch = ObjectFactory.getDBHelper();
			ResultSet companyDetails = null;
			// startDbConnection();

			Statement stmt = null;
			stmt = dch.masterCon.createStatement();
			companyDetails = stmt
					.executeQuery("Select comp_name, comp_id, comp_sql_dbname, sql_server, sql_user_id, sql_password from company_master");

			// connect to r
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("R Connection Available : " + rCon.isConnected());
			int count = 0;
			while (companyDetails.next()) {

				// run JobInitStatus
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("JobInitStatus method started");
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug(
						"Parameters for R function :  CompanyId : " + companyDetails.getInt("comp_id"));

				rCon.assign("CompanyId", new int[] { companyDetails.getInt("comp_id") });
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Calling the actual function in R Script JobInitStatus");
				REXP status = rCon.parseAndEval("try(eval(JobInitStatus(CompanyId)))");
				if (status.inherits("try-error")) {
					dch.releaseRcon();
					sendEmail(companyDetails.getInt("comp_id"), companyDetails.getString("comp_name"), "JobInitStatus", status);
				} else {
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the JobInitStatus method ");
				}
				dch.releaseRcon();

				connectToCompanyDb(companyDetails);
				count++;
			}
			companyDetails.last();
			int size = companyDetails.getRow();
			System.out.println(size);
			if (count == size) {
				EmailSender es = new EmailSender();
				try {
					es.sendEmail("Successfully executed the Scheduler Job");
				} catch (MessagingException e) {
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Unable to send the Email", e);

				}
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Failed to get the db connection details", e);

		}
	}

	/**
	 * Connects to the company database and execute the methods
	 * @param rs - ResultSet containing the details of the company
	 */

	public void connectToCompanyDb(ResultSet rs) {

		Statement stmt = null;
		ResultSet res = null;
		try {
			int companyId = rs.getInt("comp_id");
			dch.getCompanyConnection(companyId);
			org.apache.log4j.Logger.getLogger(CompanyDAO.class)
					.debug("Successfully connected to company db with companyId : " + rs.getInt("comp_id"));

			ArrayList<String> addresses = new ArrayList<String>();
			stmt = dch.companySqlConnectionPool.get(companyId).createStatement();
			res = stmt
					.executeQuery("select distinct(l.login_id) as email_id from (select Distinct(survey_batch_id) as survey_batch_id from question where date(start_date)=CURDATE()) as b join batch_target as bt on b.survey_batch_id=bt.survey_batch_id left join login_table as l on l.emp_id=bt.emp_id");

			while (res.next()) {
				addresses.add(res.getString(1));
				System.out.println(res.getString(1));
			}
			if (addresses.size() > 0) {
				EmailSender es = new EmailSender();
				es.sendEmailforQuestions(addresses);
			}

			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Starting query to retrieve number of questions closed");
			Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
			res = stmt.executeQuery("select count(que_id) as question_ended from question where end_date='" + date + "' and survey_batch_id=1;");
			res.next();
			int questionsClosed = res.getInt("question_ended");

			if (questionsClosed > 0) {
				String companyName = rs.getString("comp_name");
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Started jobs for company: " + companyName);
				runRMethod("calculate_edge", companyId, companyName);
				runRMethod("update_neo", companyId, companyName);
				runRMethod("JobIndNwMetric", companyId, companyName);
				runRMethod("JobCubeNwMetric", companyId, companyName);
				runRMethod("JobDimensionNwMetric", companyId, companyName);
				runRMethod("JobIndividualMetric", companyId, companyName);
				runRMethod("JobTeamMetric", companyId, companyName);
				runRMethod("JobDimensionMetric", companyId, companyName);
				runRMethod("JobInitiativeMetric", companyId, companyName);
				runRMethod("JobAlert", companyId, companyName);
			}

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Unable to connect to the Company db", e);
		}

	}

	/**
	 * Runs the R functions
	 * @param rFunctionName - R function name
	 * @param companyId - Company ID
	 * @param companyName - Company name
	 * @throws Exception - if the R functions are not executed successfully
	 */
	public void runRMethod(String rFunctionName, int companyId, String companyName) throws Exception {
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Parameters for R function :  CompanyId : " + companyId);
		rCon = dch.getRConn();
		rCon.assign("CompanyId", new int[] { companyId });

		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug(rFunctionName + " method started");
		REXP status = rCon.parseAndEval("try(eval(" + rFunctionName + "(CompanyId)))");
		if (status.inherits("try-error")) {
			sendEmail(companyId, companyName, rFunctionName, status);
		} else {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the " + rFunctionName + " method ");
		}
		dch.releaseRcon();
	}

	/**
	 * Sends email
	 * @param companyId - Company ID
	 * @param companyName - Name of the company
	 * @param rFunctionName - R function name
	 * @param status - status after running the function
	 * @throws Exception - if the email is not sent successfully
	 */
	public void sendEmail(int companyId, String companyName, String rFunctionName, REXP status) throws Exception {
		EmailSender es = new EmailSender();
		es.sendEmail("Error in executing " + rFunctionName + "method for Company Id:" + companyId + "Company Name:" + companyName);
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error: " + status.asString());
		throw new Exception("Error: " + status.asString());
	}

}
