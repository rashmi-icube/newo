package owen.helper;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Creates connection with the database
 * Opens the connection Close the connection
 */
public class DatabaseConnectionHelper {

	final static String DB_PATH = "/Users/apple/Downloads/neo4j-enterprise-2.3.1/data/ICICIdb";
	public static GraphDatabaseService graphDb;

	// Create a new connection with the database

	static {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
		registerShutdownHook(graphDb);
	}

	// Close the connection

	public static void shutDown() {
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
	static void registerShutdownHook(final GraphDatabaseService graphDb) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
