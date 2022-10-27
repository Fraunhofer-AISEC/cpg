# Specification: Language

Currently, the language frontends are used for numerous decisions in the passes.
This is bad design and we want to get rid of it after having parsed the language-specific AST.
The information required by the passes should be represented in a new `Language` class. This class has to be linked to each node to ensure that multiple languages can be parsed at the same time.

**Passes must not use the state of a language frontend.**

## What's a language frontend?
The language frontend parses the code of one language and translates it to the CPG AST and **nothing more!** It most likely uses a language parser for the task.

## Requirements/Tasks:
* Each node must have a relation to its language.
* The language class must not have a state.
* The language class has mostly "metadata"
* The Translation Result holds the (merged) Scope Manager which is set after executing the language frontends.
* The language traits will be moved to the new language class.

## Which information is required?
* Namespace delimiter
* Are default arguments supported?
* Superclass keyword
* HasClass, HasStruct (used for inference)
* HasFunctionPointers
* HasTemplates (already exists as language trait)

## Which functionality is required?
* Customizing call resolution => Finetune function which depends on the language frontend used.
* Language class decides which language frontend should be used


## Other related tasks:
* Removes typedefs from the ScopeManager