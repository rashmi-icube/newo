package owen.helper;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Creates connection with the database 
 * Opens the connection 
 * Close the connection
 */
public class DatabaseConnectionHelper {

	//final static String DB_PATH = getDatabaseConnectionDetails();
	final static String DB_PATH = "C:\\Users\\fermion10\\Documents\\Neo4j\\graph.db";
	
	public GraphDatabaseService graphDb;

	/*private static String getDatabaseConnectionDetails() {
		String dbPath = "";

		File configFile = new File("resources/config.properties");

		try {
			FileReader reader = new FileReader(configFile);
			Properties props = new Properties();
			props.load(reader);

			dbPath = props.getProperty("db_path");

			System.out.println("DB Path : " + dbPath);
			reader.close();
		} catch (FileNotFoundException ex) {
			// file does not exist
		} catch (IOException ex) {
			// I/O error
		}
		return dbPath;
	}*/

	// Create a new connection with the database

	public DatabaseConnectionHelper() {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
		registerShutdownHook(graphDb);
	}

	// Close the connection

	public void shutDown() {
		System.out.println();
		System.out.println("Shutting down database ...");

		graphDb.shutdown();

	}

	/**
	 * Registers a shutdown hook for the Neo4j instance so that it shuts down
	 * nicely when the VM exits (even if you "Ctrl-C" the running application).
	 * 
	 * @param graphDb - A database factory object
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
