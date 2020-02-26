package ca.ucalgary.ispia.graphpatterns.util;

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
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

public class GraphUtil {
	
	public static void degDistribution(GraphDatabaseService graphDb){
		try (Transaction tx = graphDb.beginTx()){
			Iterator<Node> nodes = graphDb.getAllNodes().iterator();
			
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			
			while(nodes.hasNext()){
				Node n = nodes.next();
				
				int degIn = n.getDegree(Direction.INCOMING);
				int degOut = n.getDegree(Direction.OUTGOING);
				
				if (map.containsKey(degIn)){
					int val = map.get(degIn) +1;
					map.put(degIn, val);
				} else {
					map.put(degIn, 1);
				}
				
				if (map.containsKey(degOut)){
					int val = map.get(degOut) +1;
					map.put(degOut, val);
				} else {
					map.put(degOut, 1);
				}
			}
			
			for(Integer key : map.keySet()){
				System.out.println(key + " " + map.get(key));
			}
			
			tx.success();
		}
	}
	
	public static void checkConnected(GraphDatabaseService graphDb){
		try (Transaction tx = graphDb.beginTx()){
			
			List<Relationship> allRel = new ArrayList<Relationship>();
			Iterator<Relationship> ite = graphDb.getAllRelationships().iterator();
			
			while (ite.hasNext()){
				allRel.add(ite.next());
			}
			
			System.out.println(allRel.size());
			
			List<Node> nodesPool = new ArrayList<Node>();
			Set<Node> tree = new HashSet<Node>();
			
			Node node = graphDb.getNodeById(0);
			nodesPool.add(node);
			tree.add(node);
			
			Set<Relationship> relsEnc = new HashSet<Relationship>();
			
			while (!nodesPool.isEmpty()){
				node = nodesPool.remove(0);
				Iterator<Relationship> rels = node.getRelationships().iterator();
				
				while (rels.hasNext()){
			
					Relationship rel = rels.next();
					
					relsEnc.add(rel);
					
					Node neighbour = rel.getOtherNode(node);
					
					if (!tree.contains(neighbour)){
						tree.add(neighbour);
						nodesPool.add(neighbour);
					}
				}
				
			}
			

			System.out.println(tree.size() + ", " + relsEnc.size());
			
			tx.success();
		}
	}

}
