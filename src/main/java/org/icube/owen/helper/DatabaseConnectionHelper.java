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
	public Map<Integer, Connection> companySqlConnectionPool;
	public Map<Integer, Connection> companyNeoConnectionPool;
	public Map<Integer, String> companyImagePath;
	private RConnection rCon;

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
				throw new REXPMismatchException(loadRScript, "Error: " + loadRScript.asString());
			} else {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully loaded metric.r script");
			}

			companySqlConnectionPool = new HashMap<>();
			companyImagePath = new HashMap<>();
			companyNeoConnectionPool = new HashMap<>();
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

		System.out.println(today.getTime());
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

			for (int companyId : companySqlConnectionPool.keySet()) {
				companySqlConnectionPool.get(companyId).close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Connection to company sql for companyId : " + companyId + " is " + "closed!!!!");
				companyNeoConnectionPool.get(companyId).close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Connection to company neo4j for companyId : " + companyId + " is closed!!!!");
			}

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while attempting to close db connections", e);
		}
	}

	public void getCompanyConnection(int companyId) {
		try {
			String sqlUrl = "", sqlUserName = "", sqlPassword = "", neoUrl = "", neoUserName = "", neoPassword = "";
			// check if the sql connection pool contains the connection for the given company and if it is valid else call the db procedure for
			// connection details
			if (!companySqlConnectionPool.containsKey(companyId) || !companyNeoConnectionPool.containsKey(companyId)
					|| (companySqlConnectionPool.containsKey(companyId) && !companySqlConnectionPool.get(companyId).isValid(0))
					|| (companyNeoConnectionPool.containsKey(companyId) && !companyNeoConnectionPool.get(companyId).isValid(0))) {
				// get company details

				CallableStatement cstmt = masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();

				while (rs.next()) {
					sqlUrl = "jdbc:mysql://" + rs.getString("sql_server") + ":3306/" + rs.getString("comp_sql_dbname");
					sqlUserName = rs.getString("sql_user_id");
					sqlPassword = rs.getString("sql_password");
					companyImagePath.put(companyId, rs.getString("images_path"));
					neoUrl = rs.getString("neo_db_url");
					neoUserName = rs.getString("neo_user_name");
					neoPassword = rs.getString("neo_password");
				}
			}

			// company sql connection
			try {
				if (!companySqlConnectionPool.containsKey(companyId)
						|| (companySqlConnectionPool.containsKey(companyId) && !companySqlConnectionPool.get(companyId).isValid(0))) {

					Class.forName("com.mysql.jdbc.Driver");
					Connection conn = DriverManager.getConnection(sqlUrl, sqlUserName, sqlPassword);
					companySqlConnectionPool.put(companyId, conn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to company db with companyId : " + companyId);
				}
			} catch (SQLException | ClassNotFoundException e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
						"An error occurred while connecting to the sql db for companyId : " + companyId, e);
			}

			// company neo connection
			try {
				if (!companyNeoConnectionPool.containsKey(companyId)
						|| (companyNeoConnectionPool.containsKey(companyId) && !companyNeoConnectionPool.get(companyId).isValid(0))) {

					Class.forName("org.neo4j.jdbc.Driver");
					String path = "jdbc:neo4j://" + neoUrl + "/";
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Neo4j connection path : " + path);
					Properties p = new Properties();
					p.setProperty("user", neoUserName);
					p.setProperty("password", neoPassword);
					Connection compNeoConn = new Driver().connect(path, p);
					companyNeoConnectionPool.put(companyId, compNeoConn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to Neo4j with company ID : " + companyId);
				}
			} catch (SQLException e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
						"An error occurred while connecting to neo4j db for companyId : " + companyId, e);
			}
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
