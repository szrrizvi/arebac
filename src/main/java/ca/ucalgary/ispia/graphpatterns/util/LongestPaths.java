package ca.ucalgary.ispia.graphpatterns.util;

import java.util.ArrayList;
import java.util.List;

import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyDirection;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

/**
 * This class provides utility algorithm for finding the longest shortest distance in the gp.
 * The path is in 1 direction. The algorithm uses the Floyd-Warshall algorithm and has the
 * running time of n^3.
 * @author szrrizvi
 *
 */

public class LongestPaths {

	/**
	 * Translates the given gp to a set of paths. This is a greedy algorithm to find the
	 * longest shortest distance at each step. The paths are generated such that each relationship is 
	 * contained in exactly 1 path. 
	 * @param gp The gp to translate to set of paths.
	 * @return The set of paths representing the gp
	 */
	public static List<List<MyRelationship>> longestPaths(GraphPattern gp){
		
		//Initialize paths array
		List<List<MyRelationship>> paths = new ArrayList<List<MyRelationship>>();
		
		//Obtain all nodes and relationships from gp
		List<MyNode> nodes = gp.getNodes();
		List<MyRelationship> rels = gp.getAllRelationships();
		
		//Repeat until all of the edges are exhausted.
		while (!rels.isEmpty()){
			//Find the longest path using the Floyd-Warshall algorithm
			List<MyRelationship> path = floydWarshall(gp);
			
			//Add it to paths list
			paths.add(path);
			
			
			//Update the graph pattern by removing the relationships that were contained
			//in the path.
			gp = new GraphPattern();
			
			for (MyNode node : nodes){
				gp.addNode(node);
			}
			
			rels = new ArrayList<MyRelationship>();
			
			for (MyRelationship rel : rels){
				if (!path.contains(rel)){
					gp.addRelationship(rel);
					rels.add(rel);
				}
			}
		}
		
		
		return paths;
	}
	
	/**
	 * Find the longest shortest distance between 2 nodes in the given gp
	 * @param gp The target graph pattern
	 * @return The path of the longest shortest distance between 2 nodes in the given gp
	 */
	private static List<MyRelationship> floydWarshall(GraphPattern gp){

		//Initialize the algorithm setup
		List<MyRelationship> rels = gp.getAllRelationships();

		List<MyNode> nodes = gp.getNodes();
		int n = nodes.size();

		//The dist 2D array holds the distance from u to v in gp (this is stored in index [u][v])
		int[][] dist = new int[n][n];
		//next[u][v] = x meaning that u -> x -> ... -> v in GP
		MyNode[][] next = new MyNode[n][n];

		//Initialize dist array with -1, meaning that there is path between any 2 nodes. 
		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++){
				dist[i][j] = -1;
			}
		}

		//Consider the paths of length 1. 
		//This is achieved by evaluating all of the relationships in gp
		for (MyRelationship rel : rels){
			int i = nodes.indexOf(rel.getSource());
			int j = nodes.indexOf(rel.getTarget());

			dist[i][j] = 1;
			next[i][j] = rel.getTarget();
		}


		//Run the main body of the Floyd-Warshall algorithm to find the shortest disances and paths
		//for all pairs of nodes
		for (int k = 0; k < n; k++){
			for (int i = 0; i < n; i++){
				for (int j = 0; j < n; j++){
					if (dist[i][k] != -1 && dist[k][j] != -1){
						if (((dist[i][k] + dist[k][j]) < dist[i][j]) || dist[i][j] == -1){
							dist[i][j] = dist[i][k] + dist[k][j];
							next[i][j] = next[i][k];
						}
					}
				}
			}
		}

		//Go through the dist array to find the longest distance
		int longestPath = -1;

		MyNode startNode = null;
		MyNode endNode = null;
		int startNodeIdx = -1;
		int endNodeIdx = -1;

		for (int i = 0; i < n; i++){
			for (int j = 0; j < n; j++){
				if (dist[i][j] > longestPath){
					longestPath = dist[i][j];
					startNode = nodes.get(i);
					startNodeIdx = i;
					endNode = nodes.get(j);
					endNodeIdx = j;
				}
			}
		}


		List<MyRelationship> path = new ArrayList<MyRelationship>();

		//Reconstruct the path based on the next array
		while (!startNode.equals(endNode)){

			MyNode prev = startNode;

			startNodeIdx = nodes.indexOf(startNode);
			startNode = next[startNodeIdx][endNodeIdx];

			List<MyRelationship> temp = gp.getRelationships(prev, MyDirection.OUTGOING);
			path.add(getRelationship(temp, prev ,startNode));

		}	
		return path;
	}

	/**
	 * Find the relationship (contained in rels) matching the given source and target.
	 * NOTE: Currently we are working with a single relation identifier.
	 * @param rels
	 * @param source
	 * @param target
	 * @return The first relationship from source to target contained within rels, if no such relationship
	 * exists then return null
	 */
	private static MyRelationship getRelationship(List<MyRelationship> rels, MyNode source, MyNode target){

		//Iterate through the relationships in rels, and find the first one where the source and target match
		//the given parameters. 
		for (MyRelationship rel : rels){
			if (rel.getSource().equals(source) && rel.getTarget().equals(target)){
				return rel;
			}
		}

		return null;
	}


}
