package ca.ucalgary.ispia.graphpatterns.util;

import java.util.ArrayList;
import java.util.List;

import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.util.PathFit.Face;
import ca.ucalgary.ispia.graphpatterns.util.PathFit.Location;

/**
 * Represents a collection of paths of relationships.
 * @author szrrizvi
 *
 */
public class Paths {

	// The collection paths.
	private List<List<PathFit>> paths;

	/**
	 * Default constructor.
	 */
	public Paths(){
		//Initialize the paths list
		paths = new ArrayList<List<PathFit>>();
	}

	/**
	 * @return paths
	 */
	public List<List<PathFit>> getPaths(){
		return this.paths;
	}

	/**
	 * Adds a new path to the paths list
	 * @param path The path to add
	 */
	public void addPath(List<PathFit> path){
		this.paths.add(path);
	}

	/**
	 * Find possible fits for the given relationship.
	 * If the relationship doesn't fit anywhere, then it returns an empty list.
	 * @param rel
	 * @return A list of PathFit for the given relationship.
	 */
	public List<PathFit> possibleFits(MyRelationship rel){

		List<PathFit> fits = new ArrayList<PathFit>();

		//Iterate through the paths
		for (List<PathFit> path : paths){

			//Sanity checking. Making sure the path is not empty.
			if (path.size() > 0){

				//Check if rel fits at the beginning of the path
				PathFit fit = path.get(0);
				MyRelationship start = fit.getRel();
				
				//Let start = u -> v
				//Let rel = a -> b

				//If the beginning of the path is right facing
				if (fit.getFace() == Face.RIGHT){
					if ((start.getSource().equals(rel.getTarget()))
							&& !(start.getTarget().equals(rel.getSource()))){
						
						// a -> b/u -> v ...
						//Note: b = u
						
						PathFit pf = new PathFit(path, rel, Location.START, Face.RIGHT);
						fits.add(pf);
					}
					else if ((start.getSource().equals(rel.getSource()))
							&& !(start.getTarget().equals(rel.getTarget()))){
						
						// b <- a/u -> v ...
						//Note: a = u
						
						PathFit pf = new PathFit(path,rel, Location.START, Face.LEFT);
						fits.add(pf);
					}
				} else {
					if ((start.getTarget().equals(rel.getTarget()))
							&& !(start.getSource().equals(rel.getSource()))){
						
						// a -> b/v <- u ...
						// Note: b = v
						
						PathFit pf = new PathFit(path, rel, Location.START, Face.RIGHT);
						fits.add(pf);
					}
					else if ((start.getTarget().equals(rel.getSource()))
							&& !(start.getSource().equals(rel.getTarget()))){
						
						// b <- a/v <- u ...
						// Note: a = v
						PathFit pf = new PathFit(path, rel, Location.START, Face.LEFT);
						fits.add(pf);
					}
				}

				//Check if rel fits at the end of the path
				fit = path.get(path.size()-1);
				MyRelationship end = fit.getRel();
				
				//Let start = u -> v
				//Let rel = a -> b
				
				//If the end of the path is right facing
				if (fit.getFace() == Face.RIGHT){
					if ((end.getTarget().equals(rel.getSource()))
							&& !(end.getSource().equals(rel.getTarget()))){
						
						// ... u -> v/a -> b
						// Note: a = v
						
						PathFit pf = new PathFit(path, rel, Location.END, Face.RIGHT);
						fits.add(pf);
					}
					else if ((end.getTarget().equals(rel.getTarget()))
							&& !(end.getSource().equals(rel.getSource()))){
						
						// ... u -> v/b <- a
						// Note: b = v
						
						PathFit pf = new PathFit(path, rel, Location.END, Face.LEFT);
						fits.add(pf);
					}
				} else {
					if ((end.getSource().equals(rel.getSource()))
							&& !(end.getTarget().equals(rel.getTarget()))){
						
						// v <- u/a -> b
						// Note: a = u
						
						PathFit pf = new PathFit(path, rel, Location.END, Face.RIGHT);
						fits.add(pf);
					}
					else if ((end.getSource().equals(rel.getTarget()))
							&& !(end.getTarget().equals(rel.getSource()))){
						
						// v <- u/b <- a
						// Note: b = u
						
						PathFit pf = new PathFit(path, rel, Location.END, Face.LEFT);
						fits.add(pf);						
					}
						
				}

				//We do not want to add rel anywhere in the middle of a path. 
			}
		}

		return fits;
	}

	/**
	 * To string method.
	 */
	public String toString(){
		StringBuilder str = new StringBuilder();

		for (List<PathFit> path : paths){
			str.append(path.get(0).getRel().getSource() + " ");
			for (PathFit pf : path){
				str.append(pf.getRel().getTarget() + " ");
			}
			str.append("\n");
		}

		return str.toString();
	}

}
