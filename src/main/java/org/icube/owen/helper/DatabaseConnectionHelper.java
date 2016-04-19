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

	// Fermion Server
	/*private final static String mysqlurl = "jdbc:mysql://192.168.1.6:3306/owen";
	private final static String user = "icube";
	private final static String password = "icube123";

	private final static String MASTER_URL = "jdbc:mysql://192.168.1.6:3306/owen_master";
	private final static String MASTER_USER = "icube";
	private final static String MASTER_PASSWORD = "icube123";*/

	private final static String MASTER_URL = UtilHelper.getConfigProperty("master_sql_url");
	private final static String MASTER_USER = UtilHelper.getConfigProperty("master_sql_user");
	private final static String MASTER_PASSWORD = UtilHelper.getConfigProperty("master_sql_password");

	public DatabaseConnectionHelper() {

		// master sql connection
		try {
			Class.forName("com.mysql.jdbc.Driver");
			masterCon = (masterCon != null && !masterCon.isValid(0)) ? masterCon : DriverManager.getConnection(MASTER_URL, MASTER_USER,
					MASTER_PASSWORD);
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to MySql with owen master database");

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred. Maybe user/password is invalid", e);
		} catch (ClassNotFoundException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("JDBC Class not found", e);
		}

		// R connection
		try {
			rCon = (rCon != null && rCon.isConnected()) ? rCon : new RConnection();
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to R");
			String rScriptPath = UtilHelper.getConfigProperty("r_script_path");
			// Fermion Server
			// String rScriptPath = "C:\\\\Users\\\\fermion10\\\\Documents\\\\Neo4j\\\\scripts";
			String workingDir = "setwd(\"" + rScriptPath + "\")";
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Trying to load the RScript file at " + rScriptPath);
			rCon.eval(workingDir);
			String s = "source(\"metric.r\")";
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("R Path for eval " + s + ".... Loading now ...");

			REXP loadRScript = rCon.eval(s);
			if (loadRScript.inherits("try-error")) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("Error: " + loadRScript.asString());
				throw new REXPMismatchException(loadRScript, "Error: " + loadRScript.asString());
			} else {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully loaded metric.r script");
			}

			companySqlConnectionPool = new HashMap<>();
			companyImagePath = new HashMap<>();
			companyNeoConnectionPool = new HashMap<>();
		} catch (RserveException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while trying to connect to R", e);
		} catch (REXPMismatchException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while trying to loading the R script", e);
		}

		runScheduler();

	}

	public void runScheduler() {
		Calendar today = Calendar.getInstance();
		// set the start date to be 12:01 AM
		// today.add(Calendar.DAY_OF_MONTH, 1);
		today.set(Calendar.HOUR_OF_DAY, 11);
		today.set(Calendar.MINUTE, 30);
		today.set(Calendar.SECOND, 0);

		try {
			System.out.println(today.getTime());
			CompanyDAO cdao = new CompanyDAO();
			timer.scheduleAtFixedRate(cdao, today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
			// timer.scheduleAtFixedRate(cdao, today.getTime(), 300000);

		} catch (Exception e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("Unable to execute the scheduled task");
			e.printStackTrace();
		}

	}

	@Override
	public void finalize() {
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Shutting down database ...");
		try {
			if (!masterCon.isClosed()) {
				try {
					masterCon.close();
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to mySql closed!!!!");
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
		if (!companySqlConnectionPool.containsKey(companyId) || !companyNeoConnectionPool.containsKey(companyId)) {
			// get company details
			String sqlUrl = "", sqlUserName = "", sqlPassword = "", neoUrl = "";
			try {
				CallableStatement cstmt = masterCon.prepareCall("{call getCompanyConfig(?)}");
				cstmt.setInt(1, companyId);
				ResultSet rs = cstmt.executeQuery();

				while (rs.next()) {
					sqlUrl = "jdbc:mysql://" + rs.getString("sql_server") + ":3306/" + rs.getString("comp_sql_dbname");
					sqlUserName = rs.getString("sql_user_id");
					sqlPassword = rs.getString("sql_password");
					companyImagePath.put(companyId, rs.getString("images_path"));
					neoUrl = rs.getString("neo_db_url");
				}
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while getting company configuration", e);
			}

			// company sql connection
			if (!companySqlConnectionPool.containsKey(companyId)) {
				try {
					Class.forName("com.mysql.jdbc.Driver");
					Connection conn = DriverManager.getConnection(sqlUrl, sqlUserName, sqlPassword);
					companySqlConnectionPool.put(companyId, conn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to company db with companyId : " + companyId);
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred. Maybe user/password is invalid", e);
				} catch (ClassNotFoundException e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("JDBC Class not found", e);
				}
			}

			// company neo connection
			if (!companyNeoConnectionPool.containsKey(companyId)) {

				try {
					Class.forName("org.neo4j.jdbc.Driver");
					String path = "jdbc:neo4j://" + neoUrl + "/";
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Neo4j connection path : " + path);
					Connection compNeoConn = new Driver().connect(path, new Properties());

					companyNeoConnectionPool.put(companyId, compNeoConn);
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
							"Successfully connected to Neo4j with company ID : " + companyId);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while connecting to neo4j server", e);
				}
			}
		}
	}

	public RConnection getRConn() {
		while (rConInUse)
			try {
				Thread.sleep(100);
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Waiting for R connection");
			} catch (InterruptedException e) {
				e.printStackTrace();
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
