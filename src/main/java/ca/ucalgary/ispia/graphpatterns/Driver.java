package ca.ucalgary.ispia.graphpatterns;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.tests.EvalTestRunner;
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

		Driver d = new Driver();
		GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		
		EvalTestRunner etr = new EvalTestRunner(graphDb);
		//etr.warmup(250);
		System.out.println("Warmup Complete\n");
		etr.runTxtBasedTests(7);
		System.out.println("Done");
		
		/*try (Transaction tx = graphDb.beginTx()){
			

			Node n = graphDb.getNodeById(62977);
			
			System.out.println(n);
			Map<String, Object> props = n.getAllProperties();
			
			for (String key : props.keySet()){
				System.out.println(key + ": " + props.get(key));
			}
			
			
			tx.success();
		}*/
		
		//GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		//d.runTests(gphList, graphDb);
		//d.runTest(gphList.get(103), graphDb);


		//Random random = new Random(621901);
		//SimTestGenWrapper.generateSimTests(graphDb, "slashdot1rid", random);

		/*
		try {
			SimTestRunner str = new SimTestRunner(graphDb);
			str.runTest("simulation-tests/Slashdot0902tests/", 3, 551);
			str.runTests(1, "simulation-tests/slashdot1ridtests/");
		} catch (Exception e){
			System.out.println(e);
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
