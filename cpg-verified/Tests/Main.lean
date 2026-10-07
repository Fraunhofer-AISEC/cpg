/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified

/-!
# Tests

Executes the compiled translation and evaluators on concrete inputs. The proofs already cover the
Lean definitions; these tests additionally exercise the compiled code (Lean's compiler is part of
the trusted base) and document the expected output. Run with `lake test`.
-/

open Go Cpg

/-- Shorthand for a span. -/
def sp (start stop : Nat) : Span := ⟨start, stop⟩

def ctx : Ctx := { nameScope := some "main", packages := ["fmt"], iota := none }

def constCtx : Ctx := { ctx with iota := some 2 }

def int (span : Span) (v : String) : Go.Expr := .basicLit span .int v

def ident (span : Span) (name : String) : Go.Expr := .ident span name

/-- `f(x, 3) * 2` -/
def callTimesTwo : Go.Expr :=
  .binary (sp 0 13) (.call (sp 0 9) (ident (sp 0 1) "f") [ident (sp 2 3) "x", int (sp 5 6) "3"])
    .mul (int (sp 12 13) "2")

/-- `false && (1/0 == 0)` -/
def shortCircuit : Go.Expr :=
  .binary (sp 0 20) (ident (sp 0 5) "false") .land
    (.paren (sp 9 20)
      (.binary (sp 10 19) (.binary (sp 10 13) (int (sp 10 11) "1") .quo (int (sp 12 13) "0")) .eql
        (int (sp 17 18) "0")))

def goEnv : Go.Env where
  vars
    | "x" => some (.int 5)
    | "p" => some (.obj 1)
    | "fmt.Version" => some (.str "1.0")
    | _ => none
  funcs
    | "f" => some fun
      | [.int a, .int b] => some (.int (a + b))
      | _ => none
    | "fmt.Sprint" => some fun
      | [.int _] => some (.str "printed")
      | _ => none
    | _ => none
  fields
    | 1, "X" => some (.int 3)
    | _, _ => none
  methods
    | "Add" => some fun
      | .obj 1, [.int a] => some (.int (a + 3))
      | _, _ => none
    | _ => none
  index
    | .obj 1, .int i => some (.int (i * 10))
    | _, _ => none
  slice
    | .obj 1, some (.int l), none, none => some (.int l)
    | _, _, _, _ => none
  deref
    | .obj 1 => some (.int 42)
    | _ => none
  cast
    | .arrayType _ (.ident _ "byte"), .str s => some (.int s.utf8ByteSize)
    | .ident _ "T", v => some v
    | _, _ => none
  new
    | .ident _ "T" => some (.obj 7)
    | _ => none
  make
    | .arrayType _ (.ident _ "int"), [.int n] => some (.int n)
    | _, _ => none
  composite
    | .ident _ "T", [(some (.name "X"), .int x)] => some (.int x)
    | _, _ => none

/-- The CPG environment matching `goEnv`: names are resolved as in `ctx`. -/
def cpgEnv : Cpg.Env where
  vars name := goEnv.vars (name.dropPrefix "main.").toString
  funcs name := goEnv.funcs (name.dropPrefix "main.").toString
  fields := goEnv.fields
  methods := goEnv.methods
  index := goEnv.index
  slice := goEnv.slice
  deref := goEnv.deref
  cast
    | .array (.resolved (.object "byte" [])), .str s => some (.int s.utf8ByteSize)
    | .resolved (.object "T" []), v => some v
    | _, _ => none
  construct
    | .resolved (.object "T" []), [] => some .nil
    | _, _ => none
  constructArray
    | .array (.resolved (.object "int" [])), [.int n] => some (.int n)
    | _, _ => none
  new
    | .pointer (.resolved (.object "T" [])), .nil => some (.obj 7)
    | _, _ => none
  composite
    | .resolved (.object "T" []), [(some (.name "main.X"), .int x)] => some (.int x)
    | _, _ => none

def goEval (e : Go.Expr) : Option Value := e.eval ctx.iota ctx.packages goEnv

def cpgEval (e : Go.Expr) : Option Value := (translate ctx e).eval Go.semantics cpgEnv

def isProblem : Cpg.Expr → Bool
  | .problem .. => true
  | _ => false

def tests : List (String × Bool) := [
  ("binary operator with qualified reference",
    translate ctx (.binary (sp 0 5) (ident (sp 0 1) "x") .add (int (sp 4 5) "1"))
      == .binaryOperator (sp 0 5) "+" (.reference (sp 0 1) "main.x")
        (.literal (sp 4 5) (.int 1) (.primitive "int") none)),
  ("package names are not qualified",
    translate ctx (ident (sp 0 3) "fmt") == .reference (sp 0 3) "fmt"),
  ("builtins are not qualified",
    translate ctx (ident (sp 0 3) "len") == .reference (sp 0 3) "len"),
  ("parentheses are dropped and the inner location is kept",
    translate ctx (.paren (sp 0 5) (ident (sp 1 4) "abc")) == .reference (sp 1 4) "main.abc"),
  ("octal literal",
    translate ctx (int (sp 0 4) "0o17") == .literal (sp 0 4) (.int 15) (.primitive "int") none),
  ("legacy octal literal",
    translate ctx (int (sp 0 3) "017") == .literal (sp 0 3) (.int 15) (.primitive "int") none),
  ("rune literal is its code point",
    translate ctx (.basicLit (sp 0 3) .char "'a'")
      == .literal (sp 0 3) (.int 97) (.primitive "rune") none),
  ("true is a named literal",
    translate ctx (ident (sp 0 4) "true")
      == .literal (sp 0 4) (.bool true) (.primitive "bool") (some "true")),
  ("shadowed true is a reference",
    translate { ctx with shadowed := ["true"] } (ident (sp 0 4) "true")
      == .reference (sp 0 4) "main.true"),
  ("shadowed make is a regular call",
    translate { ctx with shadowed := ["make"] } (.call (sp 0 7) (ident (sp 0 4) "make") [])
      == .call (sp 0 7) (.reference (sp 0 4) "make") []),
  ("iota inside a constant declaration",
    translate constCtx (ident (sp 0 4) "iota")
      == .literal (sp 0 4) (.int 2) (.primitive "int") (some "iota")),
  ("iota outside of a constant declaration is a problem",
    isProblem (translate ctx (ident (sp 0 4) "iota"))),
  ("make is outside the verified subset",
    isProblem (translate ctx (.call (sp 0 7) (ident (sp 0 4) "make") [int (sp 5 6) "1"]))),
  ("unsupported nodes become problems",
    isProblem (translate ctx (.unsupported (sp 0 3) "*ast.SelectorExpr"))),
  ("call with qualified callee",
    translate ctx (.call (sp 0 4) (ident (sp 0 1) "f") [int (sp 2 3) "1"])
      == .call (sp 0 4) (.reference (sp 0 1) "main.f")
        [.literal (sp 2 3) (.int 1) (.primitive "int") none]),
  ("evaluation: f(x, 3) * 2 = 16 in Go",
    goEval callTimesTwo == some (.int 16)),
  ("evaluation: f(x, 3) * 2 = 16 in the CPG",
    cpgEval callTimesTwo == some (.int 16)),
  ("evaluation: short circuit skips the division by zero",
    goEval shortCircuit == some (.bool false) && cpgEval shortCircuit == some (.bool false)),
  ("evaluation: division by zero fails in both",
    let e := Go.Expr.binary (sp 0 3) (int (sp 0 1) "1") .quo (int (sp 2 3) "0")
    goEval e == none && cpgEval e == none),
  ("evaluation: truncated division and remainder",
    let quo := Go.Expr.binary (sp 0 4) (int (sp 0 1) "7") .quo (.unary (sp 2 4) .sub (int (sp 3 4) "2"))
    let rem := Go.Expr.binary (sp 0 4) (.unary (sp 0 2) .sub (int (sp 1 2) "7")) .rem (int (sp 3 4) "2")
    cpgEval quo == some (.int (-3)) && cpgEval rem == some (.int (-1))),
  ("evaluation: bitwise operators on negative numbers",
    let e := Go.Expr.binary (sp 0 6) (.unary (sp 0 2) .sub (int (sp 1 2) "6")) .andNot (int (sp 5 6) "3")
    cpgEval e == some (.int (-8)) && goEval e == some (.int (-8))),
  ("package member is a qualified reference",
    translate ctx (.selector (sp 0 11) (ident (sp 0 3) "fmt") "Version")
      == .reference (sp 0 11) "fmt.Version"),
  ("field access is a member access",
    translate ctx (.selector (sp 0 3) (ident (sp 0 1) "p") "X")
      == .memberAccess (sp 0 3) "X" (.reference (sp 0 1) "main.p")),
  ("method call is a member call",
    translate ctx (.call (sp 0 8) (.selector (sp 0 5) (ident (sp 0 1) "p") "Add") [int (sp 6 7) "1"])
      == .memberCall (sp 0 8) (.memberAccess (sp 0 5) "Add" (.reference (sp 0 1) "main.p"))
        [.literal (sp 6 7) (.int 1) (.primitive "int") none]),
  ("package function call is a call",
    translate ctx (.call (sp 0 14) (.selector (sp 0 10) (ident (sp 0 3) "fmt") "Sprint") [int (sp 11 12) "1"])
      == .call (sp 0 14) (.reference (sp 0 10) "fmt.Sprint")
        [.literal (sp 11 12) (.int 1) (.primitive "int") none]),
  ("evaluation: field access, method and package calls agree",
    let field := Go.Expr.selector (sp 0 3) (ident (sp 0 1) "p") "X"
    let method := Go.Expr.call (sp 0 8) (.selector (sp 0 5) (ident (sp 0 1) "p") "Add") [int (sp 6 7) "1"]
    let pkg := Go.Expr.call (sp 0 14) (.selector (sp 0 10) (ident (sp 0 3) "fmt") "Sprint") [int (sp 11 12) "1"]
    goEval field == some (.int 3) && cpgEval field == some (.int 3) &&
    goEval method == some (.int 4) && cpgEval method == some (.int 4) &&
    goEval pkg == some (.str "printed") && cpgEval pkg == some (.str "printed")),
  ("index, slice and dereference",
    translate ctx (.index (sp 0 4) (ident (sp 0 1) "p") (int (sp 2 3) "2"))
      == .subscription (sp 0 4) (.reference (sp 0 1) "main.p")
        (.literal (sp 2 3) (.int 2) (.primitive "int") none) &&
    translate ctx (.slice (sp 0 5) (ident (sp 0 1) "p") (some (int (sp 2 3) "1")) none none)
      == .subscription (sp 0 5) (.reference (sp 0 1) "main.p")
        (.range (sp 0 5) (some (.literal (sp 2 3) (.int 1) (.primitive "int") none)) none none) &&
    translate ctx (.star (sp 0 2) (ident (sp 1 2) "p"))
      == .pointerDereference (sp 0 2) (.reference (sp 1 2) "main.p")),
  ("evaluation: index, slice and dereference agree",
    let idx := Go.Expr.index (sp 0 4) (ident (sp 0 1) "p") (int (sp 2 3) "2")
    let sl := Go.Expr.slice (sp 0 5) (ident (sp 0 1) "p") (some (int (sp 2 3) "1")) none none
    let st := Go.Expr.star (sp 0 2) (ident (sp 1 2) "p")
    goEval idx == some (.int 20) && cpgEval idx == some (.int 20) &&
    goEval sl == some (.int 1) && cpgEval sl == some (.int 1) &&
    goEval st == some (.int 42) && cpgEval st == some (.int 42)),
  ("float literal keeps its text",
    translate ctx (.basicLit (sp 0 7) .float "1_000.5")
      == .literal (sp 0 7) (.float "1000.5") (.primitive "float64") none),
  ("escaped string literal",
    translate ctx (.basicLit (sp 0 6) .string "\"a\\tb\"")
      == .literal (sp 0 6) (.str "a\tb") (.primitive "string") none),
  ("conversion is a cast",
    translate ctx (.call (sp 0 9) (.arrayType (sp 0 6) (ident (sp 2 6) "byte")) [ident (sp 7 8) "x"])
      == .cast (sp 0 9) (.array (.resolved (.object "byte" []))) (.reference (sp 7 8) "main.x")),
  ("type assertion is a cast",
    translate ctx (.typeAssert (sp 0 6) (ident (sp 0 1) "x") (some (ident (sp 3 4) "T")))
      == .cast (sp 0 6) (.resolved (.object "T" [])) (.reference (sp 0 1) "main.x")),
  ("type expressions",
    typeOf? (.mapType (sp 0 1) (ident (sp 0 1) "string") (.star (sp 0 1) (.selector (sp 0 1) (ident (sp 0 1) "big") "Int")))
      == some (.object "map" [.resolved (.object "string" []), .resolved (.pointer (.object "big.Int" []))])),
  ("evaluation: conversion and type assertion agree",
    let conv := Go.Expr.call (sp 0 13) (.arrayType (sp 0 6) (ident (sp 2 6) "byte")) [.basicLit (sp 7 12) .string "\"abc\""]
    let assert := Go.Expr.typeAssert (sp 0 6) (ident (sp 0 1) "x") (some (ident (sp 3 4) "T"))
    goEval conv == some (.int 3) && cpgEval conv == some (.int 3) &&
    goEval assert == some (.int 5) && cpgEval assert == some (.int 5)),
  ("new and make",
    translate ctx (.call (sp 0 6) (ident (sp 0 3) "new") [ident (sp 4 5) "T"])
      == .new (sp 0 6) (.pointer (.resolved (.object "T" [])))
        (.construction (sp 0 6) (.resolved (.object "T" [])) []) &&
    translate ctx (.call (sp 0 13) (ident (sp 0 4) "make")
        [.arrayType (sp 5 10) (ident (sp 7 10) "int"), int (sp 11 12) "3"])
      == .arrayConstruction (sp 0 13) (.array (.resolved (.object "int" [])))
        [.literal (sp 11 12) (.int 3) (.primitive "int") none]),
  ("make with a capacity is outside the subset",
    isProblem (translate ctx (.call (sp 0 16) (ident (sp 0 4) "make")
      [.arrayType (sp 5 10) (ident (sp 7 10) "int"), int (sp 11 12) "3", int (sp 14 15) "4"]))),
  ("evaluation: new and make agree",
    let n := Go.Expr.call (sp 0 6) (ident (sp 0 3) "new") [ident (sp 4 5) "T"]
    let m := Go.Expr.call (sp 0 13) (ident (sp 0 4) "make")
      [.arrayType (sp 5 10) (ident (sp 7 10) "int"), int (sp 11 12) "3"]
    goEval n == some (.obj 7) && cpgEval n == some (.obj 7) &&
    goEval m == some (.int 3) && cpgEval m == some (.int 3)),
  ("composite literal is an initializer list",
    translate ctx (.compositeLit (sp 0 9) (some (ident (sp 0 1) "T"))
        [.keyValue (sp 2 8) (ident (sp 2 3) "X") (int (sp 5 6) "1")])
      == .initializerList (sp 0 9) (.resolved (.object "T" []))
        [.keyValue (sp 2 8) (.reference (sp 2 3) "main.X") (.literal (sp 5 6) (.int 1) (.primitive "int") none)]),
  ("nested literal with elided type is outside the subset",
    isProblem (translate ctx (.compositeLit (sp 0 4) none []))),
  ("evaluation: composite literal agrees",
    let c := Go.Expr.compositeLit (sp 0 9) (some (ident (sp 0 1) "T"))
      [.keyValue (sp 2 8) (ident (sp 2 3) "X") (int (sp 5 6) "1")]
    goEval c == some (.int 1) && cpgEval c == some (.int 1)),
  ("evaluation: unbound names fail in both",
    goEval (ident (sp 0 1) "y") == none && cpgEval (ident (sp 0 1) "y") == none)
]

def main : IO UInt32 := do
  let mut failures := 0
  for (name, ok) in tests do
    if ok then
      IO.println s!"  ok    {name}"
    else
      IO.println s!"  FAIL  {name}"
      failures := failures + 1
  IO.println s!"{tests.length - failures}/{tests.length} tests passed"
  return if failures == 0 then 0 else 1
