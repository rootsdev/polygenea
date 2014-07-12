package org.rootsdev.polygenea.nodes;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.Node;
import org.rootsdev.polygenea.NodeLookup;

/**
 * Inference rules describe how to derive new claims based on existing claims.
 * Inference rules can be true by definition (e.g., "son-of" implies
 * "child-of"), by laws of logic (e.g., "A" and "not A" implies one of the two
 * is wrong), by laws of nature (e.g., every human has a mother), by statistical
 * trend (e.g., mothers are born at least 15 years before their eldest child),
 * or by any combination of these.
 * <p>
 * InferenceRule nodes can only represent rules that have a fixed number of
 * antecedent nodes. If a rule has one of a finite set of possible antecedents,
 * represent it using several InferenceRule nodes. Rules with an arbitrary
 * number of antecedents can be represented by adding some kind of transitivity
 * rule. For example, the rule “A is a descendant of B if A is B's child, or B's
 * grandchild, or B's great-grandchild, etc.” can be encoded as two rules:
 * “—child→ implies —descendant→” and “—descendant→(person)—descendant→ implies
 * —descendant→”. Transitive rules of this nature have the same power as the
 * Kleene star.
 * <p>
 * Both antecedents and consequents of rules are stored in the same JSON format
 * as node serialisation itself, with the following constraints:
 * <ul>
 * <li>Neither antecedents nor consequents contain "!uuid" fields since they are
 * intended to represent patterns applicable to many nodes.
 * <li>Consequents do not contain "source" fields since their source will be the
 * new Inference this rule suggests.
 * <li>Antecedents may omit any field that they do not care about.
 * <li>Node references must be by index, not by UUID. The index is into the
 * virtual list concatenate(antecedents, consequents).
 * <li>Antecedent value strings beginning with an {@literal '!'} and containing
 * a {@literal ':'} are treated specially; see the documentation for
 * valueMatches for details.
 * </ul>
 * <p>
 * It is my hope that the nature of the inference rule will make it easy for
 * user interface designers to allow any user to specify a rule. The idea is
 * that the user can identify an example of the rule's applicability (which
 * gives a candidate set of antecedents and consequents) and then be asked a set
 * of questions that lets the tool discard fields from the antecedents or write
 * them in more general form using "!___:___" strings.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class InferenceRule extends Node {
	public final List<SortedMap<String, Object>> antecedents;
	public final List<SortedMap<String, Object>> consequents;

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
	@SuppressWarnings("unchecked")
	public InferenceRule(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.antecedents = (List<SortedMap<String, Object>>) map.get("antecedents");
		this.consequents = (List<SortedMap<String, Object>>) map.get("consequents");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * 
	 * @param a
	 *            The antecedents of this rule
	 * @param c
	 *            The consequents of this rule
	 */
	public InferenceRule(List<SortedMap<String, Object>> a, List<SortedMap<String, Object>> c) {
		super();
		this.antecedents = a;
		this.consequents = c;
		this.selfCheck();
	}

	/**
	 * Guesses at what rule was intended to get from a concrete set of
	 * antecedents to a concrete set of consequents.
	 * <p>
	 * The basic process is as follows:
	 * <ol>
	 * <li>An IllegalArgumentException is thrown if the consequents refer to
	 * nodes that are not in the lists of antecedents or consequents
	 * <li>some kind of sorting of antecedents
	 * <li>The antecedents are converted to JSON objects.
	 * <li>The UUID fields are stripped from antecedents and consequents
	 * <li>The source fields are stripped from all consequents
	 * <li>external references in antecedents are removed
	 * <li>some kind of !contains: and !xref: substitution
	 * </ol>
	 * 
	 * @param a
	 *            An example set of antecedents
	 * @param c
	 *            An example set of consequents
	 * @return A rule that will match this antecedents/consequents example
	 */
	public static InferenceRule fromExample(List<Claim> a, List<SortedMap<String, Object>> c) {
		throw new UnsupportedOperationException("Unimplemented method");
	}

	/**
	 * Derives the consequences of applying this rule to the provided
	 * antecedents.
	 * 
	 * @param antecedents
	 *            An ordered array of {@link Claim}s that should fit this rule
	 * @return An {@link Inference} and a set of derived {@link Claim}s sourced
	 *         to that {@link Inference}, or {@literal null} if the provided
	 *         {@link Claim}s do not match this rule.
	 */
	public Node[] consequentsOf(Claim... antecedents) {
		// step 1: check antecedents
		if (antecedents.length != this.antecedents.size()) return null;
		for (int i = 0; i < antecedents.length; i += 1) {
			SortedMap<String, Object> targ = this.antecedents.get(i);
			Claim have = antecedents[i];
			if (targ.containsKey("!class") && !targ.get("!class").equals(have.getClass().getSimpleName())) return null;
			for (String k : targ.keySet())
				if (!k.startsWith("!")) {
					try {
						Field f = have.getClass().getField(k);
						Object o = f.get(have);
						if (!valueMatches(o, targ.get(k), antecedents)) return null;
					} catch (NoSuchFieldException e) {
						return null;
					} catch (IllegalAccessException e) {
						return null;
					}
				}
		}
		// step 2: make an inference node
		Node[] answer = new Node[this.consequents.size() + 1];
		answer[0] = new Inference(this, antecedents);
		// step 3: derive all consequents
		int i = 1;
		NodeLookup nl = new ConsequentLookup(antecedents, answer);
		for (SortedMap<String, Object> json : this.consequents) {
			answer[i] = Node.fromJSON(json, nl);
		}
		return answer;
	}

	private static class ConsequentLookup implements NodeLookup {
		private Claim[] a;
		private Node[] b;

		public ConsequentLookup(Claim[] antecedents, Node[] answer) {
			this.a = antecedents;
			this.b = answer;
		}

		public Node lookup(Object o) {
			if (o == null) return b[0]; // the inference; works because map.get("source") will be null 
			if (o instanceof Number) {
				int i = ((Number) o).intValue();
				if (i < 0) throw new IllegalArgumentException("Nodes must have non-negative index, not " + o);
				if (i < a.length) return a[i];
				i = i - a.length + 1; // +1 to skip the inference 
				if (i < b.length) return b[i];
				throw new IllegalArgumentException("Node index too large: " + o);
			}
			throw new IllegalArgumentException("Can't convert a " + o.getClass() + " into a Node");
		}
	}

	private static final Pattern BANG_COLON = Pattern.compile("!([^:]*):(.*)");
	private static final Pattern XREF_SYNTAX = Pattern.compile("([0-9]+)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

	/**
	 * Compares a value found inside a Node to a target found inside an
	 * antecedent. At present, the following grammar is supported:
	 * <ul>
	 * <li>Collections must have the same length, and are recursed into in
	 * parallel
	 * <li>Claim values may be matched by integer offsets into the lookup
	 * parameter list
	 * <li>Most strings are matched using .equals()
	 * <li>Target strings beginning with an {@literal '!'} and containing a
	 * {@literal ':'} are treated specially depending on what lies between the !
	 * and the :
	 * <dl>
	 * <dt>!re:</dt>
	 * <dd>Regular expression: the value after the colon is matched using
	 * String's match method.</dd>
	 * <dt>!contains:</dt>
	 * <dd>Matches collections if any element of the collection would match what
	 * follows the colon (which must be JSON-encoded).</dd>
	 * <dt>!xref:</dt>
	 * <dd>The right-hand side must be an integer, a period, and a field name in
	 * that order. Matches value with lookup[integer].fieldName.</dd>
	 * </dl>
	 * Other !___: openings will be added later to allow more involved match
	 * logic.
	 * </ul>
	 * 
	 * @param value
	 *            The actual value
	 * @param target
	 *            The target pattern
	 * @param lookup
	 *            The context in which targets referencing claims can be looked
	 *            up
	 * @return {@literal true} if the value matches the target; {@literal false}
	 *         otherwise.
	 */
	public static boolean valueMatches(Object value, Object target, Claim... lookup) {
		if (target instanceof Collection) {
			if (!(value instanceof Collection)) return false;
			if (((Collection<?>) target).size() != ((Collection<?>) value).size()) return false;
			Iterator<?> ti = ((Collection<?>) target).iterator();
			Iterator<?> vi = ((Collection<?>) value).iterator();
			while (ti.hasNext())
				if (!valueMatches(vi.next(), ti.next(), lookup)) return false;
			return true;
		}
		if (target instanceof Number) {
			int i = ((Number) target).intValue();
			if (i >= lookup.length || i < 0) return false;
			return lookup[i].equals(value);
		}
		if (target instanceof String) {
			String starg = (String) target;
			Matcher m = BANG_COLON.matcher(starg);
			if (m.matches()) {
				String kind = m.group(1);
				starg = m.group(2);
				if ("re".equals(kind)) {
					return value instanceof String && ((String) value).matches(starg);
				} else if ("contains".equals(kind)) {
					if (!(value instanceof Collection)) return false;
					target = JSONParser.parse(starg);
					for (Object o : (Collection<?>) value)
						if (valueMatches(o, target, lookup)) return true;
					return false;
				} else if ("xref".equals(kind)) {
					m = XREF_SYNTAX.matcher(starg);
					if (!m.matches()) throw new UnsupportedOperationException("Unknown !xref: syntax " + starg);
					Claim c = lookup[Integer.parseInt(m.group(1))];
					try {
						Field f = c.getClass().getField(m.group(2));
						return valueMatches(value, f.get(c), lookup);
					} catch (NoSuchFieldException e) {
						return false; // throw new IllegalArgumentException("node " + m.group(1) + " of type " + c.getClass() + " has no " + m.group(2) + " field");
					} catch (IllegalAccessException e) {
						throw new AssertionError("Field " + m.group(2) + " of class " + c.getClass() + " should have been public");
					}
				} else {
					throw new UnsupportedOperationException("Unknown target string beginning !" + kind + ":");
				}
			} else {
				return starg.equals(value);
			}
		}
		return false;
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (antecedents.size() < 1) {
			log.append("cannot have an inference rule with no antecedents");
			ok = false;
		}
		if (consequents.size() < 1) {
			log.append("cannot have an inference rule with no consequents");
			ok = false;
		}
		for (SortedMap<String, Object> a : antecedents) {
			if (a == null) {
				log.append("no antecedent should be null");
				ok = false;
			}
			if (a.containsKey("!uuid")) {
				log.append("antecedents should not be node-specific");
				ok = false;
			}
		}
		for (SortedMap<String, Object> a : consequents) {
			if (a == null) {
				log.append("no consequent should be null");
				ok = false;
			}
			if (a.containsKey("!uuid")) {
				log.append("consequents should not specify a uuid");
				ok = false;
			}
			if (a.containsKey("source")) {
				log.append("consequents should not specify a source");
				ok = false;
			}
			if (!a.containsKey("!class")) {
				log.append("consequents must specify a class");
				ok = false;
			}
		}
		// TODO: verify that consequents make no forward references, and that all references are by index
		// TODO: verify that consequents are otherwise complete
		return ok;
	}
}
