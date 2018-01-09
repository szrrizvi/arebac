package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public interface ReturnStruct {

	/**
	 * @return true if all of the variables in the result schema are already assigned, and backtracking
	 * from a successful complete assignment.
	 */
	public boolean isJumpingForJoy();
	
	/**
	 * @return The set of variables to jump back to. 
	 */
	public Set<MyNode> getJumpVariables();
}
