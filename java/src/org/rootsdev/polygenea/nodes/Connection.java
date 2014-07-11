package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Connection embodies directed relationships between two other claims. Any
 * possessive like “<em>object</em>'s <em>relation</em> is <em>subject</em>”
 * and most prepositional phrases like “<em>subject</em> is the
 * <em>relation</em> of <em>object</em>” suggest Connections. When drawn as
 * an arrow, it is most natural to say “<em>object</em> —<em>relation</em>→
 * <em>subject</em>”, as in “Eve —child→ Able” or “York —inside→
 * England”.
 * <p>
 * Some sources provide undirected and many-party connections, such as
 * "A, B, and C were brothers". That kind of claim is handled by Grouping nodes.
 * <p>
 * Some Connection nodes can be reduced to Property nodes. See the discussion
 * under {@link Property} for more.
 * <p>
 * At some point a set of standardised relation fields will be produced. Even
 * once they are, though, custom relations will be permitted.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public class Connection extends Claim {

	/** The claim being described. */
	public final Claim subject;
	/** The claim used to describe it. */
	public final Claim object;
	/** The relationship between the described and the describer. */
	public final String relation;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Connection(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		this.subject = (Claim) lookup.lookup(map.get("subject"));
		this.object = (Claim) lookup.lookup(map.get("object"));
		this.relation = (String) map.get("relation");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * @param source Where we got this Connection
	 * @param subject What we are connecting to
	 * @param object What we are connecting from
	 * @param relation What kind of connection
	 */
	public Connection(Source source, Claim subject, Claim object, String relation) {
		super(source);
		this.subject = subject;
		this.object = object;
		this.relation = relation;
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
		return ok;
	}
}
