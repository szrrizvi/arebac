package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;

import org.neo4j.graphdb.RelationshipType;

public enum RelType implements RelationshipType, Serializable{
	RelA(0), RelB(1), RelC(2), RelD(3), RelE(4), RelF(5), RelG(6);
	
	private final int idx;
	
	private RelType(int idx){
		this.idx = idx;
	}
	
	public int getIdx(){
		return this.idx;
	}
}
