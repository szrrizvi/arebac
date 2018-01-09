package ca.ucalgary.ispia.graphpatterns.util;

import java.util.List;

import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;

/**
 * Represents where a relationship fits within a path.
 * @author szrrizvi
 *
 */
public class PathFit {

	private final List<PathFit> path;	//The corresponding path
	private final MyRelationship rel;			//The target relationship
	private final Location loc;					//The location of the fit (at beginning or at end)
	private final Face face; 					//The facing of the relationship (left or right)
	
	public enum Location {START, END};
	public enum Face {LEFT, RIGHT};
	
	/**
	 * Basin constructor. Initializes the fields.
	 * @param path
	 * @param rel
	 * @param loc
	 */
	public PathFit(List<PathFit> path, MyRelationship rel, Location loc, Face face){
		this.rel = rel;
		this.loc = loc;
		this.path = path;
		this.face = face;
	}

	//Basic getters
	
	public MyRelationship getRel() {
		return rel;
	}

	public Location getLoc() {
		return loc;
	}
	
	public List<PathFit> getPath(){
		return this.path;
	}
	
	public Face getFace(){
		return this.face;
	}
}