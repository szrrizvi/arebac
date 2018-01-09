package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a graph pattern. The relationships are stored as an adjacency list.
 * @author szrrizvi
 *
 */
public class GraphPattern implements Serializable{
	
	private static final long serialVersionUID = -3746611137149867000L;

	//The adjacency list. The key is the the source node, and the value is the list of 
	//relationships emerging from that node.
	private Map<MyNode, List<MyRelationship>> map;
	
	//The list of nodes
	private List<MyNode> nodes;
	
	/**
	 * Default constructor.
	 */
	public GraphPattern(){
		//Initialize the map and the list of nodes
		map = new HashMap<MyNode, List<MyRelationship>>();
		nodes = new ArrayList<MyNode>();
	}
	
	/**
	 * Makes a copy of the given gp; Note that this does not duplicate the MyNode and MyRelationship
	 * objects, but on the list and map containing the information.
	 * @param gp
	 */
	public GraphPattern(GraphPattern gp){
		this(); //initialize the list and map
		
		//Make a copy of the nodes list
		List<MyNode> nodes = gp.getNodes();
		for (MyNode node : nodes){
			addNode(node);
		}
		
		//Make a copy of the relationships map
		List<MyRelationship> rels = gp.getAllRelationships();
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
	 * The the relationship to the graph pattern.
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
		
		//Add the relationship to the map (adjacency list).
		if (map.containsKey(source)){
			//If the map already contains the source node as a key,
			//add the relationship to the value list.
			map.get(source).add(rel);
		} else {
			//If the map doesn't already contain the source node as a key,
			//Generate the value list and add the given relationship.
			List<MyRelationship> list = new ArrayList<MyRelationship>();
			list.add(rel);
			
			//Put the key, value pair in the map
			map.put(source, list);
		}
		
	}
	
	/**
	 * Returns the key set for the adjacency list. IE returns the set of nodes that have at least 1 outgoing
	 * edge.
	 * @return The key set for the adjacency list.
	 */
	public Set<MyNode> keySet(){
		return map.keySet();
	}
	
	/**
	 * Returns the set of outgoing relationships from the given node.
	 * @param node The target node
	 * @return The set of outgoing relationships from the given node.
	 */
	public List<MyRelationship> getRelationships(MyNode node){
		return map.get(node);
	}
	
	/**
	 * @return The set of nodes
	 */
	public List<MyNode> getNodes(){
		return nodes;
	}
	
	/**
	 * Checks if the graph pattern contains at least 1 relationship.
	 * @return True if the graph pattern contains 0 relationships, else False. 
	 * NOTE: There cannot be less than 0 relationships.
	 */
	public boolean isEmpty(){
		
		//Iterate through the key set, and return false when the first 
		//relationship is encountered.
		for (MyNode source : map.keySet()){
			List<MyRelationship> rels = map.get(source);
			
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
	public List<MyRelationship> getAllRelationships(){

		List<MyRelationship> rels = new ArrayList<MyRelationship>();

		//Obtain the relationships from the map, and add them to the list.
		for (MyNode node : map.keySet()){
			List<MyRelationship> temp = map.get(node);
			rels.addAll(temp);
		}

		return rels;
	}
	
	/**
	 * Returns all of the relationships in the graph pattern that contain the given node
	 * @param node The target node
	 * @return all of the relationships in the graph pattern that contain the given node
	 */
	public List<MyRelationship> getAllRelationships(MyNode node){
		//Initialize result list
		List<MyRelationship> result = new ArrayList<MyRelationship>();
		
		//Get all relationships
		List<MyRelationship> rels = getAllRelationships();
		
		//Copy the relationships where node is either the source or the target.
		for (MyRelationship rel : rels){
			if (rel.getSource().equals(node) || rel.getTarget().equals(node)){
				result.add(rel);
			}
		}
		
		return result;
	}
	
	/**
	 * Removes the given relationship from the graph pattern
	 * @param rel
	 * @return rel if the relationship was removed, else null
	 */
	public MyRelationship removeRelationship(MyRelationship rel){
		
		//Get the source node of rel and check if it is in the graph pattern
		MyNode source = rel.getSource();
		if (!nodes.contains(source)){
			//If the source node is not found, then return null
			return null;
		}
		
		//If the source node was found, get its relationships
		List<MyRelationship> rels = getRelationships(source);
		
		//If the given relationship is found in rels, the remove it
		if (rels.contains(rel)){
			rels.remove(rel);
		} else {
			//If the given relationship is not found in rels, then return null
			return null;
		}
		
		return rel;
	}
	
	
	/**
	 * Returns the total (= in + out) degree for the given node.
	 * @param node The target node.
	 * @return the total (= in + out) degree for the given node. If node is not part of graph pattern, then return -1.
	 */
	public int getDegree(MyNode node){
		
		//Check if node is part of graph pattern.
		if (!nodes.contains(node)){
			return -1;
		}
		
		//Get all relationships.
		List<MyRelationship> rels = getAllRelationships();
		
		//degree counter. increment by 1 when node is either the source or target
		//for the relationship.
		int degree = 0;
		for (MyRelationship r : rels){
			if (r.getSource().equals(node) || r.getTarget().equals(node)){
				degree++;
			}
		}
				
		return degree;
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
		for (MyNode source : map.keySet()){
			List<MyRelationship> list = map.get(source);
			
			for (MyRelationship rel : list){
				str.append(rel.toString()+"\n");
			}
		}
		
		return str.toString();
	}
	
}
