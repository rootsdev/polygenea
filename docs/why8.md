Why the Eight Node Types?
=========================

Property vs Connection
----------------------

Property and Connection nodes can both be posed with identical field names:
some *key* *of* a node is a *value*.
So why are Property and Connection distinct node types?
Let's consider two ways of merging them.

### Just Connections

We could replace `<Property key="name" of="3" value="Jon"/>` with two nodes:
a content node `<Digitisation content="Jon" content-type"text/plain"/>`
and a connection `<Connection key="name" of="3" value="5"/>`.

This, however, leaves the name itself sitting out there as a node
subject to being disbelieved or having properties attached to it.
Someone will probably try to add a connection suggesting "Jon" is short for "Jonathan"
or something of that sort which is only true for some of the people named Jon.

There are some values that *should not* be their own nodes
because each Property needs its own copy.

### Common Type

We could also leave both value-is-data and value-is-reference cases
but combine them in a common node type, perhaps named something like Datum.
The problem with this model is that it can become confusing if a value 
is meant to be a reference or a literal value.
For every kind of reference I have considered thus far
there is an at least somewhat plausible scenario where a Property value
might look exactly like a reference.

So why not just add a type indicator telling us if the value is literal or a reference?
I did just that, and chose to store the indicator in the node type
instead of in a separate field or as part of the value itself.



BibItem vs Thing
----------------

A BibItem node is a set of mostly unconstrained key:value pairs.
That is also what a Thing node with its incoming Property nodes represents.
Why put the pairs inside the BibItem but tack them on outside the Thing?

Before going into the rationale, let me note that there is nothing stopping a user
from making a BibItem with just one field named "id", just like a Thing,
and adding all of its descriptors as Properties.
If you want Thing-like BibItems, go for it.

So why the difference?

Having values inside a Thing instead of pointing to it
would mean that we can't address individual values.
Once we Match a few Things and get a large set of possibly conflicting Properties
we'll probably want to point out at least a few we do not believe.
We'll also want to add some more Properties as the result of Inferences,
and don't want that process to add the headaches of versioning that come from modifying the original Things.
External properties give us the Principle of Sensible Disbelief.

I do not anticipate those same actions happening for BibItems.
I look at a source, I list what I know about it, and I move on.
If learning about source documents is the core of your research
then by all means, treat them like Thing nodes
and use Inferences and `<Connection key="meta-same"` Match-like nodes
to get the same kind of flexibility in merging sources and inferring details of a source's metadata.
Because I expect the common case to be create-once-and-never-revisit,
BibItems allow the more compact and less flexible values-inside representation.

However, I do not think any power is *lost* if we made BibItems look like Things,
so I might yet be persuaded otherwise.


Groupings
---------

Why no Group node type with a set of node references?
Because in (nearly) every case I could think of
a grouping was really either connections between individual group members
or a group Thing to which each member is connected.
The only case I could find that neither pair-wise Connections
nor a group Thing was ideal was a phrase like "Rebecca, Emily, Tina, Sue, Sarah, and Casey are sisters":
a "sisterhood" or "family" Thing (or a shared parent) is at most implied, not stated,
and it would take 15 connections to pair each of those 6 people to the other 5.
This kind of pathological case did not seem to warrant the creation of a new node type.


Undirected Connections
----------------------

Connection nodes are directed edges.
`<Connection key="mother" of="1" value="2/>`
and `<Connection key="mother" of="2" value="1/>` mean very different things.
What about undirected connections, like "distinct-from"?
If you really need both directions, add a Rule node to infer the opposite direction:

	<Rule>
		<antecedents>
			<Node/>
			<Node/>
			<Connection key="distinct-from" of="0" value="1"/>
		</antecedents>
		<consequents>
			<Connection key="distinct-from" of="1" value="0"/>
		</consequents>
	</Rule>


