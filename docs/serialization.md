Polygenea Serialisation
=======================

Polygenea is trivial to store.
Because it is write-once, there are no updates or edits to serialise.
Because it is a directed acyclic graph, there are not cyclic dependencies to resolve.
Because the graph is generally quite shallow with a small branching factor
there isn't even much to worry about as far as scaling and caching goes.
It fits easily in a relational database (with the possible exception of BibItem nodes),
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

	{"!class":"Digitisation","content-type":"text/plain","contents":"Tom's father"}



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

The constructor syntax does not lend itself to the open-ended contents of a BibItem node: the keys cannot be inferred from position.
However, Python-like constructor syntax would work at almost exactly the same space requirements as XML:

	BibItem(author="Tychonievich, Luther A", title="Polygenea")

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

