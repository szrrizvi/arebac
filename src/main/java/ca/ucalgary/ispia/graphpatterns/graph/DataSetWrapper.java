package ca.ucalgary.ispia.graphpatterns.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The DataSet class's only function is to hold the large dataset. Removing all other functionality did 
 * seem to help improve the performance of reading the dataset from txt file and saving as serialized object. 
 * This class provides the methods there were removed from the DataSet class. 
 * 
 * Assumption: After a DataSet has been 'initialized' it does not add/remove any relationships/nodes 
 * 
 * @author szrrizvi
 *
 */
public class DataSetWrapper extends DataSet{

	private static final long serialVersionUID = 1L;

	//The adjacency list for incoming relationships. The key is a MyNode, and the value is the list of 
	//relationships where the key is the target node.
	private Map<MyNode, Set<MyRelationship>> incomingRels;
	
	
	/**
	 * Default constructor. Initialize instance variables.
	 */
	public DataSetWrapper(){
		incomingRels = new HashMap<MyNode, Set<MyRelationship>>();
		populateIncomingRels();
	}
	
	/**
	 * Populates the incoming relationships map based on the outgoing relationships map.
	 */
	public void populateIncomingRels(){
		//Get the map of outgoing relationships
		Map<MyNode, Set<MyRelationship>> outgoingRels = super.getOutgoingRels();
		
		for (MyNode key : outgoingRels.keySet()){
			//Iterate through the relationships for each src node
			Set<MyRelationship> relationships = outgoingRels.get(key);
			
			for (MyRelationship rel : relationships){
				//Get the target node
				MyNode target = rel.getTarget();
				
				//Add the incoming relationships to the incomingRels map
				if (incomingRels.containsKey(target)){
					incomingRels.get(target).add(rel);
				} else {
					Set<MyRelationship> tempRels = new HashSet<MyRelationship>();
					tempRels.add(rel);
					incomingRels.put(target, tempRels);
				}
			}
		}
	}
	
	/**
	 * Returns the set of relationships from the given node, based on the given direction.
	 * @param node The node
	 * @param dir The direction of relationships
	 * @return The set of relationships to/from the given node.
	 */
	public Set<MyRelationship> getRelationships(MyNode node, MyDirection dir){
		
		//Initialize result list
		Set<MyRelationship> result = new HashSet<MyRelationship>();	
		
		if (dir == MyDirection.OUTGOING){
			//Outgoing relationships; node = src
			result.addAll(getOutgoingRels().get(node));
		} else if (dir == MyDirection.INCOMING) {
			//Incoming relationships; node = tgt
			result.addAll(incomingRels.get(node));
		} else {
			//Both directions; node = src || node = tgt
			result = getAllRelationships(node);	
		}
		
		return result;
	}
	
	/**
	 * @return The list of all relationships in the graph pattern
	 */
	public Set<MyRelationship> getAllRelationships(){

		Set<MyRelationship> rels = new HashSet<MyRelationship>();
		Map<MyNode, Set<MyRelationship>> outRels = getOutgoingRels();
		
		//Obtain the relationships from the map, and add them to the list.
		for (MyNode node : outRels.keySet()){
			Set<MyRelationship> temp = outRels.get(node);
			rels.addAll(temp);
		}

		return rels;
	}
	
	/**
	 * Returns all of the relationships in the graph pattern that contain the given node
	 * @param node The node
	 * @return all of the relationships in the graph pattern that contain the given node
	 */
	public Set<MyRelationship> getAllRelationships(MyNode node){
		//Initialize result list
		Set<MyRelationship> result = new HashSet<MyRelationship>();

		//Add all outgoing and incoming relationships for the node
		result.addAll(getOutgoingRels().get(node));
		result.addAll(incomingRels.get(node));
		return result;
	}
	
	/**
	 * Checks if the graph pattern contains at least 1 relationship.
	 * @return True if the graph pattern contains 0 relationships, else False. 
	 * NOTE: There cannot be less than 0 relationships.
	 */
	public boolean isEmpty(){

		//Iterate through the key set, and return false when the first 
		//relationship is encountered.
		for (MyNode source : getOutgoingRels().keySet()){
			Set<MyRelationship> rels = getOutgoingRels().get(source);

			if (!rels.isEmpty()){
				return false;
			}
		}
		//The map is exhausted and no relationships found, thus return true.
		return true;
	}

	/**
	 * Returns the total (= in + out) degree for the given node.
	 * @param node The target node.
	 * @return the total (= in + out) degree for the given node. If node is not part of graph pattern, then return -1.
	 */
	public int getDegree(MyNode node){

		//Check if node is part of graph pattern.
		if (!getNodes().contains(node)){
			return -1;
		}

		int degree = getOutgoingRels().get(node).size() + incomingRels.get(node).size();

		return degree;
	}
	
	public String toString(){
		return super.toString();
	}
	
}
