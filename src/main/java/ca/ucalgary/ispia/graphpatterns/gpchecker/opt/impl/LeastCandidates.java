package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.VariableOrdering;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * This class provides the method(s) for variable ordering.
 * This variable ordering is based on the least number of candidates.
 * @author szrrizvi
 *
 */	
public class LeastCandidates<N> implements VariableOrdering<N>{

	
	private final GraphPattern gp;	//The associated graph pattern
	
	/**
	 * Initializes the instance variables.
	 * @param gp The graph pattern associated with the current problem.
	 */
	public LeastCandidates(GraphPattern gp){
		//Set the instance variables.
		this.gp = gp;
	}
	
	/**
	 * Find nodes such that they are populated but not yet assigned.
	 * Pick and return the node with the smallest candidates size
	 * @param assignments The map of currently assigned nodes and their assignments
	 * @param candidates The map of populated nodes and their candidates
	 * @return The next node to be assigned in the algorithm
	 */
	public MyNode pickNextNode(Map<MyNode, N> assignments, Map<MyNode, Set<N>> candidates){

		List<MyNode> allNodes = gp.getNodes();
		// Find nodes such that they are populated but not yet assigned.
		// Pick the node with the smallest candidates size
		// Optimization idea: When a node is assigned, remove it from candidates. Thus,
		// candidates only consists of unassigned nodes.

		MyNode nextNode = null;
		int candidatesSize = 0;

		//Loop through the nodes
		for (MyNode node : allNodes){
			//If the node is populated but not assigned
			if ((!assignments.containsKey(node)) && (candidates.containsKey(node))){

				//Get the candidates size
				int newSize = candidates.get(node).size();

				//Update nextNode with the current node if nextNode is null or the current node has a smaller candidate size 
				//than the nextNode
				if ((nextNode == null) || (newSize < candidatesSize)){
					nextNode = node;
					candidatesSize = newSize;
				}
			}
		}
		
		return nextNode;
	}
}
