package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.DataSetWrapper;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class generates the test cases for the evaluation process.
 * @author szrrizvi
 *
 */
public class SimTestGenerator {
	private Random random;					//Random number generator
	private DataSetWrapper dataSet;			//graph dataset
	private float p;						//probability to keep a relationship

	private List<MyRelationship> rels;			//List of relationships for GP
	private List<MyNode> allNodes, nodePool;	//List of nodes for GP. nodePool is used for generating the GP
	Map<MyNode, MyNode> nodesMap;				//Map from the ds node to gp node
	Map<MyRelationship, MyRelationship> relsMap; //Map from the ds relationships to gp relationships

	private int endSize, rooted;			//endsize = the minimal number of nodes after phase one
	//rooted = the number of rooted nodes
	private double complete;				//The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary

	private GPHolder gpHolder;				//The resulting graph pattern holder.

	private Set<Pair<MyNode, MyNode>> pairs = null;	//Set of node pairs to prevent relationship duplication		


	/**
	 * Initialize the fields appropriately. 
	 * @param dataSet The graph dataset
	 * @param random The random number generator
	 * @param endSize The minimal number of relationships after phase one
	 * @param complete The minimal percent of the complete graph (after phase one). 0 = all acceptable, 1 = complete graph necessary
	 * @param rooted The number of rooted nodes
	 * @param p	The probability to keep a relationship
	 */
	public SimTestGenerator(DataSetWrapper dataSet, Random random, int endSize, double complete,  int rooted, float p){

		//Set the graphdb and the random number generator
		this.dataSet = dataSet;
		this.random = random;

		//Set the parameters
		this.endSize = endSize;
		this.rooted = rooted;
		this.complete = complete;
		this.p = p;

		//initialize the lists
		this.rels = new ArrayList<MyRelationship>();
		this.allNodes = new ArrayList<MyNode>();
		this.nodePool = new ArrayList<MyNode>();
		nodesMap = new HashMap<MyNode, MyNode>();
		relsMap = new HashMap<MyRelationship, MyRelationship>();
		pairs = new HashSet<Pair<MyNode, MyNode>>();

	}

	/**
	 * Finds the corresponding MyNode object for the input Node argument.
	 * @param node The input node argument.
	 * @return The MyNode object mapped to the input Node object.
	 */
	public MyNode findMyNode(MyNode node){
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
	public GPHolder createDBBasedGP(List<MyNode> inputNodes){

		//Setup allNodes and nodesPool
		if (inputNodes == null || inputNodes.size()==0){
			//Randomly pick the very first node if the input nodes list is blank
			MyNode node = pickRandomNode();
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
			int minRels = (int)Math.floor(complete * ((allNodes.size() * (allNodes.size()-1))/2));
			if (rels.size() < minRels){
				System.out.println("failed here: " + complete + ", " + minRels + " " + rels.size() + " " + allNodes.size());
				return null; 
			}
		}

		//Translate the allNodes and rels lists to GraphPattern
		GraphPattern gp = translateToGP(); 

		//Generate and return the GPHolder
		this.gpHolder = new GPHolder(gp, null, new HashMap<String, MyNode>());
		assignResultSchema();
		return this.gpHolder;
	}


	/**
	 * Picks random nodes, and their appropriate MyNode mapping, from the list of nodes.
	 * @param numNodes The number of nodes to pick.
	 * @return A randomly generated map from Node to MyNode of size n.
	 */
	public Map<MyNode, MyNode> extractNodes(int numNodes){
		//If numNodes is less than one or there aren't enough nodes, return null
		if (numNodes < 1 || nodesMap.size() < numNodes){
			return null;
		} 

		//Initialize the result set
		Map<MyNode, MyNode> result = new HashMap<MyNode, MyNode>();

		//Special case: If there are no relationships, then return all nodes.
		if(rels.size() == 0){
			for (MyNode node : nodesMap.keySet()){
				result.put(node, nodesMap.get(node));
			}

			return result;
		}

		//tempRels (used for crawling through the gp)
		List<MyRelationship> tempRels = new ArrayList<MyRelationship>();
		tempRels.addAll(relsMap.keySet());

		//Shuffle the tempRels list for random order.
		Collections.shuffle(tempRels, random);

		boolean first = true;	//flag for first run

		while (result.size() < numNodes) {	//Loop until we have reached the required result size.				
			if (first){			//On the first run
				first = false;	//Update flag

				//Pick a random relationship and extract its nodes
				MyRelationship rel = tempRels.get(random.nextInt(tempRels.size()));
				tempRels.remove(rel);
				MyNode src = rel.getSource();
				MyNode tgt = rel.getTarget();


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
					MyRelationship rel = tempRels.get(idx);
					idx++;
					MyNode other = null;

					if (result.keySet().contains(rel.getSource()) && (!result.keySet().contains(rel.getTarget()))){
						//If the startNode of the relationship is already in results, but not the end node
						//then set other to be the end node
						other = rel.getTarget();
						done = true;
					} else if ((!result.keySet().contains(rel.getSource())) && result.keySet().contains(rel.getTarget())){
						//If the startNode of the relationship is not in results, but the end node is
						//then set other to be the start node
						other = rel.getSource();
						done = true;
					} else if (result.keySet().contains(rel.getSource()) && result.keySet().contains(rel.getSource())){
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
		}
		return result;
	}

	/**
	 * @return A random node from the dataset
	 */
	private MyNode pickRandomNode(){
		
		MyNode[] nodes = new MyNode[dataSet.getNodes().size()];
		nodes = dataSet.getNodes().toArray(nodes);
		
		MyNode node = null;
		
		while (node == null){
			int randVal = random.nextInt(nodes.length);
			node = nodes[randVal];
		}

		return node;

	}

	/**
	 * The first part of generating the graph pattern. Populates allNodes and rels lists.
	 */
	private boolean phaseOne(List<MyNode> seeds){
		//Flags for sanity checks
		boolean firstNode = true;
		boolean isEmpty = true;

		if (seeds == null){
			seeds = new ArrayList<MyNode>();
		}

		while (allNodes.size() < endSize){
			//If the nodePool is empty before reaching the endSize
			//then return null (phase one failed)
			if (nodePool.size() == 0){
				return false;
			}

			//Use the seeds nodes first, and then move on to the other nodes
			MyNode node = null;
			for (int idx = 0; idx < seeds.size(); idx++){
				MyNode seed = seeds.get(idx);
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
			Iterable<MyRelationship> ite = dataSet.getAllRelationships(node);

			//Iterate through all of the relationships
			for (MyRelationship r : ite){
				isEmpty = false;
				//Decide with probability p, if we should keep or discard the relationship
				float prob = random.nextFloat();
				Pair<MyNode, MyNode> tempP = new Pair<MyNode, MyNode>(node, r.getOther(node));

				if (prob <= p && allNodes.size() < endSize && !rels.contains(r) && !pairs.contains(tempP)){
					rels.add(r);	//Add the relatioship to rels
					pairs.add(tempP);
					tempP = new Pair<MyNode, MyNode> (r.getOther(node), node);
					pairs.add(tempP);


					//If the neighbour hasn't been seen before, add it to allNodes and nodePool.
					MyNode other = r.getOther(node);
					if (!allNodes.contains(other)){
						allNodes.add(other);
						nodePool.add(other);
					}
				}	
			}


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

		for (MyNode n1 : allNodes){
			Iterable<MyRelationship> ite = dataSet.getAllRelationships(n1);
			//For each node in allNode, get all relationshpis and iterate through each relationship.

			for (MyRelationship rel : ite){
				MyNode n2 = rel.getOther(n1);	//Get the other node

				if (allNodes.contains(n2) && !n1.equals(n2)){					//If n2 is part of allNodes and n1 != n2
					Pair<MyNode, MyNode> tempP = new Pair<MyNode, MyNode>(n1, n2);		//Create the corresponding Pair for the pair of nodes
					if (!rels.contains(rel)){									//If the relationship is not already part of rels
						if (!pairs.contains(tempP)){							//If the reverse of the relationship is not already part of rels
							rels.add(rel);										//Add the relationship and the corresponding pairs to the lists.
							pairs.add(tempP);
							tempP = new Pair<MyNode, MyNode>(n2, n1);
							pairs.add(tempP);
						} else {												//If the reverse of the relationship is already part of rels
							if (random.nextBoolean()){							//Flip a coin to see if we should add the relationships to rels
								rels.add(rel);									//This way we all "reverse" relationships
							}
						}
					}
				}
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
		int nodeCount = 0;
		for (MyNode n : allNodes){

			MyNode myNode = new MyNode(nodeCount, "PERSON");
			nodesMap.put(n, myNode);
			gp.addNode(myNode);

			nodeCount++;
		}


		//For k random MyNodes add the id as an attribute/property as well.
		//This id is used for cypher queries.
		//This process uses simple random sampling

		if (rooted > allNodes.size()){
			rooted = allNodes.size();
		}

		List<MyNode> allNodesClone = new ArrayList<MyNode>();
		allNodesClone.addAll(allNodes);

		for (int i = 0; i < rooted; i++){
			//Pick a random node from allNodes and get it's corresponding MyNode
			int idx = random.nextInt(allNodesClone.size());
			MyNode node = allNodesClone.get(idx);
			MyNode myNode = nodesMap.get(node);

			myNode.addAttribute("id", node.getId() + "");
			//Remove the node from allNodesClone list.
			allNodesClone.remove(idx);
		}

		int relCount = 0;

		for (MyRelationship r : rels){

			MyNode source = null, target = null;
			RelType type = null;

			//For each relationship in rels, create a corresponding relationship in gp.
			//Note: this will add the appropriate nodes as well.

			source = nodesMap.get(r.getSource());
			target = nodesMap.get(r.getTarget());
			type = GPUtil.translateRelType(r.getIdentifier());

			MyRelationship rel = new MyRelationship(source, target, type, relCount);
			relCount++;

			if (!source.equals(target)){
				gp.addRelationship(rel);
				relsMap.put(r, rel);
			}
		}

		//Set the attribute requirements
		return gp;
	}

	private void assignResultSchema(){

		if (gpHolder != null){
			List<MyNode> temp = new ArrayList<MyNode>();
			temp.addAll(gpHolder.getGp().getNodes());

			List<MyNode> resultSchema = new ArrayList<MyNode>();

			int schemaSize = 0;

			while (schemaSize == 0){
				schemaSize = random.nextInt(temp.size());
			}

			for (int count = 0; count < schemaSize; count++){
				MyNode resNode = temp.remove(random.nextInt(temp.size()));
				resultSchema.add(resNode);
			}

			gpHolder.setResultSchema(resultSchema);
		}
	}


}
