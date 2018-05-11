package ca.ucalgary.ispia.graphpatterns.graph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a node in the graph pattern. This class is called MyNode instead of
 * Node to avoid overloading the class name Node provided by Neo4j.
 * A Node consists of an internal id, a label, and a set of attributes.
 * A label is not the same as an id. The label represents the label from Neo4j, where
 * id represents the internal id. Additionally, the node can have an attribute also
 * named id.   
 * @author szrrizvi
 */

public class MyNode implements HasAttributes, Serializable{
	
	private static final long serialVersionUID = -7348537080994739107L;
	
	
	//Fields
	private final int id;
	private final String label;
	private Map<String, String> attributes;
	
	/**
	 * Initializes the given fields
	 * @param id The id
	 * @param label The node label
	 */
	public MyNode(int id, String label){
		//Set the fields and initialize the atrributes map
		this.id = id;
		this.label = label;
		this.attributes = new HashMap<String, String>();
	}
	
	/**
	 * @return the id
	 */
	public int getId(){
		return this.id;
	}
	
	/**
	 * @return the label
	 */
	public String getLabel(){
		return this.label;
	}
	
	/**
	 * Adds the given attribute name, value pair to the map of attributes.
	 * @param name The name of the attribute.
	 * @param val The value of the attribute.
	 * @return True if the pair is added to the attributes map, else false.
	 */
	public boolean addAttribute(String name, String val){
		
		//If the attribute already exists, then return false.
		//Else add the pair to the map and return true.
		if (attributes.containsKey(name) || val == null){
			return false;
		}
		else {
			attributes.put(name, val);
		}
		
		return true;
	}
	
	/**
	 * Returns the value of given attribute name.
	 * @param name The attribute name
	 * @return
	 */
	public String getAttribute(String name){
		return attributes.get(name);
	}
	
	/**
	 * Checks if the node has at least 1 attribute
	 * @return True if the node has at least 1 attribute, else false.
	 */
	public boolean hasAttributes(){
		if (attributes.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks if the node has at least 1 attribute
	 * @return True if the node has at least 1 attribute, else false.
	 */
	public boolean hasAttribute(String attrName){
		return attributes.containsKey(attrName);
	}
	
	public void setAttributes(Map<String, String> attributes){
		this.attributes = attributes;
	}
	
	/**
	 * @return The map of attributes
	 */
	public Map<String, String> getAttributes(){
		return this.attributes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MyNode other = (MyNode) obj;
		if (id != (other.getId()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		
		StringBuilder str = new StringBuilder(id+": ");
		for (String key : attributes.keySet()){
			str.append("( " + key + " , " + attributes.get(key) + " ) ");
		}
		return str.toString();
	}
	
	
}
