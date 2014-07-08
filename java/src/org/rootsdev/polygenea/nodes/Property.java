package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Property represents a single putative fact we might encounter or derive
 * about some other claim. Each is represented as a key:value pair, along with
 * the source of the fact and the claim it is describing.
 * <p>
 * Properties can be used to represent many ideas, including "data" like
 * "age:17" and "as-of:2014-07-05" and "metadata" like "confidence:73%". At
 * present, there is no control over the key or value used in Polygenea;
 * presumably at some point a set of canonical keys and value representations
 * will be added. Even once added, though, the ability to add other custom keys
 * will remain so that we can encode whatever claims a source might make, even
 * potentially ludicrous claims that we will almost certainly never standardise
 * such as "kindness:42".
 * <p>
 * Many Properties are implicit in sources; for example, when I see a source
 * claim that John and Jane were married the names (John and Jane) are explicit
 * but the fact that they are humans is implicit. A purist might argue that all
 * implicit claims should have as their source an Inference, not an
 * ExternalSource, but that can be carried to needless extremes. At which level
 * of implicitness we switch from using the ExternalSource directly to using an
 * Inference is not, at present, defined.
 * <p>
 * Many Properties could be expressed by making their value a Thing and their
 * key a Connection. For example, is "occurred:2014-07-05" a property, or is
 * "2014-07-05" a date-type Thing and "occurred-at" a Connection? Because Thing
 * nodes store no data, Property nodes will bee needed to store it somewhere so
 * a Thing+Connection representation should only be used if the Thing might
 * sensibly have other Properties or Connections added to it.
 * <p>
 * Another class of Property vs. Connection distinctions might create many
 * additional nodes. For example, the single Property node "status:Widdowed"
 * could be represented as three Thing nodes (two events (a marriage and a
 * death) and another person (the dead spouse)) with a variety of
 * "participated-in" type of Connection nodes. This type of expansion should be
 * handled by recording the data the say the Source presents it and using
 * Inference nodes to source more expanded views.
 * 
 * @see Note
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
public class Property extends Claim {

	/** The node to which the property applies. */
	public final Claim subject;
	/** What feature of that node is being described. */
	public final String key;
	/** The details of that feature for that node. */
	public final String value;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public Property(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map, lookup);
		this.subject = (Claim) lookup.lookup(map.get("subject"));
		this.key = (String) map.get("key");
		this.value = (String) map.get("value");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * @param source How we know this Property exists
	 * @param subject What Claim (usually a Thing) this Property describes
	 * @param key What kind of attribute of the subject we are providing
	 * @param value The details provided
	 */
	public Property(Source source, Claim subject, String key, String value) {
		super(source);
		this.subject = subject;
		this.key = key;
		this.value = value;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (subject == null) {
			log.append("subject should not be null");
			ok = false;
		}
		if (key == null || key.length() == 0) {
			log.append("key should not be null or empty");
			ok = false;
		}
		if (value == null || value.length() == 0) {
			log.append("value should not be null or empty");
			ok = false;
		}
		return ok;
	}
}
