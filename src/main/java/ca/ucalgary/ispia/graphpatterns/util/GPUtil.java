package ca.ucalgary.ispia.graphpatterns.util;

import java.util.List;

import org.neo4j.graphdb.RelationshipType;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;
import ca.ucalgary.ispia.graphpatterns.tests.TripleGPHolder;

/**
 * This class provides utility methods for GraphPatterns
 * @author szrrizvi
 *
 */

public class GPUtil {
	
	/**
	 * Prints the stats (# nodes, # relationships, max in degree, max out degree, average degree) 
	 * of the given GraphPattern object.
	 * @param gp The target GraphPattern.
	 */
	
	public static void PrintStats(List<TripleGPHolder> list){
		//Store the number of nodes and relationships per graph pattern
		int numNodesQ = 0, numRelsQ = 0;
		int numNodesP = 0, numRelsP = 0;
		int numNodesC = 0, numRelsC = 0;
		
		for (TripleGPHolder triple : list){
			GPHolder query = triple.dbQeury;
			numNodesQ += query.getGp().getNodes().size();
			numRelsQ += query.getGp().getAllRelationships().size();
			
			System.out.print("QNodes: " + query.getGp().getNodes().size() + ", QRels: " + query.getGp().getAllRelationships().size() + ", ");
			
			GPHolder pol = triple.policy;
			numNodesP += pol.getGp().getNodes().size();
			numRelsP += pol.getGp().getAllRelationships().size();			
			
			System.out.print("PNodes: " + pol.getGp().getNodes().size() + ", PRels: " + pol.getGp().getAllRelationships().size() + ", ");
			
			
			GPHolder comb = triple.combined;
			numNodesC += comb.getGp().getNodes().size();
			numRelsC += comb.getGp().getAllRelationships().size();
			
			System.out.println("CNodes: " + comb.getGp().getNodes().size() + ", CRels: " + comb.getGp().getAllRelationships().size());
			
		}
		
		double avgNQ = (double)(numNodesQ/list.size());
		double avgRQ = (double)(numRelsQ/list.size());
		double avgNP = (double)(numNodesP/list.size());
		double avgRP = (double)(numRelsP/list.size());
		double avgNC = (double)(numNodesC/list.size());
		double avgRC = (double)(numRelsC/list.size());
		
		
		//System.out.println(avgNQ + "," + avgRQ + "," + avgNP + "," + avgRP + "," +avgNC + "," + avgRC);
		
	}
		
	public static RelType translateRelType(RelationshipType relationshipType){
		if (relationshipType.toString().equals("RelA")){
			return RelType.RelA;
		} else if (relationshipType.toString().equals("RelB")){
			return RelType.RelB;
		} else if (relationshipType.toString().equals("RelC")){
			return RelType.RelC;
		} else if (relationshipType.toString().equals("RelD")){
			return RelType.RelD;
		} else if (relationshipType.toString().equals("RelE")){
			return RelType.RelE;
		} else if (relationshipType.toString().equals("RelF")){
			return RelType.RelF;
		}  else if (relationshipType.toString().equals("RelG")){
			return RelType.RelG;
		} else {
			System.out.println("Relationship type didnt match: " + relationshipType);
			return null;
		}
	}
}
