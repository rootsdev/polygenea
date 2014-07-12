package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Connection node embodies directed relationships between two other claims.
 * The Connection node “<em>X</em> —<em>Y</em>→ <em>Z</em>” most often expresses
 * one of the following ideas:
 * <ul>
 * <li><em>Y</em> is a transitive verb, <em>X</em> is the subject and <em>Z</em>
 * is the direct object. For example, “Cain killed Able” yields “Cain —killed→
 * Able”.
 * <li><em>Y</em> is a noun, <em>Z</em> is (a/an/the) <em>Y</em> of <em>X</em>.
 * For example, “Seth's mother is Eve” yields “Seth —mother→ Eve”
 * and “York is inside of England” “England —inside→ York”.
 * </ul>
 * Some sources provide undirected and many-party relations, such as “A, B, and
 * C were brothers”. That kind of claim is handled by Grouping nodes.
 * <p>
 * Some Connection nodes can be reduced to Property nodes. See the discussion
 * under {@link Property} for more.
 * <p>
 * At some point a set of standardised relation fields will be produced. Even
 * once they are, though, custom relations will be permitted.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class Connection extends Claim {

	/** See main class description. */
	public final Claim to;
	/** See main class description. */
	public final Claim from;
	/** See main class description. */
	public final String relation;

	/**
	 * Constructor used by JSON loading methods in Node and Database
	 * 
	 * @param map
	 *            A JSON object of this node
	 * @param lookup
	 *            How to resolve node references into Node objects
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not proper JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public Connection(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		this.to = (Claim) lookup.lookup(map.get("to"));
		this.from = (Claim) lookup.lookup(map.get("from"));
		this.relation = (String) map.get("relation");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * 
	 * @param source
	 *            Where we got this Connection
	 * @param from
	 *            See main class description
	 * @param relation
	 *            See main class description
	 * @param to
	 *            See main class description
	 */
	public Connection(Source source, Claim from, String relation, Claim to) {
		super(source);
		this.to = to;
		this.from = from;
		this.relation = relation;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (to == null) {
			log.append("to should not be null");
			ok = false;
		}
		if (from == null) {
			log.append("from should not be null");
			ok = false;
		}
		if (relation == null || relation.length() == 0) {
			log.append("relation should not be null or empty");
			ok = false;
		}
		return ok;
	}
}
