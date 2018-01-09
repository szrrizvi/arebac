package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * Provides the method(s) for variable ordering.
 * @author szrrizvi
 *
 */	
public interface VariableOrdering {

	/**
	 * For the associated graph pattern, finds the next node such that is it populated but not yet assigned.
	 * @param assignments The map of currently assigned nodes and their assignments.
	 * @param candidates The map of populated nodes and their candidates.
	 * @return The next node to be assigned in the algorithm.
	 */
	public MyNode pickNextNode(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates);
}
