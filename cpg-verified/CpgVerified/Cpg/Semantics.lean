/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Cpg.Expr

/-!
# Semantics of CPG expressions

A big-step evaluator for CPG expressions. It is defined purely in terms of CPG concepts
(operator codes, references by name, ...) and is independent of any source language: a
translation is correct if evaluating its output with this evaluator gives the same result as
the source language's own semantics.

Operator codes do not mean the same thing in every language (e.g. `/` on integers truncates in
Go but floors in Python), so the few language-dependent choices are a parameter.
-/

namespace Cpg

/-- The language-dependent parts of the meaning of CPG operators. -/
structure LanguageSemantics where
  /-- Integer division for operator code `/`; `none` signals a run-time error. -/
  intQuo : Int → Int → Option Int
  /-- Integer remainder for operator code `%`; `none` signals a run-time error. -/
  intRem : Int → Int → Option Int

/-- The evaluation environment, keyed by the names stored in `Reference` nodes. -/
structure Env where
  vars : String → Option Value
  funcs : String → Option (List Value → Option Value)
  /-- The heap: the fields of the object at an address. -/
  fields : Nat → String → Option Value
  /-- Methods by name, applied to the receiver and the arguments. -/
  methods : String → Option (Value → List Value → Option Value)
  /-- Indexing `x[i]` of arrays, slices, maps and strings. -/
  index : Value → Value → Option Value
  /-- Slicing `x[low:high:max]`, with absent bounds as `none`. -/
  slice : Value → Option Value → Option Value → Option Value → Option Value
  /-- Dereferencing a pointer. -/
  deref : Value → Option Value
  /-- Converting a value to a type, or asserting its dynamic type. -/
  cast : TypeRef → Value → Option Value
  /-- Constructing a value of a type from arguments. -/
  construct : TypeRef → List Value → Option Value
  /-- Constructing an array of a type with dimensions. -/
  constructArray : TypeRef → List Value → Option Value
  /-- Allocating a new value of a (pointer) type with an initializer. -/
  new : TypeRef → Value → Option Value

/-- Meaning of a (strict) binary operator code. -/
def evalBinaryOp (L : LanguageSemantics) (code : String) (a b : Value) : Option Value :=
  match code, a, b with
  | "+", .int x, .int y => some (.int (x + y))
  | "+", .str x, .str y => some (.str (x ++ y))
  | "-", .int x, .int y => some (.int (x - y))
  | "*", .int x, .int y => some (.int (x * y))
  | "/", .int x, .int y => .int <$> L.intQuo x y
  | "%", .int x, .int y => .int <$> L.intRem x y
  | "&", .int x, .int y => some (.int (IntOps.land x y))
  | "|", .int x, .int y => some (.int (IntOps.lor x y))
  | "^", .int x, .int y => some (.int (IntOps.xor x y))
  | "&^", .int x, .int y => some (.int (IntOps.andNot x y))
  | "<<", .int x, .int y => .int <$> IntOps.shl x y
  | ">>", .int x, .int y => .int <$> IntOps.shr x y
  | "==", .int x, .int y => some (.bool (x == y))
  | "==", .str x, .str y => some (.bool (x == y))
  | "==", .bool x, .bool y => some (.bool (x == y))
  | "==", .nil, .nil => some (.bool true)
  | "!=", .int x, .int y => some (.bool (x != y))
  | "!=", .str x, .str y => some (.bool (x != y))
  | "!=", .bool x, .bool y => some (.bool (x != y))
  | "!=", .nil, .nil => some (.bool false)
  | "<", .int x, .int y => some (.bool (decide (x < y)))
  | "<", .str x, .str y => some (.bool (decide (x < y)))
  | "<=", .int x, .int y => some (.bool (decide (x ≤ y)))
  | "<=", .str x, .str y => some (.bool (decide (x ≤ y)))
  | ">", .int x, .int y => some (.bool (decide (y < x)))
  | ">", .str x, .str y => some (.bool (decide (y < x)))
  | ">=", .int x, .int y => some (.bool (decide (y ≤ x)))
  | ">=", .str x, .str y => some (.bool (decide (y ≤ x)))
  | _, _, _ => none

/-- Meaning of a unary operator code. -/
def evalUnaryOp (code : String) (a : Value) : Option Value :=
  match code, a with
  | "+", .int x => some (.int x)
  | "-", .int x => some (.int (-x))
  | "^", .int x => some (.int (Int.not x))
  | "!", .bool x => some (.bool (!x))
  | _, _ => none

mutual

/--
Evaluates a CPG expression. `none` means that evaluation fails (run-time error, unbound name,
type error, or a construct without modelled semantics such as a `ProblemExpression`).
-/
def Expr.eval (L : LanguageSemantics) (env : Env) : Expr → Option Value
  | .literal _ v _ _ => some v
  | .reference _ name => env.vars name
  | .binaryOperator _ code lhs rhs =>
    if code = "&&" then do
      let .bool a ← lhs.eval L env | none
      if a then
        let .bool b ← rhs.eval L env | none
        pure (.bool b)
      else pure (.bool false)
    else if code = "||" then do
      let .bool a ← lhs.eval L env | none
      if a then pure (.bool true)
      else
        let .bool b ← rhs.eval L env | none
        pure (.bool b)
    else do evalBinaryOp L code (← lhs.eval L env) (← rhs.eval L env)
  | .unaryOperator _ code input => do evalUnaryOp code (← input.eval L env)
  | .call _ (.reference _ name) args => do
    let f ← env.funcs name
    f (← Expr.evalList L env args)
  | .call .. => none
  | .memberAccess _ name base => do
    let .obj address ← base.eval L env | none
    env.fields address name
  | .memberCall _ (.memberAccess _ name base) args => do
    let receiver ← base.eval L env
    let m ← env.methods name
    m receiver (← Expr.evalList L env args)
  | .memberCall .. => none
  | .subscription _ arr (.range _ low high max) => do
    let a ← arr.eval L env
    let l ← Expr.evalOpt L env low
    let h ← Expr.evalOpt L env high
    let m ← Expr.evalOpt L env max
    env.slice a l h m
  | .subscription _ arr idx => do env.index (← arr.eval L env) (← idx.eval L env)
  | .range .. => none
  | .pointerDereference _ input => do env.deref (← input.eval L env)
  | .cast _ t expression => do env.cast t (← expression.eval L env)
  | .new _ t initializer => do env.new t (← initializer.eval L env)
  | .construction _ t args => do env.construct t (← Expr.evalList L env args)
  | .arrayConstruction _ t dims => do env.constructArray t (← Expr.evalList L env dims)
  | .problem .. => none

/-- Evaluates an optional expression; an absent expression has no value, but does not fail. -/
def Expr.evalOpt (L : LanguageSemantics) (env : Env) : Option Expr → Option (Option Value)
  | none => some none
  | some e => some <$> e.eval L env

/-- Evaluates a list of expressions from left to right. -/
def Expr.evalList (L : LanguageSemantics) (env : Env) : List Expr → Option (List Value)
  | [] => some []
  | e :: es => do
    let v ← e.eval L env
    let vs ← Expr.evalList L env es
    pure (v :: vs)

end

end Cpg
