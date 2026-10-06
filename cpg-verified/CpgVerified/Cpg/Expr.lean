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

/-- The type that the frontend assigns to a node. -/
inductive TypeRef where
  /-- `primitiveType(name)` -/
  | primitive (name : String)
  /-- `unknownType()` -/
  | unknown
deriving Repr, DecidableEq, Inhabited

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
  /-- `ProblemExpression` -/
  | problem (loc : Span) (problem : String)
deriving Repr, Inhabited

/-- The location of a node. -/
def Expr.loc : Expr → Span
  | .literal loc .. | .reference loc _ | .binaryOperator loc .. | .unaryOperator loc ..
  | .call loc .. | .problem loc _ => loc

end Cpg
