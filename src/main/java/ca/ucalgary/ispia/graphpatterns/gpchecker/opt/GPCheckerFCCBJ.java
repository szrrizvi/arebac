package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPChecker;
import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl.DBAccess;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.tests.Killable;

/**
 * This class provides the engine for checking if a given graph pattern
 * exists in the given database.
 * @author szrrizvi
 *
 */

public class GPCheckerFCCBJ<N, E> implements GPChecker<N, E>, Killable{

	//private final GraphDatabaseService graphDb;			//The graph database interface
	private int queryCount;							//The counter for transactions
	private final GPHolder gph;							//The GPHolder
	private final GraphPattern gp;						//The graph pattern contained in gph
	public List<Map<MyNode, N>> queryResults;	//The list of results that satisfy the query

	//The modularized components
	private final ConstraintsEvaluator<N, E> consEval;
	private final NeighbourhoodAccess<N> neighbourhoodAccess;
	private final VariableOrdering<N> variableOrdering;
	private final AltStart<N> altStart;

	private int count;

	private boolean killed;							//The kill flag.

	/**
	 * Constructor to set and initialize the fields.
	 * @param graphDb The database to set
	 * @param gph The graph pattern holder
	 */
	public GPCheckerFCCBJ(GPHolder gph, ConstraintsEvaluator<N, E> consEval, NeighbourhoodAccess<N> neighbourhoodAccess, VariableOrdering<N> variableOrdering, AltStart<N> altStart){
		//Assign the fields
		//this.graphDb = graphDb;
		this.gph = gph;
		this.gp = gph.getGp();

		//Initialize the results, the counter, and the kill flag
		queryResults = new ArrayList<Map<MyNode, N>>();
		queryCount = 0;
		killed = false;

		this.consEval = consEval;
		this.neighbourhoodAccess = neighbourhoodAccess;
		this.variableOrdering = variableOrdering;
		this.altStart = altStart;

		count = 0;
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
	public List<Map<MyNode, N>> check(){
		//Initialize the assignments and candidates maps
		Map<MyNode, N> assignments = new HashMap<MyNode, N>();
		Map<MyNode, Set<N>> candidates = new HashMap<MyNode, Set<N>>();

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
	public List<Map<MyNode, N>> check(Map<MyNode, Integer> extraInfo){
		//Initialize the assignments and candidates maps
		Map<MyNode, N> assignments = new HashMap<MyNode, N>();
		Map<MyNode, Set<N>> candidates = new HashMap<MyNode, Set<N>>();

		//For each node in the extraInfo map
		for (MyNode node : extraInfo.keySet()){

			N vertex = neighbourhoodAccess.findNode(node, extraInfo.get(node));
			if (vertex != null){
				Set<N> temp = new HashSet<N>();
				temp.add(vertex);
				candidates.put(node, temp);
			} else {
				//System.out.println("HERE A");
				return null;
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
	private List<Map<MyNode, N>> check_init(Map<MyNode, N> assignments, Map<MyNode, Set<N>> candidates){

		//Create the candidates for the remaining fixed nodes
		List<MyNode> nodes = gp.getNodes();
		for (MyNode node : nodes){
			//These nodes are not already fixed and have the id attribute
			if (!candidates.keySet().contains(node) && node.hasAttribute("id")){

				N vertex = neighbourhoodAccess.findNode(node);
				if (vertex != null){
					Set<N> temp = new HashSet<N>();
					temp.add(vertex);
					candidates.put(node, temp);
				} else {
					return null;
				}				
			}
		}

		//Initialize the conflit maps
		Set<MyNode> confOut = new HashSet<MyNode>();
		Map<MyNode, Set<MyNode>> confIn = new HashMap<MyNode, Set<MyNode>>();

		//If the canadidates map is still empty, because there we no fixed nodes, then
		//populate the candidates map based on the attributes.
		if (candidates.isEmpty()){
			if (!altStart.startPop(gp.getNodes(), candidates)){
				return null;
			}
		}

		//If the candidates map is still empty, even after populating it through
		//alternat start, then return null as we have no starting point.
		if (candidates.isEmpty()){
			return null;
		}

		//Start the search for the remaining nodes
		check_rec(assignments, candidates, confIn);

		
		/*DBAccess temp = (DBAccess) neighbourhoodAccess;
		Map<Integer, Integer> sizes = temp.neighbourhoodSizes;

		System.out.println("DB Queries:");
		for (Integer key : sizes.keySet()){
			System.out.println(key + ", " + sizes.get(key));
		}*/

		return queryResults;
	}

	//--------------------------//
	// RECURSIVE STEP
	//--------------------------//

	/**
	 * The recursive step of the GP-Eval algorithm.
	 * @param assignments The current state of assignments.
	 * @param candidates The current state of candidates.
	 * @param confIn Incoming conflicts. Var v is filtered/populated by the associated list. 
	 * @return
	 */

	private Set<MyNode> check_rec(Map<MyNode, N> assignments, Map<MyNode, Set<N>> candidates, Map<MyNode, Set<MyNode>> confIn){
		//If the search has been killed, return false
		if (killed){
			return null;
		}

		// BASE CASE

		//If we have assigned every node, then we are done with this result set!
		if (gp.getNodes().size() == assignments.keySet().size()){

			count++;

			//Add the assignments for the queryResults list
			List<MyNode> resultSchema = gph.getResultSchema();
			Map<MyNode, N> result = new HashMap<MyNode, N>();


			//Copy the nodes from the resultSchema to the result map
			for (MyNode req : resultSchema){
				N node = assignments.get(req);
				result.put(req, node);
			}

			//Add the result to the queryResults list. Avoid duplication
			if (!queryResults.contains(result)){
				queryResults.add(result);
			}
			Set<MyNode> res = new HashSet<MyNode>();
			return res;
		}

		// SMALLER PROBLEM AND RECURSIVE STEP

		int bjFlag = count;

		//Pick the next node to assign such that it is populated but not yet assigned 
		MyNode nextNode = variableOrdering.pickNextNode(assignments, candidates);
		if (consEval != null){
			consEval.mexFilter(nextNode, candidates.get(nextNode), assignments, confIn);
		}

		//Dead-end flag
		boolean deadEnd = true;

		//Set for outgoing conflicts.
		Set<MyNode> confOut = new HashSet<MyNode>();
		Set<MyNode> conflicts = new HashSet<MyNode>();

		//Choose a vertex for nextNode.
		//According to our algorithm, each candidate for nextNode satisfies all of the constraints
		//(i.e. the relationships with its already assigned neighbours and attribute requirements).
		for(N vertex : candidates.get(nextNode)){

			//Clone the candidates and assignments map
			Map<MyNode, Set<N>> candsClone = new HashMap<MyNode, Set<N>>();

			for (MyNode key : candidates.keySet()){
				//Get the set of candidates
				Set<N> candidatesSet = new HashSet<N>(); 
				candidatesSet.addAll(candidates.get(key));
				candsClone.put(key, candidatesSet);
			}

			Map<MyNode, N> assnClone = new HashMap<MyNode, N>();
			for (MyNode key : assignments.keySet()){
				//Get the assigned vertex
				N assn = assignments.get(key);
				assnClone.put(key, assn);
			}
			//Update the clones based on current assignment
			candsClone.remove(nextNode);
			assnClone.put(nextNode, vertex);

			//Clone the in conflicts maps

			Map<MyNode, Set<MyNode>> confInClone = new HashMap<MyNode, Set<MyNode>>();
			for (MyNode key : confIn.keySet()){
				//Get the set of in conflicts
				Set<MyNode> confSet = new HashSet<MyNode>();
				confSet.addAll(confIn.get(key));
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

				if (jumpNodes == null){
					System.out.println("Unexpected Return value: NULL");
					killed = true;
					return null;
				}

				if (!jumpNodes.isEmpty() && !jumpNodes.contains(nextNode)){
					//If there is a future node assignment that leads to a deadend, such that the future node has no conflicts with nextNode,
					//then no other candidate of nextNode can prevent the deadend. Therefore, we can just return using jumpNodes.
					return jumpNodes;
				} else {
					//If the future deadend is affected by NextNode, then we add the jumpNodes to jumpStack, and try the next candidate for nextNode.
					//When we return from this call stack, we will return using jumpStack.
					//System.out.println("PAUSED G");
					conflicts.addAll(jumpNodes);
				}
			}  // else NEED TO DO SOMETHING WITH CBJ HERE
		}

		if (deadEnd || bjFlag == count){
			conflicts.addAll(deadEndJump(nextNode, confOut, confIn));
			return conflicts;
		} else {
			return new HashSet<MyNode>();
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
	 * @param confOut
	 * @param confIn
	 * @return
	 */
	private boolean populateFilter(Map<MyNode, N> assignments, Map<MyNode, Set<N>> candidates, MyNode node, Set<MyNode> confOut, Map<MyNode, Set<MyNode>> confIn){

		N vertex = assignments.get(node);
		//Get all of the relationships from GP that contain the given node.
		List<MyRelationship> rels = gp.getAllRelationships(node);

		//Iterate through the relationships
		for (MyRelationship rel : rels){
			//Record the other node (from the perspective of the given node)
			MyNode otherNode = rel.getOther(node);

			//If the other node is not already been assigned, then populate/filter it
			if (!assignments.containsKey(otherNode)){
				Set<N> neighbours = neighbourhoodAccess.findNeighbours(rel, node, vertex);

				if (candidates.containsKey(otherNode)){
					//If the candidates set exists, then filter it
					Set<N> temp = candidates.get(otherNode);

					//If there is filtering, then add the incoming conflict.
					if (!neighbours.containsAll(temp)){
						addConflictIn(node, otherNode, confIn);
					}

					temp.retainAll(neighbours);				
				} else {
					//Else populate it
					Set<N> temp = new HashSet<N>();
					temp.addAll(neighbours);
					candidates.put(otherNode, temp);

					//If there is populating, then add the incoming conflict.
					addConflictIn(node, otherNode, confIn);
				}

				//If the updated candidates set is empty, then add the outgoing conflict and return false 
				if (candidates.get(otherNode).isEmpty()){

					confOut.add(otherNode);
					//System.out.println("FAILED: " + node.getId() + ", " + assignments.get(node));
					return false;
				}
			}
		}

		return true;
	}


	//--------------------------//
	// HELPER METHODS
	//--------------------------//	

	//-------------------------//
	// KILLABLE FEATURES	
	//-------------------------//
	/**
	 * Sets the kill switch to true
	 */
	public void kill(){
		this.killed = true;
		System.out.print("KILLED ");
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
}
