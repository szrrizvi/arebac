package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ca.ucalgary.ispia.graphpatterns.graph.DataSet;
import ca.ucalgary.ispia.graphpatterns.graph.DataSetInterface;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public class DataSetUtil {

	public static DataSet loadDataSet(String fileName){

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

		
		return dataSet;
	}

	public static void saveDataSet(String fileName, Random random){
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
	

	public static void dsStats(DataSetInterface dsi){
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
		
		int cnt = 0;
		
		Map<Integer, Integer> freqT = new HashMap<Integer, Integer>();
		Map<Integer, Integer> freqO = new HashMap<Integer, Integer>();
		Map<Integer, Integer> freqI = new HashMap<Integer, Integer>();

		MyNode[] nodes = dsi.getNodes();

		for (MyNode node : nodes){
			if (node != null){
				
				numNodes++;
				
				int tDegree = dsi.getTotalDegree(node);
				int oDegree = dsi.getOutDegree(node);
				int iDegree = dsi.getInDegree(node);

				if (freqT.containsKey(tDegree)){
					int val = freqT.get(tDegree) +1;
					freqT.put(tDegree, val);
				} else {
					freqT.put(tDegree, 1);
				}
				
				if (freqO.containsKey(oDegree)){
					int val = freqO.get(oDegree) +1;
					freqO.put(oDegree, val);
				} else {
					freqO.put(oDegree, 1);
				}
				
				if (freqI.containsKey(iDegree)){
					int val = freqI.get(iDegree) +1;
					freqI.put(iDegree, val);
				} else {
					freqI.put(iDegree, 1);
				}
				
				
				if (tDegree > maxTDegree){
					maxTDegree = tDegree;
				}
				if (tDegree < minTDegree){
					minTDegree = tDegree;
				}
				
				if (oDegree == 394){
					cnt++;
				}
				if (tDegree == 394){
					cnt++;
				}
				if (iDegree == 394){
					cnt++;
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
		
		System.out.println(cnt);
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

		System.out.println("\nFreqT:");
		for (Integer key: freqT.keySet()){
			System.out.println(key + " " + freqT.get(key));
		}
		
		System.out.println("\nFreqO:");
		for (Integer key: freqO.keySet()){
			System.out.println(key + " " + freqO.get(key));
		}
		
		System.out.println("\nFreqI:");
		for (Integer key: freqI.keySet()){
			System.out.println(key + " " + freqI.get(key));
		}
	}
	
	public static void analyzeHubs(DataSetInterface dsi, List<Integer> sizes){
		Map<Integer, Integer> mapOut = new HashMap<Integer, Integer>();
		Map<Integer, Integer> mapIn = new HashMap<Integer, Integer>();
		Map<Integer, Integer> mapTotal = new HashMap<Integer, Integer>();
		
		for (Integer num : sizes){
			mapOut.put(num, 0);
			mapIn.put(num, 0);
			mapTotal.put(num, 0);
		}
		
		MyNode[] nodes = dsi.getNodes();
		for (MyNode node : nodes){
			int out = dsi.getOutDegree(node);
			int in = dsi.getInDegree(node);
			int total = out+in;
			
			for (Integer num : sizes){
				if (out >= num){
					int val = mapOut.get(num) +1;
					mapOut.put(num, val);
				}
				if (in >= num){
					int val = mapIn.get(num) +1;
					mapIn.put(num, val);
				}
				
				if (total >= num){
					int val = mapTotal.get(num) +1;
					mapTotal.put(num, val);
				}
			}
		}
		for (Integer num : sizes){
			System.out.println(num+": " + mapOut.get(num) + ", " + mapIn.get(num) + ", " + mapTotal.get(num));
		}
	}
}
