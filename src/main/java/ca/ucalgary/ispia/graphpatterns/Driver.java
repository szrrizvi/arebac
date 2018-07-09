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
import ca.ucalgary.ispia.graphpatterns.tests.Killable;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestRunner;
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
		GraphDatabaseService graphDb = d.getGraphDb("simulation-tests/Slashdot0902db");
		//GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		//d.runTests(gphList, graphDb);
		//d.runTest(gphList.get(103), graphDb);


		/*
		Random random = new Random(1172506);
		SimTestGenWrapper.generateSimTests(graphDb, "soc-pokec", random);*/
		
		SimTestRunner str = new SimTestRunner(graphDb);
		try {
			//str.runTest("simulation-tests/Slashdot0902tests/", 3, 551);
			str.runTests(5, "simulation-tests/Slashdot0902tests/");
		} catch (Exception e){
			System.out.println(e);
			return;
		}

		graphDb.shutdown();
	}

	private void runTest(GPHolder gph,  GraphDatabaseService graphDb){
		System.out.println(gph);
		try(Transaction tx = graphDb.beginTx(6, TimeUnit.SECONDS)){

			String query = Translator.translateToCypher(gph);
			System.out.println(query);
			Result res = graphDb.execute(query);


			while (res.hasNext()){
				Map<String, Object> map = res.next();
				for (String key : map.keySet()){
					System.out.println(key + ": " + map.get(key));
				}
				System.out.println();
			}
			tx.success();
		} catch (Exception e){

		}
		
		GPCheckerFC fc = new GPCheckerFC(graphDb, gph);

		Terminator term = new Terminator(fc);
		term.terminateAfter(6000l);

		long start = System.nanoTime();
		List<Map<MyNode, Node>> resultsFC = fc.check();
		long end = System.nanoTime();

		term.nullifyObj();
		term.stop();
		
		for (Map<MyNode,Node> map : resultsFC){
			for (MyNode key : map.keySet()){
				System.out.println(key.getId() + ": " + map.get(key));
			}
			System.out.println();
		}
	}

	private void runTests(List<GPHolder> gphList, GraphDatabaseService graphDb){
		for (GPHolder gph : gphList){
			GPCheckerFC fc = new GPCheckerFC(graphDb, gph);

			Terminator term = new Terminator(fc);
			term.terminateAfter(6000l);

			long start = System.nanoTime();
			List<Map<MyNode, Node>> resultsFC = fc.check();
			long end = System.nanoTime();

			term.nullifyObj();
			term.stop();

			ConstraintsEvaluator<Node,Entity> ce = new ConstraintsChecker(gph, graphDb);
			NeighbourhoodAccess<Node> neighbourhoodAccess = new DBAccess(graphDb, ce);
			VariableOrdering<Node> variableOrdering = new LeastCandidates<Node>(gph.getGp());
			AltStart<Node> as = new AttrBasedStart(graphDb, ce);

			GPCheckerFCCBJ<Node,Entity> fccbj = new GPCheckerFCCBJ<Node,Entity>(gph, ce, neighbourhoodAccess, variableOrdering, as);
			
			term = new Terminator(fccbj);
			term.terminateAfter(6000l);

			long startcbj = System.nanoTime();
			List<Map<MyNode, Node>> resultsFCCBJ = fccbj.check();
			long endcbj = System.nanoTime();

			term.nullifyObj();
			term.stop();
/*			
			int resultsCypherCount = 0;

			try(Transaction tx = graphDb.beginTx(6, TimeUnit.SECONDS)){

				String query = Translator.translateToCypher(gph);

				Result res = graphDb.execute(query);


				while (res.hasNext()){
					res.next();
					resultsCypherCount++;
				}
				tx.success();
			} catch (Exception e){
				resultsCypherCount = -1;
			}
*/

			if (resultsFCCBJ != null && (endcbj-startcbj) <= 6000000000l){
				System.out.print(resultsFCCBJ.size()+", ");
			} else {
				System.out.print("Time out: " + resultsFCCBJ.size()+", ");
			}
			
			if (resultsFC != null && (end-start) <= 6000000000l){
				System.out.println(resultsFC.size());
			} else {
				System.out.println("Time out: " + resultsFC.size());
			}
			
		}
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


	/**
	 * Inner classes, used for killing the evaluation algorithms. 
	 * @author szrrizvi
	 *
	 */
	public class Terminator {

		private Killable obj;
		private ExecutorService service;

		/**
		 * Constructor. 
		 * @param obj The object to kill
		 */
		Terminator( Killable obj ) {
			this.obj = obj;
			service = null;
		}

		/**
		 * Removes the reference to the object
		 */
		public void nullifyObj(){
			this.obj = null;
		}

		/**
		 * Terminates the process after the specified time.
		 * @param millis The specified time to terminate the process.
		 */
		public void terminateAfter( final long millis ) {
			service = Executors.newSingleThreadExecutor();
			service.submit( new Runnable() {
				@Override
				public void run() {

					//Make the threat sleep for the specified time
					long startTime = System.currentTimeMillis();
					do {
						try {
							Thread.sleep( millis );
						} catch ( InterruptedException ignored ){
							return;
						}
					}
					while ((System.currentTimeMillis() - startTime) < millis );

					//Kill the process if we still have reference to the object
					if (obj != null) {
						obj.kill();
					}

				}
			}
					);
		}

		//Stops this service.
		public void stop(){
			service.shutdownNow();
		}
	}
}
