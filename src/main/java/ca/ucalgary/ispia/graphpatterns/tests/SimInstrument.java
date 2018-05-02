package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public class SimInstrument<N> {
	private Map<Integer, Integer> assignments;
	private Map<Integer, Integer> candidates;
	private Map<Integer, Integer> confIn;
	private Map<Integer, Integer> confOut;
	
	public SimInstrument(){
		assignments = new HashMap<Integer, Integer>();
		candidates = new HashMap<Integer, Integer>();
		confIn = new HashMap<Integer, Integer>();
		confOut = new HashMap<Integer, Integer>();
	}
	
	public SimInstrument (SimInstrument<N> src){
		this();
		update(src);
	}
	
	public void update(SimInstrument<N> src){
		updateMap(assignments, src.getAssignments());
		updateMap(candidates, src.getCandidates());
		updateMap(confIn, src.getConfIn());
		updateMap(confOut, src.getConfOut());
	}
	
	public Map<Integer, Integer> getAssignments() {
		return assignments;
	}

	public Map<Integer, Integer> getCandidates() {
		return candidates;
	}

	public Map<Integer, Integer> getConfIn() {
		return confIn;
	}

	public Map<Integer, Integer> getConfOut() {
		return confOut;
	}

	public void updateAssignments(Map<MyNode, N> assn) {
		int size = assn.keySet().size();
		if (assignments.containsKey(size)){
			int val = assignments.get(size)+1;
			assignments.put(size, val);
		} else {
			assignments.put(size, 1);
		}
	}

	public void updateCandidates(Map<MyNode, Set<N>> cand) {
		for (MyNode key : cand.keySet()){
			int size = cand.get(key).size();
			if (candidates.containsKey(size)){
				int val = candidates.get(size)+1;
				candidates.put(size, val);
			} else {
				candidates.put(size, 1);
			}
		}
	}

	public void updateConfIn(Map<MyNode, Set<MyNode>> cI) {
		for (MyNode key : cI.keySet()){
			int size = cI.get(key).size();
			if (confIn.containsKey(size)){
				int val = confIn.get(size)+1;
				confIn.put(size, val);
			} else {
				confIn.put(size, 1);
			}
		}
	}

	public void updateConfOut(Set<MyNode> cO) {
		int size = cO.size();
		if (confOut.containsKey(size)){
			int val = confOut.get(size)+1;
			confOut.put(size, val);
		} else {
			confOut.put(size, 1);
		}
	}

	private void updateMap(Map<Integer, Integer> source, Map<Integer, Integer> newInfo){
		for (Integer key : newInfo.keySet()){
			if (source.containsKey(key)){
				int val = source.get(key) + newInfo.get(key);
				source.put(key, val);
			} else {
				source.put(key, newInfo.get(key));
			}
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Assignments\n");
		for (Integer key : assignments.keySet()){
			sb.append(key + ", "+ assignments.get(key) + "\n");
		}
		
		sb.append("\nCandidates\n");
		for (Integer key : candidates.keySet()){
			sb.append(key + ", "+ candidates.get(key) + "\n");
		}
		
		sb.append("\nConfIn\n");
		for (Integer key : confIn.keySet()){
			sb.append(key + ", "+ confIn.get(key) + "\n");
		}
		
		sb.append("\nConfOut\n");
		for (Integer key : confOut.keySet()){
			sb.append(key + ", "+ confOut.get(key) + "\n");
		}
		return sb.toString();
	}

}
