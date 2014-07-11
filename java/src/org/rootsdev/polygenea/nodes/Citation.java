package org.rootsdev.polygenea.nodes;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Citation node represents a pointer from the digital system to the physical
 * world. Citations are unique among polygenea nodes in that they have an
 * unbounded number of partially-constrained fields, realized in this
 * implementation in the details field. This general form allows the user to
 * provide as much information as they wish, so if for example they want to
 * provide ISBN-10 and ISBN-13 and DOI and publisher and a visual description of
 * the book binding binding and latitude/longitude of the archive where it is
 * found and so on, they may.
 * <p>
 * At some point FHISO will probably produce a standardised list of citation
 * keys and acceptable formatting of their values, but that will be a sufficient
 * set rather than the only fields permitted.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public class Citation extends Node {

	/** 
	 * Our best description of the real-world existence of the thing being cited.
	 * <p>
	 * Unlike other Node fields, this one will be merged with the overall JSON, not included as a "details" entry. 
	 */
	public final SortedMap<String, Object> details;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects (ignored)
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Citation(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		SortedMap<String,Object> details = new TreeMap<String,Object>();
		for(String key : map.keySet()) {
			if (key.startsWith("!")) continue;
			details.put(key, map.get(key));
		}
		this.details = Collections.unmodifiableSortedMap(details);
	}

	/**
	 * A convenience constructor allowing the key:value mapping to be specified
	 * inline.
	 * 
	 * @param kind
	 *            The Citation.Kind of this Citation.
	 * @param fields
	 *            An even number of Strings; the 1st, 3rd, etc are keys and the
	 *            2nd, 4th, etc are the corresponding values.
	 */
	public Citation(String key1, Object val1, Object... fields) {
		super();

		SortedMap<String, Object> details = new TreeMap<String, Object>();
		details.put(key1, val1);

		assert fields.length % 2 == 0 : "arguments must be in (\"key\", value) pairs";
		for (int i = 0; i < fields.length; i += 2) {
			if (fields[i] == null) throw new IllegalArgumentException("all keys must be non-null");
			if (!(fields[i] instanceof String)) throw new IllegalArgumentException("all keys must be Strings");
			String key = (String)fields[i];
			if (key.startsWith("!")) throw new IllegalArgumentException("no key may start with a '!' character");
			if (details.containsKey(key)) throw new IllegalArgumentException("all keys must be distinct (\"" + key + "\" appears more than once)");
			if (fields[i + 1] == null) continue;
			details.put(key, fields[i + 1]);
		}

		this.details = java.util.Collections.unmodifiableSortedMap(details);
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects 
	 * 
	 * @param kind
	 *            The Citation.Kind of this Citation.
	 * @param fields
	 *            The key:value pairs of this object.
	 */
	public Citation(java.util.SortedMap<String, Object> fields) {
		super();
		this.details = java.util.Collections.unmodifiableSortedMap(fields);
		this.selfCheck();
	}
	
	
	/**
	 * Creates a SortedMap version of this node, suitable for JSON serialisation
	 * in canonical form (hence Sorted). Overridden to put the keys in details
	 * directly into the return value instead of having a "details":{...} field.
	 * 
	 * @param withUUID
	 *            Only invokes .getUUID() and includes "!uuid" if this is true.
	 * @return a SortedMap containing the special keys "!class" and "!uuid" as
	 *         well as all of the entries in the details map.
	 */
	@Override
	protected SortedMap<String, Object> toSerialize(boolean withUUID) {
		SortedMap<String, Object> ans = new TreeMap<String, Object>();
		for(String key : this.details.keySet()) ans.put(key, this.details.get(key));
		ans.put("!class", this.getClass().getSimpleName());
		if (withUUID) ans.put("!uuid", this.getUUID());
		return ans;
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (details.size() < 1) {
			log.append("Should have at least one real field\n");
			ok = false;
		} 
		for(String key : details.keySet()) {
			if (key == null) {
				log.append("Should not have null keys\n");
				ok = false;
			}
			if (key.length() == 0) {
				log.append("Should not have empty keys\n");
				ok = false;
			}
			if (key.startsWith("!")) {
				log.append("Should not have keys starting with an '!'\n");
				ok = false;
			}
			if (Character.isWhitespace(key.charAt(0))) {
				log.append("Should not have keys starting with white space\n");
				ok = false;
			}
			if (Character.isISOControl(key.charAt(0))) {
				log.append("Should not have keys starting with a control character\n");
				ok = false;
			}
			if (details.get(key) == null) {
				log.append("Should not have null values\n");
				ok = false;
			}
		}
		return ok;
	}
}
