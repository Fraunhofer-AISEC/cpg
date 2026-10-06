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
  vars | "x" => some (.int 5) | _ => none
  funcs
    | "f" => some fun
      | [.int a, .int b] => some (.int (a + b))
      | _ => none
    | _ => none

/-- The CPG environment matching `goEnv`: names are resolved as in `ctx`. -/
def cpgEnv : Cpg.Env where
  vars name := goEnv.vars (name.dropPrefix "main.").toString
  funcs name := goEnv.funcs (name.dropPrefix "main.").toString

def goEval (e : Go.Expr) : Option Value := e.eval ctx.iota goEnv

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
