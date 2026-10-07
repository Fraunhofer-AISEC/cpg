/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/

/-!
# Basic definitions shared by all languages

Source spans, runtime values and the primitive integer operations that both the
language semantics and the CPG semantics are built from.
-/

namespace Cpg

/-- A half-open byte range `[start, stop)` in a source file. -/
structure Span where
  start : Nat
  stop : Nat
deriving Repr, DecidableEq, Inhabited

/-- `outer` fully contains `inner`. -/
def Span.contains (outer inner : Span) : Prop :=
  outer.start ≤ inner.start ∧ inner.stop ≤ outer.stop

/--
Runtime values of the modelled expression fragment.

Integers are mathematical integers, i.e. we model Go's untyped constant arithmetic and
do not model overflow of sized integer types.
-/
inductive Value where
  | int (i : Int)
  | bool (b : Bool)
  | str (s : String)
  | nil
  /--
  A struct or pointer value, identified by its address. Its fields are looked up in the heap of the
  evaluation environment.
  -/
  | obj (address : Nat)
deriving Repr, DecidableEq, Inhabited

namespace IntOps

/-!
Bitwise operations on (two's complement, infinitely sign-extended) integers. Lean core
only provides them for `Nat`; these follow the same construction as Mathlib's `Int.land`
and friends, reducing to `Nat.bitwise` on the magnitudes.
-/

/-- Bitwise and. -/
def land : Int → Int → Int
  | .ofNat m, .ofNat n => .ofNat (Nat.bitwise and m n)
  | .ofNat m, .negSucc n => .ofNat (Nat.bitwise (fun a b => a && !b) m n)
  | .negSucc m, .ofNat n => .ofNat (Nat.bitwise (fun a b => !a && b) m n)
  | .negSucc m, .negSucc n => .negSucc (Nat.bitwise or m n)

/-- Bitwise or. -/
def lor : Int → Int → Int
  | .ofNat m, .ofNat n => .ofNat (Nat.bitwise or m n)
  | .ofNat m, .negSucc n => .negSucc (Nat.bitwise (fun a b => !a && b) m n)
  | .negSucc m, .ofNat n => .negSucc (Nat.bitwise (fun a b => a && !b) m n)
  | .negSucc m, .negSucc n => .negSucc (Nat.bitwise and m n)

/-- Bitwise exclusive or. -/
def xor : Int → Int → Int
  | .ofNat m, .ofNat n => .ofNat (Nat.bitwise bne m n)
  | .ofNat m, .negSucc n => .negSucc (Nat.bitwise bne m n)
  | .negSucc m, .ofNat n => .negSucc (Nat.bitwise bne m n)
  | .negSucc m, .negSucc n => .ofNat (Nat.bitwise bne m n)

/-- Bit clear (`a &^ b` in Go), i.e. `a & ~b`. -/
def andNot (a b : Int) : Int := land a (Int.not b)

/-- Shift left; negative shift counts are a run-time panic. -/
def shl (a b : Int) : Option Int :=
  if b < 0 then none else some (Int.shiftLeft a b.toNat)

/-- Arithmetic shift right; negative shift counts are a run-time panic. -/
def shr (a b : Int) : Option Int :=
  if b < 0 then none else some (Int.shiftRight a b.toNat)

/-- Truncated division; division by zero is a run-time panic. -/
def quo (a b : Int) : Option Int :=
  if b = 0 then none else some (Int.tdiv a b)

/-- Remainder of truncated division; division by zero is a run-time panic. -/
def rem (a b : Int) : Option Int :=
  if b = 0 then none else some (Int.tmod a b)

end IntOps

end Cpg
