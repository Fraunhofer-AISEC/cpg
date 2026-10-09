/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Cpg.Basic

/-!
# CPG expression nodes

A model of the subset of CPG expression node classes (`de.fraunhofer.aisec.cpg.graph.expressions`)
that the verified translations produce. Each constructor corresponds to one Kotlin node class;
only the properties that are set during translation are modelled. Edges that are added by later
passes (EOG, DFG, `refersTo`, ...) are deliberately absent.
-/

namespace Cpg

/--
A type, as the frontend builds it with the type builders. Types are unresolved names here; they are
resolved by the `TypeResolver` pass later.
-/
inductive TypeRef where
  /-- `primitiveType(name)` -/
  | primitive (name : String)
  /-- `unknownType()` -/
  | unknown
  /-- `objectType(name, generics)`, which is a primitive type if `name` is a built-in type -/
  | object (name : String) (generics : List TypeRef)
  /-- `type.pointer()` -/
  | pointer (type : TypeRef)
  /-- `type.array()` -/
  | array (type : TypeRef)
  /-- `typeManager.resolvePossibleTypedef(type)`, i.e. the type behind an alias, if any -/
  | resolved (type : TypeRef)
deriving Repr, BEq, Inhabited

/-- CPG expression nodes. -/
inductive Expr where
  /-- `Literal`, optionally with a `name` (e.g. for the predeclared identifiers `true`). -/
  | literal (loc : Span) (value : Value) (type : TypeRef) (name : Option String)
  /-- `Reference` with its (possibly fully qualified) name. -/
  | reference (loc : Span) (name : String)
  /-- `BinaryOperator` (or `ShortCircuitOperator` for `&&` and `||`). -/
  | binaryOperator (loc : Span) (operatorCode : String) (lhs rhs : Expr)
  /-- `UnaryOperator` -/
  | unaryOperator (loc : Span) (operatorCode : String) (input : Expr)
  /-- `Call` -/
  | call (loc : Span) (callee : Expr) (arguments : List Expr)
  /-- `MemberAccess` of the member `name` of `base` -/
  | memberAccess (loc : Span) (name : String) (base : Expr)
  /-- `MemberCall`; the callee is a `MemberAccess` -/
  | memberCall (loc : Span) (callee : Expr) (arguments : List Expr)
  /-- `Subscription` of `arrayExpression` with `subscriptExpression` (an index or a `Range`) -/
  | subscription (loc : Span) (arrayExpression subscriptExpression : Expr)
  /-- `Range` with optional `floor`, `ceiling` and `third` -/
  | range (loc : Span) (floor ceiling third : Option Expr)
  /-- `PointerDereference` -/
  | pointerDereference (loc : Span) (input : Expr)
  /-- `Cast` of `expression` to `castType` (a conversion or a type assertion) -/
  | cast (loc : Span) (castType : TypeRef) (expression : Expr)
  /-- `New` with its type and initializer -/
  | new (loc : Span) (type : TypeRef) (initializer : Expr)
  /-- `Construction` of a type with arguments -/
  | construction (loc : Span) (type : TypeRef) (arguments : List Expr)
  /-- `ArrayConstruction` of a type with dimensions -/
  | arrayConstruction (loc : Span) (type : TypeRef) (dimensions : List Expr)
  /-- `InitializerList` of a type with initializers -/
  | initializerList (loc : Span) (type : TypeRef) (initializers : List Expr)
  /-- `KeyValue` -/
  | keyValue (loc : Span) (key value : Expr)
  /-- `ProblemExpression` -/
  | problem (loc : Span) (problem : String)
deriving Repr, Inhabited, BEq

/-- The location of a node. -/
def Expr.loc : Expr → Span
  | .literal loc .. | .reference loc _ | .binaryOperator loc .. | .unaryOperator loc ..
  | .call loc .. | .memberAccess loc .. | .memberCall loc .. | .subscription loc ..
  | .range loc .. | .pointerDereference loc _ | .cast loc .. | .new loc .. | .construction loc ..
  | .arrayConstruction loc .. | .initializerList loc .. | .keyValue loc .. | .problem loc _ => loc

end Cpg
