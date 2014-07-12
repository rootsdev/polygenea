package org.rootsdev.polygenea.nodes;

import java.util.Collections;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Grouping embodies undirected relationships between two or more other
 * claims. Groupings are often introduced via “<em>(list of subjects)</em> are
 * <em>relationship</em>” or “<em>relationship</em>
 * <em>(list of subjects)</em>”. Directed relationships should be represented
 * using Connection instead. If the group itself may have properties, the group
 * should be a Thing and membership in it a Connection between each member and
 * the group.
 * <p>
 * One Grouping relationship types deserve special note: "same", as in
 * "this node and that node both represent the same real-world thing." Because
 * of its semantic importance throughout Polygenea, the "same" relationship
 * applied to Thing nodes is handled by its own class (Match) instead of by
 * Grouping. Grouping nodes do handle other "same" claims (between Property or
 * Connection nodes) as well as related ideas such as of "distinct".
 * <p>
 * At some point a set of standardised relation fields will be produced. Even
 * once they are, though, custom relations will be permitted.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class Grouping extends Claim {

	/** The set of nodes that all share the given relationship with one another. */
	public final SortedSet<Claim> subjects;
	/** The relationship between these nodes. */
	public final String relation;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Grouping(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		this.relation = (String) map.get("relation");
		SortedSet<Claim> backing = new TreeSet<Claim>();
		for (Object o : (Iterable<?>) map.get("subjects")) {
			Claim c = (Claim) lookup.lookup(o);
			if (backing.contains(c)) throw new IllegalArgumentException("duplicate claim " + c.getUUID());
			backing.add(c);
		}
		this.subjects = Collections.unmodifiableSortedSet(backing);
		this.selfCheck();
	}

	/** Constructor used by code that wishes to create new objects 
	 * @param source How we know these can be grouped.
	 * @param relation What kind of grouping this is.
	 * @param subjects What is being grouped.
	 */
	public Grouping(Source source, String relation, Claim... subjects) {
		super(source);
		this.relation = relation;
		SortedSet<Claim> backing = new TreeSet<Claim>();
		for (Claim c : subjects) {
			if (backing.contains(c)) throw new IllegalArgumentException("duplicate claim " + c.getUUID());
			backing.add(c);
		}
		this.subjects = Collections.unmodifiableSortedSet(backing);
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (subjects.size() < 2) {
			log.append("cannot have a group of only " + subjects.size() + " subject(s)");
			ok = false;
		}
		for (Claim c : subjects)
			if (c == null) {
				log.append("no subject should not be null");
				ok = false;
			}
		if (relation == null || relation.length() == 0) {
			log.append("relation should not be null or empty");
			ok = false;
		}
		return ok;
	}
}
