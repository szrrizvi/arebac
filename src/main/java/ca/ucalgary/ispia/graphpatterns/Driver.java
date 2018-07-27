package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPCheckerFC;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.AltStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ConstraintsEvaluator;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.GPCheckerFCCBJ;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.VariableOrdering;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.AttrBasedStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.ConstraintsChecker;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.DBAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.LeastCandidates;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.tests.EvalTestRunner;
import ca.ucalgary.ispia.graphpatterns.tests.Killable;
import ca.ucalgary.ispia.graphpatterns.util.Translator;
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

		//SimpleCypherParser scp = new SimpleCypherParser("profile-1.txt");
		//List<GPHolder> gphList = scp.parse();


		Driver d = new Driver();
		GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
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
