package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This class represents the overall graph pattern object. It contains the graph itself (gp), the list of
 * mutual exclusions constraints (mexList), and the mapping from actors to MyNodes (actMap)
 * @author szrrizvi
 *
 */
public class GPHolder implements Serializable{
	
	private static final long serialVersionUID = 7004437380740720329L;
	
	private GraphPattern gp;						//The graph. This also contains the vertex and edge attribute requirements
	private List<Pair<MyNode, MyNode>> mexList;		//The list of mutual exclusion constraints
	private Map<String, MyNode> actMap;				//The mapping from actor names to MyNode
	private List<MyNode> resultSchema;				//The list of nodes required in the return statement
	
	/**
	 * Constructor. Sets the fields
	 * @param gp The graph pattern
	 * @param mexList The list of mutual exclusion constraints
	 * @param actMap The actor-node map
	 */
	public GPHolder(GraphPattern gp, List<Pair<MyNode, MyNode>> mexList, Map<String, MyNode> actMap){
		
		//Set the fields
		this.gp = gp;
		this.mexList = mexList;
		this.actMap = actMap;
		resultSchema = new ArrayList<MyNode>();
	}

	/**
	 * Sets the result schema
	 * @param nodes The list of nodes for the result schema
	 */
	public void setResultSchema(List<MyNode> nodes){
		this.resultSchema = nodes;
	}
	
	/**
	 * @return The graph pattern
	 */
	public GraphPattern getGp() {
		return gp;
	}

	/**
	 * @return The list of mutual exclusion constraints
	 */
	public List<Pair<MyNode, MyNode>> getMexList() {
		return mexList;
	}

	/**
	 * @return The actMap
	 */
	public Map<String, MyNode> getActMap() {
		return actMap;
	}
	
	/**
	 * @return The result schema
	 */
	public List<MyNode> getResultSchema(){
		return this.resultSchema;
	}
	
	public void setActMap(Map<String, MyNode> actMap){
		this.actMap = actMap;
	}
	
	/**
	 * Returns the mutual exclusion constraints that contain the given node
	 * @param node The target node
	 * @return the mutual exclusion constraints that contain the given node
	 */
	public List<Pair<MyNode, MyNode>> getMexList(MyNode node) {
		List<Pair<MyNode, MyNode>> result = new ArrayList<Pair<MyNode, MyNode>>();
		//Iterate through the mexList, and see if the current constrain includes the node.
		//If so, add it to the result
		for (Pair<MyNode, MyNode> mex : mexList){
			if (mex.first.equals(node) || mex.second.equals(node)){
				result.add(mex);
			}
		}
		
		return result;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("GPHolder:\n");
		sb.append(gp + "\n");
		sb.append("mexList: " + mexList + "\n");
		sb.append("actMap: " + actMap + "\n");
		sb.append("resultSchema: " + resultSchema + "\n");
		
		return sb.toString();
	}
	
	
	

}
