package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.Serializable;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;

public class TripleGPHolder implements Serializable{
	
	private static final long serialVersionUID = -7878552334734907008L;
	
	public final GPHolder dbQeury, policy, combined;
	
	
	public TripleGPHolder(GPHolder dbq, GPHolder pol, GPHolder comb){
		this.dbQeury = dbq;
		this.policy = pol;
		this.combined = comb;
	}
}
