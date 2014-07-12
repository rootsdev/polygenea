package org.rootsdev.polygenea;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class of static methods for parsing JSON into classes from java.util and java.lang.
 * Contains the ability to request lists be parsed as sets.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would
 *         consider it a courtesy if you cite me if you benefit from this code.
 */
public class JSONParser {
	
	/**
	 * Used to force lists to be sets. 
	 * I needed this at one point and then didn't again, but it's still here in case someone else needs it…
	 */
	public static enum ListOrSet {
		SET_NEVER,
		SET_IF_SORTED,
		SET_IF_UNIQUE,
		SET_ALWAYS
	};
	
	/**
	 * A comparator for sorting arbitrary JSON objects, used in forcing lists to be sets. 
	 * I needed this at one point and then didn't again, but it's still here in case someone else needs it…
	 * 
	 * The order is {@literal null} &lt; {@literal false} &lt; {@literal true} &lt; {@code Number} &lt; {@code String} &lt; {@code Collection} &lt; {@code Map}
	 */
	public static class JSONOrderer implements Comparator<Object> {
		@SuppressWarnings("unchecked") // because this compares generics
		public int compare(Object a, Object b) {
			// null < false < true < (numbers, sorted numerically) < (strings) < (collections) < (maps)
			if (a == b) return 0;
			if (a == null) return -1;
			if (b == null) return 1;
			if (a instanceof Boolean) {
				if (b instanceof Boolean) {
					boolean ab = (Boolean)a;
					boolean bb = (Boolean)b;
					if (ab == bb) return 0;
					if (bb) return -1;
					return 1;
				}
				return -1;
			}
			if (b instanceof Boolean) return 1;
			if (a instanceof Number) {
				if (b instanceof Number) {
					double ad = ((Number)a).doubleValue();
					double bd = ((Number)b).doubleValue();
					if (ad < bd) return -1;
					if (ad > bd) return 1;
					long al = ((Number)a).longValue();
					long bl = ((Number)b).longValue();
					if (al < bl) return -1;
					if (al > bl) return 1;
					return 0;
				}
				return -1;
			}
			if (b instanceof Number) return 1;
			if (a instanceof String) {
				if (b instanceof String) {
					return ((String)a).compareTo((String)b);
				}
				return -1;
			}
			if (b instanceof String) return 1;
			if (a instanceof Collection<?>) {
				if (b instanceof Collection<?>) {
					Iterator<?> ai = ((Collection<?>)a).iterator();
					Iterator<?> bi = ((Collection<?>)b).iterator();
					while (ai.hasNext() && bi.hasNext()) {
						int c = this.compare(ai.next(), bi.next());
						if (c != 0) return c;
					}
					if (ai.hasNext()) return 1;
					if (bi.hasNext()) return -1;
					return 0;
				}
				return -1;
			}
			if (b instanceof Collection<?>) return 1;
			if (a instanceof Map<?,?>) {
				if (b instanceof Map<?,?>) {
					// I can't seem to make Java like the cast here. Map<?,?> doesn't work, 
					// nor does removing the <?,?> on Entry, and Iterator<?> looses information…
					@SuppressWarnings("rawtypes")
					Iterator<Map.Entry<?,?>> ai = ((Map)a).entrySet().iterator();
					@SuppressWarnings("rawtypes")
					Iterator<Map.Entry<?,?>> bi = ((Map)b).entrySet().iterator();
					while (ai.hasNext() && bi.hasNext()) {
						Map.Entry<?, ?> ae = ai.next();
						Map.Entry<?, ?> be = bi.next();
						int c = this.compare(ae.getKey(), be.getKey());
						if (c != 0) return c;
						c = this.compare(ae.getValue(), be.getValue());
						if (c != 0) return c;
					}
					if (ai.hasNext()) return 1;
					if (bi.hasNext()) return -1;
					return 0;
				}
				return -1;
			}
			if (b instanceof Map<?,?>) return 1;
			throw new IllegalArgumentException("Cannot compare a "+a.getClass()+" to a "+b.getClass());
		}
	}
	
	/**
	 * The exception thrown by parsers in this class.
	 */
	public static class MalformedJSONException extends RuntimeException {
		private static final long serialVersionUID = 0L;
		public MalformedJSONException() { super(); }
		public MalformedJSONException(String message) { super(message); }
		public MalformedJSONException(String message, Throwable cause) { super(message, cause); }
		public MalformedJSONException(Throwable cause) { super(cause); }
	}
	
	/// Assumes the initial " has already been read
	private static String parseRestOfString(Reader r) {
		try {
			StringBuilder sb = new StringBuilder();
			int c = r.read();
			while (c > -1 && c != '"') {
				if (c == '\\') {
					c = r.read();
					if (c == 'b') sb.append('\b');
					else if (c == 'f') sb.append('\f');
					else if (c == 'n') sb.append('\n');
					else if (c == 'r') sb.append('\r');
					else if (c == 't') sb.append('\t');
					else if (c == '"') sb.append('"');
					else if (c == '\\') sb.append('\\');
					else if (c == 'u') {
						int n = 0;
						n |= Character.digit(r.read(), 16) << 12;
						n |= Character.digit(r.read(), 16) << 8;
						n |= Character.digit(r.read(), 16) << 4;
						n |= Character.digit(r.read(), 16);
						if (n <= -1) throw new MalformedJSONException("\\u escape sequence not followed by 4 hex digits");
						sb.append((char)n);
					} else {
						throw new MalformedJSONException("\\"+(char)c+", not a legal JSON escape sequence");
					}
				} else {
					sb.append((char)c);
				}
				c = r.read();
			}
			if (c == -1) throw new EOFException("Input ended inside a quoted string");
			return sb.toString();
		} catch (MalformedJSONException t) {
			throw t;
		} catch (Throwable t) {
			throw new MalformedJSONException(t.getMessage(), t);
		}
	}
	/// assumes one character (c) has already been read. Easier than using mark all the time.
	private static Object parseRest(int c, Reader r, ListOrSet policy) {
		try {
			while (c > -1 && Character.isWhitespace(c)) c = r.read();
			if (c <= -1) throw new EOFException("End of input reached");
			switch (c) {
			case '"': return parseRestOfString(r);
			case '{':
				Map<String, Object> obj = new TreeMap<String, Object>();
				c = r.read(); while (c > -1 && Character.isWhitespace(c)) c = r.read();
				while (c != '}') {
					if (c <= -1) throw new EOFException("End of input reached after a {");
					if (c == '"') {
						String key = parseRestOfString(r);
						if (obj.containsKey(key))
							throw new MalformedJSONException("Cannot put duplicate keys in an object");
						c = r.read(); while (c > -1 && Character.isWhitespace(c)) c = r.read();
						if (c != ':') throw new MalformedJSONException("Object keys must be followed by :value, not "+(char)c);
						Object value = parse(r, policy);
						obj.put(key, value);
						c = r.read();
						while (c > -1 && Character.isWhitespace(c)) c = r.read();
						if (c == '}') return obj;
						if (c != ',') throw new MalformedJSONException("Exepcted , or }");
						c = r.read();
						while (c > -1 && Character.isWhitespace(c)) c = r.read();
						if (c == '}') throw new MalformedJSONException("Trailing commas not allowed");
					} else {
						throw new MalformedJSONException("Object keys must be strings");
					}
				}
				return obj;
			case '[':
				List<Object> ans = new LinkedList<Object>();
				c = r.read(); while (c > -1 && Character.isWhitespace(c)) c = r.read();
				while (c != ']') {
					if (c <= -1) throw new EOFException("End of input reached after a {");
					Object value = parseRest(c,r, policy);
					ans.add(value);
					c = r.read();
					while (c > -1 && Character.isWhitespace(c)) c = r.read();
					if (c == ']') break;
					if (c != ',') throw new MalformedJSONException("Expected , or ]");
					c = r.read();
					while (c > -1 && Character.isWhitespace(c)) c = r.read();
					if (c == ']') throw new MalformedJSONException("Trailing commas not allowed");
				}
				if (policy != ListOrSet.SET_NEVER) {
					SortedSet<Object> set = new TreeSet<Object>(new JSONOrderer());
					set.addAll(ans);
					if (policy == ListOrSet.SET_ALWAYS) {
						if (set.size() == ans.size()) return set;
						throw new MalformedJSONException("List had duplicate entries but SET_ALWAYS specified");
					}
					else if (policy == ListOrSet.SET_IF_UNIQUE) {
						if (set.size() == ans.size()) return set;
						return ans;
					}
					else if (policy == ListOrSet.SET_IF_SORTED) {
						if (new JSONOrderer().compare(ans, set) == 0) return set;
						return ans;
					} else {
						throw new UnsupportedOperationException("Unknown policy "+policy);
					}
				}
				return ans;
			case 't':
				if ((c = r.read()) != 'r') throw new MalformedJSONException("Unknown keyword begining t"+(char)c);
				if ((c = r.read()) != 'u') throw new MalformedJSONException("Unknown keyword begining tr"+(char)c);
				if ((c = r.read()) != 'e') throw new MalformedJSONException("Unknown keyword begining tru"+(char)c);
				r.mark(1); c = r.read(); r.reset();
				if (Character.isLetterOrDigit(c))
					throw new IOException("Unknown keyword beginning true"+(char)c);
				return true;
			case 'f':
				if ((c = r.read()) != 'a') throw new MalformedJSONException("Unknown keyword begining f"+(char)c);
				if ((c = r.read()) != 'l') throw new MalformedJSONException("Unknown keyword begining fa"+(char)c);
				if ((c = r.read()) != 's') throw new MalformedJSONException("Unknown keyword begining fal"+(char)c);
				if ((c = r.read()) != 'e') throw new MalformedJSONException("Unknown keyword begining fals"+(char)c);
				r.mark(1); c = r.read(); r.reset();
				if (Character.isLetterOrDigit(c))
					throw new IOException("Unknown keyword beginning false"+(char)c);
				return false;
			case 'n':
				if ((c = r.read()) != 'u') throw new MalformedJSONException("Unknown keyword begining n"+(char)c);
				if ((c = r.read()) != 'l') throw new MalformedJSONException("Unknown keyword begining nu"+(char)c);
				if ((c = r.read()) != 'l') throw new MalformedJSONException("Unknown keyword begining nul"+(char)c);
				r.mark(1); c = r.read(); r.reset();
				if (Character.isLetterOrDigit(c))
					throw new IOException("Unknown keyword beginning null"+(char)c);
				return null;
			case '-':
			case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
				boolean isDouble = false;
				StringBuilder sb = new StringBuilder();
				if (c == '-') { 
					sb.append((char)c); 
					r.mark(1); c = r.read();
					if (!Character.isDigit(c))
						throw new NumberFormatException("JSON requires a digit follow a leading minus sign");
				}
				if (c == '0') { 
					sb.append((char)c); 
					r.mark(1); c = r.read();
					if (Character.isDigit(c)) throw new NumberFormatException("JSON requires numbers not start with a 0");
				}
				while(Character.isDigit(c) || c == 'e' || c == 'E' || c == '.') {
					if (c == 'e' || c == 'E') {
						isDouble = true;
						sb.append((char)c);
						r.mark(1); c = r.read();
						if (c == '+' || c == '-') {
							sb.append((char)c);
							r.mark(1); c = r.read();
						}
						if (!Character.isDigit(c))
							throw new MalformedJSONException("JSON requires a number follow an exponent");
					} else if (c == '.') {
						isDouble = true;
						sb.append((char)c);
						r.mark(1); c = r.read();
						if (!Character.isDigit(c))
							throw new MalformedJSONException("JSON requires a number follow an solidus");
					}
					sb.append((char)c);
					r.mark(1); c = r.read();
				}
				r.reset();
				if (isDouble) return Double.parseDouble(sb.toString());
				return Long.parseLong(sb.toString());
			default:
				throw new MalformedJSONException("JSON values cannot start with "+(char)c);
			}
		} catch (MalformedJSONException t) {
			throw t;
		} catch (Throwable t) {
			throw new MalformedJSONException(t.getMessage(), t);
		}
	}

	/**
	 * Like parse(Reader) except you can specify that some lists be returned as sorted sets instead
	 * @param r The reader to parse from
	 * @param policy Whether to convert all, most, some, or no lists into sorted sets
	 * @return A Map&lt;String, Object&gt;, List&lt;Object&gt;, SortedSet&lt;Object&gt;, String, Long, Boolean, Double, or null
	 */
	public static Object parse(Reader r, ListOrSet policy) {
		if (!r.markSupported()) r = new BufferedReader(r);
		try {
			return parseRest(r.read(), r, policy);
		} catch (IOException e) {
			throw new MalformedJSONException("getting first character of value", e);
		} 
	}
	/**
	 * Parses valid JSON and returns the resulting value.
	 * If the input is not valid JSON, advances the reader far enough to know that.
	 * 
	 * If the reader supports marks, the end position will be the character after 
	 * the first JSON value read, or the character at which invalidity was first noticed. 
	 * Otherwise, the end position may be arbitrarily far after that point as determined
	 * by the default BufferedReader's buffering strategy.
	 * 
	 * @param r The reader to parse from.
	 * @return A Map&lt;String, Object&gt;, List&lt;Object&gt;, String, Long, Boolean, Double, or null
	 * @throws MalformedJSONException if the input does not begin with a valid JSON value
	 */
	public static Object parse(Reader r) {
		return parse(r, ListOrSet.SET_NEVER);
	}
	public static Object parse(String s) {
		return parse(new StringReader(s)); 
	}
	public static Object parse(InputStream s) {
		return parse(new BufferedReader(new InputStreamReader(s))); 
	}
	public static Object parse(File s) throws FileNotFoundException {
		return parse(new BufferedReader(new FileReader(s))); 
	}
	public static Object parse(URL s) throws IOException {
		return parse(s.openStream()); 
	}
	public static Object parse(String s, ListOrSet policy) {
		return parse(new StringReader(s), policy); 
	}
	public static Object parse(InputStream s, ListOrSet policy) {
		return parse(new BufferedReader(new InputStreamReader(s)), policy); 
	}
	public static Object parse(File s, ListOrSet policy) throws FileNotFoundException {
		return parse(new BufferedReader(new FileReader(s)), policy); 
	}
	public static Object parse(URL s, ListOrSet policy) throws IOException {
		return parse(s.openStream(), policy); 
	}
}
