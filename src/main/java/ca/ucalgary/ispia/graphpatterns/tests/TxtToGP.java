package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;

/**
 * Converts a input graph dataset to graph patterns. Essentially we are storing the dataset in memory. 
 * The graph patterns store the dataset as adjacency lists.
 * Expected format for input file: Each line has 2 numeric values, representing a relationship. The value is the id for the node.
 * The first value is the source node and the second value is the target node for the relationship. 
 * @author rizvi
 *
 */
public class TxtToGP {

	public static GraphPattern readDataSet(String fileName){

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
		Map<String, MyNode> nodesMap = new HashMap<String,MyNode>();
		
		//Counters 
		int relCounter = 0;
		int progress = 0;
		int progressNext = 0;
		
		//The graph pattern
		GraphPattern gp = new GraphPattern();
		
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
			String srcID = relInfo[0];
			String tgtID = relInfo[1];
			
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
			MyRelationship rel = new MyRelationship(srcNode, tgtNode, RelType.RelA, relCounter+"");
			relCounter++;
			
			//Add the relationship to the graph pattern
			gp.addRelationship(rel);
			
			//Update progress counter
			progressNext++;
			if (progressNext > (progress + 1000)){
				System.out.println(progressNext + ": " +line);
				progress = progressNext;
			}

		}
		
		//Close scanner
		scan.close();
		
		//Print GP size
		System.out.println("Nodes: " + gp.getNodes().size());
		System.out.println("Relationships: " + gp.getAllRelationships().size());
		
		//Return GP
		return gp;
	}
}
