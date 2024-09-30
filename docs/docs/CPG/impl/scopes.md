---
title: "Implementation and Concepts - Scopes"
linkTitle: "Implementation and Concepts - Scopes"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---


# Scopes and Symbols

The concept of scopes and symbols are at the heart of every programming language and thus are also the core of static analysis. Both concepts consist in the CPG library through the types `Scope` and `Symbol` respectively. A `Symbol` is just a typealias for a string.

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

The `Scope` class holds all its symbols in the `Scope::symbols` property. More specifically, this property is a `SymbolMap`, which is a type alias to a map, whose key type is a `Symbol` and whose value type is a list of `Declaration` nodes. This is basically a symbol lookup table for all symbols in its scope. It is a map of a list because some programming languages have concepts like function overloading, which leads to the declaration of multiple `FunctionDeclaration` nodes under the same symbol in one scope.

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
