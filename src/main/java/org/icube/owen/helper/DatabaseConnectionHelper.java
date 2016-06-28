package org.icube.owen.helper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.icube.owen.TheBorg;
import org.icube.owen.jobScheduler.CompanyDAO;
import org.neo4j.jdbc.Driver;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class DatabaseConnectionHelper extends TheBorg {

	public Connection masterCon;
	/*public Map<Integer, Connection> companySqlConnectionPool;
	public Map<Integer, Connection> companyNeoConnectionPool;
	public Map<Integer, String> companyImagePath;
	public Map<Integer, String> companySlackUrl;*/
	private RConnection rCon;
	public Map<Integer, CompanyConfig> companyConfigMap;

	private boolean rConInUse = false;
	Timer timer = new Timer();

	private final static String MASTER_URL = UtilHelper.getConfigProperty("master_sql_url");
	private final static String MASTER_USER = UtilHelper.getConfigProperty("master_sql_user");
	private final static String MASTER_PASSWORD = UtilHelper.getConfigProperty("master_sql_password");

	public DatabaseConnectionHelper() {

		// master sql connection
		try {
			Class.forName("com.mysql.jdbc.Driver");
			masterCon = (masterCon != null && !masterCon.isValid(0)) ? masterCon : DriverManager.getConnection(MASTER_URL, MASTER_USER,
					MASTER_PASSWORD);
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to MySql with master database");

		} catch (SQLException | ClassNotFoundException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while connecting to the master database on : " + MASTER_URL + " with user name : " + MASTER_USER, e);
		}

		// R connection
		try {
			rCon = (rCon != null && rCon.isConnected()) ? rCon : new RConnection();
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to R");
			String rScriptPath = UtilHelper.getConfigProperty("r_script_path");
			String workingDir = "setwd(\"" + rScriptPath + "\")";
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Trying to load the RScript file at " + rScriptPath);
			rCon.eval(workingDir);
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("R Path for eval " + s + ".... Loading now ...");

			REXP loadRScript = rCon.eval(s);
			if (loadRScript.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
						"An error occurred while trying to loading the R script : " + loadRScript.asString());
				releaseRcon();
				throw new REXPMismatchException(loadRScript, "Error: " + loadRScript.asString());
			} else {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully loaded metric.r script");
			}
			companyConfigMap = new HashMap<>();
		} catch (RserveException | REXPMismatchException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while trying to connect to R", e);
		}

		runScheduler();

	}

	public void runScheduler() {
		Calendar today = Calendar.getInstance();
		// set the start date to be 12:01 AM
		today.add(Calendar.DAY_OF_MONTH, 1);
		today.set(Calendar.HOUR_OF_DAY, 00);
		today.set(Calendar.MINUTE, 01);
		today.set(Calendar.SECOND, 0);

		// System.out.println(today.getTime());
		CompanyDAO cdao = new CompanyDAO();
		timer.scheduleAtFixedRate(cdao, today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
		// timer.scheduleAtFixedRate(cdao, today.getTime(), 300000);

	}

	@Override
	public void finalize() {
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Shutting down databases ...");
		try {
			if (!masterCon.isClosed()) {
				try {
					masterCon.close();
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to master database closed!!!!");
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class)
							.error("An error occurred while closing the mysql connection", e);
				}
			}

			if (rCon.isConnected()) {
				rCon.close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to R closed!!!!");
			}

			for (int companyId : companyConfigMap.keySet()) {
				companyConfigMap.get(companyId).getSqlConnection().close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Connection to company sql for companyId : " + companyId + " is " + "closed!!!!");
				companyConfigMap.get(companyId).getNeoConnection().close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Connection to company neo4j for companyId : " + companyId + " is closed!!!!");
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while attempting to close db connections", e);
		}
	}

	/**
	 * Retrieves the company database connections
	 * @param companyId - The ID of the company for which the connections are required
	 */
	public void getCompanyConnection(int companyId) {
		try {
			CompanyConfig compCon = new CompanyConfig();
			boolean compConChanged = false;
			String sqlUrl = "", sqlUserName = "", sqlPassword = "", neoUrl = "", neoUserName = "", neoPassword = "";
			// check if the sql connection pool contains the connection for the given company and if it is valid else call the db procedure for
			// connection details
			if (!companyConfigMap.containsKey(companyId)
					|| (companyConfigMap.containsKey(companyId) && !companyConfigMap.get(companyId).getSqlConnection().isValid(0))
					|| (companyConfigMap.containsKey(companyId) && !companyConfigMap.get(companyId).getNeoConnection().isValid(0))) {
				// get company details

				CallableStatement cstmt = masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();

				while (rs.next()) {

					// fill the company config object
					sqlUrl = "jdbc:mysql://" + rs.getString("sql_server") + ":3306/" + rs.getString("comp_sql_dbname");
					sqlUserName = rs.getString("sql_user_id");
					sqlPassword = rs.getString("sql_password");
					compCon.setImagePath(rs.getString("images_path"));
					neoUrl = rs.getString("neo_db_url");
					neoUserName = rs.getString("neo_user_name");
					neoPassword = rs.getString("neo_password");
					compCon.setSlackUrl(rs.getString("slack_url"));
					compCon.setSendEmail(rs.getBoolean("email_notification"));
					compCon.setSendSlack(rs.getBoolean("slack_notification"));
					compCon.setDisplayNetworkName(rs.getBoolean("ntw_name"));
					compCon.setSmartList(rs.getString("smart_list"));

					// retrieve the neo and sql connections only if the company is already present in the company config map
					if (companyConfigMap.containsKey(companyId)) {
						compCon.setSqlConnection(companyConfigMap.get(companyId).getSqlConnection());
						compCon.setNeoConnection(companyConfigMap.get(companyId).getNeoConnection());
					}
					compConChanged = true;
				}
			}

			// company sql connection
			try {
				if (!companyConfigMap.containsKey(companyId)
						|| (companyConfigMap.containsKey(companyId) && !companyConfigMap.get(companyId).getSqlConnection().isValid(0))) {

					Class.forName("com.mysql.jdbc.Driver");
					Connection conn = DriverManager.getConnection(sqlUrl, sqlUserName, sqlPassword);
					compCon.setSqlConnection(conn);
					// retrieve the neo connection if the company is already in the company config map
					if (companyConfigMap.containsKey(companyId)) {
						compCon.setNeoConnection(companyConfigMap.get(companyId).getNeoConnection());
					}
					compConChanged = true;
					// companySqlConnectionPool.put(companyId, conn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to company db with companyId : " + companyId);
				}
			} catch (SQLException | ClassNotFoundException e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
						"An error occurred while connecting to the sql db for companyId : " + companyId, e);
			}

			// company neo connection
			try {
				if (!companyConfigMap.containsKey(companyId)
						|| (companyConfigMap.containsKey(companyId) && !companyConfigMap.get(companyId).getNeoConnection().isValid(0))) {

					Class.forName("org.neo4j.jdbc.Driver");
					String path = "jdbc:neo4j://" + neoUrl + "/";
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Neo4j connection path : " + path);
					Properties p = new Properties();
					p.setProperty("user", neoUserName);
					p.setProperty("password", neoPassword);
					Connection compNeoConn = new Driver().connect(path, p);
					compCon.setNeoConnection(compNeoConn);
					// retrieve the sql connection if the company is already in the company config map
					if (companyConfigMap.containsKey(companyId)) {
						compCon.setSqlConnection(companyConfigMap.get(companyId).getSqlConnection());
					}
					compConChanged = true;
					// companyNeoConnectionPool.put(companyId, compNeoConn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to Neo4j with company ID : " + companyId);
				}
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
						"An error occurred while connecting to neo4j db for companyId : " + companyId, e);
			}
			// add the company config object to the company config map
			if (compConChanged)
				companyConfigMap.put(companyId, compCon);
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while retrieving connection details for companyId : " + companyId, e);
		}

	}

	public RConnection getRConn() {
		while (rConInUse)
			try {
				Thread.sleep(100);
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Waiting for R connection");
			} catch (InterruptedException e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while trying to get the R connection", e);

			}
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("RConnection provided...");
		rConInUse = true;
		return rCon;

	}

	public void releaseRcon() {
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Releasing R connection");
		rConInUse = false;
	}
}
