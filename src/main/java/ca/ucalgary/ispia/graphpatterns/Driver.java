package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.util.Random;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.graph.RelType;
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
		GraphDatabaseService graphDb = d.getGraphDb("simulation-tests/Slashdot0902db");
		
		Random random = new Random(9567504);
		
		try (Transaction tx = graphDb.beginTx()){
			ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
			
			int[] maxT = {0,0,0,0,0,0,0};
			int[] maxO = {0,0,0,0,0,0,0};
			int[] maxI = {0,0,0,0,0,0,0};
			
			
			while (nodes.hasNext()){
				Node node = nodes.next();
				
				int val = node.getDegree(RelType.RelA);
				if (val > maxT[0]){
					maxT[0] = val;
				}
				val = node.getDegree(RelType.RelB);
				if (val > maxT[1]){
					maxT[1] = val;
				}
				val = node.getDegree(RelType.RelC);
				if (val > maxT[2]){
					maxT[2] = val;
				}
				val = node.getDegree(RelType.RelD);
				if (val > maxT[3]){
					maxT[3] = val;
				}
				val = node.getDegree(RelType.RelE);
				if (val > maxT[4]){
					maxT[4] = val;
				}
				val = node.getDegree(RelType.RelF);
				if (val > maxT[5]){
					maxT[5] = val;
				}
				val = node.getDegree(RelType.RelG);
				if (val > maxT[6]){
					maxT[6] = val;
				}
				
				val = node.getDegree(RelType.RelA, Direction.OUTGOING);
				if (val > maxO[0]){
					maxO[0] = val;
					System.out.println("AO: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelB, Direction.OUTGOING);
				if (val > maxO[1]){
					maxO[1] = val;
					System.out.println("BO: " + node + ", " + val);
				}
				
				val = node.getDegree(RelType.RelC, Direction.OUTGOING);
				if (val > maxO[2]){
					maxO[2] = val;
					System.out.println("CO: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelD, Direction.OUTGOING);
				if (val > maxO[3]){
					maxO[3] = val;
					System.out.println("DO: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelE, Direction.OUTGOING);
				if (val > maxO[4]){
					maxO[4] = val;
					System.out.println("EO: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelF, Direction.OUTGOING);
				if (val > maxO[5]){
					maxO[5] = val;
					System.out.println("FO: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelG, Direction.OUTGOING);
				if (val > maxO[6]){
					maxO[6] = val;
					System.out.println("GO: " + node + ", " + val);
				}
				
				val = node.getDegree(RelType.RelA, Direction.INCOMING);
				if (val > maxI[0]){
					maxI[0] = val;
					System.out.println("AI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelB, Direction.INCOMING);
				if (val > maxI[1]){
					maxI[1] = val;
					System.out.println("BI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelC, Direction.INCOMING);
				if (val > maxI[2]){
					maxI[2] = val;
					System.out.println("CI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelD, Direction.INCOMING);
				if (val > maxI[3]){
					maxI[3] = val;
					System.out.println("DI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelE, Direction.INCOMING);
				if (val > maxI[4]){
					maxI[4] = val;
					System.out.println("EI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelF, Direction.INCOMING);
				if (val > maxI[5]){
					maxI[5] = val;
					System.out.println("FI: " + node + ", " + val);
				}
				val = node.getDegree(RelType.RelG, Direction.INCOMING);
				if (val > maxI[6]){
					maxI[6] = val;
					System.out.println("GI: " + node + ", " + val);
				}
			}
			
			for (int i = 0; i < 7; i++){
				System.out.print(maxT[i] + " ");
			}
			System.out.println();
			
			for (int i = 0; i < 7; i++){
				System.out.print(maxO[i] + " ");
			}
			System.out.println();
			
			for (int i = 0; i < 7; i++){
				System.out.print(maxI[i] + " ");
			}
			System.out.println();
			
			tx.success();
		}
		
		/*
		//SimTestGenWrapper.generateSimTests(graphDb, "Slashdot0902", random);
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
