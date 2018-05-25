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

public class SimTestGenWrapper {
	public static void generateSimTests(GraphDatabaseService graphDb, String name, Random rand){

		int endSize = 6;
		double complete = 0.4d;
		int rooted = 1; 
		float p = 0.015f; 
		int idx = 0;


		for (endSize = 6; endSize < 16; endSize = endSize+2){

			if (endSize >= 10){
				complete = 0.15d;
			}

			List<GPHolder> list = new ArrayList<GPHolder>();
			for (int count = 0; count < 1000; count++){
				GPHolder gph = null;
				while (gph == null){
					EvalTestGenerator etg = new EvalTestGenerator(graphDb, rand, endSize, complete,  rooted, p, 0, 0, 0);
					gph = etg.createDBBasedGP();
				}
				assignResultSchema(gph, rand);
				list.add(gph);
			}

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

		}
	}
	
	private static void assignResultSchema(GPHolder gpHolder, Random random){

		if (gpHolder != null){
			List<MyNode> temp = new ArrayList<MyNode>();
			temp.addAll(gpHolder.getGp().getNodes());

			List<MyNode> resultSchema = new ArrayList<MyNode>();

			int schemaSize = 0;

			while (schemaSize == 0){
				schemaSize = random.nextInt(temp.size());
			}

			for (int count = 0; count < schemaSize; count++){
				MyNode resNode = temp.remove(random.nextInt(temp.size()));
				resultSchema.add(resNode);
			}

			gpHolder.setResultSchema(resultSchema);
		}
	}
}


