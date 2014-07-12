package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.HasIdentity;
import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * Each Thing node represents the claim a source might make that
 * "something exists". These are almost always implicit, as most sources begin
 * with descriptions of the characteristics of extant entities, not with
 * explicit assertions that things exist. For example, the text "John's brother"
 * asserts the existence of two things: one has the property "name:John" and
 * both are connected by a "brother" connection.
 * <p>
 * Each Thing has identity and a source, but, by the principle of sensible
 * disbelief, nothing else. We do not even store what type of thing it is
 * internally since it is quite possible in some sources to mistake a pet for a
 * person or the like.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
@HasIdentity()
public class Thing extends Claim {

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Thing(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		this.selfCheck();
	}

	/** Constructor used by code that wishes to create new objects 
	 * @param source Why we know this thing exists
	 */
	public Thing(Source source) {
		super(source);
		this.selfCheck();
	}
}
