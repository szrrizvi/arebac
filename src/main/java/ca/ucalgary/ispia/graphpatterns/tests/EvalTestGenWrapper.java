package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.graph.EvalTestGenerator;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.HasAttributes;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This is the wrapper class for the evaluation process.
 * @author szrrizvi
 *
 */
public class EvalTestGenWrapper {
	private GraphDatabaseService graphDb;	//The graph database interface
	private Random random;					//The random number generator
	private int nodesSeedSize;				//The number of nodes to use as the seed for the second GPH

	private int endSizeA, rootedA, numMexA, numVAttrsA, numEAttrsA;
	private float pA;
	private double completeA;

	private int endSizeB, rootedB, numMexB, numVAttrsB, numEAttrsB;
	private float pB;
	private double completeB;

	private boolean paramsASet, paramsBSet;

	/**
	 * Initializes the fields
	 * @param graphDb The graph database interface
	 * @param random The pseudorandom number generator
	 */
	public EvalTestGenWrapper(GraphDatabaseService graphDb, Random random, int nodesSeedSize){
		this.graphDb = graphDb;
		this.random = random;
		this.nodesSeedSize = nodesSeedSize;


		this.paramsASet = false;
		this.paramsBSet = false;
	}

	public void setParamsA(int endSize, double complete, int rooted, float p, int numMex, int numVAttrs, int numEAttrs){
		this.endSizeA = endSize;
		this.completeA = complete;
		this.rootedA = rooted;
		this.pA = p;
		this.numMexA = numMex;
		this.numVAttrsA = numVAttrs;
		this.numEAttrsA = numEAttrs;

		this.paramsASet = true;
	}

	public void setParamsB(int endSize, double complete, int rooted, float p, int numMex, int numVAttrs, int numEAttrs){
		this.endSizeB = endSize;
		this.completeB = complete;
		this.rootedB = rooted;
		this.pB = p;
		this.numMexB = numMex;
		this.numVAttrsB = numVAttrs;
		this.numEAttrsB = numEAttrs;

		this.paramsBSet = true;
	}



	/**
	 * Generates the test graph pattern holders objects. 
	 * @return The graph pattern holder objects for the tests
	 */
	public TripleGPHolder generateTests(){

		if (!(paramsASet && paramsBSet)){
			return null;
		}

		EvalTestGenerator etgA = null;
		EvalTestGenerator etgB = null;

		boolean gpADone =false;
		boolean gpBDone = false;

		//Generate the first graph pattern holder. This is the database query.
		GPHolder gpA = null;
		while (!gpADone){
			//Keep looping until we get a non-null result.
			etgA = new EvalTestGenerator(graphDb, random, endSizeA, completeA, rootedA, pA, numMexA, numVAttrsA, numEAttrsA, "A");
			gpA = etgA.createDBBasedGP();
			if (gpA != null){
				gpADone = true;
			}
		}

		//Extract the nodes to use as the seed for the policy query.
		//Additionally, these nodes will also have an actor mapping

		Map<Node, MyNode> nodesMap = etgA.extractNodes(nodesSeedSize);

		List<MyNode> resultSchema = new ArrayList<MyNode>();
		List<Node> nodes = new ArrayList<Node>();
		Map<String, MyNode> actMap = new HashMap<String, MyNode>();
		int counter = 1;

		for (Node node : nodesMap.keySet()){
			nodes.add(node);
			resultSchema.add(nodesMap.get(node));
			actMap.put("act"+counter, nodesMap.get(node));
			counter++;
		}
		gpA.setResultSchema(resultSchema);
		gpA.setActMap(actMap);

		//Generate the second graph pattern holder. This is the policy.
		GPHolder gpB = null;
		while(!gpBDone){
			//Keep looping until we get a non-null result.
			etgB = new EvalTestGenerator(graphDb, random, endSizeB, completeB, rootedB, pB, numMexB, numVAttrsB, numEAttrsB, "B");
			gpB = etgB.createDBBasedGP(nodes);
			if (gpB != null){
				gpBDone = true;
			}
		}


		//Set the actMap for gpB
		Map<String, MyNode> actMapB = new HashMap<String, MyNode>();
		for (Node node : nodesMap.keySet()){
			MyNode first = etgA.findMyNode(node);
			MyNode second = etgB.findMyNode(node);

			//Find the key name mapped to first
			String keyName = null;
			for (String key : actMap.keySet()){
				if (actMap.get(key).equals(first)){
					keyName = key;
				}
			}

			if (keyName == null){
				System.out.println("Couldn't map");
			} else {
				actMapB.put(keyName, second);
			}
		}
		gpB.setActMap(actMapB);

		GPHolder gpC = combineGPs(etgA, etgB, nodes);

		/*System.out.println(gpA.getGp());
		System.out.println("-----");
		System.out.println(gpB.getGp());
		System.out.println("-----");
		System.out.println(gpC.getGp());
		System.out.println("XXXXX\n\n");
		 */


		return new TripleGPHolder(gpA, gpB, gpC);
	}	

	/**
	 * Combines two GPHolders into a single GPHolder.
	 * @param etgA The source for the first GPHolder.
	 * @param etgB The source for the second GPHolder.
	 * @param seeds The seeds used for overlapping the second GP with the first.
	 * @return The combined GPHolder
	 */
	private GPHolder combineGPs(EvalTestGenerator etgA, EvalTestGenerator etgB, List<Node> seeds){

		//Extract the graph patterns
		GraphPattern gpA = etgA.getGPHolder().getGp();
		GraphPattern gpB = etgB.getGPHolder().getGp();

		//The list of nodes for the new graph pattern
		List<MyNode> nodes = new ArrayList<MyNode> ();

		//The list of already visited nodes from the input graph patterns
		List<MyNode> seenNodes = new ArrayList<MyNode> ();

		//The mapping from the original nodes to the new nodes
		Map<MyNode, MyNode> nodesMap = new HashMap<MyNode, MyNode>();

		int z = 0;
		int relCount = 0;
		String nodePrefix = "C";
		String relPrefix = "rel";
		
		List<MyNode> resultSchema = new ArrayList<MyNode>();

		//For each seed, create a single node and map the nodes from the input GPs to the new node
		for (Node n : seeds){
			//Create the new node
			MyNode newNode = new MyNode(nodePrefix + Character.toString((char) (97 + z)), "PERSON");
			z++;
			if (z == 26){
				z = 0;
				nodePrefix = nodePrefix+"A";
			}

			//Add it to the nodes list
			nodes.add(newNode);

			//Find the corresponding original nodes, and map them to the new node
			MyNode temp1 = etgA.findMyNode(n);
			nodesMap.put(temp1, newNode);
			seenNodes.add(temp1);
			MyNode temp2 = etgB.findMyNode(n);
			nodesMap.put(temp2, newNode);
			seenNodes.add(temp2);

			addAttrs(temp1, newNode);
			addAttrs(temp2, newNode);
			resultSchema.add(newNode);
		}

		//Generate the actMap for the combine GPHolder
		Map<String, MyNode> actMap = new HashMap<String, MyNode>();
		Map<String, MyNode> sourceActMap = etgA.getGPHolder().getActMap();
		for (String key : sourceActMap.keySet()){
			MyNode src = sourceActMap.get(key);
			actMap.put(key, nodesMap.get(src));
		}


		//For the rest of the nodes in gpA, create a new corresponding node and mapping
		for (MyNode node : gpA.getNodes()){
			if (!seenNodes.contains(node)){
				//Create the node
				MyNode newNode = new MyNode(nodePrefix + Character.toString((char) (97 + z)), "PERSON");
				z++;
				if (z == 26){
					z = 0;
					nodePrefix = nodePrefix+"A";
				}

				//Update the mapping
				nodesMap.put(node, newNode);
				seenNodes.add(node);

				addAttrs(node, newNode);
			}
		}

		//For the rest of the nodes in gpB, create a new corresponding node and mapping
		for (MyNode node : gpB.getNodes()){
			if (!seenNodes.contains(node)){
				//Create the node
				MyNode newNode = new MyNode(nodePrefix + Character.toString((char) (97 + z)), "PERSON");
				z++;
				if (z == 26){
					z = 0;
					nodePrefix = nodePrefix+"A";
				}

				//Update the mapping
				nodesMap.put(node, newNode);
				seenNodes.add(node);

				addAttrs(node, newNode);
			}
		}

		List<MyRelationship> rels = new ArrayList<MyRelationship>();
		GraphPattern gp = new GraphPattern();

		//Create the relationships based on the mappings

		//For gpA
		for (MyRelationship rel : gpA.getAllRelationships()){
			//Get the corresponding new nodes and relationship type
			MyNode src = nodesMap.get(rel.getSource());
			MyNode tgt = nodesMap.get(rel.getTarget());
			RelType type = GPUtil.translateRelType(rel.getIdentifier());
			//Generate the relationships
			MyRelationship r = new MyRelationship(src, tgt, type, relPrefix + ((char) (97 + relCount)));
			relCount++;
			
			if (relCount == 26){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}
			
			addAttrs(rel, r);
			gp.addRelationship(r);
		}

		//For gpB
		for (MyRelationship rel : gpB.getAllRelationships()){
			//Get the corresponding new nodes
			MyNode src = nodesMap.get(rel.getSource());
			MyNode tgt = nodesMap.get(rel.getTarget());
			RelType type = GPUtil.translateRelType(rel.getIdentifier());
			//Generate the relationships
			MyRelationship r = new MyRelationship(src, tgt, type, relPrefix + ((char) (97 + relCount)));
			relCount++;

			if (relCount == 26){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}
			
			addAttrs(rel, r);
			gp.addRelationship(r);

		}


		//Create the mutual exclusion constraints based on teh mappings
		List<Pair<MyNode, MyNode>> mexList = new ArrayList<Pair<MyNode, MyNode>>();


		//Mutual exclusion constraints for gpA
		for(Pair<MyNode, MyNode> mex : etgA.getGPHolder().getMexList()){
			Pair<MyNode, MyNode> newMex = new Pair<MyNode, MyNode>(nodesMap.get(mex.first), nodesMap.get(mex.second));
			mexList.add(newMex);
		}

		//Mutual exclusion constraints for gpB
		for(Pair<MyNode, MyNode> mex : etgB.getGPHolder().getMexList()){
			Pair<MyNode, MyNode> newMex = new Pair<MyNode, MyNode>(nodesMap.get(mex.first), nodesMap.get(mex.second));
			mexList.add(newMex);
		}

		//Generate and return the GPHolder
		GPHolder gph = new GPHolder(gp, mexList, new HashMap<String, MyNode>());
		gph.setResultSchema(resultSchema);
		gph.setActMap(actMap);
		return gph;
	}

	private void addAttrs(HasAttributes source, HasAttributes target){
		Map<String, String> attrs = source.getAttributes();

		for (String key : attrs.keySet()){
			target.addAttribute(key, source.getAttribute(key));
		}
	}
}
