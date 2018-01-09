package ca.ucalgary.ispia.graphpatterns.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.util.PathFit.Face;
import ca.ucalgary.ispia.graphpatterns.util.PathFit.Location;


public class RandomPaths {
	public static List<List<PathFit>> generateRandomPaths(List<MyRelationship> rels, Random random){

		//Initialize the paths and get all of the relationships
		Paths paths = new Paths();

		//Keep looping until add relationships are exhausted
		while (!rels.isEmpty()){
			//Remove a random relationship from the list
			MyRelationship rel = rels.get(random.nextInt(rels.size()));
			rels.remove(rel);

			//Find all possible locations where this relationship can fit
			//in the existing paths
			List<PathFit> fits = paths.possibleFits(rel);

			//Randomly pick one of those paths (+1 for starting a new path)
			int fitIdx = random.nextInt(fits.size()+1);

			//If there are no possible fits into existing paths, or randomly chosen to start new path
			if (fits.size() == 0 || fitIdx == fits.size()){
				//Create the new path, populate it, and add it to the paths objects
				List<PathFit> path = new ArrayList<PathFit>();

				PathFit pf = new PathFit(path, rel, Location.START, Face.RIGHT);

				path.add(pf);
				paths.addPath(path);
			} else{
				//Otherwise, add the relationship to the selected path in the correct location
				PathFit fit = fits.get(fitIdx);
				List<PathFit> path = fit.getPath();

				if (fit.getLoc() == Location.START){
					path.add(0, fit);
				} else {
					path.add(fit);
				}
			}
		}

		return paths.getPaths();
	}

	public static List<List<PathFit>> generateSmartRandomPaths(GraphPattern gp, Random random){

		//Initialize the paths and get all of the relationships
		Paths paths = new Paths();
		List<MyRelationship> rels = gp.getAllRelationships();

		boolean first = true;
		List<MyNode> seenBefore = new ArrayList<MyNode>();


		//Keep looping until add relationships are exhausted
		while (!rels.isEmpty()){

			//Remove a random relationship from the list
			MyRelationship rel = null;
			if (!first){
				List<MyRelationship> validSet = getValidSet(rels, seenBefore);		
				rel = validSet.get(random.nextInt(validSet.size()));
				rels.remove(rel);
			} else {
				rel = rels.get(random.nextInt(rels.size()));
				rels.remove(rel);
			}
			
			System.out.println(rel);
			for (MyNode n : seenBefore){
				System.out.println(n);
			}

			//Find all possible locations where this relationship can fit
			//in the existing paths
			List<PathFit> fits = paths.possibleFits(rel);
			
			fits = pruneFits(fits, seenBefore);

			//Randomly pick one of those paths (+1 for starting a new path)
			int fitIdx = random.nextInt(fits.size()+1);

			//If there are no possible fits into existing paths, or randomly chosen to start new path
			if (fits.size() == 0 || fitIdx == fits.size()){
				//Create the new path, populate it, and add it to the paths objects
				List<PathFit> path = new ArrayList<PathFit>();

				PathFit pf = null;

				if (first || seenBefore.contains(rel.getSource())){
					pf = new PathFit(path, rel, Location.START, Face.RIGHT);
				} else {
					pf = new PathFit(path, rel, Location.START, Face.LEFT);
				}

				path.add(pf);
				paths.addPath(path);
			} else{
				//Otherwise, add the relationship to the selected path in the correct location
				PathFit fit = fits.get(fitIdx);
				List<PathFit> path = fit.getPath();

				if (fit.getLoc() == Location.START){
					path.add(0, fit);
				} else {
					path.add(fit);
				}
			}

			if (!seenBefore.contains(rel.getSource())){
				seenBefore.add(rel.getSource());
			}
			if (!seenBefore.contains(rel.getTarget())){
				seenBefore.add(rel.getTarget());
			}

			first = false;
		}

		List<MyNode> starters = new ArrayList<MyNode>();
		
		for (List<PathFit> path : paths.getPaths()){
			PathFit fit = path.get(0);
			MyNode node = null;
			if (fit.getFace() == Face.RIGHT){
				node = fit.getRel().getSource();
			} else {
				node = fit.getRel().getTarget();
			}
			if (!starters.contains(node)){
				starters.add(node);
			}
		}
		
		for (MyNode n : gp.getNodes()){
			if (!starters.contains(n)){
				n.setAttributes(new HashMap<String, String>());
			}
		}
		
		
		return paths.getPaths();
	}

	private static List<MyRelationship> getValidSet(List<MyRelationship> rels, List<MyNode> seenBefore){
		List<MyRelationship> validSet = new ArrayList<MyRelationship>();

		for (MyRelationship r : rels){

			MyNode source = r.getSource();
			MyNode target = r.getTarget();

			if (seenBefore.contains(source) || seenBefore.contains(target)){
				validSet.add(r);
			}
		}

		return validSet;
	}
	
	private static List<PathFit> pruneFits(List<PathFit> fits, List<MyNode> seenBefore){
		
		List<PathFit> newFits = new ArrayList<PathFit>();
		
		for (PathFit fit : fits){
			if (fit.getLoc() == Location.START){
				if (fit.getFace() == Face.RIGHT){
					if (seenBefore.contains(fit.getRel().getSource())
							|| fit.getRel().getSource().hasAttributes()){
						newFits.add(fit);
					}
				} else {
					if (seenBefore.contains(fit.getRel().getTarget())
							|| fit.getRel().getTarget().hasAttributes()){
						newFits.add(fit);
					}
				}
			}
		}
		
		return newFits;
	}
}
