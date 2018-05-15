package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.graph.DataSet;
import ca.ucalgary.ispia.graphpatterns.graph.DataSetInterface;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestGenerator;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestRunner;
import ca.ucalgary.ispia.graphpatterns.tests.TxtToDS;
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
		//Driver d = new Driver();
		//GraphDatabaseService graphDb = d.getGraphDb("slashdotNeo4j");
		//graphDb.shutdown();

		//Random rand = new Random(5158485);
		//saveDataSet("Slashdot0902", rand);
		//DataSetInterface dsi = loadDataSet("Slashdot0902");
		//dsStats(dsi);

		Random rand = new Random(7305156);
		//saveDataSet("soc-pokec-relationships", rand);
		//DataSetInterface dsi = loadDataSet("soc-pokec-relationships");
		//dsStats(dsi);


		//saveDataSet("Slashdot0902", rand);
		DataSetInterface dsi = loadDataSet("Slashdot0902");
		//dsStats(dsi);
		//generateSimTests(dsi, "Slashdot0902", rand);

		SimTestRunner str = new SimTestRunner(dsi);
		try {
			str.runTest("simulation-tests/Slashdot0902", 0, 0);
			//str.runTests(5, "simulation-tests/Slashdot0902");
		} catch (Exception e){
			e.printStackTrace();
		}


	}

	private static void dsStats(DataSetInterface dsi){
		int maxTDegree = -1;
		int minTDegree = Integer.MAX_VALUE;
		int totalTDegree = 0;

		int maxODegree = -1;
		int minODegree = Integer.MAX_VALUE;
		int totalODegree = 0;

		int maxIDegree = -1;
		int minIDegree = Integer.MAX_VALUE;
		int totalIDegree = 0;

		int numNodes = 0;

		MyNode[] nodes = dsi.getNodes();

		for (MyNode node : nodes){
			if (node != null){
				
				numNodes++;
				
				int tDegree = dsi.getTotalDegree(node);
				int oDegree = dsi.getOutDegree(node);
				int iDegree = dsi.getInDegree(node);

				if (tDegree > maxTDegree){
					maxTDegree = tDegree;
				}
				if (tDegree < minTDegree){
					minTDegree = tDegree;
				}


				if (oDegree > maxODegree){
					maxODegree = oDegree;
				}
				if (oDegree < minODegree){
					minODegree = oDegree;
				}


				if (iDegree > maxIDegree){
					maxIDegree = iDegree;
				}
				if (iDegree < minIDegree){
					minIDegree = iDegree;
				}

				totalTDegree += tDegree;
				totalODegree += oDegree;
				totalIDegree += iDegree;
			}
		}
		
		System.out.println(numNodes);

		System.out.println("Total Total Degree: " + totalTDegree);
		System.out.println("Max Total Degree: " + maxTDegree);
		System.out.println("Min Total Degree: " + minTDegree);
		System.out.println("Avg Total Degree: " + ((double)totalTDegree/(double)numNodes));

		System.out.println("Total Out Degree: " + totalODegree);
		System.out.println("Max Out Degree: " + maxODegree);
		System.out.println("Min Out Degree: " + minODegree);
		System.out.println("Avg Out Degree: " + ((double)totalODegree/(double)numNodes));

		System.out.println("Total In Degree: " + totalIDegree);
		System.out.println("Max In Degree: " + maxIDegree);
		System.out.println("Min In Degree: " + minIDegree);
		System.out.println("Avg In Degree: " + ((double)totalIDegree/(double)numNodes));



	}

	private static void generateSimTests(DataSetInterface dsi, String name, Random rand){

		int endSize = 6;
		double complete = 0.4d;
		int rooted = 1; 
		float p = 0.01f; 
		String nodePrefix = "n";
		int idx = 0;


		for (endSize = 6; endSize < 16; endSize = endSize+2){

			if (endSize >= 10){
				complete = 0.15d;
			}

			List<GPHolder> list = new ArrayList<GPHolder>();
			for (int count = 0; count < 1000; count++){
				GPHolder gph = null;
				while (gph == null){
					SimTestGenerator stg = new SimTestGenerator(dsi, rand, endSize, complete,  rooted, p, nodePrefix);
					gph = stg.createDBBasedGP();
				}
				list.add(gph);
			}

			try {
				FileOutputStream fout = new FileOutputStream("simulation-tests/"+name+"/test"+idx+".ser");
				ObjectOutputStream oos = new ObjectOutputStream(fout);
				oos.writeObject(list);
				oos.close();

				idx++;
				System.out.println("Completed: " +idx);
			} catch (IOException e){
				System.out.println("IOException" + e);
			}

		}


	}

	private static DataSetInterface loadDataSet(String fileName){

		DataSet dataSet = null;

		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("simulation-tests/"+fileName+".ser"));
			dataSet = (DataSet) ois.readObject();
			ois.close();
		} catch (IOException e){
			System.out.println("IOException" + e);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DataSetInterface dsi = new DataSetInterface(dataSet);
		return dsi;
	}

	private static void saveDataSet(String fileName, Random random){
		DataSet ds = TxtToDS.readDataSet("simulation-tests/"+fileName+".txt", random);

		try {
			FileOutputStream fout = new FileOutputStream("simulation-tests/"+fileName+".ser");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(ds);
			oos.close();
		} catch (IOException e){
			System.out.println("IOException" + e);
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
}
