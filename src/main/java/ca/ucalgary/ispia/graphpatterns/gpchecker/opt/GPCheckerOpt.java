package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPChecker;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.AttrBasedStart;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.ConstraintsChecker;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.DBAccess;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.LeastCandidates;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.ReturnStructImpl;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.tests.Killable;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;

/**
 * This class provides the engine for checking if a given graph pattern
 * exists in the given database.
 * @author szrrizvi
 *
 */

public class GPCheckerOpt implements GPChecker, Killable{

	private final GraphDatabaseService graphDb;			//The graph database interface
	private int queryCount;							//The counter for transactions
	private final GPHolder gph;							//The GPHolder
	private final GraphPattern gp;						//The graph pattern contained in gph
	public List<Map<MyNode, Node>> queryResults;	//The list of results that satisfy the query

	//The modularized components
	private final ConstraintsEvaluator consEval;
	private final NeighbourhoodAccess neighbourhoodAccess;
	private final VariableOrdering variableOrdering;
	private final AltStart altStart;


	private boolean killed;							//The kill flag.

	/**
	 * Constructor to set and initialize the fields.
	 * @param graphDb The database to set
	 * @param gph The graph pattern holder
	 */
	public GPCheckerOpt(GraphDatabaseService graphDb, GPHolder gph){
		//Assign the fields
		this.graphDb = graphDb;
		this.gph = gph;
		this.gp = gph.getGp();

		//Initialize the results, the counter, and the kill flag
		queryResults = new ArrayList<Map<MyNode, Node>>();
		queryCount = 0;
		killed = false;

		this.consEval = new ConstraintsChecker(gph, graphDb);
		this.neighbourhoodAccess = new DBAccess (graphDb, consEval);
		this.variableOrdering = new LeastCandidates(gp);
		this.altStart = new AttrBasedStart(graphDb, consEval);

	}

	/**
	 * @return the query count
	 */
	public int getQueryCount(){
		return this.queryCount;
	}

	//--------------------------//
	// PUBLICLY EXPOSED METHODS
	//--------------------------//

	/**
	 * Runs the query evaluation algorithm.
	 * @return The query result
	 */
	public List<Map<MyNode, Node>> check(){
		//Initialize the assignments and candidates maps
		Map<MyNode, Node> assignments = new HashMap<MyNode, Node>();
		Map<MyNode, Set<Node>> candidates = new HashMap<MyNode, Set<Node>>();

		//Delegate to the overloaded method
		return check_init(assignments, candidates);

	}

	/**
	 * Runs the query evaluation algorithm. Use the extra information to bind certain GP Nodes 
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

				if (consEval.checkAttrs(node, vertex)){
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
		return check_init(assignments, candidates);
	}

	//--------------------------//
	// INIT
	//--------------------------//

	/**
	 * Checks if the given graph pattern holder exists in the associated database.
	 * @param assignments 
	 * @return the query result
	 */
	private List<Map<MyNode, Node>> check_init(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates){

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

					if (consEval.checkAttrs(node, vertex)){
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

		//Initialize the conflit maps
		Set<MyNode> confOut = new HashSet<MyNode>();
		Map<MyNode, Set<MyNode>> confIn = new HashMap<MyNode, Set<MyNode>>();

		//Populate and filter the immediate neighbours of the fixed nodes
		for (MyNode key : assignments.keySet()){
			if (!populateFilter(assignments, candidates, key, confOut, confIn)){
				return null;
			}
		}

		//If the canadidates map is still empty, because there we no fixed nodes, then
		//populate the candidates map based on the attributes.
		if (candidates.isEmpty()){
			if (!altStart.startPop(gp.getNodes(), candidates)){
				return null;
			}
		}

		//If the candidates map is still empty, even after populating it through attribute
		//requirements, then return null as we have no starting point.
		if (candidates.isEmpty()){
			return null;
		}

		//Start the search for the remaining nodes
		check_rec(assignments, candidates, confIn);
		return queryResults;
	}

	//--------------------------//
	// RECURSIVE STEP
	//--------------------------//

	/**
	 * The recursive step of the GP-Eval algorithm.
	 * @param assignments The current state of assignments.
	 * @param candidates The current state of candidates.
	 * @param confOut Outgoing conflicts. Var v filters/populates the associated list.
	 * @param confIn Incoming conflicts. Var v is filtered/populated by the associated list. 
	 * @return
	 */

	private Set<MyNode> check_rec(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates, Map<MyNode, Set<MyNode>> confIn){

		//If the search has been killed, return false
		if (killed){
			return null;
		}

		// BASE CASE

		//If we have assigned every node, then we are done with this result set!
		if (gp.getNodes().size() == assignments.keySet().size()){

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
			return new HashSet<MyNode>(gph.getResultSchema());
		}

		// SMALLER PROBLEM AND RECURSIVE STEP

		//Pick the next node to assign such that it is populated but not yet assigned 
		MyNode nextNode = variableOrdering.pickNextNode(assignments, candidates);
		consEval.mexFilter(nextNode, candidates.get(nextNode), assignments, confIn);

		//Dead-ednd flag
		boolean deadEnd = true;
		
		//Set for outgoing conflicts.
		Set<MyNode> confOut = new HashSet<MyNode>();
		Set<MyNode> jumpStack = new HashSet<MyNode>();

		//Choose a vertex for nextNode.
		//According to our algorithm, each candidate for nextNode satisfies all of the constraints
		//(i.e. the relationships with its already assigned neighbours and attribute requirements).
		for(Node vertex : candidates.get(nextNode)){
			
			
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
			//Update the clones based on current assignment
			candsClone.remove(nextNode);
			assnClone.put(nextNode, vertex);

			//Clone the in conflicts maps

			Map<MyNode, Set<MyNode>> confInClone = new HashMap<MyNode, Set<MyNode>>();
			for (MyNode key : confIn.keySet()){
				//Get the set of out conflicts
				Set<MyNode> confSet = new HashSet<MyNode>();

				for (MyNode conflict : confIn.get(key)){
					confSet.add(conflict);
				}
				confInClone.put(key, confSet);
			}

			//Perform forward checking
			boolean validVertex = populateFilter(assnClone, candsClone, nextNode, confOut, confInClone);
			
			if (validVertex){
				//Update deadEnd flag
				deadEnd = false;

				//If we didn't abandon this vertex, then we can recurse
				//Recurse with clones of maps
				Set<MyNode> jumpNodes = check_rec(assnClone, candsClone, confInClone);

				if (killed){
					return null;
				}

				if (jumpNodes != null && !jumpNodes.isEmpty()){
					
					if (!jumpNodes.contains(nextNode)){
						return jumpNodes;
					} else {
						jumpStack.addAll(jumpNodes);
					}
				}

				//if (rs != null && 
				//		rs.isJumpingForJoy() && 
				//		!rs.getJumpVariables().contains(nextNode)){
				//	return rs;
				//}
			}
		}

		if (deadEnd){
			return deadEndJump(nextNode, confOut, confIn);
		} else {
			jumpStack.addAll(successJump(nextNode, confOut, confIn, assignments.keySet()));
			
			return jumpStack;
		}
	}

	//--------------------------//
	// FORWARD CHECKING
	//--------------------------//

	/**
	 * Populates the candidates map for assignments with possible vertices in the database
	 * @param assignments
	 * @param candidates
	 * @param node
	 */
	private boolean populateFilter(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates, MyNode node, Set<MyNode> confOut, Map<MyNode, Set<MyNode>> confIn){

		Node vertex = assignments.get(node);
		//Get all of the relationships from GP that contain the given node.
		List<MyRelationship> rels = gp.getAllRelationships(node);

		//Iterate through the relationships
		for (MyRelationship rel : rels){
			//Record the other node (from the perspective of the given node)
			MyNode otherNode = rel.getOther(node);

			//If the other node is not already been assigned, then populate/filter it
			if (!assignments.containsKey(otherNode)){
				Set<Node> neighbours = neighbourhoodAccess.findNeighbours(rel, node, vertex);

				if (candidates.containsKey(otherNode)){
					//If the candidates set exists, then filter it
					Set<Node> temp = candidates.get(otherNode);

					//If there is filtering, then add the incoming conflict.
					if (!neighbours.containsAll(temp)){
						addConflictIn(node, otherNode, confIn);
					}

					temp.retainAll(neighbours);				
				} else {
					//Else populate it
					candidates.put(otherNode, neighbours);

					//If there is populating, then add the incoming conflict.
					addConflictIn(node, otherNode, confIn);
				}

				//If the updated candidates set is empty, then add the outgoing conflict and return false 
				if (candidates.get(otherNode).isEmpty()){

					confOut.add(otherNode);

					return false;
				}
			}
		}

		return true;
	}


	//--------------------------//
	// HELPER METHODS
	//--------------------------//	

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

						if (neighbour.equals(target)){
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

	//-------------------------//
	// KILLABLE FEATURES	
	//-------------------------//
	/**
	 * Sets the kill switch to true
	 */
	public void kill(){
		this.killed = true;
	}


	//-------------------------//
	// CONFLICT-DIRECTED BACKJUMPING
	//-------------------------//

	/**
	 * Adds an incoming conflict to the confIn map. Also maintains the influence chains. 
	 * @param src The source node that filtered the target node's candidates set.
	 * @param tgt The target node whose candidates set is filtered.
	 * @param confIn The map of incoming conflicts.
	 */
	private void addConflictIn(MyNode src, MyNode tgt, Map<MyNode, Set<MyNode>> confIn){

		if (confIn.containsKey(tgt)){
			//If the target already has incoming conflicts, then add the src to the conflict in set.
			confIn.get(tgt).add(src);
		} else {
			//If the target doesn't have incoming conflicts, then create a new set, add the src, and then 
			//put the key-value pair in the confIn map.
			Set<MyNode> confList = new HashSet<MyNode>();
			confList.add(src);
			confIn.put(tgt, confList);
		}

		//Maintain the influence chains.
		//Add the src's confIn conflicts to the tgt's.
		if (confIn.containsKey(src)){
			confIn.get(tgt).addAll(confIn.get(src));
		}
	}

	/**
	 * Compute the set of nodes to jump back to in case of a deadend
	 * @param src The current node where we reached a deadend.
	 * @param confOut The set of nodes whose candidate sets were emptied.
	 * @param confIn The conflict in map. 
	 * @return The set of nodes to jump back to.
	 */
	private Set<MyNode> deadEndJump(MyNode src, Set<MyNode> confOut, Map<MyNode, Set<MyNode>> confIn){

		//The set to help the return value
		Set<MyNode> jumpVars = new HashSet<MyNode>();

		//For the nodes whose candidate set was emptied, backjump to their confIn set.
		//Perhaps this will allow us to retain a different set of candidates that would work.
		for (MyNode node : confOut){
			if (confIn.containsKey(node)){
				jumpVars.addAll(confIn.get(node));
			}
		}

		//Backjump to the nodes that filtered the candidate set for the current node.
		//Perhaps this will allows us to retain a different set of canddiates taht would work.
		if (confIn.containsKey(src)){
			jumpVars.addAll(confIn.get(src));
		}


		return jumpVars;

	}

	/**
	 * Computes the set of nodes to jump back to in case the current node has at least one assignment that can be extended. 
	 * @param src The current node.
	 * @param confOut The outgoing conflicts set.
	 * @param confIn The incoming conflicts map.
	 * @param assignedNodes The current assignments map.
	 * @return The set of nodes to jump back to.
	 */
	private Set<MyNode> successJump(MyNode src, Set<MyNode> confOut, Map<MyNode, Set<MyNode>> confIn, Set<MyNode> assignedNodes){
		Set<MyNode> jumpVars = new HashSet<MyNode>();

		//Get the filtering chains for the future Result Schema nodes
		for (MyNode node : gph.getResultSchema()){
			if (!assignedNodes.contains(node)){
				if (confIn.containsKey(node)){
					jumpVars.addAll(confIn.get(node));
				}
			}
		}
		
		//If there was at least one candidate for the current variable that caused a deadend, then get the corresponding 
		//back jump variables for that candidate as well.
		if (!confOut.isEmpty()){
			jumpVars.addAll(deadEndJump(src, confOut, confIn));
		}
		
		return jumpVars;
	}
}
