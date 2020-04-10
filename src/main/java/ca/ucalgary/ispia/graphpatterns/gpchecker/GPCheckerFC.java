package ca.ucalgary.ispia.graphpatterns.gpchecker;

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
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.HasAttributes;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.tests.Killable;
import ca.ucalgary.ispia.graphpatterns.util.AttributeTypes;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class provides the engine for checking if a given graph pattern
 * exists in the given database.
 * @author szrrizvi
 *
 */

public class GPCheckerFC implements GPChecker, Killable{

	private GraphDatabaseService graphDb;			//The graph database interface
	private int queryCount;							//The counter for transactions
	private GPHolder gph;							//The GPHolder
	private GraphPattern gp;						//The graph pattern
	public List<Map<MyNode, Node>> queryResults;	//The list of results that satisfy the query

	private boolean killed;							//The kill flag.
	private int maxNeighbourhood;
	private int allRes;
	private int searchSpace;

	/**
	 * Constructor to set and initialize the fields.
	 * @param graphDb The database to set
	 * @param gph The graph pattern holder
	 */
	public GPCheckerFC(GraphDatabaseService graphDb, GPHolder gph){
		//Assign the fields
		this.graphDb = graphDb;
		this.gph = gph;
		this.gp = gph.getGp();

		//Initialize the results, the counter, and the kill flag
		queryResults = new ArrayList<Map<MyNode, Node>>();
		queryCount = 0;
		killed = false;
		
		maxNeighbourhood = 0;
		allRes = 0;
		searchSpace = 0;
	}

	public int getMaxNeighbourhood(){
		return this.maxNeighbourhood;
	}
	
	public int getAllRes(){
		return this.allRes;
	}
	
	public int getSearchSpace(){
		return this.searchSpace;
	}
	
	/**
	 * @return the query count
	 */
	public int getQueryCount(){
		return this.queryCount;
	}

	/**
	 * Runs the query evaluation algorithm.
	 * @return The query result
	 */
	public List<Map<MyNode, Node>> check(){
		//Initialize the assignments and candidates maps
		Map<MyNode, Node> assignments = new HashMap<MyNode, Node>();
		Map<MyNode, Set<Node>> candidates = new HashMap<MyNode, Set<Node>>();

		//Delegate to the overloaded method
		return check(assignments, candidates);

	}

	/**
	 * Runs the query evaluation algorithm. Used the extra information to bind certain GP Nodes 
	 * to specific nodes in the database
	 * @param extraInfo The map of MyNode to bind to specific nodes in the database. The value
	 * for each key is the value of the "id" attribute
	 * @return The query result
	 */
	public List<Map<MyNode, Node>> check(Map<MyNode, Integer> extraInfo){
		//Initialize the assignments and candidates maps
		Map<MyNode, Node> assignments = new HashMap<MyNode, Node>();
		Map<MyNode, Set<Node>> candidates = new HashMap<MyNode, Set<Node>>();

		//For each node in the extraInfo map
		for (MyNode node : extraInfo.keySet()){
			try (Transaction tx = graphDb.beginTx()){
				//Fix the MyNode object to the corresponding Node from the database
				//based on the id property
				Node vertex = graphDb.findNode(LabelEnum.PERSON, "id", extraInfo.get(node));
				if (vertex == null){
					//return null is no db node found with the given id
					return null;
				}
				
				if (checkAttrs(node, vertex)){
					//If the db node satisfies all attribute requirements, then add the mapping to the
					//assignments map
					assignments.put(node, vertex);
				} else {
					return null;
				}
				tx.success();
			}
		}
		//Continue on by delegating to the overloaded method
		return check(assignments, candidates);
	}




	/**
	 * Checks if the given graph pattern holder exists in the associated database.
	 * @param assignments 
	 * @return the query result
	 */
	private List<Map<MyNode, Node>> check(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates){

		//Get the set of pre fixed nodes
		Set<MyNode> alreadyFixed = assignments.keySet();

		//Create the assignments for the remaining fixed nodes
		List<MyNode> nodes = gp.getNodes();
		for (MyNode node : nodes){
			//These nodes are not already fixed and have the id attribute
			if (!alreadyFixed.contains(node) && node.hasAttribute("id")){

				try (Transaction tx = graphDb.beginTx()){
					//Obtain the db nodes with the matching id
					Node vertex = graphDb.findNode(LabelEnum.PERSON, "id", Integer.parseInt(node.getAttribute("id")));
					if (vertex == null){
						//If the node is not found, return null
						System.out.println("Not fixed: " + node.getAttribute("id"));
						return null;
					}

					if (checkAttrs(node, vertex)){
						//If all requirements pass, add the assignment
						assignments.put(node, vertex);						
					} else {
						return null;
					}

					tx.success();
				}
			}
		}
		//Check the direct relationships between the fixed nodes
		if (!preCheck(assignments)){
			//If the precheck fails, then return false.
			return null;
		}

		//Populate and filter the immediate neighbours of the fixed nodes
		for (MyNode key : assignments.keySet()){
			if (!populateFilter(assignments, candidates, key)){
				return null;
			}
		}
		
		//If the canadidates map is still empty, because there we no fixed nodes, then
		//populate the candidates map based on the attributes.
		if (candidates.isEmpty()){
			if (!attrBasedPop(candidates)){
				return null;
			}
		}
		
		//If the candidates map is still empty, even after populating it through attribute
		//requirements, then return null as we have no starting point.
		if (candidates.isEmpty()){
			return null;
		}
		
		//Start the search for the remaining nodes
		checker(assignments, candidates);
		return queryResults;
	}

	/**
	 * This method checks the direct relationships between the fixed nodes.
	 * @param gp
	 * @param fixedNodes
	 * @return
	 */
	private boolean preCheck(Map<MyNode, Node> fixedNodes){//, Set<MyNode> visitedNodes){

		//Get all of the relationships from GP.
		List<MyRelationship> rels = gp.getAllRelationships();

		//Iterate through the relationships
		for (MyRelationship rel : rels){
			//Find the relationships that contain the fixed nodes
			if (fixedNodes.containsKey(rel.getSource()) && fixedNodes.containsKey(rel.getTarget())){


				//Get the nodes
				Node source = fixedNodes.get(rel.getSource());
				Node target = fixedNodes.get(rel.getTarget());

				boolean passed = false;	//Flag for checking if relationship passed

				//Check if the relationship exists between them.
				try (Transaction tx = graphDb.beginTx()){
					queryCount++;
					Iterator<Relationship> relIte = source.getRelationships(rel.getIdentifier(), Direction.OUTGOING).iterator();

					while (relIte.hasNext() && !passed){
						Relationship r = relIte.next();
						Node neighbour = r.getEndNode();

						if (neighbour.equals(target) && checkAttrs(rel, r)){
							passed = true;
						}
					}

					tx.success();
				}

				//If the relationship did not pass, then return false
				if (!passed){
					return false;
				}
			}
		}

		//If reached here, the all of the relationships passed, thus return true.
		return true;
	}
	
	/**
	 * Populates the candidates maps based on the attribute requirements of the graph pattern.
	 * Assumption: The candidates map is empty.
	 * Return false, if there was even 1 node with attr requirements that could not be satisfied.
	 * @param gp The graph pattern
	 * @param candidates The candidates map
	 * @return False if there was even 1 node with attr requirements that could not be satisfied,
	 * else true.
	 */
	private boolean attrBasedPop(Map<MyNode, Set<Node>> candidates){
		
		//Get all nodes
		List<MyNode> nodes = gp.getNodes();
		
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
						
						//Check if the candidate can satisfy the other attribute requirements as well
						//If so, add it to the nodeCads list
						Node candidate = rite.next();
						if (checkAttrs(node, candidate)){
							nodeCads.add(candidate);
						}
					}
					
					//If the nodeCads list is empty, meaning no vertex could satisfy the attribute requirements 
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

	/**
	 * Populates the candidates map for assignments with possible vertices in the database
	 * @param gp
	 * @param assignments
	 * @param candidates
	 * @param node
	 */
	private boolean populateFilter(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates, MyNode node){

		Node vertex = assignments.get(node);
		//Get all of the relationships from GP that contain the given node.
		List<MyRelationship> rels = gp.getAllRelationships(node);

		//Iterate through the relationships
		for (MyRelationship rel : rels){
			//Record the other node (from the perspective of the given node)
			MyNode otherNode = rel.getOther(node);

			//If the other node is not already been assigned, then populate/filter it
			if (!assignments.containsKey(otherNode)){
				Set<Node> neighbours = findNeighbours(rel, node, vertex);

				if (candidates.containsKey(otherNode)){
					//If the candidates set exists, then filter it
					Set<Node> temp = candidates.get(otherNode);
					temp.retainAll(neighbours);				
				} else {
					//Else populate it
					candidates.put(otherNode, neighbours);
				}

				//If the updated candidates set is empty, then return false 
				if (candidates.get(otherNode).isEmpty()){
					return false;
				}
			}
		}

		return true;
	}

	private boolean checker(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates){

		//If the search has been killed, return false
		if (killed){
			return false;
		}


		//If we have assigned every node, then we are done with this result set!
		if (gp.getNodes().size() == assignments.keySet().size()){
			//allRes++;
			//Add the assignments for the queryResults list
			List<MyNode> resultSchema = gph.getResultSchema();
			Map<MyNode, Node> result = new HashMap<MyNode, Node>();


			//Copy the nodes from the resultSchema to the result map
			for (MyNode req : resultSchema){
				Node node = assignments.get(req);
				result.put(req, node);
			}

			//Add the result to the queryResults list. Avoid duplication
			if (!queryResults.contains(result)){
				queryResults.add(result);
			}
			return true;
		}

		//Pick the next node to assign such that it is populated but not yet assigned 
		MyNode nextNode = pickNextNode(assignments, candidates);
		mexFilter(nextNode, candidates.get(nextNode), assignments);
		
		//Choose a vertex for nextNode.
		//According to our algorithm, each candidate for nextNode satisfies all of the constraints
		//(i.e. the relationships with its already assigned neighbours).
		for(Node vertex : candidates.get(nextNode)){
			//searchSpace++;
			//Clone the candidates and assignments map
			Map<MyNode, Set<Node>> candsClone = new HashMap<MyNode, Set<Node>>();
			for (MyNode key : candidates.keySet()){
				//Get the set of candidates
				Set<Node> candidatesSet = new HashSet<Node>(); 

				for (Node candidate : candidates.get(key)){
					candidatesSet.add(candidate);
				}
				candsClone.put(key, candidatesSet);
			}

			Map<MyNode, Node> assnClone = new HashMap<MyNode, Node>();
			for (MyNode key : assignments.keySet()){
				//Get the assigned vertex
				Node assn = assignments.get(key);
				assnClone.put(key, assn);
			}

			candsClone.remove(nextNode);
			assnClone.put(nextNode, vertex);
			boolean validVertex = populateFilter(assnClone, candsClone, nextNode);

			if (validVertex){
				//If we didn't abandon this vertex, then we can recurse
				//Recurse with clones of maps
				boolean isGood = checker(assnClone, candsClone);

				if (killed){
					return false;
				}

				/*if (isGood && !gph.getResultSchema().contains(nextNode)){
					return true;
				}*/
			}
		}

		return false;
	}

	/**
	 * Find nodes such that they are populated but not yet assigned.
	 * Pick and return the node with the smallest candidates size
	 * @param gp The graph pattern
	 * @param assignments The map of currently assigned nodes and their assignments
	 * @param candidates The map of populated nodes and their candidates
	 * @return The next node to be assigned in the algorithm
	 */
	private MyNode pickNextNode(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates){

		List<MyNode> allNodes = gp.getNodes();
		// Find nodes such that they are populated but not yet assigned.
		// Pick the node with the smallest candidates size
		// Optimization idea: When a node is assigned, remove it from candidates. Thus,
		// candidates only consists of unassigned nodes.

		MyNode nextNode = null;
		int candidatesSize = 0;

		//Loop through the nodes
		for (MyNode node : allNodes){
			//If the node is populated but not assigned
			if ((!assignments.containsKey(node)) && (candidates.containsKey(node))){

				//Get the candidates size
				int newSize = candidates.get(node).size();

				//Update nextNode with the current node if nextNode is null or the current node has a smaller candidate size 
				//than the nextNode
				if ((nextNode == null) || (newSize < candidatesSize)){
					nextNode = node;
					candidatesSize = newSize;
				}
			}
		}
		
		return nextNode;
	}

	private Set<Node> findNeighbours(MyRelationship rel, MyNode node, Node vertex){

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

		//Get the relation identifier

		//Get all neighbours and add them to the list
		//queryCount++;
		try (Transaction tx = graphDb.beginTx()){
			Iterable<Relationship> result = vertex.getRelationships(rel.getIdentifier(), dir);
			
			for (Relationship tempR : result){
				Node neighbour = tempR.getOtherNode(vertex);

				//If the relationship and neighbour both satisfy the attribute requirements
				//then add the neighbour to the result list
				if (checkAttrs(rel, tempR) && checkAttrs(otherNode, neighbour)){
					neighbours.add(neighbour);
				}
			}
			
			tx.success();
		}

		return neighbours;
	}


	/**
	 * Filters the candidates set based on the mutual exclusion constraints and current assignment
	 * @param variable The target graph pattern node
	 * @param node The assignment for the target node
	 * @param candidates The list of candidates for the currently populated nodes
	 */
	
	private void mexFilter(MyNode variable, Set<Node> candidates, Map<MyNode, Node> assignments){
		//Get the mutual exclusion constraints containing the variable
		List<Pair<MyNode, MyNode>> mexList = gph.getMexList(variable); 


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
			}
		}		
	}

	/**
	 * Sets the kill switch to true
	 */
	public void kill(){
		this.killed = true;
		System.out.print("KILLED ");
	}

	private boolean checkAttrs(HasAttributes source, Entity target){
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
					} catch (NumberFormatException e){
						String reqVal = attrReqs.get(attr);
						if (!reqVal.equals(val)){
							return false;
						}
					} catch (ClassCastException e){
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
