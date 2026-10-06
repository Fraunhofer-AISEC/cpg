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

Intentional deviations from the Kotlin handler: `new` and `make`, as well as literals whose value
is not modelled (floating-point, imaginary, escape sequences), become `ProblemExpression`s, i.e.
they are outside the verified subset. Shadowing of predeclared identifiers is not modelled; it
is excluded by the precondition of `translate_correct` instead.
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
  match name with
  | "true" => .literal span (.bool true) (.primitive "bool") (some name)
  | "false" => .literal span (.bool false) (.primitive "bool") (some name)
  | "nil" => .literal span .nil .unknown (some name)
  | "iota" =>
    match ctx.iota with
    | some i => .literal span (.int i) (.primitive "int") (some name)
    | none => .problem span "iota outside of constant declaration"
  | _ => .reference span (resolveName ctx name)

/-- Whether the callee is one of the built-in allocation functions `new` and `make`. -/
def isAllocation (fn : Expr) : Bool :=
  match fn.unparen with
  | .ident _ name => name == "new" || name == "make"
  | _ => false

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
  | .call span fn args =>
    if isAllocation fn then .problem span "new and make are not in the verified subset"
    else .call span (translate ctx fn) (translateList ctx args)
  | .unsupported span goType => .problem span s!"{goType} is not in the verified subset"

/-- Translates a list of expressions. -/
def translateList (ctx : Ctx) : List Expr → List Cpg.Expr
  | [] => []
  | e :: es => translate ctx e :: translateList ctx es

end

end Go
