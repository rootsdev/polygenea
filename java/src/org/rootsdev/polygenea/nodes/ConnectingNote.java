package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A ConnectingNote embodies directed relationships between two Node objects
 * that may not be claims. Its design closely mirrors that of the Connection
 * node. However, like a Note it sits outside the main data, targeting human
 * understanding instead of expressing information about the researched
 * material. Like a Note, it uses a creator field in place of a Source.
 * 
 * @see Connection
 * @see Note
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public class ConnectingNote extends Node {

	/** The node being described. */
	public final Node subject;
	/** The node used to describe it. */
	public final Node object;
	/** The relationship between the described and the describer. */
	public final String relation;
	/** Who created this note, or {@literal null} if the note is anonymous. */
	public final String creator;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public ConnectingNote(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.subject = (Node) lookup.lookup(map.get("subject"));
		this.object = (Node) lookup.lookup(map.get("object"));
		this.relation = (String) map.get("relation");
		this.creator = (String) map.get("creator");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * @param subject What this connects to
	 * @param object What this connects from
	 * @param relation What kind of connection this is
	 * @param creator Who created this connection
	 */
	public ConnectingNote(Node subject, Node object, String relation, String creator) {
		super();
		this.subject = subject;
		this.object = object;
		this.relation = relation;
		this.creator = creator;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (subject == null) {
			log.append("subject should not be null");
			ok = false;
		}
		if (object == null) {
			log.append("object should not be null");
			ok = false;
		}
		if (relation == null || relation.length() == 0) {
			log.append("relation should not be null or empty");
			ok = false;
		}
		// the creator may be null
		return ok;
	}
}
