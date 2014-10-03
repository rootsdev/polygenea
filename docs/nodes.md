The Nodes
=========

Polygenea data is made up of eight kinds of nodes.

In addition to this overview, examples can be found in [Polygenea Serialisation](serialisation.md)
and additional discussion in [Use Cases](usecases.md) and [Why Eight Node  Types?](why8.md).

Claims
------

### Core Claims

There are three core kinds of claims:

#### **Thing** node

Represents the claim "something existed."

Because *anything* about the claim could be sensibly disbelieved,
the only content of a Thing node is a unique identifier and a reference to the source asserting that the thing exists.


#### **Property** node

Attaches a datum to another node.
The datum should not be sensible to disbelieve in isolation of the node to which is applies.

The content of a Property node is a key/value pair
and references to its source and the node to which the property applies.


#### **Connection** node

Attaches two nodes together.

The content of a Connection node is a description of the relationship between the nodes
and a reference to its source and the pair of nodes it connects.


### Matches

In addition to the four core claims, there is also a fourth kind of quasi-claim node:

#### **Match** node
Represents the decision "these two Thing nodes represent the same real-world entity."

The content of a Match node is a reference to its source and the pair of nodes it "merges".

A Match acts in every way as a Thing node.
Every node referring to either matched thing by definition is also treated as referring to the Match node.

Match nodes are similar to connections and could be modelled as connections
but are kept separate because of their acts-like-a-Thing character.
Properties and Connections that point to a Connection refer to the relationship the Connection describes;
Properties and Connections that point to a Match refer to the aggregate Thing the Match describes.
	
### Special Claims
	
#### Comments are Properties

Any property whose key begins `meta-` and any connection whose label begins `meta-`
is interpreted as expressing meta-level information about the data,
such as comments, assertions of likelihood, or other metadata.

#### "Optional" Sources

It is a guiding principle of Polygenea that there is always a source.
However, the source link in each node is optional; a missing source represents the following meaning:

*	A missing source link in a Thing node, as well as non-meta Property nodes and Connection nodes, means "The source is unknown or was not recorded."

*	A missing source link in a meta-type node means "The contributor chose to remain anonymous."
	
*	A missing source link in a Match node, means "The source is an inference based on the quantity of similar Property and Connection nodes pointing to the matched Thing nodes."



Sourcing
--------


### Source Nodes

There are at three kinds of source nodes.

#### **BibItem** node
A reference pointing to something external to the digital data.

At present, my expectation is that these nodes are a more-or-less unconstrained sets of key-value pairs.
A lot of work is needed to standardise these,
including at least a set of "preferred" or "standard" keys,
a discipline for translingual activities,
and probably a set of formatting guides for at least some classes of values.

In earlier versions of Polygenea I called these "Citation nodes".
Confusion over the user of "citation" as a textual presentation vs. underlying data
caused me to switch terms to "BibItem" (inspired by BibTeX's database of reference entries and LaTeX's command `\bibitem`).
I am not yet convinced that this term is optimal.

#### **Digitisation** node
An in-data document being referenced.
May be derivative, like a transcript or image of an external document;
or may be original, a user-testified source entered directly by the user.

The content of a digitisation blob
is a media-type field
and either a content-hash based pointer to a local file
or an internal byte stream.
	
#### **Inference** node
A representation of a particular application of reasoning.

The content of an Inference node is a reference to the Rule node of which this inference is an instance and a set of node references that match to the Rule's antecedents.

If there is no Rule reference, the rule is assumed to be a "one-off" rule:
the specific support nodes referenced 
imply the nodes that are sourced to the Inference and nothing else.


### Inference Rules

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


#### **Rule** node

A representation of a trend or rule.

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

The name "Rule" is problematic,
suggesting it only covers natural laws and not strong trends.
"Trend" has the opposite problem.
Other terms I've considered (e.g. "hypothetical proposition", "reason", "pattern") have their own problems.
I am open to alternative names.
