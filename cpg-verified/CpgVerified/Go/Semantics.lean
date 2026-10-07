/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Go.Literal

/-!
# Semantics of Go expressions

A big-step evaluator for the Go expression subset, written against the Go specification
(<https://go.dev/ref/spec#Expressions>) and independently of the CPG. Integers are mathematical
integers (see `Cpg.Value`).
-/

namespace Go

open Cpg (Value)

/-- The evaluation environment, keyed by Go identifiers as they appear in the source. -/
structure Env where
  vars : String → Option Value
  funcs : String → Option (List Value → Option Value)
  /-- The heap: the fields of the struct at an address. -/
  fields : Nat → String → Option Value
  /-- Methods by name, applied to the receiver and the arguments. -/
  methods : String → Option (Value → List Value → Option Value)

/--
Value of a predeclared constant identifier (<https://go.dev/ref/spec#Predeclared_identifiers>).
`iota` only has a value inside a constant declaration.
-/
def predeclared (iota : Option Int) : String → Option Value
  | "true" => some (.bool true)
  | "false" => some (.bool false)
  | "nil" => some .nil
  | "iota" => Value.int <$> iota
  | _ => none

/-- Arithmetic, comparison and bitwise operators (<https://go.dev/ref/spec#Operators>). -/
def evalBinary : BinaryOp → Value → Value → Option Value
  | .add, .int x, .int y => some (.int (x + y))
  | .add, .str x, .str y => some (.str (x ++ y))
  | .sub, .int x, .int y => some (.int (x - y))
  | .mul, .int x, .int y => some (.int (x * y))
  | .quo, .int x, .int y => .int <$> Cpg.IntOps.quo x y
  | .rem, .int x, .int y => .int <$> Cpg.IntOps.rem x y
  | .and, .int x, .int y => some (.int (Cpg.IntOps.land x y))
  | .or, .int x, .int y => some (.int (Cpg.IntOps.lor x y))
  | .xor, .int x, .int y => some (.int (Cpg.IntOps.xor x y))
  | .andNot, .int x, .int y => some (.int (Cpg.IntOps.andNot x y))
  | .shl, .int x, .int y => .int <$> Cpg.IntOps.shl x y
  | .shr, .int x, .int y => .int <$> Cpg.IntOps.shr x y
  | .eql, .int x, .int y => some (.bool (x == y))
  | .eql, .str x, .str y => some (.bool (x == y))
  | .eql, .bool x, .bool y => some (.bool (x == y))
  | .eql, .nil, .nil => some (.bool true)
  | .neq, .int x, .int y => some (.bool (x != y))
  | .neq, .str x, .str y => some (.bool (x != y))
  | .neq, .bool x, .bool y => some (.bool (x != y))
  | .neq, .nil, .nil => some (.bool false)
  | .lss, .int x, .int y => some (.bool (decide (x < y)))
  | .lss, .str x, .str y => some (.bool (decide (x < y)))
  | .leq, .int x, .int y => some (.bool (decide (x ≤ y)))
  | .leq, .str x, .str y => some (.bool (decide (x ≤ y)))
  | .gtr, .int x, .int y => some (.bool (decide (y < x)))
  | .gtr, .str x, .str y => some (.bool (decide (y < x)))
  | .geq, .int x, .int y => some (.bool (decide (y ≤ x)))
  | .geq, .str x, .str y => some (.bool (decide (y ≤ x)))
  | _, _, _ => none

/-- Unary operators. Receive (`<-`) and address-of (`&`) are not modelled. -/
def evalUnary : UnaryOp → Value → Option Value
  | .add, .int x => some (.int x)
  | .sub, .int x => some (.int (-x))
  | .xor, .int x => some (.int (Int.not x))
  | .not, .bool x => some (.bool (!x))
  | _, _ => none

mutual

/--
Evaluates a Go expression. Identifiers bound in `env` shadow predeclared ones. Operands are
evaluated left to right; `&&` and `||` short-circuit. `packages` are the names of the imported
packages, whose members are bound in `env` under their qualified name (`fmt.Println`). Calls of
parenthesized selectors, e.g. `(x.f)()`, are not modelled.
-/
def Expr.eval (iota : Option Int) (packages : List String) (env : Env) : Expr → Option Value
  | .basicLit _ kind value => litValue kind value
  | .ident _ name =>
    match env.vars name with
    | some v => some v
    | none => predeclared iota name
  | .binary _ x .land y => do
    let .bool a ← x.eval iota packages env | none
    if a then
      let .bool b ← y.eval iota packages env | none
      pure (.bool b)
    else pure (.bool false)
  | .binary _ x .lor y => do
    let .bool a ← x.eval iota packages env | none
    if a then pure (.bool true)
    else
      let .bool b ← y.eval iota packages env | none
      pure (.bool b)
  | .binary _ x op y => do evalBinary op (← x.eval iota packages env) (← y.eval iota packages env)
  | .unary _ op x => do evalUnary op (← x.eval iota packages env)
  | .paren _ x => x.eval iota packages env
  | .selector _ x sel =>
    match packageOf? packages x with
    | some p => env.vars (p ++ "." ++ sel)
    | none => do
      let .obj address ← x.eval iota packages env | none
      env.fields address sel
  | .call _ (.selector _ x sel) args =>
    match packageOf? packages x with
    | some p => do
      let f ← env.funcs (p ++ "." ++ sel)
      f (← Expr.evalList iota packages env args)
    | none => do
      let receiver ← x.eval iota packages env
      let m ← env.methods sel
      m receiver (← Expr.evalList iota packages env args)
  | .call _ fn args =>
    match fn.unparen with
    | .ident _ name => do
      let f ← env.funcs name
      f (← Expr.evalList iota packages env args)
    | _ => none
  | .unsupported .. => none

/-- Evaluates a list of expressions from left to right. -/
def Expr.evalList (iota : Option Int) (packages : List String) (env : Env) :
    List Expr → Option (List Value)
  | [] => some []
  | e :: es => do
    let v ← e.eval iota packages env
    let vs ← Expr.evalList iota packages env es
    pure (v :: vs)

end

end Go
