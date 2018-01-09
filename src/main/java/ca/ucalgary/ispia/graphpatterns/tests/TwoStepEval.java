package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPCheckerFC;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public class TwoStepEval implements Killable{

	private GPCheckerFC currentChecker = null;
	private boolean kill = false;
	public List<Map<MyNode, Node>> unfiltered;
	
	public List<Map<MyNode, Node>> check(GraphDatabaseService graphDb, GPHolder dbQuery, GPHolder policy){
		
		Map<String, MyNode> qActMap = dbQuery.getActMap();
		Map<String, MyNode> pActMap = policy.getActMap();
		
		Map<MyNode, MyNode> seedsMap = new HashMap<MyNode, MyNode>();
		for (String key : qActMap.keySet()){
			seedsMap.put(qActMap.get(key), pActMap.get(key));
		}
		
		GPCheckerFC queryFC = new GPCheckerFC(graphDb, dbQuery);
		currentChecker = queryFC;
		
		queryFC.check();
		unfiltered = queryFC.queryResults;
		
		if(kill){
			return null;
		}
		
		//System.out.println("Unfiltered Size: " + queryFC.queryResults.size());
		
		List<Map<MyNode, Node>> results = queryFC.queryResults;
		
		List<Map<MyNode, Node>> filtered = new ArrayList<Map<MyNode, Node>>();
		
		for (Map<MyNode, Node> res : results){
			
			Map<MyNode, Integer> extraInfo = new HashMap<MyNode, Integer>();
			
			for (MyNode src : seedsMap.keySet()){
				try (Transaction tx = graphDb.beginTx()){		
					int idVal = (int) res.get(src).getProperty("id");
					extraInfo.put(seedsMap.get(src), idVal);
					tx.success();
				}
			}
			
			GPCheckerFC policyFC = new GPCheckerFC(graphDb, policy);
			currentChecker = policyFC;
			policyFC.check(extraInfo);
			
			if (kill){
				return null;
			}
			
			if (!policyFC.queryResults.isEmpty()){
				filtered.add(res);
			}
			
		}
		//System.out.println("Filtered Size: " + filtered.size());
		
		return filtered;
	}
	
	public void kill(){
		kill = true;
		if (currentChecker != null){
			currentChecker.kill();
		}
	}
}
