/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Go.Literal
import CpgVerified.Cpg.Expr

/-!
# Translation of Go expressions into CPG nodes

The verified counterpart of `ExpressionHandler` in `cpg-language-go`. Everything the Kotlin
handler reads from mutable frontend state (current scope, imports, `iota`) is an explicit
input (`Ctx`), so the translation is a pure, total function.

Intentional deviations from the Kotlin handler: `new` and `make`, calls of anything but a named
function or a (non-parenthesized) selector, and literals whose value is not modelled (floating-point, imaginary, escape sequences)
become `ProblemExpression`s, i.e. they are outside the verified subset. Shadowing of predeclared
identifiers is taken from the context (`Ctx.shadowed`).
-/

namespace Go

open Cpg (Span Value TypeRef)

/-- Everything the translation needs to know about the surrounding code. -/
structure Ctx where
  /--
  The fully qualified name of the current scope, if it is a name scope (i.e. we are at package
  level rather than inside a function).
  -/
  nameScope : Option String
  /-- Names under which imported packages are visible in the current file. -/
  packages : List String
  /-- The value of `iota`, if we are inside a constant declaration. -/
  iota : Option Int
  /--
  Predeclared identifiers (such as `true` or `make`) that are shadowed by a visible declaration and
  therefore refer to that declaration.
  -/
  shadowed : List String := []

/-- Predeclared identifiers that are never qualified with the current namespace. -/
def builtins : List String :=
  [ "bool", "uint8", "uint16", "uint32", "uint64", "int8", "int16", "int32", "int64", "float32",
    "float64", "complex64", "complex128", "string", "int", "uint", "uintptr", "byte", "rune",
    "any", "comparable", "iota", "nil", "append", "copy", "delete", "len", "cap", "make", "max",
    "min", "new", "complex", "real", "imag", "clear", "close", "panic", "recover", "print",
    "println", "error" ]

/-- The name a `Reference` to the identifier `name` gets. -/
def resolveName (ctx : Ctx) (name : String) : String :=
  if name ∈ builtins ∨ name ∈ ctx.packages then name
  else
    match ctx.nameScope with
    | some ns => ns ++ "." ++ name
    | none => name

/-- The type the frontend assigns to a literal of the given kind. -/
def litType : LitKind → TypeRef
  | .int => .primitive "int"
  | .float => .primitive "float64"
  | .imag => .primitive "complex128"
  | .char => .primitive "rune"
  | .string => .primitive "string"

/-- Translates an identifier: predeclared constants become literals, everything else a reference. -/
def translateIdent (ctx : Ctx) (span : Span) (name : String) : Cpg.Expr :=
  if name ∈ ctx.shadowed then .reference span (resolveName ctx name) else
  match name with
  | "true" => .literal span (.bool true) (.primitive "bool") (some name)
  | "false" => .literal span (.bool false) (.primitive "bool") (some name)
  | "nil" => .literal span .nil .unknown (some name)
  | "iota" =>
    match ctx.iota with
    | some i => .literal span (.int i) (.primitive "int") (some name)
    | none => .problem span "iota outside of constant declaration"
  | _ => .reference span (resolveName ctx name)

/--
The name of the called function, if the callee is a (parenthesized) identifier. Other callees,
e.g. type expressions in conversions like `[]byte(s)`, are not in the verified subset yet.
-/
def calleeName? (fn : Expr) : Option String :=
  match fn.unparen with
  | .ident _ name => some name
  | _ => none

mutual

/-- Translates a Go expression into a CPG expression. -/
def translate (ctx : Ctx) : Expr → Cpg.Expr
  | .basicLit span kind value =>
    match litValue kind value with
    | some v => .literal span v (litType kind) none
    | none => .problem span s!"literal {value} is not in the verified subset"
  | .ident span name => translateIdent ctx span name
  | .binary span x op y => .binaryOperator span op.token (translate ctx x) (translate ctx y)
  | .unary span op x => .unaryOperator span op.token (translate ctx x)
  | .paren _ x => translate ctx x
  | .selector span x sel =>
    match packageOf? ctx.packages x with
    -- A member of an imported package is referred to by its qualified name
    | some p => .reference span (p ++ "." ++ sel)
    | none => .memberAccess span sel (translate ctx x)
  | .call span fn@(.selector _ x _) args =>
    match packageOf? ctx.packages x with
    | some _ => .call span (translate ctx fn) (translateList ctx args)
    | none => .memberCall span (translate ctx fn) (translateList ctx args)
  | .call span fn args =>
    match calleeName? fn with
    | some name =>
      if (name == "new" || name == "make") && !ctx.shadowed.contains name then
        .problem span "new and make are not in the verified subset"
      else .call span (translate ctx fn) (translateList ctx args)
    | none => .problem span "only calls of named functions and methods are in the verified subset"
  | .index span x i => .subscription span (translate ctx x) (translate ctx i)
  | .slice span x low high max =>
    .subscription span (translate ctx x)
      (.range span (translateOpt ctx low) (translateOpt ctx high) (translateOpt ctx max))
  | .star span x => .pointerDereference span (translate ctx x)
  | .unsupported span goType => .problem span s!"{goType} is not in the verified subset"

/-- Translates an optional expression. -/
def translateOpt (ctx : Ctx) : Option Expr → Option Cpg.Expr
  | none => none
  | some e => some (translate ctx e)

/-- Translates a list of expressions. -/
def translateList (ctx : Ctx) : List Expr → List Cpg.Expr
  | [] => []
  | e :: es => translate ctx e :: translateList ctx es

end

end Go
