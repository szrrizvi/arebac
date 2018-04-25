package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
import ca.ucalgary.ispia.graphpatterns.util.AttributeTypes;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.LabelEnum;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class generates the test cases for the evaluation process.
 * @author szrrizvi
 *
 */
public class EvalTestGenerator {

	private String nodePrefix;				//The prefix for the node names

	private Random random;					//Random number generator
	private GraphDatabaseService graphDb;	//db interface
	private float p;						//probability to keep a relationship

	private List<Relationship> rels;		//List of relationships for GP
	private List<Node> allNodes, nodePool;	//List of nodes for GP. nodePool is used for generating the GP
	Map<Node, MyNode> nodesMap;				//Map from the db node to my node in the gp
	Map<Relationship, MyRelationship> relsMap; //Map from the db relationships to my relationships in the gp

	private int endSize, rooted;			//endsize = the minimal number of relationships after phase one
	//rooted = the number of rooted nodes
	private double complete;				//The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary

	private int numMex;						//Number of mutual exclusion constraints
	private int numVAttrs;					//Number of vertex attributes required
	private int numEAttrs;					//Number of edge attributes required

	private GPHolder gpHolder;				//The resulting graph pattern holder.

	private Set<Pair<Node, Node>> pairs = null;	//Set of node pairs to prevent relationship duplication		


	/**
	 * Initialize the fields appropriately. 
	 * @param graphDb The graph database interface
	 * @param random The random number generator
	 * @param endSize The minimal number of relationships after phase one
	 * @param complete The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary
	 * @param rooted The number of rooted nodes
	 * @param p	The probability to keep a relationship
	 * @param numMex The number of mutual exclusion constraitns
	 * @param numAttrs The number of attribute requirements
	 * @param nodePrefix The prefix for the node names
	 */
	public EvalTestGenerator(GraphDatabaseService graphDb, Random random, int endSize, double complete,  int rooted, float p, int numMex, int numVAttrs, int numEAttrs, String nodePrefix){

		//Set the graphdb and the random number generator
		this.graphDb = graphDb;
		this.random = random;

		//Set the parameters
		this.endSize = endSize;
		this.rooted = rooted;
		this.complete = complete;
		this.p = p;
		this.numMex = numMex;
		this.numVAttrs = numVAttrs;
		this.numEAttrs = numEAttrs;
		this.nodePrefix = nodePrefix;

		//initialize the lists
		this.rels = new ArrayList<Relationship>();
		this.allNodes = new ArrayList<Node>();
		this.nodePool = new ArrayList<Node>();
		nodesMap = new HashMap<Node, MyNode>();
		relsMap = new HashMap<Relationship, MyRelationship>();
		pairs = new HashSet<Pair<Node, Node>>();

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
	public GPHolder createDBBasedGP(List<Node> inputNodes){


		//Setup allNodes and nodesPool
		if (inputNodes == null || inputNodes.size()==0){
			//Randomly pick the very first node if the input nodes list is blank
			Node node = pickRandomNode();
			nodePool.add(node);
			allNodes.add(node);
		} else {
			//If the input nodes list is not blank, then add the input nodes list 
			//to nodePool and allNodes
			nodePool.addAll(inputNodes);
			allNodes.addAll(inputNodes);
		}


		//Gather the nodes and relationships for the patterns
		if (!phaseOne(inputNodes)){
			return null;
		}

		//For all pairs of nodes in allNodes, if a relationship exists between them
		//in the database, then ensure that relationship also exists in rels.
		phaseTwo();


		//Make sure that gp passes the size requirement.
		if (endSize > 4){
			//# of maximum possible edges = (gp.getNodes().size() * (gp.getNodes().size()-1))/2) 
			int finalSize = (int)Math.floor(complete * ((allNodes.size() * (allNodes.size()-1))/2));

			if (rels.size() < finalSize){
				System.out.println("failed here: " + complete + ", " + finalSize + " " + rels.size() + " " + allNodes.size());
				return null; 
			}
		}

		//Translate the allNodes and rels lists to GraphPattern
		GraphPattern gp = translateToGP(); 

		//Generate the mutual exclusion constraints
		List<Pair<MyNode, MyNode>> mex = generateMex(gp.getNodes());

		//Generate and return the GPHolder
		this.gpHolder = new GPHolder(gp, mex, new HashMap<String, MyNode>());
		return this.gpHolder;
	}


	/**
	 * Picks n random nodes, and their appropriate MyNode mapping, from the list of nodes.
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
	 * @return A random node from the database
	 */
	private Node pickRandomNode(){
		Node node = null;

		try (Transaction tx = graphDb.beginTx()){
			//Find a random node from the database, using the unique id attribute
			//Note: There are 82,168 nodes in this database. MAGIC NUMBER!
			int nodeId = random.nextInt(82168);
			node = graphDb.findNode(LabelEnum.PERSON, "id" , nodeId);
			tx.success();
		}

		return node;

	}

	/**
	 * The first part of generating the graph pattern. Populates allNodes and rels lists.
	 */
	private boolean phaseOne(List<Node> seeds){
		//Flags for sanity checks
		boolean firstNode = true;
		boolean isEmpty = true;

		if (seeds == null){
			seeds = new ArrayList<Node>();
		}

		while (allNodes.size() < endSize){
			//If the nodePool is empty before reaching the endSize
			//then return null (phase one failed)
			if (nodePool.size() == 0){
				return false;
			}

			try (Transaction tx = graphDb.beginTx()){
				//Use the seeds nodes first, and then move on to the other nodes
				Node node = null;
				for (int idx = 0; idx < seeds.size(); idx++){
					Node seed = seeds.get(idx);
					//If the seed is in the nodes pool, then use this seed as
					//the next node
					if (nodePool.contains(seed)){
						node = seed;
					}
				}

				//If the seeds are all used, then pick a random node from the nodesPool
				if(node == null){
					//Pick a random node from the nodes list
					int idx = random.nextInt(nodePool.size());
					node = nodePool.get(idx);
				}

				//Get all relationships of node
				Iterable<Relationship> ite = node.getRelationships(Direction.BOTH);

				//Iterate through all of the relationships
				for (Relationship r : ite){
					isEmpty = false;
					//Decide with probability p, if we should keep or discard the relationship
					float prob = random.nextFloat();
					Pair<Node, Node> tempP = new Pair<Node, Node>(node, r.getOtherNode(node));

					if (prob <= p && allNodes.size() < endSize && !rels.contains(r) && !pairs.contains(tempP)){
						rels.add(r);	//Add the relatioship to rels
						pairs.add(tempP);
						tempP = new Pair<Node, Node> (r.getOtherNode(node), node);
						pairs.add(tempP);


						//If the neighbour hasn't been seen before, add it to allNodes and nodePool.

						Node other = r.getOtherNode(node);
						if (!allNodes.contains(other)){
							allNodes.add(other);
							nodePool.add(other);
						}
					}	
				}

				tx.success();

				//If the first node had no neighbours
				//return null (phase one failed)
				if (firstNode && isEmpty){
					return false;
				}
				else if (firstNode){
					//If the first node had neighbours, but none were selected, retry the node
					//This is done at most twice, so that we don't run into an infinite loop.
					if(rels.size() > 0 ){
						nodePool.remove(node);
					}
					firstNode = false;
				} else {
					//Remove the curernt node from the nodePool
					nodePool.remove(node);
				}
			}
		}

		//If reached here, then phase one completed successfully.
		return true;
	}

	/**
	 * Finds all relationships in the database, between the nodes in allNodes, then adds them to
	 * the list rels.
	 */
	private void phaseTwo(){
		//If a node n1 is related to any other node in allNodes, add those relationships to rels.
		//Avoid duplication, and self loops.

		for (Node n1 : allNodes){
			try (Transaction tx = graphDb.beginTx()){
				Iterable<Relationship> ite = n1.getRelationships(Direction.BOTH);

				for (Relationship rel : ite){
					Node n2 = rel.getOtherNode(n1);

					if (allNodes.contains(n2) && !n1.equals(n2)){
						Pair<Node, Node> tempP = new Pair<Node, Node>(n1, n2);
						if (!rels.contains(rel)){
							if (!pairs.contains(tempP)){
								rels.add(rel);
								pairs.add(tempP);
								tempP = new Pair<Node, Node>(n2, n1);
								pairs.add(tempP);
							} else {
								if (random.nextBoolean()){
									rels.add(rel);
									pairs.add(tempP);
									tempP = new Pair<Node, Node>(n2, n1);
									pairs.add(tempP);
								}
							}
						}
					}
				}

				tx.success();
			}
		}
	}

	/**
	 * Uses the lists allNodes and rels to generate a graph pattern
	 * @return A graph pattern based on the contents of allNodes and rels
	 */
	private GraphPattern translateToGP(){

		//Start creating the GraphPattern Object

		//Generate the graph pattern object
		GraphPattern gp = new GraphPattern();


		//For each Node object, create an associated MyNode object
		int z = 0;
		for (Node n : allNodes){

			MyNode myNode = new MyNode(nodePrefix + Character.toString((char) (97 + z)), "PERSON");
			nodesMap.put(n, myNode);
			gp.addNode(myNode);
			
			z++;
			if (z == 26){
				z = 0;
				nodePrefix = nodePrefix+"A";
			}
		}


		//For k random MyNodes add the id as an attribute/property as well.
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

		int relCount = 0;
		
		String relPrefix = "rel";
		
		for (Relationship r : rels){

			MyNode source = null, target = null;
			RelType type = null;

			//For each relationship in rels, create a corresponding relationship in gp.
			//Note: this will add the appropriate nodes as well.

			try (Transaction tx = graphDb.beginTx()){

				source = nodesMap.get(r.getStartNode());
				target = nodesMap.get(r.getEndNode());
				type = GPUtil.translateRelType(r.getType());

				tx.success();
			}

			MyRelationship rel = new MyRelationship(source, target, type, relPrefix + ((char) (97 + relCount)));
			relCount++;
			MyRelationship relOpp = new MyRelationship(target, source, type, relPrefix + ((char) (97 + relCount)));
			relCount++;
			
			if (relCount >= 25){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}

			if (!source.equals(target) && !gp.getAllRelationships().contains(relOpp)){
				gp.addRelationship(rel);
				relsMap.put(r, rel);
			}
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
		if (numMex < 0){
			return null;
		} else if (numMex == 0){
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
