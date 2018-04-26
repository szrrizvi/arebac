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

		Scanner scan = null;

		try {
			scan = new Scanner(new FileInputStream(fileName));
		} catch (IOException e){
			System.out.println("File not found: " + fileName);
			return null;
		}

		Map<String, MyNode> nodesMap = new HashMap<String,MyNode>();
		int relCounter = 0;
		
		int progress = 0;
		int progressNext = 0;
		
		GraphPattern gp = new GraphPattern();
		
		while (scan.hasNext()){
			String line = scan.nextLine();

			String[] relInfo = line.split("\\s+");

			if (relInfo.length != 2){
				System.out.println("Illegal line format: " + line);
				return null;
			}
			
			String srcID = relInfo[0];
			String tgtID = relInfo[1];
			
			MyNode srcNode = null;
			MyNode tgtNode = null;
			
			if (nodesMap.containsKey(srcID)){
				srcNode = nodesMap.get(srcID);
			} else {
				srcNode = new MyNode(srcID, "");
				nodesMap.put(srcID, srcNode);
			}
			
			if (nodesMap.containsKey(tgtID)){
				tgtNode = nodesMap.get(tgtID);
			} else {
				tgtNode = new MyNode(tgtID, "");
				nodesMap.put(tgtID, tgtNode);
			}
			
			MyRelationship rel = new MyRelationship(srcNode, tgtNode, RelType.RelA, relCounter+"");
			relCounter++;
			
			gp.addRelationship(rel);
			
			progressNext++;
			if (progressNext > (progress + 1000)){
				System.out.println(progressNext + ": " +line);
				progress = progressNext;
			}

		}
		
		scan.close();
		
		System.out.println("Nodes: " + gp.getNodes().size());
		System.out.println("Relationships: " + gp.getAllRelationships().size());
		return gp;
	}
}
