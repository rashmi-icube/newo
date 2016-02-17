package org.icube.owen.helper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.icube.owen.TheBorg;
import org.icube.owen.metrics.MetricsList;
import org.neo4j.jdbc.Driver;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class DatabaseConnectionHelper extends TheBorg {

	public Connection mysqlCon;
	public Connection neo4jCon;
	public RConnection rCon;
	private final static String mysqlurl = "jdbc:mysql://192.168.1.6:3306/owen";
	private final static String user = "icube";
	private final static String password = "icube123";

	/*private final static String mysqlurl = "jdbc:mysql://localhost:3306/owen";
	private final static String user = "root";
	private final static String password = "root";*/

	public DatabaseConnectionHelper() {

		// mysql connection
		try {
			Class.forName("com.mysql.jdbc.Driver");
			mysqlCon = DriverManager.getConnection(mysqlurl, user, password);
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to MySql with owen database");

		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred. Maybe user/password is invalid", e);
		} catch (ClassNotFoundException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("JDBC Class not found", e);
		}

		// neo4j connection
		try {
			CallableStatement cstmt = mysqlCon.prepareCall("{call getNeoConnectionUrl(?)}");
			cstmt.setInt(1, 1);
			ResultSet rs = cstmt.executeQuery();
			String url = "";
			int port = 0;
			while (rs.next()) {
				url = rs.getString("db_url");
				port = rs.getInt("port");
			}

			try {
				Class.forName("org.neo4j.jdbc.Driver");
				String path = "jdbc:neo4j://" + url + ":" + port + "/";
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Neo4j connection path : " + path);
				neo4jCon = new Driver().connect(path, new Properties());
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to Neo4j with owen database");
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while connecting to neo4j server", e);
			}

			// R connection
			rCon = new RConnection();
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Successfully connected to R");
			// String rScriptPath = new java.io.File("").getAbsolutePath() + "\\scripts";
			String rScriptPath = "C:\\\\Users\\\\fermion10\\\\Documents\\\\Neo4j\\\\scripts";
			// String workingDir = "setwd(\"" + rScriptPath.replace("\\", "\\\\") + "\")";
			String workingDir = "setwd(\"" + rScriptPath + "\")";
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Trying to load the RScript file at " + rScriptPath);
			rCon.eval(workingDir);
			org.apache.log4j.Logger.getLogger(MetricsList.class).debug("Successfully loaded rScript: source(\"//" + rScriptPath);
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error(
					"An error occurred while attempting to get neo4j connection details", e);
		} catch (RserveException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while trying to connect to R", e);
		}

	}

	@Override
	public void finalize() {
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Shutting down database ...");
		try {
			if (!mysqlCon.isClosed()) {
				try {
					mysqlCon.close();
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to mySql closed!!!!");
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class)
							.error("An error occurred while closing the mysql connection", e);
				}
			}
			if (!neo4jCon.isClosed()) {
				try {
					neo4jCon.close();
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to neo4j closed!!!!");
				} catch (SQLException e) {
					org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class)
							.error("An error occurred while closing the neo4j connection", e);
				}
			}
			if (rCon.isConnected()) {
				rCon.close();
				org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Connection to R closed!!!!");
			}
		} catch (SQLException e) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("An error occurred while attempting to close db connections", e);
		}
	}
}
