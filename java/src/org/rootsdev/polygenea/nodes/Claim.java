package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * The Claim class is merely a placeholder to represent those kinds of nodes
 * that represent claims made by a source.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public abstract class Claim extends Node {
	
	/** The thing that is making this claim. */
	public final Source source;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Claim(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.source = (Source)lookup.lookup(map.get("source"));
	}

	/** Constructor used by code that wishes to create new objects 
	 * @param source the Source of this claim
	 */
	public Claim(Source source) {
		super();
		this.source = source;
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (source == null) {
			log.append("Source should not be null");
			ok = false;
		}
		return ok;
	}
}