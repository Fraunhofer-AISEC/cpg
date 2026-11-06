import CpgVerified

/-!
Lists the axioms the main results depend on. The output must only contain Lean's standard
axioms (`propext`, `Classical.choice`, `Quot.sound`), in particular neither `sorryAx` nor
`Lean.ofReduceBool` (which would mean trusting the compiler via `native_decide`).
-/

#print axioms Go.translate_correct
#print axioms Go.translateList_correct
#print axioms Go.translate_loc
