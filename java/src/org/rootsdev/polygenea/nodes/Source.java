package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;

/**
 * The Source class is merely a placeholder to represent those kinds of nodes
 * that can be the source of a Claim.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public abstract class Source extends Node {
	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Source(SortedMap<String, Object> map) {
		super(map);
	}
	/** Constructor used by code that wishes to create new objects */
	public Source() {
		super();
	}
}