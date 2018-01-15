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

		//Populate and filter the immediate neighbours of the fixed nodes
		for (MyNode key : assignments.keySet()){
			if (!populateFilter(assignments, candidates, key)){
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
		check_rec(assignments, candidates);
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

	private Set<MyNode> check_rec(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates, Map<MyNode, Set<MyNode>> confOut, Map<MyNode, Set<MyNode>> confIn){

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
		consEval.mexFilter(nextNode, candidates.get(nextNode), assignments);

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

			//Perform forward checking
			boolean validVertex = populateFilter(assnClone, candsClone, nextNode);

			if (validVertex){
				//If we didn't abandon this vertex, then we can recurse
				//Recurse with clones of maps
				Set<MyNode> jumpNodes = check_rec(assnClone, candsClone);

				if (killed){
					return null;
				}

				if (jumpNodes != null && !jumpNodes.contains(nextNode)){
					return jumpNodes;
				}

				//if (rs != null && 
				//		rs.isJumpingForJoy() && 
				//		!rs.getJumpVariables().contains(nextNode)){
				//	return rs;
				//}
			}
		}

		return null;
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
	private boolean populateFilter(Map<MyNode, Node> assignments, Map<MyNode, Set<Node>> candidates, MyNode node, Map<MyNode, Set<MyNode>> confOut, Map<MyNode, Set<MyNode>> confIn){

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
					
					if (!neighbours.containsAll(temp)){
						addConflict(node, otherNode, confOut, confIn);
					}
					
					temp.retainAll(neighbours);				
				} else {
					//Else populate it
					candidates.put(otherNode, neighbours);
					
					addConflict(node, otherNode, confOut, confIn);
				}

				//If the updated candidates set is empty, then return false 
				if (candidates.get(otherNode).isEmpty()){
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

	private void addConflict(MyNode src, MyNode tgt, Map<MyNode, Set<MyNode>> confOut, Map<MyNode, Set<MyNode>> confIn){
		if (confOut.containsKey(src)){
			confOut.get(src).add(tgt);
		} else {
			Set<MyNode> confList = new HashSet<MyNode>();
			confList.add(tgt);
			confOut.put(src, confList);
		}

		if (confIn.containsKey(tgt)){
			confIn.get(tgt).add(src);
		} else {
			Set<MyNode> confList = new HashSet<MyNode>();
			confList.add(src);
			confIn.put(tgt, confList);
		}
	}
}
