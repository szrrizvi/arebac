package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.RelationshipType;

import ca.ucalgary.ispia.graphpatterns.graph.DataSet;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;

/**
 * Converts a input graph dataset to a DataSet object. Essentially we are storing the dataset in memory. 
 * The DataSet stores the graph as adjacency lists.
 * Expected format for input file: Each line has 2 numeric values, representing a relationship. The value is the id for the node.
 * The first value is the source node and the second value is the target node for the relationship. 
 * @author rizvi
 *
 */
public class TxtToDS {

	public static DataSet readDataSet(String fileName, Random random){

		/**
		 * Prepare to read the source file.
		 */
		Scanner scan = null;
		try {
			scan = new Scanner(new FileInputStream(fileName));
		} catch (IOException e){
			System.out.println("File not found: " + fileName);
			return null;
		}
		
		//Map to store the nodes.
		Map<Integer, MyNode> nodesMap = new HashMap<Integer,MyNode>();
		
		//Counters 
		int relCounter = 0;
		int progress = 0;
		int progressNext = 0;
		
		//The graph pattern
		DataSet dataset = new DataSet();
		
		while (scan.hasNext()){
			
			//Read each line, and split it at a white space
			String line = scan.nextLine();
			String[] relInfo = line.split("\\s+");

			//The line should contain two "words", each an ID for a node
			if (relInfo.length != 2){
				System.out.println("Illegal line format: " + line);
				return null;
			}
			
			//Read the IDs for the nodes in the relationship
			int srcID = Integer.parseInt(relInfo[0]);
			int tgtID = Integer.parseInt(relInfo[1]);
			
			MyNode srcNode = null;
			MyNode tgtNode = null;
			
			//If the src node has been seen in the past, then look it up in the nodesMap
			//else create a new node and add it to the nodesMap
			if (nodesMap.containsKey(srcID)){
				srcNode = nodesMap.get(srcID);
			} else {
				srcNode = new MyNode(srcID, "");
				nodesMap.put(srcID, srcNode);
			}
			
			//If the tgt node has been seen in the past, then look it up in the nodesMap
			//else create a new node and add it to the nodesMap
			if (nodesMap.containsKey(tgtID)){
				tgtNode = nodesMap.get(tgtID);
			} else {
				tgtNode = new MyNode(tgtID, "");
				nodesMap.put(tgtID, tgtNode);
			}
			
			//Create the relationship object
			RelType relType = randomRelType(random);
			MyRelationship rel = new MyRelationship(srcNode, tgtNode, relType, relCounter);
			relCounter++;
			
			//Add the relationship to the graph pattern
			dataset.addRelationship(rel);
			
			//Update progress counter
			progressNext++;
			if (progressNext > (progress + 500000)){
				System.out.println(progressNext + ": " +line);
				progress = progressNext;
			}

		}
		
		//Close scanner
		scan.close();
		
		//Print GP size
		System.out.println("Nodes: " + dataset.getNodes().size());
		
		//Add the relationship types
		Set<RelationshipType> relTypes = new HashSet<RelationshipType>();
		relTypes.add(RelType.RelA);
		relTypes.add(RelType.RelB);
		relTypes.add(RelType.RelC);
		relTypes.add(RelType.RelD);
		relTypes.add(RelType.RelE);
		relTypes.add(RelType.RelF);
		relTypes.add(RelType.RelG);
		
		dataset.setRelationshipTypes(relTypes);
		
		//Return dataset
		return dataset;
	}
	
	private static RelType randomRelType(Random random){
		RelType[] types = RelType.values();
		
		int idx = random.nextInt(types.length);
		
		return types[idx];
	}
}
