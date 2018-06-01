package ca.ucalgary.ispia.graphpatterns.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;

public class JacksonRogersModel {
	
	private int totalNodes;
	private int baseNodes;
	private int mr;
	private int mn;
	
	private int generateNodes;
	private int nodeCounter;
	private int relCounter;
	
	private float pr;
	private float pn;
	
	Random random;
	
	Set<MyNode> allNodes;
	Set<MyNode>[] matrix;
	
	
	public JacksonRogersModel(int totalNodes, int baseNodes, int mr, int mn, float pr, float pn, Random random){
		this.totalNodes = totalNodes;
		this.baseNodes = baseNodes;
		this.generateNodes = totalNodes - baseNodes;
		
		this.mr = mr;
		this.mn = mn;
		this.pr = pr;
		this.pn = pn;
		
		this.random = random;	
		
	}
	
	public void generateModdel(){
		if (matrix == null){
			System.out.println("Seed Network is still empty");
			return;
		}
		
		for (int count = 0; count < generateNodes; count++){
			MyNode newNode = new MyNode(nodeCounter, "Person");
			nodeCounter++;
			
			//Phase 1 Part 1; Pick mr nodes that already exist in the network.
			List<MyNode> allNodesCopy = new ArrayList<MyNode>();
			allNodesCopy.addAll(allNodes);
			
			List<MyNode> parents = new ArrayList<MyNode>();
			for (int mrCount = 0; mrCount > mr; mrCount++){
				parents.add(allNodesCopy.remove(random.nextInt(allNodes.size())));
			}
			
			//Phase 1 Part 2; Create a relationship between newNode and each parent with probability pr.
			for (MyNode parent : parents){
				if (pr > random.nextFloat()){
					MyRelationship rel = new MyRelationship(newNode, parent, RelType.RelA, relCounter);
					relCounter++;
					
					if (matrix[newNode.getId()] == null){
						matrix[newNode.getId()] = new HashSet<MyNode>();
					}
					matrix[newNode.getId()].add(parent);
				}
			}
			
			//Phase 2 Part 1; Pick mn nodes that are connected to the parent nodes (excluding all parents and newNode).
			List<MyNode> grandParents = new ArrayList<MyNode>();
			List<MyNode> candidates = new ArrayList<MyNode>();
			for (MyNode parent: parents){
				candidates.addAll(matrix[parent.getId()]);
			}
			candidates.remove(newNode);
			for (MyNode parent: parents){
				candidates.remove(parent);
			}
			
			for (int mnCount = 0; mnCount < mn; mnCount++){
				grandParents.add(candidates.remove(random.nextInt(candidates.size())));
			}
			
			//Phase 2 Part 2; Create a relationship between newNode and each grand parent with probability pn.
			for (MyNode grandParent : grandParents){
				if (pn > random.nextFloat()){
					MyRelationship rel = new MyRelationship(newNode, grandParent, RelType.RelA, relCounter);
					relCounter++;
					
					if (matrix[newNode.getId()] == null){
						matrix[newNode.getId()] = new HashSet<MyNode>();
					}
					matrix[newNode.getId()].add(grandParent);
				}
			}
		}
	}
	
	public void randomBase(int p){
		matrix = new HashSet[totalNodes];
	}
}
