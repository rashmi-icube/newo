package owen.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Manages database connections
 */

public class DatabaseConnectionHelper {	
	
	//private final static String DB_PATH = getDatabaseConnectionDetails();
	private final static String DB_PATH = "C:\\Users\\fermion10\\Documents\\Neo4j\\graph.db";
	public GraphDatabaseService graphDb;

	private static String getDatabaseConnectionDetails() {

		//String url = DatabaseConnectionHelper.class.getResource("resources/config.properties").toString();

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
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("Database path file doesn't exist");
		} catch (IOException ex) {
			org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).error("IOException in Database");
		}
		return dbPath;
	}

	public DatabaseConnectionHelper() {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(
				DB_PATH));
		registerShutdownHook(graphDb);
	}

	public void shutDown() {
		System.out.println();
		System.out.println("Shutting down database ...");
		org.apache.log4j.Logger.getLogger(DatabaseConnectionHelper.class).debug("Shutting down database ...");

		graphDb.shutdown();

	}

	/**
	 * Registers a shutdown hook for the Neo4j instance so that it shuts down
	 * nicely when the VM exits (even if you "Ctrl-C" the running application).
	 * 
	 * @param graphDb
	 *            - A database factory object
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
