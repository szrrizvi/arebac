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
				ois = new ObjectInputStream(new FileInputStream(fileNamePrefix +"-" + i + ".ser"));
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
				executeSoloTestFC(test);
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
		System.out.print(time + ", ");
		if (result != null){
			System.out.print(result.size() + ", ");
		}
	}
	
	public void executeSoloTestFCCBJ(GPHolder test){
		
		ConstraintsEvaluator<Node,Entity> ce = new ConstraintsChecker(test, graphDb);
		NeighbourhoodAccess<Node> neighbourhoodAccess = new DBAccess(graphDb, ce);
		VariableOrdering<Node> variableOrdering = new LeastCandidates<Node>(test.getGp());
		AltStart<Node> as = new AttrBasedStart(graphDb, ce);
		
		GPCheckerFCCBJ<Node, Entity> gpEval = new GPCheckerFCCBJ<Node, Entity>(test, ce, neighbourhoodAccess, variableOrdering, as);
		
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
		System.out.print(time + ", ");
		if (result != null){
			System.out.print(result.size() + ", ");
		}
	}
	
	public void executeSoloTestFC(GPHolder test){
		
		GPCheckerFC gpEval = new GPCheckerFC(graphDb, test);
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
		System.out.print(time + ", ");
		if (result != null){
			System.out.println(result.size());
		}
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

		//Create the domains for the parameters in generation
		int[] endSize = {5, 15};
		int[] numMex = {0,1};
		int[] numC = {0, 5};

		//Initialize PRGN with a seed
		Random rand = new Random(274185);

		//Randomly generate the test cases, and run them
		for (int i = 0; i < numTests; i++){

			//Obtain the parameter values for the query gp
			int esA = endSize[rand.nextInt(2)];
			int nmA = numMex[rand.nextInt(2)];
			int ncA = numC[rand.nextInt(2)];

			//Obtain the parameter values for the policy gp
			int esB = endSize[rand.nextInt(2)];
			int nmB = numMex[rand.nextInt(2)];
			int ncB = numC[rand.nextInt(2)];

			//Generate the test case
			EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
			//etw.setParamsA(esA, 0.5d, 2, 0.01f, nmA, ncA, ncA);
			etw.setParamsA(2, 0.1d, 0, 0, 1, 0);
			etw.setParamsB(esB, 0.1d, 2, nmB, ncB, ncB);
			TripleGPHolder test = etw.generateTests();

			//Randomly decide which algorithm(s) to run.
			//Must run at least one of the algorithms.
			boolean twoStep = rand.nextBoolean();
			boolean comb = true;
			if (twoStep){
				comb = rand.nextBoolean();
			}

			//Run the test case
			executeSoloTestFCCBJ(test.combined);
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
	//	Methods for generating test cases				 //
	//													 //
	///////////////////////////////////////////////////////
	
	public int writeDiffTests(Random rand, String folder) throws Exception{
		int count = 0;

		//Initialize the domain for the parameters
		int[] numMex = {0, 1, 2};
		int[] numAttr = {1, 2, 4};


		//Iterate throught the 7 profiles
		for (int profile = 0; profile < 7; profile++){

			List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();

			int eq = 0, ep = 0;
			double compQ = 0.5d, compP = 0.5d;

			if (profile == 0){
				eq = 1;
				ep = 5;
			} else if (profile == 1){
				eq = 1;
				ep = 7;
			} else if (profile == 2){
				eq = 1;
				ep = 10;
				compP = 0.25d;
			}  else if (profile == 3){
				eq = 5;
				ep = 5;
			}  else if (profile == 4){
				eq = 5;
				ep = 7;
			}  else if (profile == 5){
				eq = 7;
				ep = 5;
			}   else if (profile == 6){
				eq = 7;
				ep = 7;
			}



			//Generate 1000 cases for the profile, and add them to the list
			for (int idx = 0; idx < 1000; idx++){
				
				int mq = numMex[rand.nextInt(numMex.length)];
				int aq = numAttr[rand.nextInt(numAttr.length)];
				int rq = numAttr[rand.nextInt(numAttr.length)];

				int mp = numMex[rand.nextInt(numMex.length)];
				int ap = numAttr[rand.nextInt(numAttr.length)];
				int rp = numAttr[rand.nextInt(numAttr.length)];

				
				EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
				etw.setParamsA(eq, compQ, 0, mq, aq, rq);
				etw.setParamsB(ep, compP, 1, mp, ap, rp);

				TripleGPHolder test = etw.generateTests();
				tests.add(test);	
			}

			//Save the list in a new file
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(folder +"/tests" + count + ".ser"));
			oos.writeObject(tests);
			oos.flush();
			oos.close();

			count++;
		}

		//Return the number of files created
		return count;
	}


	/**
	 * Creates the 50 cases for each parameter combination.
	 * Store the 50 cases as a list in a serialized file. Creates a separate
	 * file for each parameter value combination.
	 * @param rand The PRGN
	 * @param folder The name of the folder to store the files in
	 * @return The number of test files created
	 * @throws Exception
	 */
	public int writeTests(Random rand, String folder) throws Exception{

		int count = 0;
		//Initialize the domain for the parameters
		int[] endSize = {1, 5, 7, 10, 15, 20};
		//double[] complete = {0.5d};//, 0.20d};
		int[] numMex = {0, 5, 10};
		int[] numAttr = {0, 5, 7};

		//Iterate through the parameters (query params on top, then the policy params)
		for (int eq = 0; eq < endSize.length; eq++){
			for (int ep = 1; ep < endSize.length; ep++){

				List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();

				double cq = 0.0d;
				int mq = 0;
				int aq = 1;
				if (eq > 0 && eq <= 2){
					cq = 0.5d;
					aq = numAttr[rand.nextInt(2)];
				} else if (eq > 2) {
					cq = 0.25d;
					mq = numMex[rand.nextInt(numMex.length)];
					aq = numAttr[rand.nextInt(numAttr.length)];
				}

				if (aq == 0){
					aq = 1;
				}

				double cp = 0.0d;
				int mp = 0;
				int ap = 0;
				if (ep <= 2){
					cp = 0.5d;
					ap = numAttr[rand.nextInt(2)];
				} else if (ep > 2) {
					cp = 0.25d;
					mp = numMex[rand.nextInt(numMex.length)];
					ap = numAttr[rand.nextInt(numAttr.length)];
				}

				//Generate 50 cases for the profile, and add them to the list
				for (int idx = 0; idx < 50; idx++){
					EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
					etw.setParamsA(endSize[eq], cq, 0, mq, aq, aq);
					etw.setParamsB(endSize[ep], cp, 2, mp, ap, ap);

					TripleGPHolder test = etw.generateTests();
					tests.add(test);	
				}

				//Save the list in a new file
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(folder +"/tests" + count + ".ser"));
				oos.writeObject(tests);
				oos.flush();
				oos.close();

				count++;
			}
		}

		//Return the number of files created
		return count;
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
