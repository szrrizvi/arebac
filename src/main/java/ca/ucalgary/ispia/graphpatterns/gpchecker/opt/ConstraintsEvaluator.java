package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.HasAttributes;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

/**
 * Provides the methods for constraints checking for the graph pattern evaluation.
 * So far the constraints we have are: mutual exclusion constraints and attribute requirements.
 * @author szrrizvi
 *
 */
public interface ConstraintsEvaluator<N, E> {

	/**
	 * Simple constructor. Assigns the instance variables.
	 * @param gph The GPHolder
	 * @param graphDb The GraphDatabaseService
	 */
	public abstract void mexFilter(MyNode variable, Set<N> candidates, Map<MyNode, N> assignments, Map<MyNode, Set<MyNode>> confIn);
	
	/**
	 * Filters the candidates set based on the mutual exclusion constraints and current assignment
	 * @param variable The target graph pattern node
	 * @param node The assignment for the target node
	 * @param candidates The list of candidates for the currently populated nodes
	 */
	public abstract boolean checkAttrs(HasAttributes source, E target);
}
