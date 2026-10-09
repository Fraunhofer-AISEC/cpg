/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/

/-!
# Canonical S-expressions

The wire format between the JVM and the Lean translation. Atoms are length-prefixed UTF-8 strings
(`5:hello`), lists are enclosed in parentheses, and there is no whitespace inside an expression.
This makes the format unambiguous and trivial to produce and parse on both sides.

The (de)serialization is part of the trusted base; it is not verified (yet).
-/

namespace Wire

inductive Sexp where
  | atom (s : String)
  | list (xs : List Sexp)
deriving Repr, Inhabited, BEq

partial def Sexp.encode : Sexp → String
  | .atom s => s!"{s.utf8ByteSize}:{s}"
  | .list xs => "(" ++ String.join (xs.map encode) ++ ")"

private def isDigit (b : UInt8) : Bool := b ≥ 48 && b ≤ 57

private def isSpace (b : UInt8) : Bool := b == 32 || b == 10 || b == 13 || b == 9

mutual

/-- Parses one expression starting at position `i`; returns it with the position after it. -/
partial def parseAt (b : ByteArray) (i : Nat) : Except String (Sexp × Nat) := do
  let some c := b[i]? | throw s!"unexpected end of input at {i}"
  if c == 40 then -- '('
    parseList b (i + 1) #[]
  else if isDigit c then
    let mut j := i
    let mut len := 0
    while h : j < b.size do
      let d := b[j]
      if !isDigit d then break
      len := len * 10 + (d - 48).toNat
      j := j + 1
    unless b[j]? == some 58 do throw s!"expected ':' at {j}" -- ':'
    let start := j + 1
    unless start + len ≤ b.size do throw s!"atom at {i} exceeds the input"
    let some s := String.fromUTF8? (b.extract start (start + len))
      | throw s!"atom at {i} is not valid UTF-8"
    return (.atom s, start + len)
  else
    throw s!"unexpected byte {c} at {i}"

/-- Parses the elements of a list until the closing parenthesis. -/
partial def parseList (b : ByteArray) (i : Nat) (acc : Array Sexp) :
    Except String (Sexp × Nat) := do
  let some c := b[i]? | throw "unterminated list"
  if c == 41 then -- ')'
    return (.list acc.toList, i + 1)
  else
    let (x, j) ← parseAt b i
    parseList b j (acc.push x)

end

/-- Parses a sequence of expressions, separated by optional whitespace. -/
partial def parseAll (b : ByteArray) : Except String (List Sexp) :=
  go 0 #[]
where
  go (i : Nat) (acc : Array Sexp) : Except String (List Sexp) := do
    match b[i]? with
    | none => return acc.toList
    | some c =>
      if isSpace c then go (i + 1) acc
      else
        let (x, j) ← parseAt b i
        go j (acc.push x)

#guard (Sexp.list [.atom "ab", .list [], .atom "€"]).encode == "(2:ab()3:€)"

end Wire
