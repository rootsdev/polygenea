package org.rootsdev.polygenea.nodes;

import java.util.Collections;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

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
public class Match extends Thing {

	/** A set of Thing nodes that all refer to the same real-world thing. */ 
	public final SortedSet<Thing> same;
	
	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Match(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		SortedSet<Thing> backing = new TreeSet<Thing>();
		for (Object o : (Iterable<?>) map.get("subjects")) {
			Thing t = (Thing) lookup.lookup(o);
			if (backing.contains(t)) throw new IllegalArgumentException("duplicate claim " + t.getUUID());
			backing.add(t);
		}
		this.same = Collections.unmodifiableSortedSet(backing);
		this.selfCheck();
	}

	/** Constructor used by code that wishes to create new objects 
	 * @param source How we know these are the same (usually an Inference)
	 * @param same The Things that all refer to the same real-world thing.
	 */
	public Match(Source source, Thing... same) {
		super(source);
		SortedSet<Thing> backing = new TreeSet<Thing>();
		for (Thing t : same) {
			if (backing.contains(t)) throw new IllegalArgumentException("duplicate claim " + t.getUUID());
			backing.add(t);
		}
		this.same = Collections.unmodifiableSortedSet(backing);
		this.selfCheck();
	}
}
