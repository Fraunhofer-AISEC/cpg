---
title: "Implementation and Concepts"
linkTitle: "Implementation and Concepts"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---

# Implementation and Concepts

The translation of source code to the graph consists of two main steps. First,
the source code is parsed and transferred to the CPG nodes by a so-called
**Language Frontend**. Then, **Passes** refine the information which is kept in
the graph. These two stages are strictly separated one from each other.

![Overview of the CPG pipeline](../../assets/img/cpg-flow.png) 
{ align=center }


* [Languages and Language Frontends](./language)
* [Scopes](./scopes)
* [Passes](./passes)
* [Symbol Resolution](./symbol-resolver.md)
