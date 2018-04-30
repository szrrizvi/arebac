package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.AltStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ConstraintsEvaluator;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.util.AttributeTypes;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;

/**
 * This class populates the candidates set based on the attribute requirements for nodes.
 * @author szrrizvi
 *
 */
public class AttrBasedStart implements AltStart<Node>{
	
	
	private final GraphDatabaseService graphDb;					//GraphDatabaseService: Provides access to database
	private final ConstraintsEvaluator constraintsEvaluator;	//ConstraintsEvaluator: Provides access to constraints evaluator component.
	
	/**
	 * Initializes the instance variables.
	 * @param graphDb The graph database service
	 * @param constraintsChecker The constraints checker module
	 */
	public AttrBasedStart(GraphDatabaseService graphDb, ConstraintsEvaluator constraintsEvaluator){
		//Assign the instance variables.
		this.graphDb = graphDb;
		this.constraintsEvaluator = constraintsEvaluator;
	}
	
	
	/**
	 * Populates the candidates maps based on the attribute requirements of the graph pattern.
	 * Assumption: The candidates map is empty.
	 * Return false, if there was even 1 node with attr requirements that could not be satisfied.	 
	 * @param nodes The list of all nodes in the graph pattern
	 * @param candidates The candidates map
	 * @return False if there was even 1 node with attr requirements that could not be satisfied,
	 * else true.
	 * 
	 * Side Effect: candidates will be updated
	 */
	public boolean startPop(List<MyNode> nodes, Map<MyNode, Set<Node>> candidates){
	
		
		//Iterate through all nodes
		for (MyNode node : nodes){
			if (node.hasAttributes()){	//Populate the candidate set for each node that has at least one required attribute
				Set<Node> nodeCads = new HashSet<Node>();
				
				//Get the required attribute names and values
				Map<String, String> attrs = node.getAttributes();
				List<String> keys = new ArrayList<String>();
				keys.addAll(attrs.keySet());
				
				try (Transaction tx = graphDb.beginTx()){
					
					//Get the first required attribute, and query the database
					//for nodes that can satisfy the attribute requirement
					String key = keys.get(0);
					
					ResourceIterator<Node> rite = null;
					Object val = null;
					
					if (AttributeTypes.isIntType(key)){
						//Compare the property, based on if its an int or a String
						//If the values don't match, return false.
						try{
							val = Integer.parseInt(attrs.get(key));
						} catch (Exception e){
							val = attrs.get(key);
						}
					} else {
						val = attrs.get(key);
					}
					
					rite = graphDb.findNodes(LabelEnum.PERSON, key.trim(), val);
					
					//Iterate through the candidates that can satisfy the first attribute requirement
					while(rite.hasNext()){
						//If so, add it to the nodeCads list
						Node candidate = rite.next();
						if (constraintsEvaluator.checkAttrs(node, candidate)){
							nodeCads.add(candidate);
						}
					}
					
					//If the nodeCads list is empty, meaning no vertex could satisfy the attribute requirements attrBasedPop(candidates))
					//for the node, then return false.
					if (nodeCads.isEmpty()){
						tx.success();
						return false;
					}
					//Otherwise, update the candidates map
					candidates.put(node, nodeCads);
					tx.success();
				}
			}
		}
		//If reached here, then there is at least one candidate for each 
		//Node with attr requirement.
		return true;
	}	
}
