package ca.ucalgary.ispia.graphpatterns.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyDirection;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.util.PathFit.Face;

/**
 * The class contains various methods for translating graph patterns into Cypher queries.
 * @author szrrizvi
 */

public class Translator {

	public static String translateToCypher(GPHolder gph){
		Set<MyNode> seenNodes = new HashSet<MyNode>();
		List<MyRelationship> rels = gph.getGp().getAllRelationships();
		List<MyNode> nodes = gph.getGp().getNodes();
		
		StringBuilder sb = new StringBuilder();
		
		for (MyRelationship rel : rels){
			sb.append("MATCH (a" + rel.getSource().getId() + ":PERSON) -[b" + rel.getId() +" : " + rel.getIdentifier() + "]->(a"+rel.getTarget().getId()+":PERSON) \n");
			seenNodes.add(rel.getSource());
			seenNodes.add(rel.getTarget());
		}
		
		for (MyNode node : nodes){
			if (!seenNodes.contains(node)){
				sb.append("MATCH (a" + node.getId() + ":PERSON) \n");
			}
		}
		
		boolean first = true;
		
		for (MyNode node : nodes){
			if (node.hasAttributes()){
				if (first){
					sb.append("WHERE ");
					first = false;
				} else {
					sb.append("AND ");
				}
				
				boolean firstAttr = true;
				
				for (String key : node.getAttributes().keySet()){
					if (firstAttr){
						firstAttr = false;
					} else {
						sb.append("AND ");
					}
					sb.append("a"+node.getId()+ ".`" + key + "`=");
					
					if (AttributeTypes.isIntType(key) || key.equals("id")){
						sb.append(node.getAttribute(key) + " ");
					} else {
						sb.append("\"" + node.getAttribute(key) + "\" ");
					}

				}
			}
		}
		
		
		for (MyRelationship rel : rels){
			if (rel.hasAttributes()){
				if (first){
					sb.append("WHERE ");
					first = false;
				} else {
					sb.append("AND ");
				}
				
				boolean firstAttr = true;
				
				for (String key : rel.getAttributes().keySet()){
					if (firstAttr){
						firstAttr = false;
					} else {
						sb.append("AND ");
					}
					sb.append("b"+rel.getId()+ ".`" + key + "`=");
					
					if (AttributeTypes.isIntType(key) || key.equals("id")){
						sb.append(rel.getAttribute(key) + " ");
					} else {
						sb.append("\"" + rel.getAttribute(key) + "\" ");
					}

				}
			}
		}
		
		
		for (Pair<MyNode, MyNode> mex : gph.getMexList()){
			if (first){
				sb.append("WHERE ");
				first = false;
			} else {
				sb.append("AND ");
			}
			
			sb.append("a"+mex.first.getId() + "<>a" + mex.second.getId() + " ");
		}
		
		boolean retFirst = true;
		for (MyNode node : gph.getResultSchema()){
			if (retFirst){
				sb.append("\nRETURN distinct ");
				retFirst = false;
			} else {
				sb.append(",");
			}
			sb.append("a"+node.getId());
		}
		
		return sb.toString();
	}
	
	/**
	 * A naive method for translating graph patterns into Cypher queries.
	 * This method treats each edge separately, and produces a "MATCH" statement
	 * for each edge in the graph pattern. 
	 * @param gp The target graph pattern.
	 * @return The naive Cypher query translation of the given graph pattern
	 */
	public static String translateNaive(GraphPattern gp){
		StringBuilder str = new StringBuilder("PROFILE \n");

		boolean first = true;
		for (MyNode n : gp.getNodes()){
			if (n.hasAttributes()){
				if (first){
					str.append("MATCH ");
					first = false;
				} else {
					str.append(", ");
				}

				str.append("("+ n.getId() + " {");

				Map<String, String> attrs = n.getAttributes();
				for (String key : attrs.keySet()){
					str.append(key + ":" +  attrs.get(key) + " ");
				}
				str.append("}) ");
			}
		}
		str.append("\n");
		
		//Generate a MATCH statement for each edge in the graph pattern.
		//Obtain the nodes from the graph pattern and iterate through the
		//edges. 

		for (MyNode node : gp.srcKeySet()){

			List<MyRelationship> rels = gp.getRelationships(node, MyDirection.OUTGOING);

			for (MyRelationship rel : rels){
				str.append("MATCH (" + node.getId() + ") -[]-> (" + rel.getTarget().getId() +")\n");
			}
		}

		//Add the RETURN statement at the end.
		//Return the id attribute for each node in the graph pattern.
		str.append("RETURN ");
		int count = 0;
		for (MyNode node : gp.getNodes()){
			if (count > 0){
				str.append(", ");
			}
			str.append(node.getId()+".id");
			count++;
		}
		str.append("\nLIMIT 1");

		return str.toString();
	}

	/**
	 * This method translates the graph pattern with the help of paths. 
	 * This method assumes that the gives paths are correct and cover all of the relationships.
	 * A MATCH statement is generated for each path.
	 * PRECONDITION: Each relationship in the graph pattern is contained in at least one path.
	 * @param gp The target graph pattern
	 * @param paths The specified paths
	 * @return The Cypher query translation of the given graph pattern based on the given paths.
	 */
	public static String translatePaths(GraphPattern gp, List<List<PathFit>> paths){
		StringBuilder str = new StringBuilder("PROFILE \n");

		boolean first = true;
		for (MyNode n : gp.getNodes()){
			if (n.hasAttributes()){
				if (first){
					str.append("MATCH ");
					first = false;
				} else {
					str.append(", ");
				}

				str.append("("+ n.getId() + " {");

				Map<String, String> attrs = n.getAttributes();
				for (String key : attrs.keySet()){
					str.append(key + ":" +  attrs.get(key) + " ");
				}
				str.append("}) ");
			}
		}
		str.append("\n");


		List<MyNode> seenNodes = new ArrayList<MyNode>();

		//Iterate through the paths and create a MATCH statement for each path
		//and an appropriate WHERE clause.
		for (List<PathFit> path : paths){
			str.append("MATCH (");

			MyNode node = null;
			if (path.get(0).getFace() == Face.RIGHT){
				node = path.get(0).getRel().getSource();

			} else {
				node = path.get(0).getRel().getTarget();
			}

			str.append(node.getId());
			str.append(":PERSON)");

			List<MyNode> newNodes = new ArrayList<MyNode>();
			
			if (!seenNodes.contains(node)){
				newNodes.add(node);
			}

			for (PathFit fit : path){

				if (fit.getFace() == Face.RIGHT){
					str.append("-[:FRIEND]-> (");
					node = fit.getRel().getTarget();
				} else {
					str.append("<-[:FRIEND]- (");
					node = fit.getRel().getSource();
				}

				str.append(node.getId());

				if (!seenNodes.contains(node)){
					newNodes.add(node);
				}

				//Add the node attributes for the target node

				str.append(":PERSON) ");
			}
			str.append("\n");

			/*if (!newNodes.isEmpty()){
				str.append("WHERE ");
				first = true;
				for (MyNode nodeA : seenNodes){
					for (MyNode nodeB : newNodes){
						if (first){
							first = false;
						} else {
							str.append(" AND ");
						}
						str.append(nodeA.getId() + "<>" + nodeB.getId());
					}
				}

				for (int i = 0; i < newNodes.size(); i++){
					for (int j = i+1; j < newNodes.size(); j++){
						if (first){
							first = false;
						} else {
							str.append(" AND ");
						}
						str.append(newNodes.get(i).getId() + "<>" + newNodes.get(j).getId());
					}
				}
				str.append("\n");

				seenNodes.addAll(newNodes);
			}*/
		}

		//Add the not equals clauses
		/*first = true;
		str.append("WHERE ");
		List<MyNode> nodes = gp.getNodes();
		for (int i = 0; i < nodes.size(); i++){
			MyNode nodeA = nodes.get(i);
			for (int j = i+1; j < nodes.size(); j++){
				MyNode nodeB = nodes.get(j);
				if (first){
					first = false;
				} else {
					str.append(" AND ");
				}

				str.append(nodeA.getId() + "<>" + nodeB.getId());
			}
		}*/


		//Add the RETURN statement at the end.
		//Return the id attribute for each node in the graph pattern.
		str.append("RETURN ");
		first = true;
		for (MyNode node : gp.getNodes()){
			if (first){
				first = false;
			} else {
				str.append(", ");
			}
			str.append(node.getId()+".id");
		}
		str.append("\nLIMIT 1;");

		return str.toString();
	}
}
