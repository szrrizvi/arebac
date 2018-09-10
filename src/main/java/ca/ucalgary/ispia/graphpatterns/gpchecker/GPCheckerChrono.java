package ca.ucalgary.ispia.graphpatterns.gpchecker;

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

import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class provides the engine for checking if a given graph pattern
 * exists in the given database.
 * @author szrrizvi
 * @deprecated
 */

public class GPCheckerChrono implements GPChecker{

	public List<Map<MyNode, Node>> check(){
		return null;
	}
	
	private GraphDatabaseService graphDb;
	private Set<Set<Pair<MyNode, Node>>> conflicts;
	private int queryCount;
	
	
	public int getMaxNeighbourhood(){
		return 0;//neighbourhoodAccess.getMaxNeighbourhood();
	}
	
	public int getAllRes(){
		return 0;//this.allRes;
	}
	
	public int getSearchSpace(){
		return 0;//this.searchSpace;
	}
	
	/**
	 * Constructor to set the database.
	 * @param graphDb The database to set
	 */
	public GPCheckerChrono(GraphDatabaseService graphDb){
		this.graphDb = graphDb;
		queryCount= 0;
	}
	
	public int getQueryCount(){
		return this.queryCount;
	}
	
	/**
	 * Checks if the given graph pattern exists in the associated database.
	 * @param gp The graph pattern to test for.
	 * @return
	 */
	public boolean check(GraphPattern gp){
		conflicts = new HashSet<Set<Pair<MyNode, Node>>>();
		
		Map<MyNode, Node> assignments = new HashMap<MyNode, Node>();
		Set<MyNode> visitedNodes = new HashSet<MyNode>();
		
		List<MyNode> nodes = gp.getNodes();
		for (MyNode node : nodes){
			if (node.hasAttributes()){
				visitedNodes.add(node);
				
				try (Transaction tx = graphDb.beginTx()){
					Node vertex = graphDb.findNode(LabelEnum.PERSON, "id", Integer.parseInt(node.getAttribute("id")));
					if (vertex == null){
						System.out.println("Not fixed: " + node.getAttribute("id"));
					}
					assignments.put(node, vertex);
					tx.success();
				}
			}
		}
		
		//Check the direct relationships between the fixed nodes
		if (!preCheck(gp, assignments, visitedNodes)){
			//If the precheck fails, then return false.
			return false;
		}
		
		return checker(gp, assignments, visitedNodes);
	}
	
	/**
	 * This method checks the direct relationships between the fixed nodes.
	 * @param gp
	 * @param fixedNodes
	 * @return
	 */
	private boolean preCheck(GraphPattern gp, Map<MyNode, Node> fixedNodes, Set<MyNode> visitedNodes){
		
		//Get all of the relationships from GP.
		List<MyRelationship> rels = gp.getAllRelationships();
		
		//Iterate through the relationships
		for (MyRelationship rel : rels){
			//Find the relationships that contain the fixed nodes
			if (visitedNodes.contains(rel.getSource()) && visitedNodes.contains(rel.getTarget())){
				
				
				//Get the nodes
				Node source = fixedNodes.get(rel.getSource());
				Node target = fixedNodes.get(rel.getTarget());
				
				boolean passed = false;	//Flag for checking if relationship passed
				
				//Check if the relationship exists between them.
				try (Transaction tx = graphDb.beginTx()){
					queryCount++;
					Iterator<Relationship> relIte = source.getRelationships(Direction.OUTGOING).iterator();
					
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
				if (passed = false){
					return false;
				}
				
				//Remove the relationship from GP, because we already checked it.
				gp.removeRelationship(rel);
			}
		}
		
		//If reached here, the all of the relationships passed, thus return true.
		return true;
	}
	
	/**
	 * 
	 * @param gp
	 * @param assignments
	 * @param visitedNodes
	 * @return
	 */
	private boolean checker(GraphPattern gp, Map<MyNode, Node> assignments, Set<MyNode> visitedNodes){
		
		//If gp is empty then we have found the vertices that satisfy the graph pattern, and we're done!
		if (gp.isEmpty()){
			return true;
		}
		
		
		/*System.out.println(gp);
		System.out.println("Bounded Nodes");
		for (MyNode temp : assignments.keySet()){
			System.out.println(temp + " " + assignments.get(temp));
		}
		
		System.out.println("Seen Nodes");
		for (MyNode temp : visitedNodes){
			System.out.println(temp);
		}*/
		
		//Pick the next node to assign, along with its corresponding relationship
		Pair <MyNode, MyRelationship> pair = getNode(gp, visitedNodes);
		MyNode nextNode = pair.first;
		MyRelationship rel = pair.second;
		/*System.out.println("NextNode: " + nextNode);
		System.out.println("Rel: " + rel);
		System.out.println("X-----X\n");*/
		
		
		//Get the list of seen vertices, instead of having to extract the set at each iteration of the loop.
		Set<Node> fixedVertices = new HashSet<Node>();
		for (MyNode key : visitedNodes){
			fixedVertices.add(assignments.get(key));
		}
		
		//List to hold possible candidates of verticies that fit the node
		List<Node> vertices = new ArrayList<Node>();
		
		//Query the database for vertexes that fit the constraints.
		try (Transaction tx = graphDb.beginTx()){
		
			//First get the nodes in the source relationship
			MyNode other = rel.getOther(nextNode);
			Node v1 = assignments.get(other);
			Iterable<Relationship> rels;
			
			queryCount++;
			if (other.equals(rel.getSource())){
				rels = v1.getRelationships(Direction.OUTGOING);
			} else {
				rels = v1.getRelationships(Direction.INCOMING);
			}
			
			for (Relationship tempR : rels){
				Node v2 = tempR.getOtherNode(v1);
				
				//if (!fixedVertices.contains(v2)){
					vertices.add(v2);
				//}
			}
			
			tx.success();
		}

		//Get the constraints associated with the node
		Set<MyRelationship> cons = getConstraints(gp, nextNode, visitedNodes);
		
		//Store the vertices that satisfy all the constraints.
		Set<Node> validVertices = new  HashSet<Node>();
		
		//Iterate through the vertices and find ones who satisfy all of the constraints.
		for (Node v2 : vertices){
			try (Transaction tx = graphDb.beginTx()){
				
				//Get all of the neighbours of the current vertex (this avoids having to query
				//the database multiple times
				queryCount++;
				Iterable<Relationship> tempRels = v2.getRelationships();
				
				//Filter out the relations that don't seem relevant
				//I.E. The pair of vertices involved are not relevant to the constraints
				Set<Relationship> rels = new HashSet<Relationship>();
				
				for (Relationship tRel : tempRels){
					Node otherNode = tRel.getOtherNode(v2);
					if (fixedVertices.contains(otherNode)){
						rels.add(tRel);
					}
				}
				
	
				boolean failed = false;
				Iterator<MyRelationship> consIte = cons.iterator();
				
				//Iterate through the constraints, and check if they are satisfied
				while (consIte.hasNext() && !failed){
					
					//Get the next constraint
					MyRelationship con = consIte.next();
					Node v1 = assignments.get(con.getOther(nextNode));
					boolean conSat = false;
					
					//If 'nextNode' is the source node in the constraint, then there must be a 
					//relationship from nextNode (v2) to v1
					if (nextNode.equals(con.getSource())){
						for (Relationship r : rels){
							if (r.getEndNode().equals(v1)){
								//Relationship found!
								conSat = true;
							}
						}
					}
					//If 'nextNode' is the target node in the constraint, then there must be a 
					//relationship from v1 to nextNode (v2) 
					else if (nextNode.equals(con.getTarget())){
						for (Relationship r : rels){
							if (r.getStartNode().equals(v2)){
								//Relationship found!
								conSat = true;
							}
						}
					} else {
						//If nextNode is not involved in the constraint, then something
						//went wrong, and this is an illegal state
						System.out.println("Illegal State");
					}
					
					//If the constraint was not satisfied, then v2 is not a valid candidate for nextNode
					if (!conSat){
						failed = true;
					}
				}
				
				//If none of the constraints failed, then v2 is a valid candidate for nextNode
				if (!failed){
					validVertices.add(v2);
				}
				
				tx.success();
			}
		}
		
		//Update the graph pattern by removing the evaluated constraints
		for (MyRelationship con : cons){
			gp.removeRelationship(con);
		}
		
		//Add nextNode to visitedNodes, and attempt each vertex in validVertices for assignment
		visitedNodes.add(nextNode);
		for (Node v2 : validVertices){
			//assignments.put(nextNode, v2);
			
			/*try (Transaction tx = graphDb.beginTx()){
				
				System.out.println(nextNode.getId() + " " + v2.getProperty("id"));
				tx.success();
			}*/
			
			//Create a copy of the graph pattern to pass onto the next call.
			//Each recursive call removes some relationships from the GP, thus if we 
			//back track we would have to add those relationships back in.
			//It is easier to just pass a copy of the GP instead, thus we maintain the
			//state of the GP at this level.
			GraphPattern gpCopy = new GraphPattern(gp);
			Map<MyNode, Node> bnCopy = new HashMap<MyNode, Node>();
			bnCopy.putAll(assignments);
			bnCopy.put(nextNode, v2);
			Set<MyNode> snCopy = new HashSet<MyNode>();
			snCopy.addAll(visitedNodes);
			
			boolean result = checker(gpCopy, bnCopy, snCopy);
			
			if (result){
				//If the result was true, then return true.
				//System.out.println(nextNode + " " + v2);
				return true;
			}
		}
		
		//If none of the vertices worked out, then return false
		return false;
	}
	
	
	/**
	 * Returns a node from graph pattern that is connected to at least one of the already bounded nodes
	 * @param gp
	 * @param assignments
	 * @return
	 */
	private Pair<MyNode,MyRelationship> getNode(GraphPattern gp, Set<MyNode> visitedNodes){
		
		List<MyRelationship> rels = gp.getAllRelationships();
		
		for (MyNode node : visitedNodes){
			for (MyRelationship rel : rels){
				if (rel.getSource().equals(node)){
					Pair<MyNode, MyRelationship> pair = new Pair<MyNode, MyRelationship>(rel.getTarget(), rel);
					return pair;
				} else if (rel.getTarget().equals(node)) {
					Pair<MyNode, MyRelationship> pair = new Pair<MyNode, MyRelationship>(rel.getSource(), rel);
					return pair;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param gp
	 * @param node
	 * @param visitedNodes
	 * @return
	 */
	private Set<MyRelationship> getConstraints(GraphPattern gp, MyNode node, Set<MyNode> visitedNodes){
		
		//Get the list of all relationships
		List<MyRelationship> rels = gp.getAllRelationships();
		Set<MyRelationship> cons = new HashSet<MyRelationship>();
		
		//Iterate through all of the relationships and find the ones that contain
		//the given node and the 'other' node has already been seen
		for (MyRelationship rel : rels){
			MyNode other = rel.getOther(node);
			if (other != null){						//If other == null, then rel does not contain node
				if (visitedNodes.contains(other)){
					cons.add(rel);
				}
			}
		}
		
		return cons;
	}
}
