package org.icube.owen.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class DatabaseConnectionHelper {

	public static void main(String args[]) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/owen", "root", "");

			Statement stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("select * from employee");

			while (rs.next())
				System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3));

			conn.close();

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// private final static String DB_PATH = getDatabaseConnectionDetails();
	private final static String DB_PATH = "C:\\Users\\fermion10\\Documents\\Neo4j\\graph.db";
	// private final static String DB_PATH = "//Users/apple/Documents/neo4j-enterprise-2.3.1/data/ICICIdb";
	public GraphDatabaseService graphDb;

	private static String getDatabaseConnectionDetails() {

		String dbPath = "";
		File configFile = new File("resources/config.properties");

		try {
			FileReader reader = new FileReader(configFile);
			Properties props = new Properties();
			props.load(reader);
			dbPath = props.getProperty("db_path");
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("DB Path : " + dbPath);
			reader.close();
		} catch (FileNotFoundException ex) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("Database path file doesn't exist", ex);
		} catch (IOException ex) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("IOException in Database", ex);
		}
		return dbPath;
	}

	public DatabaseConnectionHelper() {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
		registerShutdownHook(graphDb);
	}

	public void shutDown() {
		System.out.println();
		System.out.println("Shutting down database ...");
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Shutting down database ...");

		graphDb.shutdown();

	}

	/**
	 * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits (even if you "Ctrl-C" the running application).
	 * 
	 * @param graphDb
	 * - A database factory object
	 */
	public void registerShutdownHook(final GraphDatabaseService graphDb) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
