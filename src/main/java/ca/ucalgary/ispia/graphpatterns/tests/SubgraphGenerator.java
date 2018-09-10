package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

public class SubgraphGenerator {
	private Random random;					//Random number generator
	private GraphDatabaseService graphDb;	//db interface
	private int totalGraphNodes;			//Total number of nodes in the db

	private List<Relationship> rels;		//List of relationships for GP
	private List<Node> allNodes;			//List of nodes for GP.
	Map<Node, MyNode> nodesMap;				//Map from the db node to my node in the gp
	Map<Relationship, MyRelationship> relsMap; //Map from the db relationships to my relationships in the gp

	private int endSize, rooted;			//endsize = the minimal number of nodes after phase one
	//rooted = the number of rooted nodes
	private double complete;				//The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary

	private int numMex;						//Number of mutual exclusion constraints
	private int numVAttrs;					//Number of vertex attributes required
	private int numEAttrs;					//Number of edge attributes required

	private int resSize;					//Number of variables in the result schema
	
	private GPHolder gpHolder;				//The resulting graph pattern holder.


	/**
	 * Initialize the fields appropriately. 
	 * @param graphDb The graph database interface
	 * @param totalGraphNodes The total number of nodes in the graph db
	 * @param random The random number generator
	 * @param endSize The minimal number of relationships after phase one
	 * @param complete The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary
	 * @param rooted The number of rooted nodes
	 * @param numMex The number of mutual exclusion constraitns
	 * @param numAttrs The number of attribute requirements
	 */
	public SubgraphGenerator(GraphDatabaseService graphDb, int totalGraphNodes, Random random, int endSize, double complete,  int rooted, int numMex, int numVAttrs, int numEAttrs, int resSize){

		//Set the graphdb and the random number generator
		this.graphDb = graphDb;
		this.totalGraphNodes = totalGraphNodes;
		this.random = random;

		//Set the parameters
		this.endSize = endSize;
		this.rooted = rooted;
		this.complete = complete;
		this.numMex = numMex;
		this.numVAttrs = numVAttrs;
		this.numEAttrs = numEAttrs;
		this.resSize = resSize;

		//initialize the lists
		this.rels = new ArrayList<Relationship>();
		this.allNodes = new ArrayList<Node>();
		nodesMap = new HashMap<Node, MyNode>();
		relsMap = new HashMap<Relationship, MyRelationship>();

	}

	//////////////////////////////////////
	//									//
	// Exposed Methods					//
	//									//
	//////////////////////////////////////
	/**
	 * Creates a GPHolder based on the data in the database.
	 * @return A randomly generated GPHolder
	 */
	public GPHolder createDBBasedGP(){
		return createDBBasedGP(null);
	}

	/**
	 * Creates a GPHolder based on the data in the database, and using the inpu parameters as a starting point.
	 * 1) Pick a random node from the database
	 * 2) Query for all neighbours.
	 * 3) Randomly pick neighbours.
	 * 4) Add each picked relationship to set E.
	 * 5) Add each picked neighbour node to set N.01
	 * 6) Pick and remove a random node from N and repeat steps 2 to 5 until there are endSize relationships in E.
	 * @param inputNodes The list of nodes as the starting point
	 * @param inputRels The list of relationships as the starting point
	 * @return The randomly generated GPH based on the database and parameters
	 */
	public GPHolder createDBBasedGP(Node seed){

		//Setup allNodes and nodesPool
		if (seed == null){
			//Randomly pick the very first node if the input nodes list is blank
			Node node = pickRandomNode();
			allNodes.add(node);
		} else {
			//If the input nodes list is not blank, then add the input nodes list 
			//to nodePool and allNodes
			allNodes.add(seed);
		}

		//Gather the nodesfor the patterns
		phaseOne();

		//For all pairs of nodes in allNodes, if a relationship exists between them
		//in the database, then ensure that relationship also exists in rels.
		phaseTwo();

		//Make sure that gp passes the minimum number of relationships requirement. 
		int minRels = (int)Math.floor((allNodes.size()-1)*complete);

		if (rels.size() < minRels){
			System.out.println("failed here: " + complete + ", " + minRels + " " + rels.size() + " " + allNodes.size());
			return null; 
		}


		//Translate the allNodes and rels lists to GraphPattern
		GraphPattern gp = translateToGP(); 

		//Generate the mutual exclusion constraints
		List<Pair<MyNode, MyNode>> mex = generateMex(gp.getNodes());

		//Generate the GPHolder
		this.gpHolder = new GPHolder(gp, mex, new HashMap<String, MyNode>());
		
		//Check if result schema size is greater than the number of nodes
		if (resSize > gp.getNodes().size()){
			return null;
		}
		
		//Set the result schema
		List<MyNode> resultSchema = new ArrayList<MyNode>();
		resultSchema.addAll(gp.getNodes());
		while(resultSchema.size() > resSize){
			resultSchema.remove(random.nextInt(resultSchema.size()));
		}
		this.gpHolder.setResultSchema(resultSchema);

		return this.gpHolder;
	}


	/**
	 * Picks random nodes, and their appropriate MyNode mapping, from the list of nodes.
	 * @param numNodes The number of nodes to pick.
	 * @return A randomly generated map from Node to MyNode of size n.
	 */
	public Map<Node, MyNode> extractNodes(int numNodes){
		//If numNodes is less than one or there aren't enough nodes, return null
		if (numNodes < 1 || nodesMap.size() < numNodes){
			return null;
		} 

		//Initialize the result set
		Map<Node, MyNode> result = new HashMap<Node, MyNode>();

		//Special case: If there are no relationships, then return all nodes.
		if(rels.size() == 0){
			for (Node node : nodesMap.keySet()){
				result.put(node, nodesMap.get(node));
			}

			return result;
		}

		//tempRels (used for crawling through the gp)
		List<Relationship> tempRels = new ArrayList<Relationship>();
		tempRels.addAll(relsMap.keySet());

		//Shuffle the tempRels list for random order.
		Collections.shuffle(tempRels, random);

		boolean first = true;	//flag for first run

		while (result.size() < numNodes) {	//Loop until we have reached the required result size.
			try (Transaction tx = graphDb.beginTx()){				
				if (first){			//On the first run
					first = false;	//Update flag

					//Pick a random relationship and extract its nodes
					Relationship rel = tempRels.get(random.nextInt(tempRels.size()));
					tempRels.remove(rel);
					Node src = rel.getStartNode();
					Node tgt = rel.getEndNode();


					if (numNodes > 1){
						//If numNodes > 1, then add both nodes to result
						result.put(src, nodesMap.get(src));
						result.put(tgt, nodesMap.get(tgt));
					} else {
						//If numNodes == 1, then randomly pick one of the nodes to add to result
						if (random.nextBoolean()){
							result.put(src, nodesMap.get(src));
						} else {
							result.put(tgt, nodesMap.get(tgt));
						}
					}
				} else {	//On subsequent runs

					boolean done = false;	//Flag to specify we're done with this iteration
					int idx = 0;			//Loop index counter
					while (!done){
						//Pick the next relationship in tempRels
						Relationship rel = tempRels.get(idx);
						idx++;
						Node other = null;

						if (result.keySet().contains(rel.getStartNode()) && (!result.keySet().contains(rel.getEndNode()))){
							//If the startNode of the relationship is already in results, but not the end node
							//then set other to be the end node
							other = rel.getEndNode();
							done = true;
						} else if ((!result.keySet().contains(rel.getStartNode())) && result.keySet().contains(rel.getEndNode())){
							//If the startNode of the relationship is not in results, but the end node is
							//then set other to be the start node
							other = rel.getStartNode();
							done = true;
						} else if (result.keySet().contains(rel.getStartNode()) && result.keySet().contains(rel.getEndNode())){
							//If both of the nodes of the relationship are in result, remove it from the list
							tempRels.remove(rel);
						}

						//If we found a matching relationship
						if (done){
							//Add other to the result, and its corresponding mapping
							result.put(other, nodesMap.get(other));
							//Remove rel from the list
							tempRels.remove(rel);
						}
					}

				}
				tx.success();
			}
		}
		return result;
	}

	/**
	 * Finds the corresponding MyNode object for the input Node argument.
	 * @param node The input node argument.
	 * @return The MyNode object mapped to the input Node object.
	 */
	public MyNode findMyNode(Node node){
		return nodesMap.get(node);
	}

	/**
	 * @return The GPHolder object
	 */
	public GPHolder getGPHolder(){
		return this.gpHolder;
	}


	//////////////////////////////////////
	//									//
	// The Two Main Phases				//
	//									//
	//////////////////////////////////////


	/**
	 * The first part of generating the graph pattern. Populates allNodes.
	 * At the end of the method, all of the nodes in allNodes are connected to each other.
	 * Assumption: allNodes has at least one one node at the start.
	 * Assumption: If allNodes has more than one node at the start, then there is a path between
	 * any two pairs of nodes, through only the nodes in the list.
	 */
	private void phaseOne(){

		while (allNodes.size() < endSize){

			try (Transaction tx = graphDb.beginTx()){
				//Pick a random node from allNodes
				int idx = random.nextInt(allNodes.size());
				Node node = allNodes.get(idx);

				//Get all relationships of node				
				Iterable<Relationship> ite = node.getRelationships(Direction.BOTH);
				List<Relationship> tempRels = new ArrayList<Relationship>();

				//Pick one of the relationships uniformly at random.
				for (Relationship rel : ite){
					tempRels.add(rel);
				}

				idx = random.nextInt(tempRels.size());
				Relationship rel = tempRels.get(idx);
				Node neighbour = rel.getOtherNode(node);

				//Add the neighbour to allNodes
				if (!allNodes.contains(neighbour)){
					allNodes.add(neighbour);
				}

				tx.success();

			}
		}
		//If reached here, then phase one completed successfully.
		return;
	}

	/**
	 * Finds all relationships in the database, between the nodes in allNodes, then adds them to
	 * the list rels.
	 */
	private void phaseTwo(){

		Collections.shuffle(allNodes, random);
		List<Pair<Node, Node>> pairs = new ArrayList<Pair<Node, Node>>();
		
		//For each node in allNode, get all relationshpis and iterate through each relationship.
		for (Node n1 : allNodes){

			try (Transaction tx = graphDb.beginTx()){
				//If a node n1 is related to any other node in allNodes, add those relationships to rels.
				//Avoid duplication, and self loops.
				Iterable<Relationship> ite = n1.getRelationships(Direction.BOTH);				

				for (Relationship rel : ite){
					Node n2 = rel.getOtherNode(n1);	//Get the other node
					if (allNodes.contains(n2) && !n1.equals(n2)){					//If n2 is part of allNodes and n1 != n2
						if (!rels.contains(rel)){									//If the relationship is not already part of rels
							Pair<Node, Node> pA = new Pair<Node, Node>(n1, n2);
							Pair<Node, Node> pB = new Pair<Node, Node>(n2, n1);

							if (!pairs.contains(pA)){
								rels.add(rel);											//Add the relationship to the lists.
								pairs.add(pA);
								pairs.add(pB);
							}
						}
					}
				}
				tx.success();
			}
		}
		return;
	}



	//////////////////////////////////////
	//									//
	// Helper Methods					//
	//									//
	//////////////////////////////////////

	/**
	 * @return A random node from the database
	 */
	private Node pickRandomNode(){
		Node node = null;

		try (Transaction tx = graphDb.beginTx()){
			//Find a random node from the database, using the unique id attribute
			int nodeId = random.nextInt(totalGraphNodes);
			node = graphDb.findNode(LabelEnum.PERSON, "id" , nodeId);
			tx.success();
		}
		return node;
	}

	/**
	 * Uses the lists allNodes and rels to generate a graph pattern
	 * @return A graph pattern based on the contents of allNodes and rels
	 */
	private GraphPattern translateToGP(){

		//Generate the graph pattern object
		GraphPattern gp = new GraphPattern();


		//For each Node object, create an associated MyNode object
		int nodeCount = 0;
		for (Node n : allNodes){
			MyNode myNode = new MyNode(nodeCount, "PERSON");
			nodesMap.put(n, myNode);
			gp.addNode(myNode);

			nodeCount++;
		}

		//For k random MyNodes add the id as an attribute/property.
		//This id is used for cypher queries.
		//This process uses simple random sampling
		if (rooted > allNodes.size()){
			rooted = allNodes.size();
		}

		List<Node> allNodesClone = new ArrayList<Node>();
		allNodesClone.addAll(allNodes);

		for (int i = 0; i < rooted; i++){
			//Pick a random node from allNodes and get it's corresponding MyNode
			int idx = random.nextInt(allNodesClone.size());
			Node node = allNodesClone.get(idx);
			MyNode myNode = nodesMap.get(node);

			//Add the property to myNode
			try (Transaction tx = graphDb.beginTx()){
				myNode.addAttribute("id", node.getProperty("id")+"");
				tx.success();
			}
			//Remove the node from allNodesClone list.
			allNodesClone.remove(idx);
		}

		//Process the relationships
		int relCount = 0;
		String relPrefix = "rel";

		for (Relationship r : rels){

			MyNode source = null, target = null;
			RelType type = null;

			//For each relationship in rels, create a corresponding relationship in gp.
			try (Transaction tx = graphDb.beginTx()){
				source = nodesMap.get(r.getStartNode());
				target = nodesMap.get(r.getEndNode());
				type = GPUtil.translateRelType(r.getType());
				tx.success();
			}

			MyRelationship rel = new MyRelationship(source, target, type, relCount);
			relCount++;

			if (relCount >= 25){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}

			gp.addRelationship(rel);
			relsMap.put(r, rel);
		}

		//Set the attribute requirements
		attrsReq(gp, true);
		attrsReq(gp, false);
		return gp;
	}

	/**
	 * Randomly generates and returns a list of mutual exclusion constraints, of size numMex.
	 * @param nodes The list of all MyNode objects for the constraints.
	 * @return The list of mutual exclusion constraints.
	 */
	private List<Pair<MyNode, MyNode>> generateMex(List<MyNode> nodes){

		//Sanity check
		if (numMex <= 0){
			return new ArrayList<Pair<MyNode, MyNode>>();
		}

		//Generate all possible mutual exclusion constraints. 
		List<Pair<MyNode, MyNode>> result = new ArrayList<Pair<MyNode, MyNode>>();
		for (int idxA = 0; idxA < nodes.size(); idxA++){
			for (int idxB = idxA+1; idxB < nodes.size(); idxB++){
				Pair<MyNode, MyNode> mex = new Pair<MyNode, MyNode>(nodes.get(idxA), nodes.get(idxB));
				result.add(mex);
			}
		}

		//Shuffle the result list for random order.
		Collections.shuffle(result, random);

		//Repeatedly remove constraints from the list until we reach the numMex size.
		while (result.size() > numMex){
			result.remove(random.nextInt(result.size()));
		}

		return result;
	}

	/**
	 * Sets the edge attribute requirements in the graph pattern
	 * @param gp
	 */
	private void attrsReq(GraphPattern gp, boolean nodeAttrs){

		List<Entity> keys = new ArrayList<Entity>();
		Map<Entity, Map<String, Object>> attrsAll = new HashMap<Entity, Map<String, Object>>();	
		int loopMax = 0;

		if (nodeAttrs){
			loopMax = numVAttrs;
			//Get all of the attributes of all of the nodes.
			//Store the nodes in the list called "keys"
			//attrsAll is a map where the keys are the nodes, and the values are a further map
			//where the keys are the attribute name, and values are the attribute values
			for (Node node : allNodes){
				try (Transaction tx = graphDb.beginTx()){
					Map<String, Object> props = node.getAllProperties();

					if (props != null && !props.isEmpty()){
						attrsAll.put(node, props);
						keys.add(node);
					}

					tx.success();
				}
			}
		} else {
			loopMax = numEAttrs;
			//Get all of the attributes of all of the relationships.
			//Store the relationships in the list called "keys"
			//attrsAll is a map where the keys are the relationship, and the values are a further map
			//where the keys are the attribute name, and values are the attribute values
			for (Relationship rel : relsMap.keySet()){
				try (Transaction tx = graphDb.beginTx()){
					Map<String, Object> props = rel.getAllProperties();

					if (props != null && !props.isEmpty()){
						attrsAll.put(rel, props);
						keys.add(rel);
					}

					tx.success();
				}
			}
		}

		//Loop numEAttrs times
		for (int i = 0; i < loopMax; i++){
			//Return if we have run out of keys.
			if (keys.isEmpty()){
				return;
			}

			//Randomly pick an entity from the keys list
			Entity ent = keys.get(random.nextInt(keys.size()));

			//For the entity, randomly pick an attribute name-value pair.
			List<String> attrKeys = new ArrayList<String>();
			attrKeys.addAll(attrsAll.get(ent).keySet());
			String attr = attrKeys.get(random.nextInt(attrKeys.size()));
			String val = attrsAll.get(ent).get(attr)+"";

			if (!attr.equals("id") && !attr.equals("name") && !attr.equals("degree")){
				//Add the attribute name-value pair to corresponding GP object
				if (nodeAttrs){
					MyNode vertex = nodesMap.get((Node)ent);
					vertex.addAttribute(attr, val);
				} else {
					MyRelationship edge = relsMap.get((Relationship)ent);
					edge.addAttribute(attr, val);
				}
			} else {
				i--;
			}

			//Remove the chosen attribute name-value pair from the inner map
			attrsAll.get(ent).remove(attr);
			//If the entity has exhausted all of its attributes, remove it
			//from the keys list.
			if (attrsAll.get(ent).isEmpty()){
				keys.remove(ent);
			}
		}		
	}

}
