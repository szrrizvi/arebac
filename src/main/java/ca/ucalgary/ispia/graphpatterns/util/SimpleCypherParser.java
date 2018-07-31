package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;

/**
 * This class provides the methods used for parsing a Cypher query and creating an equivalent Graph Pattern object.
 * Note, this is a very specific parser, it only works with test cases provided for the CODASPY2018 publication.
 */
public class SimpleCypherParser {

	private String fileName;
	private Map<String, RelType> relTypesMap;
	
	public SimpleCypherParser(String fileName){
		this.fileName = fileName;
		
		this.relTypesMap = new HashMap<String, RelType>();
		relTypesMap.put("RelA", RelType.RelA);
		relTypesMap.put("RelB", RelType.RelB);
		relTypesMap.put("RelC", RelType.RelC);
		relTypesMap.put("RelD", RelType.RelD);
		relTypesMap.put("RelE", RelType.RelE);
		relTypesMap.put("RelF", RelType.RelF);
		relTypesMap.put("RelG", RelType.RelG);
	}
	
	public List<GPHolder> parse(){
		
		//Open the text file to read from.
		Scanner scan = null;
		try {
			scan = new Scanner(new FileInputStream(fileName));
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
		
		List<GPHolder> list = new ArrayList<GPHolder>();
		//Convert each "combined" Cypher query into a Graph Pattern Holder object. 
		//NOTE: We are ignoring the "dbQuery" and "policy" queries.
		while(scan.hasNext()){
			String line = scan.nextLine();
			
			if (line.equals("combined:")){
				boolean done = false;
				StringBuilder sb = new StringBuilder();
				while (!done){
					String statement = scan.nextLine();
					if (statement.equals("-----")){
						done = true;
					} else {
						sb.append(statement+"\n");
					}
				}
				GPHolder gph = makeGPH(sb.toString());
				if (gph == null){
					System.out.println("GPH Input file not formatted properly");
					return null;
				}
				list.add(gph);
			}
		}
		
		return list;
	}
	
	private GPHolder makeGPH(String query){
		//Split the query up by lines
		String[] lines = query.split("\n");
		
		GraphPattern gp = new GraphPattern();
		List<Pair<MyNode, MyNode>> mexList = new ArrayList<Pair<MyNode, MyNode>>();
		List<MyNode> resultSchema = new ArrayList<MyNode>();
		//Variables for tracking information (self explanatory)
		int nodeCounter = 0;
		int relCounter = 0;
		Map<String,MyNode> nodesMap = new HashMap<String, MyNode>();
		Map<String,MyRelationship> relsMap = new HashMap<String, MyRelationship>();
		
		for (int idx = 0; idx < lines.length; idx++){
			String line = lines[idx];	//Get the next line
			//Each query line starts with one of 3 possible values, "MATCH", "WHERE", and "RETURN"
			//Assumption:The match statements come first, followed by a Where line (with multiple clauses), and then finally a Return statement.
			if (line.startsWith("MATCH")){					//Match statement
				//Extract the components
				
				//The src node label
				int srcStart = line.indexOf("(")+1;
				int srcEnd = line.indexOf(":");
				String src = line.substring(srcStart, srcEnd).trim();
				
				//The relationship label
				int relStart = line.indexOf("[")+1;
				int relEnd = line.indexOf(":", srcEnd+1);
				String rel = line.substring(relStart, relEnd).trim();
				
				//The relationship type
				int relTypeStart = relEnd+1;
				int relTypeEnd = line.indexOf("]");
				String relType = line.substring(relTypeStart, relTypeEnd).trim();
				
				//The tgt node label
				int tgtStart = line.lastIndexOf("(")+1;
				int tgtEnd = line.lastIndexOf(":");
				String tgt = line.substring(tgtStart, tgtEnd).trim();		//The tgt node label
				
				//Obtain the corresponding MyNode objects for the src and tgt nodes.
				MyNode srcNode = null;
				MyNode tgtNode = null;
				if (nodesMap.containsKey(src)){
					srcNode = nodesMap.get(src);
				} else {
					srcNode = new MyNode(nodeCounter,"Person");
					nodeCounter++;
					gp.addNode(srcNode);
					nodesMap.put(src, srcNode);
				}
				if (nodesMap.containsKey(tgt)){
					tgtNode = nodesMap.get(tgt);
				} else {
					tgtNode = new MyNode(nodeCounter,"Person");
					nodeCounter++;
					gp.addNode(tgtNode);
					nodesMap.put(tgt, tgtNode);
				}
				
				//Get the RelType object.
				RelType rt = relTypesMap.get(relType);
				
				//Generate the relationship.
				MyRelationship myRel = new MyRelationship(srcNode, tgtNode, rt, relCounter);
				relCounter++;
				relsMap.put(rel, myRel);
				gp.addRelationship(myRel);
			}
			else if (line.startsWith("RETURN")){
				//The return statements only have a single node
				String node = line.substring(16,18);
				resultSchema.add(nodesMap.get(node));
			} 
			else if (line.startsWith("WHERE")){
				line = line.substring(6);
				//The where lines have multiple clauses, a clause be either be a mutual exclusion constraint, or an attribute requirement.
				//An attribute requirement could be for a node or a relationship
								
				String[] clauses = line.split(" AND ");	//clauses are separated by " AND "
				for (int x = 0; x < clauses.length; x++){
					String clause = clauses[x].trim();
					if (clause.contains("<>")){			//Mutual Exclusion constraints
						
						//Get the nodes
						String nodeA = clause.substring(0, 2);
						String nodeB = clause.substring(4,6);
						MyNode nA = nodesMap.get(nodeA);
						MyNode nB = nodesMap.get(nodeB);
						//Add the mutual exclusion constraint
						Pair<MyNode, MyNode> mex = new Pair<MyNode, MyNode>(nA, nB);
						mexList.add(mex);
					}
					else if (clause.startsWith("rel")){	//Attribute requirement for relationship
						//Get the relationship
						String rel = clause.substring(0,4);
						MyRelationship myRel = relsMap.get(rel);
						//Get the attribute name and value
						String[] req = clause.substring(5).split("=");
						String attrName = req[0].substring(1, req[0].length()-1);
						String val = req[1];
						
						//Add the attribute requirement
						myRel.addAttribute(attrName, val);
						
					}
					else {								//Attribute requirement for node
						//Get the node
						String node = clause.substring(0,2);
						MyNode myNode = nodesMap.get(node);
						//Get the attribute name and value
						String[] req = clause.substring(3).split("=");
						String attrName = req[0].substring(1, req[0].length()-1);
						String val = req[1];
						if (req[1].startsWith("\"")){
							val = req[1].substring(1, req[1].length()-1);
						}
						//Add the attribute requirement
						myNode.addAttribute(attrName, val);	
						
					}
						
				}
			}
			else {
				System.out.println("Unanticipated line start");
				return null;
			}
		}
		//Create and return the GPHolder object
		GPHolder gph = new GPHolder(gp, mexList, new HashMap<String,MyNode>());
		gph.setResultSchema(resultSchema);
		return gph;
	}
	
}
