package ca.ucalgary.ispia.graphpatterns.graph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class DataSetInterface {
	private Set<MyRelationship>[][] matrixOut;
	private Set<MyRelationship>[][] matrixIn;
	private MyNode[] nodes;

	public DataSetInterface(DataSet dataSet){

		int numNodes = dataSet.getNodes().size()+1;
		int numRelTypes = dataSet.getRelTypes().size();
		matrixOut = new HashSet[numNodes][numRelTypes];
		matrixIn = new HashSet[numNodes][numRelTypes];
		nodes = new MyNode[numNodes];
		
		populateNodes(dataSet);
		populateMatrix(dataSet);
	}
	
	private void populateNodes(DataSet dataSet){
		Set<MyNode> input = dataSet.getNodes();
		
		for (MyNode node : input){
			nodes[node.getId()] = node;
		}
	}

	private void populateMatrix(DataSet dataSet){
		//Get the map of outgoing relationships
		Map<MyNode, Set<MyRelationship>> relationships = dataSet.getOutgoingRels();

		for (MyNode key : relationships.keySet()){
			//Iterate through the relationships for each src node
			Set<MyRelationship> rels = relationships.get(key);

			for (MyRelationship rel : rels){
				int srcNodeIdx = key.getId();
				int tgtNodeIdx = rel.getTarget().getId();
				int relIdIdx = rel.getIdentifier().getIdx();
				
				if (matrixOut[srcNodeIdx][relIdIdx] == null){
					matrixOut[srcNodeIdx][relIdIdx] = new HashSet<MyRelationship>();
				}
				if (matrixIn[tgtNodeIdx][relIdIdx] == null){
					matrixIn[tgtNodeIdx][relIdIdx] = new HashSet<MyRelationship>();
				}
				
				matrixOut[srcNodeIdx][relIdIdx].add(rel);
				matrixIn[tgtNodeIdx][relIdIdx].add(rel);
			}
		}
	}
	
	/**
	 * Returns the set of relationships from the given node, based on the given direction.
	 * @param node The node
	 * @param dir The direction of relationships
	 * @return The set of relationships to/from the given node.
	 */
	public Set<MyRelationship> getRelationships(MyNode node, RelType relType, MyDirection dir){
		Set<MyRelationship> temp;
		
		if (dir == MyDirection.OUTGOING){
			//Outgoing relationships; node = src
			temp =  matrixOut[node.getId()][relType.getIdx()];
		} else if (dir == MyDirection.INCOMING) {
			//Incoming relationships; node = tgt
			temp =  matrixIn[node.getId()][relType.getIdx()];
		} else {
			//Both directions; node = src || node = tgt
			temp = new HashSet<MyRelationship>();
			temp.addAll(matrixOut[node.getId()][relType.getIdx()]);
			temp.addAll(matrixIn[node.getId()][relType.getIdx()]);
		}
		if (temp == null){
			temp = new HashSet<MyRelationship>();
		}
		return temp;
		
		
	}
	
	/**
	 * Returns all of the relationships in the graph pattern that contain the given node
	 * @param node The node
	 * @return all of the relationships in the graph pattern that contain the given node
	 */
	public Set<MyRelationship> getAllRelationships(MyNode node){
		
		//Initialize result list
		Set<MyRelationship> result = new HashSet<MyRelationship>();

		if (node == null){
			return result;
		}
		
		//Add all outgoing and incoming relationships for the node
		for (int idx = 0; idx < matrixOut[node.getId()].length; idx++){
			if (matrixOut[node.getId()][idx] != null){
				result.addAll(matrixOut[node.getId()][idx]);
			}
		}
		
		for (int idx = 0; idx < matrixIn[node.getId()].length; idx++){
			if (matrixIn[node.getId()][idx] != null){
				result.addAll(matrixIn[node.getId()][idx]);
			}
		}
		
		return result;
	}
	
	public int getInDegree(MyNode node){
		int total = 0;
		for (int idx = 0; idx < matrixIn[node.getId()].length; idx++){
			if (matrixIn[node.getId()][idx] != null){
				total += matrixIn[node.getId()][idx].size();
			}
		}
		
		return total;
	}
	
	public int getOutDegree(MyNode node){
		int total = 0;
		for (int idx = 0; idx < matrixOut[node.getId()].length; idx++){
			if (matrixOut[node.getId()][idx] != null){
				total += matrixOut[node.getId()][idx].size();
			}
		}
		
		return total;
	}
	
	public int getTotalDegree(MyNode node){
		return getInDegree(node) + getOutDegree(node);
	}
	
	public MyNode findNode(int id){
		return nodes[id];
	}
	
	public MyNode[] getNodes(){
		return this.nodes;
	}
}
