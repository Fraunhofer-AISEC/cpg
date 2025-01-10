---
title: "Inference of new nodes"
linkTitle: "Inference of new nodes"
no_list: true
weight: 1
date: 2025-01-10
description: >
    Inference of new nodes
---

One of the goals of this library is to deal with incomplete code. In
this case, the library provides various options to create new nodes
and include them in the resulting graph. The user of the library can
configure which of the inference options should be enabled. This
document provides an overview of the different options and their
expected behavior. The rules for the inferring new nodes are implemented
in the class `de.fraunhofer.aisec.cpg.passes.inference.Inference` and
are typically used by various passes.

# Inference of namespace declarations

# Inference of record declarations

# Inference of function declarations

If we try to resolve a `CallExpression`, where no `FunctionDeclaration` with
a matching name and signature exists in the CPG, we infer a new
`FunctionDeclaration`. This may include inferring a receiver (i.e., the base a
method is invoked on) for object-oriented programming languages. We also infer
the required parameters for this specific call as well as their types.

The function declaration must be inferred within the scope of a
`RecordDeclaration`, a `NamespaceDeclaration` or a `TranslationUnitDeclaration`.
If the function `foo` is inferred within the scope of a `RecordDeclaration`,
`foo` *may* represent a method but it could also be a static import depending
on the `LanguageTraits` of the programming language. If we add a
`MethodDeclaration` to a `RecordDeclaration` which we treated as a "struct", we
change its `type` to "class".

# Inference of variables

While we do aim at handling incomplete code, we assume that it is more likely
to analyze complete functions and missing some files/dependencies compared to
having all files/dependencies available and missing few lines within a file.
Based on this assumption, we infer global variables if we cannot find any
matching symbol for a reference.

# Inference of return types of functions

This is a rather experimental feature and is therefore disabled by default.

This option can be used to guess the return type of an inferred function
declaration. We make use of the usage of the returned value (e.g. if it
is assigned to a varible/reference, used as an input to a unary or binary
operator or as an argument to another function call) and propagate this
type to the return type, if it known. One interesting case are unary and
binary operators which can be overloaded but we assume that they are more
likely to treat numeric values (for `+`, `-`, `*`, `/`, `%`, `++`, `--`)
and boolean values (for `!`).

# Inference of DFG edges

The library can apply heuristics to infer DFG edges for functions which
do not have a body (i.e., functions not implemented in the given source
code) if there is no custom specification for the respective function
available. All parameters will flow into the return value.
