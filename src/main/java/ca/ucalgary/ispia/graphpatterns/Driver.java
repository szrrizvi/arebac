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
import ca.ucalgary.ispia.graphpatterns.graph.DataSetWrapper;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestGenerator;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestRunner;
import ca.ucalgary.ispia.graphpatterns.tests.TxtToGP;
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


		/*Random rand = new Random(1535364);
		saveDataSet("Slashdot0902", rand);
		DataSetWrapper dsw = loadDataSet("Slashdot0902");
		generateSimTests(dsw, "Slashdot0902", rand);
		*/
		
		DataSetInterface dsi = loadDataSet("Slashdot0902");
		SimTestRunner str = new SimTestRunner(dsi);
		try {
			str.runTest("simulation-tests/Slashdot0902", 1, 10);
			//str.runTests(5, "simulation-tests/Slashdot0902");
		} catch (Exception e){
			e.printStackTrace();
		}
		

	}

	private static void generateSimTests(DataSetWrapper dsw, String name, Random rand){

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
					SimTestGenerator stg = new SimTestGenerator(dsw, rand, endSize, complete,  rooted, p, nodePrefix);
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
		DataSet ds = TxtToGP.readDataSet("simulation-tests/"+fileName+".txt", random);

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
