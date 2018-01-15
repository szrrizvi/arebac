package ca.ucalgary.ispia.graphpatterns.gpchecker.opt;

import java.util.Map;
import java.util.Set;

import ca.ucalgary.ispia.graphpatterns.graph.MyNode;

public interface ComputeJump {

	public Set<MyNode> getJumpVars(Set<MyNode> assignedNodes, Map<MyNode, Set<MyNode>> confIn, Map<MyNode, Set<MyNode>> confOut);
}
