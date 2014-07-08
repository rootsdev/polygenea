package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * Note nodes are generic human-targeted information that can be attached to any
 * other node. They are intended for small pieces of text. If more scope is
 * needed, create a Citation with the USER kind, a Source with the content, and
 * a set of ConnectingNote nodes to represent its relationship to other nodes.
 * 
 * @see ConnectingNote
 * @see Property
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public abstract class Note extends Node {
	/** The node to which the note applies. */
	public final Node about;
	/** What is being said about that node. */
	public final String content;
	/** Who is doing the saying, or {@literal null} if anonymous. */
	public final String creator;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Note(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.about = lookup.lookup(map.get("about"));
		this.content = (String) map.get("content");
		this.creator = (String) map.get("creator");
		this.selfCheck();
	}

	/** Constructor used by code that wishes to create new objects
	 * @param about A Node to which to attached this note
	 * @param content Whatever text we wish to provide
	 * @param creator Who made the note (may be null for anonymous notes)
	 */
	public Note(Node about, String content, String creator) {
		super();
		this.about = about;
		this.content = content;
		this.creator = creator;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (this.about == null) {
			log.append("notes have to be about some Node");
			ok = false;
		}
		if (this.content == null || this.content.length() < 1) {
			log.append("notes must have some content");
			ok = false;
		}
		// creator may be null...
		return ok;
	}
}