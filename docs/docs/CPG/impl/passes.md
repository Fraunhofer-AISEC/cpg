---
title: "Implementation and Concepts - Passes"
linkTitle: "Implementation and Concepts - Passes"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---



# Implementation and Concepts: Passes

## What is a Pass?

Passes get a prebuilt CPG that at least contains the CPG-AST and output a
modified graph. Their purpose is to extend the syntactic representation of code
with additional nodes and edges to represent the semantics of the program.
Passes can be executed in sequence, where the output of the previous pass serves
as input of the next pass.

## Creating a new Pass

The user of the cpg library can implement her own passes. Each pass needs to
extend the class `Pass` and implement its base function`accept(result: TranslationResult)`.
The remaining structure of the pass is free to be designed by the
implementer.

Each pass should have a short description of its purpose in the annotation `@Description`.

## Registering a Pass

A newly created pass has to be registered with the `TranslationManager` through
its builder by calling
```
val configuration = TranslationConfiguration.builder().
    // ...
    .registerPass(...)
```
## Modifying a Pass

A preexisting pass can be modified by extending it and overwriting its
functions. For this purpose, all member functions of existing library passes
have the visibility `protected`. Depending on the modified pass, internal
constructs have to be respected.

For example, the `EvaluationOrderGraphPass` uses an internal handle structure.
When extending this pass, it is necessary to add handlers of new Node types to
the internal handler map. If a developer needs to override an existing handler,
the handle has to be implemented with the same signature to use the polymorphism
feature. Additionally, the mapping of `node type -> handler` needs to be replaced
by a new entry `node type -> overridden` handler.

## Ordering Passes
Passes may depend on the information added by another pass. This requires us to
enforce the order in which passes are executed. To do so, we provide the
following annotations for the passes:

* `DependsOn(other: KClass<out Pass>, softDependency: Boolean = false)` -- The annotated pass is executed after
   the other pass(es). If `softDependency` is set to `false`, it automatically
   registers these passes if they haven't been registered by the user.
* `ExecuteBefore(other: KClass<out Pass>, ...)` -- The annotated pass is executed
   before the other pass(es) specified.
* `ExecuteFirst` -- The annotated pass is executed as the first pass if possible.
* `ExecuteLast` -- The annotated pass is executed as the last pass if possible.
* `RequiredFrontend(frontend: KClass<out LanguageFrontend>)` -- The annotated pass
   is only executed if the frontend has been used.
* `RequiresLanguageTrait(trait: KClass<out LanguageTrait>)` -- The annotated pass
  is only executed if the `language` of the `TranslationUnit` which is currently studied
  implements the given `trait`.
