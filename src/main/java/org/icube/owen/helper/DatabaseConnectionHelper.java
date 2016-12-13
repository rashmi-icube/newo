package org.icube.owen.helper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.icube.owen.TheBorg;
import org.icube.owen.jobScheduler.CompanyDAO;
import org.neo4j.jdbc.Driver;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class DatabaseConnectionHelper extends TheBorg {

	// public Connection masterCon;
	public DataSource masterDS;
	private RConnection rCon;
	public Map<Integer, CompanyConfig> companyConfigMap;
	public Map<Integer, CompanyConnection> companyConnectionMap;

	private boolean rConInUse = false;
	Timer timer = new Timer();

	private final static String MASTER_URL = UtilHelper.getConfigProperty("master_sql_url");
	private final static String MASTER_USER = UtilHelper.getConfigProperty("master_sql_user");
	private final static String MASTER_PASSWORD = UtilHelper.getConfigProperty("master_sql_password");

	public DatabaseConnectionHelper() {

		PoolProperties p = new PoolProperties();
		p.setUrl(MASTER_URL);
		p.setDriverClassName("com.mysql.jdbc.Driver");
		p.setUsername(MASTER_USER);
		p.setPassword(MASTER_PASSWORD);
		p.setJmxEnabled(true);
		p.setTestWhileIdle(true); // this was false : RM
		p.setTestOnBorrow(true);
		p.setValidationQuery("SELECT 1");
		p.setTestOnReturn(false);
		p.setValidationInterval(30000);
		p.setTimeBetweenEvictionRunsMillis(30000);
		p.setMaxActive(100);
		p.setInitialSize(10);
		p.setMaxWait(10000);
		p.setRemoveAbandonedTimeout(60);
		p.setMinEvictableIdleTimeMillis(30000);
		p.setMinIdle(10);
		p.setLogAbandoned(true);
		p.setRemoveAbandoned(true);
		p.setConnectionProperties("connectionTimeout=\"300000\"");
		p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
				+ "org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer");
		masterDS = new DataSource();
		masterDS.setPoolProperties(p);

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

		// runScheduler();

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
			if (!masterDS.getConnection().isClosed()) {
				try {
					masterDS.getConnection().close();
					masterDS.close();
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
				companyConnectionMap.get(companyId).getDataSource().getConnection().close();
				companyConnectionMap.get(companyId).getDataSource().close();
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
	public void refreshCompanyConnection(int companyId) {
		try {
			CompanyConfig compConfig = null;
			CompanyConnection compConnection = new CompanyConnection();
			if (!companyConnectionMap.containsKey(companyId)) {
				// get company details
				try (CallableStatement cstmt = masterDS.getConnection().prepareCall("{call getCompanyConfig(?)}")) {
					cstmt.setInt(1, companyId);
					try (ResultSet rs = cstmt.executeQuery()) {
						while (rs.next()) {
							compConfig = setCompanyConfigDetails(companyId, rs);
							companyConfigMap.put(companyId, compConfig);
						}
					}
				}

				// company sql connection
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Creating a brand new Connection to company sql for companyId : " + companyId);
				DataSource ds = createDataSource(compConfig);
				compConnection.setDataSource(ds);
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug(
						"Created new Connection to company sql for companyId : " + companyId);

				// company neo connection
				compConnection.setNeoConnection(createNeoConnection(companyId, compConfig));
				companyConnectionMap.put(companyId, compConnection);
			} else {
				// check if Neo connection is valid; if not refresh the connection
				if (!companyConnectionMap.get(companyId).getNeoConnection().isValid(0)) {
					compConnection = companyConnectionMap.get(companyId);
					compConnection.setNeoConnection(createNeoConnection(companyId, companyConfigMap.get(companyId)));
					companyConnectionMap.put(companyId, compConnection);
				}
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

	private DataSource createDataSource(CompanyConfig compConfig) {
		PoolProperties p = new PoolProperties();
		p.setUrl(compConfig.getSqlUrl());
		p.setDriverClassName("com.mysql.jdbc.Driver");
		p.setUsername(compConfig.getSqlUserName());
		p.setPassword(compConfig.getSqlPassword());
		p.setJmxEnabled(true);
		p.setTestWhileIdle(false);
		p.setTestOnBorrow(true);
		p.setValidationQuery("SELECT 1");
		p.setTestOnReturn(false);
		p.setValidationInterval(Integer.valueOf(UtilHelper.getConfigProperty("validationInterval")));
		p.setTimeBetweenEvictionRunsMillis(Integer.valueOf(UtilHelper.getConfigProperty("timeBetweenEvictionRunsMillis")));
		p.setMaxActive(Integer.valueOf(UtilHelper.getConfigProperty("maxActive")));
		p.setInitialSize(Integer.valueOf(UtilHelper.getConfigProperty("initialSize")));
		p.setMaxWait(Integer.valueOf(UtilHelper.getConfigProperty("maxWait")));
		p.setRemoveAbandonedTimeout(Integer.valueOf(UtilHelper.getConfigProperty("removeAbandonedTimeout")));
		p.setMinEvictableIdleTimeMillis(Integer.valueOf(UtilHelper.getConfigProperty("minEvictableIdleTimeMillis")));
		p.setMinIdle(Integer.valueOf(UtilHelper.getConfigProperty("minIdle")));
		p.setLogAbandoned(true);
		p.setConnectionProperties("connectionTimeout=\"300000\"");
		p.setRemoveAbandoned(true);
		p.setMaxIdle(Integer.valueOf(UtilHelper.getConfigProperty("maxIdle")));
		p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
				+ "org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer");
		DataSource datasource = new DataSource();
		datasource.setPoolProperties(p);
		return datasource;
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
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Entering the get R connection function");
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
