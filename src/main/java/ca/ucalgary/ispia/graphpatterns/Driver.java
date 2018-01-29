package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.tests.EvalTestRunner;
import ca.ucalgary.ispia.graphpatterns.tests.Neo4jQueries;
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
			Neo4jQueries njq = new Neo4jQueries(graphDb);
			njq.setDebug(true);
			String q = "MATCH (Cd:PERSON) -[rela : RelD]->(Cb:PERSON) " +  
							"MATCH (Ce:PERSON) -[relb : RelG]->(Cb:PERSON) " +
							"MATCH (Ca:PERSON) -[relc : RelD]->(Cb:PERSON) " +
							"MATCH (Cb:PERSON) -[reld : RelB]->(Cc:PERSON) " +
							"MATCH (Cb:PERSON) -[rele : RelE]->(Cd:PERSON) " +
							"MATCH (Cb:PERSON) -[relf : RelE]->(Ce:PERSON) " +
							"WHERE Cb.`full or part time employment stat`=\"Children or Armed Forces\" AND Ce.`id`=3931 AND Ca.`major occupation code`=\"Not in universe\" AND Ca.`reason for unemployment`=\"Not in universe\" AND Ca.`own business or self employed`=0 AND Ca.`major industry code`=\"Not in universe or children\" AND relb.`weight`=7 AND relf.`weight`=4 AND Ca<>Cd " + 
							"RETURN distinct Ca";
			String q1 = "MATCH (n) WHERE n.id=32706 RETURN n";
							
			//njq.runQuery(q);
			
			TempFCCompare tfcc = new TempFCCompare(graphDb);
			tfcc.runTests(7, "0724files");
			//tfcc.runSpecificTest(872, "0724files/tests6");
			
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