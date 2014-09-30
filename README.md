The data model has been simplified somewhat since July 2014 when the following README.md was written.
The README still describes the available code, 
but the current model is documented in the [docs folder](/rootsdev/polygenea/blob/master/docs/overview.md).


polygenea
=========

An effort to document a data model for collaborative genealogy.

Early work was summarized on [the wiki](https://github.com/rootsdev/polygenea/wiki).
That material no longer reflects the current status of the project.

Some other reference material can be found in 
[this slide deck](https://www.cs.virginia.edu/tychonievich/blog/media/DEV1349_Tychonievich_slides.pdf) from RootsTech 2014 
and [CFPS 4](http://fhiso.org/files/cfp/cfps4.pdf) from FHISO's call for papers.
FHISO's [call for papers](http://fhiso.org/call-for-papers-submissions/) has several other related papers as well.


Overview
========

Polygenea differs from most previous genealogy data models 
in that the structure of the data
neither mirrors the structure of a conclusion nor the structure of reality
but instead mimics the structure of research.
Small research decisions can have large structural impact on conclusions;
one goal of Polygenea is to ensure that all small decisions result in only small changes to the data.
Research also frequently results in incomplete, inconsistent, and contradictory ideas co-existing
in a single researcher's mind at a single time,
over time as researchers disagree with their past selves,
and between different researchers working on shared lines;
polygenea aims to be able to store these many genea simultaneously
without any duplication of data and without forcing a particular view of the data on anyone.


Immutability
------------

Polygenea stores research decisions in a set of nodes.
Each node, once created, is immutable.
Immutability gives many benefits in simplifying code and database implementations
and makes replication, consistency, and atomicity trivial to implement.
From a researchers' perspective, immutability means that nodes can refer to one another safely 
with perfect assurance that the nodes to which they refer will never change.

Because nodes are immutable, their references are acyclic.
Thus, the nodes and their references form a directed acyclic graph.

Effort has been made in polygenea's design to reduce the likelihood of updates by adhering to the principle of sensible disbelief.
If a node does need to be updated, this is done by creating a new, corrected node
and an "update-of" relationship between the new node and the old.
This model preserves immutability;
it also means that decisions to update nodes are themselves nodes contained in the data.


Principle of Sensible Disbelief
-------------------------------

The principle of sensible disbelief is a rule of thumb used in the design of the Polygenea data model.
It states that 

1. It should *not* be sensible to disbelieve just *part* of a node
2. It *should* be sensible to disbelieve a node *without* disbelieving the nodes to which it refers

This principle helps to keep the data model focussed on research (i.e., belief).
It keeps nodes simple, as each one should contain only one atomic assertion.
And it reduces the likelihood that data is inconsistent because each node stands on its own.



The Nodes
=========

A *Citation* node describes something external to the data, such as a document or conversation.
Unlike other nodes, Citation nodes have no constraints on the fields they may contain.

A *Claim* node is either a *Thing* node, a *Property* node, a *Connection* node, a *Grouping* node, or a *Match* node.

Note: the distinction between Claim nodes and other node types has been removed from the most recent [docs](/rootsdev/polygenea/blob/master/docs/overview.md).


A *Connection* node represents a directed relationship between two other nodes. Each contains
- "source", a reference to a *Source*
- "from", a reference to a *Claim*
- "to", a reference to a *Claim*
- "relation", a string containing the relationship between the claims

An *ExternalSource* node describes a source of information external to the data. Each contains
- "citation", a reference to a *Citation* 
- "content", a string containing a digitisation of the cited source
- "contentType", a string describing how to interpret the content as outlined in [RFC 2045](http://www.ietf.org/rfc/rfc2045.txt) and [2046](http://www.ietf.org/rfc/rfc2046.txt)

Note: the ExternalSource node has been renamed the Digitisation node in the most recent [docs](/rootsdev/polygenea/blob/master/docs/overview.md).


A *Grouping* node represents an undirected relationship between two or more other nodes. Each contains
- "source", a reference to a *Source*
- "subjects", a set (not list) of references to a *Claim* nodes
- "relation", a string containing the relationship between the subjects

Note: the Grouping node has been removed from the most recent [docs](/rootsdev/polygenea/blob/master/docs/overview.md).

An *Inference* node describes a source of information internal to the data. Each contains
- "antecedents", a list (not set) of references to *Claim* nodes
- "rule", a reference to an *InferenceRule*

An *InferenceRule* node describes how to derive new claims based on existing claims. Each contains
- "antecedents", a description of what claims are needed for this rule to apply
- "consequents", a description of what claims can be derived if the rule applies

A *Match* node asserts that two or more *Thing* nodes refer to the same real-world thing. Each contains
- "source", a reference to a *Source* (almost always an *Inference*)
- "same", a set (not list) of references to a *Thing* nodes

A *Note* node describes the action of a user of the system providing some non-conclusion-oriented information. Each contains
- "user", a string
- "date", a string

Note: the Note node has been removed from the most recent [docs](/rootsdev/polygenea/blob/master/docs/overview.md).

A *Property* node describes some attribute of another node. Each contains
- "source", a reference to a *Source*
- "subject", a reference to a *Claim*
- "key", a string describing what kind of information this property provides
- "value", a string containing the information itself

A *Source* node is either an *Inference* node, an *ExternalSource* node, or a *Note* node.

A *Thing* node describes the claim a source makes that "something exists". Thing nodes have identity: that is, Thing nodes with identical fields may be distinct. Each Thing node contains
- "source", a reference to a *Source*


Connection nodes
----------------

Connection nodes proved surprisingly difficult to define.
There are clearly many directed labelled properties possible,
but it was surprisingly hard to define which end is the "from" and which the "to".
The current model is

- If the relation is a transitive verb, like "loves" or "contains", from is the subject and to the direct object of the verb.

- Otherwise there should be some sentence of the form "to is (clause containing relation and from)"

It is not clear to me if this model is sufficient for all connections we might wish to add.


InferenceRule nodes
-------------------

Both of the fields of an InferenceRule node are lists of partially-specified nodes.
Each partially-specified node has a unique *index*, being the zero-based index of the node in the virtual list formed by concatenating (antecedents, consequents).
Each partially-specified node satisfies the following:
- All node references are by index; the index must be smaller than the index of the node making the reference.
- Nodes in the antecedent list may omit any or all fields.
- Nodes in the consequent list are fully specified *except* they have no "source" field (and the uuid is not specified).

These simple rules allow for the representation of a wide variety of fixed-arity inference rules.
Variable-arity rules that I have considered thus far can be realised by a fixed-arity rule coupled with one or more transitivity rules such as

	[{"!class":"Thing"}
	,{"!class":"Thing"}
	,{"!class":"Thing"}
	,{"!class":"Connection","from":0,"relation":"descendant","to":1}
	,{"!class":"Connection","from":1,"relation":"descendant","to":2}
	] => [{"!class":"Connection","from":0,"relation":"descendant","to":2}]

Transitivity rules of this sort provide as much expressive power as the Kleene plus, albeit without the ability to make anonymous chains.

Greater flexibility is needed in specifying rules.
For example, it would be nice to be able to say things like "node1.date is before node3.date" or "node1.name = node0.name" or "result.name = concatenate(node0.value," ",node1.value)". 
Work in designing such flexibility is ongoing.

By definition, a *Match* node acts like a *Thing* node, and any node that points to any node which the *Match* node matches is considered to point to the *Match* node as well.

Inference rules are not currently unique; that is, the exact same logic can be represented as several rule instances.
To be unique a well-defined canonical ordering of antecedents and consequents is needed.
I anticipate adding such an ordering in the future.



UUIDs and Content-based Hashing
===============================

Each node in Polygenea is given a unique identifier.
This simplifies communication between different databases
and makes for an unambiguous means of serializing references between nodes.
The current version of Polygenea uses UUIDs as outlined in [RFC 4122](http://www.ietf.org/rfc/rfc4122.txt).

Most nodes have UUIDs that are based on a content-based hashing using the SHA-1 algorithm,
meaning they are version 5 UUIDs under the RFC 4122 standard.
Content-based hashing is possible because nodes are immutable.
Using content-based UUIDs removes some problems associated with the repeated generation of duplicate nodes.

Some node types (currently only *Thing* nodes) are said to "have identity".
These nodes refer to things that may be distinct even if they have identical fields.
For example, a source with the phrase "four siblings" 
asserts the existence of four Thing nodes without any distinction between them.
Nodes with identity use a non-content-based UUID, either version 1 or version 4.

There is some extremely small possibility of UUID collisions using either version 4 or version 5 UUIDs. 
Polygenea does not handle that possibility in any way.


Canonical JSON for Hashing
--------------------------

Content-based hashing means there must be a single unambiguous byte sequence for every node.
In the current version of Polygenea this is provided through a canonical JSON serialisation.
Note that this canonical byte sequence does not mean that Polygenea nodes need to be transferred or stored in JSON format,
only that they must be able to be serialized that way for the purpose of generating UUIDs.

Each node is encoded as a JSON object.
The encoding satisfies [the JSON specification](http://json.org)
and has the following additional constraints:
- All keys of an object are unique
- All keys of an object are sorted lexicographically by unicode code point
- The data is converted into bytes using UTF-8
- No white space outside of strings
- Strings use escape sequences only for `"`, `\`, and ASCII control codes. Two-character escapes are used instead of six-character escapes where possible.

Sets are encoded as sorted lists.

References are encoded as the UUID of the referenced node.
UUIDs are encoded as strings in canonical form with lower-case hex digits.

These constraints define a single byte stream for any node.
The stream thus created can be parsed by any JSON parser, but should probably be created
but custom code because JSON serialisers do not, in general, adhere to the constraints given above (particularly the sorted keys constraint).


Clarifying UUID version 5
-------------------------

RFC 4122 is ambiguous when describing how 160-bit SHA-1 hashes
are to be reduced into the 122 free bits of a type-5 UUID.
Polygenea uses the same assumptions as the existing software libraries that I surveyed:
the first 128 bits are used, with the version and variant bits replacing the appropriate six of those bits in place.


