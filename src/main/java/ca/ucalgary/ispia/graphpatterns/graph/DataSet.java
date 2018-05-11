package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.RelationshipType;

/**
 * Represents a graph pattern. The relationships are stored as an adjacency list.
 * @author szrrizvi
 *
 */
public class DataSet implements Serializable{

	private static final long serialVersionUID = 1L;

	//The adjacency list for outgoing relationships. The key is a MyNode, and the value is the list of 
	//relationships where the key is the source node.
	private Map<MyNode, Set<MyRelationship>> outgoingRels;

	//The list of nodes
	private Set<MyNode> nodes;
	
	private Set<RelationshipType> relTypes;

	/**
	 * Default constructor.
	 */
	public DataSet(){
		//Initialize the map and the list of nodes
		outgoingRels = new HashMap<MyNode, Set<MyRelationship>>();
		nodes = new HashSet<MyNode>();
		relTypes = new HashSet<RelationshipType>();
	}

	/**
	 * Makes a copy of the given gp; Note that this does not duplicate the MyNode and MyRelationship
	 * objects, but on the list and map containing the information.
	 * @param gp
	 */
	public DataSet(DataSet gp){
		this(); //initialize the list and map

		//Make a copy of the nodes list
		Set<MyNode> nodes = gp.getNodes();
		for (MyNode node : nodes){
			addNode(node);
		}

		//Make a copy of the relationships map
		Set<MyRelationship> rels = gp.getAllRelationships();
		for (MyRelationship rel : rels){
			addRelationship(rel);
		}
	}

	/**
	 * Add the node to the nodes list, as long as the node already doesn't exist in the list.
	 * @param node The node to add to the nodes list.
	 */
	public void addNode(MyNode node){
		if (!nodes.contains(node)){
			nodes.add(node);
		}
	}

	/**
	 * Add the relationship to the graph pattern.
	 * @param rel The relationship to add.
	 */
	public void addRelationship(MyRelationship rel){

		//Get the source and target nodes,
		MyNode source = rel.getSource();
		MyNode target = rel.getTarget();

		//If the nodes list doesn't contain the source and target nodes, add them.
		if (!nodes.contains(source)){
			nodes.add(source);
		}
		if (!nodes.contains(target)){
			nodes.add(target);
		}

		//Add the relationship to the outgoing Rels map (adjacency list).
		if (outgoingRels.containsKey(source)){
			//If the map already contains the source node as a key,
			//add the relationship to the value list.
			outgoingRels.get(source).add(rel);
		} else {
			//If the map doesn't already contain the source node as a key,
			//Generate the value list and add the given relationship.
			Set<MyRelationship> list = new HashSet<MyRelationship>();
			list.add(rel);

			//Put the key, value pair in the map
			outgoingRels.put(source, list);
		}

	}

	/**
	 * Returns the key set for the outgoing relationships. IE returns the set of nodes that have at least 1 outgoing
	 * edge.
	 * @return The key set for the adjacency list.
	 */
	public Set<MyNode> srcKeySet(){
		return outgoingRels.keySet();
	}


	/**
	 * @return The set of nodes
	 */
	public Set<MyNode> getNodes(){
		return nodes;
	}
	
	public void setRelationshipTypes(Set<RelationshipType> relTypes){
		this.relTypes = relTypes;
	}
	
	public Set<RelationshipType> getRelTypes(){
		return this.relTypes;
	}
	
	/**
	 * @return The map of outgoing relationships
	 */
	public Map<MyNode, Set<MyRelationship>> getOutgoingRels(){
		return outgoingRels;
	}

	/**
	 * Checks if the graph pattern contains at least 1 relationship.
	 * @return True if the graph pattern contains 0 relationships, else False. 
	 * NOTE: There cannot be less than 0 relationships.
	 */
	public boolean isEmpty(){

		//Iterate through the key set, and return false when the first 
		//relationship is encountered.
		for (MyNode source : outgoingRels.keySet()){
			Set<MyRelationship> rels = outgoingRels.get(source);

			if (!rels.isEmpty()){
				return false;
			}
		}
		//The map is exhausted and no relationships found, thus return true.
		return true;
	}

	/**
	 * @return The list of all relationships in the graph pattern
	 */
	public Set<MyRelationship> getAllRelationships(){

		Set<MyRelationship> rels = new HashSet<MyRelationship>();

		//Obtain the relationships from the map, and add them to the list.
		for (MyNode node : outgoingRels.keySet()){
			Set<MyRelationship> temp = outgoingRels.get(node);
			rels.addAll(temp);
		}

		return rels;
	}



	/**
	 * @return A human readable representation of the graph pattern
	 */
	public String toString(){

		StringBuilder str = new StringBuilder("Graph Pattern:\n");

		//Add the nodes info
		for (MyNode node : nodes){
			str.append(node + "\n");
		}

		//Add the relationships info
		for (MyNode source : outgoingRels.keySet()){
			Set<MyRelationship> list = outgoingRels.get(source);

			for (MyRelationship rel : list){
				str.append(rel.toString()+"\n");
			}
		}

		return str.toString();
	}

}
