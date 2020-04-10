package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.tests.EvalTestRunner;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;
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
		//GraphDatabaseService graphDb = d.getGraphDb("simulation-tests/soc-pokecdb");
		GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		
		/*try (Transaction tx = graphDb.beginTx()){
			//Node n = graphDb.getNodeById(52);
			Node n = graphDb.findNode(LabelEnum.PERSON, "id", 8722);
			Map<String, Object> props = n.getAllProperties();
			
			for (String key : props.keySet()) {
				System.out.println(key + " " + props.get(key));
			}
			
			Iterator<Relationship> rels = n.getRelationships().iterator();
			System.out.println(n);
			while(rels.hasNext()){
				Relationship r = rels.next();
				System.out.println(r);
				props = r.getAllProperties();
				
				for (String key : props.keySet()){
					System.out.println(key + ", " + props.get(key));
				}
			}
		}*/
		
		//GraphUtil.degDistribution(graphDb);
		//GraphUtil.checkConnected(graphDb);
		
		Random rand = new Random(5927541);
		EvalTestRunner etr = new EvalTestRunner(graphDb);
		//etr.warmup(250);
		//System.out.println("Done Warmup\n"); 
		//etr.runGPHTestsList("simulation-tests/slashdottests/testCase", 6);
		//etr.runSimTests("simulation-tests/slashdottests/testCase", rand);
		//etr.runSimTests("performance-tests/testCase", rand);
		//etr.runSimTests("simulation-tests/soc-pokectests/testCase-1.ser");
		//etr.runTxtBasedTests("text-tests/profile", 1);
		
		etr.runGPHTestsList("performance-tests/testCase", 6);
		graphDb.shutdown();
		System.exit(0);
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
