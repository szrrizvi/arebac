package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionTerminatedException;

/**
 * This class provides the means for running Neo4j queries.
 * @author szrrizvi
 *
 */
public class Neo4jQueries {


	private GraphDatabaseService graphDb;
	private boolean debug;

	/**
	 * Constructor. Initializes the Neo4j database.
	 * @param db_path The path to the Neo4j database.
	 */
	public Neo4jQueries(GraphDatabaseService graphDb){

		this.graphDb = graphDb;
		debug = false;
	}

	public void setDebug(boolean debug){
		this.debug = debug;
	}

	/**
	 * The method runs the given query on the database
	 * @param query The cypher query to run
	 */
	public long runQuery(String query){

		//Initialize variables to check performance
		long start = 0l, end = 0l;

		Terminator term = null;

		try (Transaction tx = graphDb.beginTx()){
			term = new Terminator(tx);
			term.terminateAfter(10000l);

			//Perform query, iterate through all of the results, and record the performance.
			start = System.nanoTime();
			Result result = graphDb.execute(query);

			while(result.hasNext()){
				Map<String, Object> row = result.next();

				if (debug){
					System.out.println("true");
					for (String str : row.keySet()){
						System.out.print(str + ": " + row.get(str) + ", ");
						
						Node n = (Node) row.get(str);
						Map<String, Object> prop = n.getAllProperties();
						for (String p : prop.keySet()){
							System.out.println(p + " " + prop.get(p));
						}
					}
					System.out.println();
				}
			}
			end = System.nanoTime();
			term.nullifyTx();
			term.stop();
			result.close();

			//Print the running time
			if (debug){
				System.out.println("Time: " + (end-start));
			}

			//Print the ExecutionPlanDescriptions
			ExecutionPlanDescription plan = result.getExecutionPlanDescription();
			List<ExecutionPlanDescription> plans = plan.getChildren();


			if (debug){
				for (ExecutionPlanDescription p : plans){
					System.out.println(plan.toString());
					System.out.println(p.getProfilerStatistics().getDbHits() + ", " + p.getProfilerStatistics().getRows());
				}
			}
			tx.success();
		} catch (TransactionTerminatedException e){
			term.stop();
			System.out.println("Transaction lasted more than 5 seconds");
		}

		return (end-start);
	}

	public class Terminator {

		private Transaction tx;
		private ExecutorService service;

		Terminator( Transaction tx ) {
			this.tx = tx;
			service = null;
		}

		public void nullifyTx(){
			this.tx = null;
		}

		public void terminateAfter( final long millis ) {
			service = Executors.newSingleThreadExecutor();
			service.submit( new Runnable() {
				@Override
				public void run() {
					
					
					long startTime = System.currentTimeMillis();
					do {
						try {
							Thread.sleep( millis );
						} catch ( InterruptedException ignored ){
							return;
						}
					}
					while ((System.currentTimeMillis() - startTime) < millis );
					// START SNIPPET: terminateTx


					if (tx != null) {
						tx.terminate();
					}
					
				}
			}
					);
		}

		public void stop(){
			service.shutdownNow();
		}
	}
}
