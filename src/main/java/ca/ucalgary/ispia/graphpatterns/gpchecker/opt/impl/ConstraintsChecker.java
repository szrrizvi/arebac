package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ConstraintsEvaluator;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.HasAttributes;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.util.AttributeTypes;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class provides the methods for constraints checking for the graph pattern evaluation.
 * So far the constraints we have are: mutual exclusion constraints and attribute requirements.
 * @author szrrizvi
 *
 */
public class ConstraintsChecker implements ConstraintsEvaluator<Node, Entity>{


	private final GPHolder gph;					//GPHolder - gives access to the constraints
	private final GraphDatabaseService graphDb;	//GraphDatabaseService - gives access to underlying database
	
	/**
	 * Simple constructor. Assigns the instance variables.
	 * @param gph The GPHolder
	 * @param graphDb The GraphDatabaseService
	 */
	public ConstraintsChecker(GPHolder gph, GraphDatabaseService graphDb){
		//Initialize instance variables.
		this.gph = gph;
		this.graphDb = graphDb;
	}
	
	/**
	 * Filters the candidates set based on the mutual exclusion constraints and current assignment
	 * @param variable The target graph pattern node
	 * @param node The assignment for the target node
	 * @param candidates The list of candidates for the currently populated nodes
	 */
	public void mexFilter(MyNode variable, Set<Node> candidates, Map<MyNode, Node> assignments, Map<MyNode, Set<MyNode>> confIn){
		//Get the mutual exclusion constraints containing the variable
		List<Pair<MyNode, MyNode>> mexList = gph.getMexList(variable); 

		if (!confIn.containsKey(variable)){
			confIn.put(variable, new HashSet<MyNode>());
		}
		

		for (Pair<MyNode, MyNode> mex : mexList){
			//For the constraint, get the other node
			MyNode other = null;
			if (mex.first.equals(variable)){
				other = mex.second;
			} else {
				other = mex.first;
			}

			//If the other node is populate
			if (assignments.containsKey(other)){
				//Remove 'node' from its candidates set 
				candidates.remove(assignments.get(other));
				
				confIn.get(variable).add(other);
			}
		}		
	}
	
	
	/**
	 * Checks if the given entity has the required attributes.
	 * @param source The MyNode or MyRelationship object from gp.
	 * @param target The Node or Relationship object from the database.
	 * @return true if the target can satisfy the required attributes, else false.
	 */
	public boolean checkAttrs(HasAttributes source, Entity target){
		//Prepare the list of source attribute requirements (if any)
		Map<String, String> attrReqs = source.getAttributes();
		List<String> attrNames = new ArrayList<String>();
		attrNames.addAll(attrReqs.keySet());

		//Check if the target satisfies all attribute requirements.
		for (String attr : attrNames){

			//Get the attribute/property from the target
			Object val = null;
			try (Transaction tx = graphDb.beginTx()){
				val = target.getProperty(attr, null);
				tx.success();
			}

			if (val != null){	//Ensure that the target has the attribute/property
				if (AttributeTypes.isIntType(attr)){
					//Compare the property, based on if its an int or a String
					//If the values don't match, return false.
					try{
						int reqVal = Integer.parseInt(attrReqs.get(attr));
						if (reqVal != (int) val){
							return false;
						}
					} catch (NumberFormatException | ClassCastException e){
						String reqVal = attrReqs.get(attr);
						if (!reqVal.equals(val)){
							return false;
						}
					}

				} else {
					String reqVal = attrReqs.get(attr);
					String tempVal = val+"";
					if (!reqVal.equals(tempVal)){
						return false;
					}
				}
			} else {
				//If the target did not have the required attribute/property, return false
				return false;
			}

		}

		//If the target satisfies all attribute requirements, return true
		return true;
	}
	
}
