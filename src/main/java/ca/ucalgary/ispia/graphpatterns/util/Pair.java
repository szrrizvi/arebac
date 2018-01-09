package ca.ucalgary.ispia.graphpatterns.util;

import java.io.Serializable;

/**
 * A generic relationship between two objects/values.
 * @param <F> Type of first object/value.
 * @param <S> Type of second object/value.
 */

// Used for extend

public class Pair<F, S> implements Serializable{
	private static final long serialVersionUID = 9140234360009053518L;
	
	/**
	 * The first object/value.
	 */
	public final F first;
	/**
	 * The second object/value.
	 */
	public final S second;
	
	/**
	 * Initializes the fields.
	 * @param first The first object/value.
	 * @param second The second object/value.
	 */
	public Pair(F first, S second){
		this.first = first;
		this.second = second;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + "Pair".hashCode();
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;		// Compare pointers
		}
		if (obj == null) {
			return false;		// False if obj is null
		}
		if (!(obj instanceof Pair)) {
			return false;		// False if obj is not of same instance
		}
		Pair<?,?> other = (Pair<?,?>) obj;
		if (first == null) {
			if (other.first != null) {
				return false;	// False if first is null and obj's first is not null
			}
		} else if (!first.equals(other.first)) {
			return false;		// False is first does not match
		}
		if (second == null) {
			if (other.second != null) {
				return false;	// False if second is null and obj's second is not null
			}
		} else if (!second.equals(other.second)) {
			return false;		// False if second does not match
		}
		return true;			// If reached here, true
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + "]";
	}


}

