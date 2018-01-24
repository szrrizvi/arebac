package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.GPCheckerOpt;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public class TempFCCompare {
	private GraphDatabaseService graphDb;

	/**
	 * Constructor. Initialize the graph database service (graphDb)
	 * @param graphDb
	 */
	public TempFCCompare(GraphDatabaseService graphDb){
		this.graphDb = graphDb;
	}

	/**
	 * Reads the test from the files, and executes them. 
	 * @param numFiles The number of test files in the folder.
	 * @param folder The name of the folder containing test files.
	 * @throws Exception
	 */
	public void runTests(int numFiles, String folder) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+i+".ser"));
			List<TripleGPHolder> tests = (List<TripleGPHolder>) ois.readObject();
			ois.close();
			//Execute the tests
			System.out.println("Profile " + (i+1));
			executeTests(tests);
		}
	}
	
	public void runSpecificTest(int testNum, String fileName) throws Exception{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName+".ser"));
		List<TripleGPHolder> tests = (List<TripleGPHolder>) ois.readObject();
		ois.close();
		
		TripleGPHolder test = tests.get(testNum);
		executeTests(test);
		
	}

	/**
	 * Executes the given test.
	 * @param test The target test.
	 */
	public void executeTests(TripleGPHolder test){
		//Add the test to a list, and invoke the overloading method.
		List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();
		tests.add(test);
		executeTests(tests);
	}

	/**
	 * Executes the given list of tests.
	 * @param tests The tests to execute.
	 */
	public void executeTests(List<TripleGPHolder> tests){
		for (TripleGPHolder test : tests){	//Iterate through the tests

			//Initialize the variables
			GPHolder dbQuery = test.dbQeury;
			GPHolder policy = test.policy;
			GPHolder combined = test.combined;


			GPCheckerOpt gpFC = new GPCheckerOpt(graphDb, combined);
			//GPCheckerFC gpFC = new GPCheckerFC(graphDb, combined);
			List<Map<MyNode, Node>> result = gpFC.check();

			if (result != null){
				
				for (Map<MyNode, Node> res : result){
					for (MyNode key : res.keySet()){
						System.out.println("Ca: " + res.get(key));
						
						try(Transaction tx = graphDb.beginTx()){
							Node n = res.get(key);
							
							Map<String, Object> prop = n.getAllProperties();
							for (String p : prop.keySet()){
								System.out.println(p + " " + prop.get(p));
							}
							
							tx.success();							
						}
						
					}
					System.out.println();
				}
				
				//System.out.println(result.size());
			}
		}
	}

}
