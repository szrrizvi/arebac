package ca.ucalgary.ispia.graphpatterns.graph;

import java.util.Map;
/**
 * Specifies entities that have attributes, along with the methods that must be supported.
 * @author szrrizvi
 *
 */
public interface HasAttributes {
	/**
	 * Given a attribute name, return its value (as string).
	 * @param key The name of the attribute.
	 * @return The attribute value (as string).
	 */
	public String getAttribute(String key);
	
	/**
	 * @return True if entity has at least one attribute, else return false.
	 */
	public boolean hasAttributes();
	
	/**
	 * Checks if the entity has the given attribute.
	 * @param key The name of the attribute to check.
	 * @return True if the enetity has the attribute, else false.
	 */
	public boolean hasAttribute(String key);
	
	/**
	 * @return The map of all attribute names and values. 
	 */
	public Map<String, String> getAttributes();
	
	/**
	 * Adds the given attribute name and value to the entity.
	 * @param key The name of the attribute.
	 * @param val The value of the attribute.
	 * @return True if attribute successfully added, else false.
	 */
	public boolean addAttribute(String key, String val);
}
