package ca.ucalgary.ispia.graphpatterns.gpchecker;

import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * Interface for a Graph Pattern checker class. A class implementing this interface must ensure that
 * all data required for evaluating a graph pattern query is provided. 
 * @author szrrizvi
 *
 */
public interface GPChecker<N, E> {
	/**
	 * Evaluates the stored graph pattern.
	 * @return The list of all maps, from graph pattern nodes to the database nodes, that together satisfy the pattern.
	 */
	public List<Map<MyNode, N>> check();
	
	/**
	 * Internal debugging/analyzing method. Used for counting the number of database invokactions. 
	 * @return The number of times the database has been invoked.
	 */
	public int getQueryCount();
	
	public int getMaxNeighbourhood();
	
	public int getAllRes();
	
	public int getSearchSpace();
}
