package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

public class GPAnalysis {

	/**
	 * Runs the GPH Test cases.
	 * Precondition: Each file contains a list of GPHolder objects.
	 * @param fileNamePrefix The name of the file: fileNamePrefix + "-" + i + ".ser"
	 * @param numProfiles The number of files to read (starting at 1)
	 */
	public static void main(String[] args){
		List<GPHolder> tests = new ArrayList<GPHolder>();
		for (int i = 1; i <= 6; i++){
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new FileInputStream("performance-tests/testCase-" + i + ".ser"));
				tests.addAll((List<GPHolder>) ois.readObject());
				ois.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		//analyzeAvgDegree(tests);
		analyzeDiameter(tests);
		
	}
	
	private static void analyzeAvgDegree(List<GPHolder> tests){
		Map<Float, Integer> avgDegree = new HashMap<Float, Integer>();
		
		for (GPHolder test : tests){
			GraphPattern gp = test.getGp();
			
			float nodes = gp.getNodes().size() * 1.0f;
			float edges = gp.getAllRelationships().size() * 2.0f;
			
			float degreeVal = edges/nodes;
			
			if (avgDegree.containsKey(degreeVal)){
				int count = avgDegree.get(degreeVal);
				avgDegree.put(degreeVal, count+1);
			} else {
				avgDegree.put(degreeVal, 1);
			}
		}
		
		for (Float key : avgDegree.keySet()){
			System.out.println(key + " " + avgDegree.get(key));
		}
	}
	
	private static void analyzeDiameter(List<GPHolder> tests){
		Map<Integer, Integer> diameters = new HashMap<Integer, Integer>();
		
		for (GPHolder test : tests){
			GraphPattern gp = test.getGp();
			for (MyNode node : test.getGp().getNodes()){
				if (node.hasAttribute("id")){
					Map<MyNode, Integer> distances = dijkstra(test.getGp(), node);
					int max = Collections.max(distances.values());
					
					if (diameters.containsKey(max)){
						int val = diameters.get(max) + 1;
						diameters.put(max, val);
					} else {
						diameters.put(max, 1);
					}
					
					break;
				}
			}
		}
		
		for (Integer key : diameters.keySet()){
			System.out.println(key + " " + diameters.get(key));
		}
	}
	
	private static Map<MyNode, Integer> dijkstra(GraphPattern gp, MyNode startNode){
		Map<MyNode, Integer> distances = new HashMap<MyNode, Integer>();
		Set<MyNode> visitedNodes = new HashSet<MyNode>();
		List<MyNode> allNodes = gp.getNodes();
		
		for (MyNode node : allNodes){
			distances.put(node, Integer.MAX_VALUE);
		}
		distances.put(startNode, 0);
		
		MyNode currNode = startNode;
		boolean done = false;
		
		while(!done){
			visitedNodes.add(currNode);
			List<MyRelationship> rels = gp.getAllRelationships(currNode);
			int newDistance = distances.get(currNode) + 1;
			
			for (MyRelationship rel : rels){
				MyNode otherNode = rel.getOther(currNode);
				
				if (distances.get(otherNode) > newDistance){
					distances.put(otherNode, newDistance);
				}
			}
			
			if (visitedNodes.size() == allNodes.size()){
				done = true;
			} else {
				currNode = pickNextNode(allNodes, visitedNodes, distances);
			}
		}
		
		return distances;
	}
	
	private static MyNode pickNextNode(List<MyNode> allNodes, Set<MyNode> visitedNodes, Map<MyNode, Integer> distances){
		MyNode result = null;
		
		for (MyNode node : allNodes){
			if (!visitedNodes.contains(node)){
				if (result == null || distances.get(node) < distances.get(result)){
					result = node;
				}
			}
		}
		
		return result;
	}
	
}
