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
	private RConnection rCon;
	public Map<Integer, CompanyConfig> companyConfigMap;
	public Map<Integer, CompanyConnection> companyConnectionMap;

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
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).info("HashMap created!!!");
			companyConfigMap = new HashMap<>();
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).info("HashMap created!!!");
			companyConnectionMap = new HashMap<>();
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

			for (int companyId : companyConnectionMap.keySet()) {
				companyConnectionMap.get(companyId).getSqlConnection().close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Connection to company sql for companyId : " + companyId + " is " + "closed!!!!");
				companyConnectionMap.get(companyId).getNeoConnection().close();
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
			CompanyConfig compConfig = null;
			CompanyConnection compConnection = new CompanyConnection();
			boolean compConnectionChanged = false;
			if (!companyConnectionMap.containsKey(companyId)) {

				// get company details
				CallableStatement cstmt = masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();

				while (rs.next()) {
					compConfig = setCompanyConfigDetails(companyId, rs);
					companyConfigMap.put(companyId, compConfig);
				}

				// company sql connection
				compConnection.setSqlConnection(createSqlConnection(companyId, compConfig));

				// company neo connection
				compConnection.setNeoConnection(createNeoConnection(companyId, compConfig));

				compConnectionChanged = true;
			} else {
				// check if SQL connection is valid; if not refresh the connection
				if (!companyConnectionMap.get(companyId).getSqlConnection().isValid(0)) {
					compConnection.setSqlConnection(createSqlConnection(companyId, companyConfigMap.get(companyId)));
					compConnectionChanged = true;
				}

				// check if Neo connection is valid; if not refresh the connection
				if (!companyConnectionMap.get(companyId).getNeoConnection().isValid(0)) {
					compConnection.setNeoConnection(createNeoConnection(companyId, companyConfigMap.get(companyId)));
					compConnectionChanged = true;
				}
			}

			if (compConnectionChanged) {
				companyConnectionMap.put(companyId, compConnection);
			}
		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while retrieving connection details for companyId : " + companyId, e);
		}

	}

	private Connection createNeoConnection(int companyId, CompanyConfig compConfig) {
		Connection conn = null;
		try {
			String neoUrl = compConfig.getNeoUrl();
			String neoUserName = compConfig.getNeoUserName();
			String neoPassword = compConfig.getNeoPassword();
			Class.forName("org.neo4j.jdbc.Driver");
			String path = "jdbc:neo4j://" + neoUrl + "/";
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Neo4j connection path : " + path);
			Properties p = new Properties();
			p.setProperty("user", neoUserName);
			p.setProperty("password", neoPassword);
			conn = new Driver().connect(path, p);
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to Neo4j with company ID : " + companyId);
		} catch (SQLException | ClassNotFoundException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while connecting to neo4j db for companyId : " + companyId, e);
		}
		return conn;
	}

	private Connection createSqlConnection(int companyId, CompanyConfig compConfig) {
		Connection conn = null;
		try {
			String sqlUrl = compConfig.getSqlUrl();
			String sqlUserName = compConfig.getSqlUserName();
			String sqlPassword = compConfig.getSqlPassword();
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(sqlUrl, sqlUserName, sqlPassword);
		} catch (SQLException | ClassNotFoundException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while connecting to the sql db for companyId : " + companyId, e);
		}
		return conn;
	}

	/**
	 * @param companyId - company ID
	 * @param rs - resultset containing the company config details
	 * @return updated company config object
	 */
	public CompanyConfig setCompanyConfigDetails(int companyId, ResultSet rs) {
		CompanyConfig compConfig = new CompanyConfig();
		try {
			compConfig.setImagePath(rs.getString("images_path"));
			compConfig.setSlackUrl(rs.getString("slack_url"));
			compConfig.setSendEmail(rs.getBoolean("email_notification"));
			compConfig.setSendSlack(rs.getBoolean("slack_notification"));
			compConfig.setDisplayNetworkName(rs.getBoolean("ntw_name"));
			compConfig.setSmartList(rs.getString("smart_list"));
			compConfig.setStatus(rs.getString("comp_status"));
			compConfig.setNeoUrl(rs.getString("neo_db_url"));
			compConfig.setNeoUserName(rs.getString("neo_user_name"));
			compConfig.setNeoPassword(rs.getString("neo_password"));
			compConfig.setSqlUrl("jdbc:mysql://" + rs.getString("sql_server") + ":3306/" + rs.getString("comp_sql_dbname"));
			compConfig.setSqlUserName(rs.getString("sql_user_id"));
			compConfig.setSqlPassword(rs.getString("sql_password"));
			compConfig.setRunJobs(rs.getBoolean("jobs"));
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"Unable to retrieve the company config details from the resultset for companyId : " + companyId, e);
		}
		return compConfig;
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
