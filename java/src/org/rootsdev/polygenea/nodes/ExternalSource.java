package org.rootsdev.polygenea.nodes;

import java.util.SortedMap;

import org.rootsdev.polygenea.JSONParser;
import org.rootsdev.polygenea.NodeLookup;

/**
 * ExternalSource is the most commonly discussed type of Source, originating
 * from the real world. The other kind of Source is Inference, which originates
 * from logic.
 * <p>
 * Each ExternalSource wraps a Citation that indicates its real-world existence
 * and adds to that Citation some kind of extract, digitisation, or summary of
 * the information contained in the source.
 * <p>
 * I am might rename this class {@code Input} instead of {@code ExternalSource}.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class ExternalSource extends Source {

	/** Where this Source came from in the real world. */
	public final Citation citation;
	/** What this Source contains (a summary, digitisation, extract, etc). */
	public final String content;
	/** How the computer should interpret the content field's contents. See W3C's RFC 2045 and 2046. */
	public final String contentType;

	/** Constructor used by JSON loading methods in Node and Database 
	 * @param map A JSON object of this node
	 * @param lookup How to resolve node references into Node objects 
	 * @throws JSONParser.MalformedJSONException if the data is not proper JSON
	 * @throws IllegalArgumentException if JSON is not a Node or list of Nodes.
	 */
	public ExternalSource(SortedMap<String, Object> map, NodeLookup lookup) {
		super(map);
		this.citation = (Citation) lookup.lookup(map.get("citation"));
		this.content = (String) map.get("content");
		this.contentType = (String) map.get("contentType");
		this.selfCheck();
	}

	/** Constructor used by code that wishes to create new objects; uses the text/plain content type 
	 * @param citation The underlying citation
	 * @param text A textual summary, description, or transcript of the cited material
	 */
	public ExternalSource(Citation citation, String text) {
		this(citation, text, "text/plain");
	}

	/**
	 * Constructor used by code that wishes to create new objects
	 * @param citation A description of where this source came from
	 * @param contents A digitisation of the source of a part thereof
	 * @param contentType The content type of the contents
	 */
	public ExternalSource(Citation citation, String contents, String contentType) {
		super();
		this.citation = citation;
		this.content = contents;
		this.contentType = contentType;
		this.selfCheck();
	}

	@Override
	public boolean validate(StringBuilder log) {
		boolean ok = super.validate(log);
		if (citation == null) {
			log.append("Citation should not be null");
			ok = false;
		}
		// To do: check for content/content type agreement?
		return ok;
	}
}
