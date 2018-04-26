package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyDirection;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

/**
 * This class provides the wrapper for invoking database queries against the database.
 * @author szrrizvi
 *
 */

public class GPAccess implements NeighbourhoodAccess<MyNode>{

	GraphPattern dataset;

	/**
	 * Initilizes the instance variables.
	 * @param graphDb The GraphDatabaseService
	 * @param constraintsChecker The ConstraintsChecker
	 */
	public GPAccess (GraphPattern dataset){
		//Initialize the instance variables
		this.dataset = dataset;
	}

	/**
	 * Returns the set of neighbours, for the given node (with assigned vertex) and the relationship, that satisfy all required constraints.
	 * @param rel The target relationship. Used to specifying the relationship direction and the relationship attribute requirements.
	 * @param node The gp node. Used for specifying the relationship direction and the node attribute requirements.
	 * @param vertex The vertex assigned to the node. Used for actually querying the database.
	 * @return The set of neighbours that satisfy the associated constraints.
	 */
	public Set<MyNode> findNeighbours(MyRelationship rel, MyNode node, MyNode vertex){

		//Setup the checking step
		Set<MyNode> neighbours = new HashSet<MyNode>();					//The list containing the result
		MyNode otherNode = rel.getOther(node);						//The other node in the relationship

		//Prepare the list of edge attribute requirements (if any)
		Map<String, String> eAttrReqs = rel.getAttributes();	
		List<String> eAttrNames = new ArrayList<String>();
		eAttrNames.addAll(eAttrReqs.keySet());

		//Get the direction
		MyDirection dir = null;
		if (rel.getSource().equals(node)){
			dir = MyDirection.OUTGOING;
		} else {
			dir = MyDirection.INCOMING;
		}


		//Query the database for the neighbours of vertex, where direction = dir.
		//Only keep the neighbours where the edge and vertex attributes are satisfied
		Iterable<MyRelationship> result = dataset.getRelationships(vertex, dir);

		for (MyRelationship tempR : result){
			MyNode neighbour = tempR.getOther(vertex);
			neighbours.add(neighbour);

		}

		return neighbours;
	}
}
