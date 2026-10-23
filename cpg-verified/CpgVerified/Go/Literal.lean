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
the lexical structure (e.g. the placement of `_`). Imaginary literals, and strings that are not
valid UTF-8, are not modelled; their value is `none`. Floating-point values are kept as text, since
operations on them are not modelled.
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

/-- One unit of an interpreted string or rune literal: a code point, or a byte from `\x`/octal. -/
inductive LitUnit where
  | codePoint (n : Nat)
  | byte (n : Nat)
deriving Repr, DecidableEq

/-- The numeric value of a unit. -/
def LitUnit.value : LitUnit → Nat
  | .codePoint n | .byte n => n

/--
Decodes the escape sequences of an interpreted string or rune literal
(<https://go.dev/ref/spec#Rune_literals>).
-/
def unescape : List Char → Option (List LitUnit)
  | [] => some []
  | '\\' :: 'a' :: rest => (.codePoint 7 :: ·) <$> unescape rest
  | '\\' :: 'b' :: rest => (.codePoint 8 :: ·) <$> unescape rest
  | '\\' :: 'f' :: rest => (.codePoint 12 :: ·) <$> unescape rest
  | '\\' :: 'n' :: rest => (.codePoint 10 :: ·) <$> unescape rest
  | '\\' :: 'r' :: rest => (.codePoint 13 :: ·) <$> unescape rest
  | '\\' :: 't' :: rest => (.codePoint 9 :: ·) <$> unescape rest
  | '\\' :: 'v' :: rest => (.codePoint 11 :: ·) <$> unescape rest
  | '\\' :: '\\' :: rest => (.codePoint 92 :: ·) <$> unescape rest
  | '\\' :: '\'' :: rest => (.codePoint 39 :: ·) <$> unescape rest
  | '\\' :: '"' :: rest => (.codePoint 34 :: ·) <$> unescape rest
  | '\\' :: 'x' :: a :: b :: rest => do
    let v ← parseDigits 16 [a, b]
    (.byte v :: ·) <$> unescape rest
  | '\\' :: 'u' :: a :: b :: c :: d :: rest => do
    let v ← parseDigits 16 [a, b, c, d]
    (.codePoint v :: ·) <$> unescape rest
  | '\\' :: 'U' :: a :: b :: c :: d :: e :: f :: g :: h :: rest => do
    let v ← parseDigits 16 [a, b, c, d, e, f, g, h]
    if v.isValidChar then (.codePoint v :: ·) <$> unescape rest else none
  | '\\' :: a :: b :: c :: rest => do
    let v ← parseDigits 8 [a, b, c]
    if v < 256 then (.byte v :: ·) <$> unescape rest else none
  | '\\' :: _ => none
  | c :: rest => (.codePoint c.toNat :: ·) <$> unescape rest

/-- The UTF-8 encoding of a unit; bytes are taken as they are. -/
def LitUnit.utf8 : LitUnit → List UInt8
  | .codePoint n => (String.singleton (Char.ofNat n)).toUTF8.toList
  | .byte n => [n.toUInt8]

/--
The value of a string literal. Raw strings (in back quotes) drop carriage returns; interpreted
strings (in double quotes) have their escape sequences decoded. Go strings are arbitrary byte
sequences; strings that are not valid UTF-8 are not modelled.
-/
def parseStringLit (s : String) : Option String :=
  match s.toList with
  | '`' :: _ => do
    let inner ← stripDelimiters '`' s.toList
    pure (String.ofList (inner.filter (· != '\r')))
  | _ => do
    let inner ← stripDelimiters '"' s.toList
    let units ← unescape inner
    String.fromUTF8? (ByteArray.mk (units.flatMap LitUnit.utf8).toArray)

/-- The value of a rune literal, i.e. its code point. -/
def parseRuneLit (s : String) : Option Nat := do
  let inner ← stripDelimiters '\'' s.toList
  match ← unescape inner with
  | [u] => some u.value
  | _ => none

/-- The value of a floating-point literal: its decimal or hexadecimal text without separators. -/
def parseFloatLit (s : String) : String :=
  String.ofList (s.toList.filter (· != '_'))

/-- The value denoted by a basic literal. -/
def litValue : LitKind → String → Option Value
  | .int, s => (Value.int ∘ Int.ofNat) <$> parseIntLit s
  | .string, s => Value.str <$> parseStringLit s
  | .char, s => (Value.int ∘ Int.ofNat) <$> parseRuneLit s
  | .float, s => some (.float (parseFloatLit s))
  | .imag, _ => none

example : parseIntLit "42" = some 42 := by decide
example : parseIntLit "1_000_000" = some 1000000 := by decide
example : parseIntLit "0x_Ff" = some 255 := by decide
example : parseIntLit "0o17" = some 15 := by decide
example : parseIntLit "017" = some 15 := by decide
example : parseIntLit "0b101" = some 5 := by decide
example : parseIntLit "0" = some 0 := by decide
example : parseRuneLit "'a'" = some 97 := by decide
#guard parseStringLit "\"hi\"" == some "hi"
example : parseRuneLit "'\\n'" = some 10 := by decide
example : parseRuneLit "'\\x41'" = some 65 := by decide
example : parseRuneLit "'\\101'" = some 65 := by decide
example : parseRuneLit "'\\u00e9'" = some 233 := by decide
example : parseRuneLit "'\\''" = some 39 := by decide
#guard parseStringLit "\"a\\tb\"" == some "a\tb"
#guard parseStringLit "\"\\xe2\\x82\\xac\"" == some "€"
#guard parseStringLit "\"\\xff\"" == none
#guard parseStringLit "\"\\u00e9\"" == some "é"
#guard parseFloatLit "1_000.5e3" == "1000.5e3"
example : parseStringLit "`a\rb`" = some "ab" := by decide

end Go
