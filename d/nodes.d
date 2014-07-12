/**
 * This file matches the 2014-07-11 java prototype closely, but is written much 
 * more briefly because of the more succinct nature of D. In general, the 
 * javadocs there apply to the same classes here.
 */

import tools;
import std.uuid;
import std.functional;
import std.string;
import std.algorithm;
import std.container;

enum UUID polygeneaNamespace = parseUUID("954aac7d-47b2-5975-9a80-37eeed186527");

abstract class Node {
	private UUID uuid;
	public UUID getUUID() {
		if (uuid.empty) {
			if (this.hasIdentity) uuid = randomUUID();
			else uuid = sha1UUID(this.hashable().toString(), polygeneaNamespace);
		}
		return uuid;
	}
	private int height = -1;
	public int getHeight() {
		if (height == -1) {
			height = 0;
			foreach(n ; pointsTo) {
				int h = n.getHeight();
				if (h >= height) height = h + 1;
			}
		}
		return this.height;
	}
	public int getHeight() const {
		if (height == -1) {
			int h2 = 0;
			foreach(n ; pointsTo) {
				int h = n.getHeight();
				if (h >= h2) h2 = h + 1;
			}
			return h2;
		}
		return height;
	}
	
	public UUID getUUID() const {
		assert(uuid != UUID.init, "To call getUUID on a const Node, called in on a non-const version first");
		return uuid;
	}
	bool hasIdentity() { return false; }
	
	JNode[string] asJSON(bool withUUID, JNode delegate(Node) lookup) {
		JNode[string] data;
		data["!class"] = JNode(this.classinfo.name[this.classinfo.name.lastIndexOf('.')+1..$]);
		if (withUUID)
			data["!uuid"] = JNode(this.getUUID.toString);
		return data;
	}
	
	abstract const(Node)[] pointsTo() const;
	
	final JNode[string] asJSON(bool withUUID, JNode function(Node) lookup) { return asJSON(withUUID, toDelegate(lookup));}
	
	public final JNode hashable() { return JNode(asJSON(false, (Node n) => JNode(n.getUUID().toString()))); }
	public final JNode compress(JNode function(Node) lookup) { return JNode(asJSON(this.hasIdentity(), lookup)); }
	public final JNode compress(JNode delegate(Node) lookup) { return JNode(asJSON(this.hasIdentity(), lookup)); }
	
	this() {}
	this(JNode[string] data, Node function(JNode) lookup) {
		if (hasIdentity) this.uuid = parseUUID(data["!uuid"].get!string);
	}
	this(JNode[string] data, Node delegate(JNode) lookup) {	
		if (hasIdentity) this.uuid = parseUUID(data["!uuid"].get!string);
	}
	
	override public string toString() {
		return JNode(asJSON(true, (Node n) => JNode(n.getUUID().toString()))).toString();
	}
	
	override int opCmp(Object o) {
		Node n = cast(Node)o;
		if (n) return getUUID.opCmp(n.getUUID);
		return super.opCmp(o);
	}
	override bool opEquals(Object o) {
		Node n = cast(Node)o;
		if (n) return getUUID.opEquals(n.getUUID);
		return super.opEquals(o);
	}
	override size_t toHash() {
		return uuid.toHash();
	}
	
}



private mixin template NodeFields(T, string b, R...) { mixin("private T "~b~";"); mixin NodeFields!(R); }
private mixin template NodeFields() {}

private template NodeAsJSONLine(T, string b, R...) { 
	static if (is(T : Node)) enum NodeAsJSONLine = "\n\t\tif (this."~b~" !is null) data[\""~b~"\"] = lookup("~b~");" ~ NodeAsJSONLine!(R); 
	else static if (is(T : B[], B : Node)) enum NodeAsJSONLine = "\n\t\tif (this."~b~" !is null) { JNode[] tmp; foreach(v; "~b~") if (v !is null) tmp ~= lookup(v); data[\""~b~"\"] = tmp; }" 
	~ NodeAsJSONLine!(R); 
	else enum NodeAsJSONLine = "\n\t\tif (this."~b~" !is null) data[\""~b~"\"] = JNode("~b~");" ~ NodeAsJSONLine!(R); 
}
private template NodeAsJSONLine() { enum NodeAsJSONLine = ""; }

private template NodeConstructorParam(T, string b, R...) { enum NodeConstructorParam = T.stringof~" "~b~", "~NodeConstructorParam!(R); }
private template NodeConstructorParam() { enum NodeConstructorParam = "";}
private template NodeConstructorLine(T, string b, R...) { 
	static if (is(T : B[], B : Node)) enum NodeConstructorLine = "sort("~b~"); this."~b~" = "~b~";"~NodeConstructorLine!(R); 
	else enum NodeConstructorLine = "this."~b~" = "~b~";"~NodeConstructorLine!(R); 
}
private template NodeConstructorLine() { enum NodeConstructorLine = "";}

private template NodeJNodeConstructorLine(T, string b, R...) { 
	static if (is(T : Node)) enum NodeJNodeConstructorLine = "\n\t\tif (\""~b~"\" in data) this."~b~" = cast("~T.stringof~")lookup(data[\""~b~"\"]);" ~ NodeJNodeConstructorLine!(R); 
	else static if (is(T : B[], B : Node)) enum NodeJNodeConstructorLine = "\n\t\tif (\""~b~"\" in data) { "~T.stringof~" tmp; foreach(v; data[\""~b~"\"].get!(JNode[])) if (v.type != typeid(null)) tmp ~= cast("~B.stringof~")lookup(v); sort(tmp); this."~b~" = tmp; }" 
	~ NodeJNodeConstructorLine!(R); 
	else enum NodeJNodeConstructorLine = "\n\t\tif (\""~b~"\" in data) this."~b~" = data[\""~b~"\"].get!("~T.stringof ~");" ~ NodeJNodeConstructorLine!(R); 
}
private template NodeJNodeConstructorLine() { enum NodeJNodeConstructorLine = ""; }

private template JustOddValues(T, string x, R...) { enum JustOddValues = x~", "~JustOddValues!(R); }
private template JustOddValues() { enum JustOddValues = ""; }

private template JustNodes(string list, T, string x, R...) { 
	static if (is(T : Node)) enum JustNodes = "\n\t\tif ("~x~" !is null) "~list~" ~= "~x~";" ~ JustNodes!(list, R); 
	else static if (is(T : B[], B : Node)) enum JustNodes = "\n\t\tforeach(v ; "~x~") if (v !is null) "~list~" ~= v;" ~ JustNodes!(list, R); 
	else enum JustNodes = JustNodes!(list, R); 
}
private template JustNodes(string list) { enum JustNodes = ""; }


/**
 * The single line
 * ------
 * class MyFunkyNode : Node { mixin NodeBody!(0, Node, "master", string, "balance", string, "ray"); }
 * ------
 * defines a full Node class, complete with to- and from-JNode methods and a
 * constructor this(Node, string, string) that handles nulls. If a type is an
 * array of some Node subclass, it is stored sorted with no duplicates. 
 * 
 * The first template parameter is the number of these fields that are inherited.
 * Even if you inherit fields, list them.
 */
mixin template NodeBody(int supers, R...) {
	mixin NodeFields!(R[supers*2..$]);
	override JNode[string] asJSON(bool withUUID, JNode delegate(Node) lookup) {
		JNode[string] data = super.asJSON(withUUID, lookup);
		mixin(NodeAsJSONLine!(R[2*supers..$])); 
		return data;
	}
	mixin("this("~NodeConstructorParam!(R)~") { super("~JustOddValues!(R[0..supers*2])~"); "~NodeConstructorLine!(R[supers*2..$])~"}"); 
	this(JNode[string] data, Node function(JNode) lookup) { this(data, toDelegate(lookup)); }
	this(JNode[string] data, Node delegate(JNode) lookup) {
		super(data, lookup);
		mixin(NodeJNodeConstructorLine!(R[supers*2..$]));
	}
	override const(Node)[] pointsTo() const{
		const(Node)[] ans = [];
		mixin(JustNodes!("ans", R));
		return ans;
	}
}


class Note : Node { mixin NodeBody!(0, Node, "about", string, "content", string, "creator"); }

class InferenceRule : Node { mixin NodeBody!(0, JNode[], "antecedents", JNode[], "consequents"); }

class Citation : Node { 
	private JNode[string] details;
	override JNode[string] asJSON(bool withUUID, JNode delegate(Node) lookup) {
		JNode[string] data = super.asJSON(withUUID, lookup);
		foreach(k, v ; details) data[k] = v;
		return data;
	}
	this(string[string] parts) {
		foreach(k, v; parts) {
			details[k] = JNode(v);
		}
	}
	this(JNode[string] data, Node function(JNode) lookup) { this(data, toDelegate(lookup)); }
	this(JNode[string] data, Node delegate(JNode) lookup) {
		foreach(k, v ; data)
			if (k[0] != '!') details[k] = v;
	}
	override const(Node)[] pointsTo() const { return []; }
}

abstract class Source : Node { mixin NodeBody!(0); }
class ExternalSource : Source { mixin NodeBody!(0, Citation, "citation", string, "content", string, "contentType"); }
class Inference : Source { mixin NodeBody!(0, InferenceRule, "rule", Claim[], "antecedents"); }

abstract class Claim : Node { mixin NodeBody!(0, Source, "source"); }
class Thing : Claim { override public bool hasIdentity() { return true; } mixin NodeBody!(1, Source, "source"); }
class Match : Thing { 
	override public bool hasIdentity() { return false; } mixin NodeBody!(1, Source, "source", Thing[], "same"); 
}
class Property : Claim { mixin NodeBody!(1, Source, "source", Claim, "subject", string, "key", string, "value"); }
class Connection : Claim { mixin NodeBody!(1, Source, "source", Claim, "from", string, "relation", Claim, "to"); }
class Grouping : Claim { mixin NodeBody!(1, Source, "source", Claim[], "subjects", string, "relation"); }



template ParseCase(T, R...) { enum ParseCase = "\n\t\t\t\tcase \""~T.stringof~"\": ans ~= add(new "~T.stringof~"(obj, &nodeLookup)); break;" ~ ParseCase!R; }
template ParseCase() { enum ParseCase = ""; }

class Database {
	alias NodeSet = RedBlackTree!Node;
	Node[UUID] nodes;
	NodeSet[const(Node)] incoming;
	T add(T)(auto ref T node) {
		auto key = node.getUUID();
		if (key !in nodes) {
			nodes[key] = node;
			incoming[node] = new NodeSet();
			foreach(n ; node.pointsTo) {
				incoming[n].insert(node);
			}
		}
		return cast(T)nodes[key];
	}
	override public string toString() {
		Node[] all = nodes.values();
		sort!("a.getHeight() < b.getHeight()")(all);
		int[Node] backref;
		string ans = "";
		string lead = "[";
		foreach(Node n ; all) {
			if (n in backref) continue;
			ans ~= lead;
			ans ~= n.compress(
				(Node n2) => (n2 in backref ? JNode(backref[n2]) : JNode(n2.getUUID.toString()))
			).toString();
			backref[n] = backref.length;
			lead = "\n,";
		}
		if (lead == "[") ans ~= "[]";
		else ans ~= "\n]";
		return ans;
	}
	Node[] fromJSON(string json) { return fromJSON(parse(json)); }
	Node[] fromJSON(JNode objects) { 
		if (objects.type == typeid(JNode[])) return fromJSON(objects.get!(JNode[]));
		else if (objects.type == typeid(JNode[string])) return fromJSON([objects.get!(JNode[string])]);
		else throw new NodeConsistencyException("Can't parse a "~objects.type.toString);
	}
	Node[] fromJSON(JNode[] objects...) { 
		JNode[string][] tmp = new JNode[string][objects.length];
		foreach(i, v ; objects) tmp[i] = v.get!(JNode[string]);
		return fromJSON(tmp); 
	}
	Node[] fromJSON(JNode[string][] objects...) {
		Node[] ans = [];
		Node nodeLookup(JNode d) {
			if (d.type == typeid(int)) {
				int i = d.get!int;
				if (i < 0 || i >= ans.length) throw new NodeConsistencyException(d.toString~" too big or too small to be an index");
				return ans[i];
			} else if (d.type == typeid(string)) {
				try {
					UUID u = parseUUID(d.get!string);
					if (u in nodes) {
						return nodes[u];
					} else {
						throw new NodeConsistencyException("Unknown UUID: "~u.toString);
					}
				} catch (UUIDParsingException e) {
					throw new NodeConsistencyException(d.toString~" is not a valid UUID string");
				}
			} else {
				throw new NodeConsistencyException("A "~d.type.toString~" cannot be a valid Node index");
			}
		}
		foreach (obj ; objects) {
			if ("!uuid" in obj) {
				UUID u = parseUUID(obj["!uuid"].get!string);
				if (u in nodes) {
					ans ~= nodes[u];
					continue;
				}
			}
			if ("!class" !in obj) throw new NodeConsistencyException("No !class field");
			switch(obj["!class"].get!string) {
				mixin(ParseCase!(Citation, ExternalSource, Inference, InferenceRule, Thing, Property, Grouping, Connection, Note));
				default: throw new NodeConsistencyException("Unknown class "~obj["!class"].get!string);
			}
		}
		return ans;
	}
}
/**
 * An exception that is thrown if a node is given inconsistent data
 */
class NodeConsistencyException : Exception {
	this(string msg) { super(msg); }
}



import std.stdio;
void main() {
	Database d = new Database();
	
	Citation c = d.add(new Citation(["author":"Luther","title":"quick and dirty test case"]));
	ExternalSource s = d.add(new ExternalSource(c, "James is glad that he and his father are friends", "text/plain"));
	Thing james = d.add(new Thing(s));
	Thing father = d.add(new Thing(s));
	Property name = d.add(new Property(s, james, "name", "James"));
	Connection conn = d.add(new Connection(s, james, "father", father));
	Grouping friends = d.add(new Grouping(s, [james, father], "friends"));
	Connection conn2 = d.add(new Connection(s, james, "glad", friends));
	
	writeln(d);
	
	Database d2 = new Database();
	d2.fromJSON(d.toString);
	writeln(d2);
}
