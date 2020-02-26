package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPCheckerFC;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.AltStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ConstraintsEvaluator;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.GPCheckerFCCBJ;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.GPCheckerFCLBJ;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.VariableOrdering;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.AttrBasedStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.ConstraintsChecker;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.DBAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.LeastCandidates;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.SimpleCypherParser;

/**
 * This class runs the evaluation tests. 
 * @author szrrizvi
 *
 */
public class EvalTestRunner {

	private GraphDatabaseService graphDb;

	/**
	 * Constructor. Initialize the graph database service (graphDb)
	 * @param graphDb
	 */
	public EvalTestRunner(GraphDatabaseService graphDb){
		this.graphDb = graphDb;
	}

	///////////////////////////////////////////////////////
	//													 //
	//	Methods for running test cases					 //
	//													 //
	///////////////////////////////////////////////////////

	public void runSimTests(String fileNamePrefix, Random rand){
		
		List<GPHolder> samples = new ArrayList<GPHolder>();
		
		for (int i = 4; i <= 6; i++){
			ObjectInputStream ois = null;
			List<GPHolder> tests = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(fileNamePrefix +"-" + i + ".ser"));
				tests = (List<GPHolder>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			samples.addAll(tests);
			
		}
		
		while (samples.size()>250){
			samples.remove(rand.nextInt(samples.size()));
		}
		
		for (GPHolder test : samples){
			executeSoloTestFCLBJ(test);
			//executeSoloTestFCCBJ(test);
			//executeSoloTestFC(test);
		}
	}
	
	public void runSimTests(String fileName){
		ObjectInputStream ois = null;
		List<GPHolder> tests = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(fileName));
			tests = (List<GPHolder>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
			
		for (GPHolder test : tests){
			executeSoloTestFCLBJ(test);
			//executeSoloTestFCCBJ(test);
			//executeSoloTestFC(test);
		}
		
	}
	
	
	/**
	 * Runs the GPH Test cases.
	 * Precondition: Each file contains a list of GPHolder objects.
	 * @param fileNamePrefix The name of the file: fileNamePrefix + "-" + i + ".ser"
	 * @param numProfiles The number of files to read (starting at 1)
	 */
	public void runGPHTestsList(String fileNamePrefix, int numProfiles){

		for (int i = 1; i <= numProfiles; i++){
			ObjectInputStream ois = null;
			List<GPHolder> tests = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(fileNamePrefix +"-" + 8 + ".ser"));
				tests = (List<GPHolder>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return; 
			}
			
			for (GPHolder test : tests){
				//executeSoloTestFCLBJ(test);
				executeSoloTestFCCBJ(test); 
				//executeSoloTestFC(test);
			}
		}
	}

	/**
	 * Runs the test cases provided in text based format
	 * @param fileNamePrefix The name of the file: fileNamePrefix + "-" + i + ".ser"
	 * @param numProfiles The number of files to read (starting at 1)
	 */
	public void runTxtBasedTests(String fileNamePrefix, int numProfiles){
		for (int i = 1; i <= numProfiles; i++){
			SimpleCypherParser scp = new SimpleCypherParser(fileNamePrefix + "-" + i + ".txt");
			List<GPHolder> gphList = scp.parse();

			for (GPHolder test : gphList){
				executeSoloTestFCCBJ(test);
			}
		}
	}

	public void executeSoloTestFCLBJ(GPHolder test){

		ConstraintsEvaluator<Node,Entity> ce = new ConstraintsChecker(test, graphDb);
		NeighbourhoodAccess<Node> neighbourhoodAccess = new DBAccess(graphDb, ce);
		VariableOrdering<Node> variableOrdering = new LeastCandidates<Node>(test.getGp());
		AltStart<Node> as = new AttrBasedStart(graphDb, ce);

		GPCheckerFCLBJ<Node, Entity> gpEval = new GPCheckerFCLBJ<Node, Entity>(test, ce, neighbourhoodAccess, variableOrdering, as);

		GPCheckerFC gpEvalB = new GPCheckerFC(graphDb, test);
		//Set a 6 second kill switch
		Terminator term = new Terminator(gpEval);
		term.terminateAfter(6000l);
		//Run the algorithm and record the time
		long start = System.nanoTime();
		List<Map<MyNode, Node>> result = gpEval.check();
		long end = System.nanoTime();
		//Make sure the terminator is killed
		term.nullifyObj();
		term.stop();

		long time = end - start;

		//Print the performance time
		int resSize = 0;
		if (result!= null){
			resSize = result.size();
		}
		
		System.out.println(time);// + ", " + resSize + ", " + gpEval.getAllRes() + ", " + gpEval.getSearchSpace() + ", " + gpEval.getMaxNeighbourhood());
		/*if (result != null){
			System.out.print(result.size() + ", ");

			for (Map<MyNode, Node> res : result){
				System.out.print("Result [");
				for (MyNode key : res.keySet()){
					System.out.print("(" + key.getId() + ", " + res.get(key).getId() + "), ");
				}
				System.out.println("]");
			}
		}*/
	}

	public void executeSoloTestFCCBJ(GPHolder test){

		ConstraintsEvaluator<Node,Entity> ce = new ConstraintsChecker(test, graphDb);
		NeighbourhoodAccess<Node> neighbourhoodAccess = new DBAccess(graphDb, ce);
		VariableOrdering<Node> variableOrdering = new LeastCandidates<Node>(test.getGp());
		AltStart<Node> as = new AttrBasedStart(graphDb, ce);

		GPCheckerFCCBJ<Node, Entity> gpEval = new GPCheckerFCCBJ<Node, Entity>(test, ce, neighbourhoodAccess, variableOrdering, as);

		//Set a 6 second kill switch
		Terminator term = new Terminator(gpEval);
		term.terminateAfter(6000l);
		//Run the algorithm and record the time
		long start = System.nanoTime();
		List<Map<MyNode, Node>> result = gpEval.check();
		long end = System.nanoTime();
		//Make sure the terminator is killed
		term.nullifyObj();
		term.stop();

		long time = end - start;

		//Print the performance time
		int resSize = 0;
		if (result!= null){
			resSize = result.size();
		}
		
		System.out.println(time + ", " + resSize + ", " + gpEval.getAllRes() + ", " + gpEval.getSearchSpace() + ", " + gpEval.getMaxNeighbourhood());

	}

	public void executeSoloTestFC(GPHolder test){

		GPCheckerFC gpEval = new GPCheckerFC(graphDb, test);
		//Set a 6 second kill switch
		Terminator term = new Terminator(gpEval);
		term.terminateAfter(60000l);
		//Run the algorithm and record the time
		long start = System.nanoTime();
		List<Map<MyNode, Node>> result = gpEval.check();
		long end = System.nanoTime();
		//Make sure the terminator is killed
		term.nullifyObj();
		term.stop();

		long time = end - start;

		//Print the performance time
		int resSize = 0;
		if (result!= null){
			resSize = result.size();
		}
		
		System.out.println(time + ", " + resSize + ", " + gpEval.getAllRes() + ", " + gpEval.getSearchSpace() + ", " + 0);
		/*if (result != null){
			System.out.println(result.size());

			for (Map<MyNode, Node> res : result){
				System.out.print("Result [");
				for (MyNode key : res.keySet()){
					System.out.print("(" + key.getId() + ", " + res.get(key).getId() + "), ");
				}
				System.out.println("]");
			}
		}*/
	}


	///////////////////////////////////////////////////////
	//													 //
	//	Methods for running warmup cases				 //
	//													 //
	///////////////////////////////////////////////////////

	/**
	 * Runs the warmup tests.
	 * @param The number of warmup tests to run
	 */
	public void warmup(int numTests){

		Random rand = new Random(274185);

		int[] sizes = {5, 7, 9, 10, 11, 13};
		int[] attrs = {1, 2, 4};
		int[] mex = {0, 1, 2};
		int[] resSizes = {1, 2, 4};
		int count = 1;
		for (int i = 0; i < numTests; i++){
			int size = sizes[rand.nextInt(sizes.length)];
			int vattrs = attrs[rand.nextInt(attrs.length)];
			int eattrs = attrs[rand.nextInt(attrs.length)];
			int mexs = mex[rand.nextInt(mex.length)];
			int resSize = resSizes[rand.nextInt(resSizes.length)];
			SubgraphGenerator sg = new SubgraphGenerator(graphDb, 82168, rand, size, 1.0d, 1, mexs, vattrs, eattrs, resSize);

			GPHolder gph = sg.createDBBasedGP();
			if (gph == null){
				i--;
			} else { 
				executeSoloTestFCLBJ(gph);
				//executeSoloTestFCCBJ(gph);
				//executeSoloTestFC(gph);
			}
		}
	}

	///////////////////////////////////////////////////////
	//													 //
	//	Helper Methods									 //
	//													 //
	///////////////////////////////////////////////////////


	/**
	 * Runs the graph pattern analys for the tests.
	 * @param numFiles The number of test files in the folder.
	 * @param folder The name of the folder containing test files.
	 * @throws Exception
	 */
	public void analyzeTests(int numFiles, String folder) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+i+".ser"));
			List<TripleGPHolder> tests = (List<TripleGPHolder>) ois.readObject();
			ois.close();
			//Analyze the stats
			GPUtil.PrintStats(tests);

		}
	}

	///////////////////////////////////////////////////////
	//													 //
	//	Terminators for running test cases				 //
	//													 //
	///////////////////////////////////////////////////////

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

	public class TerminatorCypher {

		private Transaction tx;
		private ExecutorService service;

		TerminatorCypher( Transaction tx ) {
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
					while ( (System.currentTimeMillis() - startTime) < millis );
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
