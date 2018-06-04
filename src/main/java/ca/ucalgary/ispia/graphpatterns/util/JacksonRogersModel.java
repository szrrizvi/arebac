package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

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
	Set<Integer> allNodes;
	Set<Integer>[] matrix;
	
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
			Integer newNode = new Integer(nodeCounter);
			nodeCounter++;
			
			//Phase 1 Part 1; Pick mr nodes that already exist in the network.
			List<Integer> allNodesCopy = new ArrayList<Integer>();
			allNodesCopy.addAll(allNodes);
			//These nodes would be the parents of newNode
			List<Integer> parents = new ArrayList<Integer>();
			//Pick mr random parents
			for (int mrCount = 0; mrCount < mr; mrCount++){
				parents.add(allNodesCopy.remove(random.nextInt(allNodesCopy.size())));
			}
			
			//Phase 1 Part 2; Create a relationship between newNode and each parent with probability pr.
			for (Integer parent : parents){
				if (pr > random.nextFloat()){
					
					if (matrix[newNode] == null){
						matrix[newNode] = new HashSet<Integer>();
					}
					matrix[newNode].add(parent);
				}
			}

			
			//Phase 2 Part 1; Pick mn nodes that are connected to the parent nodes (excluding all parents and newNode).
			//These nodes will be the grand parents of newNode
			List<Integer> grandParents = new ArrayList<Integer>();
			List<Integer> candidates = new ArrayList<Integer>();
			//Gather all potential grand parents
			for (Integer parent: parents){
				if (matrix[parent] != null){
					candidates.addAll(matrix[parent]);
				}
			}
			//Filtering
			candidates.remove(newNode);		//newNode cannot be its own grand parent		
			for (Integer parent: parents){
				candidates.remove(parent);	//A parent cannot also be a grand parent
			}
			
			//Pick mn random grand parents
			for (int mnCount = 0; mnCount < mn; mnCount++){
				if (candidates.size()>0){
					grandParents.add(candidates.remove(random.nextInt(candidates.size())));
				}
			}
			
			//Phase 2 Part 2; Create a relationship between newNode and each grand parent with probability pn.
			for (Integer grandParent : grandParents){
				if (pn > random.nextFloat()){
					
					if (matrix[newNode] == null){
						matrix[newNode] = new HashSet<Integer>();
					}
					matrix[newNode].add(grandParent);
				}
			}
			allNodes.add(newNode);
			
		}
		
		
		
		writeSNToFile();
		analysis();
	}
	
	public void slashDotNetwork(){
		if (totalNodes <82168){
			System.out.println("total Nodes less than nodes in seed netword");
			return;
		}
		
		matrix = new HashSet[totalNodes];
		Set<Integer> seenNodes = new HashSet<Integer>();
		
		Scanner scan = null;
		try {
			scan = new Scanner(new FileInputStream("simulation-tests/Slashdot0902.txt"));
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("File not found");
			return;
		}
		
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
			
			seenNodes.add(srcID);
			seenNodes.add(tgtID);
						
			
			if (matrix[srcID] == null){
				Set<Integer> rels = new HashSet<Integer>();
				matrix[srcID] = rels;
			}
			
			matrix[srcID].add(tgtID);
		}
		
		scan.close();
		nodeCounter = seenNodes.size();
		allNodes = new HashSet<Integer>();
		allNodes.addAll(seenNodes);
		analysis();
		return;
	}
	
	private void analysis(){
		int maxTDegree = -1;
		int minTDegree = Integer.MAX_VALUE;
		int totalTDegree = 0;

		int numNodes = 0;
		
		int cnt = 0;

		for (Integer node : allNodes){
			if (node != null){
				
				numNodes++;
				int tDegree = -1;
				if (matrix[node] != null){
					tDegree = matrix[node].size();
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
	
	private void writeSNToFile(){
		PrintWriter pw = null;
		
		try {
			pw = new PrintWriter(new FileOutputStream("nodes.csv"));
		} catch (Exception e){
			e.printStackTrace();
			return;
		}
		pw.println(":ID,id:int,:LABEL");
		for (int idx = 0; idx < matrix.length; idx++){
			pw.println(idx+","+idx+",Person");
		}
		pw.close();
		
		try {
			pw = new PrintWriter(new FileOutputStream("rels.csv"));
		} catch (Exception e){
			e.printStackTrace();
			return;
		}
		pw.println(":START_ID,:END_ID,:TYPE");
		for (int idx = 0; idx < matrix.length; idx++){
			Set<Integer> rels = matrix[idx]; 
			if (rels != null){
				for (Integer tgt : rels){
					pw.println(idx+","+tgt+",RelA");
				}
			}
		}
		pw.close();
	}
}
