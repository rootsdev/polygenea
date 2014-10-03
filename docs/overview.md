Polygenea
=========

Polygenea is designed to be a simple and versatile data model
for all kinds of family history research.
Its goals include

*	Representing the core atomic elements of genealogical research

*	Allowing disagreements and alternate views without conflict

*	Being able to represent all of the semantic nuances of all existing models

*	Simplicity: the spec should be as small and simple as possible


You can read more in the following articles:

1.	[Guiding Principles](principles.md), a set of ideals and beliefs that guide polygenea development

2.	[The 8 Node Types](nodes.md), describing the data model itself, and some discussion of [why these 8](why8.md).

3.	[Serialization](serialization.md), describing several options for turning the nodes into byte streams
	I anticipate converging on a prefered model at some point.

4.	[Use Cases](usecases.md), initially including
	
	* 	[Changing data](usecases.md#fixing-a-mistake)
	* 	[Distributed research](usecases.md#distributed-research)
	* 	[Privacy and Data Ownership](usecases.md#privacy-and-data-ownership)
	* 	[Handling Disagreement](usecases.md#handling-disagreement)
	* 	[Handling Uncertainty](usecases.md#handling-uncertainty)
	* 	[Recording Source Provenance](usecases.md#recording-provenance)
	* 	[Attribution](usecases.md#attribution)
	* 	[Safe, Small, Interesting Tasks](usecases.md#safe-small-interesting-tasks)
	* 	[Interactive Rule Creation](usecases.md#rule-creation)
	* 	[Tool-Specific Values](usecases.md#tool-specific-values)
	* 	[Belief Sets](usecases.md#belief-sets)
	* 	[Authority-Controlled Attributions](usecases.md#authority-controlled-attributions)


Reference Implementation
------------------------

The reference Java and D implementations currently available
use reflection and meta-programming (which makes the hard to read)
and reflect a slightly older, more complicated version of Polygenea.
Updated reference implementations are in the works.


Licensing
---------

I hereby release polygenea, including its ideas, documentation, and reference code,
into the public domain.
