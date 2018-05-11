package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;

import org.neo4j.graphdb.RelationshipType;

public enum RelType implements RelationshipType, Serializable{
	RelA(1), RelB(2), RelC(3), RelD(4), RelE(5), RelF(6), RelG(7);
	
	private final int idx;
	
	private RelType(int idx){
		this.idx = idx;
	}
	
	public int getIdx(){
		return this.idx;
	}
}
