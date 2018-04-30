package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * Provides an alternative way for populating the candidates sets.
 * @author szrrizvi
 *
 */
public interface AltStart<N> {

	/**
	 * Populates the candidates maps.
	 * Assumption: The candidates map is empty.
	 * Return false, if the candidates map could not be populated.	 
	 * @param nodes The list of all nodes in the graph pattern
	 * @param candidates The candidates map
	 * @return False if the candidates map could not be populated, else true.
	 * 
	 * Side Effect: candidates will be updated
	 */
	public abstract boolean startPop(List<MyNode> nodes, Map<MyNode, Set<N>> candidates);
}
