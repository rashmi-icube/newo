package org.icube.owen.jobScheduler;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.helper.UtilHelper;
import org.icube.owen.slack.SlackIntegration;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class CompanyDAO extends TimerTask {

	private RConnection rCon;
	private DatabaseConnectionHelper dch;
	Map<Integer, List<Map<String, String>>> schedulerJobStatusMap = new HashMap<>();
	private String loginUrl = UtilHelper.getConfigProperty("login_page_url");
	boolean jobStatus = true;

	@Override
	public void run() {

		runSchedulerJob();
		EmailSender es = new EmailSender();
		String subject;
		try {
			// check if all the jobs have run successfully or not
			if (jobStatus == true) {
				subject = "Scheduler job executed successfully";
			} else {
				subject = "Scheduler job failed";
			}
			es.sendEmail(schedulerJobStatusMap, subject);
		} catch (MessagingException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error in sending mail", e);
		}
	}

	/**
	 * Retrieves the details of the company (ID,name,username,password,sql server,sql user id)
	 */
	public void runSchedulerJob() {

		try {
			// get company connections
			dch = ObjectFactory.getDBHelper();
			ResultSet companyDetails = null;
			// startDbConnection();

			Statement stmt = null;
			stmt = dch.masterCon.createStatement();
			companyDetails = stmt
					.executeQuery("Select comp_name, comp_id, comp_sql_dbname, sql_server, sql_user_id, sql_password from company_master where comp_status='Active'");

			// connect to r
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("R Connection Available : " + rCon.isConnected());
			while (companyDetails.next()) {
				Map<String, String> jobStatusMap = new HashMap<>();
				List<Map<String, String>> jobStatusMapList = new ArrayList<>();
				// run JobInitStatus
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("JobInitStatus method started");
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug(
						"Parameters for R function :  CompanyId : " + companyDetails.getInt("comp_id"));

				rCon.assign("CompanyId", new int[] { companyDetails.getInt("comp_id") });
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Calling the actual function in R Script JobInitStatus");
				REXP status = rCon.parseAndEval("try(eval(JobInitStatus(CompanyId)))");
				if (status.inherits("try-error")) {
					// add to map of status
					jobStatusMap.put("JobInitStatus", "failed :" + status.asString());
					jobStatus = false;
				} else {
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the JobInitStatus method ");
					// add to map of status
					jobStatusMap.put("JobInitStatus", "Pass");
				}
				jobStatusMapList.add(jobStatusMap);
				schedulerJobStatusMap.put(companyDetails.getInt("comp_id"), jobStatusMapList);
				dch.releaseRcon();
				runCompanyMetricJobs(companyDetails);
				runNewQuestionJob(companyDetails.getInt("comp_id"));
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Failed to get the db connection details", e);

		} finally {
			dch.releaseRcon();
		}
	}

	/**
	 * Connects to the company database and execute the methods
	 * @param rs - ResultSet containing the details of the company
	 */

	public void runCompanyMetricJobs(ResultSet rs) {

		try {
			int companyId = rs.getInt("comp_id");
			String companyName = rs.getString("comp_name");
			dch.getCompanyConnection(companyId);
			Statement stmt = dch.companyConnectionMap.get(companyId).getSqlConnection().createStatement();
			org.apache.log4j.Logger.getLogger(CompanyDAO.class)
					.debug("Successfully connected to company db with companyId : " + rs.getInt("comp_id"));

			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Starting query to retrieve number of questions closed");
			Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
			ResultSet res = stmt.executeQuery("select count(que_id) as question_ended from question where end_date='" + date
					+ "' and survey_batch_id=1;");
			res.next();
			int questionsClosed = res.getInt("question_ended");
			if (questionsClosed > 0) {
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Started jobs for company: " + companyName);
				try {
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
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Unable to execute Scheduler jobs ", e);
				}

			} else {
				Map<String, String> jobStatusMap = new HashMap<>();
				jobStatusMap.put("Company Metrics Job", "No questions were closed");
				schedulerJobStatusMap.get(companyId).add(jobStatusMap);
			}

		} catch (Exception e) {
			// add to map of status
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Unable to execute Scheduler jobs ", e);
		} finally {
			dch.releaseRcon();
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

		Map<String, String> jobStatusMap = new HashMap<>();
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Parameters for R function :  CompanyId : " + companyId);
		rCon = dch.getRConn();
		rCon.assign("CompanyId", new int[] { companyId });
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug(rFunctionName + " method started");

		REXP status = rCon.parseAndEval("try(eval(" + rFunctionName + "(CompanyId)))");
		if (status.inherits("try-error")) {
			dch.releaseRcon();
			jobStatus = false;
			jobStatusMap.put(rFunctionName, "Failed : " + status.asString());
			schedulerJobStatusMap.get(companyId).add(jobStatusMap);
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error: " + status.asString());
			throw new Exception("Error: " + status.asString());
		} else {
			jobStatusMap.put(rFunctionName, "Pass");
			schedulerJobStatusMap.get(companyId).add(jobStatusMap);
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the " + rFunctionName + " method ");
		}
		dch.releaseRcon();
	}

	/**
	 * Retrieves the new questions to be answered and sends notification mails
	 * @param companyId - Company ID
	 * @throws SQLException - if error in retrieving data from database
	 */
	public void runNewQuestionJob(int companyId) throws SQLException {
		ArrayList<String> addresses = new ArrayList<String>();
		Map<String, String> jobStatusMapForEmail = new HashMap<>();
		Map<String, String> jobStatusMapForSlack = new HashMap<>();
		Map<String, String> jobStatusMap = new HashMap<>();
		try {
			Statement stmt = dch.companyConnectionMap.get(companyId).getSqlConnection().createStatement();
			// check if new emails have to be sent for the specific company

			ResultSet res = stmt
					.executeQuery("select distinct(l.login_id) as email_id from (select Distinct(survey_batch_id) as survey_batch_id from question where date(start_date)=CURDATE()) as b join batch_target as bt on b.survey_batch_id=bt.survey_batch_id left join login_table as l on l.emp_id=bt.emp_id where l.status='active'");

			while (res.next()) {
				addresses.add(res.getString(1));
				System.out.println(res.getString(1));
			}
			// in case of new questions send email
			if (addresses.size() > 0) {
				
				CallableStatement cstmt = dch.masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();
				while(rs.next()){
					dch.setCompanyConfigDetails(companyId, dch.companyConfigMap.get(companyId), rs);
				}
                
				if (dch.companyConfigMap.get(companyId).isSendEmail()) {
					EmailSender es = new EmailSender();
					es.sendEmailforQuestions(companyId, addresses);
					jobStatusMapForEmail.put("NewQuestionJobEmail", "Total emails sent : " + addresses.size());
				} else {
					jobStatusMapForEmail.put("NewQuestionJobEmail", "New question emails are disabled for the company");
				}
				schedulerJobStatusMap.get(companyId).add(jobStatusMapForEmail);

				if (dch.companyConfigMap.get(companyId).isSendSlack()) {
					SlackIntegration sl = new SlackIntegration();
					sl.sendMessage(companyId, "You have new questions to answer. Please login to answer : " + loginUrl + "");
					jobStatusMapForSlack.put("NewQuestionJobSlack", "Slack message sent for company");
				} else {
					jobStatusMapForSlack.put("NewQuestionJobSlack", "New question slack is disabled for the company");
				}
				schedulerJobStatusMap.get(companyId).add(jobStatusMapForSlack);

			} else {
				jobStatusMap.put("NewQuestionJob", "No new questions");
				schedulerJobStatusMap.get(companyId).add(jobStatusMap);
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error in executing runNewQuestionJob function", e);
			jobStatus = false;
		}

	}

}
