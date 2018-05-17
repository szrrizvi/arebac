package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.graph.DataSetInterface;
import ca.ucalgary.ispia.graphpatterns.graph.MyDirection;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

/**
 * This class provides the wrapper for invoking database queries against the database.
 * @author szrrizvi
 *
 */

public class DSAccess implements NeighbourhoodAccess<MyNode>{

	private DataSetInterface dataset;
	private Map<Integer, Integer> neighbourhoodSizes; 

	

	/**
	 * Initilizes the instance variables.
	 * @param graphDb The GraphDatabaseService
	 * @param constraintsChecker The ConstraintsChecker
	 */
	public DSAccess (DataSetInterface dataset){
		//Initialize the instance variables
		this.dataset = dataset;
		neighbourhoodSizes = new HashMap<Integer, Integer>();
	}
	
	public Map<Integer, Integer> getNeighbourhoodSizes() {
		return neighbourhoodSizes;
	}

	/**
	 * Returns the set of neighbours, for the given node (with assigned vertex) and the relationship, that satisfy all required constraints.
	 * @param rel The target relationship. Used to specifying the relationship direction and the relationship attribute requirements.
	 * @param node The gp node. Used for specifying the relationship direction and the node attribute requirements.
	 * @param vertex The vertex assigned to the node. Used for actually querying the database.
	 * @return The set of neighbours that satisfy the associated constraints.
	 */
	public Set<MyNode> findNeighbours(MyRelationship rel, MyNode node, MyNode vertex){
		
		long start = System.nanoTime();
		//Setup the checking step
		Set<MyNode> neighbours = new HashSet<MyNode>();					//The list containing the result

		//Get the direction
		MyDirection dir = null;
		if (rel.getSource().equals(node)){
			dir = MyDirection.OUTGOING;
		} else {
			dir = MyDirection.INCOMING;
		}

		//Query the database for the neighbours of vertex, where direction = dir.
		//Only keep the neighbours where the edge and vertex attributes are satisfied
		
		Set<MyNode> result = dataset.getNeighbours(vertex, rel.getIdentifier(), dir);
		
		int size = neighbours.size();
		if (neighbourhoodSizes.containsKey(size)){
			int val = neighbourhoodSizes.get(size)+1;
			neighbourhoodSizes.put(size, val);
		} else {
			neighbourhoodSizes.put(size, 1);
		}
		
		long end = System.nanoTime();
		
		System.out.println("Query time: " + (end-start));

		
		return neighbours;
	}

	@Override
	public MyNode findNode(MyNode src) {
		return findNode(src, Integer.parseInt(src.getAttribute("id")));
	}

	@Override
	public MyNode findNode(MyNode src, Integer id) {
		MyNode tgt = dataset.findNode(id);

		if (tgt == null){
			//If the node is not found, return null
			System.out.println("Not fixed: " + id+"");
		}

		return tgt;
	}

	@Override
	public boolean relationshipExists(MyNode src, MyNode tgt, MyRelationship rel) {
		boolean retVal = false;

		//Check if the relationship exists between them.
		Iterator<MyNode> neighbourhoodIte = dataset.getNeighbours(src, rel.getIdentifier(), MyDirection.OUTGOING).iterator(); 

		while (neighbourhoodIte.hasNext()){
			MyNode neighbour = neighbourhoodIte.next();

			if (neighbour.equals(tgt)){
				retVal = true;
			}
		}

		return retVal;
	}
}
