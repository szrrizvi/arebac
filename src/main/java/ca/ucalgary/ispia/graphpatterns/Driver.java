package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.tests.EvalTestRunner;
import ca.ucalgary.ispia.graphpatterns.tests.TempFCCompare;
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
		GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		Random rand = new Random(3706715);
		
		EvalTestRunner etr = new EvalTestRunner(graphDb);
				
		try {
			TempFCCompare tfcc = new TempFCCompare(graphDb);
			tfcc.runTests(5, "0724files");
			
			//etr.warmup(100);
			//etr.writeDiffTests(rand, "0724files");
			//etr.runTests(7, "0724files", true, true);
			//etr.writeTests(rand, "0623files");
			//etr.analyzeTests(1, "0621files");
			//etr.debugging(7, "0724files");

		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Writing Failed");
		}
		
		graphDb.shutdown();
		System.out.println("ENDING");
		
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