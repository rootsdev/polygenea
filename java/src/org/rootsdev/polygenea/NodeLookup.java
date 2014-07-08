package org.rootsdev.polygenea;

/**
 * A NodeLookup allows nodes to change node references in JSON into Node
 * objects.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public interface NodeLookup {
	/**
	 * Convert a JSON value into a Node object by looking it up in some pool of
	 * objects.
	 * <p>
	 * The most common node encodings are UUID strings and integer list indices,
	 * but for future compatibility and to simplify interaction with Map&lt;String,
	 * Object&gt; representations of JSON the interface takes an arbitrary
	 * java.lang.Object.
	 * 
	 * @param o
	 *            The JSON value to look up as a Node
	 * @return The Node found; or null if o was null.
	 * @throws IllegalArgumentException
	 *             if o is not a type of object that can be looked up, or it is
	 *             that type but it does not correspond to any Node that this
	 *             NodeLookup knows about.
	 */
	public Node lookup(Object o);
}
