package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;
import java.util.TreeMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Citation node represents a pointer from the digital system to the physical
 * world. Citations are unique among polygenea nodes in that they have an
 * unbounded number of partially-understood fields, wrapped in this
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

	/** Our best description of the real-world existence of the thing being cited. */
	public final SortedMap<String, String> details;
	/** What broad category of citation this is. */
	public final Kind kind;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	@SuppressWarnings("unchecked")
	public Citation(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.details = (SortedMap<String, String>) map.get("details");
		this.kind = Kind.valueOf((String) map.get("kind"));
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
	public Citation(Kind kind, String... fields) {
		super();
		this.kind = kind;

		SortedMap<String, String> details = new TreeMap<String, String>();

		assert fields.length % 2 == 0 : "arguments must be in \"key\", \"value\", pairs";
		for (int i = 0; i < fields.length; i += 2) {
			if (details.containsKey(fields[i])) throw new IllegalArgumentException("all keys must be distinct (\"" + fields[i] + "\" appears more than once)");
			details.put(fields[i], fields[i + 1]);
		}

		this.details = java.util.Collections.unmodifiableSortedMap(details);
	}

	/**
	 * Constructor used by code that wishes to create new objects 
	 * 
	 * @param kind
	 *            The Citation.Kind of this Citation.
	 * @param fields
	 *            The key:value pairs of this object.
	 */
	public Citation(Kind kind, java.util.SortedMap<String, String> fields) {
		super();
		this.kind = kind;
		this.details = java.util.Collections.unmodifiableSortedMap(fields);
	}

	/**
	 * There are currently three kinds of things one might cite: USER,
	 * TRANSIENT, and DURABLE.
	 */
	public static enum Kind {
		/**
		 * USER is used for things a user of the system knows or does
		 * themselves. A common USER Citation might refer to an event the
		 * genealogist participated in first-hand.
		 * <ul>
		 * <li>For something the user was told but did not witness directly, use
		 * TRANSIENT instead.</li>
		 * <li>For something written down or recorded, use DURABLE instead.</li>
		 * </ul>
		 */
		USER,

		/**
		 * TRANSIENT is used for external things that do not last, and hence
		 * that other researchers can't verify. A common TRANSIENT Citation
		 * might be a conversation or interview.
		 * <ul>
		 * <li>To describe the user's own experiences, use USER instead.</li>
		 * <li>For something written down or recorded, use DURABLE instead.</li>
		 * </ul>
		 */
		TRANSIENT,

		/**
		 * DURABLE is used for external things that other researchers could
		 * access with sufficient motivation. These include documents,
		 * photographs, recordings, monuments, etc.
		 * <ul>
		 * <li>If you are not describing a physical artefact, use USER or
		 * TRANSIENT instead.</li>
		 * </ul>
		 */
		DURABLE
	}

}
