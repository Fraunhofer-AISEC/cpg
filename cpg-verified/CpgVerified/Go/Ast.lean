/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Cpg.Basic

/-!
# Go abstract syntax (expression subset)

A model of the expression nodes of Go's `go/ast` package, as produced by the Go parser that the
Go frontend uses (`libgoast`). Node kinds that are not (yet) part of the verified subset are
represented by `unsupported`, so that every parsed expression has a representation.
-/

namespace Go

open Cpg (Span)

/-- `token.Token` values that can appear as the `Kind` of a `*ast.BasicLit`. -/
inductive LitKind where
  | int | float | imag | char | string
deriving Repr, DecidableEq, Inhabited

/-- `token.Token` values that can appear as the `Op` of a `*ast.BinaryExpr`. -/
inductive BinaryOp where
  | add | sub | mul | quo | rem
  | and | or | xor | shl | shr | andNot
  | land | lor
  | eql | neq | lss | leq | gtr | geq
deriving Repr, DecidableEq, Inhabited

/-- `token.Token` values that can appear as the `Op` of a `*ast.UnaryExpr`. -/
inductive UnaryOp where
  | add | sub | not | xor | arrow | and | tilde
deriving Repr, DecidableEq, Inhabited

/-- `ast.Expr`. Every node carries its source span (`Pos()`/`End()`). -/
inductive Expr where
  /-- `*ast.BasicLit`; `value` is the literal's source text. -/
  | basicLit (span : Span) (kind : LitKind) (value : String)
  /-- `*ast.Ident` -/
  | ident (span : Span) (name : String)
  /-- `*ast.BinaryExpr` -/
  | binary (span : Span) (x : Expr) (op : BinaryOp) (y : Expr)
  /-- `*ast.UnaryExpr` -/
  | unary (span : Span) (op : UnaryOp) (x : Expr)
  /-- `*ast.ParenExpr` -/
  | paren (span : Span) (x : Expr)
  /-- `*ast.CallExpr` (without `...` spreading) -/
  | call (span : Span) (fn : Expr) (args : List Expr)
  /-- `*ast.SelectorExpr`, i.e. `x.sel` -/
  | selector (span : Span) (x : Expr) (sel : String)
  /-- `*ast.IndexExpr`, i.e. `x[index]` -/
  | index (span : Span) (x : Expr) (index : Expr)
  /-- `*ast.SliceExpr`, i.e. `x[low:high:max]` -/
  | slice (span : Span) (x : Expr) (low high max : Option Expr)
  /-- `*ast.StarExpr`, i.e. a pointer dereference `*x` or a pointer type `*T` -/
  | star (span : Span) (x : Expr)
  /-- `*ast.TypeAssertExpr`, i.e. `x.(T)`, or `x.(type)` in a type switch -/
  | typeAssert (span : Span) (x : Expr) (type : Option Expr)
  /-- `*ast.CompositeLit`, i.e. `T{elts...}`; the type is elided in nested literals -/
  | compositeLit (span : Span) (type : Option Expr) (elts : List Expr)
  /-- `*ast.KeyValueExpr`, i.e. `key: value` in a composite literal -/
  | keyValue (span : Span) (key value : Expr)
  /-- `*ast.ArrayType`, i.e. `[n]T` or `[]T` (the length is not modelled) -/
  | arrayType (span : Span) (elt : Expr)
  /-- `*ast.MapType`, i.e. `map[K]V` -/
  | mapType (span : Span) (key value : Expr)
  /-- `*ast.ChanType`, i.e. `chan T` (the direction is not modelled) -/
  | chanType (span : Span) (value : Expr)
  /-- Any other expression node; `goType` is its Go type name, e.g. `*ast.CompositeLit`. -/
  | unsupported (span : Span) (goType : String)
deriving Repr, Inhabited

/-- The source span of an expression. -/
def Expr.span : Expr → Span
  | .basicLit span .. | .ident span _ | .binary span .. | .unary span ..
  | .paren span _ | .call span .. | .selector span .. | .index span .. | .slice span ..
  | .star span _ | .typeAssert span .. | .compositeLit span .. | .keyValue span .. | .arrayType span _
  | .mapType span .. | .chanType span _
  | .unsupported span _ => span

/-- Removes any enclosing parentheses. -/
def Expr.unparen : Expr → Expr
  | .paren _ x => x.unparen
  | e => e

/--
The imported package that a selector `x.sel` refers into, if `x` is (a parenthesized) identifier
that names an imported package.
-/
def packageOf? (packages : List String) (x : Expr) : Option String :=
  match x.unparen with
  | .ident _ name => if name ∈ packages then some name else none
  | _ => none

/-- Whether a callee is a type expression that makes the call a conversion, e.g. `[]byte(s)`. -/
def isConversion : Expr → Bool
  | .arrayType .. | .chanType .. | .mapType .. => true
  | _ => false

/-- Whether a type expression is an array or slice type, for which `make` creates an array. -/
def isArrayType : Expr → Bool
  | .arrayType .. => true
  | _ => false

/--
Whether the keys of the elements of a composite literal are in the verified subset: identifiers
(except the predeclared constants) and basic literals.
-/
def keysSupported (elts : List Expr) : Bool :=
  elts.all fun
    | .keyValue _ (.ident _ name) _ => !["true", "false", "nil", "iota"].contains name
    | .keyValue _ (.basicLit ..) _ => true
    | .keyValue .. => false
    | _ => true

/-- The source text of a binary operator. -/
def BinaryOp.token : BinaryOp → String
  | .add => "+" | .sub => "-" | .mul => "*" | .quo => "/" | .rem => "%"
  | .and => "&" | .or => "|" | .xor => "^" | .shl => "<<" | .shr => ">>" | .andNot => "&^"
  | .land => "&&" | .lor => "||"
  | .eql => "==" | .neq => "!=" | .lss => "<" | .leq => "<=" | .gtr => ">" | .geq => ">="

/-- The source text of a unary operator. -/
def UnaryOp.token : UnaryOp → String
  | .add => "+" | .sub => "-" | .not => "!" | .xor => "^" | .arrow => "<-" | .and => "&"
  | .tilde => "~"

end Go
