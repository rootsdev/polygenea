package org.rootsdev.polygenea.nodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * An Inference is a source that is derived through reasoning (represented by an
 * InferenceRule) that is supported by other Claim nodes, rather than an
 * external witness.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public class Inference extends Source {

	/** The set of Claims that, taken together, support this inference. */
	public final Collection<Claim> antecedents;
	/** The rule that tells us what those claims imply, or {@literal null} if the rule is not transferable to other situations. */
	public final InferenceRule rule;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Inference(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		Collection<Claim> backing;
		if (map.containsKey("rule")) {
			this.rule = (InferenceRule) lookup.lookup(map.get("rule"));
			backing = new LinkedList<Claim>();
		} else {
			this.rule = null;
			backing = new TreeSet<Claim>();
		}
		for (Object o : (Iterable<?>) map.get("subjects")) {
			Claim c = (Claim) lookup.lookup(o);
			if (this.rule == null && backing.contains(c)) throw new IllegalArgumentException("duplicate claim " + c.getUUID());
			backing.add(c);
		}
		if (backing instanceof List) {
			this.antecedents = Collections.unmodifiableList((List<Claim>) backing);
		} else {
			this.antecedents = Collections.unmodifiableSortedSet((SortedSet<Claim>) backing);
		}
		this.selfCheck();
	}
	/** Constructor used by code that wishes to create new objects 
	 * @param rule The underlying rule. May be null if this is not an instance of a more general rule.
	 * @param antecedents The Claim nodes that are sufficient to support this infernece 
	 */
	public Inference(InferenceRule rule, Claim... antecedents) {
		super();
		this.rule = rule;
		if (rule != null) {
			this.antecedents = Collections.unmodifiableList(Arrays.asList(antecedents));
		} else {
			SortedSet<Claim> backing = new TreeSet<Claim>();
			for (Claim c : antecedents) {
				if (backing.contains(c)) throw new IllegalArgumentException("duplicate claim " + c.getUUID());
				backing.add(c);
			}
			this.antecedents = Collections.unmodifiableSortedSet(backing);
		}
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (antecedents.size() < 1) {
			log.append("cannot have an inference with no antecedents");
			ok = false;
		}
		for (Claim c : antecedents)
			if (c == null) {
				log.append("no antecedent should not be null");
				ok = false;
			}
		// rule may be null...
		return ok;
	}
}
