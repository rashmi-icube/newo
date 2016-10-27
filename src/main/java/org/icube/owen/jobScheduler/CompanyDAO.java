package org.icube.owen.jobScheduler;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.icube.owen.ObjectFactory;
import org.icube.owen.helper.CompanyConfig;
import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.slack.SlackIntegration;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

public class CompanyDAO extends TimerTask {

	private RConnection rCon;
	private DatabaseConnectionHelper dch;
	Map<Integer, Map<String, String>> schedulerJobStatusMap = new HashMap<>();
	boolean jobStatus = true;

	@Override
	public void run() {

		runSchedulerJob();
		EmailSender es = new EmailSender();
		String subject;
		try {
			// check if all the jobs have run successfully or not and accordingly set the subject of the mail
			if (jobStatus == true) {
				subject = "Scheduler job executed successfully";
			} else {
				subject = "Scheduler job failed";
				jobStatus = true;
			}

			// send the scheduler mail
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
			Statement stmt = dch.masterCon.createStatement();
			ResultSet companyDetails = stmt
					.executeQuery("Select comp_name, comp_id, comp_sql_dbname, sql_server, sql_user_id, sql_password from company_master where comp_status='Active'");

			// connect to r
			RConnection rCon = dch.getRConn();
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("R Connection Available : " + rCon.isConnected());

			// loop through every active company from the database
			while (companyDetails.next()) {

				int companyId = companyDetails.getInt("comp_id");
				dch.getCompanyConnection(companyId);
				CompanyConfig compConfig = dch.companyConfigMap.get(companyId);

				// check if run jobs is enabled for the company or not
				if (compConfig.isRunJobs()) {
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).info("HashMap created!!!");
					Map<String, String> jobStatusMap = new HashMap<>();

					// run JobInitStatus if run jobs is enabled for the company
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("JobInitStatus method started");
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Parameters for R function :  CompanyId : " + companyId);

					rCon.assign("CompanyId", new int[] { companyId });
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Calling the actual function in R Script JobInitStatus");
					REXP status = rCon.parseAndEval("try(eval(JobInitStatus(CompanyId)))");
					if (status.inherits("try-error")) {

						// add the error to map of status
						jobStatusMap.put("JobInitStatus", "failed :" + status.asString());
						jobStatus = false;
					} else {
						org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the JobInitStatus method ");

						// add the job success status to map of status
						jobStatusMap.put("JobInitStatus", "Pass");
					}

					// add the jobStatusMapList to the schedulerJobStatusMap for the company
					schedulerJobStatusMap.put(companyId, jobStatusMap);
					dch.releaseRcon();

					// call runCompanyMetricJobs function to run the 10 scheduler jobs
					runCompanyMetricJobs(companyDetails, jobStatusMap);

					// call function to run the new question jobs to find if there any any new questions
					runNewQuestionJob(companyId, jobStatusMap);
				}
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Failed to get the db connection details", e);

		} finally {
			dch.releaseRcon();
		}
	}

	/**
	 * Connects to the company database and execute the scheduler job methods
	 * @param rs - ResultSet containing the details of the company
	 * @param jobStatusMap 
	 */

	public void runCompanyMetricJobs(ResultSet rs, Map<String, String> jobStatusMap) {

		try {

			// get the company connection details and connect to the company database
			int companyId = rs.getInt("comp_id");
			String companyName = rs.getString("comp_name");
			dch.getCompanyConnection(companyId);
			Statement stmt = dch.companyConnectionMap.get(companyId).getSqlConnection().createStatement();
			org.apache.log4j.Logger.getLogger(CompanyDAO.class)
					.debug("Successfully connected to company db with companyId : " + rs.getInt("comp_id"));

			// find the number of questions closed
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Starting query to retrieve number of questions closed");
			Date date = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
			ResultSet res = stmt.executeQuery("select count(que_id) as question_ended from question where end_date='" + date
					+ "' and survey_batch_id=1;");
			res.next();
			int questionsClosed = res.getInt("question_ended");

			// if there are questions closed, only then execute the jobs
			if (questionsClosed > 0) {
				org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Started jobs for company: " + companyName);
				try {
					runRMethod("calculate_edge", companyId, companyName, jobStatusMap);
					runRMethod("update_neo", companyId, companyName, jobStatusMap);
					runRMethod("JobIndNwMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobCubeNwMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobDimensionNwMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobIndividualMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobTeamMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobDimensionMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobInitiativeMetric", companyId, companyName, jobStatusMap);
					runRMethod("JobAlert", companyId, companyName, jobStatusMap);
				} catch (Exception e) {
					dch.releaseRcon();
					org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Unable to execute Scheduler jobs ", e);
				}

			} else {

				// update the jobStatusMap
				jobStatusMap.put("Company Metrics Job", "No questions were closed");
				schedulerJobStatusMap.put(companyId, jobStatusMap);
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
	 * @param jobStatusMap 
	 * @throws Exception - if the R functions are not executed successfully
	 */
	public void runRMethod(String rFunctionName, int companyId, String companyName, Map<String, String> jobStatusMap) throws Exception {
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Parameters for R function :  CompanyId : " + companyId);
		rCon = dch.getRConn();
		rCon.assign("CompanyId", new int[] { companyId });
		org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug(rFunctionName + " method started");

		// execute the R function
		REXP status = rCon.parseAndEval("try(eval(" + rFunctionName + "(CompanyId)))");

		// if job fails, update the job fail error message
		if (status.inherits("try-error")) {
			dch.releaseRcon();
			jobStatus = false;
			jobStatusMap.put(rFunctionName, "Failed : " + status.asString());
			schedulerJobStatusMap.put(companyId, jobStatusMap);
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error: " + status.asString());
			throw new Exception("Error: " + status.asString());
		} else {

			// if job is successfully executed, update the job success status
			jobStatusMap.put(rFunctionName, "Pass");
			schedulerJobStatusMap.put(companyId, jobStatusMap);
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).debug("Successfully executed the " + rFunctionName + " method ");
		}
		dch.releaseRcon();
	}

	/**
	 * Retrieves the new questions to be answered and sends notification mails
	 * @param companyId - Company ID
	 * @param jobStatusMap 
	 * @throws SQLException - if error in retrieving data from database
	 */
	public void runNewQuestionJob(int companyId, Map<String, String> jobStatusMap) throws SQLException {
		ArrayList<String> addresses = new ArrayList<String>();
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
				// if Send email is enabled for the company, send the new question emails
				if (dch.companyConfigMap.get(companyId).isSendEmail()) {
					EmailSender es = new EmailSender();
					es.sendEmailforQuestions(companyId, addresses);

					// update the count of number of new question emails sent
					jobStatusMap.put("NewQuestionJobEmail", "Total emails sent : " + addresses.size());
				} else {
					jobStatusMap.put("NewQuestionJobEmail", "New question emails are disabled for the company");
				}

				// add the jobStatusMapForEmail to the schedulerJobStatusMap
				schedulerJobStatusMap.put(companyId, jobStatusMap);
				// schedulerJobStatusMap.get(companyId).add(jobStatusMapForEmail);

				// if send slack is enabled for the company, then send slack message
				if (dch.companyConfigMap.get(companyId).isSendSlack()) {
					SlackIntegration sl = new SlackIntegration();
					sl.sendMessage(companyId,
							"You have new questions to answer\nPlease login to answer\n<http://engage.owenanalytics.com|engage.owenanalytics.com>");
					jobStatusMap.put("NewQuestionJobSlack", "Slack message sent for company");
				} else {
					jobStatusMap.put("NewQuestionJobSlack", "New question slack is disabled for the company");
				}
				// schedulerJobStatusMap.get(companyId).add(jobStatusMapForSlack);
				schedulerJobStatusMap.put(companyId, jobStatusMap);

			} else {

				// if no new questions are there for the company, just update the jobStatusMap
				jobStatusMap.put("NewQuestionJob", "No new questions");
				// schedulerJobStatusMap.get(companyId).add(jobStatusMap);
				schedulerJobStatusMap.put(companyId, jobStatusMap);
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(CompanyDAO.class).error("Error in executing runNewQuestionJob function", e);
			jobStatus = false;
		}

	}

}
