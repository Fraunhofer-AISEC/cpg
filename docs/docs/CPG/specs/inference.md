---
title: "Inference of new nodes"
linkTitle: "Inference of new nodes"
no_list: true
weight: 1
date: 2025-01-10
description: >
    Inference of new nodes
---

# Inference System

One of the goals of this library is to deal with incomplete code. In this case,
the library provides various options to create new nodes and include them in the
resulting graph. The user of the library can configure which of the inference
options should be enabled. This document provides an overview of the different
options and their expected behavior. The rules for the inferring new nodes are
implemented in the class
[`de.fraunhofer.aisec.cpg.passes.inference.Inference`](https://fraunhofer-aisec.github.io/cpg/dokka/main/older/main/cpg-core/de.fraunhofer.aisec.cpg.passes.inference/-inference/index.html)
and are typically used by various passes.

## Inference of namespace and record declarations

If we encounter a scope, e.g, in a call to a function such as
`java.lang.Object.toString()`, and we do not have a corresponding `NameScope`
for the qualified name `java.lang`, we try to infer one. We recursively infer a
namespace, e.g., `java` as well as `java.lang` until the scope can be resolved.
There is one special check, in case the name refers to a type. In this case we
infer a record declaration instead. This is usually the case when a type is
nested in another type, e.g. `MyClass::MyIterator::next`. If we encounter usage
of `MyClass::MyIterator` as a type somewhere, we infer a record instead of a
namespace.

Record declarations are indeed inferred for all (object) types that we
encounter. The scope of the type or a fully qualified name (if specified) is
taken into account when creating an inferred `RecordDeclaration`. If the record
is supposed to exist in a scope / namespace that was "seen" (e.g., it was
specified as a fully qualified name), but a corresponding `NamespaceDeclaration`
did not exist, we also try to infer this namespace (see above). 

For example, if we encounter the type `java.lang.String` (and do not find a
matching declaration), we recursively infer the following nodes:

- `NamespaceDeclaration` for `java` in the `GlobalScope`
- `NamespaceDeclaration` for `java.lang` in the scope of the inferred `java`
  namespace
- `RecordDeclaration` for `java.lang.String` in the scope of the inferred
  `java.lang` namespace

It is sometimes indistinguishable whether we should infer a namespace or a
record as a parent scope, since usually languages support nested records or
classes. However, we tend to assume that the case that it is a namespace is far
more likely.

## Inference of function declarations

If we try to resolve a `Call`, where no `FunctionDeclaration` with a
matching name and signature exists in the CPG, we infer a new
`FunctionDeclaration`. This may include inferring a receiver (i.e., the base a
method is invoked on) for object-oriented programming languages. We also infer
the required parameters for this specific call as well as their types.

The function declaration must be inferred within the scope of a
`RecordDeclaration`, a `NamespaceDeclaration` or a `TranslationUnitDeclaration`.
If the function `foo` is inferred within the scope of a `RecordDeclaration`,
`foo` *may* represent a method, but it could also be a static import depending on
the `LanguageTraits` of the programming language. If we add a
`MethodDeclaration` to a `RecordDeclaration` which we treated as a "struct", we
change its `type` to "class".

## Inference of variables

While we do aim at handling incomplete code, we assume that it is more likely to
analyze complete functions and missing some files/dependencies compared to
having all files/dependencies available and missing few lines within a file.
Based on this assumption, we infer global variables if we cannot find any
matching symbol for a reference, but we do NOT infer local variables.

## Inference of return types of functions

This is a rather experimental feature and is therefore disabled by default.

This option can be used to guess the return type of an inferred function
declaration. We make use of the usage of the returned value (e.g. if it is
assigned to a variable/reference, used as an input to a unary or binary operator
or as an argument to another function call) and propagate this type to the
return type, if it is known. One interesting case are unary and binary operators
which can be overloaded, but we assume that they are more likely to treat numeric
values (for `+`, `-`, `*`, `/`, `%`, `++`, `--`) and boolean values (for `!`).

## Inference of DFG edges

The library can apply heuristics to infer DFG edges for functions which do not
have a body (i.e., functions not implemented in the given source code) if there
is no custom specification for the respective function available. All parameters
will flow into the return value.
