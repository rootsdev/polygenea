package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * A Note node represents a Source that comes neither from some ExternalSource
 * nor from Inference but instead from a user of the system. Users may also
 * create ExternalSource nodes directly to represent, e.g., their personal
 * knowledge of various events related to their own lives. A Note node instead
 * represents informative but unessential information that might be of interest
 * to other users of the system.
 * <p>
 * The term “note” is used to apply to Note nodes and transitively to any other
 * node that references a note. All of the following would be represented as
 * notes:
 * <ul>
 * <li>The Property node “I had trouble reading this; you might want to verify
 * it.”
 * <li>The Connection node “This node supersedes that one”, with a Property node
 * “because that one has a spelling error” attached to the Connection node.
 * <li>The Grouping node “Are these all the same person or not?”
 * </ul>
 * I am unaware of any reason that a Thing node would be a note, but it is not
 * prevented by the data model.
 * <p>
 * Because the data is stored in other nodes, all the Note node contains is the
 * creator of the data (that is, its source).
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class Note extends Source {

	/** Who created this Note. */
	public final String user;
	/** When this Note was created. */
	public final String date;

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
	public Note(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.user = (String) map.get("user");
		this.date = (String) map.get("date");
		this.selfCheck();
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * 
	 * @param user
	 *            A description of who created this comment
	 * @param date
	 *            A date string of when the comment was created
	 */
	public Note(String user, String date) {
		super();
		this.user = user;
		this.date = date;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (user == null || user.length() == 0) {
			log.append("user should not be null\n");
			ok = false;
		}
		if (date == null || date.length() == 0) {
			log.append("date should not be null\n");
			ok = false;
		}
		// To do: check for date formatting?
		return ok;
	}
}
