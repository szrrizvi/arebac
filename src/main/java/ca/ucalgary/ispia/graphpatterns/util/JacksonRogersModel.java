package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * This class provides the method(s) needed to generate a random social network based on the Jackson and Rogers model. 
 * @author szrrizvi
 *
 */
public class JacksonRogersModel {
	
	private int totalNodes;		//The total number of nodes in the final product
	
	//The 4 parameters for the model
	private int mr;				//# of nodes to match in step 1
	private int mn;				//# of nodes to match in step 2
	private float pr;			//Probability of relationship for step 1
	private float pn;			//Probability of relationship for step 1
	
	private int nodeCounter;	//Node Counter, also used for ids
	
	Random random;				//PRNG
	
	//The network state,
	Set<MyNode> allNodes;
	Set<MyNode>[] matrix;
	
	/**
	 * Constructor. Initialize the instance variables.
	 * @param totalNodes The total number of nodes in the final product
	 * @param mr # of nodes to match in step 1
	 * @param mn # of nodes to match in step 2
	 * @param pr Probability of relationship for step 1
	 * @param pn Probability of relationship for step 2
	 * @param random PRNG
	 */
	public JacksonRogersModel(int totalNodes, int mr, int mn, float pr, float pn, Random random){
		this.totalNodes = totalNodes;
		
		this.mr = mr;
		this.mn = mn;
		this.pr = pr;
		this.pn = pn;
		
		this.random = random;	
		
	}
	
	/**
	 * Generates the social network model. Assumption: maxtrix and allNodes are already populated with the seed network.
	 */
	public void generateModdel(){
		if (matrix == null || allNodes == null){
			System.out.println("Seed Network is still empty");
			return;
		}
		
		int prevX = nodeCounter;
		
		//Generate the model
		for (int count = nodeCounter; count < totalNodes; count++){
			//Create a new node
			MyNode newNode = new MyNode(nodeCounter, "Person");
			nodeCounter++;
			
			//Phase 1 Part 1; Pick mr nodes that already exist in the network.
			List<MyNode> allNodesCopy = new ArrayList<MyNode>();
			allNodesCopy.addAll(allNodes);
			//These nodes would be the parents of newNode
			List<MyNode> parents = new ArrayList<MyNode>();
			//Pick mr random parents
			for (int mrCount = 0; mrCount < mr; mrCount++){
				parents.add(allNodesCopy.remove(random.nextInt(allNodesCopy.size())));
			}
			
			//Phase 1 Part 2; Create a relationship between newNode and each parent with probability pr.
			for (MyNode parent : parents){
				if (pr > random.nextFloat()){
					
					if (matrix[newNode.getId()] == null){
						matrix[newNode.getId()] = new HashSet<MyNode>();
					}
					matrix[newNode.getId()].add(parent);
				}
			}

			
			//Phase 2 Part 1; Pick mn nodes that are connected to the parent nodes (excluding all parents and newNode).
			//These nodes will be the grand parents of newNode
			List<MyNode> grandParents = new ArrayList<MyNode>();
			List<MyNode> candidates = new ArrayList<MyNode>();
			//Gather all potential grand parents
			for (MyNode parent: parents){
				if (matrix[parent.getId()] != null){
					candidates.addAll(matrix[parent.getId()]);
				}
			}
			//Filtering
			candidates.remove(newNode);		//newNode cannot be its own grand parent		
			for (MyNode parent: parents){
				candidates.remove(parent);	//A parent cannot also be a grand parent
			}
			
			//Pick mn random grand parents
			for (int mnCount = 0; mnCount < mn; mnCount++){
				if (candidates.size()>0){
					grandParents.add(candidates.remove(random.nextInt(candidates.size())));
				}
			}
			
			//Phase 2 Part 2; Create a relationship between newNode and each grand parent with probability pn.
			for (MyNode grandParent : grandParents){
				if (pn > random.nextFloat()){
					
					if (matrix[newNode.getId()] == null){
						matrix[newNode.getId()] = new HashSet<MyNode>();
					}
					matrix[newNode.getId()].add(grandParent);
				}
			}
			allNodes.add(newNode);
			
		}
		analysis();
	}
	
	public void slashDotNetwork(){
		if (totalNodes <82168){
			System.out.println("total Nodes less than nodes in seed netword");
			return;
		}
		
		matrix = new HashSet[totalNodes];
		
		Scanner scan = null;
		try {
			scan = new Scanner(new FileInputStream("simulation-tests/Slashdot0902.txt"));
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("File not found");
			return;
		}
		
		Map<Integer, MyNode> nodesMap = new HashMap<Integer, MyNode>();
		
		while (scan.hasNext()){
			String line = scan.nextLine();
			String[] relInfo = line.split("\\s+");

			//The line should contain two "words", each an ID for a node
			if (relInfo.length != 2){
				System.out.println("Illegal line format: " + line);
				scan.close();
				return;
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
			
			if (matrix[srcID] == null){
				Set<MyNode> rels = new HashSet<MyNode>();
				matrix[srcID] = rels;
			}
			
			matrix[srcID].add(tgtNode);
		}
		
		scan.close();
		nodeCounter = nodesMap.size();
		allNodes = new HashSet<MyNode>();
		for (Integer key : nodesMap.keySet()){
			allNodes.add(nodesMap.get(key));
		}
		analysis();
		return;
	}
	
	private void analysis(){
		int maxTDegree = -1;
		int minTDegree = Integer.MAX_VALUE;
		int totalTDegree = 0;

		int numNodes = 0;
		
		int cnt = 0;

		for (MyNode node : allNodes){
			if (node != null){
				
				numNodes++;
				int tDegree = -1;
				if (matrix[node.getId()] != null){
					tDegree = matrix[node.getId()].size();
				} else {
					tDegree = 0;
				}
				

				
				if (tDegree > maxTDegree){
					maxTDegree = tDegree;
				}
				if (tDegree < minTDegree){
					minTDegree = tDegree;
				}

				totalTDegree += tDegree;
			}
		}
		
		System.out.println(numNodes);

		System.out.println("Total Total Degree: " + totalTDegree);
		System.out.println("Max Total Degree: " + maxTDegree);
		System.out.println("Min Total Degree: " + minTDegree);
		System.out.println("Avg Total Degree: " + ((double)totalTDegree/(double)numNodes));
	}
}
