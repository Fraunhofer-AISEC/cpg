---
title: "Implementation and Concepts - Scopes and Symbols"
linkTitle: "Implementation and Concepts - Scopes and Symbols"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---


# Scopes and Symbols

The concept of scopes and symbols are at the heart of every programming language and thus are also the core of static analysis. Both concepts consist in the CPG library through the types `Scope` and `Symbol` respectively.

A "symbol" can be seen as an identifier in most programming languages, referring to variables or functions. Symbols are often grouped in scopes, which defines the visibility of a symbol, e.g. a slice of a program that can "see" the symbol. Often this is also synonymous with the life-time of a variable, e.g., that its memory will be freed (or collected by a garbage collector) once it goes "out of scope".

```c
// This defines a symbol "a" in the global/file scope.
// Its visibility is global within the file.
int a = 1;

int main() {
    // this defines another symbol "a" in a function/block scope. 
    // Its visibility is limited to the block it is defined in. 
    int a = 1;
}
```

Usually symbols declared in a local scope override the declaration of a symbol in a higher (e.g., global scope), which is also referred to as "shadowing". This needs to be taken into account when resolving symbols to their declarations.

The `Scope` class holds all its symbols in the `Scope::symbols` property. More specifically, this property is a `SymbolMap`, which is a type alias to a map, whose key type is a `Symbol` and whose value type is a list of `Declaration` nodes. This is basically a symbol lookup table for all symbols in its scope. It is a map of a list because some programming languages have concepts like function overloading, which leads to the declaration of multiple `FunctionDeclaration` nodes under the same symbol in one scope. In the current implementation, a `Symbol` is just a typealias for a string, and it is always "local" to the scope, meaning that it MUST NOT contain any qualifier. If you want to refer to a fully qualified identifier, a `Name` must be used. In the future, we might consider merging the concepts of `Symbol` and `Name`. 

For a frontend or pass developer, the main interaction point with scopes and symbols is through the `ScopeManager`. The scope manager is available to all nodes via the `TranslationContext` and also injected in frontend, handlers and passes.

## Hierarchy of Scopes

Each scope (except the `GlobalScope`) can have a parent and possible child scopes. This can be used to model a hierarchy of scopes within a program. For example using the snippet above, the following scopes are defined in the CPG:

* A `GlobalScope` that comprises the whole file
* A `FunctionScope` that comprises the function `main`
* A `BlockScope` that comprises the function body

Note, that each programming language is different when it comes to scoping and this needs to be thought of by a frontend developer. For example in C/C++ each block introduced by `{}` introduces a new scope and variables can be declared only for such a block, meaning that each `for`, `if` and other statements also introduce a new scope. In contrast, Python only differentiates between a global scope, function and class scope. 

## Defining Scopes and Declaring Symbols

In order to define new scopes, the `ScopeManager` offers two main APIs:

* `enterScope(node)`, which specifies that `node` will declare a new scope and that an appropriate `Scope` (or derived type) will be created 
* `leaveScope(node)`, which closes the scope again

It is important that every opened scope must also be closed again. When scopes are nested, they also need to be closed in reverse order.

```Kotlin
// We are inside the global scope here and want to create a new function
var func = newFunctionDeclaration("main")

// Create a function scope
scopeManager.enterScope(func)

// Create a block scope for the body because our language works this way
var body = newBlock()
func.body = body
scopeManager.enterScope(body)

// Add statements here
body.statements += /* ... */

// Leave block scope    
scopeManager.leaveScope(body)

// Back to global scope, add the function to global scope
scopeManager.leaveScope(func)
scopeManager.addDeclaration(func)
```

Inside the scope, declarations can be added with `ScopeManager::addDeclaration`. This takes care of adding the declaration to an appropriate place in the AST (which beyond the scope of this document) and also adds the `Declaration` to the `Scope` under the appropriate `Symbol`.


## Looking up Symbols

During different analysis steps, e.g., in different passes, we want to find certain symbols or lookup the declaration(s) belonging to a particular symbol. There are two functions in order to do so - a "higher" level concept in the `ScopeManager` and a "lower" level function on the `Scope` itself.

The lower level one is called `Scope::lookupSymbol` and can be used to retrieve a list of `Declaration` nodes that belong to a particular `Symbol` that is "visible" the scope. It does so by first looking through its own `Scope::symbols`. If no match was found, the scope is traversed upwards to its `Scope::parent`, until a match is found. Furthermore, additional logic is needed to resolve symbol that are pointing to another scope, e.g., because they represent an `ImportDeclaration`. 

```Kotlin
var scope = /* ... */
var declarations = scope.lookupSymbol("a") {
    // Some additional predicate if we want
}
```

Additionally, the lookup can be fine-tuned by an additional predicate. However, this should be used carefully as it restricts the possible list of symbols very early. In most cases the list of symbols should be quite exhaustive at first to find all possible candidates and then selecting the best candidate in a second step (e.g., based on argument types for a function call).

While the aforementioned API works great if we already have a specific start scope and local `Symbol`, we often start our resolution process with a `Name` -- which could potentially be qualified, such as `std::string`. Therefore, the "higher level" function `ScopeManager::lookupSymbolByName` can be used to retrieve a list of candidate declarations by a given `Name`. In a first step, the name is checked for a potential scope qualifier (`std` in this example). If present, it is extracted and the search scope is set to it. This is what is usually referred to as a "qualified lookup". Otherwise, the local part of the name is used to start the lookup, in what is called an "unqualified lookup". In both cases, the actual lookup is delegated to `ScopeManager::lookupSymbols`, but with different parameters.

```Kotlin
var name = parseName("std::string")
// This will return all the 'string' symbols within the 'std' name scope
var stringSymbols = scopeManager.lookupSymbolByName(name)
```

Developers should avoid symbol lookup during frontend parsing, since often during parsing, only a limited view of all symbols is available. Instead, a dedicated pass that is run on the complete translation result is the preferred option. Apart from that, the main usage of this API is in the [SymbolResolver](symbol-resolver.md).