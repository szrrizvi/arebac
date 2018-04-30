package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.Set;


import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

/**
 * Provides the wrapper for invoking database queries against the database.
 * @author szrrizvi
 *
 */
public interface NeighbourhoodAccess<N> {

	/**
	 * Returns the set of neighbours, for the given node (with assigned vertex) and the relationship, that satisfy all required constraints.
	 * @param rel The target relationship. Used to specifying the relationship direction and the relationship attribute requirements.
	 * @param node The gp node. Used for specifying the relationship direction and the node attribute requirements.
	 * @param vertex The vertex assigned to the node. Used for actually querying the database.
	 * @return The set of neighbours that satisfy the associated constraints.
	 */
	public abstract Set<N> findNeighbours(MyRelationship rel, MyNode node, N vertex);
	
	public abstract N findNode(MyNode src);
	
	public abstract N findNode(MyNode src, Integer id);
	
	public abstract boolean relationshipExists (N src, N tgt, MyRelationship rel);
}
