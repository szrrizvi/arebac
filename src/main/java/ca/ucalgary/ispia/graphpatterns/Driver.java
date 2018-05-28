package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.tests.SimTestGenWrapper;
/**
 * The driver.
 * @author szrrizvi
 *
 */
public class Driver {
	/**
	 * The main control for specifying tasks.
	 * @param args
	 */
	public static void main(String[] args){	
		//Test!
				
		Driver d = new Driver();
		GraphDatabaseService graphDb = d.getGraphDb("simulation-tests/soc-pokecdb");
		
		Random random = new Random(1172506);
		
		
		SimTestGenWrapper.generateSimTests(graphDb, "soc-pokec", random);
		/*
		SimTestRunner str = new SimTestRunner(graphDb);
		try {
			str.runTest("simulation-tests/Slashdot0902tests/", 3, 551);
			//str.runTests(5, "simulation-tests/Slashdot0902tests/");
		} catch (Exception e){
			System.out.println(e);
			return;
		}*/
		
		graphDb.shutdown();
	}

	/**
	 * Initialize the graph database
	 * @param db The name of directory containing the database
	 * @return The generated GraphDatabaseService object.
	 */
	private GraphDatabaseService getGraphDb(String db){
		File db_file = new File(db);

		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( db_file ).loadPropertiesFromFile("neo4j.properties").newGraphDatabase();
		registerShutdownHook( graphDb );

		return graphDb;
	}

	// START SNIPPET: shutdownHook
	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		} );
	}
}
