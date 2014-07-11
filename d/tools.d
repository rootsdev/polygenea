import std.algorithm;
import std.array;
import std.conv;
import std.range;
import std.uni;
import std.utf;
import std.variant;

/// Thrown if the input to a parsing method isn't in the accepted subset of JSON.
class JSONParsingException : Exception {
	this(string msg) { super(msg); }
}
/// Currently the JSONEncodingException is never thrown because anything that type-checks works.
class JSONEncodingException : Exception {
	this(string msg) { super(msg); }
}

private char toHexChar(size_t i) { return cast(char)(i <= 9 ? '0' + i : ('a'-10) + i); }
unittest {
	assert(toHexChar(0) == '0');
	assert(toHexChar(1) == '1');
	assert(toHexChar(2) == '2');
	assert(toHexChar(3) == '3');
	assert(toHexChar(4) == '4');
	assert(toHexChar(5) == '5');
	assert(toHexChar(6) == '6');
	assert(toHexChar(7) == '7');
	assert(toHexChar(8) == '8');
	assert(toHexChar(9) == '9');
	assert(toHexChar(0xa) == 'a');
	assert(toHexChar(0xb) == 'b');
	assert(toHexChar(0xc) == 'c');
	assert(toHexChar(0xd) == 'd');
	assert(toHexChar(0xe) == 'e');
	assert(toHexChar(0xf) == 'f');
}

private byte fromHexChar(dchar i) { 
	if (i >= '0' && i <= '9') return cast(byte)(i - '0');
	if (i >= 'a' && i <= 'f') return cast(byte)(i - ('a' - 0xa));
	if (i >= 'A' && i <= 'F') return cast(byte)(i - ('A' - 0xA));
	return -1;
}
unittest {
	assert(fromHexChar('0') == 0);
	assert(fromHexChar('1') == 1);
	assert(fromHexChar('2') == 2);
	assert(fromHexChar('3') == 3);
	assert(fromHexChar('4') == 4);
	assert(fromHexChar('5') == 5);
	assert(fromHexChar('6') == 6);
	assert(fromHexChar('7') == 7);
	assert(fromHexChar('8') == 8);
	assert(fromHexChar('9') == 9);
	assert(fromHexChar('a') == 10);
	assert(fromHexChar('b') == 11);
	assert(fromHexChar('c') == 12);
	assert(fromHexChar('d') == 13);
	assert(fromHexChar('e') == 14);
	assert(fromHexChar('f') == 15);
	assert(fromHexChar('A') == 10);
	assert(fromHexChar('B') == 11);
	assert(fromHexChar('C') == 12);
	assert(fromHexChar('D') == 13);
	assert(fromHexChar('E') == 14);
	assert(fromHexChar('F') == 15);
}

private void jsonify(Appender!string json, string value) {
	json.put('"');
	foreach (char c; value) switch(c) {
		case '"':  json.put("\\\""); break;
		case '\\': json.put("\\\\"); break;
		case '\b': json.put("\\b"); break;
		case '\f': json.put("\\f"); break;
		case '\n': json.put("\\n"); break;
		case '\r': json.put("\\r"); break;
		case '\t': json.put("\\t"); break;
		default:
			if (c < 0x20 || c == 0x7f) {
				json.put("\\u");
				for (int shift = 12; shift >= 0; shift -= 4) {
					json.put(toHexChar((c >> shift) & 0xf));
				}
			} else {
				json.put(c);
			}
	}
	json.put('"');
}
unittest {
	{ auto a = appender!string; jsonify(a, ""); assert(a.data == "\"\""); }
	{ auto a = appender!string; jsonify(a, "\\"); assert(a.data == "\"\\\\\""); }
	{ auto a = appender!string; jsonify(a, "\\\""); assert(a.data == "\"\\\\\\\"\""); }
	{ auto a = appender!string; jsonify(a, " "); assert(a.data == "\" \""); }
	{ auto a = appender!string; jsonify(a, "\n	☺𝄞Ø"); assert(a.data == "\"\\n\\t☺𝄞Ø\""); }
	{ auto a = appender!string; jsonify(a, "\0"); assert(a.data == "\"\\u0000\""); }
}
private void jsonify(Appender!string json, int value) { json.put(to!string(value)); }
private void jsonify(Appender!string json, bool value) { json.put(value ? "true" : "false"); }
private void jsonify(Appender!string json, JNode[] value) {
	json.put('[');
	foreach(i,v; value) {
		if(i != 0) json.put(',');
		v.addTo(json);
	}
	json.put(']'); 
}
unittest {
	{ auto a = appender!string; jsonify(a, [JNode("")]); assert(a.data == "[\"\"]"); }
	{ auto a = appender!string; jsonify(a, [JNode(11),JNode(45)]); assert(a.data == "[11,45]"); }
	{ auto a = appender!string; JNode[] x = []; jsonify(a, x); assert(a.data == "[]"); }
}
private void jsonify(Appender!string json, JNode[string] value) {
	json.put('{');
	string[] keys = value.keys;
	sort(keys);
	foreach(i, key; keys) {
		if(i != 0) json.put(',');
		jsonify(json, key);
		json.put(':');
		value[key].addTo(json);
	}
	json.put('}');
}
unittest {
	{ auto a = appender!string; jsonify(a, ["":JNode("")]); assert(a.data == "{\"\":\"\"}"); }
	{ auto a = appender!string; JNode[string] x; jsonify(a, x); assert(a.data == "{}"); }
	{ auto a = appender!string; jsonify(a, [
		"a":JNode(0),
		"c":JNode(0),
		"yahoo":JNode(0),
		"b":JNode(0),
		"x":JNode(0),
		"y":JNode(0),
		"z":JNode(0),
	]); assert(a.data == "{\"a\":0,\"b\":0,\"c\":0,\"x\":0,\"y\":0,\"yahoo\":0,\"z\":0}"); }
}




struct JNode {
	public Algebraic!(string, int, JNode[string], JNode[], bool, typeof(null)) data;
	alias data this;
	private void addTo(Appender!string json) {
		if (data.type == typeid(string)) {
			jsonify(json, data.get!string);
		} else if (data.type == typeid(JNode[string])) {
			jsonify(json, data.get!(JNode[string]));
		} else if (data.type == typeid(JNode[])) {
			jsonify(json, data.get!(JNode[]));
		} else if (data.type == typeid(int)) { 
			jsonify(json, data.get!int);
		} else if (data.type == typeid(bool)) { 
			jsonify(json, data.get!bool);
		} else if (data.type == typeid(null)) { 
			json.put("null");
		} else {
			throw new VariantException("Unknown type "~data.type.toString());
		}
	}
	public string toString() { auto ans = appender!string; this.addTo(ans); return ans.data; }
	JNode opAssign(T)(T rhs) if (!is(T == JNode)) { this.data = rhs; return this; }
	this(T)(T x) { this.data = x; }
}

/// Assumes the initial " has already been read
private string parseRestOfString(T)(ref T r) if (isInputRange!T) {
	dchar read() { dchar ans = r.front(); r.popFront(); return ans; }
	auto sb = appender!string;
	dchar c;
	while(!r.empty) {
		c = read;
		if (c == '"') break;
		else if (c == '\\') {
			c = read;
			if (c == 'u') {
				wchar n = 0;
				n |= fromHexChar(read) << 12;
				n |= fromHexChar(read) <<  8;
				n |= fromHexChar(read) <<  4;
				n |= fromHexChar(read) <<  0;
				try {
					sb.put(n);
				} catch(UTFException e) {
					if (read != '\\') throw new JSONParsingException("Escaped UTF-16 surrogate must be followed by another escaped UTF-16 surrogate");
					if (read != 'u') throw new JSONParsingException("Escaped UTF-16 surrogate must be followed by another escaped UTF-16");
					wchar n2 = 0;
					n2 |= fromHexChar(read) << 12;
					n2 |= fromHexChar(read) <<  8;
					n2 |= fromHexChar(read) <<  4;
					n2 |= fromHexChar(read) <<  0;
					try {
						sb.put([n,n2]);
					} catch(UTFException e) {
						throw new JSONParsingException("Escaped UTF-16 surrogate pair did not represent a unicode character");
					}
				}
			} else if (c == 'b') sb.put('\b');
			else if (c == 'f') sb.put('\f');
			else if (c == 'n') sb.put('\n');
			else if (c == 'r') sb.put('\r');
			else if (c == 't') sb.put('\t');
			else if (c == '"') sb.put('"');
			else sb.put(c);
		} else sb.put(c);
	}
	if (c != '"') throw new JSONParsingException("Strings must end with a '\"' character");
	return sb.data;
}

JNode parse(T)(T r) if (isInputRange!T) { return parse(r); } // calls the reference-based one
JNode parse(T)(ref T r) if (isInputRange!T) {
	if (r.empty) throw new JSONParsingException("Cannot parse an empty range");
	while(isWhite(r.front)) r.popFront();
	if (r.empty) throw new JSONParsingException("Cannot parse an empty range");

	dchar read() { 
		if (r.empty) throw new JSONParsingException("Input ended while still parsing");
		dchar ans = r.front(); 
		r.popFront(); return ans; 
	}
	dchar c;
	switch(c = read) {
		case '"': return JNode(parseRestOfString(r));
		case '{':
			JNode[string] obj;
			while(!r.empty && isWhite(r.front)) r.popFront();
			while (!r.empty && r.front != '}') {
				if (obj.length > 0 && read != ',') 
					throw new JSONParsingException("Missing ',' or '}' in object");
				while(!r.empty && isWhite(r.front)) r.popFront();
				if (r.empty || r.front != '"') throw new JSONParsingException("Object keys must start with a '\"'");
				r.popFront();
				string key = parseRestOfString(r);
				if (key.empty) throw new JSONParsingException("Object keys must not be empty");
				if (isWhite(key.front)) throw new JSONParsingException("Object keys must not start with whitespace");
				if (isControl(key.front)) throw new JSONParsingException("Object keys must not start with a control character");
				if (key in obj) throw new JSONParsingException("Object keys must be unique (repeated "~key~")");
				while(!r.empty && isWhite(r.front)) r.popFront();
				if (read != ':') throw new JSONParsingException("Missing ':' in object");
				obj[key] = parse(r);
				while(!r.empty && isWhite(r.front)) r.popFront();
			}
			if (r.empty) throw new JSONParsingException("Input ended while looking for ',' or ']' in list");
			r.popFront;
			return JNode(obj);
		case '[':
			JNode[] list;
			while(!r.empty && isWhite(r.front)) r.popFront();
			while (!r.empty && r.front != ']') {
				if (list.length > 0 && read != ',') 
					throw new JSONParsingException("Missing ',' or ']' in list");
				while(!r.empty && isWhite(r.front)) r.popFront();
				list ~= parse(r);
				while(!r.empty && isWhite(r.front)) r.popFront();
			}
			if (r.empty) throw new JSONParsingException("Input ended while looking for ',' or ']' in list");
			r.popFront;
			return JNode(list);
		case '-':
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			int ans = c == '-' ? 0 : c - '0';
			while(!r.empty && r.front >= '0' && r.front <= '9') {
				ans = ans*10 + (read - '0');
				if (ans < 0) throw new JSONParsingException("Parser can only handle numbers less than 2 billion");
			}
			return JNode(c == '-' ? -ans : ans);
		case 't':
			if ((c=read) != 'r') throw new JSONParsingException("No value begins t"~cast(string)[c]);
			if ((c=read) != 'u') throw new JSONParsingException("No value begins tr"~cast(string)[c]);
			if ((c=read) != 'e') throw new JSONParsingException("No value begins tru"~cast(string)[c]);
			return JNode(true);
		case 'f':
			if ((c=read) != 'a') throw new JSONParsingException("No value begins f"~cast(string)[c]);
			if ((c=read) != 'l') throw new JSONParsingException("No value begins fa"~cast(string)[c]);
			if ((c=read) != 's') throw new JSONParsingException("No value begins fal"~cast(string)[c]);
			if ((c=read) != 'e') throw new JSONParsingException("No value begins fals"~cast(string)[c]);
			return JNode(false);
		default:
			throw new JSONParsingException("Parser only handles strings, objects, lists, booleans, and integers, none of which beginning with "~to!string(c));
	}
}
unittest {
	import std.exception;
	// booleans
	assert(parse("true").get!bool == true);
	assert(parse("false").get!bool == false);
	assertThrown!JSONParsingException(parse("truth"));
	// integers
	assert(parse("0").get!int == 0);
	assert(parse("-0").get!int == 0);
	assert(parse("23").get!int == 23);
	assert(parse("-34").get!int == -34);
	assert(parse("2147483647").get!int == 2147483647);
	assertThrown!JSONParsingException(parse("2147483648"));
	// strings
	assert(parse("\"\"").get!string == "");
	assert(parse("\"\\ud834\\udd1e\"").get!string == "𝄞");
	assert(parse("\"𝄞\"").get!string == "𝄞");
	assert(parse("\"\\\"\\\\\\n\\r\\t\\b\\f\"").get!string == "\"\\\n\r\t\b\f");
	assert(parse("\"\\u0050\"").get!string == "P");
	assertThrown!JSONParsingException(parse("\""));
	assertThrown!JSONParsingException(parse("\"\\ud834\""));
	// lists
	assert(parse("[]").get!(JNode[]) == []);
	assert(parse("[1]").get!(JNode[]) == [JNode(1)]);
	assert(parse("[1,2]").get!(JNode[]) == [JNode(1),JNode(2)]);
	assert(parse(" [\n\t1 ,\r\n\r2\t\t]").get!(JNode[]) == [JNode(1),JNode(2)]);
	assert(parse("[[[0]]]").get!(JNode[]) == [JNode([JNode([JNode(0)])])]);
	assertThrown!JSONParsingException(parse("["));
	assertThrown!JSONParsingException(parse("[2 3]"));
	assertThrown!JSONParsingException(parse("[2, 3"));
	// objects
	assert(parse("{}").get!(JNode[string]) == null);
	assert(parse("{\"a\":0}").get!(JNode[string]) == ["a":JNode(0)]);
	assert(parse("{\"a\":0,\"A\":\"\"}").get!(JNode[string]) == ["a":JNode(0),"A":JNode("")]);
	assert(parse("\t\n{ \"a\"  : 0  ,  \"A\"  :  \"\"  }").get!(JNode[string]) == ["a":JNode(0),"A":JNode("")]);
	assertThrown!JSONParsingException(parse("{"));
	assertThrown!JSONParsingException(parse("{23:1}"));
	assertThrown!JSONParsingException(parse("{\" a\":0}"));
	assertThrown!JSONParsingException(parse("{\"a\":0,\"a\":\"\"}"));
	// nothing else
	assertThrown!JSONParsingException(parse("null"));
	assertThrown!JSONParsingException(parse("True"));
	assertThrown!JSONParsingException(parse(".3"));
	assertThrown!JSONParsingException(parse(""));
	assert(parse("5.8").type == typeid(int));
	assert(parse("5e-4").type == typeid(int));
}

version(none) {
	import std.stdio;
	void main() {
		foreach(wchar c ; "☺𝄞☹") writeln(cast(int)c);
		string s = "{\"\\ud834\\udd1e\":123, \"x\" : false }";
		writeln(parse(s));
	}
}
