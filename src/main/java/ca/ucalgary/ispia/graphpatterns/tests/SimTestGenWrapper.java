package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * This class generates graph pattern queries.
 * Currently, the class is customized to create tests for the simluation experiments, 
 * but it can easily be generalized for generating generic graph pattern.
 * @author szrrizvi
 *
 */

public class SimTestGenWrapper {

	/**
	 * This method creates the test cases and saves them in a serialized manner. The tests stored in location "simulation-tests/"+name+"tests/test"+idx+".ser" 
	 * @param graphDb The graph database interface to use as the base.
	 * @param name The name of the folder where we want to save the test cases.
	 * @param rand The PRNG object. This object is used for generating each test case (in sequence). We never re-initialize the PRNG object.
	 */
	public static void generateSimTests(GraphDatabaseService graphDb, String name, Random rand){

		//Initialize the parameters for the graph patterns (test cases)
		int endSize = 16;			//Minimal number of nodes in the graph pattern
		double complete = 0.15d;		//The connectedness (in %) between nodes in the graph pattern
		int rooted = 1; 			//# of rooted nodes
		float p = 0.02f; 			//The probability with which to take a relationship

		int idx = 0;				//The index counter for the output files


		//Create test cases of sizes {6, 8, 10,12, 14}
		//for (endSize = 6; endSize < 16; endSize = endSize+2){

		//For gp's of size 10 or higher, change the connectedness requirement to 15%
		//if (endSize >= 10){
		//	complete = 0.15d;
		//}

		//Generate 1000 graph patterns 
		List<GPHolder> list = new ArrayList<GPHolder>();
		for (int count = 0; count < 1000; count++){
			//The createDbBasedGP method returns null if it was not able to generate a graph patter that matched the parameters
			GPHolder gph = null;
			while (gph == null){
				EvalTestGenerator etg = new EvalTestGenerator(graphDb, rand, endSize, complete,  rooted, p, 0, 0, 0);
				gph = etg.createDBBasedGP();
			}
			//If reached here, then we have a valid graph pattern

			assignResultSchema(gph, rand); //Assign the result schema for the graph pattern
			list.add(gph); //Add the graph pattern to the list
		}

		//Save the list of graph pattern in a serialized manner.
		try {
			FileOutputStream fout = new FileOutputStream("simulation-tests/"+name+"tests/test"+idx+".ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(list);
			oos.close();

			idx++;
			System.out.println("Completed: " +idx);
		} catch (IOException e){
			System.out.println("IOException" + e);
		}

		//}
	}

	/**
	 * Given a graph pattern holder and a PRNG, this method assigns a result schema to the graph pattern holder.
	 * @param gpHolder The graph pattern holder.
	 * @param random The PRNG
	 */
	private static void assignResultSchema(GPHolder gpHolder, Random random){

		if (gpHolder != null){	//ensure gpHolder is not null

			List<MyNode> temp = new ArrayList<MyNode>();
			temp.addAll(gpHolder.getGp().getNodes());

			List<MyNode> resultSchema = new ArrayList<MyNode>();

			int schemaSize = 0;
			//Randomly choose a (non-zero) size of result schema.
			while (schemaSize == 0){
				schemaSize = random.nextInt(temp.size());
			}

			//Randomly pick nodes to fill the schema.
			for (int count = 0; count < schemaSize; count++){
				MyNode resNode = temp.remove(random.nextInt(temp.size()));
				resultSchema.add(resNode);
			}

			//Set the schema in the graph pattern holder.
			gpHolder.setResultSchema(resultSchema);
		}
	}
}


