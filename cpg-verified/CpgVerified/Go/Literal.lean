/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Go.Ast

/-!
# Values of Go literals

The value denoted by the source text of a `*ast.BasicLit`, following
<https://go.dev/ref/spec#Integer_literals>, <https://go.dev/ref/spec#Rune_literals> and
<https://go.dev/ref/spec#String_literals>.

The input has already been accepted by the Go parser, so these functions do not re-validate
the lexical structure (e.g. the placement of `_`). Escape sequences in rune and interpreted
string literals, as well as floating-point and imaginary literals, are not modelled yet; their
value is `none`.
-/

namespace Go

open Cpg (Value)

/-- The value of a single hexadecimal (or lower base) digit. -/
def digitValue (c : Char) : Option Nat :=
  if '0' ≤ c ∧ c ≤ '9' then some (c.toNat - '0'.toNat)
  else if 'a' ≤ c ∧ c ≤ 'f' then some (c.toNat - 'a'.toNat + 10)
  else if 'A' ≤ c ∧ c ≤ 'F' then some (c.toNat - 'A'.toNat + 10)
  else none

/-- The value of a non-empty sequence of digits in the given base. -/
def parseDigits (base : Nat) : List Char → Option Nat
  | [] => none
  | cs => cs.foldlM (init := 0) fun acc c => do
    let d ← digitValue c
    if d < base then some (acc * base + d) else none

/--
The value of an integer literal: decimal, `0x` hexadecimal, `0o` octal, `0b` binary, or a
legacy octal literal with a leading `0`. Underscores are digit separators.
-/
def parseIntLit (s : String) : Option Nat :=
  match s.toList.filter (· != '_') with
  | ['0'] => some 0
  | '0' :: c :: rest =>
    if c == 'x' || c == 'X' then parseDigits 16 rest
    else if c == 'o' || c == 'O' then parseDigits 8 rest
    else if c == 'b' || c == 'B' then parseDigits 2 rest
    else parseDigits 8 (c :: rest)
  | cs => parseDigits 10 cs

/-- Strips the delimiter `d` from both ends of `cs`. -/
def stripDelimiters (d : Char) (cs : List Char) : Option (List Char) :=
  match cs with
  | c :: rest =>
    if c == d then
      match rest.reverse with
      | c' :: inner => if c' == d then some inner.reverse else none
      | [] => none
    else none
  | [] => none

/--
The value of a string literal. Raw strings (in back quotes) drop carriage returns; interpreted
strings (in double quotes) must not contain escape sequences.
-/
def parseStringLit (s : String) : Option String :=
  match s.toList with
  | '`' :: _ => do
    let inner ← stripDelimiters '`' s.toList
    pure (String.ofList (inner.filter (· != '\r')))
  | _ => do
    let inner ← stripDelimiters '"' s.toList
    if inner.contains '\\' then none else pure (String.ofList inner)

/-- The value of a rune literal (a single character without escape sequence). -/
def parseRuneLit (s : String) : Option Nat :=
  match s.toList with
  | ['\'', c, '\''] => if c == '\\' then none else some c.toNat
  | _ => none

/-- The value denoted by a basic literal. -/
def litValue : LitKind → String → Option Value
  | .int, s => (Value.int ∘ Int.ofNat) <$> parseIntLit s
  | .string, s => Value.str <$> parseStringLit s
  | .char, s => (Value.int ∘ Int.ofNat) <$> parseRuneLit s
  | .float, _ | .imag, _ => none

example : parseIntLit "42" = some 42 := by decide
example : parseIntLit "1_000_000" = some 1000000 := by decide
example : parseIntLit "0x_Ff" = some 255 := by decide
example : parseIntLit "0o17" = some 15 := by decide
example : parseIntLit "017" = some 15 := by decide
example : parseIntLit "0b101" = some 5 := by decide
example : parseIntLit "0" = some 0 := by decide
example : parseRuneLit "'a'" = some 97 := by decide
example : parseStringLit "\"hi\"" = some "hi" := by decide
example : parseStringLit "`a\rb`" = some "ab" := by decide

end Go
