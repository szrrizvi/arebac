package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;

public class TestGeneration {
	public static void generatePokecSimTests(GraphDatabaseService graphDb){
		Random rand = new Random(2556021);

		int[] sizes = {10, 11, 13};
		int[] attrs = {0};
		int[] mex = {0};
		int[] resSizes = {1, 2, 4};
		int count = 1;
		List<GPHolder> tests = new ArrayList<GPHolder>();
		for (int i = 0; i < 250; i++){
			int size = sizes[rand.nextInt(sizes.length)];
			int vattrs = attrs[rand.nextInt(attrs.length)];
			int eattrs = attrs[rand.nextInt(attrs.length)];
			int mexs = mex[rand.nextInt(mex.length)];
			int resSize = resSizes[rand.nextInt(resSizes.length)];
			SubgraphGenerator sg = new SubgraphGenerator(graphDb, 82168, rand, size, 1.5d, 1, mexs, vattrs, eattrs, resSize);

			GPHolder gph = sg.createDBBasedGP();
			if (gph == null){
				i--;
			} else {
				tests.add(gph);
			}
		}

		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("simulation-tests/soc-pokectests/testCase-"+count+".ser"));
			count++;
			oos.writeObject(tests);
			oos.close();
		} catch (Exception e){
			e.printStackTrace();
			return;
		}

	}
}
