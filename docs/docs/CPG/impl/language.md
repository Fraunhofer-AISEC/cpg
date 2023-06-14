---
title: "Implementation and Concepts - Language Frontends"
linkTitle: "Implementation and Concepts - Language Frontends"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---



# Implementation and Concepts: Language and Language Frontends

Even though we are aiming for a language-independent representation of source
code, we still need to parse source code depending on the original programming
language used. Therefore, we are introduce two concepts that help developers and
users to understand how the CPG translates language-specific code into an
abstract form. 

## `Language`

The first concept is a `Language`. It represents the programming language as a
general concept and contains meta-information about it. This includes:

* The name of the language, e.g. C++
* The delimiter used to separate namespaces, e.g., `::`
* The [`LanguageFrontend`](#LanguageFrontend) used to parse it
* Additional [`LanguageTrait`](#LanguageTrait) implementations

Each `Node` has a `language` property that specifies its language.

### `LanguageTrait`

A language trait aims to further categorize a programming language based on
conceptual paradigms. This can be easily extended by introducing new interfaces
based on `LanguageTrait`. Examples include:

* Are default arguments supported?
* Does the language have structs or classes?
* Are function pointers supported?
* Are templates or generics used in the language?
* Do we need some special knowledge to resolve symbols, calls, variables?

These traits are used during the pass execution phase to fine-tune things like
call resolution or type hierarchies.

## `LanguageFrontend`

In contrast to the `Language` concept, which represents the generic concept of a
programming language, a `LanguageFrontend` is a specific module in the CPG
library that does the actual translating of a programming language's source code
into our CPG representation.

At minimum a language frontend needs to parse the languages' code and translate
it to specific CPG nodes. It will probably use some library to retrieve the
abstract syntax tree (AST). The frontend will set the nodes' `AST` edges and
establish proper scopes via the scope manager. Everything else, such as call or
symbol resolving is optional and will be done by later passes. However, if a
language frontend is confident in setting edges, such as `REFERS_TO`, it is
allowed to and this is respected by later passes. However, one must be extremely
careful in doing so.

The frontend has a limited life-cycle and only exists during the *translation*
phase. Later, during the execution of passes, the language frontend will not
exist anymore. Language-specific customization of passes are done using
[`LanguageTraits`](#LanguageTrait).

To create nodes, a language frontend MUST use the node builder functions in the
`ExpressionBuilder`, `DeclarationBuilder` or `StatementBuilder`. These are
Kotlin extension functions that automatically inject the context, such as
language, scope or code location of a language frontend or its handler into the
created nodes.

## Supporting a new language

To support a new language, all you have to do is to

* Provide a new `Language`. Here, you have to think about the features of the
  programming language and which `LanguageTraits` the respective language has to
  implement. With this, you provide the respective fine-tuning to make the
  Passes work properly.
* Implement a new `LanguageFrontend` which is executed if a file matches the new
  `Language`. The requirements of the frontends are described above.

To make use of the respective frontend by the CPG, you have to configure the
translation accordingly. This is done by the `TranslationConfiguration` where
you register your new language by calling one of the `registerLanguage()`
methods. As an example
```kotlin
val config: TranslationConfiguration = TranslationConfiguration
    .builder()
    // More configuration
    .registerLanguage(MyNewLanguage()) // Option 1
    .registerLanguage<MyOtherNewLanguage>() // Option 2
    .registerLanguage("MyThirdNewLanguage") // Option 3
    .build()
```
