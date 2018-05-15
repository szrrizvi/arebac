package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.GPCheckerFCCBJ;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.VariableOrdering;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.DSAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.LeastCandidates;
import ca.ucalgary.ispia.graphpatterns.graph.DataSetInterface;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * This class runs the evaluation tests. 
 * @author szrrizvi
 *
 */
public class SimTestRunner {

	private DataSetInterface dataset;

	/**
	 * Constructor. Initialize the data set wrapper
	 * @param graphDb
	 */
	public SimTestRunner(DataSetInterface dataset){
		this.dataset = dataset;
	}



	/**
	 * Reads the test from the files, and executes them. 
	 * Assumed: At least one of the flags is true.
	 * @param numFiles The number of test files in the folder.
	 * @param folder The name of the folder containing test files.
	 * @param twoStep Flag. If true, run the twoStep algorithm, else don't run it.
	 * @param comb Flag. If true, run the combined algorithm, else don't run it.
	 * @throws Exception
	 */
	public void runTests(int numFiles, String path) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path+"/test"+i+".ser"));
			List<GPHolder> tests = (List<GPHolder>) ois.readObject();
			ois.close();
			//Execute the tests
			executeTests(tests);
		}
	}
	
	public void runTest(String path, int set, int idx) throws Exception{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path+"/test"+set+".ser"));
		List<GPHolder> tests = (List<GPHolder>) ois.readObject();
		ois.close();
		
		GPHolder gph = tests.get(idx);
		System.out.println(gph);
		
		List<GPHolder> temp = new ArrayList<GPHolder>();
		temp.add(gph);
		executeTests(temp);
	}

	/**
	 * Executes the given list of tests. The flags specify which algorithm(s) to run.
	 * Assumed: At least one of the flags is true.
	 * @param tests The tests to execute.
	 */
	public void executeTests(List<GPHolder> tests){
		for (GPHolder test : tests){	//Iterate through the tests
			NeighbourhoodAccess<MyNode> neighbourhoodAccess = new DSAccess(dataset);
			VariableOrdering<MyNode> variableOrdering = new LeastCandidates<MyNode>(test.getGp());

			GPCheckerFCCBJ<MyNode, Object> gpFC = new GPCheckerFCCBJ<MyNode, Object>(test, null, neighbourhoodAccess, variableOrdering, null);
			//Run the algorithm and record the time

			Terminator term = 	new Terminator(gpFC);
			term.terminateAfter(60000l);

			long start = System.nanoTime();
			List<Map<MyNode, MyNode>> result = gpFC.check();
			long end = System.nanoTime();
			
			//Make sure the terminator is killed
			term.nullifyObj();
			term.stop();
			
			if (result != null){
				System.out.print("Size: " + result.size());
			} else {
				System.out.print("Result NULL");
			}
			System.out.println(", Time: " + (end-start));
		}
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
