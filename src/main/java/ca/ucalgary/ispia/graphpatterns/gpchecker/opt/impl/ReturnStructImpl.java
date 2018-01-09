package ca.ucalgary.ispia.graphpatterns.gpchecker.opt.impl;

import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.gpchecker.opt.ReturnStruct;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public class ReturnStructImpl implements ReturnStruct{

	private boolean liveEnd;
	private Set<MyNode> jumpVariables;
	
	public ReturnStructImpl(boolean liveEnd, Set<MyNode> jumpVariables){
		this.liveEnd = liveEnd;
		this.jumpVariables = jumpVariables;
	}
	
	public boolean isJumpingForJoy(){
		return this.liveEnd;
	}
	
	
	public Set<MyNode> getJumpVariables(){
		return this.jumpVariables;
	}

}
