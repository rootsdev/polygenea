Guiding Principles
==================

1.	Sensible disbelief
	
	*	It should be sensible to disbelieve any node. 
		
		For example, the name "John Doe" should probably *not* be a node in itself because it is not sensible to disbelieve a name without the context of the entity the name describes.
	
	*	It should *not* be sensible to disbelieve just *part* of a node.
		
		For example, the date and location of a birth should probably *not* be part of the same node because it is sensible to disbelieve one without disbelieving the other

	*	Anything that is is sensible to disbelieve should be a node.
		
		See the principle "There is only one source" for an example of this.

2.	There is always a source
	
	Nothing comes from nothing.
	It might come from a document, a story a now-dead relative used to tell,
	some complicated logic based on dozens of antecedent facts,
	or even a personal hunch, but it came from something.

3.	There is only one source
	
	If you want to say "Based on X, Y, and Z" then X, Y, and Z are antecedents to an inference
	and that inference is the source.
	
	The existence of inferences as data is vital to maintaining the principle of Sensible Disbelief.
	It is sensible to disbelieve the inference without disbelieving any of its antecedent claims:
	"Yes, it is raining and rain makes things wet but that doesn't imply that I am wet".
	It is separately possible to disbelieve a claim sourced to an inference without disbelieving the inference: "Yes, he was named Sue and the name Sue in that culture does imply being female, but he wasn't female".
	
4.	Edits beget edit-wars
	
	All data, once created, is immutable.
	An update to a datum is really the creation of a new datum 
	coupled with a new connecting datum 
	that indicates that the new datum is preferred by its creator over the old one.

5.	Duplicates are a UI problem
	
	If five sources all claim the same thing, there are five claims.
	Merging them in the data is incorrect, violating the principle of sensible disbelief
	because I might not believe that one of the sources actually makes this claim.
	
	If the user interface wants to present a collection of similar-content nodes
	as if they were all one node, that is fine; but the *data* must keep them separate.


The Nodes
=========

Claims
------

There are only three kinds of core claims:

**Thing** node
:	Represents the claim "something existed."
	
	Because *anything* about the claim could be sensibly disbelieved,
	the only content of a Thing node is a unique identifier and a reference to the source asserting that the thing exists.

**Property** node
:	Attaches a datum to another node.
	The datum should not be sensible to disbelieve in isolation of the node to which is applies.
	
	The content of a Property node is a key/value pair
	and references to its source and the node to which the property applies.

**Connection** node
:	Attaches two nodes together.

	The content of a Connection node is a description of the relationship between the nodes
	and a reference to its source and the pair of nodes it connects.

There is also a fourth kind of quasi-claim node:

**Match** node
:	Represents the decision "these two Thing nodes represent the same real-world entity."

	The content of a Match node is a reference to its source and the pair of nodes it "merges".

	A Match acts in every way as a Thing node.
	Every node referring to either matched thing by definition is also treated as referring to the Match node.

Match nodes are similar to connections and could be modelled as connections
but are kept separate because of their acts-like-a-Thing character.
Properties and Connections that point to a Connection refer to the relationship the Connection describes;
Properties and Connections that point to a Match refer to the aggregate Thing the Match describes.
	
	
### Comments are Properties

Any property whose key begins `meta-` and any connection whose label begins `meta-`
is interpreted as expressing meta-level information about the data,
such as comments, assertions of likelihood, or other metadata.

### "Optional" Sources

It is a guiding principle of Polygenea that there is always a source.
However, the source link in each node is optional; a missing source represents the following meaning:

*	A missing source link in a Thing node, as well as non-meta Property nodes and Connection nodes, means "The source is unknown or was not recorded."

*	A missing source link in a meta-type node means "The contributor chose to remain anonymous."
	
*	A missing source link in a Match node, means "The source is an inference based on the quantity of similar Property and Connection nodes pointing to the matched Thing nodes."



Sourcing
--------

There are at three kinds of source nodes.

**Citation** node
:	A reference pointing to something external to the digital data.
	
	At present, my expectation is that these nodes are a more-or-less unconstrained sets of key-value pairs.
	A lot of work is needed to standardise these,
	including at least a set of "preferred" or "standard" keys,
	a discipline for translingual activities,
	and probably a set of formatting guides for at least some classes of values.

**Digitisation** node
:	An in-data document being referenced.
	May be derivative, like a transcript or image of an external document;
	or may be original, a user-testified source entered directly by the user.
	
	The content of a digitisation blob
	is a media-type field
	and either a content-hash based pointer to a local file
	or an internal byte stream.
	
**Inference** node
:	A representation of a particular application of reasoning.
	
	The content of an Inference node is a reference to the Rule node of which this inference is an instance and a set of node references that match to the Rule's antecedents.
	
	If there is no Rule reference, the rule is assumed to be a "one-off" rule:
	the specific support nodes referenced 
	imply the nodes that are sourced to the Inference and nothing else.

The Inference node depends on the Rule node.
This is an optional dependency: it is possible to represent all inferences with a missing rule reference.
Some of the reasons for including rules are:

*	The Principle of Sensible Disbelief.
	
	A rule node can separating the difference between "I don't believe that birthing a child means having been born at least 14 years earlier" and "I don't believe that that trend applies in the case of Sarah Elias's birth"

*	Research guides.
	
	Many of the ideas that are present in existing research guides
	can be encoded as rules.
	These include naming conventions in a particular era and region,
	rules-of-thumb for where the next record might exist,
	probable match hints,
	etc.
	
*	Research validation.
	
	If we have a rule as a first-class element of the data we can write code that uses them.
	For example, we could mine how often it holds in trusted histories
	and use that to give feedback like "This rule holds only 93% of the time,
	but you have applied it to 154 out of the 155 times it could apply in your research (99.4%).
	That means that on average 10 of the times you applied it are probably incorrect."


**Rule** node
:	A representation of a trend or rule.
	
	The contents of a Rule node is simply two lists of other nodes.
	The first of these is the list of *antecedents* of the rule, which specifies under what conditions the rule applies;
	the second is the list of *consequents* of the rule, which specifies what may be concluded from the antecedents.
	
	The antecedents nodes may be under-specified in order to specify generality.
	For example, an antecedent `Property(key="name", value="Jno")` would match any property with that key and value no matter what node it discussed or what source it came from.
	
	Both antecedents and consequents may only use *local* references to other nodes.
	These references are expressed as 0-based integer indexes.
	Thus the first antecedent has local index 0, the second 1, the third 2, and so on.
	If there are *n* nodes in the antecedent list
	then the first consequent has local index *n*, the second has *n* + 1, and so on.
	All references in a node with local index *i* must be between 0 and *i* &minus; 1, inclusive.
	
	There are multiple possible Rule nodes for each meaning of the rule.
	I may at some point design add additional constraints that make Rule form canonical
	by forcing only a single ordering of antecedents and consequents.
	
	I anticipate adding other syntax later that will increase expressiveness of rules.
	One example might be regular expressions in antecedent values
	and back-references to the matches of those expressions in other nodes.


Serialisation
=============

Polygenea is trivial to store.
Because it is write-once, there are no updates or edits to serialise.
Because it is a directed acyclic graph, there are not cyclic dependencies to resolve.
Because the graph is generally quite shallow with a small branching factor
there isn't even much to worry about as far as scaling and caching goes.
It fits easily in a relational database (with the possible exception of Citation nodes),
in a NoSQL database, 
or in just about any other data store desired.
Even simple append-only files can store the data correctly if not in an efficient way for querying later.

I present in this section several possible JSON serialisations of a Polygenea data set,
all representing the nodes that can be generated from the extremely small source string "Tom's father".
They differ only in how they reference other nodes.

## JSON Serialisation Commonalities

I represent a node as a JSON object with special keys beginning with a `!`.
I sort the keys lexicographically, use no internal whitespace, and use as few and as short escape sequences in strings and JSON allows;
these conventions make the JSON version of any node uniquely defined,
allowing it to be used as a proxy for the node
in hashing or equality comparison.

For example,

`{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}`



## Local Index

In the local index style, nodes are stored in a list and references are indexes into the list.
This assumes that all the nodes that a node references are in the list with the node.
Because references are acyclic, there exists an ordering of the nodes
such that all indexes are to an earlier part of the list, enabling single-pass processing.

	[{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}
	,{"!class":"Thing","id":"KWE-2E7","source":0}
	,{"!class":"Property","key":"name","of":1,"source":0,"value":"Tom"}
	,{"!class":"Thing","id":"EV8-3WQ","source":0}
	,{"!class":"Connection","label":"father","of":1,"source":0,"target":3}
	]

Local index form is simple and compact;
however, when importing one list into another
all of the references need to be re-computed
based on both whether a similar node already exists and where the new nodes are being placed.
A simple implementation of local index merging is *O((m+n)n)* 
where *n* and *m* are the sizes of the lists of nodes being merged.


## UUID

In the UUID style, the contents of each node are hashed to create a UUID version 5.
Those UUIDs are used as references.
Thus, for example, the UUID version 5 made from the node

	{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}

using namespace 954aac7d-47b2-5975-9a80-37eeed186527 (the hash of "polygenea" in the null namespace) 
is ddf929fb-68a8-5118-9358-6fd462f7d7de.
Thus, the first Thing node looks like

	{"!class":"Thing","id":"KWE-2E7","source":"ddf929fb-68a8-5118-9358-6fd462f7d7de"}

That node's UUID version 5 is then 13f9e182-4037-52df-ad82-123ee94252e2,
which would be used in the name property, and so on.

Note that my current reference implementations use UUID version 4 for the id of Thing nodes
and then use that id instead of a hash-based UUID in references.
This is just an implementation detail and not one I am convinced was wise.

## Inline Contents

Because nodes are immutable and references are acyclic, we can use the entire body of a node as it's reference.	
The idea of value-as-reference is verbose, but is suggested by some NoSQL databases as an efficiency boost.
I present it below with line breaks for clarity

	{"!class":"Digitisation"
	,"content-type":"text/plain"
	,"contents":"Tom's father"
	}
	
	{"!class":"Thing",
	"id":"KWE-2E7",
	"source":
		{"!class":"Digitisation"
		,"content-type":"text/plain"
		,"contents":"Tom's father"
		}
	}
	
	{"!class":"Property"
	,"key":"name"
	,"of":{"!class":"Thing",
		"id":"KWE-2E7",
		"source":
			{"!class":"Digitisation"
			,"content-type":"text/plain"
			,"contents":"Tom's father"
			}
		}
	,"source":
		{"!class":"Digitisation"
		,"content-type":"text/plain"
		,"contents":"Tom's father"
		}
	,"value":"Tom"
	}

… and so on.


## Aside: Inferences

It might be tempting to add in more details to the preceding example:

	[{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}
	,{"!class":"Thing","id":"KWE-2E7","source":0}
	,{"!class":"Property","key":"name","of":1,"source":0,"value":"Tom"}
	,{"!class":"Thing","id":"EV8-3WQ","source":0}
	,{"!class":"Connection","label":"father","of":1,"source":0,"target":3}
	,{"!class":"Property","key":"type","of":1,"source":0,"value":"person"}
	,{"!class":"Property","key":"type","of":3,"source":0,"value":"person"}
	,{"!class":"Property","key":"type","of":4,"source":0,"value":"biological"}
	]

but the last three Property nodes in that example are incorrectly sourced: they are inferred, not asserted by the source text.
More correct would be

	[{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}
	,{"!class":"Thing","id":"KWE-2E7","source":0}
	,{"!class":"Property","key":"name","of":1,"source":0,"value":"Tom"}
	,{"!class":"Thing","id":"EV8-3WQ","source":0}
	,{"!class":"Connection","label":"father","of":1,"source":0,"target":3}
	,{"!class":"Inference","antecedents":[1,3,4]}
	,{"!class":"Property","key":"type","of":1,"source":5,"value":"person"}
	,{"!class":"Property","key":"type","of":3,"source":5,"value":"person"}
	,{"!class":"Property","key":"type","of":4,"source":5,"value":"biological"}
	]

or even

	[{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}
	,{"!class":"Thing","id":"KWE-2E7","source":0}
	,{"!class":"Property","key":"name","of":1,"source":0,"value":"Tom"}
	,{"!class":"Thing","id":"EV8-3WQ","source":0}
	,{"!class":"Connection","label":"father","of":1,"source":0,"target":3}
	,{ "!class":"Rule"
	 , "antecedents":
	   [ {"!class":"Thing"}
	   , {"!class":"Thing"}
	   , {"!class":"Connection","label":"father","of":1,"target":0}
	   ]
	 , "consequents":
	   [ {"!class":"Property","key":"type","of":0,"value":"person"}
	   , {"!class":"Property","key":"type","of":1,"value":"person"}
	   , {"!class":"Property","key":"type","of":2,"value":"biological"}
	   ]
	 }
	,{"!class":"Inference","antecedents":[3,1,4],"rule":5}
	,{"!class":"Property","key":"type","of":1,"source":6,"value":"person"}
	,{"!class":"Property","key":"type","of":3,"source":6,"value":"person"}
	,{"!class":"Property","key":"type","of":4,"source":6,"value":"biological"}
	]

based on the "rule" that most father references refer to the biological father of a human.


## Alternatives to JSON

I've so-far presented JSON:

	{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}

	,{ "!class":"Rule"
	 , "antecedents":
	   [ {"!class":"Thing"}
	   , {"!class":"Thing"}
	   , {"!class":"Connection","label":"father","of":1,"target":0}
	   ]
	 , "consequents":
	   [ {"!class":"Property","key":"type","of":0,"value":"person"}
	   , {"!class":"Property","key":"type","of":1,"value":"person"}
	   , {"!class":"Property","key":"type","of":2,"value":"biological"}
	   ]
	 }


XML is slightly more compact than JSON for these nodes because it has fewer quotes and doesn't need a special "!class" field:

	<Digitisation content-type="text/plain" contents="Tom's father"/>

	<Rule>
		<antecedents>
			<Thing/>
			<Thing/>
			<Connection label="father" of="1" target="0"/>
		</antecedents>
		<consequents>
			<Property key="type" of="0" value:"person"/>
			<Property key="type" of="1" value:"person"/>
			<Property key="type" of="2" value:"biological"/>
		</consequents>
	</Rule>


We can also use the more compact constructor-style notation for most nodes:
	
	Digitisation("text/plain", "Tom's father")
	
	Rule([Thing(null,null)
	     ,Thing(null,null)
	     ,Connection("father",1,null,0)
	     ]
	    ,[Property("type",0,"person")
	     ,Property("type",1,"person")
	     ,Property("type",2,"biological")
	     ]
	    )

The constructor syntax does not lend itself to the open-ended contents of a Citation node: the keys cannot be inferred from position.
However, Python-like constructor syntax would work at almost exactly the same space requirements as XML:

	Citation(author="Tychonievich, Luther A", title="Polygenea")

By design, each of the eight node types starts with a unique letter, allowing further compression; missing elements can also be implied with commas alone for even more compression:

	D("text/plain","Tom's father")

	R([T(,)
	  ,T(,)
	  ,C("father",1,,0)
	  ]
	 ,[P("type",0,"person")
	  ,P("type",1,"person")
	  ,P("type",2,"biological")
	  ]
	 )

Comparing these different approaches for these example nodes:

Format            | Rule Bytes | Digitisation Bytes
------------------|------------|--------------------
JSON              |  327       |  79
XML               |  265       |  65
Constructor       |  161       |  41
Small Constructor |  100       |  30


Even more compact binary formats using 3-bit type codes and length-encoding instead of delimiters could reduce this even further if needed.


Use Cases
=========

Following are a few use cases that Polygenea makes straightforward
but are more difficult under other common data models,
as well as some that are not first-class in Polygenea but are nonetheless easily implemented.


## Fixing a Mistake

If a node is in error, you simply

1.	Add another node that is not in error

2.	Add a Connection node noting the new node is an "update-of" the old one

The error gets corrected and the fact that the error existed and was corrected gets recorded
without any chance of stepping on the toes of another researcher.


## Distributed Research

Because nodes are immutable, there is no danger of incompatible edits.
If a hundred users and databases each have a copy of some nodes
and each add new nodes to the mix,
all that is needed to re-synchronize the various copies
is to send one another the new nodes.

The only possible challenge is in ensuring that the id fields of independently-created Thing nodes
are unique.
I consider this a minor challenge at best 
because many solutions to the unique identifier problem are known,
including UUID versions 1 and 4 among others.


## Privacy and Data Ownership

Private data is easily handled: it's just a set of nodes that do not get shared with others.

Because data is immutable, sharing it with others does not risk it being changed in any way.
Thus, most of the importance of data ownership is moot.
If you wish to keep a copy of some data, doing so does not hurt anyone else.

Paywall data is a bit more difficult.
It can easily be tagged with a Property with a key of `meta-paywall-owner`
so that tools will know it was paywalled, but keeping someone 
from spreading it once they have it is not feasible.

I usually assume that each user will have a "belief set", 
a set of nodes that the user in question has decided to accept.
This set could be kept secret or revealed to others, 
but I would be hesitant to make a single set shared by multiple users.
However, the belief set idea is really a matter of the user experience;
it is not part of the data itself.


## Handling Disagreement

If you and I disagree about some fact,
all we need to do is have a different set of nodes we chose to believe.
We can still share with one another new nodes we create
and most of these will probably apply to both of our views of the world
whether the disagreement is rooted in a match, a property, a connection, or a source.
Edit wars are difficult to create in a world without edits.


## Handling Uncertainty

Even more useful than being able to disagree with collaborators
is being able to disagree with one's self.
I can, for example, have data asserting that persons A and B are the same individual,
that persons B and C are the same individual, and that A and C are distinct individuals.
Even though this state contains a logical contradiction and cannot be how history actually looked,
I can leave it in the data to represent my confusion
until such time as I have sufficient evidence to resolve the puzzle.
In general, I can enter both sides of a difficult choice into the data
and explore what the world would look like under each scenario,
only later deciding that one decision is superior in some way to the others.

Polygenea can also model probabilistic uncertainty with `meta-probability` properties,
though the merits of such properties are not at present evident to me.


## Recording Provenance

Provenance of sources is easily modelled
as two sources with a "derived-from" Connection between them.
Those connection nodes themselves may be sourced 
to express the rationale behind the provenance decisions,
or may be left without a source.


## Attribution

Attribution (to the creator of a node) is easily handled with `meta-creator` properties.
Nodes that are independently created several times by several researchers
will simply accrue a set of creator properties.


## Safe, Small, Interesting Tasks

The polygenea data model was designed with the goal of having each atomic research step
result in the creation of a few new nodes.
If I succeeded in this goal then *any* small-but-useful contribution one might make
can be represented by the addition of a few nodes.
Additionally, the impact of a mistake is limited since others can simply ignore it.


## Rule Creation

I envision a simple way to guide novices through the process of creating a Rule node:

1.	User creates a ruleless Inference and the nodes that will cite it as their source.

2.	A overly-constrained Rule is generated by

	1.	Copying each antecedent node of the Inference into the Rule's antecedent
	2.	Copying each nodes sourcing the Inference to the Rule's consequent pool
	3.	Adding any nodes that the consequent references that are not yet either an antecedent or consequent to the antecedent pool
	4.	Removing all fields of antecedent nodes that reference nodes not in the antecedent pool
	5.	Replacing all remaining references with local references

3.	The Rule is generalized by asking the user questions like the following:
	
	*	"Is the fact that the name was 'John' important to this inference?"
	*	"Both things in this example had the same date property; is that important to this inference?"
		
	…and so on to determine parts of the antecedents that can be loosened up.
	




