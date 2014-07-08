package org.rootsdev.polygenea;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Node is a superclass of all Polygenea node objects. It handles JSON
 * serialisation (via reflection), UUID creation (via {@link UUID5}.fromUTF8 for
 * nodes without identity, {@link java.util.UUID}.randomUUID for nodes with
 * Identity), and portions of JSON parsing (aided by special constructors in
 * each subclass).
 * <p>
 * Writers of subclasses should be aware of the following assumptions of this
 * class:
 * <ul>
 * <li>All serialisable attributes of a node are declared as
 * {@code public final} fields.
 * <li>Every node has a JSON-reading constructor that accepts a JSON object and
 * a NodeLookup object. See the Note class for a simple stand-alone example of
 * what these might look like.
 * <li>All fields should have a well-defined canonical representation. Thus, the
 * interfaces SortedMap and SortedSet are preferred over Map and Set; SortedSet
 * is also preferred over most other Collections, with the exception that List
 * is permissible if (and only if) the order of elements of the list has
 * semantic significance.
 * <li>Fields containing objects (as opposed to primitives) should be immutable.
 * {@link java.lang.String} is immutable by design, but most other classes are
 * not. The various {@link java.util.Collections}.unmodifiable___ methods are
 * useful here.
 * </ul>
 * Subclass writers are also encouraged to override the
 * Node.validate(StringBuilder log) method, calling super.validate(log)
 * somewhere inside; and to make the last line of each concrete class's
 * constructor be this.selfCheck(). While not essential, these practices can
 * help detect a variety of potential problems at node creation time rather than
 * later when the nodes are in use or are being serialised.
 * 
 * @see org.rootsdev.polygenea.nodes.Note
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public abstract class Node implements Comparable<Node> {

	private UUID uuid;
	private int height = -1; // 0 at the bottom of the DAG, increasing above that

	public boolean hasIdentity() {
		for(Annotation a : this.getClass().getDeclaredAnnotations())
			if (a.annotationType().equals(HasIdentity.class)) return true;
		return false;
	}
	
	/**
	 * Nodes form a directed acyclic graph. As such, each can be given a height.
	 * A leaf node (one where out().size() == 0) has height 0. Every other
	 * node's height is 1 + the maximum height of any of its out() nodes.
	 * <p>
	 * Height is primarily useful for serialising nodes such that dependencies
	 * precede nodes that depend upon them.
	 * <p>
	 * Height is computed on demand the first time this method is invoked on a
	 * node, but is thereafter cached for future reference.
	 * 
	 * @return The height of this node (&ge; 0)
	 */
	public int getHeight() {
		if (height == -1) {
			height = 0;
			for (Node n : this.out()) {
				int h = n.getHeight();
				if (h >= height) height = h + 1;
			}
		}
		return this.height;
	}

	/**
	 * If the node has a uuid, it is returned. Otherwise one is generated using
	 * either type-5 (if !hasIdentity) or type-4 (if hasIdentity).
	 * 
	 * @return This node's UUID.
	 */
	public final UUID getUUID() {
		if (this.uuid == null) {
			if (this.hasIdentity()) this.uuid = UUID.randomUUID();
			else this.uuid = UUID5.fromUTF8(UUID5.POLYGENEA_NAMESPACE, this.hashableJSON());
		}
		return this.uuid;
	}

	/**
	 * Performs any validation that the node might need. The Node class just
	 * checks the UUID. Subclasses should override this method to perform any
	 * additional validation they might need, such as verifying that fields are
	 * not null.
	 * 
	 * @param log
	 *            A place where descriptions of problems will be written. If the
	 *            node is valid, log is not used.
	 * @return true if the node is valid, false otherwise.
	 */
	public boolean validate(StringBuilder log) {
		if (this.uuid != null) {
			if (this.hasIdentity()) {
				if (this.uuid.version() != 1 && this.uuid.version() != 4) {
					if (log != null) log.append("because hasIdentity, uuid version should be 1 or 4 not ").append(this.uuid.version()).append("\n");
					return false;
				}
			} else {
				if (this.uuid.version() != 5) {
					if (log != null) log.append("because !hasIdentity, uuid version should be 5 not ").append(this.uuid.version()).append("\n");
					return false;
				}
				UUID correct = UUID5.fromUTF8(UUID5.POLYGENEA_NAMESPACE, this.hashableJSON());
				if (!this.uuid.equals(correct)) {
					if (log != null) log.append("contents hash to ").append(correct).append(" not to ").append(this.uuid).append("\n");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Performs a validate check and throws an IllegalArgumentException if it
	 * fails.
	 */
	protected void selfCheck() {
		StringBuilder sb = new StringBuilder();
		if (!this.validate(sb)) {
			throw new IllegalArgumentException(sb.toString());
		}
	}

	/**
	 * Creates a SortedMap version of this node, suitable for JSON serialisation
	 * in canonical form (hence Sorted). If subclasses override toSerialize,
	 * they MUST return a map where the following hold:
	 * 
	 * <pre>
	 * assert this.getClass().getSimpleName().equals(map.get(&quot;!class&quot;));
	 * if (withUUID) assert this.getUUID().equals(map.get(&quot;!uuid&quot;));
	 * else assert !map.containsKey(&quot;!uuid&quot;);
	 * </pre>
	 * 
	 * They must also ensure that they can parse the output of this method.
	 * 
	 * @param withUUID
	 *            Only invokes .getUUID() and includes "!uuid" if this is true.
	 * @return a SortedMap containing the special keys "!class" and "!uuid" as
	 *         well as an entry for every non-null public final field of this
	 *         object.
	 */
	protected SortedMap<String, Object> toSerialize(boolean withUUID) {
		SortedMap<String, Object> ans = new TreeMap<String, Object>();
		ans.put("!class", this.getClass().getSimpleName());
		if (withUUID) ans.put("!uuid", this.getUUID());
		for (Field f : this.getClass().getFields()) {
			if (Modifier.isPublic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
				try {
					Object val = f.get(this);
					if (val != null) ans.put(f.getName(), val);
				} catch (IllegalAccessException e) {
					throw new SecurityException(e);
				}
			}
		}
		return ans;
	}

	/**
	 * Creates a SortedMap version of this node, suitable for JSON serialisation
	 * in canonical form (hence Sorted). If subclasses override toSerialize,
	 * they MUST return a map where the following hold:
	 * 
	 * <pre>
	 * assert this.getClass().getSimpleName().equals(map.get(&quot;!class&quot;));
	 * if (withUUID) assert this.getUUID().equals(map.get(&quot;!uuid&quot;));
	 * else assert !map.containsKey(&quot;!uuid&quot;);
	 * </pre>
	 * 
	 * They must also ensure that they can parse the output of this method.
	 * 
	 * @param withUUID
	 *            Only invokes .getUUID() and includes "!uuid" if this is true.
	 * @return a SortedMap containing the special keys "!class" and "!uuid" as
	 *         well as an entry for every non-null public final field of this
	 *         object.
	 */
	protected final List<SortedMap<String, Object>> toSerializeWithDependencies(boolean withUUID) {
		Collection<Node> todo = this.dependsOn();
		Set<Node> done = new TreeSet<Node>();
		List<SortedMap<String, Object>> list = new ArrayList<SortedMap<String, Object>>(todo.size());
		for (Node n : todo) {
			if (done.contains(n)) continue;
			list.add(n.toSerialize(withUUID));
			done.add(n);
		}
		return list;
	}

	/**
	 * A full JSONification except there is no "!uuid" field; used to compute
	 * digest-based hashes.
	 * 
	 * @return A valid JSON object string representing this node
	 */
	public String hashableJSON() {
		StringBuilder sb = new StringBuilder();
		Node.jsonify(sb, this, XRefer.HASHABLE);
		return sb.toString();
	}

	/**
	 * A full JSONification, suitable for saving the node to disk or sharing the
	 * node with other computers.
	 * 
	 * @return A valid JSON object string representing this node
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Node.jsonify(sb, this, XRefer.STANDALONE);
		return sb.toString();
	}

	/**
	 * There are various ways a Node might be represented in JSON: as its UUID,
	 * its index, or as an object; and if as an object it might or might not
	 * contain a !uuid field and might represent any nested Nodes in various
	 * ways as well.
	 */
	public static interface XRefer {
		public void encode(StringBuilder sb, Node j);

		public static final XRefer AS_UUID = new XRefer() {
			public void encode(StringBuilder sb, Node j) {
				jsonify(sb, j.getUUID(), null);
			}
		};
		public static final XRefer HASHABLE = new XRefer() {
			public void encode(StringBuilder sb, Node j) {
				jsonify(sb, j.toSerialize(false), AS_UUID);
			}
		};
		public static final XRefer STANDALONE = new XRefer() {
			public void encode(StringBuilder sb, Node j) {
				jsonify(sb, j.toSerialize(true), AS_UUID);
			}
		};

		public static class Compressor implements XRefer {
			private static class Internal implements XRefer {
				Map<UUID, Integer> lookup;

				Internal(Map<UUID, Integer> lookup) {
					this.lookup = lookup;
				}

				public void encode(StringBuilder sb, Node j) {
					if (lookup.containsKey(j.getUUID())) jsonify(sb, lookup.get(j.getUUID()), this);
					else jsonify(sb, j.getUUID(), this);
				}
			}

			private Internal internal;

			public Compressor(Map<UUID, Integer> lookup) {
				this.internal = new Internal(lookup);
			}

			public void encode(StringBuilder sb, Node j) {
				jsonify(sb, j.toSerialize(j.hasIdentity()), internal);
			}
		}
	}

	/**
	 * Returns a JSON list string with compressed JSON object strings inside.
	 * Compression removed "!uuid" fields from non-identity nodes and represents
	 * cross-references with indices if the node being references is in the list
	 * returned. Nodes are returned in an order that guarantees that, if a node
	 * and its dependencies are both in the output, the node will appear after
	 * the nodes on which it depends.
	 * 
	 * @param nodes
	 *            The nodes to represent in a JSON list.
	 * @return A JSON list of JSON objects.
	 */
	public static String compressedJSON(Node... nodes) {
		StringBuilder sb = new StringBuilder();
		Map<UUID, Integer> indices = new TreeMap<UUID, Integer>();
		Arrays.sort(nodes, new Comparator<Node>() {
			public int compare(Node a, Node b) {
				int ah = a.getHeight();
				int bh = b.getHeight();
				if (ah != bh) return ah - bh;
				return a.compareTo(b);
			}
		});
		XRefer x = new XRefer.Compressor(indices);
		sb.append('[');
		boolean comma = false;
		for (Node node : nodes) {
			if (indices.containsKey(node.getUUID())) throw new IllegalArgumentException("Can't have node " + node.getUUID() + " more than once.");
			if (comma) sb.append("\n,");
			jsonify(sb, node, x);
			indices.put(node.getUUID(), indices.size());
			comma = true;
		}
		return sb.append("\n]").toString();
	}

	/**
	 * A convenience method for creating canonical JSON of primitives, Sets,
	 * Lists, Maps, UUIDs, and Nodes
	 * 
	 * @param sb
	 *            The StringBuilder into which to place the JSON.
	 * @param o
	 *            The object to JSONify. Nested Nodes are represented as UUID
	 *            strings.
	 * @param detail
	 *            If o is a Node, this describes how it will be represented.
	 */
	public static void jsonify(StringBuilder sb, Object o, XRefer detail) {
		if (o == null) {
			sb.append("null");
			return;
		}
		if (o instanceof Node) {
			Node j = (Node) o;
			detail.encode(sb, j);
		} else if (o instanceof UUID) {
			jsonify(sb, o.toString(), detail);
		} else if (o instanceof SortedSet<?>) {
			SortedSet<?> ss = (SortedSet<?>) o;
			sb.append('[');
			boolean needComma = false;
			for (Object element : ss) {
				if (needComma) sb.append(',');
				jsonify(sb, element, detail);
				needComma = true;
			}
			sb.append(']');
		} else if (o instanceof SortedMap<?, ?>) {
			SortedMap<?, ?> sm = (SortedMap<?, ?>) o;
			sb.append('{');
			boolean needComma = false;
			for (Object key : sm.keySet()) {
				if (needComma) sb.append(',');
				if (!(key instanceof CharSequence)) throw new JSONificationException("JSON map keys must be strings, not " + key.getClass());
				jsonify(sb, key, detail);
				sb.append(':');
				jsonify(sb, sm.get(key), detail);
				needComma = true;
			}
			sb.append('}');
		} else if (o instanceof List<?>) {
			List<?> l = (List<?>) o;
			sb.append('[');
			boolean needComma = false;
			for (Object element : l) {
				if (needComma) sb.append(',');
				jsonify(sb, element, detail);
				needComma = true;
			}
			sb.append(']');
		} else if (o instanceof Set<?>) {
			Set<?> s = (Set<?>) o;
			SortedSet<Object> ss = new TreeSet<Object>();
			ss.addAll(s);
			jsonify(sb, ss, detail);
		} else if (o instanceof Map<?, ?>) {
			Map<?, ?> m = (Map<?, ?>) o;
			SortedMap<Object, Object> sm = new TreeMap<Object, Object>();
			sm.putAll(m);
			jsonify(sb, sm, detail);
		} else if (o instanceof CharSequence) {
			CharSequence cs = (CharSequence) o;
			sb.append('"');
			for (int i = 0; i < cs.length(); i += 1) {
				char c = cs.charAt(i);
				if (c == '\n') sb.append("\\n");
				else if (c == '\r') sb.append("\\r");
				else if (c == '\t') sb.append("\\t");
				else if (c == '\f') sb.append("\\f");
				else if (c == '\b') sb.append("\\b");
				else if (c == '\\') sb.append("\\\\");
				else if (c == '"') sb.append("\\\"");
				else if (c < 0x20 || c == 0x7f) {
					sb.append("\\u");
					for (int shift = 12; shift >= 0; shift -= 4) {
						sb.append(Integer.toHexString((c >> shift) & 0xf));
					}
				} else {
					sb.append(c);
				}
			}
			sb.append('"');
		} else if (o instanceof Number) {
			Number n = (Number) o;
			if (n.longValue() == n.doubleValue()) sb.append(n.longValue()); // so 1.0 is written as 1 not 1.0
			else sb.append(n.doubleValue());
		} else if (o instanceof Boolean) {
			sb.append(o.toString());
		} else if (o instanceof Enum) {
			jsonify(sb, o.toString(), detail);
		} else {
			throw new JSONificationException("Cannot jsonify " + o.getClass() + " in a canonical way");
		}
	}

	/**
	 * Turns a JSON-returned object containing a Node and turns it back into a
	 * Node object. It does this via the special "!class" and "!uuid" fields of
	 * the input.
	 * 
	 * @param sm
	 *            The parsed JSON containing a single node,
	 * @param nodes
	 *            A place to look up nodes referenced by index or UUID.
	 * @return A Node subclass representation of the provided map.
	 * @throws JSONParser.MalformedJSONException
	 *             if the "!class" field doesn't match the data.
	 * @throws IllegalArgumentException
	 *             if the "!uuid" field is in error or the data is otherwise
	 *             corrupted or incomplete.
	 */
	public static Node fromJSON(SortedMap<String, Object> sm, NodeLookup nodes) {
		if (!sm.containsKey("!class")) throw new JSONParser.MalformedJSONException("Node JSON must contain key \"!class\"");
		Object cls = sm.get("!class");
		if (!(cls instanceof String)) throw new JSONParser.MalformedJSONException("key \"!class\" must have a String value");
		String scls = (String) cls;

		try {
			Class<?> c = Class.forName(Node.class.getCanonicalName().replace(Node.class.getSimpleName(), "nodes."+scls));
			if (!(Node.class.isAssignableFrom(c))) throw new JSONParser.MalformedJSONException("The class " + scls + " is not a polygenea node type");
			Constructor<?> maker = c.getConstructor(SortedMap.class, NodeLookup.class);
			Object o = maker.newInstance(sm, nodes);
			Node n = (Node) o;
			if (sm.containsKey("!uuid") && !n.getUUID().toString().equals(sm.get("!uuid"))) throw new IllegalArgumentException("JSON had " + sm.get("!uuid") + " but data suggested " + n.getUUID() + " instead");
			return n;
		} catch (ClassNotFoundException e) {
			throw new JSONParser.MalformedJSONException("The class " + scls + " is not known by this system", e);
		} catch (NoSuchMethodException e) {
			throw new JSONParser.MalformedJSONException("Every concrete subclass of Node, including " + scls + ", must have a constructor taking a SortedMap<String, Object> and a NodeLookup", e);
		} catch (InstantiationException e) {
			throw new JSONParser.MalformedJSONException(scls + " is abstract; you need a concrete class in the !class field", e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) throw (Error) e.getCause();
			if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
			throw new JSONParser.MalformedJSONException(scls + "'s constructor threw a checked exception", e.getCause());
		} catch (IllegalAccessException e) {
			throw new JSONParser.MalformedJSONException(scls + "'s constructor should be public", e);
		}
	}

	/**
	 * Turns a JSON-encoded String containing a single Node into a Node object.
	 * It does this via the special "!class" and "!uuid" fields of the input.
	 * 
	 * @param json
	 *            The json string to parse
	 * @param nodes
	 *            A place to look up nodes referenced by index or UUID.
	 * @return A Node subclass representation of the provided map.
	 * @throws JSONParser.MalformedJSONException
	 *             if the input is not valid JSON, is not a JSON object, or the
	 *             "!class" field doesn't match the data.
	 * @throws IllegalArgumentException
	 *             if the "!uuid" field is in error or the data is otherwise
	 *             corrupted or incomplete.
	 */
	public static Node fromJSON(String json, NodeLookup nodes) {
		Object bit = JSONParser.parse(json);
		if (!(bit instanceof SortedMap)) throw new JSONParser.MalformedJSONException("Node JSON must be an object, not a " + bit.getClass());
		@SuppressWarnings("unchecked")
		SortedMap<String, Object> sm = (SortedMap<String, Object>) bit;
		return fromJSON(sm, nodes);
	}

	protected Node() {
		try {
			this.getClass().getConstructor(SortedMap.class, NodeLookup.class);
		} catch (NoSuchMethodException ex) {
			throw new AssertionError("Every concrete subclass of Node, including " + this.getClass() + ", must have a public constructor taking a SortedMap<String, Object> and a NodeLookup");
		}
	}

	protected Node(UUID uuid) {
		try {
			this.getClass().getConstructor(SortedMap.class, NodeLookup.class);
		} catch (NoSuchMethodException ex) {
			throw new AssertionError("Every concrete subclass of Node, including " + this.getClass() + ", must have a constructor taking a SortedMap<String, Object> and a NodeLookup");
		}
		if (uuid.version() != 1 && uuid.version() != 4) {
			throw new IllegalArgumentException("You may only specify time-based (version 1) or random-based (version 4) UUIDs directly");
		}
		this.uuid = uuid;
	}

	/**
	 * Constructor used by JSON loading methods in Node and Database. Processes
	 * the "!class" and "!uuid" fields.
	 * 
	 * @param map
	 *            the JSON object to be turned into this object
	 */
	protected Node(SortedMap<String, Object> map) {
		if (!map.containsKey("!class")) throw new IllegalArgumentException("map lacks !class field");
		if (!this.getClass().getSimpleName().equals(map.get("!class"))) throw new IllegalArgumentException("!class of " + map.get("!class") + " and class of " + this.getClass().getSimpleName() + " do not agree");
		if (map.containsKey("!uuid")) {
			Object o = map.get("!uuid");
			if (!(o instanceof String)) throw new IllegalArgumentException("!uuid field must be a UUID String");
			String s = (String) o;
			this.uuid = UUID.fromString(s);
			if (this.hasIdentity() && uuid.version() != 1 && uuid.version() != 4) throw new IllegalArgumentException("!uuid field was version " + uuid.version() + " but should have been 1 or 4 for a " + this.getClass());
			if (!this.hasIdentity() && uuid.version() != 5) throw new IllegalArgumentException("!uuid field was version " + uuid.version() + " but should have been 5 for a " + this.getClass());
		}
	}

	/**
	 * @return a list of all nodes this Node references, transitively down to
	 *         nodes with no references. May repeat some Nodes.
	 */
	public Collection<Node> dependsOn() {
		return dependsOn(true);
	}

	/**
	 * @return a list of all nodes this Node references directly. May repeat
	 *         some Nodes.
	 */
	public Collection<Node> out() {
		return dependsOn(false);
	}

	private List<Node> dependsOn(boolean recur) {
		List<Node> list = new LinkedList<Node>();
		this.dependsOnHelper(list, recur);
		return list;
	}

	private void dependsOnHelper(List<Node> list, boolean recur) {
		SortedMap<String, Object> m = this.toSerialize(false);
		dependsOnHelper(list, m.values(), recur);
	}

	private static void dependsOnHelper(List<Node> list, Iterable<? extends Object> collection, boolean recur) {
		for (Object o : collection) {
			if (o instanceof Node) {
				if (recur) ((Node) o).dependsOnHelper(list, recur);
				list.add((Node) o);
			} else if (o instanceof Iterable<?>) {
				dependsOnHelper(list, (Iterable<?>) o, recur);
			} else if (o instanceof Map<?, ?>) {
				dependsOnHelper(list, ((Map<?, ?>) o).values(), recur);
			}
		}
	}

	/**
	 * Comparison is performed based on UUID only. This is important because it
	 * is part of how serialisation (and thus UUID computation) works.
	 */
	public int compareTo(Node that) {
		return this.getUUID().compareTo(that.getUUID());
	}

	/**
	 * Equality is performed based class and UUID only, to match compareTo.
	 * Assuming that UUIDs are actually unique, that is fine.
	 */
	public boolean equals(Object that) {
		if (that instanceof Node && this.getClass().equals(that.getClass())) {
			return this.getUUID().equals(((Node) that).getUUID());
		}
		return false;
	}

	/**
	 * Generated by the various string parsing methods if the input is not a DAG
	 * string or if it is not a stand-alone string and the parsing DAG doesn't
	 * have the needed nodes.
	 */
	// @formatter:off
	public static class JSONificationException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public JSONificationException() { super(); }
		public JSONificationException(String message) { super(message); }
		public JSONificationException(String message, Throwable causedBy) { super(message, causedBy); }
		public JSONificationException(Throwable causedBy) { super(causedBy); }
	}
	// @formatter:on

	/**
	 * Performs any validation that the node might need. The Node class just
	 * checks the UUID.
	 * <p>
	 * Do not override this method; override validate(StringBuilder) instead.
	 * 
	 * @param log
	 *            A place where descriptions of problems will be written. If the
	 *            node is valid, log is not used.
	 * @return true if the node is valid, false otherwise.
	 */
	public final boolean validate(Writer log) {
		StringBuilder sb = new StringBuilder();
		boolean answer = validate(sb);
		new PrintWriter(log).print(sb);
		return answer;
	}

	/**
	 * Performs any validation that the node might need. The Node class just
	 * checks the UUID.
	 * <p>
	 * Do not override this method; override validate(StringBuilder) instead.
	 * 
	 * @param log
	 *            A place where descriptions of problems will be written. If the
	 *            node is valid, log is not used.
	 * @return true if the node is valid, false otherwise.
	 */
	public final boolean validate(OutputStream log) {
		return this.validate(new PrintWriter(log));
	}

	/**
	 * Performs any validation that the node might need. The Node class just
	 * checks the UUID.
	 * <p>
	 * Do not override this method; override validate(StringBuilder) instead.
	 * 
	 * @return true if the node is valid, false otherwise.
	 */
	public final boolean validate() {
		StringBuilder sb = null;
		return this.validate(sb);
	}
}
