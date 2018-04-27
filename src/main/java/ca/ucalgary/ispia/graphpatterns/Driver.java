package ca.ucalgary.ispia.graphpatterns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import ca.ucalgary.ispia.graphpatterns.graph.DataSet;
import ca.ucalgary.ispia.graphpatterns.graph.DataSetWrapper;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.tests.SimTestGenerator;
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
		//System.out.println("ENDING");
		
		//saveDataSet("Slashdot0902");
		testDataSet("Slashdot0902");
		
	}
	
	private static void testDataSet(String fileName){
		
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
		
		DataSetWrapper dsw = new DataSetWrapper(dataSet);
		MyNode node = dsw.getNodes().iterator().next();
		Set<MyRelationship> rels = dsw.getAllRelationships(node);
		System.out.println(dsw.getDegree(node));
		for (MyRelationship r : rels){
			System.out.println(r);
		}
		
		SimTestGenerator stg = new SimTestGenerator(dsw, new Random(), 13, 0.1d,  1, 0.5f, "nodes");
		
		GPHolder gph = null;
		while (gph == null){
			gph = stg.createDBBasedGP();
		}
		System.out.println(gph.getGp());
	}
	
	private static void saveDataSet(String fileName){
		DataSet ds = TxtToGP.readDataSet("simulation-tests/"+fileName+".txt");
		
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