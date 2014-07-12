package org.rootsdev.polygenea;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.rootsdev.polygenea.nodes.Match;
import org.rootsdev.polygenea.nodes.Thing;

/**
 * The Database class represents a set of Node objects and provides indexed
 * performance for UUID-based node lookups and incoming edge queries.
 * <p>
 * Eventually, this class is intended to become a full disk-backed database;
 * right now it primarily serves as a NodeLookup implementation.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class Database implements NodeLookup, Iterable<Node> {
	private Map<UUID, Node> all = new TreeMap<UUID, Node>();
	private Map<Node, Set<Node>> incoming = new TreeMap<Node, Set<Node>>();

	/**
	 * The number of nodes in the database.
	 * 
	 * @return The number of nodes in the database.
	 */
	public int size() {
		return all.size();
	}

	/**
	 * All of the nodes in this database in some arbitrary order.
	 * 
	 * @return A collection of nodes in the database.
	 */
	public Collection<Node> asCollection() {
		return all.values();
	}

	/**
	 * All of the nodes in this database in an order such that all any node
	 * referenced by node <var>X</var> appears in the list before <var>X</var>
	 * itself appears. The result is guaranteed not to contain duplicates.
	 * 
	 * @return A list of nodes in the database in an order that can be directly
	 *         serialised.
	 */
	public List<Node> asSerializableCollection() {
		List<Node> ans = new LinkedList<Node>();
		for (Node n : all.values()) {
			if (ans.contains(n)) continue;
			Collection<Node> before = n.dependsOn();
			for (Node n2 : before) {
				if (ans.contains(n2)) continue;
				ans.add(n2);
			}
			ans.add(n);
		}
		return ans;
	}

	public Iterator<Node> iterator() {
		return all.values().iterator();
	}

	/**
	 * This method is more efficient than lookup, but it performs no error
	 * checking and may result in undefined behaviour if the provided nodes
	 * contains references to nodes that have not been added to this Database.
	 * 
	 * @param nodes
	 *            Node(s) to add to the database.
	 */
	public void add(Node... nodes) {
		for (Node n : nodes) {
			if (!all.containsKey(n.getUUID())) all.put(n.getUUID(), n);
		}
		for (Node n : nodes) {
			n = all.get(n.getUUID());
			for (Node n2 : n.out()) {
				if (!all.containsKey(n2.getUUID())) throw new UnsupportedOperationException("Cannot add a node that refers to nodes you haven't added");
				if (!incoming.containsKey(n2)) incoming.put(n2, new TreeSet<Node>());
				incoming.get(n2).add(n);
			}
		}
	}

	public Node lookup(Object o) {
		if (o == null) return null;
		if (o instanceof Node) {
			Node n = (Node) o;
			if (!all.containsKey(n.getUUID())) this.addJSON(n.toSerializeWithDependencies(true));
			n = this.all.get(n.getUUID());
			for (Node n2 : n.out()) {
				assert all.containsKey(n2.getUUID()) : "Somehow addJSON failed to add prerequisite nodes?";
				if (!incoming.containsKey(n2)) incoming.put(n2, new TreeSet<Node>());
				incoming.get(n2).add(n);
			}
			return n;
		} else if (o instanceof UUID) {
			UUID u = (UUID) o;
			if (!all.containsKey(u)) throw new IllegalArgumentException("Node " + u + " is not in this database");
			return this.all.get(u);
		} else if (o instanceof String) {
			String s = (String) o;
			UUID u = null;
			try {
				u = UUID.fromString(s);
			} catch (IllegalArgumentException ex) {}
			if (u != null) return this.lookup(u);
			try {
				Node n = Node.fromJSON(s, this);
				return this.lookup(n);
			} catch (Throwable ex) {
				throw new IllegalArgumentException("The provided string " + s + " is not a JSON node or a UUID\n\t" + s);
			}
		} else {
			throw new IllegalArgumentException("This database can only process JSON and UUID nodes, not " + o.getClass());
		}
	}

	/**
	 * Returns a collection of the nodes that {code n} points to. Just wraps the
	 * Node class's out method right now.
	 * 
	 * @param n
	 *            The node in question
	 * @return A collection of nodes that {@code n} references.
	 */
	public Collection<Node> out(Node n) {
		return n.out();
	}

	/**
	 * Returns a collection of the nodes that point to {code n}.
	 * 
	 * @param n
	 *            The node in question
	 * @return A collection of nodes that reference {@code n}.
	 */
	public Collection<Node> in(Node n) {
		if (n instanceof Match) {
			SortedSet<Node> ans = new TreeSet<Node>();
			if (incoming.containsKey(n)) ans.addAll(incoming.get(n));
			for (Thing t : ((Match) n).same)
				ans.addAll(this.in(t));
			return ans;
		} else {
			if (incoming.containsKey(n)) return incoming.get(n);
			else {
				@SuppressWarnings("unchecked")
				List<Node> ans = (List<Node>) Collections.EMPTY_LIST;
				return ans;
			}
		}
	}

	/**
	 * parses the given JSON and adds all of its nodes to this database
	 * 
	 * @param json
	 *            A JSON-encoded String of either a Map or a List of Maps.
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(String json) {
		this.addJSON(JSONParser.parse(json));
	}

	/**
	 * parses the given JSON and adds all of its nodes to this database
	 * 
	 * @param json
	 *            A JSON-encoded File containing either a Map or a List of Maps.
	 * @throws FileNotFoundException
	 *             if the file is not found
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(File json) throws FileNotFoundException {
		this.addJSON(JSONParser.parse(json));
	}

	/**
	 * parses the given JSON and adds all of its nodes to this database
	 * 
	 * @param json
	 *            A JSON-encoded resource containing either a Map or a List of
	 *            Maps.
	 * @throws IOException
	 *             if there is an error accessing the URL
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(URL json) throws IOException {
		this.addJSON(JSONParser.parse(json));
	}

	/**
	 * parses the given JSON and adds all of its nodes to this database
	 * 
	 * @param json
	 *            A JSON-encoded character stream containing either a Map or a
	 *            List of Maps.
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(Reader json) {
		this.addJSON(JSONParser.parse(json));
	}

	/**
	 * parses the given JSON and adds all of its nodes to this database
	 * 
	 * @param json
	 *            A JSON-encoded character stream containing either a Map or a
	 *            List of Maps.
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(InputStream json) {
		this.addJSON(JSONParser.parse(json));
	}

	/**
	 * Parses the given JSON and adds all of its nodes to this database.
	 * 
	 * @param json
	 *            Either a SortedMap&lt;String, Object&gt; parseable by
	 *            Node.fromJSON or a List&lt;SortedMap&lt;String, Object&gt;&gt;
	 *            of such objects.
	 * @throws JSONParser.MalformedJSONException
	 *             if the data is not valid JSON
	 * @throws IllegalArgumentException
	 *             if JSON is not a Node or list of Nodes.
	 */
	public void addJSON(Object json) {
		if (json instanceof SortedMap<?, ?>) {
			@SuppressWarnings("unchecked")
			SortedMap<String, Object> sm = (SortedMap<String, Object>) json;
			if (sm.containsKey("!uuid")) {
				Object uuid = sm.get("!uuid");
				if (uuid instanceof String) {
					UUID u = UUID.fromString((String) uuid);
					if (this.all.containsKey(u)) return;// this.all.get(u);
				}
			}
			Node n = Node.fromJSON(sm, this);
			this.add(n);
		} else if (json instanceof SortedSet<?> || json instanceof List<?>) {
			Collection<?> i = (Collection<?>) json;
			ArrayList<Node> list = new ArrayList<Node>(i.size());
			NodeLookup context = new UseList(this, list);
			for (Object o : i) {
				if (!(o instanceof SortedMap)) throw new IllegalArgumentException("Expected a JSON object, not a " + o.getClass());
				@SuppressWarnings("unchecked")
				SortedMap<String, Object> sm = (SortedMap<String, Object>) o;
				if (sm.containsKey("!uuid")) {
					Object uuid = sm.get("!uuid");
					if (uuid instanceof String) {
						UUID u = UUID.fromString((String) uuid);
						if (this.all.containsKey(u)) {
							list.add(this.all.get(u));
							continue;
						}
					}
				}
				Node n = Node.fromJSON(sm, context);
				this.add(n);
				list.add(n);
			}
		} else {
			throw new IllegalArgumentException("Expected a parsed JSON object or map, not a " + json.getClass());
		}
	}

	/**
	 * All of the nodes in this database serialised using Node.compressedJSON
	 */
	public String toString() {
		Node[] nodes = new Node[this.all.size()];
		nodes = this.all.values().toArray(nodes);
		return Node.compressedJSON(nodes);
	}

	private static class UseList implements NodeLookup {
		Database base;
		List<Node> list;

		UseList(Database base, List<Node> list) {
			this.base = base;
			this.list = list;
		}

		public Node lookup(Object o) {
			if (o instanceof Number) {
				Number n = (Number) o;
				if (n.doubleValue() == n.intValue()) {
					int i = n.intValue();
					if (i >= 0 && i < list.size()) return list.get(i);
				}
				throw new IllegalArgumentException("Node indices must be positive integers smaller than the current position in the list.");
			}
			return base.lookup(o);
		}
	}
}
