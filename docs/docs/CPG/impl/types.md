# Types

We currently maintain a rather complex type system in the CPG, with several layers of inheritance. The most basic abstract type is `Type`, which holds properties common to all types.

## `HasType`

Not all nodes in the CPG have a type. For example, a `NamespaceDeclaration` does not have a type, but a `FunctionDeclaration` has a corresponding `FunctionType`. To make this distinction, we have the `HasType` interface, which is implemented by all nodes that have a type. The `HasType` interface requires mainly the implementation of two properties:

### `type`

This refers to the type of the node at *compile-time* and is a single `Type` object. For example, when declaring a variable `int i` in C/C++, the `type` of the variable `i` is `int`. 

Often, languages allow to skip setting the explicitly, for example using `var i = 1` in Java. In this case, the type is inferred by the compiler and set to `int`. This still happens at compile-time, since the compiler is able to deduce the type, and it will stay the same during runtime. To simulate this, the initial `type` will be set to `AutoType`. Once we know the correct type, the CPG will also infer the type of the variable `i` to `int` through its type-observer system and replace the `AutoType` with the correct type.

### `assignedTypes`

The second property is `assignedTypes`, which is a set of `Type` objects. This property is used to store the types that are assigned to the node at *runtime*. For example considering the following C++ code:

```cpp
Interface obj;
if (something) {
    obj = new A();
} else {
    obj = new B();
}
```

The `assignedTypes` of `obj` will be a set containing `A` and `B` (plus the interface `Interface` itself), since the type of `obj` can be either `A` or `B` at runtime. This feature is especially important for languages that allow dynamic typing, such as JavaScript or Python, where the type of a variable can change at runtime (see [below](#dynamically-typed-languages)).

## Dynamically Typed Languages

Dynamically typed languages such as Python or JavaScript present an extra challenge because for most (if not all) variables, the type is not known at compile-time. In these languages, the type of a variable can change at runtime, and the CPG needs to be able to represent this. We therefore introduce a special `DynamicType` type class and assign it to `type` of most nodes. There are some exceptions to this rule:
- `ConstructExpression`: When we construct an object, we know the type of the object at compile-time, so we set the `type` to the type of the object.
- `Literal`: When we have a literal, we also know the type at compile-time, so we set the `type` to the type of the literal, for example `str` for string-based literals in Python.
- Lists, dictionaries, comprehensions: When we encounter a collection comprehension, list or dictionary expressions, we also know a partial type at compile-time, so we set the `type` to the type of the collection, e.g. a list or a dictionary in Python.
