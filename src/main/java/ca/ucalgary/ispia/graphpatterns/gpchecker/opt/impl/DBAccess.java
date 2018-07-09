package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ConstraintsEvaluator;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.NeighbourhoodAccess;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;

/**
 * This class provides the wrapper for invoking database queries against the database.
 * @author szrrizvi
 *
 */

public class DBAccess implements NeighbourhoodAccess<Node>{
	private final GraphDatabaseService graphDb;					//GraphDatabaseService - gives access to the database
	private final ConstraintsEvaluator<Node, Entity> constraintsEvaluator;	//ConstraintsEvaluator - gives access to the components that ensures constraints are satisfied
	
	public Map<Integer, Integer> neighbourhoodSizes; 

	/**
	 * Initilizes the instance variables.
	 * @param graphDb The GraphDatabaseService
	 * @param constraintsChecker The ConstraintsChecker
	 */
	public DBAccess (GraphDatabaseService graphDb, ConstraintsEvaluator<Node, Entity> constraintsEvaluator){
		//Initialize the instance variables
		this.graphDb = graphDb;
		this.constraintsEvaluator = constraintsEvaluator;
		this.neighbourhoodSizes = new HashMap<Integer, Integer>();
	}
	
	/**
	 * Returns the set of neighbours, for the given node (with assigned vertex) and the relationship, that satisfy all required constraints.
	 * @param rel The target relationship. Used to specifying the relationship direction and the relationship attribute requirements.
	 * @param node The gp node. Used for specifying the relationship direction and the node attribute requirements.
	 * @param vertex The vertex assigned to the node. Used for actually querying the database.
	 * @return The set of neighbours that satisfy the associated constraints.
	 */
	public Set<Node> findNeighbours(MyRelationship rel, MyNode node, Node vertex){

		//Setup the checking step
		Set<Node> neighbours = new HashSet<Node>();					//The list containing the result
		MyNode otherNode = rel.getOther(node);						//The other node in the relationship

		//Prepare the list of edge attribute requirements (if any)
		Map<String, String> eAttrReqs = rel.getAttributes();	
		List<String> eAttrNames = new ArrayList<String>();
		eAttrNames.addAll(eAttrReqs.keySet());

		//Get the direction
		Direction dir = null;
		if (rel.getSource().equals(node)){
			dir = Direction.OUTGOING;
		} else {
			dir = Direction.INCOMING;
		}

		
		//Query the database for the neighbours of vertex, where direction = dir.
		//Only keep the neighbours where the edge and vertex attributes are satisfied
		try (Transaction tx = graphDb.beginTx()){
			Iterable<Relationship> result = vertex.getRelationships(rel.getIdentifier(), dir);
			
			for (Relationship tempR : result){
				Node neighbour = tempR.getOtherNode(vertex);

				//If the relationship and neighbour both satisfy the attribute requirements
				//then add the neighbour to the result list
				if (constraintsEvaluator.checkAttrs(rel, tempR) && constraintsEvaluator.checkAttrs(otherNode, neighbour)){
					neighbours.add(neighbour);
				}
			}
			//End transaction
			tx.success();
		}

		int size = neighbours.size();
		if (neighbourhoodSizes.containsKey(size)){
			int val = neighbourhoodSizes.get(size)+1;
			neighbourhoodSizes.put(size, val);
		} else {
			neighbourhoodSizes.put(size, 1);
		}
		
		if (size == 391){
			System.out.println("391: " + rel + ", " + node + ", " + vertex);
		}
		
		return neighbours;
	}
	
	public Node findNode(MyNode src){
		
		return findNode(src, Integer.parseInt(src.getAttribute("id")));

	}
	
	public Node findNode(MyNode src, Integer id){
		Node tgt = null;
		
		try (Transaction tx = graphDb.beginTx()){
			
			tgt = graphDb.findNode(LabelEnum.Person, "id", id);
			
			if (tgt == null){
				//If the node is not found, return null
				System.out.println("Not fixed: " + id);
			}
			
			if (!constraintsEvaluator.checkAttrs(src, tgt)){
				//If other attr requirements fail, then tgt is not the correct node
				tgt = null;						
			}

			tx.success();
		}
		return tgt;
	}
	
	public boolean relationshipExists (Node src, Node tgt, MyRelationship rel){
		
		Relationship dbRel = null;
		boolean retVal = false;
		
		//Check if the relationship exists between them.
		try (Transaction tx = graphDb.beginTx()){
			Iterator<Relationship> relIte = src.getRelationships(rel.getIdentifier(), Direction.OUTGOING).iterator();

			while (relIte.hasNext() && dbRel == null){
				Relationship r = relIte.next();
				Node neighbour = r.getEndNode();

				if (neighbour.equals(tgt)){
					dbRel = r;
				}
			}

			if (dbRel != null){
				if(!constraintsEvaluator.checkAttrs(rel, dbRel)){
					dbRel = null;
				}
			}
			
			tx.success();
		}
		
		return retVal;
	}
}
